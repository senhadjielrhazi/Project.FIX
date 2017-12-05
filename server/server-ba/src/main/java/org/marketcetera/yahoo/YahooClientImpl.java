package org.marketcetera.yahoo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.concurrent.GuardedBy;

import org.apache.commons.lang.ArrayUtils;
import org.marketcetera.core.CoreException;
import org.marketcetera.event.EntryPx;
import org.marketcetera.event.Event;
import org.marketcetera.event.TickEvent;
import org.marketcetera.event.TradeEvent;
import org.marketcetera.event.impl.TickEventBuilder;
import org.marketcetera.event.impl.TradeEventBuilder;
import org.marketcetera.marketdata.Content;
import org.marketcetera.marketdata.MarketDataRequest;
import org.marketcetera.marketdata.MarketDataRequestBuilder;
import org.marketcetera.trade.BrokerID;
import org.marketcetera.trade.Equity;
import org.marketcetera.trade.Instrument;
import org.marketcetera.util.log.SLF4JLoggerProxy;
import org.marketcetera.util.misc.ClassVersion;

import com.google.common.collect.Lists;

/**
 * Provides a <code>YahooClient</code> implementation.
 */
@ClassVersion("$Id: YahooClientImpl.java 16339 2012-10-31 15:59:24Z colin $")
class YahooClientImpl
        implements Runnable, YahooClient
{
    /* (non-Javadoc)
     * @see org.springframework.context.Lifecycle#isRunning()
     */
    @Override
    public boolean isRunning()
    {
        return isRunning.get();
    }
    /* (non-Javadoc)
     * @see org.springframework.context.Lifecycle#start()
     */
    @Override
    public synchronized void start()
    {
        if(isRunning.get()) {
            return;
        }
        thread = new Thread(this,
                            "Yahoo Client Thread"); //$NON-NLS-1$
        thread.start();
        isRunning.set(true);
    }
    /* (non-Javadoc)
     * @see org.springframework.context.Lifecycle#stop()
     */
    @Override
    public synchronized void stop()
    {
        if(!isRunning.get()) {
            return;
        }
        try {
            if(thread != null) {
                thread.interrupt();
                try {
                    thread.join();
                } catch (InterruptedException ignored) {}
            }
        } finally {
            thread = null;
            isRunning.set(false);
        }
    }
    /* (non-Javadoc)
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run()
    {
        try {
            while(isRunning.get()) {
                synchronized(requests) {
                    for(YahooRequest request : requests) {
                        try {
                            doDataReceived(request.getHandle(),
                                                        submit(request));
                        } catch (IOException e) {
                            SLF4JLoggerProxy.debug(YahooClientImpl.class,
                                                   e,
                                                   "Retrying...");
                        }
                    }
                }
                Thread.sleep(getRefreshInterval());
            }
        } catch (InterruptedException e) {
        }
    }
    
    /**
     * Indicates receipt of market data.
     *
     * @param inHandle a <code>String</code> value
     * @param inData an <code>Object</code> value
     */
    void doDataReceived(String inHandle,
                        Object inData) {

		System.out.println("inHandle: " + inData);
		List<Event> events = toEvent(inData, inHandle);
		
		for(Event event:events){
			System.out.println("event: " + event);
		}
	}

	public synchronized List<Event> toEvent(Object inData, String inHandle) throws CoreException {
		if (!(inData instanceof String)) {
			throw new UnsupportedOperationException( "Error: "+ inData.getClass().getName());
		}
		String data = (String) inData;
		SLF4JLoggerProxy.debug(YahooClientImpl.class, "Received [{}] {}", //$NON-NLS-1$
				inHandle, data);
		// split the data into the query description string and the data itself
		String[] components = data.split(YahooClientImpl.QUERY_SEPARATOR);
		// the query consists of a header and a field description, split that
		// again to get just the field description
		String header = components[0];
		String completeFields = header.split("&f=")[1]; //$NON-NLS-1$
		// split the fields using the delimiter
		String[] fields = completeFields.split(YahooClientImpl.FIELD_DELIMITER); // $NON-NLS-1$
		// the values are also comma-delimited
		String completeValues = components[1];
		String symbol = completeValues.split(YahooClientImpl.FIELD_DELIMITER)[0];

		// extract the field values splitting based on symbol which avoids any ,
		// as part of data could be split up properly..
		StringBuilder builder = new StringBuilder();
		builder.append(YahooClientImpl.DELIMITER_SYMBOL);
		builder.append(symbol);
		builder.append(YahooClientImpl.FIELD_DELIMITER);
		String[] values = completeValues.split(builder.toString()); // $NON-NLS-1$
		values = (String[]) ArrayUtils.subarray(values, 1, values.length);

		if (fields.length != values.length) {
			String errorMsg = String.format("fields.length: %s, values.length : %s", fields.length, values.length);
			SLF4JLoggerProxy.warn(YahooClientImpl.class, errorMsg);
			throw new UnsupportedOperationException("UNEXPECTED_VALUE_CODE");
		}

		Map<YahooField, String> matchedData = new HashMap<>();
		for (int i = 0; i < fields.length; i++) {
			YahooField field = YahooField.getFieldFor(fields[i].substring(1));
			if (field == null) {
				//Messages.UNEXPECTED_FIELD_CODE.error(YahooFeedEventTranslator.class, fields[i]);
			} else {
				matchedData.put(field, values[i]);
			}
		}
		return getEventsFrom(matchedData, inHandle);
	}

	/**
     * Gets all the events it can find from the given data collection.
     *
     * @param inData a <code>Map&lt;YahooField,String&gt;</code> value
     * @param inHandle 
     * @return a <code>List&lt;Event&gt;</code> value
     */
    private List<Event> getEventsFrom(Map<YahooField,String> inData, String inHandle)
    {
        SLF4JLoggerProxy.debug(YahooClientImpl.class,
                               "Getting events from {}", //$NON-NLS-1$
                               inData);
        String errorIndication = inData.get(YahooField.ERROR_INDICATION);
        if(!errorIndication.equals(NO_ERROR)) {
            SLF4JLoggerProxy.warn(org.marketcetera.core.Messages.USER_MSG_CATEGORY,
                                  errorIndication);
            return Lists.newArrayList();
        }
        // no error found, continue
        LinkedList<Event> events = new LinkedList<Event>();
        // look for specific event types
        lookForTickEvent(inData,
                        events, inHandle);
        lookForTradeEvent(inData,
                          events);

        
        return events;
    }
    
    /**
     * Looks for tick events in the given data. 
     *
     * @param inData a <code>Map&lt;YahooField,String&gt;</code> value
     * @param inEvents a <code>List&lt;Event&gt;</code> value
     */
    private void lookForTickEvent(Map<YahooField,String> inData,
                                 List<Event> inEvents, String inHandle)
    {
        String askPrice = inData.get(YahooField.REAL_TIME_ASK);
        String askSize = inData.get(YahooField.ASK_SIZE);
        String symbol = inData.get(YahooField.SYMBOL);
        // check for a missing field
        if(symbol == null ||
           askPrice == null ||
           askSize == null) {
            return;
        }

        String bidPrice = inData.get(YahooField.REAL_TIME_BID);
        String bidSize = inData.get(YahooField.BID_SIZE);
        // check for a missing field
        if(bidPrice == null ||
           bidSize == null) {
            return;
        }
        
        // construct instrument
        Instrument instrument = getInstrumentFrom(symbol);
        String exchange = inData.get(YahooField.STOCK_EXCHANGE);

        TickEventBuilder<TickEvent> builder = TickEventBuilder.tickEvent()
        		.withInstrument(instrument)
        		.withBrokerID(new BrokerID("YH"))
        		.withBid(new EntryPx(new BigDecimal(bidPrice), new BigDecimal(bidSize), exchange))
        		.withBid(new EntryPx(new BigDecimal(askPrice), new BigDecimal(askSize), exchange));
        inEvents.add(builder.create());
    }
    
    /**
     * Looks for trade events in the given data. 
     *
     * @param inData a <code>Map&lt;YahooField,String&gt;</code> value
     * @param inEvents a <code>List&lt;Event&gt;</code> value
     */
    private void lookForTradeEvent(Map<YahooField,String> inData,
                                   List<Event> inEvents)
    {
        String tradePrice = inData.get(YahooField.LAST_TRADE_PRICE_ONLY);
        String tradeSize = inData.get(YahooField.LAST_TRADE_SIZE);
        String tradeDate = inData.get(YahooField.LAST_TRADE_DATE);
        String tradeTime = inData.get(YahooField.LAST_TRADE_TIME);
        String symbol = inData.get(YahooField.SYMBOL);
        String exchange = inData.get(YahooField.STOCK_EXCHANGE);
        // check for a missing field
        if(symbol == null ||
           exchange == null ||
           tradePrice == null ||
           tradeSize == null ||
           tradeDate == null ||
           tradeTime == null) {
            return;
        }
        // construct instrument
        Instrument instrument = getInstrumentFrom(symbol);
        BigDecimal price;
        BigDecimal size;
        try {
            price = new BigDecimal(tradePrice);
            size = new BigDecimal(tradeSize);
        } catch (Exception e) {
            return;
        }
        TradeEventBuilder<TradeEvent> builder = TradeEventBuilder.tradeEvent()
        		.withInstrument(instrument)
        		.withBrokerID(new BrokerID("YH"))
        		.withTrade(new EntryPx(price, size, exchange));
        inEvents.add(builder.create());
    }

    /**
     * Gets an <code>Instrument</code> for the given symbol.
     *
     * @param inSymbol a <code>String</code> value
     * @return an <code>Instrument</code> value
     */
    private Instrument getInstrumentFrom(String inSymbol)
    {
        // TODO account for other instrument types
        return new Equity(inSymbol);
    }
    
    /*
     * (non-Javadoc)
     * @see org.marketcetera.server.ba.brokers.api.YahooClient#login(java.lang.String)
     */
    @Override
    public boolean login(String inURL)
    {
        url = inURL;
        start();
        return isRunning();
    }
    /* (non-Javadoc)
     * @see org.marketcetera.server.ba.brokers.api.YahooClient#logout()
     */
    @Override
    public void logout()
    {
        stop();
    }
    /* (non-Javadoc)
     * @see org.marketcetera.server.ba.brokers.api.YahooClient#isLoggedIn()
     */
    @Override
    public boolean isLoggedIn()
    {
        return isRunning();
    }
    /* (non-Javadoc)
     * @see org.marketcetera.server.ba.brokers.api.YahooClient#request(org.marketcetera.server.ba.brokers.api.YahooRequest)
     */
    @Override
    public void request(YahooRequest inRequest)
    {
        synchronized(requests) {
            requests.add(inRequest);
        }
    }
    /* (non-Javadoc)
     * @see org.marketcetera.server.ba.brokers.api.YahooClient#cancel(org.marketcetera.server.ba.brokers.api.YahooRequest)
     */
    @Override
    public void cancel(YahooRequest inRequest)
    {
        synchronized(requests) {
            requests.remove(inRequest);
        }
    }
    /* (non-Javadoc)
     * @see org.marketcetera.server.ba.brokers.api.YahooClient#getRequestCounter()
     */
    @Override
    public long getRequestCounter()
    {
        return requestCounter.get();
    }
    /* (non-Javadoc)
     * @see org.marketcetera.server.ba.brokers.api.YahooClient#resetRequestcounter()
     */
    @Override
    public void resetRequestcounter()
    {
        requestCounter.set(0);
    }
    
    /**
     * Create a new YahooClient instance.
     */
    YahooClientImpl()
    {
    }
    
    /**
     * Submits the given request and returns the response from Yahoo.
     *
     * @param inRequest a <code>YahooRequest</code> value
     * @return a <code>String</code> value
     * @throws IOException if an error occurs submitting the request
     */
    private String submit(YahooRequest inRequest)
            throws IOException
    {
        StringBuilder response = new StringBuilder();
        String query = inRequest.getQuery();
        response.append(query).append(QUERY_SEPARATOR);
        // Create a URL for the desired page
        URL url = new URL(getURL() + query.replaceAll(",", //$NON-NLS-1$
                                                                  "")); //$NON-NLS-1$
        
        SLF4JLoggerProxy.trace(YahooClientImpl.class,
                               "Submitting request for {}", //$NON-NLS-1$
                               url);
        // Read all the text returned by the server
        BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
        requestCounter.incrementAndGet();
        String str;
        while ((str = in.readLine()) != null) {
            // str is one line of text; readLine() strips the newline character(s)
            response.append(str);
        }
        in.close();
        return response.toString();
    }
    
    /**
     * Returns the URL that describes the location of the Yahoo server.
     *
     * @return a <code>String</code> value
     */
    private String getURL() {
		return url;
	}
    
    /**
     * Gets the interval at which to refresh market data requests.
     *
     * @return an <code>int</code> value in ms
     */
    private int getRefreshInterval(){
    	return refreshInterval;
    }
	/**
     * sentinel value used to separate query tokens
     */
    static final String QUERY_SEPARATOR = "&&/&&"; //$NON-NLS-1$

    /**
     * sentinel value used to separate query tokens
     */
    static final String FIELD_DELIMITER = ","; //$NON-NLS-1$

    /**
     * sentinel value used to separate query tokens
     */
    static final String DELIMITER_SYMBOL = ",?"; //$NON-NLS-1$
    
    /**
     * default interval at which to refresh the market data
     */
    private volatile int refreshInterval = 250;
    /**
     * thread used for submitting requests
     */
    private volatile Thread thread;
    /**
     * the URL at which Yahoo provides the data
     */
    private volatile String url = "http://finance.yahoo.com/d/quotes.csv"; //$NON-NLS-1$
    /**
     * indicates if the client is running
     */
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    /**
     * indicates no error
     */
    private static final String NO_ERROR = "\"N/A\"";  //$NON-NLS-1$
    /**
     * the active Yahoo requests
     */
    @GuardedBy("requests")
    private final Set<YahooRequest> requests = new HashSet<>();
    /**
     * the counter used to keep track of the number of requests
     */
    private final AtomicLong requestCounter = new AtomicLong(0);
    
    public static void main(String[] args)
    {
    	YahooClientImpl client = new YahooClientImpl();
    	
    	String inSymbol = "C";
		MarketDataRequest inRequest = MarketDataRequestBuilder.newRequest()
				.withBrokerID(new BrokerID("YH"))
				.withInstrument(new Equity(inSymbol))
				.withContent(Content.TICK/*, Content.TRADE*/)
				.withExchange("NYSE")
				.create()
				;
		YahooRequest request = new YahooRequest(inRequest, inSymbol);
    	
    	
    	client.login(client.getURL());
    	

		client.request(request);
    }
}
