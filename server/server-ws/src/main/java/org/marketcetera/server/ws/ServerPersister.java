package org.marketcetera.server.ws;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.marketcetera.info.SessionInfo;
import org.marketcetera.marketdata.DataReferenceKey;
import org.marketcetera.marketdata.GenericDataRequest;
import org.marketcetera.module.RequestID;
import org.marketcetera.server.ws.history.ReportSavedListener;
import org.marketcetera.server.ws.security.SessionListener;
import org.marketcetera.trade.BrokerID;
import org.marketcetera.trade.OrderID;
import org.marketcetera.trade.UserID;
import org.marketcetera.util.misc.ClassVersion;
import org.marketcetera.ws.ResponseMessage;

import quickfix.Message;

/**
 * A persister of messages (replies) sent by the server to
 * clients. It also handles mapping of messages to actors, via
 * either replies previously persisted, or via an in-memory cache.
 * 
 * A cache of order information. Cache entries are instances of {@link
 * TradeInfo}.
 * 
 * A cache of market data information. Entries are instances of {@link 
 * DataInfo}.
 */
@ClassVersion("$Id: ServerPersister.java")
public interface ServerPersister 
	extends SessionListener, ReportSavedListener {
	
    /**
     * Persists the given message, which, while doing so, may be
     * modified. Persistence may be effected synchronously or
     * asynchronously.
     *
     * @param msg The message.
     */
    public void persistMessage
        (ResponseMessage msg);
    
    /**
     * Deletes the given message.
     *
     * @param msg a <code>ResponseMessage</code> value
     */
    public void deleteMessage
    	(ResponseMessage msg);
    
	/**
     * Returns the actor ID associated with the given message.
     *
     * @param msg The message.
     * @param isAck True if the request is made for the purposes of
     * processing an Server ack.
     *
     * @return The actorIDs. If no report with the given request ID
     * exists, empty set is returned, and no exception is thrown.
     */
    public Collection<UserID> getActorIDs
    	(Message inMessage, boolean isAck);
    
    /**
     * Adds the given outgoing order message, with the given actorID,
     * to the receiver's cache, and returns the new cache entry.
     *
     * @param msg The message.
     * @param actorID The actor ID.
     *
     * @return The new cache entry, or null if one could not be
     * created.
     */
    public TradeInfo put
        (Message msg, UserID actorID);
    
    /**
     * Adds (and returns) a new entry to the receiver for an order
     * with the given self and parent IDs, and the given actor ID.
     *
     * @param orderID The order ID.
     * @param origOrderID The parent order ID. It may be null for
     * orders that are chain roots.
     * @param actorID The actor ID.
     *
     * @return The new entry.
     */
    public TradeInfo put
        (OrderID orderID,
         OrderID origOrderID,
         UserID actorID);
    
    /**
     * Returns the receiver's entry for the order with the given ID.
     *
     * @param orderID The order ID.
     *
     * @return The entry. It may be null if the received has no such
     * entry.
     */
    public TradeInfo get
        (OrderID orderID);
    
    /**
     * Removes the receiver's entry for the order with the given ID.
     *
     * @param orderID The order ID.
     */
    public void remove
        (OrderID orderID);
    
    /**
     * Add the given request and compile the sub market data requests 
     * with respect to the broker state.
     * 
     * @param request The client request.
     * @param sessionInfo the session info.
     * 
     * @return a <code>List&lt;GenericDataRequest&gt;</code> value
     */
    public List<GenericDataRequest> put
    	(GenericDataRequest request, 
    			SessionInfo sessionInfo);
  
    /**
     * Gets the market data info related to current broker request ID.
     * 
     * @param reqID The broker request ID.
     * 
     * @return The DataInfo. It may be null if the received has no such
     * entry.
     */
	public DataInfo get
		(RequestID reqID);
    
	/**
     * Gets the market data info related to current market data key.
     * 
     * @param reqKey The reference key.
     * 
     * @return The DataInfo. It may be null if the received has no such
     * entry.
     */
	public DataInfo get
		(DataReferenceKey reqKey);
    
	/**
	 * Returns all the broker requests.
	 * 
     * @param bID The broker ID.
     * 
	 * @return a Set of broker requests
	 */
	public Set<Message> get
		(BrokerID bID);
}
