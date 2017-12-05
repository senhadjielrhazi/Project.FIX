package org.marketcetera.server.ws;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang.Validate;
import org.marketcetera.core.time.Period;
import org.marketcetera.marketdata.MarketDataRequest;
import org.marketcetera.module.RequestID;
import org.marketcetera.trade.UserID;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import quickfix.Message;

/**
 * An entry for data information in a {@link ServerPersister} cache.
 */
public class DataInfoImpl implements DataInfo {

	/**
     * Mapping the client requests. 
     */
	private final Map<RequestID, MarketDataRequest> mDataMap = Maps.newConcurrentMap();
	
	/**
     * Mapping the client quote recorders. 
     */
    private final Map<Period, QuoteRecorder> mQuoteMap = Maps.newConcurrentMap();
    
    /**
     * Mapping the user IDs
     */
	private final Map<UserID, List<RequestID>> mUserMap = Maps.newConcurrentMap();
	
	
    /**
     * Mapping unique server request. 
     */
    private Entry<RequestID, MarketDataRequest> mKey;
    
    private Message mMessage;
    
    /**
     * Returns the receiver's in-memory map of client requests
     *
     * @return The map.
     */
    private Map<RequestID, MarketDataRequest> getReqIDMap()
    {
        return mDataMap;
    }
    
    /**
     * Returns the receiver's in-memory map of client quote recorders.
     *
     * @return The map.
     */
    private Map<Period, QuoteRecorder> getQuoteMap()
    {
        return mQuoteMap;
    }
    
    /**
     * Returns the receiver's in-memory map of user IDs
     *
     * @return The map.
     */
    private Map<UserID, List<RequestID>> getUserMap()
    {
        return mUserMap;
    }
    
    /**
     * Instance of DataInfoImpl
     * 
     * @param inKey key an <code>Map.Entry</code> value
     */
	DataInfoImpl(Entry<RequestID, MarketDataRequest> inKey){
    	setKey(inKey);
    }
	
	/* (non-Javadoc)
     * @see  org.marketcetera.server.ws.DataInfo#getKey()
     */
	public Entry<RequestID, MarketDataRequest> getKey()
    {
        return mKey;
    }
    
    /* (non-Javadoc)
     * @see  org.marketcetera.server.ws.DataInfo#setKey(java.util.Map.Entry<RequestID, MarketDataRequest>)
     */
	public void setKey(Entry<RequestID, MarketDataRequest> inKey) {
		mKey = inKey;
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.marketcetera.server.ws.DataInfo#getMessage()
	 */
	@Override
	public Message getMessage() {
		return mMessage;
	}

	/*
	 * (non-Javadoc)
	 * @see org.marketcetera.server.ws.DataInfo#setMessage(quickfix.Message)
	 */
	@Override
	public void setMessage(Message message) {
		mMessage = message;
	}
	
    /* (non-Javadoc)
     * @see  org.marketcetera.server.ws.DataInfo#addRequest(org.marketcetera.marketdata.MarketDataRequest, org.marketcetera.trade.UserID)
     */
	@Override
	public void addRequest(MarketDataRequest request, UserID actorID) {
		Validate.noNullElements(new Object[]{request, actorID});
		getReqIDMap().put(request.getRequestID(), request);
		
		for(Period period:request.getPeriods()){
			QuoteRecorder recorder = getQuoteMap().get(period);
			if(recorder == null){
				recorder = new QuoteRecorderImpl(request.getBrokerID(), request.getInstrument(), request.getExchange(), period);
				getQuoteMap().put(period, recorder);
			}
			recorder.addRequest(request.getRequestID(), actorID);
		}
		
		List<RequestID> requests = getUserMap().get(actorID);
		if(requests == null){
			requests = Lists.newArrayList();
			getUserMap().put(actorID, requests);
		}
		requests.add(request.getRequestID());
	}
	
    /* (non-Javadoc)
     * @see  org.marketcetera.server.ws.DataInfo#removeRequest(org.marketcetera.module.RequestID, org.marketcetera.trade.UserID)
     */
	@Override
	public void removeRequest(RequestID requestID, UserID actorID) {
		Validate.noNullElements(new Object[]{requestID, actorID});
		MarketDataRequest request = getReqIDMap().remove(requestID);
		
		if(request != null){
			for(Period period:request.getPeriods()){
				QuoteRecorder recorder = getQuoteMap().get(period);
				recorder.removeRequest(request.getRequestID(), actorID);
				if(recorder.isEmpty()){
					getQuoteMap().remove(period);
	    		}
			}
		}
		
		List<RequestID> requests = getUserMap().get(actorID);
		if(requests != null){
			requests.remove(requestID);
			if(requests.isEmpty()){
				getUserMap().remove(actorID);
			}
		}
	}
	
    /* (non-Javadoc)
     * @see  org.marketcetera.server.ws.DataInfo#getRequests()
     */
	@Override
	public Collection<MarketDataRequest> getRequests() {
		return getReqIDMap().values();
	}
	
    /* (non-Javadoc)
     * @see  org.marketcetera.server.ws.DataInfo#getRecorders()
     */
	@Override
	public Collection<QuoteRecorder> getRecorders() {
		return getQuoteMap().values();
	}
	
    /* (non-Javadoc)
     * @see  org.marketcetera.server.ws.DataInfo#values()
     */
	@Override
	public Collection<UserID> getUsers() {
		return getUserMap().keySet();
	}
	
    /* (non-Javadoc)
     * @see  org.marketcetera.server.ws.DataInfo#isEmpty()
     */
	@Override
	public boolean isEmpty() {
		return getReqIDMap().isEmpty();
	}
}
