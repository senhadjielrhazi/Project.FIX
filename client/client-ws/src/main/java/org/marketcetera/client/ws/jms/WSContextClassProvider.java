package org.marketcetera.client.ws.jms;

import javax.annotation.concurrent.Immutable;

import org.marketcetera.brokers.BrokerStatus;
import org.marketcetera.event.EntryPx;
import org.marketcetera.event.Quote;
import org.marketcetera.event.impl.DepthEventImpl;
import org.marketcetera.event.impl.QuoteEventImpl;
import org.marketcetera.event.impl.TickEventImpl;
import org.marketcetera.event.impl.TradeEventImpl;
import org.marketcetera.marketdata.MarketDataCancel;
import org.marketcetera.marketdata.MarketDataReject;
import org.marketcetera.marketdata.MarketDataRequest;
import org.marketcetera.trade.FIXOrderImpl;
import org.marketcetera.trade.FIXResponseImpl;
import org.marketcetera.trade.Instrument;
import org.marketcetera.trade.OrderBaseImpl;
import org.marketcetera.trade.ReportBaseImpl;
import org.marketcetera.util.misc.ClassVersion;
import org.marketcetera.util.ws.ContextClassProvider;
import org.marketcetera.client.ws.jms.RequestEnvelope;

/**
 * Provides context classes for trade objects.
 */
@Immutable
@ClassVersion("$Id: WSContextClassProvider.java 16914 2014-06-24 21:27:56Z colin $")
public class WSContextClassProvider
        implements ContextClassProvider
{   
	/**
     * Creates an uninitialized instance. This constructor is meant to be
     * used by JAXB.
     */
	public WSContextClassProvider(){
		
	}
    /* (non-Javadoc)
     * @see org.marketcetera.util.ws.ContextClassProvider#getContextClasses()
     */
    @Override
    public Class<?>[] getContextClasses()
    {
        return CLASSES;
    }
    /**
     * static instance
     */
    public static final WSContextClassProvider INSTANCE = new WSContextClassProvider();
    /**
     * classes value
     */
    private static final Class<?>[] CLASSES = new Class<?>[] { 
    	Instrument.class,//Inputs
    	OrderBaseImpl.class,FIXOrderImpl.class,RequestEnvelope.class,//Requests
    	ReportBaseImpl.class,FIXResponseImpl.class,BrokerStatus.class, //Responses Trade
    	MarketDataRequest.class,MarketDataCancel.class,//Requests
    	TradeEventImpl.class,QuoteEventImpl.class,TickEventImpl.class,DepthEventImpl.class,
    	EntryPx.class,Quote.class,MarketDataReject.class,FIXResponseImpl.class,BrokerStatus.class //Responses Data
    };
}
