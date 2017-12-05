package org.technosystem.modules.marketdata.quote;

import org.neurosystem.util.common.annotations.javax.Nonnull;
import org.neurosystem.util.common.annotations.javax.Nullable;
import org.neurosystem.util.common.annotations.javax.concurrent.Immutable;

/**
 * Encapsulates the price bar information.
 */
@Immutable
public final class Quote implements IQuote {

	private final long p_time;
	private final double p_open, p_high, p_low, p_close;

	private int p_hashUID;

	/**
	 * This constructor used to create a new real time bar
	 */
	public Quote(@Nonnull long time, @Nonnull double open, @Nonnull double high, @Nonnull double low, @Nonnull double close) {
		this.p_time = time;
    	this.p_open = open;
    	this.p_high = high;
    	this.p_low = low;
    	this.p_close = close;
	}

	public Quote(@Nonnull long time, @Nonnull double price) {
		this(time, price, price, price, price);
	}

	private int getHashUID() {
		final int prime = 31;
		int hash = 1;

		hash = prime * hash + Long.hashCode(this.p_time);
		hash = prime * hash + Double.hashCode(this.p_open);
        hash = prime * hash + Double.hashCode(this.p_high);
        hash = prime * hash + Double.hashCode(this.p_low);
        hash = prime * hash + Double.hashCode(this.p_close);

		return hash;
	}

	@Override
	public long getTime() {
		return this.p_time;
	}

	@Override
    public double getOpen() {
        return this.p_open;
    }
    
    @Override
    public double getHigh() {
        return this.p_high;
    }
    
    @Override
    public double getLow() {
        return this.p_low;
    }
    
    @Override
    public double getClose() {
        return this.p_close;
    }
    
    @Override
    public double getMedian() {
        return (this.p_low + this.p_high) / 2.;
    }
    
    @Override
    public double getTypical() {
        return (this.p_low + this.p_high + this.p_close) / 3.;
    }

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}

		Quote other = (Quote) obj;
		if (this.p_time != other.p_time)
			return false;
		if (this.p_open != other.p_open)
            return false;
        if (this.p_high != other.p_high)
            return false;
        if (this.p_low != other.p_low)
            return false;
        if (this.p_close != other.p_close)
            return false;

		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		if (this.p_hashUID == 0) {
			this.p_hashUID = getHashUID();
		}
		return this.p_hashUID;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return String.format("Time: %s, OHLC: [%s, %s, %s, %s]", formatedTime(), 
				this.p_open, this.p_high, this.p_low, this.p_close);
	}
	
	//C = a * A + b * B + c
	public static IQuote plus(@Nonnull double a, @Nonnull IQuote quoteA, @Nonnull double b, @Nonnull IQuote quoteB, @Nullable double c){
		long time = quoteA.getTime();
		double open = c, close = c, high = c, low = c;
		
		open		+= a * quoteA.getOpen();
		close		+= a * quoteA.getClose();
		if(a > 0){
			high	+= a * quoteA.getHigh();
			low		+= a * quoteA.getLow();
		}else{
			high	+= a * quoteA.getLow();
			low		+= a * quoteA.getHigh();
		}
		
		open		+= b * quoteB.getOpen();
		close		+= b * quoteB.getClose();
		if(b > 0){
			high	+= b * quoteB.getHigh();
			low		+= b * quoteB.getLow();
		}else{
			high	+= b * quoteB.getLow();
			low		+= b * quoteB.getHigh();
		}
		
		return new Quote(time, open, high, low, close);
	}

	//C = a * A + c
	public static IQuote plus(@Nonnull double a, @Nonnull IQuote quoteA, @Nullable double c){
		long time = quoteA.getTime();
		double open = c, close = c, high = c, low = c;
		
		open		+= a * quoteA.getOpen();
		close		+= a * quoteA.getClose();
		if(a > 0){
			high	+= a * quoteA.getHigh();
			low		+= a * quoteA.getLow();
		}else{
			high	+= a * quoteA.getLow();
			low		+= a * quoteA.getHigh();
		}
		
		return new Quote(time, open, high, low, close);
	}
		
	//C = pow(A, a) * pow(B, b) * c
	public static IQuote times(@Nonnull double a, @Nonnull IQuote quoteA, @Nonnull double b, @Nonnull IQuote quoteB, @Nullable double c){
		long time = quoteA.getTime();
		double open = c, close = c, high = c, low = c;
		
		open		*= Math.pow(quoteA.getOpen(), a);
		close		*= Math.pow(quoteA.getClose(), a);
		if(c * a > 0){
			high	*= Math.pow(quoteA.getHigh(), a);
			low		*= Math.pow(quoteA.getLow(), a);
		}else{
			high	*= Math.pow(quoteA.getLow(), a);
			low		*= Math.pow(quoteA.getHigh(), a);
		}
		
		open		*= Math.pow(quoteB.getOpen(), b);
		close		*= Math.pow(quoteB.getClose(), b);
		if(c * b > 0){
			high	*= Math.pow(quoteB.getHigh(), b);
			low		*= Math.pow(quoteB.getLow(), b);
		}else{
			high	*= Math.pow(quoteB.getLow(), b);
			low		*= Math.pow(quoteB.getHigh(), b);
		}
		
		return new Quote(time, open, high, low, close);
	}
	
	//C = pow(A, a) * c
	public static IQuote times(@Nonnull double a, @Nonnull IQuote quoteA, @Nullable double c){
		long time = quoteA.getTime();
		double open = c, close = c, high = c, low = c;
		
		open		*= Math.pow(quoteA.getOpen(), a);
		close		*= Math.pow(quoteA.getClose(), a);
		if(c * a > 0){
			high	*= Math.pow(quoteA.getHigh(), a);
			low		*= Math.pow(quoteA.getLow(), a);
		}else{
			high	*= Math.pow(quoteA.getLow(), a);
			low		*= Math.pow(quoteA.getHigh(), a);
		}
		
		return new Quote(time, open, high, low, close);
	}
}
