package org.marketcetera.server.ws.history;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.marketcetera.core.IDFactory;
import org.marketcetera.core.position.PositionKey;
import org.marketcetera.core.time.Period;
import org.marketcetera.event.Quote;
import org.marketcetera.event.QuoteEvent;
import org.marketcetera.event.info.InstrumentInfo;
import org.marketcetera.persist.PersistenceException;
import org.marketcetera.trade.*;
import org.marketcetera.util.misc.ClassVersion;

import com.mchange.v2.c3p0.ComboPooledDataSource;

import org.marketcetera.ws.server.security.SimpleUser;

/**
 * Provides service to save and query reports.
 */
@ClassVersion("$Id: HistoryService.java 16670 2013-08-28 19:49:06Z colin $")
public interface HistoryService {

    /**
     * Initializes the receiver with the given system resources.
     *
     * @param idFactory The ID factory to be used for report ID
     * generation.
     * @param reportSavedListener The listener notified after a report
     * has been saved (successfully or not). It may be null if no
     * notifications are needed.
     * @param dataSource the data source
     *
     * @throws PersistenceException if an error occurs during initialization
     */
    public void init(IDFactory idFactory,
    		ReportSavedListener reportSavedListener,
    		ComboPooledDataSource dataSource)
            throws PersistenceException;
    
    /**
     * Returns the actor ID associated with the report with given
     * order ID.
     *
     * @param orderID The order ID.
     *
     * @return The actor ID. If no report with the given order ID
     * exists, null is returned, and no exception is thrown.
     *
     * @throws PersistenceException if there were errors accessing the
     * report.
     */
    public UserID getActorID
    		(final OrderID orderID)
    		throws PersistenceException;
    
    /**
     * Returns all the reports received after the supplied date-time
     * value, and which are visible to the given user.
     *
     * @param user the user making the query. Cannot be null.
     * @param data the date-time value. Cannot be null.
     *
     * @return the reports that were received after the date-time
     * value, and which are visible to the given user.
     *
     * @throws PersistenceException if there were persistence errors
     * fetching the reports.
     */
    public ReportBaseImpl[] getReportsSince
    		(SimpleUser user, Date data)
    		throws PersistenceException;

    /**
     * Returns the open orders visible to the given user.
     *
     * @param user a <code>SimpleUser</code> value
     * @return a <code>List&lt;ReportBaseImpl</code> value
     * @throws PersistenceException if there were errors retrieving the data
     */
    public List<ReportBaseImpl> getOpenOrders(SimpleUser user)
            throws PersistenceException;
 
    /**
     * Returns the position of the instrument based on all reports
     * received for it before or on the supplied date, and which are visible
     * to the given user.
     *
     * @param user the user making the query. Cannot be null.
     * @param data the date to compare with all the reports. Only the reports
     * that were received prior to or on this date will be used in this calculation.
     * Cannot be null.
     * @param instrument the instrument whose position is desired. Cannot be null.
     *
     * @return the instrument position.
     *
     * @throws PersistenceException if there were errors retrieving the equity
     * position
     */
    public BigDecimal getPositionAsOf
    		(SimpleUser user, Date data, Instrument instrument)
    		throws PersistenceException;
    
    /**
     * Returns the aggregate position of each (instrument,account,actor)
     * tuple based on all reports for given security type
     * received for each tuple on or before the supplied date, 
     * and which are visible to the given user.
     *
     * @param user the user making the query. Cannot be null.
     * @param data the date to compare with all the reports. Only
     * the reports that were received on or prior to this date will be
     * used in this calculation. Cannot be null.
     * @param securityType the security type. Cannot be null.
     *
     * @return the position map.
     *
     * @throws PersistenceException if there were errors retrieving the
     * position map.
     */
    public Map<PositionKey, BigDecimal> getPositionsAsOf
    		(SimpleUser user, Date data, SecurityType securityType)
    		throws PersistenceException;

    /**
     * Returns the aggregate position of each (instrument,account,actor)
     * tuple based on all reports received for each tuple on or before
     * the supplied date, and which are visible to the given user.
     *
     * @param user the user making the query. Cannot be null.
     * @param data the date to compare with all the reports. Only
     * the reports that were received on or prior to this date will be
     * used in this calculation. Cannot be null.
     *
     * @return the position map.
     *
     * @throws PersistenceException if there were errors retrieving the
     * position map.
     */
    public Map<PositionKey, BigDecimal> getAllPositionsAsOf
    		(SimpleUser user, Date data)
    		throws PersistenceException;
   
    /**
     * Saves the supplied report to the database. Saving may be
     * immediate or delayed; in either case the report ID is set
     * before this method returns.
     *
     * @param report the report to be saved. Cannot be null.
     *
     * @throws PersistenceException if there were errors saving the report.
     */
    public void saveReport(ReportBase report)
    		throws PersistenceException;
    
    /**
     * Deletes the supplied report from the database.
     *
     * @param report a <code>ReportBase</code> value
     * @throws PersistenceException if there were errors deleting the report
     */
    public void deleteReport(ReportBase report)
            throws PersistenceException;
    
    /**
     * Returns the history quotes visible to the given dates.
     *
     * @param instrument The instrument of the request.
     * @param inPeriod The period of the quotes.
     * @param inFrom The date from which to start in UTC. Cannot be null.
     * @param inTo The date to which to stop in UTC. Cannot be null.
     * @return a <code>List&lt;Quote</code> value
     * @throws PersistenceException if there were errors retrieving the data
     */
    public List<Quote> getQuoteHistory(Instrument instrument, Period period, 
    		Date fromDate, Date toDate)
            throws PersistenceException;
    
    /**
     * Returns the available instruments supported by the Broker.
     *
     * @param brokerID a <code>BrokerID</code> value
     * @throws PersistenceException if there were errors retrieving the instruments
     */
    public Set<Instrument> getInstruments(BrokerID brokerID)
    		throws PersistenceException;
    
    /**
     * Gets the supplied instrument info to the database.
     *
     * @param brokerID a <code>BrokerID</code> value
	 * @param instrument a <code>Instrument</code> value
     * @throws PersistenceException if there were errors retrieving the info
     */
    public InstrumentInfo getInstrumentInfo(BrokerID brokerID, Instrument instrument)
    		throws PersistenceException;
    
    /**
     * Saves/Replaces the supplied instrument info to the database.
     *
     * @param brokerID a <code>BrokerID</code> value
	 * @param instrument a <code>Instrument</code> value
     * @param info a <code>InstrumentInfo</code> value
     * @throws PersistenceException if there were errors saving the info
     */
    public void setInstrumentInfo(BrokerID brokerID, Instrument instrument, InstrumentInfo info)
    		throws PersistenceException;
    
    /**
     * Deletes the instrument info from the database.
     *
     * @param brokerID a <code>BrokerID</code> value
	 * @param instrument a <code>Instrument</code> value
     * @throws PersistenceException if there were errors deleting the info
     */
    public void deleteInstrumentInfo(BrokerID brokerID, Instrument instrument)
    		throws PersistenceException;

    /**
     * Gets the root order ID for the given order ID.
     *
     * @param inOrderID an <code>OrderID</code> value
     * @return an <code>OrderID</code> value or <code>null</code>
     */
	public OrderID getRootOrderID(OrderID inOrderID)
			throws PersistenceException;

    /**
     * Saves the supplied quote to the database. Saving may be
     * immediate or delayed;
     *
     * @param quote the quote to be saved. Cannot be null.
     *
     * @throws PersistenceException if there were errors saving the quote.
     */
    public void saveQuote(QuoteEvent quote)
    		throws PersistenceException;
    
    /**
     * Deletes the supplied quote from the database.
     *
     * @param quote a <code>QuoteEvent</code> value
     * @throws PersistenceException if there were errors deleting the quote
     */
	public void deleteQuote(QuoteEvent quote)
			throws PersistenceException;
}
