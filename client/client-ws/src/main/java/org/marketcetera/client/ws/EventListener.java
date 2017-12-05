package org.marketcetera.client.ws;

import org.marketcetera.util.misc.ClassVersion;
import org.marketcetera.event.Event;
import org.marketcetera.marketdata.MarketDataReject;

/**
 * A receiver of market data. Classes that need to be able to
 * receive market data can implement this interface and register
 * themselves to receive execution reports via
 * {@link IDataClient#addEventListener(EventListener)}.
 * <p>
 * It's not expected that data listeners will take too much time to
 * return. Currently all market data listeners are invoked sequentially.
 * If a data listener takes too much time to process the market event, it will
 * delay the delivery of market data to other registered listeners. 
 */
@ClassVersion("$Id: EventListener.java 16154 2012-07-14 16:34:05Z colin $") //$NON-NLS-1$
public interface EventListener {
	/**
     * Invoked to supply a market data event instance to the data listener.
     *
     * @param inEvent The received event.
     */
    public void receiveDataEvent(Event inEvent);

    /**
     * Invoked to supply a market data reject event instance to the data
     * listener.
     *
     * @param inReject The received data rejection report.
     */
    public void receiveDataReject(MarketDataReject inReject);
}
