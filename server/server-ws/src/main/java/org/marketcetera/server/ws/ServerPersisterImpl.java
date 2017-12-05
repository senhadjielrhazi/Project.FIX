package org.marketcetera.server.ws;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.commons.lang.Validate;
import org.marketcetera.core.IDFactory;
import org.marketcetera.core.InMemoryIDFactory;
import org.marketcetera.core.time.Period;
import org.marketcetera.event.QuoteEvent;
import org.marketcetera.info.SessionInfo;
import org.marketcetera.marketdata.Content;
import org.marketcetera.marketdata.DataReferenceKey;
import org.marketcetera.marketdata.GenericDataRequest;
import org.marketcetera.marketdata.MarketDataCancel;
import org.marketcetera.marketdata.MarketDataRequest;
import org.marketcetera.marketdata.MarketDataRequestBuilder;
import org.marketcetera.marketdata.MarketDepth;
import org.marketcetera.module.RequestID;
import org.marketcetera.persist.PersistenceException;
import org.marketcetera.server.ws.history.HistoryService;
import org.marketcetera.server.ws.security.ClientSession;
import org.marketcetera.trade.BrokerID;
import org.marketcetera.trade.OrderID;
import org.marketcetera.trade.ReportBase;
import org.marketcetera.trade.UserID;
import org.marketcetera.util.log.SLF4JLoggerProxy;
import org.marketcetera.ws.ResponseMessage;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import quickfix.FieldNotFound;
import quickfix.Message;
import quickfix.field.ClOrdID;
import quickfix.field.MDReqID;
import quickfix.field.OrigClOrdID;

public class ServerPersisterImpl implements ServerPersister {

	// INSTANCE DATA.
	/**
	 * The history service
	 */
    private final HistoryService mHistoryService; 
    /**
     * Mapping the client order reference to a trade info. 
     */
    private final Map<OrderID,TradeInfo> mOrderMap=Maps.newConcurrentMap();    
    /**
     * Mapping the market reference to a data info. 
     */
    private final Map<DataReferenceKey,DataInfo> mKeyMap=Maps.newConcurrentMap();
    /**
     * Mapping unique broker request to a data info. 
     */
    private final Map<RequestID,DataInfo> mReqIDMap=Maps.newConcurrentMap();
    /**
     * Mapping session info to data info set. 
     */
    private final Map<SessionInfo,Set<DataInfo>> mSessionMap=Maps.newConcurrentMap();
    /**
     * Implementation of IDFactory that provides identifiers unique to broker requests. 
     */
    public static final IDFactory mIDFactory=new InMemoryIDFactory(0, "sreq");
    
    // CONSTRUCTORS.
    /**
     * Creates a new persister which relies on the given report
     * history services provider for persistence operations.
     *
     * @param historyService The history service provider.
     */    
    public ServerPersisterImpl
        (HistoryService historyService)
    {
        mHistoryService=historyService;
    }
 
    // INSTANCE METHODS.
    /**
     * Returns the receiver's history service provider.
     *
     * @return The provider.
     */
    private HistoryService getHistoryService()
    {
        return mHistoryService;
    }
    
    /**
     * Returns the receiver's in-memory map of order ID to order
     * information.
     *
     * @return The map.
     */
    private Map<OrderID, TradeInfo> getOrderMap()
    {
        return mOrderMap;
    }

    /**
     * Returns the receiver's in-memory map of key to data info.
     *
     * @return The map.
     */
    private Map<DataReferenceKey, DataInfo> getKeyMap()
    {
        return mKeyMap;
    }
    
    /**
     * Returns the receiver's in-memory map of server request ID to data info.
     *
     * @return The map.
     */
    private Map<RequestID, DataInfo> getReqIDMap()
    {
        return mReqIDMap;
    }
    
    /**
     * Returns the receiver's in-memory map of session info data info set.
     *
     * @return The map.
     */
    private Map<SessionInfo, Set<DataInfo>> getSessionMap()
    {
        return mSessionMap;
    }
    
    /**
     * Gets the next unique request identifier.
     * @return the next unique identifier as a RequestID
     */
	private RequestID getNext() {
		return new RequestID(mIDFactory.getNext());
	}
	
    /**
     * Constructs an <code>OrderID</code> object from the given field of the given <code>Message</code>.
     *
     * @param inMessage a <code>Message</code> value
     * @param inField an <code>int</code> value
     * @return an <code>OrderID</code> value or <code>null</code> if the given field does not exist on the given <code>Message</code>
     * @throws FieldNotFound if an error occurs retrieving the field from the <code>Message</code>
     */
    private OrderID getOrderIDFrom
    	(Message inMessage, int inField)
            throws FieldNotFound
    {
    	return new OrderID(inMessage.getString(inField));
    }
    
    /**
     * Constructs an <code>RequestID</code> object from the given field of the given <code>Message</code>.
     *
     * @param inMessage a <code>Message</code> value
     * @param inField an <code>int</code> value
     * @return an <code>RequestID</code> value or <code>null</code> if the given field does not exist on the given <code>Message</code>
     * @throws FieldNotFound if an error occurs retrieving the field from the <code>Message</code>
     */
    private RequestID getRequestIDFrom
    	(Message inMessage, int inField)
            throws FieldNotFound
    {
    	return new RequestID(inMessage.getString(inField));
    }
    
    /**
     * Gets the <code>UserID</code> associated with the given <code>Message</code> and <code>TradeInfo</code>.
     *
     * @param inMessage a <code>Message</code> value
     * @param isAck a <code>boolean</code> value
     * @param inInfo an <code>TradeInfo</code> value
     * @return a <code>UserID</code> value
     * @throws IllegalStateException if the given <code>TradeInfo</code> is malformed
     */
    private UserID getActorIDFromInfo
    	(Message inMessage, boolean isAck, TradeInfo inInfo)
    {
        // Update cache entry flags.
        if (isAck) {
            inInfo.setAckProcessed(true);
        } else {
            inInfo.setResponseProcessed(true);
        }
        inInfo.setMessageProcessed(inMessage);

        // Return result.
        return inInfo.getActorID();
    }
    
    /* 
     * (non-Javadoc)
     * @see org.marketcetera.server.ws.security.SessionListener#addSession(org.marketcetera.server.ws.security.ClientSession)
     */
	@Override
	public void addSession
		(ClientSession session) 
	{
		SessionInfo sessionInfo = session.getSessionInfo();
		getSessionMap().put(sessionInfo, Sets.newHashSet());
	}
	
	/* 
	 * (non-Javadoc)
	 * @see org.marketcetera.server.ws.security.SessionListener#removedSession(org.marketcetera.server.ws.security.ClientSession)
	 */
	@Override
	public void removedSession
		(ClientSession session) 
	{
		SessionInfo sessionInfo = session.getSessionInfo();
		if(getSessionMap().containsKey(sessionInfo)){
			Set<DataInfo> dataInfos = Sets.newHashSet(getSessionMap().get(sessionInfo));
			for(DataInfo dataInfo:dataInfos){
				for(MarketDataRequest request:dataInfo.getRequests()){
					RequestHandler.getInstance()
						.receiveMessage(new MarketDataCancel(request), sessionInfo);
				}
			}
		}
		getSessionMap().remove(sessionInfo);
	}

	/* 
	 * (non-Javadoc)
	 * @see org.marketcetera.server.ws.history.ReportSavedListener#reportSaved(org.marketcetera.trade.ReportBase, boolean)
	 */
	@Override
	public void reportSaved
		(ReportBase report, boolean status) 
	{
		OrderID orderID=report.getOrderID();
        if (orderID==null) {
            return;
        }
        TradeInfo info=get(orderID);
        if (info==null) {
            return;
        }
        info.setERPersisted(status);
	}
	
	/* 
	 * (non-Javadoc)
	 * @see org.marketcetera.server.ws.ServerPersister#persistMessage(org.marketcetera.ws.ResponseMessage)
	 */
	@Override
	public void persistMessage
		(ResponseMessage msg) 
	{
        try {
        	if (msg instanceof ReportBase) {
        		getHistoryService().saveReport((ReportBase)msg);
            }else if(msg instanceof QuoteEvent){
            	getHistoryService().saveQuote((QuoteEvent)msg);
        	}
        } catch (PersistenceException e) {
            Messages.RP_PERSIST_ERROR.error(this, e, msg);
            return;
        }
	}
	
	/* 
	 * (non-Javadoc)
	 * @see org.marketcetera.server.ws.deleteMessage#persistMessage(org.marketcetera.ws.ResponseMessage)
	 */
	@Override
	public void deleteMessage
		(ResponseMessage msg) 
	{
        try {
        	if(msg instanceof ReportBase) {
        		getHistoryService().deleteReport((ReportBase)msg);
            }else if(msg instanceof QuoteEvent){
            	getHistoryService().deleteQuote((QuoteEvent)msg);
        	}
        } catch (PersistenceException e) {
            Messages.RP_PERSIST_ERROR.error(this, e, msg);
        }
	}

	/* 
	 * (non-Javadoc)
	 * @see org.marketcetera.server.ws.ServerPersister#getActorIDs(quickfix.Message, boolean)
	 */
	@Override
	public Collection<UserID> getActorIDs
		(Message inMessage, boolean isAck) 
	{
		Collection<UserID> actorIDs = Sets.newHashSet();
    	if(inMessage.isSetField(MDReqID.FIELD)){
    	//  try using the reqID
    		RequestID reqID;
            try {
            	reqID = getRequestIDFrom(inMessage, MDReqID.FIELD);
            	DataInfo dataInfo = get(reqID);
    			if(dataInfo != null){
    				actorIDs.addAll(dataInfo.getUsers());
    			}
            } catch (FieldNotFound ignored) {
                Messages.RP_COULD_NOT_DETERMINE_ACTOR_ID.warn(ServerPersister.class,
                												MDReqID.FIELD,
                                                                inMessage);
            }
    	}else if(inMessage.isSetField(ClOrdID.FIELD)){
    	//  try using the orderID
    		OrderID orderID;
            try {
                orderID = getOrderIDFrom(inMessage,
                                         ClOrdID.FIELD);
                TradeInfo tradeInfo = get(orderID);
                if(tradeInfo != null){
                	UserID actorID = getActorIDFromInfo(inMessage, isAck, tradeInfo);
    				if(actorID != null) {
    					actorIDs.add(actorID);
    				}
                }else{
                	UserID actorID = getHistoryService().getActorID(orderID);
    				if(actorID != null) {
    					actorIDs.add(actorID);
    				}
                }
            } catch (FieldNotFound ignored) {
                Messages.RP_COULD_NOT_DETERMINE_ACTOR_ID.warn(ServerPersister.class,
                                                                ClOrdID.FIELD,
                                                                inMessage);
            }
    	}else if(inMessage.isSetField(OrigClOrdID.FIELD)){
    	//  try using the origOrderID
            OrderID origOrderID;
            try {
                origOrderID = getOrderIDFrom(inMessage,
                                             OrigClOrdID.FIELD);
                TradeInfo tradeInfo = get(origOrderID);
                if(tradeInfo != null){
                	UserID actorID = getActorIDFromInfo(inMessage, isAck, tradeInfo);
                	if(actorID != null) {
    					actorIDs.add(actorID);
    				}
                }else{
                	UserID actorID = getHistoryService().getActorID(origOrderID);
    				if(actorID != null) {
    					actorIDs.add(actorID);
    				}
                }
            } catch (FieldNotFound e) {
                Messages.RP_COULD_NOT_DETERMINE_ACTOR_ID.warn(ServerPersister.class,
                                                                OrigClOrdID.FIELD,
                                                                inMessage);
            }
    	}else{
	        // so now, we're stuck, we don't have any other way of getting the actorIDs 
	        SLF4JLoggerProxy.warn(ServerPersister.class,
	                              "Message {} could not be mapped to a known request so the actorIDs cannot be determined",
	                              inMessage);
    	}
    	
        return actorIDs;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.marketcetera.server.ws.ServerPersister#put(quickfix.Message, org.marketcetera.trade.UserID)
	 */
	@Override
	public TradeInfo put
		(Message msg, UserID actorID) 
	{
        OrderID orderID;
        try {
            orderID=new OrderID(msg.getString(ClOrdID.FIELD));
        } catch(FieldNotFound ex) {
            Messages.RP_ADD_TO_CACHE_FAILED.warn(this,ex,actorID,msg);
            return null;
        }
        OrderID origOrderID;
        try {
            origOrderID=new OrderID(msg.getString(OrigClOrdID.FIELD));
        } catch(FieldNotFound ex) {
            origOrderID=null;
        }
        return put(orderID,origOrderID,actorID);
	}

	/*
	 * (non-Javadoc)
	 * @see org.marketcetera.server.ws.ServerPersister#put(org.marketcetera.trade.OrderID, org.marketcetera.trade.OrderID, org.marketcetera.trade.UserID)
	 */
	@Override
	public TradeInfo put
		(OrderID orderID, OrderID origOrderID, UserID actorID) 
	{
		 TradeInfo info=new TradeInfoImpl
            (this,orderID,origOrderID,actorID);
		 getOrderMap().put(orderID,info);
		 Messages.OIM_ADDED_ENTRY.debug(this,orderID,getOrderMap().size());
        return info;
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.marketcetera.server.ws.ServerPersister#get(org.marketcetera.trade.OrderID)
	 */
	@Override
	public TradeInfo get(OrderID orderID) 
	{
		 return getOrderMap().get(orderID);
	}

	/*
	 * (non-Javadoc)
	 * @see org.marketcetera.server.ws.ServerPersister#remove(org.marketcetera.trade.OrderID)
	 */
	@Override
	public void remove(OrderID orderID) {
		getOrderMap().remove(orderID);
		Messages.OIM_REMOVED_ENTRY.debug(this, orderID, getOrderMap().size());
	}

	/*
	 * (non-Javadoc)
	 * @see org.marketcetera.server.ws.ServerPersister#put(org.marketcetera.marketdata.GenericDataRequest, org.marketcetera.info.SessionInfo)
	 */
	@Override
	public List<GenericDataRequest> put(GenericDataRequest inRequest, SessionInfo sessionInfo) {
		Validate.noNullElements(new Object[]{inRequest, sessionInfo});
		
		DataReferenceKey key = new DataReferenceKey(inRequest.getBrokerID(), inRequest.getInstrument(), inRequest.getExchange());
		
		UserID actorID = (UserID)sessionInfo.getValue(SessionInfo.ACTOR_ID);
		DataInfo dataInfo = getKeyMap().get(key);
		Entry<RequestID, MarketDataRequest> reqKey;
		
		//Validate the session info
		if(!getSessionMap().containsKey(sessionInfo)){
			throw new UnsupportedOperationException();
		}
		
    	if(inRequest instanceof MarketDataRequest){
    		MarketDataRequest request = (MarketDataRequest)inRequest;
    		
    		if(dataInfo == null){       		
    			MarketDataRequest mDReq = MarketDataRequestBuilder.newRequest()
						.withBrokerID(request.getBrokerID())
						.withInstrument(request.getInstrument())
						.withExchange(request.getExchange())
						.withRequestID(getNext())
						.withContent(request.getContents())
						.withPeriods(request.getPeriods())
						.withDepths(request.getDepths())
						.withCustomFields(request.getCustomFields())
						.create();
    			
    			reqKey = Maps.immutableEntry(mDReq.getRequestID(), mDReq);
    			dataInfo = new DataInfoImpl(reqKey);
        		dataInfo.addRequest(request, actorID);
        		
        		getSessionMap().get(sessionInfo).add(dataInfo);
        		getKeyMap().put(key, dataInfo);
    			getReqIDMap().put(mDReq.getRequestID(), dataInfo);
    			
    			return Lists.newArrayList(mDReq);
    		}
    		dataInfo.addRequest(request, actorID);
    		reqKey = dataInfo.getKey();
    	}else if(inRequest instanceof MarketDataCancel){
    		MarketDataCancel request = (MarketDataCancel)inRequest;
    		
    		if(dataInfo != null){
    			reqKey = dataInfo.getKey();
    			dataInfo.removeRequest(request.getRequestID(), actorID);
    			if(dataInfo.isEmpty()){    				
    				getSessionMap().get(sessionInfo).remove(dataInfo);
    				getKeyMap().remove(key);
    				getReqIDMap().remove(reqKey.getKey());
	        		return Lists.newArrayList(new MarketDataCancel(reqKey.getValue()));
				}
    			reqKey = dataInfo.getKey();
    		}else{
    			return Lists.newArrayList();
    		}
    	}else{
    		throw new UnsupportedOperationException();
    	}
    	
    	MarketDataRequest mDReq = reqKey.getValue();
    	List<GenericDataRequest> requests = Lists.newArrayList();
    	
    	//Compare mDReq to new dataMap
		Set<Content> newContent = Sets.newHashSet();
		Set<Period> newPeriods = Sets.newHashSet();
		Set<MarketDepth> newDepths = Sets.newHashSet();
		Map<String, String> newCustoms = Maps.newHashMap();
		for(MarketDataRequest request:dataInfo.getRequests()){
			newContent.addAll(request.getContents());
			newPeriods.addAll(request.getPeriods());
			newDepths.addAll(request.getDepths());
			newCustoms.putAll(request.getCustomFields());
		}
		
		if((mDReq.getMarketDepth() != MarketDepth.getMarketDepth(newDepths))//Different depth
				|| (Collections.disjoint(mDReq.getContents(), Content.reqTrade) !=
						Collections.disjoint(newContent, Content.reqTrade))//Different trade request
				|| (Collections.disjoint(mDReq.getContents(), Content.reqBidAsk) !=
						Collections.disjoint(newContent, Content.reqBidAsk))//Different tick request
				){
			//Cancel current request
			requests.add(new MarketDataCancel(mDReq));
			getReqIDMap().remove(reqKey.getKey());
			
			//Create new request
			mDReq = MarketDataRequestBuilder.newRequest()
					.withBrokerID(mDReq.getBrokerID())
					.withInstrument(mDReq.getInstrument())
					.withExchange(mDReq.getExchange())
					.withRequestID(getNext())
					.withContent(newContent)
					.withPeriods(newPeriods)
					.withDepths(newDepths)
					.withCustomFields(newCustoms)
					.create();
			requests.add(mDReq);
			
			reqKey = Maps.immutableEntry(mDReq.getRequestID(), mDReq);
			dataInfo.setKey(reqKey);
			getReqIDMap().put(mDReq.getRequestID(), dataInfo);
		}else{
			mDReq = MarketDataRequestBuilder.newRequest()
					.withBrokerID(mDReq.getBrokerID())
					.withInstrument(mDReq.getInstrument())
					.withExchange(mDReq.getExchange())
					.withRequestID(mDReq.getRequestID())
					.withContent(newContent)
					.withPeriods(newPeriods)
					.withDepths(newDepths)
					.withCustomFields(newCustoms)
					.create();
			
			reqKey = Maps.immutableEntry(mDReq.getRequestID(), mDReq);
			dataInfo.setKey(reqKey);
		}
		
		return requests;
	}

	/*
	 * (non-Javadoc)
	 * @see org.marketcetera.server.ws.ServerPersister#get(org.marketcetera.module.RequestID)
	 */
	@Override
	public DataInfo get(RequestID reqID) {
		return getReqIDMap().get(reqID);
	}

	/*
	 * (non-Javadoc)
	 * @see org.marketcetera.server.ws.ServerPersister#get(org.marketcetera.marketdata.DataReferenceKey)
	 */
	@Override
	public DataInfo get(DataReferenceKey reqKey) {
		return getKeyMap().get(reqKey);
	}

	/*
	 * (non-Javadoc)
	 * @see org.marketcetera.server.ws.ServerPersister#get(org.marketcetera.trade.BrokerID)
	 */
	@Override
	public Set<Message> get(BrokerID bID) {
		Set<Message> requests = Sets.newHashSet();
		for(DataInfo info:getReqIDMap().values()){
			MarketDataRequest request = info.getKey().getValue();
			if(request.getBrokerID().equals(bID)){
				requests.add(info.getMessage());
			}
		}
		return requests;
	}
}
