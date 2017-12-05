package org.technosystem.modules.marketdata.server;

import java.util.ArrayList;
import java.util.List;

import org.technosystem.modules.marketdata.indicator.IIndicator;
import org.technosystem.modules.marketdata.quote.IQuote;
import org.technosystem.modules.marketdata.quote.Quote;
import org.neurosystem.util.basic.HasValues;
import org.neurosystem.util.common.annotations.javax.concurrent.Immutable;
import org.neurosystem.util.time.IPeriod;

@Immutable
public final class DataServer extends ArrayList<IQuote> implements IDataServer {

	private final IPeriod p_barPeriod;
	private final int p_serverSize;
	private final List<IIndicator<?>> p_indicators;

	private volatile IQuote p_lastValue = null;
	
	/**
	 * History of tick & bar quotes
	 */
	private static final long serialVersionUID = 1L;
	
	public DataServer(IPeriod barPeriod) {
		this(barPeriod, easybites);
	}
	
	public DataServer(IPeriod barPeriod, int serverSize) {
		super();
		this.p_barPeriod = barPeriod;
		this.p_serverSize = serverSize;
		
		//Indicators
		this.p_indicators =  new ArrayList<>();
	}

	@Override
	public synchronized void onQuote(IPeriod period, IQuote quote) {
		if(this.p_lastValue == null){
			this.p_lastValue = quote;
		}else{
			this.p_lastValue = new Quote(this.p_lastValue.getTime(), 
					this.p_lastValue.getOpen(), 
					Math.max(this.p_lastValue.getHigh(), quote.getHigh()),
					Math.min(this.p_lastValue.getLow(), quote.getLow()), 
					quote.getClose());
		}
		if(isNewPeriod(period, quote.getTime())){
			if(size() >= this.p_serverSize){
				remove(0);
			}
			add(this.p_lastValue);
			this.p_lastValue = null;
			
			for(IIndicator<?> indicator:this.p_indicators){
				indicator.calculate(this);
			}
		}
	}

	@Override
	public void subscribe(IIndicator<?> indicator) {
		this.p_indicators.add(indicator);
	}

	@Override
	public void subscribe(IIndicator<?>... indicators) {
		for(IIndicator<?> indicator:indicators){
			subscribe(indicator);
		}
	}
	
	@Override
	public IPeriod getPeriod() {
		return this.p_barPeriod;
	}
	
	private boolean isNewPeriod(IPeriod period, long time){
		return ((time + period.getInterval()) % this.p_barPeriod.getInterval() == 0);
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
	    
	    DataServer other = (DataServer) obj;
	    if (!this.p_barPeriod.equals(other.p_barPeriod))
			return false;
        if (this.p_serverSize != other.p_serverSize)
            return false;

		return super.equals(obj);
	}
	
	/* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
	@Override
	public int hashCode() {
		final int prime = 31;
        int hash = 1;
        
        hash = prime * hash + this.p_barPeriod.hashCode();
        hash = prime * hash + Integer.hashCode(this.p_serverSize);
        hash = prime * hash + super.hashCode();
        
	    return hash;
	}
	
	/* (non-Javadoc)
    * @see java.lang.Object#toString()
    */
	@Override
	public String toString() {
		return String.format("Period: %s, %s", this.p_barPeriod, 
				HasValues.formatedValues(this));
	}
}
