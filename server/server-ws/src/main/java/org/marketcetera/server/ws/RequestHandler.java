package org.marketcetera.server.ws;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import org.apache.commons.lang.ObjectUtils;
import org.marketcetera.brokers.algo.BrokerAlgo;
import org.marketcetera.brokers.algo.BrokerAlgoSpec;
import org.marketcetera.client.ws.jms.RequestEnvelope;
import org.marketcetera.client.ws.jms.MessageHandler;
import org.marketcetera.core.CoreException;
import org.marketcetera.core.IDFactory;
import org.marketcetera.metrics.ConditionsFactory;
import org.marketcetera.metrics.ThreadedMetric;
import org.marketcetera.server.ws.brokers.Broker;
import org.marketcetera.server.ws.brokers.Brokers;
import org.marketcetera.ws.server.filters.BasicFilter;
import org.marketcetera.info.RequestInfo;
import org.marketcetera.info.RequestInfoImpl;
import org.marketcetera.info.SessionInfo;
import org.marketcetera.marketdata.GenericDataRequest;
import org.marketcetera.marketdata.MarketDataCancel;
import org.marketcetera.marketdata.MarketDataRequest;
import org.marketcetera.ws.server.security.SimpleUser;
import org.marketcetera.quickfix.FIXMessageFactory;
import org.marketcetera.quickfix.FIXMessageUtil;
import org.marketcetera.quickfix.FIXVersion;
import org.marketcetera.trade.*;
import org.marketcetera.util.except.I18NException;
import org.marketcetera.util.log.I18NBoundMessage1P;
import org.marketcetera.util.log.I18NBoundMessage2P;
import org.marketcetera.util.misc.ClassVersion;
import org.marketcetera.ws.RequestMessage;
import org.marketcetera.ws.ResponseMessage;

import quickfix.*;
import quickfix.field.*;
import quickfix.field.OrderID;

/**
 * A handler for incoming trade/data requests.
 */
@ClassVersion("$Id: RequestHandler.java 16752 2013-11-14 02:54:13Z colin $")
public class RequestHandler 
    implements MessageHandler<RequestEnvelope>
{
    // CLASS DATA.
    private static final String SELF_SENDER_COMP_ID=
        "Server"; //$NON-NLS-1$
    private static final String SELF_TARGET_COMP_ID=
        "Client"; //$NON-NLS-1$
    private static final String SELF_ORDER_ID=
        "NONE"; //$NON-NLS-1$
    private static final String UNKNOWN_EXEC_ID=
        "ERROR"; //$NON-NLS-1$
    private static final char SOH=
        '\u0001';
    private static final char SOH_REPLACE=
        '|';
    private static final Callable<Boolean> METRIC_CONDITION_RH=
        ConditionsFactory.createSamplingCondition
        (100,"metc.metrics.server.rh.sampling.interval"); //$NON-NLS-1$
    /**
     * singleton instance reference
     */
    private static RequestHandler mIinstance;
    
    // INSTANCE DATA.
    private final Brokers mBrokers;
    private final List<BasicFilter> mAllowedRequests;
    private final ServerPersister mServerPersister;
    private final ServerManager mServerManager;
    private final IDFactory mIDFactory;
    private final DataDictionary mDataDictionary;

    // CONSTRUCTORS.
    public RequestHandler
        (Brokers brokers,
         List<BasicFilter> allowedRequests,
         ServerPersister serverPersister,
         ServerManager serverManager,
         IDFactory idFactory)
        throws ConfigError
    {
    	mIinstance = this;
        
        mBrokers=brokers;
        mAllowedRequests=allowedRequests;
        mServerPersister=serverPersister;
        mServerManager=serverManager;
        mIDFactory=idFactory;
        mDataDictionary=new DataDictionary
            (FIXVersion.FIX_SYSTEM.getDataDictionaryURL());
    }

    // INSTANCE METHODS.
    /**
     * Gets the <code>ServerFIXRouting</code> value.
     *
     * @return an <code>ServerFIXRouting</code> value
     */
    public static RequestHandler getInstance()
    {
        return mIinstance;
    }
    
    /**
     * Gets the <code>Brokers</code> value.
     *
     * @return an <code>Brokers</code> value
     */
    private Brokers getBrokers()
    {
        return mBrokers;
    }
    
    /**
     * Gets the <code>QuickFIXSender</code> value.
     *
     * @return an <code>QuickFIXSender</code> value
     */
    private List<BasicFilter> getAllowedRequests()
    {
        return mAllowedRequests;
    }

    /**
     * Gets the <code>ServerPersister</code> value.
     *
     * @return an <code>ServerPersister</code> value
     */
    private ServerPersister getServerPersister()
    {
        return mServerPersister;
    }
    
    /**
     * Gets the <code>ServerManager</code> value.
     *
     * @return an <code>ServerManager</code> value
     */
    private ServerManager getServerManager()
    {
        return mServerManager;
    }
    
    /**
     * Gets the <code>IDFactory</code> value.
     *
     * @return an <code>IDFactory</code> value
     */
    private IDFactory getIDFactory()
    {
        return mIDFactory;
    }
    
    /**
     * Gets the <code>FIXMessageFactory</code> value.
     *
     * @return an <code>FIXMessageFactory</code> value
     */
    private FIXMessageFactory getMsgFactory()
    {
        return FIXVersion.FIX_SYSTEM.getMessageFactory();
    }
    
    /**
     * Gets the <code>DataDictionary</code> value.
     *
     * @return an <code>DataDictionary</code> value
     */
    private DataDictionary getDataDictionary()
    {
        return mDataDictionary;
    }

    /**
     * Returns the best message factory available: this is either the
     * system factory, if the given broker is null, or the broker's
     * factory otherwise.
     *
     * @param b The broker.
     *
     * @return The factory.
     */
    private FIXMessageFactory getBestMsgFactory
        (Broker b)
    {
        if (b==null) {
            return getMsgFactory();
        }
        return b.getFIXMessageFactory();
    }

    /**
     * Returns the best data dictionary available: this is either the
     * system dictionary, if the given broker is null, or the broker's
     * dictionary otherwise.
     *
     * @param b The broker. It may be null.
     *
     * @return The data dictionary.
     */
    private DataDictionary getBestDataDictionary
        (Broker b)
    {
        if (b==null) {
            return getDataDictionary();
        }
        return b.getDataDictionary();
    }

    /**
     * Gets the next ExecID
     */
    private ExecID getNextExecId()
        throws CoreException
    {
        return new ExecID(getIDFactory().getNext());
    }
    
    /**
     * Adds the required fields
     */
    private static void addRequiredFields
        (Message msg)
    {
        msg.getHeader().setField(new MsgSeqNum(0));
        msg.getHeader().setField(new SenderCompID(SELF_SENDER_COMP_ID));
        msg.getHeader().setField(new TargetCompID(SELF_TARGET_COMP_ID));
        msg.getHeader().setField(new SendingTime(new Date()));

        // This indirectly adds body length and checksum.
        msg.toString();
    }

    /**
     * Creates a QuickFIX/J rejection if processing of the given
     * message, associated with the given broker, failed with the
     * given exception.
     *
     * @param ex The exception.
     * @param b The broker. It may be null.
     * @param msg The message, in FIX Agnostic form. It may be null.
     *
     * @return The rejection.
     */
    private Message createRejection
        (I18NException ex, Broker b, Order msg)
    {
        // Special handling of unsupported incoming messages.
        if (ex.getI18NBoundMessage()==Messages.RH_UNSUPPORTED_MESSAGE) {
            return getBestMsgFactory(b).newBusinessMessageReject
                (msg.getClass().getName(),
                 BusinessRejectReason.UNSUPPORTED_MESSAGE_TYPE,
                 ex.getLocalizedDetail().replace(SOH,SOH_REPLACE));
        }

        // Attempt conversion of incoming message into a QuickFIX/J
        // message.
        Message qMsg=null;
        try {
            qMsg=FIXConverter.toQMessage
                (getBestMsgFactory(b),getBestDataDictionary(b),msg);
        } catch (I18NException ex2) {
            Messages.RH_REJ_CONVERSION_FAILED.warn(this,ex2,msg);
        }

        // Create basic rejection shell.
        Message qMsgReply;
        boolean orderCancelType=
            (FIXMessageUtil.isCancelRequest(qMsg) ||
             FIXMessageUtil.isCancelReplaceRequest(qMsg));
        if (orderCancelType) {
            qMsgReply=getBestMsgFactory(b).newOrderCancelRejectEmpty();
            char reason;
            if (FIXMessageUtil.isCancelRequest(qMsg)) {
                reason=CxlRejResponseTo.ORDER_CANCEL_REQUEST;
            } else {
                reason=CxlRejResponseTo.ORDER_CANCEL_REPLACE_REQUEST;
            }
            qMsgReply.setField(new CxlRejResponseTo(reason));
        } else {
            qMsgReply=getBestMsgFactory(b).newExecutionReportEmpty();
            try {
                qMsgReply.setField(getNextExecId());
            } catch (CoreException ex2) {
                Messages.RH_REJ_ID_GENERATION_FAILED.warn(this,ex2);
                qMsgReply.setField(new ExecID(UNKNOWN_EXEC_ID));
            }
            qMsgReply.setField(new AvgPx(0));
            qMsgReply.setField(new CumQty(0));
            qMsgReply.setField(new LastShares(0));
            qMsgReply.setField(new LastPx(0));
            qMsgReply.setField(new ExecTransType(ExecTransType.STATUS));
        }
        qMsgReply.setField(new OrdStatus(OrdStatus.REJECTED));
        qMsgReply.setString(Text.FIELD,ex.getLocalizedDetail().replace
                            (SOH,SOH_REPLACE));

        // Add all the fields of the incoming message.
        if (qMsg!=null) {
            FIXMessageUtil.fillFieldsFromExistingMessage
                (qMsgReply,qMsg,getBestDataDictionary(b),false);
        }

        // Add an order ID, if there was none from the incoming message.
        if (!qMsgReply.isSetField(OrderID.FIELD)) {
            qMsgReply.setField(new OrderID(SELF_ORDER_ID));
        }

        // Augment rejection.
        if (!orderCancelType) {
            try {
                getBestMsgFactory(b).getMsgAugmentor().executionReportAugment
                    (qMsgReply);
            } catch (FieldNotFound ex2) {
                Messages.RH_REJ_AUGMENTATION_FAILED.warn(this,ex2,qMsgReply);
            }
        }

        // Add required header/trailer fields.
        addRequiredFields(qMsgReply);
        return qMsgReply;
    }
    
    /**
     * Creates a QuickFIX/J rejection if processing of the given
     * message, associated with the given broker, failed with the
     * given exception.
     *
     * @param ex The exception.
     * @param b The broker. It may be null.
     * @param msg The message, in FIX Agnostic form. It may be null.
     *
     * @return The rejection.
     */
    private Message createRejection
        (I18NException ex,
         Broker b, GenericDataRequest msg)
    {
        // Special handling of unsupported incoming messages.
        if (ex.getI18NBoundMessage()==Messages.RH_UNSUPPORTED_MESSAGE) {
            return getBestMsgFactory(b).newBusinessMessageReject
                (msg.getClass().getName(),
                 BusinessRejectReason.UNSUPPORTED_MESSAGE_TYPE,
                 ex.getLocalizedDetail().replace(SOH,SOH_REPLACE));
        }

        // Attempt conversion of incoming message into a QuickFIX/J
        // message.
        Message qMsg=null;
        try {
            qMsg=FIXConverter.toQMessage
                (getBestMsgFactory(b),getBestDataDictionary(b),msg);
        } catch (I18NException ex2) {
           
        	Messages.RH_REJ_CONVERSION_FAILED.warn(this,ex2,msg);
        }

        // Create basic rejection shell.
        Message qMsgReply;
        qMsgReply=getBestMsgFactory(b).newMarketDataRejectEmpty();
        qMsgReply.setString(Text.FIELD,ex.getLocalizedDetail().replace
                            (SOH,SOH_REPLACE));

        // Add all the fields of the incoming message.
        if (qMsg!=null) {
            FIXMessageUtil.fillFieldsFromExistingMessage
                (qMsgReply,qMsg,getBestDataDictionary(b),false);
        }

        // Add required header/trailer fields.
        addRequiredFields(qMsgReply);
        return qMsgReply;
    }
    
    // ReplyHandler.
    @Override
    public void receiveMessage(RequestEnvelope msgEnv)
    {
        Messages.RH_RECEIVED_MESSAGE.info(this,msgEnv);
        RequestMessage msg=null;
        
        // Reject null message envelopes.
        if (msgEnv==null) {
            throw new I18NException(Messages.RH_NULL_MESSAGE_ENVELOPE);
        }
        ThreadedMetric.begin(msgEnv.getRequest());
        
        // Reject null messages.
        msg=msgEnv.getRequest();
        if (msg==null) {
            throw new I18NException(Messages.RH_NULL_MESSAGE);
        }
        
        // Reject invalid sessions.
        SessionInfo sessionInfo=
        		getServerManager().getSessionInfo(msgEnv.getSessionId());
    	if (sessionInfo==null) {
            throw new I18NException
                (new I18NBoundMessage1P
                 (Messages.RH_SESSION_EXPIRED,msgEnv.getSessionId()));
        }
    	
        // Reject messages of unsupported types.
        if ((msg instanceof OrderSingle) ||
            (msg instanceof OrderCancel) ||                
            (msg instanceof OrderReplace) ||                
            (msg instanceof FIXOrder)) {
        	Order tMsg=(Order)msg;
        	receiveMessage(tMsg, sessionInfo);
        } else if ((msg instanceof MarketDataRequest) ||
            (msg instanceof MarketDataCancel)) {
        	GenericDataRequest dMsg = (GenericDataRequest)msg;
        	receiveMessage(dMsg, sessionInfo);
        } else{
        	throw new I18NException(Messages.RH_UNSUPPORTED_MESSAGE);
        }
	}
    
    // Handler for trade session.
    public void receiveMessage(Order oMsg, SessionInfo sessionInfo)
    {
        RequestMessage msg=null;
        UserID actorID=null;
        BrokerID bID=null;
        Broker b=null;
        Message qMsg=null;
        Message qMsgToSend=null;
        boolean responseExpected=false;
        TradeInfo tradeInfo=null;
        try {
            if (sessionInfo==null) {
                throw new I18NException(Messages.RH_NULL_SESSION_INFO);
            }
            actorID=(UserID)sessionInfo.getValue(SessionInfo.ACTOR_ID);
            RequestInfo requestInfo=new RequestInfoImpl(sessionInfo);
            ThreadedMetric.event
                ("requestHandler.sessionInfoObtained"); //$NON-NLS-1$

            // Identify broker.
            bID=oMsg.getBrokerID();
            if (bID==null) {
                throw new I18NException(Messages.RH_UNKNOWN_BROKER);
            }
            requestInfo.setValue
                (RequestInfo.BROKER_ID,bID);
            
            // Ensure broker ID maps to existing broker.
            b=getBrokers().getTradeBroker(bID);
            if (b==null) {
                throw new I18NException(Messages.RH_UNKNOWN_BROKER_ID);
            }
            requestInfo.setValue
                (RequestInfo.BROKER,b);
            requestInfo.setValue
                (RequestInfo.FIX_MESSAGE_FACTORY,b.getFIXMessageFactory());
            ThreadedMetric.event
                ("requestHandler.brokerSelected"); //$NON-NLS-1$
            
            // apply broker algos, if available
            applyBrokerAlgos(oMsg, b);
            
            // Convert to a QuickFIX/J message.
            try {
                qMsg=FIXConverter.toQMessage
                    (b.getFIXMessageFactory(),b.getDataDictionary(),oMsg);
            } catch (I18NException ex) {
                throw new I18NException(ex,Messages.RH_CONVERSION_FAILED);
            }
            tradeInfo=getServerPersister().put(qMsg,actorID);
            b.logMessage(qMsg);
            ThreadedMetric.event
                ("requestHandler.tradeConverted"); //$NON-NLS-1$
            
            // Ensure broker is allowed for this user
            final SimpleUser actor = (SimpleUser)sessionInfo.getValue(SessionInfo.ACTOR);
            if(!b.isUserAllowed(actor.getName())) {
                throw new I18NException(Messages.RH_UNKNOWN_BROKER_ID);
            }
            
            // Ensure broker is available.
            if (!b.getLoggedOn()) {
                throw new I18NException(Messages.RH_UNAVAILABLE_BROKER);
            }

            // Ensure the order is allowed.
            if(getAllowedRequests() != null) {
                for(BasicFilter basicFilter : getAllowedRequests()) {
                    if(!basicFilter.isAccepted(new BasicFilter.MessageInfo() {
                        @Override
                        public SimpleUser getUser()
                        {
                            return actor;
                        }
                    },qMsg)) {
                        throw new I18NException(Messages.RH_REQUEST_DISALLOWED);
                    }
                }
            }
            ThreadedMetric.event
                ("requestHandler.tradeAllowed"); //$NON-NLS-1$

            // Apply message modifiers.
            if (b.getModifiers()!=null) {
                requestInfo.setValue(RequestInfo.CURRENT_MESSAGE,qMsg);
                try {
                    b.getModifiers().modifyMessage(requestInfo);
                } catch (I18NException ex) {
                    throw new I18NException(ex,Messages.RH_MODIFICATION_FAILED);
                }
                qMsg=requestInfo.getValueIfInstanceOf
                    (RequestInfo.CURRENT_MESSAGE,Message.class);
            }
            ThreadedMetric.event
                ("requestHandler.modifiersApplied"); //$NON-NLS-1$

            // Apply trade routing.
            if (b.getRoutes()!=null) {
                try {
                    b.getRoutes().modifyMessage
                        (qMsg,b.getFIXMessageAugmentor());
                } catch (I18NException ex) {
                    throw new I18NException(ex,Messages.RH_ROUTING_FAILED);
                }
            }
            ThreadedMetric.event
                ("requestHandler.tradeRoutingApplied"); //$NON-NLS-1$

            // Apply pre-sending message modifiers.
            if (b.getPreSendModifiers()!=null) {
                qMsgToSend=(Message)qMsg.clone();
                requestInfo.setValue(RequestInfo.CURRENT_MESSAGE,qMsgToSend);
                try {
                    try {
                        b.getPreSendModifiers().modifyMessage(requestInfo);
                    } catch (I18NException ex) {
                        throw new I18NException
                            (ex,Messages.RH_PRE_SEND_MODIFICATION_FAILED);
                    }
                    qMsgToSend=requestInfo.getValueIfInstanceOf
                        (RequestInfo.CURRENT_MESSAGE,Message.class);
                } finally {
                    requestInfo.setValue(RequestInfo.CURRENT_MESSAGE,qMsg);
                }
            } else {
                qMsgToSend=qMsg;
            }
            ThreadedMetric.event
                ("requestHandler.preSendModifiersApplied"); //$NON-NLS-1$

            // Send message to Broker.
            try {
            	b.sendToTarget(qMsgToSend);
            } catch (SessionNotFound ex) {
                throw new I18NException(ex,Messages.RH_UNAVAILABLE_BROKER);
            }
            responseExpected=true;
            ThreadedMetric.event
                ("requestHandler.tradeSent"); //$NON-NLS-1$
        } catch (I18NException ex) {
            Messages.RH_MESSAGE_PROCESSING_FAILED.error(this,ex,msg,qMsg,qMsgToSend,ObjectUtils.toString(b,ObjectUtils.toString(bID)));
            if(b!= null && oMsg != null && actorID != null){
	            Message qMsgReply = createRejection(ex, b, oMsg);	            	
	            try {
	            	ResponseMessage reply=FIXConverter.fromQMessage(qMsgReply,
	                                                Originator.Server,
	                                                bID,
	                                                Hierarchy.Flat,
	                                                actorID);
		            getServerPersister().persistMessage(reply);
		            Messages.RH_SENDING_REPLY.info(this, reply);
		            getServerManager().convertAndSend(reply, actorID);
	            } catch (MessageCreationException ex2) {
	                Messages.RH_REPORT_FAILED.error(this,
	                                                ex2, qMsgReply);
	                return;
	            }
            }
        } finally {
            if (tradeInfo!=null) {
                tradeInfo.setResponseExpected(responseExpected);
            }
        }
        ThreadedMetric.end(METRIC_CONDITION_RH);
	}
    
    // Handler for data session.
    public void receiveMessage(GenericDataRequest oMsg, SessionInfo sessionInfo)
    {
        RequestMessage msg=null;
        UserID actorID=null;
        BrokerID bID=null;
        Broker b=null;
        Message qMsg=null;
        Message qMsgToSend=null;
        DataInfo dataInfo=null;
        try {
            if (sessionInfo==null) {
                throw new I18NException(Messages.RH_NULL_SESSION_INFO);
            }
            actorID=(UserID)sessionInfo.getValue(SessionInfo.ACTOR_ID);
            RequestInfo requestInfo=new RequestInfoImpl(sessionInfo);
            ThreadedMetric.event
                ("requestHandler.sessionInfoObtained"); //$NON-NLS-1$

            // Identify broker.
            bID=oMsg.getBrokerID();
            if (bID==null) {
                throw new I18NException(Messages.RH_UNKNOWN_BROKER);
            }
            requestInfo.setValue
                (RequestInfo.BROKER_ID,bID);
            
            // Ensure broker ID maps to existing broker.
            b=getBrokers().getDataBroker(bID);
            if (b==null) {
                throw new I18NException(Messages.RH_UNKNOWN_BROKER_ID);
            }
            requestInfo.setValue
                (RequestInfo.BROKER,b);
            requestInfo.setValue
                (RequestInfo.FIX_MESSAGE_FACTORY,b.getFIXMessageFactory());
            ThreadedMetric.event
                ("requestHandler.brokerSelected"); //$NON-NLS-1$
            
            //Global request management
            List<GenericDataRequest> sMsgs = getServerPersister().put(oMsg, sessionInfo);

            for(GenericDataRequest sMsg:sMsgs){
	            // apply broker algos, if available
	            applyBrokerAlgos(sMsg, b);
	            
	            // Convert to a QuickFIX/J message.
	            try {
	                qMsg=FIXConverter.toQMessage
	                    (b.getFIXMessageFactory(),b.getDataDictionary(),sMsg);
	            } catch (I18NException ex) {
	                throw new I18NException(ex,Messages.RH_CONVERSION_FAILED);
	            }
	            b.logMessage(qMsg);
	            ThreadedMetric.event
	                ("requestHandler.dataConverted"); //$NON-NLS-1$
	            
	            // Persist the message in cache
	            if(sMsg instanceof MarketDataRequest){
	            	dataInfo = getServerPersister().get(sMsg.getRequestID());
	            	if(dataInfo != null){
	            		dataInfo.setMessage(qMsg);
	            	}
	            }
	            
	            // Ensure broker is allowed for this user
	            final SimpleUser actor = (SimpleUser)sessionInfo.getValue(SessionInfo.ACTOR);
	            if(!b.isUserAllowed(actor.getName())) {
	                throw new I18NException(Messages.RH_UNKNOWN_BROKER_ID);
	            }
	            
	            // Ensure broker is available.
	            if (!b.getLoggedOn()) {
	                throw new I18NException(Messages.RH_UNAVAILABLE_BROKER);//TODO
	            }
	
	            // Ensure the data is allowed.
	            if(getAllowedRequests() != null) {
	                for(BasicFilter basicFilter : getAllowedRequests()) {
	                    if(!basicFilter.isAccepted(new BasicFilter.MessageInfo() {
	                        @Override
	                        public SimpleUser getUser()
	                        {
	                            return actor;
	                        }
	                    },qMsg)) {
	                        throw new I18NException(Messages.RH_REQUEST_DISALLOWED);
	                    }
	                }
	            }
	            ThreadedMetric.event
	                ("requestHandler.dataAllowed"); //$NON-NLS-1$
	
	            // Apply message modifiers.
	            if (b.getModifiers()!=null) {
	                requestInfo.setValue(RequestInfo.CURRENT_MESSAGE,qMsg);
	                try {
	                    b.getModifiers().modifyMessage(requestInfo);
	                } catch (I18NException ex) {
	                    throw new I18NException(ex,Messages.RH_MODIFICATION_FAILED);
	                }
	                qMsg=requestInfo.getValueIfInstanceOf
	                    (RequestInfo.CURRENT_MESSAGE,Message.class);
	            }
	            ThreadedMetric.event
	                ("requestHandler.modifiersApplied"); //$NON-NLS-1$
	
	            // Apply data routing.
	            if (b.getRoutes()!=null) {
	                try {
	                    b.getRoutes().modifyMessage
	                        (qMsg,b.getFIXMessageAugmentor());
	                } catch (I18NException ex) {
	                    throw new I18NException(ex,Messages.RH_ROUTING_FAILED);
	                }
	            }
	            ThreadedMetric.event
	                ("requestHandler.dataRoutingApplied"); //$NON-NLS-1$
	
	            // Apply pre-sending message modifiers.
	            if (b.getPreSendModifiers()!=null) {
	                qMsgToSend=(Message)qMsg.clone();
	                requestInfo.setValue(RequestInfo.CURRENT_MESSAGE,qMsgToSend);
	                try {
	                    try {
	                        b.getPreSendModifiers().modifyMessage(requestInfo);
	                    } catch (I18NException ex) {
	                        throw new I18NException
	                            (ex,Messages.RH_PRE_SEND_MODIFICATION_FAILED);
	                    }
	                    qMsgToSend=requestInfo.getValueIfInstanceOf
	                        (RequestInfo.CURRENT_MESSAGE,Message.class);
	                } finally {
	                    requestInfo.setValue(RequestInfo.CURRENT_MESSAGE,qMsg);
	                }
	            } else {
	                qMsgToSend=qMsg;
	            }
	            ThreadedMetric.event
	                ("requestHandler.preSendModifiersApplied"); //$NON-NLS-1$
	
	            // Send message to Broker.
	            try {
	            	b.sendToTarget(qMsgToSend);
	            } catch (SessionNotFound ex) {
	                throw new I18NException(ex,Messages.RH_UNAVAILABLE_BROKER);
	            }
	            ThreadedMetric.event
	                ("requestHandler.dataSent"); //$NON-NLS-1$
            }
        } catch (I18NException ex) {
            Messages.RH_MESSAGE_PROCESSING_FAILED.error(this,ex,msg,qMsg,qMsgToSend,ObjectUtils.toString(b,ObjectUtils.toString(bID)));
            if(b!= null && oMsg != null && actorID != null){
	            Message qMsgReply = createRejection(ex, b, oMsg);	            	
	            try {
	            	ResponseMessage reply=FIXConverter.fromQMessage(qMsgReply,
	                                                Originator.Server,
	                                                bID,
	                                                Hierarchy.Flat,
	                                                actorID);
		            getServerPersister().persistMessage(reply);
		            Messages.RH_SENDING_REPLY.info(this, reply);
		            getServerManager().convertAndSend(reply, actorID);
	            } catch (MessageCreationException ex2) {
	                Messages.RH_REPORT_FAILED.error(this,
	                                                ex2, qMsgReply);
	                return;
	            }
            }
        }
        ThreadedMetric.end(METRIC_CONDITION_RH);
	}
    
    /**
     * Validates and applies the tags associated with the broker algos on the order, if any.
     *
     * @param inRequest an <code>RequestMessage</code> value
     * @param inBroker a <code>Broker</code> value
     * @throws CoreException if an algo validation fails
     */
    private void applyBrokerAlgos(RequestMessage inRequest,
                                  Broker inBroker)
    {
        if(inRequest instanceof NewOrReplaceOrder) {
            NewOrReplaceOrder algoOrder = (NewOrReplaceOrder)inRequest;
            BrokerAlgo algo = algoOrder.getBrokerAlgo();
            if(algo != null) {
                // find the cannonical (from-the-config) algo specs - these will have validators, if any
                Map<String,BrokerAlgoSpec> cannonicalAlgos = inBroker.getBrokerAlgosAsMap();
                // retrieve the cannonical spec for this algo
                BrokerAlgoSpec cannonicalAlgoSpec = cannonicalAlgos.get(algo.getAlgoSpec().getName());
                if(cannonicalAlgoSpec == null) {
                    throw new CoreException(new I18NBoundMessage2P(Messages.RH_NO_BROKER_ALGO,
                                                                   algo.getAlgoSpec().getName(),
                                                                   inBroker.getBrokerID()));
                }
                // validators specified in config exist in the algo specs available from the broker, but not on the bound
                //  algo (or algo tags) themselves so map the validators back on the bound algos
                algo.mapValidatorsFrom(cannonicalAlgoSpec);
                // now, validation can be performed using the bound algo
                algo.validate();
                // apply the tags on the order (maps to custom fields)
                algo.applyTo(algoOrder);
            }
        }
    }
}
