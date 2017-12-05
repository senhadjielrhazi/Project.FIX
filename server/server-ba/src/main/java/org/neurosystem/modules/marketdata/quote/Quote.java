package org.neurosystem.modules.marketdata.quote;

import org.neurosystem.util.basic.HasTime;
import org.neurosystem.util.common.annotations.javax.Nonnull;
import org.neurosystem.util.common.annotations.javax.concurrent.Immutable;

/**
 * Encapsulates the price bar information.
 */
@Immutable
public final class Quote implements IQuote {

	 private final long p_time;
	 private final double p_open, p_high, p_low, p_close;
	
	 private final int p_hashUID;
	 /**
     * This constructor is used to create a new historical bar
     */
    public Quote(@Nonnull long time, 
    		@Nonnull double open, @Nonnull double high, @Nonnull double low, @Nonnull double close) {
    	this.p_time = time;
    	this.p_open = open;
    	this.p_high = high;
    	this.p_low = low;
    	this.p_close = close;
    	
    	//Fast access UID
    	this.p_hashUID = getHashUID();
    }
	
    /**
     * This constructor used to create a new real time bar
     */
    public Quote(@Nonnull long time, @Nonnull double price) {
        this(time, price, price, price, price);
    }
    
    private int getHashUID(){
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
 
	/* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
	@Override
	public boolean equals(Object obj){
		if (obj == this){
	        return true;
	    }
	    if (obj == null){
	        return false;
	    }
	    if (getClass() != obj.getClass()){
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
	
	/* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
	@Override
	public int hashCode() {
	    return this.p_hashUID;
	}
	
	 /* (non-Javadoc)
    * @see java.lang.Object#toString()
    */
	@Override
	public String toString() {
		return String.format("Time: %s, OHLC: [ %s, %s, %s, %s ]", 
				formatedTime(), this.p_open, this.p_high, this.p_low, this.p_close);
	}
	
	public static IQuote valueOf(String regex){
		String[] split = regex.split("Time: |, OHLC: \\[ |, |, |, | \\]");
		
		long time = HasTime.parse(split[1]);
		double open = Double.parseDouble(split[2]);
		double high = Double.parseDouble(split[3]);
		double low = Double.parseDouble(split[4]);
		double close = Double.parseDouble(split[5]);
		
		return new Quote(time, open, high, low, close);
	}
	
	//C = a * A + b * B
	public static IQuote plus(@Nonnull double a, @Nonnull IQuote quoteA, @Nonnull double b, @Nonnull IQuote quoteB){
		long time = quoteA.getTime();
		
		double open		= a * quoteA.getOpen() + b * quoteB.getOpen();
		double close	= a * quoteA.getClose() + b * quoteB.getClose();
		
		double high = 0, low = 0;
		if(a > 0){
			high	+= a * quoteA.getHigh();
			low		+= a * quoteA.getLow();
		}else{
			high	+= a * quoteA.getLow();
			low		+= a * quoteA.getHigh();
		}
		
		if(b > 0){
			high	+= b * quoteB.getHigh();
			low		+= b * quoteB.getLow();
		}else{
			high	+= b * quoteB.getLow();
			low		+= b * quoteB.getHigh();
		}
		
		return new Quote(time, open, high, low, close);
	}
	
	//C = A + B
	public static IQuote plus(@Nonnull IQuote quoteA, @Nonnull IQuote quoteB){
		return plus(1., quoteA, 1., quoteB);
	}
	
	//C = a * A + B
	public static IQuote plus(@Nonnull IQuote quoteA, double b, @Nonnull IQuote quoteB){
		return plus(1., quoteA, b, quoteB);
	}
	
	//C = A + b * B
	public static IQuote plus(double a, @Nonnull IQuote quoteA, @Nonnull IQuote quoteB){
		return plus(a, quoteA, 1., quoteB);
	}
	
	//C = a * A
	public static IQuote times(@Nonnull double a, @Nonnull IQuote quoteA){
		long time = quoteA.getTime();

		double open		= a * quoteA.getOpen();
		double close	= a * quoteA.getClose();
		
		double high, low;
		if(a > 0){
			high	= a * quoteA.getHigh();
			low		= a * quoteA.getLow();
		}else{
			high	= a * quoteA.getLow();
			low		= a * quoteA.getHigh();
		}
		
		return new Quote(time, open, high, low, close);
	}
}
