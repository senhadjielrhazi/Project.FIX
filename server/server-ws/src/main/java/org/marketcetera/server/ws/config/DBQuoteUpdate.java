package org.marketcetera.server.ws.config;

import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.marketcetera.client.ws.jms.RequestEnvelope;
import org.marketcetera.core.time.Period;
import org.marketcetera.event.QuoteEvent;
import org.marketcetera.marketdata.Content;
import org.marketcetera.marketdata.MarketDataRequest;
import org.marketcetera.marketdata.MarketDataRequestBuilder;
import org.marketcetera.quickfix.FIXMessageUtil;
import org.marketcetera.server.ws.RequestHandler;
import org.marketcetera.server.ws.ServerFIXRouting;
import org.marketcetera.server.ws.brokers.Broker;
import org.marketcetera.server.ws.history.HistoryService;
import org.marketcetera.symbol.InstrumentSymbolResolver;
import org.marketcetera.trade.Instrument;
import org.marketcetera.util.except.I18NException;
import org.marketcetera.util.ws.tags.SessionId;
import org.marketcetera.util.ws.wrappers.MapWrapper;
import org.marketcetera.ws.MarshallerFactory;
import org.marketcetera.ws.server.BasicJob;
import org.marketcetera.ws.server.CoreExecutors;
import org.springframework.beans.factory.InitializingBean;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import quickfix.Message;
import quickfix.field.XmlData;
import quickfix.field.XmlDataLen;

public class DBQuoteUpdate implements BasicJob, InitializingBean {
	
	/**
     * indicates how frequently to check for market data server (in ms)
     */
    private static final long BROKER_CHECK_FREQUENCY = 1000;
    
	// INSTANCE DATA.
	private HistoryService mHistoryService;
	private List<Instrument> mInstruments;
	private Broker mFIXBroker;
	private Broker mAPIBroker;
	private Period mPeriod;
	private Period mHistoryBack;
    
	/**
	 * Manages the loading of the live data
	 */
	private static final AtomicBoolean sFIXTask = new AtomicBoolean();
	
	/**
	 * Manages the loading of the histry data
	 */
	private static final AtomicBoolean sAPITask = new AtomicBoolean();
	
	
	// CONSTRUCTORS.	
	/**
	 * Empty constructor for Spring
	 */
	public DBQuoteUpdate()
	{
	}
	
    // INSTANCE METHODS.
	/**
     * Sets the receiver's history service provider to the
     * given one. A non-null value should be set during the receiver's
     * initialization.
     *
     * @param reportHistoryService The provider.
     */
    public void setHistoryService
        (HistoryService historyService)
    {
        mHistoryService=historyService;
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
    
    /**
     * Sets the list of instruments.
     *
     * @param instruments a map with full symbol and securityType.
     */
    public void setInstruments
        (Map<String, String> instruments)
    {
    	mInstruments = Lists.newArrayList();
    	for(Entry<String, String> entry:instruments.entrySet()){
    		Instrument instrument = InstrumentSymbolResolver.resolveSymbol(entry.getValue(), entry.getKey()); 
    		mInstruments.add(instrument);
    	}
    }

    /**
     * Returns the instruments.
     *
     * @return The list.
     */
    public List<Instrument> getInstruments()
    {
        return mInstruments;
    }
    
    /**
     * Sets the FIX broker.
     *
     * @param broker.
     */
    public void setFIXBroker
        (Broker broker)
    {
    	mFIXBroker=broker;
    }

    /**
     * Gets the FIX broker.
     *
     * @return The broker.
     */
    public Broker getFIXBroker()
    {
        return mFIXBroker;
    }
    
    /**
     * Sets the API broker.
     *
     * @param broker.
     */
    public void setAPIBroker
        (Broker broker)
    {
    	mAPIBroker=broker;
    }

    /**
     * Gets the API broker.
     *
     * @return The broker.
     */
    public Broker getAPIBroker()
    {
        return mAPIBroker;
    }
    
    /**
     * Sets the quote period.
     *
     * @param period.
     */
    public void setPeriod
        (String period)
    {
    	mPeriod=Period.valueOf(period);
    }

    /**
     * Gets the quote period.
     *
     * @return The period.
     */
    public Period getPeriod()
    {
        return mPeriod;
    }
    
    /**
     * Sets the history back to be loaded.
     *
     * @param length of history.
     */
    public void setHistoryBack
        (String historyBack)
    {
    	mHistoryBack=Period.valueOf(historyBack);
    }

    /**
     * Gets the history back to be loaded.
     *
     * @return The length of history.
     */
    public Period getHistoryBack()
    {
        return mHistoryBack;
    }
    
    // InitializingBean.
	@Override
	public void afterPropertiesSet() throws Exception 
	{
		if (getHistoryService()==null) {
            throw new I18NException(Messages.NO_REPORT_HISTORY_SERVICE);
		}
		if (getInstruments()==null) {
            throw new I18NException(Messages.NO_INSTRUMENTS);
		}
		if (getFIXBroker()==null) {
            throw new I18NException(Messages.NO_BROKER);
		}
		if (getAPIBroker()==null) {
            throw new I18NException(Messages.NO_BROKER);
		}
		if (getPeriod()==null) {
            throw new I18NException(Messages.NO_PERIOD);
		}
		if (getHistoryBack()==null) {
            throw new I18NException(Messages.NO_HISTORY_BACK);
		}
	}
	
	/*********************************************************DK/DB Jobs************************************************/	
	/*
	 * (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		if (!sFIXTask.get() && !sAPITask.get()) {
		    CoreExecutors.schedule(this,
					BROKER_CHECK_FREQUENCY,
					TimeUnit.MILLISECONDS);
		}
		
		// Wait for the FIX broker to login
		if(getFIXBroker().getLoggedOn()){
			if (sFIXTask.compareAndSet(false, true)) {
				//Send the request for live data
				for(Instrument instrument:getInstruments()){
					MarketDataRequest request = MarketDataRequestBuilder.newRequest()
							.withBrokerID(getFIXBroker().getBrokerID())
							.withInstrument(instrument)
							.withContent(Content.QUOTE)
							.withPeriods(getPeriod())
							.create();
					SessionId sessionId = ServerFIXRouting.getInstance().getJobSession().getSessionId();
					RequestHandler.getInstance().receiveMessage(new RequestEnvelope(request, sessionId));
				}
			}
		}
				
		// Wait for the API broker to login
		if(getAPIBroker().getLoggedOn()){
			if (sAPITask.compareAndSet(false, true)) {
				//Send the request for historical data
				for(Instrument instrument:getInstruments()){
					try {
						Message msg = getAPIBroker().getFIXMessageFactory()
								.newXMLMessageEmpty();
						
						Map<String, Object> inputs = Maps.newHashMap();
						inputs.put(INSTRUMENT, instrument);
						inputs.put(PERIOD, getPeriod());
						inputs.put(HISTORY_BACK, getHistoryBack().getInterval());
					
						String xml = MarshallerFactory.toXML(new MapWrapper<>(inputs));
						msg.getHeader().setField(new XmlData(xml));
						msg.getHeader().setField(new XmlDataLen(xml.length()));
						
						getAPIBroker().sendToTarget(msg);
					} catch (Throwable e) {
						Messages.JOBS_ERROR_RUN.error(this, e);
					}
				}
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.marketcetera.client.DataReceiver#receiveData(java.lang.Object)
	 */
	@Override
	public void receiveData(Object inObject) {
		//Ignore null objects
		if(inObject == null)
			return;
		
		//persist the quote in the data base
		if(inObject instanceof QuoteEvent){
			QuoteEvent quote = (QuoteEvent)inObject;
			if(quote.getBrokerID().equals(getFIXBroker().getBrokerID())){
				persistQuote(quote);
			}
		} else if(inObject instanceof Message){ 
			Message msg = (Message)inObject;
			if(FIXMessageUtil.isXMLMessage(msg)){
            	try{
                	String xml = msg.getHeader().getString(XmlData.FIELD);
                	Object object = MarshallerFactory.fromXML(xml);
                	
                	if(object instanceof QuoteEvent){
            			QuoteEvent quote = (QuoteEvent)object;
            			if(quote.getBrokerID().equals(getAPIBroker().getBrokerID())){
            				persistQuote(quote);
            			}
            		}
            	}catch(Throwable ignored){
            		Messages.JOBS_ERROR_RUN.error(this, ignored);
            	}
            }
		}
	}
	
	/**
	 * Persist the quote if requested
	 * 
	 * @param quote the event quote
	 */
	private void persistQuote(QuoteEvent quote)
	{
		if(quote.getPeriod().equals(getPeriod()))
		{
			if(isTradingTime(quote.getTime()))
			{
				if(getInstruments().contains(quote.getInstrument()))
				{
					getHistoryService().saveQuote(quote);
				}
			}
		}
	}
	
	/**
	 * The time filter for this job
	 * 
	 * @param time the quote time
	 * 
	 * @return true if time for trading
	 */
	private boolean isTradingTime(long time) 
	{
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(time);
		
		boolean friday = cal.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY;
		boolean saturday = cal.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY;
		boolean sunday = cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY;
		int hour = cal.get(Calendar.HOUR_OF_DAY);
		
		if ((friday && hour >= 20) || saturday || (sunday && hour <= 22)) {
			return false;
		}

		return true;
	}
}
