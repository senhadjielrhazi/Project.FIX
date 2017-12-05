package org.marketcetera.server.ws;

import org.marketcetera.event.QuoteEvent;
import org.marketcetera.trade.ExecutionReport;
import org.marketcetera.util.misc.ClassVersion;

/**
 * Receives reports/quotes and processes them according to the implementer's nature.
 */
@ClassVersion("$Id: ServerReceiver.java 16543 2013-04-11 16:02:00Z colin $")
public interface ServerReceiver
{
    /**
     * Adds the given <code>ExecutionReport</code> object.
     *
     * @param inReport an <code>ExecutionReport</code> value
     * @throws RuntimeException if the report could not be added 
     */
    public void addReport(ExecutionReport inReport);
    /**
     * Deletes the given <code>ExecutionReport</code> object.
     *
     * @param inReport an <code>ExecutionReport</code> value
     */
    public void deleteReport(ExecutionReport inReport);
    /**
     * Adds the given <code>QuoteEvent</code> object.
     *
     * @param inQuote an <code>QuoteEvent</code> value
     * @throws RuntimeException if the quote could not be added 
     */
    public void addQuote(QuoteEvent inQuote);
    /**
     * Deletes the given <code>QuoteEvent</code> object.
     *
     * @param inQuote an <code>QuoteEvent</code> value
     */
    public void deleteQuote(QuoteEvent inQuote);
}
