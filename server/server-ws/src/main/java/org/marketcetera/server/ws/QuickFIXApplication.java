package org.marketcetera.server.ws;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.marketcetera.server.ws.brokers.Broker;
import org.marketcetera.server.ws.brokers.Brokers;
import org.marketcetera.server.ws.brokers.LogonAction;
import org.marketcetera.server.ws.brokers.LogoutAction;
import org.marketcetera.event.QuoteEvent;
import org.marketcetera.event.TickEvent;
import org.marketcetera.info.*;
import org.marketcetera.marketdata.DataReferenceKey;
import org.marketcetera.module.RequestID;
import org.marketcetera.quickfix.FIXMessageUtil;
import org.marketcetera.ws.server.filters.MessageFilter;
import org.marketcetera.trade.*;
import org.marketcetera.util.except.I18NException;
import org.marketcetera.util.log.SLF4JLoggerProxy;
import org.marketcetera.util.misc.ClassVersion;
import org.marketcetera.ws.ResponseMessage;

import quickfix.*;
import quickfix.field.*;

/**
 * The QuickFIX/J intermediary, intercepting messages from/to the
 * QuickFIX/J counterparts and the Server.
 */
@ClassVersion("$Id: QuickFIXApplication.java 16639 2013-07-23 01:36:32Z colin $")
public class QuickFIXApplication
    implements Application, ServerReceiver
{	
    // CLASS DATA
    private static final String HEARTBEAT_CATEGORY=
    		QuickFIXApplication.class.getName()+".HEARTBEATS"; //$NON-NLS-1$
    
    // INSTANCE DATA.
    private final SystemInfo mSystemInfo;
    private final Brokers mBrokers;
    private final MessageFilter mSupportedMessages;
    private final ServerPersister mServerPersister;
    private final ServerManager mServerManager;
    private final Map<Broker, MessageProcessor> messageProcessors = new HashMap<Broker, MessageProcessor>();

    // CONSTRUCTORS.
    public QuickFIXApplication
        (SystemInfo systemInfo,
         Brokers brokers,
         MessageFilter supportedMessages,
         ServerPersister serverPersister,
         ServerManager serverManager)
    {
        mSystemInfo=systemInfo;
        mBrokers=brokers;
        mSupportedMessages=supportedMessages;
        mServerPersister=serverPersister;
        mServerManager=serverManager;
        for(Broker b:brokers.getBrokers()){
        	messageProcessors.put(b, new MessageProcessor(b));
        }
    }

    // INSTANCE METHODS.
    public SystemInfo getSystemInfo()
    {
        return mSystemInfo;         
    }
    
    /**
     * Gets the <code>Brokers</code> value.
     *
     * @return an <code>Brokers</code> value
     */     
    public Brokers getBrokers()
    {
        return mBrokers;
    }

    public MessageFilter getSupportedMessages()
    {
        return mSupportedMessages;
    }
    
    /**
     * Gets the <code>ServerPersister</code> value.
     *
     * @return an <code>ServerPersister</code> value
     */
    public ServerPersister getServerPersister()
    {
        return mServerPersister;
    }
    
    /**
     * Gets the <code>ServerManager</code> value.
     *
     * @return an <code>ServerManager</code> value
     */
    public ServerManager getServerManager()
    {
        return mServerManager;
    }

    private Object getCategory
        (Message msg)
    {
        if (FIXMessageUtil.isHeartbeat(msg)) {
            return HEARTBEAT_CATEGORY;
        }
        return this;
    }
	
    /* (non-Javadoc)
     * @see org.marketcetera.server.ws.ServerReceiver#addReport(org.marketcetera.trade.ExecutionReport)
     */
    @Override
    public void addReport(ExecutionReport inReport)
    {
        SLF4JLoggerProxy.debug(this,
                               "Manually adding {}", //$NON-NLS-1$
                               inReport);
        if(!(inReport instanceof FIXMessageSupport)) {
            throw new UnsupportedOperationException();
        }
        Broker broker = getBrokers().getTradeBroker(inReport.getBrokerID());
        if(broker == null) {
            throw new IllegalArgumentException(Messages.QF_UNKNOWN_BROKER_ID.getText(inReport.getBrokerID()));
        }
        SessionID sessionID = broker.getSessionID();
        Message msg = ((FIXMessageSupport)inReport).getMessage();
        try {
            SessionSettings sessionSettings = broker.getDescriptor().getSettings().getQSettings();
            // need to modify message version of this message to match the broker's
            msg.getHeader().setField(new BeginString(broker.getFIXVersion().toString()));
            // invert the target and sender because the message is supposed to have come *from* the target *to* the sender
            msg.getHeader().setField(new SenderCompID(sessionSettings.getString(sessionID,
                                                                                SessionSettings.TARGETCOMPID)));
            msg.getHeader().setField(new TargetCompID(sessionSettings.getString(sessionID,
                                                                                SessionSettings.SENDERCOMPID)));
            // mark these messages as stinkers if there's ever any question about the data
            msg.getHeader().setField(new MsgSeqNum(Integer.MIN_VALUE));
            if(!msg.getHeader().isSetField(SendingTime.FIELD)) {
                msg.getHeader().setField(new SendingTime(new Date()));
            }
            // recalculate checksum and length
            String newMessageValue = msg.toString();
            SLF4JLoggerProxy.debug(this,
                                   "Message converted to {}", //$NON-NLS-1$
                                   newMessageValue);
            // validate fix message with the broker's dictionary
            broker.getDataDictionary().validate(msg);
        } catch (IncorrectTagValue 
        		| FieldNotFound | IncorrectDataFormat 
        		| ConfigError | FieldConvertError e) {
            throw new IllegalArgumentException(Messages.QF_CANNOT_ADD_INVALID_REPORT.getText(ExceptionUtils.getRootCauseMessage(e)));
        }
        Messages.QF_FROM_APP.info(getCategory(msg), msg, broker);
        messageProcessors.get(getBrokers().getBroker(sessionID)).add(new MessagePackage(msg,
        		MessageType.FROM_APP, sessionID));
    }
    /* (non-Javadoc)
     * @see org.marketcetera.server.ws.ServerReceiver#deleteReport(org.marketcetera.trade.ExecutionReport)
     */
    @Override
    public void deleteReport(ExecutionReport inReport)
    {
        SLF4JLoggerProxy.debug(this, "Deleting {}", inReport);
        getServerPersister().deleteMessage(inReport);
    }
    
    /* (non-Javadoc)
     * @see org.marketcetera.server.ws.ServerReceiver#addQuote(org.marketcetera.event.QuoteEvent)
     */
	@Override
	public void addQuote(QuoteEvent inQuote) {
		SLF4JLoggerProxy.debug(this,
                "Manually adding {}", inQuote);
		
		DataReferenceKey key = new DataReferenceKey(inQuote.getBrokerID(), inQuote.getInstrument(), FIXMessageUtil.DEFAULT_EXCH);
		DataInfo dataInfo = getServerPersister().get(key);
		if(dataInfo != null){
			Collection<UserID> actorIDs = dataInfo.getUsers();
			for(UserID actorID:actorIDs){
				Messages.QF_SENDING_REPLY.info(this,inQuote);
		        getServerManager().convertAndSend(inQuote, actorID);
			}
		}
		getServerPersister().persistMessage(inQuote);
	}
	
    /* (non-Javadoc)
     * @see org.marketcetera.server.ws.ServerReceiver#deleteQuote(org.marketcetera.event.QuoteEvent)
     */
	@Override
	public void deleteQuote(QuoteEvent inQuote) {
        SLF4JLoggerProxy.debug(this,  "Deleting {}", inQuote);
        getServerPersister().deleteMessage(inQuote);
	}
	
    private void sendToClient
    (Broker b, boolean status)
    {
	    if (b.getLoggedOn()==status) {
	        return;
	    }
	    Messages.QF_SENDING_STATUS.info(this,status,b);
	    b.setLoggedOn(status);
	    getServerManager().convertAndBroadcast(b.getStatus());
	    
	    if(status){
            // Re-submit market data requests if 
	    	// broker has market data requests
	    	Set<Message> requests = getServerPersister().get(b.getBrokerID());
	    	if(requests != null){
	    		for(Message request:requests){
	    			try {
	    				b.sendToTarget(request);
					} catch (SessionNotFound ignore) {}
	    		}
	    	}
	    }
	}
	
    private void sendToClient
    (Broker b, 
    		Message msg, Originator originator)
    {    	
		// Obtain actorIDs.
        Collection<UserID> actorIDs=getServerPersister().getActorIDs(msg,false);
        if(actorIDs.isEmpty()){
        	actorIDs=getServerManager().getUserIDs();
        }
        
        for(UserID actorID:actorIDs){
        	// Apply message modifiers.
    	    if (b.getResponseModifiers()!=null) {
    	        try {	            
                    SessionInfo sessionInfo=new SessionInfoImpl(getSystemInfo());
                    sessionInfo.setValue(SessionInfo.ACTOR_ID,actorID);
                    RequestInfo requestInfo=new RequestInfoImpl(sessionInfo);
    	            requestInfo.setValue(RequestInfo.BROKER,b);
    	            requestInfo.setValue(RequestInfo.BROKER_ID,b.getBrokerID());
    	            requestInfo.setValue(RequestInfo.ORIGINATOR,originator);
    	            requestInfo.setValue(RequestInfo.FIX_MESSAGE_FACTORY,b.getFIXMessageFactory());
    	            requestInfo.setValue(RequestInfo.CURRENT_MESSAGE,msg);
    	            b.getResponseModifiers().modifyMessage(requestInfo);
    	            msg=requestInfo.getValueIfInstanceOf(RequestInfo.CURRENT_MESSAGE,Message.class);
    	        } catch (I18NException ex) {
    	            Messages.QF_MODIFICATION_FAILED.error(getCategory(msg),ex,msg,b.toString());
    	            return;
    	        }
    	    }
    	    
    		// Convert and replay Market data events
    	    if (FIXMessageUtil.isMarketDataSnapshotFullRefresh(msg)) {
    	    	if(msg.isSetField(MDReqID.FIELD)){
    				try {
    					RequestID reqID=new RequestID(msg.getString(MDReqID.FIELD));
    					DataInfo dataInfo=getServerPersister().get(reqID);
    					if(dataInfo != null){
    						List<ResponseMessage> replies = FIXConverter.fromQMessage(msg, dataInfo.getKey().getValue());    						
    						
    						// Send replies.
					        for(ResponseMessage reply:replies){
						        Messages.QF_SENDING_REPLY.info(getCategory(msg),reply);
						        getServerManager().convertAndSend(reply, actorID);

						        // Update quote recorders
    							if(reply instanceof TickEvent){
    								for(QuoteRecorder recorder:dataInfo.getRecorders()){
    									recorder.onTick((TickEvent)reply);
    								}
    							}
    						}
    					}
    				}catch(FieldNotFound ex) {
    					Messages.QF_EVENT_FAILED.error
    		            	(getCategory(msg),ex,msg,b.toString());
    					return;
    				}
    			}
    	    	return;
    	    }
    	    
    	    // The QuickFIX/J engine is sending a reject 
    	    if (FIXMessageUtil.isReject(msg) ||
    	    		FIXMessageUtil.isMarketDataReject(msg)) {
                try {
                    String msgType=(msg.isSetField(RefMsgType.FIELD)?null:
                                    msg.getString(RefMsgType.FIELD));
                    String msgTypeName=b.getFIXDataDictionary().
                        getHumanFieldValue(MsgType.FIELD, msgType);
                    msg.setString(Text.FIELD,Messages.QF_IN_MESSAGE_REJECTED.
                                  getText(msgTypeName,msg.getString(Text.FIELD)));
                } catch (FieldNotFound ex) {
                    Messages.QF_MODIFICATION_FAILED.warn
                        (getCategory(msg),ex,msg,b.toString());
                    // Send original message instead of modified one.
                }
            }

    	    ResponseMessage reply;
            try {
                reply=FIXConverter.fromQMessage
                    (msg,originator,b.getBrokerID(),
                     Hierarchy.Flat, actorID);
            } catch (MessageCreationException ex) {
                Messages.QF_REPORT_FAILED.error
                    (getCategory(msg),ex,msg,b.toString());
                return;
            }

            // Persist and send reply.
            getServerPersister().persistMessage(reply);
            Messages.QF_SENDING_REPLY.info(getCategory(msg),reply);
            getServerManager().convertAndSend(reply, actorID);
        }
    }
    
    // Application.
    @Override
    public void onCreate(SessionID session) {
    	Messages.QF_INFO_LOGGING.info(this, "FIX Session:", session.toString());
    }

    @Override
	public void onLogon(SessionID session)
    {
    	Messages.QF_INFO_LOGGING.info(this, "FIX Login:", session.toString());
        Broker b=getBrokers().getBroker(session);
        sendToClient(b,true);
        if(b.getLogonActions() != null) {
            for(LogonAction action : b.getLogonActions()) {
                try {
                    action.onLogon(b);
                } catch (Exception e) {
                    SLF4JLoggerProxy.warn(QuickFIXApplication.class, e);
                }
            }
        }
         // fromAdmin() will forward an execution report following the
        // logon; there is no need to send a message from here.
    }

    @Override
	public void onLogout(SessionID session)
    {
    	Messages.QF_INFO_LOGGING.info(this, "FIX Logout:", session.toString());
        Broker b=getBrokers().getBroker(session);
        sendToClient(b,false);
        Collection<LogoutAction> logoutActions = b.getLogoutActions();
        if(logoutActions != null) {
            for(LogoutAction action : logoutActions) {
                try {
                    action.onLogout(b);
                } catch (Exception e) {
                    SLF4JLoggerProxy.warn(QuickFIXApplication.class, e);
                }
            }
        }
    }
	
    @Override
    public void toAdmin(Message msg, SessionID session)
    {
        Broker b=getBrokers().getBroker(session);
		//Setup the password into the Logon msg
		if (FIXMessageUtil.isLogon(msg)) {
	    	Messages.QF_INFO_LOGGING.info(this, "FIX toAdmin->SetPassword:", msg.toString());
			String propValue = b.getDescriptor().getProperty("Password");
			if(propValue != null){
				msg.setField(new Password(propValue.trim())); //setpassword
			}
			propValue = b.getDescriptor().getProperty("Username");
			if(propValue != null){
				msg.setField(new Username(propValue.trim())); //setUserName
			}
		}//OR Fix

        Messages.QF_TO_ADMIN.info(getCategory(msg),msg,b);
        b.logMessage(msg);
        
        // If the QuickFIX/J engine is sending a reject (e.g. the
        // counterparty sent us a malformed execution report, for
        // example, and we are rejecting it)
        if (FIXMessageUtil.isReject(msg)) {
            // Send message to client.
        	sendToClient(b, msg, Originator.Server);
        }
    }
	
    @Override
    public void fromAdmin(Message msg, SessionID session)
    		throws FieldNotFound, RejectLogon
    {
		messageProcessors.get(getBrokers().getBroker(session)).add(
    		new MessagePackage(msg, MessageType.FROM_ADMIN, session));
    }
	
    @Override
	public void toApp(Message msg, SessionID session)
        throws DoNotSend
    {
        Broker b=getBrokers().getBroker(session);
        Messages.QF_TO_APP.info(getCategory(msg),msg,b);
        b.logMessage(msg);
    }
	
    @Override
	public void fromApp(Message msg, SessionID session)
            throws UnsupportedMessageType, FieldNotFound
    {
        Broker b=getBrokers().getBroker(session);
        Messages.QF_FROM_APP.info(getCategory(msg),msg,b);
        b.logMessage(msg);

        // Accept only certain message types.
        if (!getSupportedMessages().isAccepted(msg)){
            Messages.QF_DISALLOWED_MESSAGE.info(getCategory(msg));
            throw new UnsupportedMessageType();
        }
        messageProcessors.get(getBrokers().getBroker(session)).add(
        		new MessagePackage(msg, MessageType.FROM_APP, session));
    }
	
    /**
     * Indicates the type of message.
     */
    private enum MessageType
    {
        FROM_ADMIN,
        FROM_APP
    }
    /**
     * Encapsulates a message to be processed.
     */
    private static class MessagePackage
    {
        /* (non-Javadoc)
         * @see java.lang.Object#hashCode()
         */
        @Override
        public int hashCode()
        {
            final int prime = 31;
            int result = 1;
            result = prime * result + (int) (id ^ (id >>> 32));
            return result;
        }
        /* (non-Javadoc)
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals(Object obj)
        {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (!(obj instanceof MessagePackage)) {
                return false;
            }
            MessagePackage other = (MessagePackage) obj;
            if (id != other.id) {
                return false;
            }
            return true;
        }
        /**
         * Create a new MessagePackage instance.
         *
         * @param inMessage a <code>Message</code> value
         * @param inMessageType a <code>MessageType</code> value
         * @param inSessionId a <code>SessionID</code> value
         */
        private MessagePackage(Message inMessage,
                               MessageType inMessageType,
                               SessionID inSessionId)
        {
            message = inMessage;
            messageType = inMessageType;
            sessionId = inSessionId;
        }
        /**
         * Gets the <code>Message</code> value.
         *
         * @return a <code>Message</code> value
         */
        private Message getMessage()
        {
            return message;
        }
        /**
         * Gets the <code>MessageType</code> value.
         *
         * @return a <code>MessageType</code> value
         */
        private MessageType getMessageType()
        {
            return messageType;
        }
        /**
         * Gets the <code>SessionID</code> value.
         *
         * @return a <code>SessionID</code> value
         */
        private SessionID getSessionId()
        {
            return sessionId;
        }
        /**
         * message value
         */
        private final Message message;
        /**
         * message type value
         */
        private final MessageType messageType;
        /**
         * session ID value
         */
        private final SessionID sessionId;
        /**
         * message counter
         */
        private final long id = counter.incrementAndGet();
        /**
         * counter used to uniquely and sequentially identify messages
         */
        private static final AtomicLong counter = new AtomicLong(0);
    }
    /**
     * Processes incoming messages.
     */
    private class MessageProcessor
            implements Runnable
    {
        /* (non-Javadoc)
         * @see java.lang.Runnable#run()
         */
        @Override
        public void run()
        {
            try {
                while(true) {
                    MessagePackage message = messagesToProcess.take();
                    SessionID session = message.getSessionId();
                    Message msg = message.getMessage();
                    Broker b = getBrokers().getBroker(session);
                    
                	// Convert reply to FIX Agnostic message (unless it's a heartbeat)
            		if(FIXMessageUtil.isHeartbeat(msg)) {
            			continue;
            		}
            		
                    // Report trading session status in a human-readable format.
                    if (FIXMessageUtil.isTradingSessionStatus(msg)) {
                        Messages.QF_TRADE_SESSION_STATUS.info(getCategory(msg),
                                                              b.getFIXDataDictionary().getHumanFieldValue(TradSesStatus.FIELD,
                                                              msg.getString(TradSesStatus.FIELD)));
                        continue;
                    }
                    
            		// Record unknown data received
                	if (FIXMessageUtil.isMarketDataIncrementalRefresh(msg)) {
                    	Messages.QF_RECEIVE_UNKNOWN_DATA.info(getCategory(msg),msg);
                    	continue;
                    }
                    
                    switch(message.getMessageType()) {
                        case FROM_APP: {
                            try {
                                // OpenFIX certification: we reject all DeliverToCompID since we don't re-deliver.
                                if (msg.getHeader().isSetField(DeliverToCompID.FIELD)) {
                                    try {
                                        Message reject = b.getFIXMessageFactory().createSessionReject(msg,
                                                                                                      SessionRejectReason.COMPID_PROBLEM);
                                        reject.setString(Text.FIELD,
                                                         Messages.QF_COMP_ID_REJECT.getText(msg.getHeader().getString(DeliverToCompID.FIELD)));
                                        b.sendToTarget(reject);
                                    } catch (SessionNotFound ex) {
                                        Messages.QF_COMP_ID_REJECT_FAILED.error(getCategory(msg),
                                                                                ex, b.toString());
                                    }
                                    break;
                                }
                                
                                // Send Trade Response
                                if (FIXMessageUtil.isExecutionReport(msg) ||
                                		FIXMessageUtil.isCancelReject(msg)) {
                                	// Record filled (partially or totally) execution reports.
                                    char ordStatus=msg.getChar(OrdStatus.FIELD);
                                    if ((ordStatus==OrdStatus.FILLED) ||
                                        (ordStatus==OrdStatus.PARTIALLY_FILLED)) {
                                    	Messages.QF_SENDING_TRADE_RECORD.info(getCategory(msg),msg);
                                    }
                                    // Send message to client.
                                    sendToClient(b, msg, Originator.Broker);
                                    break;
                                }
                             
                                // Send Data Response
                                if(FIXMessageUtil.isMarketDataSnapshotFullRefresh(msg) ||
                                		FIXMessageUtil.isMarketDataReject(msg)){
                                	// Send message to client.
                                    sendToClient(b, msg, Originator.Broker);
                                    break;
                                }
 
                                if(FIXMessageUtil.isXMLMessage(msg)){
                                	// Send message to Job managers
	                                ServerFIXRouting.getInstance().getJobSession()
	                                	.convertAndSend(msg);
	                                break;
                                }
                                
                                Messages.QF_RECEIVE_UNKNOWN_DATA.error(this, msg);
                                break;
                            } catch (FieldNotFound e) {
                                SLF4JLoggerProxy.error(QuickFIXApplication.class, e);
                            }
                            break;
                        }
                        case FROM_ADMIN: {
                            Messages.QF_FROM_ADMIN.info(getCategory(msg), msg, b);
                            b.logMessage(msg);
                            
                            // Send message to client.
                            sendToClient(b, msg, Originator.Broker);
                            break;
                        }
                        default:
                            throw new UnsupportedOperationException();
                    }
                }
            } catch (Exception ignored) {}
        }
        /**
         * Create a new MessageProcessor instance.
         */
        private MessageProcessor(Broker b)
        {
        	messagesToProcess = new LinkedBlockingDeque<>();
            thread = new Thread(this, "QFJ Broker: " + b.getName()); //$NON-NLS-1$
            thread.start();
        }
        /**
         * Add the message to the queue.
         */
        private void add(MessagePackage message){
        	messagesToProcess.add(message);
        }
        /**
         * thread on which the messages are processed
         */
        private final Thread thread;
        /**
         * the list of messages to be processed
         */
        private final BlockingDeque<MessagePackage> messagesToProcess;
    }
}
