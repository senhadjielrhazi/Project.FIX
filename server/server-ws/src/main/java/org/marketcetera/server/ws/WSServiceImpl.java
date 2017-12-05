package org.marketcetera.server.ws;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.jws.WebParam;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.marketcetera.server.ws.brokers.Brokers;
import org.marketcetera.brokers.BrokersStatus;
import org.marketcetera.client.ws.WSService;
import org.marketcetera.core.CoreException;
import org.marketcetera.core.IDFactory;
import org.marketcetera.core.Util;
import org.marketcetera.core.position.PositionKey;
import org.marketcetera.core.time.Period;
import org.marketcetera.event.Quote;
import org.marketcetera.event.QuoteEvent;
import org.marketcetera.event.info.InstrumentInfo;
import org.marketcetera.info.UserInfo;
import org.marketcetera.server.ws.history.HistoryService;
import org.marketcetera.persist.PersistenceException;
import org.marketcetera.trade.*;
import org.marketcetera.util.log.SLF4JLoggerProxy;
import org.marketcetera.util.misc.ClassVersion;
import org.marketcetera.util.ws.stateful.*;
import org.marketcetera.util.ws.wrappers.DateWrapper;
import org.marketcetera.util.ws.wrappers.MapWrapper;
import org.marketcetera.util.ws.wrappers.RemoteException;
import org.marketcetera.server.ws.security.ClientSession;
import org.marketcetera.ws.server.security.SimpleUser;
import org.marketcetera.ws.server.security.SingleSimpleUserQuery;

import quickfix.Message;

/**
 * The implementation of the application's web services.
 */
@ClassVersion("$Id: WSServiceImpl.java 16664 2013-08-23 23:06:00Z colin $")
public class WSServiceImpl
    extends ServiceBaseImpl<ClientSession>
    implements WSService
{
    // INSTANCE DATA.
    private final Brokers mBrokers;
    private final IDFactory mIDFactory;
    private final HistoryService mHistoryService;

    // CONSTRUCTORS.
    /**
     * Creates a new service implementation with the given session
     * manager, brokers, and history services provider.
     *
     * @param sessionManager The session manager, which may be null.
     * @param brokers The brokers.
     * @param idFactory the ID factory.
     * @param historyService The history service provider.
     */
    public WSServiceImpl(SessionManager<ClientSession> sessionManager,
                       Brokers brokers, IDFactory idFactory,
                       HistoryService historyService)
    {
        super(sessionManager);
        mBrokers=brokers;
        mIDFactory=idFactory;
        mHistoryService=historyService;
    }
    
    // INSTANCE METHODS.
    /**
     * Returns the receiver's brokers.
     *
     * @return The brokers.
     */
    private Brokers getBrokers()
    {
        return mBrokers;
    }

    /**
     * Returns the receiver's ID factory.
     *
     * @return The factory.
     */
    public IDFactory getIDFactory()
    {
        return mIDFactory;
    }

    /**
     * Returns the receiver's history service provider.
     *
     * @return The provider.
     */
    public HistoryService getHistoryService()
    {
        return mHistoryService;
    }

	// Service IMPLEMENTATIONS.
    /**
     * Returns all the reports (execution report and order cancel
     * rejects) generated and received by the server since the
     * supplied date to the client with the given context.
     *
     * @param inSession The session.
     * @param date The date, in UTC.
     * 
     * @return The reports.
     * @throws PersistenceException Thrown if the operation cannot be
     * completed.
     */
    private ReportBaseImpl[] getReportsSinceImpl
        (ClientSession inSession, Date date)
        throws PersistenceException
    {
        return getHistoryService().getReportsSince
            (inSession.getUser(),date);
    }

    /**
     * Returns the open orders visible to the given user.
     *
     * @param inSession a <code>ClientSession</code> value
     * 
     * @return a <code>List&lt;ReportBaseImpl&gt;</code> value
     * @throws PersistenceException if an error occurs retrieving the order data
     */
    private List<ReportBaseImpl> getOpenOrdersImpl(ClientSession inSession)
            throws PersistenceException
    {
        return getHistoryService().getOpenOrders(inSession.getUser());
    }
    
    /**
     * Gets the position for the given instrument.
     *
     * @param inSession a <code>ClientSession</code> value
     * @param date a <code>Date</code> value
     * @param instrument a <code>Future</code> value
     * 
     * @return a <code>BigDecimal</code> value
     * @throws PersistenceException if an error occurs
     */
    private BigDecimal getPositionAsOfImpl
    (ClientSession inSession, Date date, Instrument instrument) 
    		throws PersistenceException
     {
        return getHistoryService().getPositionAsOf(inSession.getUser(), date, instrument);
     }
    
    /**
     * Gets all positions as of the given date for given security type.
     *
     * @param inSession a <code>ClientSession</code> value
     * @param date a <code>Date</code> value
     * @param securityType a <code>SecurityType</code> value
     * 
     * @return a <code>MapWrapper&lt;PositionKey&lt;Instrument&gt;,BigDecimal&gt;</code> value
     * @throws PersistenceException if an error occurs
     */
    private MapWrapper<PositionKey, BigDecimal> getPositionsAsOfImpl
    (ClientSession inSession, Date date, SecurityType securityType)
    		throws PersistenceException
    {
        return new MapWrapper<PositionKey, BigDecimal>(
                getHistoryService().getPositionsAsOf(inSession.getUser(), date, securityType));
    }
    
    /**
     * Gets all positions as of the given date.
     *
     * @param inSession a <code>ClientSession</code> value
     * @param date a <code>Date</code> value
     * 
     * @return a <code>MapWrapper&lt;PositionKey&lt;Instrument&gt;,BigDecimal&gt;</code> value
     * @throws PersistenceException if an error occurs
     */
    private MapWrapper<PositionKey, BigDecimal> getAllPositionsAsOfImpl
    (ClientSession inSession, Date date)
    		throws PersistenceException
    {
        return new MapWrapper<PositionKey, BigDecimal>(
                getHistoryService().getAllPositionsAsOf(
                		inSession.getUser(), date));
    }
    
    /**
     * Returns the server's broker status to the client with the
     * given context.
     *
     * @param clientSession a <code>ClientSession</code> value
     * 
     * @return The status.
     */
    private BrokersStatus getBrokersStatusImpl(ClientSession clientSession)
    {
        return getBrokers().getStatus(clientSession.getUser().getName());
    }
    
    /**
     * Returns the information of the user with the given ID to the
     * client with the given context.
     *
     * @param id a <code>UserID</code> value
     * 
     * @return The information.
     * @throws PersistenceException Thrown if the operation cannot be
     * completed.
     */
	private UserInfo getUserInfoImpl(UserID id) 
			throws PersistenceException {
		SimpleUser u = (new SingleSimpleUserQuery(id.getValue())).fetch();
		return new UserInfo(u.getName(), u.getUserID(), u.isActive(), u.isSuperuser(),
				Util.propertiesFromString(u.getUserData()));
	}
    
    /**
     * Adds the given message to the system data bus.
     *
     * @param report a <code>Message</code> value
     * @param brokerID a <code>BrokerID</code> value
     * @param clientSession a <code>ClientSession</code> value
     * 
     * @throws PersistenceException if the report could not be added
     */
    private void addReportImpl(Message report, BrokerID brokerID,
                           ClientSession clientSession)
            throws PersistenceException
    {
        SLF4JLoggerProxy.debug(this,
                               "Received {} from({}) for {} to add", //$NON-NLS-1$
                               report, clientSession, brokerID);
        try {
            ExecutionReport newReport = Factory.getInstance().createExecutionReport(report,
                                                                                    brokerID,
                                                                                    Originator.Broker,
                                                                                    clientSession.getUser().getUserID());
            ServerFIXRouting.getInstance().getServerReceiver().addReport(newReport);
        } catch (Exception e) {
            Messages.CANNOT_ADD_REPORT.warn(this, clientSession, brokerID,
                                            ExceptionUtils.getRootCauseMessage(e));
            throw new PersistenceException(e);
        }
    }
    
    /**
     * Deletes the given report from the system persistence.
     *
     * @param report an <code>ExecutionReport</code> value
     * @param clientSession a <code>ClientSession</code> value
     * 
     * @throws PersistenceException if the report cannot be deleted 
     */
    private void deleteReportImpl(ExecutionReport report, ClientSession clientSession)
            throws PersistenceException
    {
        SLF4JLoggerProxy.debug(this,
                               "Received {} from user ({}) to delete", //$NON-NLS-1$
                               report, clientSession);
        try {
            ServerFIXRouting.getInstance().getServerReceiver().deleteReport(report);
        } catch (Exception e) {
            Messages.CANNOT_DELETE_REPORT.warn(this, clientSession,
                                               ExceptionUtils.getRootCauseMessage(e));
            throw new PersistenceException(e);
        }
    }
    
    /**
     * Gets the user data associated with the given username.
     *
     * @param clientSession a <code>ClientSession</code> value
     * 
     * @return a <code>String</code> value
     * @throws PersistenceException if an error occurs retrieving the user data
     */
    private String getUserDataImpl(ClientSession clientSession)
            throws PersistenceException
    {
        return new SingleSimpleUserQuery(clientSession.getUser().getName()).fetch().getUserData();
    }
    
    /**
     * Sets the user data associated with the given username.
     *
     * @param clientSession a <code>ClientSession</code> value
     * @param inUserData a <code>String</code> value
     * 
     * @throws PersistenceException if an error occurs saving the user data
     */
    private void setUserDataImpl(ClientSession clientSession, String inUserData)
            throws PersistenceException
    {
        SimpleUser user = new SingleSimpleUserQuery(clientSession.getUser().getName()).fetch();
        user.setUserData(inUserData);
        user.save();
    }
    
    /**
     * Returns all the market data quotes in the server from to the supplied dates in UTC.
     *
     * @param inInstrument The instrument of the request.
     * @param inPeriod The period of the quotes.
     * @param inFrom The date from which to start in UTC. Cannot be null.
     * @param inTo The date to which to stop in UTC. Cannot be null.
     *  
     * @return All the quotes between the supplied dates, may be empty.
     * @throws PersistenceException if there were connection errors fetching
     * data from the server.
     */
	protected List<Quote> getQuoteHistoryImpl(Instrument instrument, Period period, 
			Date inFrom, Date inTo) 
			   		 throws PersistenceException 
	{
		return getHistoryService().getQuoteHistory(instrument, period, inFrom, inTo);
	}
	
	/**
     * Returns the available instruments supported by the Broker.
     * 
     * @param brokerID The broker ID of the request.
     * 
     * @return All the instruments supported by the broker.
     * @throws PersistenceException if the operation cannot be completed
     */
	protected Set<Instrument> getInstrumentsImpl(BrokerID brokerID) 
	   		 throws PersistenceException 
	{
		return getHistoryService().getInstruments(brokerID);
	}
	
	 /**
     * Adds the given message to the system data bus.
     *
     * @param quote a <code>QuoteEvent</code> value
     * @param clientSession a <code>ClientSession</code> value
     * 
     * @throws PersistenceException if the event could not be added
     */
    protected void addQuoteImpl(QuoteEvent quote, ClientSession clientSession) {
    	SLF4JLoggerProxy.debug(this,
                "Received {} from ({}) to add", //$NON-NLS-1$
                quote, clientSession);
    	try {
    		ServerFIXRouting.getInstance().getServerReceiver().addQuote(quote);
    	} catch (Exception e) {
    		Messages.CANNOT_ADD_QUOTE.warn(this,
    								  clientSession,
                             		  ExceptionUtils.getRootCauseMessage(e));
    		throw new PersistenceException(e);
    	}
    }
    
    /**
     * Deletes the given report from the system persistence.
     *
     * @param quote an <code>QuoteEvent</code> value
     * @param clientSession a <code>ClientSession</code> value
     * 
     * @throws PersistenceException if the event cannot be deleted 
     */
    protected void deleteQuoteImpl(QuoteEvent quote, ClientSession clientSession) {
    	 SLF4JLoggerProxy.debug(this,
                 "Received {} from ({}) to delete", //$NON-NLS-1$
                 quote, clientSession);
		try {
			ServerFIXRouting.getInstance().getServerReceiver().deleteQuote(quote);
		} catch (Exception e) {
			Messages.CANNOT_DELETE_QUOTE.warn(this,
										 clientSession,
		                                 ExceptionUtils.getRootCauseMessage(e));
			throw new PersistenceException(e);
		}
	}
    
    /**
     * Gets the instrument info associated with the current instrument and Broker.
     *
     * @param brokerID a <code>BrokerID</code> value
	 * @param instrument a <code>Instrument</code> value
	 * 
     * @return a <code>InstrumentInfo</code> value
     * @throws PersistenceException if the operation cannot be completed
     */
	protected InstrumentInfo getInstrumentInfoImpl(BrokerID brokerID, Instrument instrument) 
   		 throws PersistenceException 
	{
		return getHistoryService().getInstrumentInfo(brokerID, instrument);
	}
	
    /**
     * Deletes the instrument info associated with the current instrument and
     * Broker.
     *
     * @param brokerID a <code>BrokerID</code> value
	 * @param instrument a <code>Instrument</code> value
     * 
     * @throws PersistenceException if the operation cannot be completed
     */
    protected void deleteInstrumentInfoImpl(BrokerID brokerID, Instrument instrument) 
   		 throws PersistenceException 
    {
    	getHistoryService().deleteInstrumentInfo(brokerID, instrument);
	}
    
    /**
     * Sets the instrument info associated with the current instrument and
     * Broker.
     *
     * @param brokerID a <code>BrokerID</code> value
	 * @param instrument a <code>Instrument</code> value
     * @param info a <code>InstrumentInfo</code> value
     * 
     * @throws PersistenceException if the operation cannot be completed
     */
    protected void setInstrumentInfoImpl(BrokerID brokerID, Instrument instrument, InstrumentInfo info) 
   		 throws PersistenceException 
    {
    	getHistoryService().setInstrumentInfo(brokerID, instrument, info);
	}
    
    /**
     * Gets the root order ID for the given order ID.
     *
     * @param inOrderID an <code>OrderID</code> value
     * 
     * @return an <code>OrderID</code> value or <code>null</code>
     * @throws PersistenceException if there were errors retrieving the
     * OrderId.
     */
    public OrderID getRootOrderIDImpl(OrderID inOrderID)
    		 throws PersistenceException 
    {
    	return getHistoryService().getRootOrderID(inOrderID);
    }
    
    /**
     * Returns the next server order ID to the client with the given
     * context.
     *
     * @return The next order ID.
     * @throws CoreException Thrown if the operation cannot be
     * completed.
     */
	private String getNextOrderIDImpl() 
			throws CoreException 
	{
		return getIDFactory().getNext();
	}
    
    // Service.
    
    /* (non-Javadoc)
     * @see org.marketcetera.client.ws.WSService#getReportsSince(org.marketcetera.util.ws.stateful.ClientContext, org.marketcetera.util.ws.wrappers.DateWrapper)
     */
    @Override
    public ReportBaseImpl[] getReportsSince(
    		final @WebParam(name="context")ClientContext context,
    		final @WebParam(name="date")DateWrapper date)
    				throws RemoteException
    {
        return (new RemoteCaller<ClientSession,ReportBaseImpl[]>
                (getSessionManager()) {
            @Override
            protected ReportBaseImpl[] call
                (ClientContext context,
                 SessionHolder<ClientSession> sessionHolder)
                throws PersistenceException
            {
                return getReportsSinceImpl
                    (sessionHolder.getSession(),date.getRaw());
            }}).execute(context);
    }
    
    /* (non-Javadoc)
     * @see org.marketcetera.client.ws.WSService#getOpenOrders(org.marketcetera.util.ws.stateful.ClientContext)
     */
    @Override
    public List<ReportBaseImpl> getOpenOrders(
    		final @WebParam(name="context")ClientContext context)
    				throws RemoteException
    {
        return (new RemoteCaller<ClientSession,List<ReportBaseImpl>>(getSessionManager()) {
            @Override
            protected List<ReportBaseImpl> call(ClientContext context,
                                            SessionHolder<ClientSession> sessionHolder)
                    throws PersistenceException
            {
                return getOpenOrdersImpl(sessionHolder.getSession());
        }}).execute(context);
    }

    /* (non-Javadoc)
     * @see org.marketcetera.client.ws.WSService#getPositionAsOf(org.marketcetera.util.ws.stateful.ClientContext, org.marketcetera.util.ws.wrappers.DateWrapper, org.marketcetera.trade.Instrument)
     */
    @Override
    public BigDecimal getPositionAsOf(
    		final @WebParam(name="context")ClientContext context,
    		final @WebParam(name="date")DateWrapper date,
    		final @WebParam(name="instrument")Instrument instrument)
    				throws RemoteException
    {
        return (new RemoteCaller<ClientSession,BigDecimal>
                (getSessionManager()) {
            @Override
            protected BigDecimal call
                (ClientContext context,
                 SessionHolder<ClientSession> sessionHolder)
                throws PersistenceException
            {
                return getPositionAsOfImpl
                    (sessionHolder.getSession(), date.getRaw(), instrument);
            }}).execute(context);
    }
    
    /* (non-Javadoc)
     * @see org.marketcetera.client.ws.WSService#getAllPositionsAsOf(org.marketcetera.util.ws.stateful.ClientContext, org.marketcetera.util.ws.wrappers.DateWrapper, org.marketcetera.trade.SecurityType)
     */
    @Override
    public MapWrapper<PositionKey, BigDecimal> getPositionsAsOf(
    		final @WebParam(name="context")ClientContext context,
    		final @WebParam(name="date")DateWrapper date,
    		final @WebParam(name="type")SecurityType securityType)
    				throws RemoteException
    {
        return (new RemoteCaller<ClientSession,MapWrapper<PositionKey,
                                                          BigDecimal>>
                (getSessionManager()) {
            @Override
            protected MapWrapper<PositionKey,BigDecimal> call
                (ClientContext context,
                 SessionHolder<ClientSession> sessionHolder)
                throws PersistenceException
            {
                return getPositionsAsOfImpl
                    (sessionHolder.getSession(), date.getRaw(), securityType);
            }}).execute(context);
    }
    
    /* (non-Javadoc)
     * @see org.marketcetera.client.ws.WSService#getAllPositionsAsOf(org.marketcetera.util.ws.stateful.ClientContext, org.marketcetera.util.ws.wrappers.DateWrapper)
     */
    @Override
    public MapWrapper<PositionKey, BigDecimal> getAllPositionsAsOf(
    		final @WebParam(name="context")ClientContext context,
    		final @WebParam(name="date")DateWrapper date)
    				throws RemoteException
    {
        return (new RemoteCaller<ClientSession,MapWrapper<PositionKey,
                                                          BigDecimal>>
                (getSessionManager()) {
            @Override
            protected MapWrapper<PositionKey,BigDecimal> call
                (ClientContext context,
                 SessionHolder<ClientSession> sessionHolder)
                throws PersistenceException
            {
                return getAllPositionsAsOfImpl
                    (sessionHolder.getSession(), date.getRaw());
            }}).execute(context);
    }
    
    /* (non-Javadoc)
     * @see org.marketcetera.client.ws.WSService#getBrokersStatus(org.marketcetera.util.ws.stateful.ClientContext)
     */
    @Override
    public BrokersStatus getBrokersStatus(
    		final @WebParam(name="context")ClientContext context)
    				throws RemoteException
    {
        return (new RemoteCaller<ClientSession,BrokersStatus>
                (getSessionManager()) {
            @Override
            protected BrokersStatus call
                (ClientContext context,
                 SessionHolder<ClientSession> sessionHolder)
            {
                return getBrokersStatusImpl(sessionHolder.getSession());
            }}).execute(context);
    }
    
    /* (non-Javadoc)
     * @see org.marketcetera.client.ws.WSService#getUserInfo(org.marketcetera.util.ws.stateful.ClientContext, org.marketcetera.trade.UserID)
     */
    @Override
    public UserInfo getUserInfo(
    		final @WebParam(name="context")ClientContext context,
    		final @WebParam(name="userID")UserID id)
    				throws RemoteException
    {
        return (new RemoteCaller<ClientSession,UserInfo>
                (getSessionManager()) {
            @Override
            protected UserInfo call
                (ClientContext context,
                 SessionHolder<ClientSession> sessionHolder)
                throws PersistenceException
            {
                return getUserInfoImpl(id);
            }}).execute(context);
    }
    
    /* (non-Javadoc)
     * @see org.marketcetera.client.ws.WSService#addReport(org.marketcetera.util.ws.stateful.ClientContext, quickfix.Message, org.marketcetera.trade.BrokerID, org.marketcetera.trade.Hierarchy)
     */
    @Override
    public void addReport(
    		final @WebParam(name="context")ClientContext context,
    		final @WebParam(name="report")FIXMessageWrapper report,
    		final @WebParam(name="brokerID")BrokerID brokerID,
    		final @WebParam(name="hierarchy")Hierarchy hierarchy)
    				throws RemoteException
    {
        (new RemoteRunner<ClientSession>(getSessionManager()) {
            @Override
            protected void run(ClientContext context,
                               SessionHolder<ClientSession> sessionHolder)
                    throws PersistenceException
            {
                addReportImpl(report.getMessage(), brokerID, sessionHolder.getSession());
            }}).execute(context);
    }
    
    /* (non-Javadoc)
     * @see org.marketcetera.client.ws.WSService#deleteReport(org.marketcetera.util.ws.stateful.ClientContext, org.marketcetera.trade.ExecutionReportImpl)
     */
    @Override
    public void deleteReport(
    		final @WebParam(name="context")ClientContext context,
    		final @WebParam(name="report")ExecutionReportImpl report)
    				throws RemoteException
    {
        (new RemoteRunner<ClientSession>(getSessionManager()) {
            @Override
            protected void run(ClientContext context,
                               SessionHolder<ClientSession> sessionHolder)
                    throws PersistenceException
            {
                deleteReportImpl(report, sessionHolder.getSession());
            }}).execute(context);
    }
    
    /* (non-Javadoc)
     * @see org.marketcetera.client.ws.WSService#getUserData(org.marketcetera.util.ws.stateful.ClientContext)
     */
    @Override
    public String getUserData(
    		final @WebParam(name="context")ClientContext context)
    				throws RemoteException
    {
        String userData = (new RemoteCaller<ClientSession,String>(getSessionManager()) {
            @Override
            protected String call(ClientContext context,
                                  SessionHolder<ClientSession> sessionHolder)
                    throws CoreException, PersistenceException
            {
                return getUserDataImpl(sessionHolder.getSession());
        }}).execute(context);
        return userData;
    }
    
    /* (non-Javadoc)
     * @see org.marketcetera.client.ws.WSService#setUserData(org.marketcetera.util.ws.stateful.ClientContext, java.util.Properties)
     */
    @Override
    public void setUserData(
    		final @WebParam(name="context")ClientContext context, 
    		final @WebParam(name="userData")String data)
    				throws RemoteException
    {
        (new RemoteRunner<ClientSession>(getSessionManager()) {
            @Override
            protected void run(ClientContext context,
                               SessionHolder<ClientSession> sessionHolder)
                    throws PersistenceException
            {
                setUserDataImpl(sessionHolder.getSession(), data);
            }}).execute(context);
    }
    
    /* (non-Javadoc)
     * @see org.marketcetera.client.ws.WSService#getQuoteHistory(org.marketcetera.util.ws.stateful.ClientContext, org.marketcetera.trade.Instrument, org.marketcetera.core.time.Period, java.util.Date, java.util.Date)
     */
	@Override
	public List<Quote> getQuoteHistory(
			final @WebParam(name="context")ClientContext context, 
			final @WebParam(name="instrument")Instrument instrument, 
			final @WebParam(name="period")Period period,
			final @WebParam(name="fromDate")DateWrapper fromDate, 
			final @WebParam(name="toDate")DateWrapper toDate)
					throws RemoteException 
	{
		return (new RemoteCaller<ClientSession, List<Quote>>(getSessionManager()) {
			@Override
			protected List<Quote> call(ClientContext context,
					SessionHolder<ClientSession> sessionHolder) throws PersistenceException {
				return getQuoteHistoryImpl(instrument, period, fromDate.getRaw(), toDate.getRaw());
			}
		}).execute(context);
	}
    
	/* (non-Javadoc)
     * @see org.marketcetera.client.ws.WSService#getInstruments(org.marketcetera.util.ws.stateful.ClientContext, org.marketcetera.trade.BrokerID)
     */
	@Override
	public Set<Instrument> getInstruments(
			final @WebParam(name="context")ClientContext context, 
			final @WebParam(name="brokerID")BrokerID brokerID)
					throws RemoteException 
	{
		return (new RemoteCaller<ClientSession, Set<Instrument>>(getSessionManager()) {
			@Override
			protected Set<Instrument> call(ClientContext context,
					SessionHolder<ClientSession> sessionHolder) throws PersistenceException {
				return getInstrumentsImpl(brokerID);
			}
		}).execute(context);
	}

	/* (non-Javadoc)
     * @see org.marketcetera.client.ws.WSService#addQuote(org.marketcetera.util.ws.stateful.ClientContext, org.marketcetera.event.impl.QuoteEvent)
     */
	@Override
	public void addQuote(
			final @WebParam(name="context")ClientContext context, 
			final @WebParam(name="event")QuoteEvent marketData)
					throws RemoteException 
	{
        (new RemoteRunner<ClientSession>(getSessionManager()) {
            @Override
            protected void run(ClientContext context,
                               SessionHolder<ClientSession> sessionHolder)
                    throws PersistenceException
            {
            	addQuoteImpl(marketData, sessionHolder.getSession());
            }}).execute(context);
	}
	
	/* (non-Javadoc)
     * @see org.marketcetera.client.ws.WSService#deleteQuote(org.marketcetera.util.ws.stateful.ClientContext, org.marketcetera.event.impl.QuoteEvent)
     */
	@Override
	public void deleteQuote(
			final @WebParam(name="context")ClientContext context, 
			final @WebParam(name="event")QuoteEvent marketData)
					throws RemoteException 
	{
		(new RemoteRunner<ClientSession>(getSessionManager()) {
            @Override
            protected void run(ClientContext context,
                               SessionHolder<ClientSession> sessionHolder)
                    throws PersistenceException
            {
            	deleteQuoteImpl(marketData, sessionHolder.getSession());
            }}).execute(context);
	}

	/* (non-Javadoc)
     * @see org.marketcetera.client.ws.WSService#getInstrumentInfo(org.marketcetera.util.ws.stateful.ClientContext, org.marketcetera.trade.BrokerID, org.marketcetera.trade.Instrument)
     */
	@Override
	public InstrumentInfo getInstrumentInfo(
			final @WebParam(name="context")ClientContext context, 
			final @WebParam(name="brokerID")BrokerID brokerID, 
			final @WebParam(name="instrument")Instrument instrument)
					throws RemoteException 
	{
		return (new RemoteCaller<ClientSession,InstrumentInfo>(getSessionManager()) {
            @Override
            protected InstrumentInfo call(ClientContext context,
                                  SessionHolder<ClientSession> sessionHolder)
                    throws PersistenceException
            {
                return getInstrumentInfoImpl(brokerID, instrument);
        }}).execute(context);
	}

	/* (non-Javadoc)
     * @see org.marketcetera.client.ws.WSService#deleteInstrumentInfo(org.marketcetera.util.ws.stateful.ClientContext, org.marketcetera.trade.BrokerID, org.marketcetera.trade.Instrument)
     */
	@Override
	public void deleteInstrumentInfo(
			final @WebParam(name="context")ClientContext context, 
			final @WebParam(name="brokerID")BrokerID brokerID, 
			final @WebParam(name="instrument")Instrument instrument)
					throws RemoteException 
	{
		(new RemoteRunner<ClientSession>(getSessionManager()) {
            @Override
            protected void run(ClientContext context,
                                  SessionHolder<ClientSession> sessionHolder)
                    throws PersistenceException
            {
            	deleteInstrumentInfoImpl(brokerID, instrument);
        }}).execute(context);
	}
	
	/* (non-Javadoc)
     * @see org.marketcetera.client.ws.WSService#setInstrumentInfo(org.marketcetera.util.ws.stateful.ClientContext, org.marketcetera.trade.BrokerID, org.marketcetera.trade.Instrument, org.marketcetera.event.info.InstrumentInfo)
     */
	@Override
	public void setInstrumentInfo(
			final @WebParam(name="context")ClientContext context, 
			final @WebParam(name="brokerID")BrokerID brokerID, 
			final @WebParam(name="instrument")Instrument instrument, 
			final @WebParam(name="info")InstrumentInfo info)
					throws RemoteException 
	{
		(new RemoteRunner<ClientSession>(getSessionManager()) {
            @Override
            protected void run(ClientContext context,
                               SessionHolder<ClientSession> sessionHolder)
                    throws PersistenceException
            {
            	setInstrumentInfoImpl(brokerID, instrument, info);
            }}).execute(context);
	}

	/* (non-Javadoc)
     * @see org.marketcetera.client.ws.WSService#getRootOrderID(org.marketcetera.util.ws.stateful.ClientContext, org.marketcetera.trade.OrderID)
     */
	@Override
	public OrderID getRootOrderID(
			final @WebParam(name="context")ClientContext context, 
			final @WebParam(name="orderId")OrderID inOrderID)
					throws RemoteException 
	{
		return (new RemoteCaller<ClientSession, OrderID>(getSessionManager()) {
			@Override
			protected OrderID call(ClientContext context, SessionHolder<ClientSession> sessionHolder)
					throws PersistenceException {
				return getRootOrderIDImpl(inOrderID);
			}
		}).execute(context);
	}
	
	/* (non-Javadoc)
     * @see org.marketcetera.client.ws.WSService#getNextOrderID(org.marketcetera.util.ws.stateful.ClientContext)
     */
    @Override
    public String getNextServerID(
    		final @WebParam(name="context")ClientContext context)
    				throws RemoteException
    {
        return (new RemoteCaller<ClientSession,String>
                (getSessionManager()) {
            @Override
            protected String call
                (ClientContext context,
                 SessionHolder<ClientSession> sessionHolder)
                throws CoreException
            {
                return getNextOrderIDImpl();
            }}).execute(context);
    }

    /* (non-Javadoc)
     * @see org.marketcetera.client.ws.WSService#heartbeat(org.marketcetera.util.ws.stateful.ClientContext)
     */
    @Override
    public void heartbeat(
    		final @WebParam(name="context")ClientContext context)
    				throws RemoteException
    {
        (new RemoteRunner<ClientSession>
         (getSessionManager()) {
            @Override
            protected void run
                (ClientContext context,
                 SessionHolder<ClientSession> sessionHolder)
            {
                // The enclosing RemoteRunner takes care of marking
                // the session as active.
                SLF4JLoggerProxy.debug
                    (this,"Received heartbeat for: {}", //$NON-NLS-1$
                     context.getSessionId());
            }}).execute(context);
    }
}
