package org.marketcetera.client.ws;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import javax.jws.WebParam;
import javax.jws.WebService;
import javax.xml.bind.annotation.XmlSeeAlso;

import org.marketcetera.brokers.BrokersStatus;
import org.marketcetera.core.position.PositionKey;
import org.marketcetera.core.position.impl.PositionKeyImpl;
import org.marketcetera.core.time.Period;
import org.marketcetera.event.Quote;
import org.marketcetera.event.QuoteEvent;
import org.marketcetera.event.info.InstrumentInfo;
import org.marketcetera.info.UserInfo;
import org.marketcetera.trade.*;
import org.marketcetera.util.misc.ClassVersion;
import org.marketcetera.util.ws.stateful.ClientContext;
import org.marketcetera.util.ws.stateful.ServiceBase;
import org.marketcetera.util.ws.wrappers.DateWrapper;
import org.marketcetera.util.ws.wrappers.MapWrapper;
import org.marketcetera.util.ws.wrappers.RemoteException;

/**
 * The application's web services.
 */
@WebService(targetNamespace="http://marketcetera.org/services")
@ClassVersion("$Id: WSService.java 16888 2014-04-22 18:32:36Z colin $")
@XmlSeeAlso({PositionKeyImpl.class})
public interface WSService
    extends ServiceBase
{
    /**
     * Returns all the reports (execution report and order cancel
     * rejects) generated and received by the server since the
     * supplied date to the client with the given context.
     *
     * @param context The context.
     * @param date The date, in UTC.
     * 
     * @return The reports.
     * @throws RemoteException Thrown if the operation cannot be
     * completed.
     */
    ReportBaseImpl[] getReportsSince(
    		@WebParam(name="context")ClientContext context,
    		@WebParam(name="date")DateWrapper date)
        throws RemoteException;
    
    /**
     * Gets the list of open orders visible to the current user.
     *
     * @param context a <code>ClientContext</code> value
     * 
     * @return a <code>List&lt;ReportBaseImpl&gt;</code> value
     * @throws RemoteException if an error occurs
     */
    List<ReportBaseImpl> getOpenOrders(
    		@WebParam(name="context")ClientContext context)
            throws RemoteException;
    
    /**
     * Returns the position of the supplied instrument based on reports,
     * generated and received on or before the supplied date in UTC to the
     * client with the given context.
     *
     * @param context The context.
     * @param date The date, in UTC.
     * @param instrument The instrument.
     * 
     * @return The position.
     * @throws RemoteException Thrown if the operation cannot be
     * completed.
     */
    BigDecimal getPositionAsOf(
    		@WebParam(name="context")ClientContext context,
    		@WebParam(name="date")DateWrapper date,
    		@WebParam(name="instrument")Instrument instrument)
        throws RemoteException;
    
    /**
     * Returns all the open positions for instruments type based on reports,
     * generated and received on or before the supplied date in UTC to the client
     * with the given context.
     *
     * @param context The context.
     * @param date The date, in UTC.
     * @param securityType The security type.
     * 
     * @return The open positions.
     * @throws RemoteException Thrown if the operation cannot be
     * completed.
     */
    MapWrapper<PositionKey,BigDecimal> getPositionsAsOf(
    		@WebParam(name="context")ClientContext context,
    		@WebParam(name="date")DateWrapper date,
    		@WebParam(name="type")SecurityType securityType)
        throws RemoteException;
    
    /**
     * Returns all the open positions based on reports,
     * generated and received on or before the supplied date in UTC to the client
     * with the given context.
     *
     * @param context The context.
     * @param date The date, in UTC.
     * 
     * @return The open positions.
     * @throws RemoteException Thrown if the operation cannot be
     * completed.
     */
    MapWrapper<PositionKey,BigDecimal> getAllPositionsAsOf(
    		@WebParam(name="context")ClientContext context,
    		@WebParam(name="date")DateWrapper date)
        throws RemoteException;
    
    /**
     * Returns the server's broker status to the client with the
     * given context.
     *
     * @param context The context.
     * 
     * @return The status.
     * @throws RemoteException Thrown if the operation cannot be
     * completed.
     */
    BrokersStatus getBrokersStatus(
    		@WebParam(name="context") ClientContext context)
        throws RemoteException;

    /**
     * Returns the information of the user with the given ID to the
     * client with the given context.
     *
     * @param context The context.
     * @param id The user ID.
     * 
     * @return The information.
     * @throws RemoteException Thrown if the operation cannot be
     * completed.
     */
    UserInfo getUserInfo(
    		@WebParam(name="context")ClientContext context,
    		@WebParam(name="userID")UserID id)
        throws RemoteException;

    /**
     * Adds the given report to the system data flow.
     * 
     * <p>Reports added this way will be added to the system data bus. Reports will be
     * persisted and become part of the system record. All clients will receive this
     * report.
     * 
     * <p><em>This will affect reported positions</em></p>.
     *
     * @param context a <code>ClientContent</code> value
     * @param report a <code>FIXMessageWrapper</code> value
     * @param brokerID a <code>BrokerID</code> value
     * @param hierarchy a <code>Hierarchy</code> value
     * 
     * @throws RemoteException if an error occurs
     */
    void addReport(
    		@WebParam(name="context")ClientContext context,
    		@WebParam(name="report")FIXMessageWrapper report,
    		@WebParam(name="brokerID")BrokerID brokerID,
    		@WebParam(name="hierarchy")Hierarchy hierarchy)
            throws RemoteException;
    
    /**
     * Removes the given report from the persistent report store.
     * 
     * <p>Reports removed this way will not be added to the system data bus and no clients
     * will receive this report.
     * 
     * <p><em>This will affect reported positions</em></p>.
     *
     * @param context a <code>ClientContent</code> value
     * @param report an <code>ExecutionReportImpl</code> value
     * 
     * @throws RemoteException if an error occurs
     */
    void deleteReport(
    		@WebParam(name="context")ClientContext context,
    		@WebParam(name="report")ExecutionReportImpl report)
            throws RemoteException;

    /**
     * Gets the user data associated with the current user. 
     *
     * @param context a <code>ClientContent</code> value
     * 
     * @return a <code>String</code> value
     * @throws RemoteException if the operation cannot be completed
     */
    String getUserData(
    		@WebParam(name="context")ClientContext context)
            throws RemoteException;
    
    /**
     * Sets the user data associated with the current user.
     *
     * @param context a <code>ClientContent</code> value
     * @param data a <code>String</code> value 
     * 
     * @throws RemoteException if the operation cannot be completed
     */
    void setUserData(
    		@WebParam(name="context")ClientContext context,
    		@WebParam(name="userData")String data)
            throws RemoteException;
    
    /**
     * Returns all the market data quotes in the server from to the supplied dates in UTC.
     *
     * @param context The context.
     * @param inInstrument The instrument of the request.
     * @param inPeriod The period of the quotes.
     * @param inFrom The date from which to start in UTC. Cannot be null.
     * @param inTo The date to which to stop in UTC. Cannot be null.
     * 
     * @return All the quotes between the supplied dates, may be empty.
     * @throws ConnectionException if there were connection errors fetching
     * data from the server.
     */
	List<Quote> getQuoteHistory(
			@WebParam(name="context")ClientContext context,
			@WebParam(name="instrument")Instrument instrument,
			@WebParam(name="period")Period period,		
			@WebParam(name="fromDate")DateWrapper fromDate,
			@WebParam(name="toDate")DateWrapper toDate)
        throws RemoteException;
	
	/**
     * Returns the available instruments supported by the Broker.
     * 
     * @param context The context.
     * @param brokerID The broker ID of the request.
     * 
     * @return All the instruments supported by the broker.
     */
	Set<Instrument> getInstruments(
			@WebParam(name="context")ClientContext context,
			@WebParam(name="brokerID")BrokerID brokerID)
            throws RemoteException;
	
	/**
     * Adds the given quote event to the system data flow.
     * 
     * <p>Market data added this way will be added to the system data bus. Events will be
     * persisted and become part of the system record. All clients will receive this
     * event.
     * 
     * <p><em>This will affect reported data history</em></p>.
     *
     * @param context a <code>ClientContent</code> value
     * @param quote a <code>QuoteEvent</code> value
     * 
     * @throws RemoteException if an error occurs
     */
    void addQuote(
    		@WebParam(name="context")ClientContext context,
            @WebParam(name="event")QuoteEvent quote)
            throws RemoteException;
    
    /**
     * Removes the given quote event from the persistent db store.
     * 
     * <p>Market data removed this way will not be added to the system data bus and no clients
     * will receive this event.
     * 
     * <p><em>This will affect reported data history</em></p>.
     *
     * @param context a <code>ClientContent</code> value
     * @param quote an <code>QuoteEvent</code> value
     * 
     * @throws RemoteException if an error occurs
     */
    void deleteQuote(
    		@WebParam(name="context")ClientContext context,
            @WebParam(name="event")QuoteEvent quote)
            throws RemoteException;

    /**
     * Gets the instrument info associated with the current instrument and Broker.
     *
     * @param context a <code>ClientContent</code> value
     * @param brokerID a <code>BrokerID</code> value
	 * @param instrument a <code>Instrument</code> value
	 * 
     * @return a <code>InstrumentInfo</code> value
     * @throws RemoteException if the operation cannot be completed
     */
    InstrumentInfo getInstrumentInfo(
    		@WebParam(name="context")ClientContext context,
    		@WebParam(name="brokerID")BrokerID brokerID, 
    		@WebParam(name="instrument")Instrument instrument)
            throws RemoteException;
    
    /**
     * Deletes the instrument info associated with the current instrument and Broker.
     *
     * @param context a <code>ClientContent</code> value
     * @param brokerID a <code>BrokerID</code> value
	 * @param instrument a <code>Instrument</code> value
	 * 
     * @return a <code>InstrumentInfo</code> value
     * @throws RemoteException if the operation cannot be completed
     */
    void deleteInstrumentInfo(
    		@WebParam(name="context")ClientContext context,
    		@WebParam(name="brokerID")BrokerID brokerID, 
    		@WebParam(name="instrument")Instrument instrument)
            throws RemoteException;
    
    /**
     * Sets the instrument info associated with the current instrument and
     * Broker.
     *
     * @param context a <code>ClientContent</code> value
     * @param brokerID a <code>BrokerID</code> value
	 * @param instrument a <code>Instrument</code> value
     * @param info a <code>InstrumentInfo</code> value
     * 
     * @throws RemoteException if the operation cannot be completed
     */
    void setInstrumentInfo(
    		@WebParam(name="context")ClientContext context,
    		@WebParam(name="brokerID")BrokerID brokerID, 
    		@WebParam(name="instrument")Instrument instrument,
    		@WebParam(name="info")InstrumentInfo info)
            throws RemoteException;
	
    /**
     * Gets the order ID of the root order in the given order chain.
     *
     * @param context a <code>ClientContext</code> value
     * @param orderID an <code>OrderID</code> value
     * 
     * @return an <code>OrderID</code> value
     * @throws RemoteException if an error occurs 
     */
    OrderID getRootOrderID(
    		@WebParam(name="context")ClientContext context,
    		@WebParam(name="orderId")OrderID orderID)
            throws RemoteException;
    
    /**
     * Returns the next server order ID to the client with the given
     * context.
     *
     * @param context The context.
     * 
     * @return The next order ID.
     * @throws RemoteException Thrown if the operation cannot be
     * completed.
     */
    String getNextServerID(
    		@WebParam(name="context")ClientContext context)
        throws RemoteException;

    /**
     * Sends a heartbeat to the server.
     * @param context The context.
     *
     * @throws RemoteException Thrown if the operation cannot be
     * completed.
     */
    void heartbeat(
    		@WebParam(name="context")ClientContext context)
        throws RemoteException;
}
