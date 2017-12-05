package org.marketcetera.client.ws;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang.ObjectUtils;
import org.marketcetera.brokers.BrokerStatus;
import org.marketcetera.brokers.BrokerStatusListener;
import org.marketcetera.brokers.BrokersStatus;
import org.marketcetera.core.Util;
import org.marketcetera.core.notifications.ConnectionStatusListener;
import org.marketcetera.core.position.PositionKey;
import org.marketcetera.core.time.Period;
import org.marketcetera.event.Event;
import org.marketcetera.event.Quote;
import org.marketcetera.event.QuoteEvent;
import org.marketcetera.event.info.InstrumentInfo;
import org.marketcetera.info.UserInfo;
import org.marketcetera.marketdata.GenericDataRequest;
import org.marketcetera.marketdata.MarketDataCancel;
import org.marketcetera.marketdata.MarketDataReject;
import org.marketcetera.marketdata.MarketDataRequest;
import org.marketcetera.module.RequestID;
import org.marketcetera.trade.BrokerID;
import org.marketcetera.trade.ExecutionReport;
import org.marketcetera.trade.ExecutionReportImpl;
import org.marketcetera.trade.FIXMessageWrapper;
import org.marketcetera.trade.Hierarchy;
import org.marketcetera.trade.Instrument;
import org.marketcetera.trade.Order;
import org.marketcetera.trade.OrderCancelReject;
import org.marketcetera.trade.OrderID;
import org.marketcetera.trade.ReportBase;
import org.marketcetera.trade.ReportBaseImpl;
import org.marketcetera.trade.SecurityType;
import org.marketcetera.trade.UserID;
import org.marketcetera.trade.validation.Validations;
import org.marketcetera.util.except.ExceptUtils;
import org.marketcetera.util.log.I18NBoundMessage1P;
import org.marketcetera.util.log.SLF4JLoggerProxy;
import org.marketcetera.util.ws.stateful.ClientContext;
import org.marketcetera.util.ws.wrappers.DateWrapper;
import org.marketcetera.util.ws.wrappers.RemoteException;
import org.marketcetera.ws.ValidationException;
import org.marketcetera.ws.client.ConnectionException;
import org.marketcetera.client.ws.jms.MessageHandler;
import org.marketcetera.ws.RequestMessage;
import org.marketcetera.ws.ResponseMessage;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * Provides common behavior for <code>WSClient</code> implementations.
 */ 
public abstract class AbstractWSClient 
	implements WSClient
{
    // INSTANCE DATA.
    /**
     * market data requests collection
     */
    private final Map<RequestID, MarketDataRequest> mServerRequests = Maps.newHashMap();
    /**
     * report listeners collection
     */
    private final Deque<ReportListener> mReportListeners = Lists.newLinkedList();
    /**
     * event listeners collection
     */
    private final Deque<EventListener> mEventListeners = Lists.newLinkedList();
    /**
     * broker status listeners collection
     */
    private final Deque<BrokerStatusListener> mBrokerStatusListeners = Lists.newLinkedList();
    /**
     * connection status listeners collection
     */
    private final Deque<ConnectionStatusListener> mConnectionStatusListeners = Lists.newLinkedList();
    /**
     * indicates if the connection is active or not
     */
    protected final AtomicBoolean mRunning = new AtomicBoolean(false);
    /**
     * indicates if the scheduler for the re-send market data requests
     */
    private final ScheduledExecutorService mRequestExecutor = Executors.newSingleThreadScheduledExecutor();
    /**
     * indicates the delay for re-sending the market data server (in ms)
     */
    private static final long SERVER_RESEND_DELAY = 10000;
    /**
     * The traffic tag for this instance
     */
    protected static final String TRAFFIC = AbstractWSClient.class.getPackage().
            getName() + ".traffic";  //$NON-NLS-1$

    
	/**
	 * The clients context
	 */
    protected abstract ClientContext getClientContext();
    
    /**
     * Web service
     */
	protected abstract WSService getWebService();
    
	/**
	 * Converting and sending the request message
	 * 
	 * @param inRequest The request
	 */
	protected abstract void convertAndSend(RequestMessage inRequest);
    
	/**
     * Checks to see if the client is closed and fails if the client
     * is closed.
     *
     * @throws IllegalStateException if the client is closed.
     */
    protected abstract void failIfClosed() throws IllegalStateException;
	
    /**
	 * Returns the Report Listeners 
	 */
	protected final Deque<ReportListener> getReportListeners(){
		return mReportListeners;
	}
	
	/**
	 * Returns the Event Listeners 
	 */
	protected final Deque<EventListener> getEventListeners(){
		return mEventListeners;
	}
	
	/**
	 * Returns the BrokerStatus Listeners 
	 */
	protected final Deque<BrokerStatusListener> getBrokerStatusListeners(){
		return mBrokerStatusListeners;
	}
	
	/**
	 * Returns the ServerStatus Listeners 
	 */
	protected final Deque<ConnectionStatusListener> getConnectionStatusListeners(){
		return mConnectionStatusListeners;
	}
	
    /**
     * Create a new AbstractWSClient instance.
     */
    protected AbstractWSClient()
    {
    }
    
    /* (non-Javadoc)
     * @see org.marketcetera.client.ws.WSClient#send(org.marketcetera.ws.RequestMessage)
     */
	@Override
	public final void send(RequestMessage inRequest) throws ConnectionException, ValidationException {
		failIfDisconnected();
		if(inRequest instanceof Order){
    		Order inTrade = (Order)inRequest;
	        
    		Validations.validateTrade(inTrade);
	        convertAndSend(inTrade);
    	} else if(inRequest instanceof GenericDataRequest){
    		GenericDataRequest inData = (GenericDataRequest)inRequest;
    		
    		Validations.validateData(inData);
            convertAndSend(inData);
            
            //Track the requests
            if(inData instanceof MarketDataRequest){
            	mServerRequests.put(inData.getRequestID(), (MarketDataRequest)inData);
            }else if(inData instanceof MarketDataCancel){
            	mServerRequests.remove(inData.getRequestID());
            }
    	} else {
    		throw new ValidationException(new I18NBoundMessage1P(
                    Messages.VALIDATION_UNKNOWN_REQUEST, inRequest.toString()));
    	}
	}
	
    /* (non-Javadoc)
     * @see org.marketcetera.client.ws.WSClient#reSendData()
     */
	@Override
	public void reSendData() {
		mRequestExecutor.schedule(new Runnable() {
			@Override
			public void run() {
				for(MarketDataRequest inData : mServerRequests.values()) {
		        	try {
		        		Validations.validateData(inData);
		            	convertAndSend(inData);
		    		} catch (ConnectionException | ValidationException e) {}
		        }
			}
		}, SERVER_RESEND_DELAY, TimeUnit.MILLISECONDS);
    }
	
    /* (non-Javadoc)
     * @see org.marketcetera.client.ws.WSClient#addReportListener(org.marketcetera.client.ws.ReportListener)
     */
	@Override
	public final void addReportListener(ReportListener inListener) {
		failIfDisconnected();
        if(inListener == null) {
            throw new NullPointerException();
        }
        synchronized (mReportListeners) {
            mReportListeners.addFirst(inListener);
        }
	}
	
    /* (non-Javadoc)
     * @see org.marketcetera.client.ws.WSClient#removeReportListener(org.marketcetera.client.ws.ReportListener)
     */
	@Override
	public final void removeReportListener(ReportListener inListener) {
		failIfDisconnected();
        if(inListener == null) {
            throw new NullPointerException();
        }
        synchronized (mReportListeners) {
            mReportListeners.removeFirstOccurrence(inListener);
        }
	}
	
    /* (non-Javadoc)
     * @see org.marketcetera.client.ws.WSClient#addEventListener(org.marketcetera.client.ws.EventListener)
     */
	@Override
	public final void addEventListener(EventListener inListener) {
		failIfDisconnected();
        if(inListener == null) {
            throw new NullPointerException();
        }
        synchronized (mEventListeners) {
            mEventListeners.addFirst(inListener);
        }
	}
	
    /* (non-Javadoc)
     * @see org.marketcetera.client.ws.WSClient#removeEventListener(org.marketcetera.client.ws.EventListener)
     */
	@Override
	public final void removeEventListener(EventListener inListener) {
		failIfDisconnected();
        if(inListener == null) {
            throw new NullPointerException();
        }
        synchronized (mEventListeners) {
            mEventListeners.removeFirstOccurrence(inListener);
        }
	}
	
    /* (non-Javadoc)
     * @see org.marketcetera.client.ws.WSClient#addBrokerStatusListener(org.marketcetera.brokers.BrokerStatusListener)
     */
	@Override
	public final void addBrokerStatusListener(BrokerStatusListener inListener) {
		failIfDisconnected();
        if(inListener == null) {
            throw new NullPointerException();
        }
        synchronized (mBrokerStatusListeners) {
            mBrokerStatusListeners.addFirst(inListener);
            BrokersStatus brokersStatus = getBrokersStatus();
            inListener.asyncUpdateBrokers(brokersStatus);
        }
	}
	
    /* (non-Javadoc)
     * @see org.marketcetera.client.ws.WSClient#removeBrokerStatusListener(org.marketcetera.brokers.BrokerStatusListener)
     */
	@Override
	public final void removeBrokerStatusListener(BrokerStatusListener inListener) {
		failIfDisconnected();
        if(inListener == null) {
            throw new NullPointerException();
        }
        synchronized (mBrokerStatusListeners) {
            mBrokerStatusListeners.removeFirstOccurrence(inListener);
        }
	}
	
    /* (non-Javadoc)
     * @see org.marketcetera.client.ws.WSClient#addConnectionStatusListener(org.marketcetera.core.notifications.ConnectionStatusListener)
     */
	@Override
	public final void addConnectionStatusListener(ConnectionStatusListener inListener) {
		failIfDisconnected();
        if(inListener == null) {
            throw new NullPointerException();
        }
        synchronized (mConnectionStatusListeners) {
            mConnectionStatusListeners.addFirst(inListener);
        }
        inListener.receiveConnectionStatus(isRunning());
	}
	
    /* (non-Javadoc)
     * @see org.marketcetera.client.ws.WSClient#removeConnectionStatusListener(org.marketcetera.core.notifications.ConnectionStatusListener)
     */
	@Override
	public final void removeConnectionStatusListener(ConnectionStatusListener inListener) {
		failIfDisconnected();
        if(inListener == null) {
            throw new NullPointerException();
        }
        synchronized (mConnectionStatusListeners) {
            mConnectionStatusListeners.removeFirstOccurrence(inListener);
        }
	}
	
	
    /* (non-Javadoc)
     * @see org.marketcetera.client.ws.WSClient#getReportsSince(java.util.Date)
     */
	@Override
	public final ReportBase[] getReportsSince(Date inDate) throws ConnectionException {
        failIfDisconnected();
        try {
            ReportBaseImpl[] reports = getWebService().getReportsSince(getClientContext(),new DateWrapper(inDate));
            return reports == null ? new ReportBase[0] : reports;
        } catch (RemoteException ex) {
            throw new ConnectionException(ex,Messages.ERROR_REMOTE_EXECUTION);
        }
	}
	
    /* (non-Javadoc)
     * @see org.marketcetera.client.ws.WSClient#getOpenOrders()
     */
	@Override
	public final List<ReportBaseImpl> getOpenOrders() throws ConnectionException {
        failIfDisconnected();
        try {
            return getWebService().getOpenOrders(getClientContext());
        } catch (RemoteException ex) {
            throw new ConnectionException(ex,
                                          Messages.ERROR_REMOTE_EXECUTION);
        }
	}
	
    /* (non-Javadoc)
     * @see org.marketcetera.client.ws.WSClient#getPositionAsOf(java.util.Date, org.marketcetera.trade.Instrument)
     */
	@Override
	public final BigDecimal getPositionAsOf(Date inDate, Instrument inInstrument) throws ConnectionException {
        failIfDisconnected();
        try {
            return getWebService().getPositionAsOf
                (getClientContext(),new DateWrapper(inDate),inInstrument);
        } catch (RemoteException ex) {
            throw new ConnectionException(ex,Messages.ERROR_REMOTE_EXECUTION);
        }
	}
	
    /* (non-Javadoc)
     * @see org.marketcetera.client.ws.WSClient#getPositionsAsOf(java.util.Date, org.marketcetera.trade.SecurityType)
     */
	@Override
	public final Map<PositionKey, BigDecimal> getPositionsAsOf(Date inDate, SecurityType inSecurityType)
			throws ConnectionException {
        failIfDisconnected();
        try {
            return getWebService().getPositionsAsOf
                (getClientContext(),new DateWrapper(inDate), inSecurityType).getMap();
        } catch (RemoteException ex) {
            throw new ConnectionException(ex,Messages.ERROR_REMOTE_EXECUTION);
        }
	}
	
    /* (non-Javadoc)
     * @see org.marketcetera.client.ws.WSClient#getAllPositionsAsOf(java.util.Date)
     */
	@Override
	public final Map<PositionKey, BigDecimal> getAllPositionsAsOf(Date inDate) throws ConnectionException {
        failIfDisconnected();
        try {
            return getWebService().getAllPositionsAsOf
                (getClientContext(),new DateWrapper(inDate)).getMap();
        } catch (RemoteException ex) {
            throw new ConnectionException(ex,Messages.ERROR_REMOTE_EXECUTION);
        }
	}
	
    /* (non-Javadoc)
     * @see org.marketcetera.client.ws.WSClient#getQuoteHistory(org.marketcetera.trade.Instrument, org.marketcetera.core.time.Period, java.util.Date, java.util.Date)
     */
	@Override
	public final List<Quote> getQuoteHistory(Instrument inInstrument, Period inPeriod, Date inFrom, Date inTo)
			throws ConnectionException {
        failIfDisconnected();
        try {
        	List<Quote> history = getWebService().getQuoteHistory(getClientContext(), inInstrument, 
        			inPeriod, new DateWrapper(inFrom), new DateWrapper(inTo));
            return history;
        } catch (RemoteException ex) {
            throw new ConnectionException(ex,Messages.ERROR_REMOTE_EXECUTION);
        }
	}
	
    /* (non-Javadoc)
     * @see org.marketcetera.client.ws.WSClient#getInstruments(org.marketcetera.trade.BrokerID)
     */
	@Override
	public final Set<Instrument> getInstruments(BrokerID inBrokerID) throws ConnectionException {
        failIfDisconnected();
        try {
            return getWebService().getInstruments(getClientContext(), inBrokerID);
        } catch (RemoteException ex) {
            throw new ConnectionException(ex,
                                          Messages.ERROR_REMOTE_EXECUTION);
        }
	}
	
    /* (non-Javadoc)
     * @see org.marketcetera.client.ws.WSClient#addQuote(org.marketcetera.event.impl.QuoteEvent)
     */
	@Override
	public final void addQuote(QuoteEvent inMarketData) throws ConnectionException {
        failIfDisconnected();
        try {
            getWebService().addQuote(getClientContext(),inMarketData);
        } catch (RemoteException ex) {
            throw new ConnectionException(ex,
                                          Messages.ERROR_REMOTE_EXECUTION);
        }
	}
	
    /* (non-Javadoc)
     * @see org.marketcetera.client.ws.WSClient#deleteQuote(org.marketcetera.event.impl.QuoteEvent)
     */
	@Override
	public final void deleteQuote(QuoteEvent inMarketData) throws ConnectionException {
        failIfDisconnected();
        try {
            getWebService().deleteQuote(getClientContext(),inMarketData);
        } catch (RemoteException ex) {
            throw new ConnectionException(ex,
                                          Messages.ERROR_REMOTE_EXECUTION);
        }
	}
	
	/* (non-Javadoc)
     * @see org.marketcetera.client.ws.WSClient#setInstrumentInfo(org.marketcetera.trade.BrokerID, org.marketcetera.trade.Instrument, org.marketcetera.event.info.InstrumentInfo)
     */
	@Override
	public final void setInstrumentInfo(BrokerID inBrokerID, Instrument inInstrument, InstrumentInfo inInfo)
			throws ConnectionException {
        failIfDisconnected();
        try {
            getWebService().setInstrumentInfo(getClientContext(),
            		inBrokerID, inInstrument, inInfo);
        } catch (RemoteException ex) {
            throw new ConnectionException(ex,
                                          Messages.ERROR_REMOTE_EXECUTION);
        }
	}
	
    /* (non-Javadoc)
     * @see org.marketcetera.client.ws.WSClient#deleteInstrumentInfo(org.marketcetera.trade.BrokerID, org.marketcetera.trade.Instrument)
     */
	@Override
	public final void deleteInstrumentInfo(BrokerID inBrokerID, Instrument inInstrument) throws ConnectionException {
        failIfDisconnected();
        try {
            getWebService().deleteInstrumentInfo(getClientContext(), inBrokerID, inInstrument);
        } catch (RemoteException ex) {
            throw new ConnectionException(ex,
                                          Messages.ERROR_REMOTE_EXECUTION);
        }
	}
	
    /* (non-Javadoc)
     * @see org.marketcetera.client.ws.WSClient#getInstrumentInfo(org.marketcetera.trade.BrokerID, org.marketcetera.trade.Instrument)
     */
	@Override
	public final InstrumentInfo getInstrumentInfo(BrokerID inBrokerID, Instrument inInstrument) throws ConnectionException {
        failIfDisconnected();
        try {
            return getWebService().getInstrumentInfo(getClientContext(), inBrokerID, inInstrument);
        } catch (RemoteException ex) {
            throw new ConnectionException(ex,
                                          Messages.ERROR_REMOTE_EXECUTION);
        }
	}
	
    /* (non-Javadoc)
     * @see org.marketcetera.client.ws.WSClient#getBrokersStatus()
     */
	@Override
	public final BrokersStatus getBrokersStatus() throws ConnectionException {
        failIfDisconnected();
        try {
            return getWebService().getBrokersStatus(getClientContext());
        } catch (RemoteException ex) {
            throw new ConnectionException(ex,Messages.ERROR_REMOTE_EXECUTION);
        }
	}
	
    /* (non-Javadoc)
     * @see org.marketcetera.client.ws.WSClient#getUserInfo(org.marketcetera.trade.UserID, Boolean)
     */
	@Override
	public final UserInfo getUserInfo(UserID id, boolean useCache) throws ConnectionException {
        failIfDisconnected();
        UserInfo result;
        try {
            result=getWebService().getUserInfo(getClientContext(),id);
        } catch (RemoteException ex) {
            throw new ConnectionException
                (ex,Messages.ERROR_REMOTE_EXECUTION);
        }
        return result;
	}
	
    /* (non-Javadoc)
     * @see org.marketcetera.client.ws.WSClient#addReport(org.marketcetera.trade.FIXMessageWrapper, org.marketcetera.trade.BrokerID, org.marketcetera.trade.Hierarchy)
     */
	@Override
	public final void addReport(FIXMessageWrapper inReport, BrokerID inBrokerID, Hierarchy inHierarchy)
			throws ConnectionException {
        failIfDisconnected();
        try {
            getWebService().addReport(getClientContext(),
                               inReport,
                               inBrokerID,
                               inHierarchy);
        } catch (RemoteException ex) {
            throw new ConnectionException(ex,
                                          Messages.ERROR_REMOTE_EXECUTION);
        }
	}
	
    /* (non-Javadoc)
     * @see org.marketcetera.client.ws.WSClient#deleteReport(org.marketcetera.trade.ExecutionReportImpl)
     */
	@Override
	public final void deleteReport(ExecutionReportImpl inReport) throws ConnectionException {
        failIfDisconnected();
        try {
            getWebService().deleteReport(getClientContext(),inReport);
        } catch (RemoteException ex) {
            throw new ConnectionException(ex,
                                          Messages.ERROR_REMOTE_EXECUTION);
        }
	}
	
    /* (non-Javadoc)
     * @see org.marketcetera.client.ws.WSClient#setUserData(java.util.Properties)
     */
	@Override
	public final void setUserData(Properties inProperties) throws ConnectionException {
        failIfDisconnected();
        try {
        	getWebService().setUserData(getClientContext(),
                                 Util.propertiesToString(inProperties));
        } catch (RemoteException ex) {
            throw new ConnectionException(ex,
                                          Messages.ERROR_REMOTE_EXECUTION);
        }
	}
	
    /* (non-Javadoc)
     * @see org.marketcetera.client.ws.WSClient#getUserData()
     */
	@Override
	public final Properties getUserData() throws ConnectionException {
        failIfDisconnected();
        try {
            return Util.propertiesFromString(getWebService().getUserData(getClientContext()));
        } catch (RemoteException ex) {
            throw new ConnectionException(ex,
                                          Messages.ERROR_REMOTE_EXECUTION);
        }
	}
	
    /* (non-Javadoc)
     * @see org.marketcetera.client.ws.WSClient#getRootOrderID(org.marketcetera.trade.OrderID)
     */
	@Override
	public final OrderID getRootOrderID(OrderID inOrderID) throws ConnectionException {
        failIfDisconnected();
        try {
            return getWebService().getRootOrderID(getClientContext(),
                                               inOrderID);
        } catch (RemoteException ex) {
            throw new ConnectionException(ex,
                                          Messages.ERROR_REMOTE_EXECUTION);
        }
	}

	/* (non-Javadoc)
     * @see org.marketcetera.client.ws.WSClient#getNextServerID()
     */
	@Override
	public String getNextServerID() throws ConnectionException {
		failIfDisconnected();
		try {
			return getWebService().getNextServerID(getClientContext());
        } catch (RemoteException ex) {
            throw new ConnectionException(ex,
                                          Messages.ERROR_REMOTE_EXECUTION);
        }
    }
	
    /* (non-Javadoc)
     * @see org.springframework.context.Lifecycle#isRunning()
     */
    @Override
    public boolean isRunning()
    {
        return mRunning.get();
    }
    
    /**
     * Fails if the connection to the client is closed or disconnected.
     *
     * @throws ConnectionException if the connection to the client is closed or disconnected.
     */
    protected final void failIfDisconnected()
            throws ConnectionException
    {
        if(!isRunning()) {
            throw new ConnectionException(Messages.CLIENT_DISCONNECTED);
        }
    }

    /**
     *Client messaging receiver
     */
    protected class ServerMessageReceiver
    			implements MessageHandler<ResponseMessage>
	{
	    @Override
	    public void receiveMessage
	        (ResponseMessage inMessage)
	    {
	        if (inMessage instanceof ExecutionReport) {
	            notifyExecutionReport((ExecutionReport)inMessage);
	        } else if (inMessage instanceof OrderCancelReject) {
	            notifyCancelReject((OrderCancelReject)inMessage);
	        } else if(inMessage instanceof BrokerStatus){
	        	notifyBrokerStatus((BrokerStatus)inMessage);
	        } else if (inMessage instanceof Event) {
	        	notifyDataEvent((Event)inMessage);
	        } else if(inMessage instanceof MarketDataReject){
	        	notifyDataReject((MarketDataReject)inMessage);
	        } else {
	            Messages.LOG_RECEIVED_FIX_REPORT.warn
	                (this,ObjectUtils.toString(inMessage));
	        }
	    }
	}
    
	/**
	 * Notify of ExecutionReport
	 */
	void notifyExecutionReport(ExecutionReport inReport) {
        SLF4JLoggerProxy.debug(TRAFFIC, "Received Exec Report:{}", inReport);  //$NON-NLS-1$
        synchronized (mReportListeners) {
            for(ReportListener listener: mReportListeners) {
                try {
                    listener.receiveExecutionReport(inReport);
                } catch (Throwable t) {
                    Messages.LOG_ERROR_RECEIVE_EXEC_REPORT.warn(this, t,
                            ObjectUtils.toString(inReport));
                    ExceptUtils.interrupt(t);
                }
            }
        }
    }
	
	/**
	 * Notify of OrderCancelReject
	 */
	void notifyCancelReject(OrderCancelReject inReport) {
        SLF4JLoggerProxy.debug(TRAFFIC, "Received Cancel Reject:{}", inReport);  //$NON-NLS-1$
        synchronized (mReportListeners) {
            for(ReportListener listener: mReportListeners) {
                try {
                    listener.receiveCancelReject(inReport);
                } catch (Throwable t) {
                    Messages.LOG_ERROR_RECEIVE_CANCEL_REJECT.warn(this, t,
                            ObjectUtils.toString(inReport));
                    ExceptUtils.interrupt(t);
                }
            }
        }
    }
	
	/**
	 * Notify of BrokerStatus
	 */
    void notifyBrokerStatus(BrokerStatus inStatus) {
        SLF4JLoggerProxy.debug
            (TRAFFIC,"Received Broker Status:{}",inStatus); //$NON-NLS-1$
        synchronized (mBrokerStatusListeners) {
            for (BrokerStatusListener listener:
                     mBrokerStatusListeners) {
                try {
                	listener.receiveBrokerStatus(inStatus);
                	listener.asyncUpdateBrokers(getBrokersStatus());
                } catch (Throwable t) {
                    Messages.LOG_ERROR_RECEIVE_BROKER_STATUS.warn(this, t,
                            ObjectUtils.toString(inStatus));
                    ExceptUtils.interrupt(t);
                }
            }
        }
    }
    
	/**
	 * Notify of MarketDataEvent
	 */
    void notifyDataEvent(Event inEvent) {
        SLF4JLoggerProxy.debug(TRAFFIC, "Received Data Event:{}", inEvent);  //$NON-NLS-1$
        synchronized (mEventListeners) {
            for(EventListener listener: mEventListeners) {
                try {
                    listener.receiveDataEvent(inEvent);
                } catch (Throwable t) {
                    Messages.LOG_ERROR_RECEIVE_MARKET_DATA.warn(this, t,
                            ObjectUtils.toString(inEvent));
                    ExceptUtils.interrupt(t);
                }
            }
        }
    }
    
	/**
	 * Notify of ServerStatus
	 */
    void notifyDataReject(MarketDataReject inReject) {
        SLF4JLoggerProxy.debug(TRAFFIC, "Received Data Reject:{}", inReject);  //$NON-NLS-1$
        synchronized (mEventListeners) {
            for(EventListener listener: mEventListeners) {
                try {
                    listener.receiveDataReject(inReject);
                } catch (Throwable t) {
                    Messages.LOG_ERROR_RECEIVE_DATA_REJECT.warn(this, t,
                            ObjectUtils.toString(inReject));
                    ExceptUtils.interrupt(t);
                }
            }
        }
    }
	
	/**
	 * Notify of ServerStatus
	 */
	protected void notifyServerStatus(boolean inStatus) {
        SLF4JLoggerProxy.debug
            (TRAFFIC,"Received Server Status:{}", inStatus); //$NON-NLS-1$
        synchronized (mBrokerStatusListeners) {
            for (BrokerStatusListener listener:
                     mBrokerStatusListeners) {
                try {
                	if(!inStatus){
                		listener.asyncUpdateBrokers(new BrokersStatus());
                	}else{
                		listener.asyncUpdateBrokers(getBrokersStatus());
                	}
                } catch (Throwable t) {
                    Messages.LOG_ERROR_RECEIVE_BROKER_STATUS.warn(this, t,
                            ObjectUtils.toString(inStatus));
                    ExceptUtils.interrupt(t);
                }
            }
        }
        synchronized (mConnectionStatusListeners) {
            for (ConnectionStatusListener listener:
                     mConnectionStatusListeners) {
                try {
                    listener.receiveConnectionStatus(inStatus);
                } catch (Throwable t) {
                    Messages.LOG_ERROR_RECEIVE_SERVER_STATUS.warn(this, t,
                    		inStatus);
                    ExceptUtils.interrupt(t);
                }
            }
        }
    }
}
