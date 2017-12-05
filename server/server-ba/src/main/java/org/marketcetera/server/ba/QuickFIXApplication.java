package org.marketcetera.server.ba;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicLong;

import org.marketcetera.server.ba.brokers.Broker;
import org.marketcetera.server.ba.brokers.Brokers;
import org.marketcetera.server.ba.brokers.LogonAction;
import org.marketcetera.server.ba.brokers.LogoutAction;
import org.marketcetera.info.*;
import org.marketcetera.quickfix.FIXMessageUtil;
import org.marketcetera.ws.server.filters.MessageFilter;
import org.marketcetera.util.log.SLF4JLoggerProxy;
import org.marketcetera.util.misc.ClassVersion;

import quickfix.*;
import quickfix.field.*;

/**
 * The QuickFIX/J intermediary, intercepting messages from/to the
 * QuickFIX/J counterparts and the Server.
 */
@ClassVersion("$Id: QuickFIXApplication.java 16639 2013-07-23 01:36:32Z colin $")
public class QuickFIXApplication
    implements Application
{	
    // CLASS DATA
    private static final String HEARTBEAT_CATEGORY=
    		QuickFIXApplication.class.getName()+".HEARTBEATS"; //$NON-NLS-1$
    
    // INSTANCE DATA.
    private final SystemInfo mSystemInfo;
    private final Brokers mBrokers;
    private final MessageFilter mSupportedMessages;
    private final Map<Broker, MessageProcessor> messageProcessors = new HashMap<>();

    // CONSTRUCTORS.
    public QuickFIXApplication
        (SystemInfo systemInfo,
         Brokers brokers,
         MessageFilter supportedMessages)
    {
        mSystemInfo=systemInfo;
        mBrokers=brokers;
        mSupportedMessages=supportedMessages;
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

    private Object getCategory
        (Message msg)
    {
        if (FIXMessageUtil.isHeartbeat(msg)) {
            return HEARTBEAT_CATEGORY;
        }
        return this;
    }
    
    private void updateStatus
    (Broker b, boolean status)
    {
	    if (b.getLoggedOn()==status) {
	        return;
	    }
	    Messages.QF_SENDING_STATUS.info(this,status,b);
	    b.setLoggedOn(status);
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
        updateStatus(b,true);
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
        updateStatus(b,true);
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
        Messages.QF_TO_ADMIN.info(getCategory(msg),msg,b);
        b.logMessage(msg);
    }
	
    @Override
    public void fromAdmin(Message msg, SessionID session)
    		throws RejectLogon
    {
    	if (FIXMessageUtil.isLogon(msg)) {
    		Broker b=getBrokers().getBroker(session);
	    	Messages.QF_INFO_LOGGING.info(this, "FIX fromAdmin->CheckPassword:", msg.toString());
			
	    	String username = null, password = null;
			try {
	    		username = msg.getString(Password.FIELD);
	    		password = msg.getString(Username.FIELD);
			} catch (FieldNotFound e) {}
			
			if (!b.getDescriptor().getProperty("Password").equals(username) ||
					!b.getDescriptor().getProperty("Username").equals(password)) {
				throw new RejectLogon(Messages.ERROR_LOGON_USERPASSWORD.getText(
						username, password));
			}
			
			if(!b.getClientKernel().isRunning()){
				throw new RejectLogon(Messages.CLIENT_ERROR_CONNECTION.getText());
			}
    	}
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
                                
                                // FIX Send Requests
                                if (FIXMessageUtil.isOrderSingle(msg) ||
                                		FIXMessageUtil.isCancelReplaceRequest(msg) ||
                                		FIXMessageUtil.isCancelRequest(msg) ||
                                		FIXMessageUtil.isMarketDataRequest(msg)) {
                                    // Send message to broker API.
                                    b.receiveMessage(msg, session);
                                    break;
                                }
                                
                                // API Send Requests
                                if(FIXMessageUtil.isXMLMessage(msg)){
                                    // Send message to broker API.
                                    b.receiveMessage(msg, session);
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
                            
                            // Send message to broker API.
                            b.receiveMessage(msg, session);
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
