package org.marketcetera.server.ws;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.marketcetera.core.time.Period;
import org.marketcetera.event.Quote;
import org.marketcetera.event.QuoteEvent;
import org.marketcetera.event.TickEvent;
import org.marketcetera.event.impl.QuoteEventBuilder;
import org.marketcetera.module.RequestID;
import org.marketcetera.trade.BrokerID;
import org.marketcetera.trade.Instrument;
import org.marketcetera.trade.UserID;
import org.marketcetera.util.primitives.BigDecimalUtils;
import org.marketcetera.ws.server.CoreExecutors;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class QuoteRecorderImpl implements QuoteRecorder {
	
	/**
	 * Moving average of the spread
	 */
	private final Double mMultiplier = 2./(10. + 1.);
	/**
     * the list or recorder dependent request IDs
     */
	private final List<RequestID> mRequests = Lists.newArrayList();
	
	/**
     * the list or recorder dependent request IDs
     */
	private final Collection<UserID> mUserIDs = Sets.newHashSet();
    
	/**
     * schedules quote timer job
     */
	private final ScheduledFuture<?> mScheduledTask;
	
	/**
     * the broker ID from which the price was reported 
     */
	private final BrokerID mBrokerID;
    
	/**
     * the market data instrument
     */
	private final Instrument mInstrument;
	
    /**
     * the quote period
     */
	private final Period mPeriod;
    
    /**
     * the open price for the current period
	*/
    private BigDecimal mOpen;
    /**
     * the high price for the current period
     */
    private BigDecimal mHigh;
    /**
     * the low price for the current period
     */
    private BigDecimal mLow;
    /**
     * the close price for the current period
     */
    private BigDecimal mClose;
    /**
     * the volume for the current period
     */
    private BigDecimal mVolume;
    /**
     * the quote bid-ask
     */
    private BigDecimal mSpread = BigDecimal.ZERO;
    /**
     * the time stamp of the first tick in the quote period
     */
    private long mTime;
    /**
     * the recorder lock
     */
	private final Object mLock = new Object();
    
	/**
     * Get the list or request IDs.
     *
     * @return a <code>List</code> value
     */
	private List<RequestID> getRequests(){
		return mRequests;
	}
	
	/**
     * Get the list or user IDs.
     *
     * @return a <code>Collection</code> value
     */
	private Collection<UserID> getUserIDs(){
		return mUserIDs;
	}
	
    /**
     * Get the broker ID value.
     *
     * @return a <code>String</code> value
     */
	private BrokerID getBrokerID(){
		return mBrokerID;
	}
	
    /**
     * Get the instrument value.
     *
     * @return an <code>Instrument</code> value
     */
	private Instrument getInstrument(){
		return mInstrument;
	}
	
	/**
     * Returns the recorder's period.
     *
     * @return The period.
     */
	private Period getPeriod(){
		return mPeriod;
	}
	
    /**
     * Get the openPrice value.
     *
     * @return a <code>BigDecimal</code> value
     */
	private BigDecimal getOpen()
    {
        return mOpen;
    }
	
    /**
     * Get the highPrice value.
     *
     * @return a <code>BigDecimal</code> value
     */
	private BigDecimal getHigh()
    {
        return mHigh;
    }
    
    /**
     * Get the lowPrice value.
     *
     * @return a <code>BigDecimal</code> value
     */
	private BigDecimal getLow()
    {
        return mLow;
    }
    
    /**
     * Get the closePrice value.
     *
     * @return a <code>BigDecimal</code> value
     */
	private BigDecimal getClose()
    {
        return mClose;
    }
    
    /**
     * Get the volume value.
     *
     * @return a <code>BigDecimal</code> value
     */
	private BigDecimal getVolume()
    {
        return mVolume;
    }
	
    /**
     * Gets the bid/ask spread value.
     * 
     */
	private BigDecimal getSpread() {
		return mSpread;
	}
	
    /**
     * Get the time stamp value.
     *
     * @return a <code>long</code> value
     */
	private long getTime()
    {
        return mTime;
    }
	
	/**
     * Returns the recorder lock.
     *
     * @return The lock.
     */
	private Object lock() {
		return mLock;
	}
	
    /**
     * Instance of QuoteRecorder
     * 
     * @param inBrokerID an <code>BrokerID</code> value
     * @param inInstrument an <code>Instrument</code> value
     * @param inExchange an <code>String</code> the exchange
     * @param inPeriod an <code>Period</code> value
     */
	public QuoteRecorderImpl(BrokerID inBrokerID, Instrument inInstrument, String inExchange, Period inPeriod) {
		mBrokerID = inBrokerID;
		mInstrument = inInstrument;
		mPeriod = inPeriod;
		
		long delay = inPeriod.getInterval();		
		long time = Calendar.getInstance().getTimeInMillis();
		long initialDelay = delay - (time % delay);
		
		mTime = time + initialDelay - delay;
		mScheduledTask = CoreExecutors.scheduleWithFixedDelay(new Runnable() {
            public void run() {
        		synchronized (lock()) {
        			sendQuote();
        		}
            }
        }, initialDelay, delay, TimeUnit.MILLISECONDS);
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.marketcetera.server.ws.QuoteRecorder#addRequest(org.marketcetera.module.RequestID, org.marketcetera.trade.UserID)
	 */
	@Override
	public void addRequest(RequestID requestID, UserID actorID) {
		synchronized (lock()) {
			getRequests().add(requestID);
			getUserIDs().add(actorID);
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.marketcetera.server.ws.QuoteRecorder#removeRequest(org.marketcetera.module.RequestID, org.marketcetera.trade.UserID)
	 */
	@Override
	public void removeRequest(RequestID requestID, UserID actorID) {
		synchronized (lock()) {
			getRequests().remove(requestID);
			getUserIDs().remove(actorID);
		}
		if(isEmpty()){
			synchronized (lock()) {
				CoreExecutors.cancel(mScheduledTask, true);
			}
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.marketcetera.server.ws.QuoteRecorder#onTick(org.marketcetera.event.TickEvent)
	 */
	@Override
	public void onTick(TickEvent event) {
		synchronized (lock()) {
			BigDecimal midPrice = BigDecimalUtils.precision(event.getAsk().getPrice(), event.getBid().getPrice(),
					(event.getAsk().getPrice().doubleValue() + event.getBid().getPrice().doubleValue())*0.5);
			BigDecimal midSize = BigDecimalUtils.precision(event.getAsk().getSize(), event.getBid().getSize(),
					(event.getAsk().getSize().doubleValue() + event.getBid().getSize().doubleValue())*0.5);
			BigDecimal midSpread = BigDecimalUtils.precision(event.getAsk().getPrice(), event.getBid().getPrice(),
					(event.getAsk().getPrice().doubleValue() - event.getBid().getPrice().doubleValue()-mSpread.doubleValue()) * mMultiplier);
			
			if(mOpen == null){
				mOpen = midPrice;
				mHigh = midPrice;
				mLow = midPrice;
				mClose = midPrice;
				mVolume = midSize;
				mSpread = midSpread;
				return;
			}
			
			mHigh = mHigh.max(midPrice);
			mLow = mLow.min(midPrice);
			mClose = midPrice;
			mVolume = mVolume.add(midSize);
			mSpread = mSpread.add(midSpread);
		}
	}
	
	private void sendQuote() {
		synchronized (lock()) {
			Quote quote = new Quote(getTime(), 
					getOpen(), getHigh(), getLow(), getClose(), getVolume(), getSpread());
			
			QuoteEvent quoteEvent = QuoteEventBuilder.quoteEvent()
                    .withInstrument(getInstrument())
                    .withBrokerID(getBrokerID())
                    .withQuote(quote)
                    .withPeriod(getPeriod())
                    .create();			
			for(UserID actorID:getUserIDs()){
				ServerFIXRouting.getInstance().getServerManager()
								.convertAndSend(quoteEvent, actorID);
			}

			mOpen = quote.getClose();
			mHigh = quote.getClose();
			mLow = quote.getClose();
			mVolume = BigDecimal.ZERO;
			mSpread = quote.getSpread();
			mTime = System.currentTimeMillis();
		}
	}

    /* (non-Javadoc)
     * @see  org.marketcetera.server.mds.QuoteRecorder#isEmpty()
     */
	@Override
	public synchronized boolean isEmpty() {
		synchronized (lock()) {
			return getRequests().isEmpty();
		}
	}
}
