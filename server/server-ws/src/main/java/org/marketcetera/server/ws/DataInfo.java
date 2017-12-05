package org.marketcetera.server.ws;

import java.util.Collection;
import java.util.Map.Entry;

import org.marketcetera.marketdata.MarketDataRequest;
import org.marketcetera.module.RequestID;
import org.marketcetera.trade.UserID;

import quickfix.Message;

/**
 * An entry for data information in a {@link ServerPersister} cache.
 */
public interface DataInfo {
    
    /**
     * Returns the receiver's server request ID.
     *
     * @return The entry key.
     */
	public Entry<RequestID, MarketDataRequest> getKey();
	
    /**
     * Sets the receiver's server request ID.
     *
     * @param The entry key.
     */
    public void setKey(Entry<RequestID, MarketDataRequest> key);
	
	/**
     * Gets the key message.
     *
     * @return The message.
     */

	public Message getMessage();
	
    /**
     * Sets the key message.
     *
     * @param The message.
     */

	public void setMessage(Message qMsg);
    
    /**
     * Adds the client market data request.
     *
     * @param request the market data request
     * 
     * @param UserID the user.
     */
    public void addRequest(MarketDataRequest request, UserID actorID);
	
    /**
     * Removes the client market data request.
     *
     * @param RequestID the market data request
     * 
     * @param UserID the user.
     */
	public void removeRequest(RequestID requestID, UserID actorID);

	/**
	 * Returns a Collection of the market data request contained in this data info.
	 */
	public Collection<MarketDataRequest> getRequests();
	
	/**
	 * Returns a Collection of the market data recorders contained in this data info.
	 */
	public Collection<QuoteRecorder> getRecorders();
	
    /**
     * Gets the data info users
     * @return The collection of users. It may be null if the received has no such
     * entry.
     */
	public Collection<UserID> getUsers();
	
	/**
     * Returns true if this data info contains no key-value mappings.
     */
	public boolean isEmpty();
}
