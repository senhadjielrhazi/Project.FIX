package org.marketcetera.client.ws;

import java.beans.ExceptionListener;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.marketcetera.brokers.BrokerStatusListener;
import org.marketcetera.brokers.BrokersStatus;
import org.marketcetera.util.misc.ClassVersion;
import org.marketcetera.ws.RequestMessage;
import org.marketcetera.ws.ValidationException;
import org.marketcetera.ws.client.ClientParameters;
import org.marketcetera.ws.client.ConnectionException;
import org.marketcetera.core.notifications.ConnectionStatusListener;
import org.marketcetera.core.position.PositionKey;
import org.marketcetera.core.time.Period;
import org.marketcetera.event.Quote;
import org.marketcetera.event.QuoteEvent;
import org.marketcetera.event.info.InstrumentInfo;
import org.marketcetera.info.UserInfo;
import org.marketcetera.trade.BrokerID;
import org.marketcetera.trade.ExecutionReportImpl;
import org.marketcetera.trade.FIXMessageWrapper;
import org.marketcetera.trade.Hierarchy;
import org.marketcetera.trade.Instrument;
import org.marketcetera.trade.OrderID;
import org.marketcetera.trade.ReportBase;
import org.marketcetera.trade.ReportBaseImpl;
import org.marketcetera.trade.SecurityType;
import org.marketcetera.trade.UserID;

/**
 * A client end point that communicates with the server.
 * <p>
 * The client provides the following lifecycle methods.
 * <ul>
 *      <li>{@link #reconnect()}: to reconnect to the server</li>
 *      <li>{@link #reconnect(ClientParameters)}: to reconnect to a server
 *      that is different from the one the client was originally connected
 *      to.</li>
 * </ul>
 * 
 * The client provides the following set of services:
 * <ul>
 *      <li>{@link #send(RequestMessage)}
 *      <li>{@link #reSendData() resends market data requests}    
 *      <li>{@link #addReportListener(ReportListener) receive reports}</li>
 *      <li>{@link #addEventListener(EventListener) receive events}</li> 
 *      <li>{@link #addBrokerStatusListener(BrokerStatusListener) receive broker status}</li>
 *      <li>{@link #addConnectionStatusListener(ConnectionStatusListener) receive connection status}</li>  
 *      <li>{@link #addExceptionListener(ExceptionListener) receive exceptions}</li> 
 *      <li>{@link #getReportsSince(Date) fetch past reports} </li>
 *      <li>{@link #getOpenOrders() fetch all visible open orders}</li>
 *      <li>{@link #getPositionAsOf(Date, Instrument) fetch instrument position} </li>
 *      <li>{@link #getPositionsAsOf(Date, SecurityType) fetch all open positions with securityType} </li>
 *      <li>{@link #getAllPositionsAsOf(java.util.Date) fetch all positions} </li>
 *      <li>{@link #getQuoteHistory(Instrument, Period, Date, Date) fetch past history} </li>
 *      <li>{@link #getInstruments(BrokerID) fetch all visible instruments for brokerID}</li>   
 *      <li>{@link #getInstrumentInfo(BrokerID, Instrument, InstrumentInfo) load instrument info} </li>
 *      <li>{@link #isRunning() returns the state of the service} </li>  
 * </ul>  
 */
@ClassVersion("$Id: IWSClient.java 16888 2014-04-22 18:32:36Z colin $")
public interface WSClient 
{	
	/**
     * Sends the supplied data request to the server.
     *
     * @param inRequest The data to send.
     *
     * @throws ConnectionException if there were connection errors sending
     * the trade out to the server.
     *
     * @throws ValidationException if the data didn't have complete
     * or consistent state to be sent to the server.
     */
    public void send(RequestMessage inRequest)
            throws ConnectionException, ValidationException;
    
    /**
     * Re-Sends the supplied market data request to the server.
     */
    public void reSendData();
    
    /**
     * Adds a report listener. The report listener receives all the reports
     * sent out by the server.
     * <p>
     * If the same listener is added more than once, it will receive
     * notifications as many times as it's been added.
     * <p>
     * The listeners are notified in the reverse order of their addition. 
     *
     * @param inListener The listener instance that should be supplied
     * the reports.
     */
    public void addReportListener
    	(ReportListener inListener);

    /**
     * Removes a report listener that was previously added via
     * {@link #addReportListener(ReportListener)}. If the listener
     * was added more than once, only its most recently added occurrence
     * will be removed. 
     *
     * @param inListener The listener instance that should no longer
     * be receiving the reports.
     */
    public void removeReportListener
    	(ReportListener inListener);
    
    /**
     * Adds a market data listener. The data listener receives all the market 
     * data sent out by the server.
     * <p>
     * If the same listener is added more than once, it will receive
     * notifications as many times as it's been added.
     * <p>
     * The listeners are notified in the reverse data of their addition. 
     *
     * @param inListener The listener instance that should be supplied
     * the market data.
     */
    public void addEventListener
    	(EventListener inListener);

    /**
     * Removes a market data listener that was previously added via
     * {@link #addMarketDataListener(DataEventListener)}. If the listener
     * was added more than once, only its most recently added occurrence
     * will be removed. 
     *
     * @param inListener The listener instance that should no longer
     * be receiving the market data.
     */
    public void removeEventListener
    	(EventListener inListener);
    
    /**
     * Adds a broker status listener, which receives all the
     * broker status changes sent out by the server.
     *
     * <p>If the same listener is added more than once, it will receive
     * notifications as many times as it has been added.</p>
     *
     * <p>The listeners are notified in the reverse order of their
     * addition.</p>
     *
     * @param listener The listener which should be supplied the
     * broker status changes.
     */
    public void addBrokerStatusListener
        (BrokerStatusListener listener);

    /**
     * Removes a broker status listener that was previously added
     * via {@link
     * #addBrokerStatusListener(BrokerStatusListener)}.
     *
     * <p>If the listener was added more than once, only its most
     * recently added instance will be removed.</p>
     *
     * @param listener The listener which should stop receiving
     * broker status changes.
     */
    public void removeBrokerStatusListener
        (BrokerStatusListener listener);

    /**
     * Adds a server connection status listener, which receives all
     * the server connection status changes.
     *
     * <p>If the same listener is added more than once, it will receive
     * notifications as many times as it has been added.</p>
     *
     * <p>The listeners are notified in the reverse order of their
     * addition.</p>
     *
     * @param listener The listener which should be supplied the
     * server connection status changes.
     */
    public void addConnectionStatusListener
        (ConnectionStatusListener listener);

    /**
     * Removes a server connection status listener that was previously
     * added via {@link
     * #addConnectionStatusListener(ConnectionStatusListener)}.
     *
     * <p>If the listener was added more than once, only its most
     * recently added instance will be removed.</p>
     *
     * @param listener The listener which should stop receiving server
     * connection status changes.
     */
    public void removeConnectionStatusListener
        (ConnectionStatusListener listener);
    
    /**
     * Adds an exception listener. The exception listeners are notified
     * whenever the client encounters connectivity issues when communicating
     * with the server.
     * <p>
     * The listeners are notified only when connectivity issues are
     * encountered when sending or receiving messages, ie. when any of
     * the <code>send*()</code> methods are invoked, or when the
     * client receives a message and encounters errors processing it
     * before delivering it to {@link ReportListener} or {@link
     * BrokerStatusListener}, or when client heartbeats cannot reach
     * the server.
     * <p>
     * If the same listener is added more than once, it will receive
     * notifications as many times as it's been added.
     * <p>
     * The listeners are notified in the reverse order of their addition.
     *
     * @param inListener the listener instance.
     */
    public void addExceptionListener
    	(ExceptionListener inListener);

    /**
     * Removes exception listener that was previously added via
     * {@link #addExceptionListener(java.beans.ExceptionListener)}. The
     * listener will stop receiving exception notifications after this
     * method returns.
     * If the listener was added more than once, only its most
     * recently added occurrence will be removed. 
     *
     * @param inListener The exception listener that should no longer
     */
    public void removeExceptionListener
    	(ExceptionListener inListener);

    /**
     * Returns all the reports (execution report and order cancel rejects)
     * generated and received by the server since the supplied date in UTC.
     *
     * @param inDate The date in UTC. Cannot be null.
     *
     * @return All the reports since the supplied date, may be empty.
     *
     * @throws ConnectionException if there were connection errors fetching
     * data from the server.
     */
    public ReportBase[] getReportsSince(Date inDate) throws ConnectionException;

    /**
     * Gets all open orders visible to the current user.
     *
     * @return a <code>List&lt;ReportBaseImpl&gt;</code> value
     * @throws ConnectionException if an error occurs connecting to the server
     */
    public List<ReportBaseImpl> getOpenOrders() throws ConnectionException;
    
    /**
     * Returns the position of the supplied instrument based on reports,
     * generated and received on or before the supplied date in UTC.
     *
     * @param inDate the date in UTC. Cannot be null.
     * @param inInstrument The instrument. Cannot be null.
     *
     * @return the current position of the instrument.
     *
     * @throws ConnectionException if there were connection errors fetching
     * data from the server.
     */
    public BigDecimal getPositionAsOf(Date inDate, Instrument inInstrument)
            throws ConnectionException;

    /**
     * Returns all open positions based on reports,
     * generated and received on or before the supplied date in UTC.
     *
     * @param inDate the date in UTC. Cannot be null.
     * @param inSecurityType the security type. Cannot be null.
     * 
     * @return the open positions for given security type. Includes non-zero positions only.
     *
     * @throws ConnectionException if there were connection errors fetching
     * data from the server.
     */
    public Map<PositionKey, BigDecimal> getPositionsAsOf(Date inDate, SecurityType inSecurityType)
            throws ConnectionException;
    
    /**
     * Returns all open positions based on reports,
     * generated and received on or before the supplied date in UTC.
     *
     * @param inDate the date in UTC. Cannot be null.
     *
     * @return the open positions. Includes non-zero positions only.
     *
     * @throws ConnectionException if there were connection errors fetching
     * data from the server.
     */
    public Map<PositionKey,BigDecimal> getAllPositionsAsOf(Date inDate)
            throws ConnectionException;
    
    /**
     * Returns all the market data quotes in the server from to the supplied dates in UTC.
     *
     * @param inInstrument The instrument of the request.
     * @param inPeriod The period of the quotes.
     * @param inFrom The date from which to start in UTC. Cannot be null.
     * @param inTo The date to which to stop in UTC. Cannot be null.
     * 
     * @return All the quotes between the supplied dates, may be empty.
     *
     * @throws ConnectionException if there were connection errors fetching
     * data from the server.
     */
    public List<Quote> getQuoteHistory(Instrument inInstrument, Period inPeriod, 
    		Date inFrom, Date inTo) throws ConnectionException;
    
    /**
     * Returns the available instruments supported by the Broker.
     * @param inBrokerID The broker ID of the request.
     *
     * @return All the instruments supported by the broker.
     */
    public Set<Instrument> getInstruments(BrokerID inBrokerID) 
    		throws ConnectionException;
    
    /**
     * Adds the given quote event to the system data flow.
     * 
     * <p>Market data added this way will be added to the system data bus. Events will be
     * persisted and become part of the system record. All clients will receive this
     * event.
     * 
     * <p><em>This will affect reported data history</em></p>.
     *
     * @param inQuote a <code>QuoteEvent</code> value
     * @throws ConnectionException if an error occurs connecting to the server
     */
    public void addQuote(QuoteEvent inQuote)
            throws ConnectionException;

    /**
     * Removes the given quote event from the persistent db store.
     * 
     * <p>Market data removed this way will not be added to the system data bus and no clients
     * will receive this event.
     * 
     * <p><em>This will affect reported data history</em></p>.
     *
     * @param inQuote an <code>QuoteEvent</code> value
     * @throws ConnectionException if an error occurs connecting to the server
     */
    public void deleteQuote(QuoteEvent inQuote)
            throws ConnectionException;
    
    /**
     * Sets the instrument info associated with the current instrument and
     * Broker.
     *
     * @param inBrokerID a <code>BrokerID</code> value
	 * @param inInstrument a <code>Instrument</code> value
     * @param inInfo a <code>InstrumentInfo</code> value
     * @throws ConnectionException if an error occurs connecting to the server
     */
    public void setInstrumentInfo(BrokerID inBrokerID, Instrument inInstrument, InstrumentInfo inInfo)
            throws ConnectionException;
    
    /**
     * Deletes the instrument info associated with the current instrument and Broker.
     *
     * @param inBrokerID a <code>BrokerID</code> value
	 * @param inInstrument a <code>Instrument</code> value
     * @throws ConnectionException if an error occurs connecting to the server
     */
	public void deleteInstrumentInfo(BrokerID inBrokerID, Instrument inInstrument);
    /**
     * Gets the instrument info associated with the current instrument and Broker.
     *
     * @param inBrokerID a <code>BrokerID</code> value
	 * @param inInstrument a <code>Instrument</code> value
     * @return a <code>InstrumentInfo</code> value
     * @throws ConnectionException if an error occurs connecting to the server
     */
    public InstrumentInfo getInstrumentInfo(BrokerID inBrokerID, Instrument inInstrument)
            throws ConnectionException;
    
    /**
     * Returns the server's broker status.
     *
     * @return The status.
     *
     * @throws ConnectionException Thrown if the operation cannot be
     * completed.
     */
    public BrokersStatus getBrokersStatus() throws ConnectionException;

    /**
     * Returns the information of the user with the given ID. A local
     * cache can be used to avoid frequent server roundtrips, but it
     * may return stale information. The cache is updated whether or
     * not it was used for retrieval.
     * <p>
     * All cached values are cleared when the client is {@link #close() closed}.
     *
     * @param id The user ID.
     * @param useCache True if the local cache should be used.
     *
     * @return The information.
     *
     * @throws ConnectionException Thrown if the operation cannot be
     * completed.
     */
    public UserInfo getUserInfo(UserID id,
                         boolean useCache) throws ConnectionException;
    
    /**
     * Adds the given report to the system data flow.
     * 
     * <p>Reports added this way will be added to the system data bus. Reports will be
     * persisted and become part of the system record. All clients will receive this
     * report.
     * 
     * <p><em>This will affect reported positions</em></p>.
     *
     * @param inReport a <code>FIXMessageWrapper</code> value
     * @param inBrokerID a <code>BrokerID</code> value
     * @param inHierarchy a <code>Hierarchy</code> value
     * @throws ConnectionException if an error occurs connecting to the server
     */
    public void addReport(FIXMessageWrapper inReport,
                   BrokerID inBrokerID, Hierarchy inHierarchy)
            throws ConnectionException;

    /**
     * Removes the given report from the persistent report store.
     * 
     * <p>Reports removed this way will not be added to the system data bus and no clients
     * will receive this report.
     * 
     * <p><em>This will affect reported positions</em></p>.
     *
     * @param inReport an <code>ExecutionReportImpl</code> value
     * @throws ConnectionException if an error occurs connecting to the server
     */
    public void deleteReport(ExecutionReportImpl inReport)
            throws ConnectionException;
    
    /**
     * Sets the user data associated with the current user.
     *
     * @param inProperties a <code>Properties</code> value
     * @throws ConnectionException if an error occurs connecting to the server
     */
    public void setUserData(Properties inProperties)
            throws ConnectionException;
    
    /**
     * Gets the user data associated with the current user.
     *
     * @return a <code>Properties</code> value
     * @throws ConnectionException if an error occurs connecting to the server
     */
    public Properties getUserData()
            throws ConnectionException;
    
    /**
     * Find the root order ID for the order chain of the given order ID.
     *
     * @param inOrderID an <code>OrderID</code> value
     * @return an <code>OrderID</code> value
     * @throws ConnectionException if an error occurs connecting to the server
     */
    public OrderID getRootOrderID(OrderID inOrderID)
            throws ConnectionException;
    
    /**
     * Fetches the next ID base from server.
     *
     * @return the next ID base from server.
     * @throws ConnectionException if an error occurs connecting to the server
     */
	public String getNextServerID()
            throws ConnectionException;
	
    /**
     * Returns true if client has a live connection to the server.
     *
     * @return true, if the connection to the server is alive.
     */
    public boolean isRunning();

    /**
     * Disconnects the connection to the server and reconnects back
     * using the properties supplied to this method.
     *
     * @param inParameters The parameters to use when reconnecting to the
     * server. These parameters are stored so that subsequent invocations
     * of {@link #reconnect()} will use these parameters instead of the
     * ones that were supplied when creating this instance.
     * 
     * @throws ConnectionException if there were errors reconnecting.
     */
    public void reconnect(ClientParameters inParameters) throws ConnectionException;

    /**
     * Returns true if the supplied user name, password match the
     * credentials used to connect to the server.
     * <p>
     * This method returns false if the client is not connected to
     * the server.
     *
     * @param inUsername the username
     * @param inPassword the password
     *
     * @return true, if the supplied credentials match the ones used to
     * authenticate to the server and the client is connected to the server,
     * false otherwise.
     */
    public boolean isCredentialsMatch(String inUsername, char[] inPassword);

    /**
     * Closes the connection to the server. The behavior of any of
     * the methods of this class after this method is invoked is undefined.
     */
    public void close();
}
