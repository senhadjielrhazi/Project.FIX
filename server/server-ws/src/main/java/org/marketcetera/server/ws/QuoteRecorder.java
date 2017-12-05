package org.marketcetera.server.ws;

import org.marketcetera.event.QuoteEvent;
import org.marketcetera.event.TickEvent;
import org.marketcetera.module.RequestID;
import org.marketcetera.trade.UserID;

/**
 * The quote recorder input: {@link TickEvent}, output: {@link QuoteEvent}
 */
public interface QuoteRecorder {

    /**
     * Adds the client market data request.
     *
     * @param request the market data request
     * 
     * @param UserID the user.
     */
	public void addRequest(RequestID requestID, UserID actorID);
    
	/**
     * Removes the client market data request.
     *
     * @param RequestID the market data request
     * 
     * @param UserID the user.
     */
	public void removeRequest(RequestID requestID, UserID actorID);
	
	/**
	 * Recorder the tick event and process the current quote
	 * 
	 * @param event the tick event
	 */
	public void onTick(TickEvent event);
	
	/**
     * Returns true if this data info contains no key-value mappings.
     */
	public boolean isEmpty();
}
