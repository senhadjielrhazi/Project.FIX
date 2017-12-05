package org.technosystem.modules.marketdata.indicator.basic;

import org.technosystem.modules.marketdata.AppliedPrice;
import org.technosystem.modules.marketdata.indicator.Indicator;
import org.technosystem.modules.marketdata.server.IDataServer;

public class EMA extends Indicator<Double> {
    
	/**
	 * Exponential Moving Average.
	 */
	private static final long serialVersionUID = 1L;
	
	private final Double p_multiplier;
	
    public EMA(Integer length) {
        this(length, AppliedPrice.CLOSE);
    }

	public EMA(Integer length, AppliedPrice appliedPrice) {
		 super(appliedPrice);
	     this.p_multiplier = 2. / (length + 1.);
    }
	
    @Override
    public Double calculate(IDataServer ds) {
    	Double value = 0D;
    	
    	Double price = ds.lastValue().getPrice(getAppliedPrice());
    	if(size() == 0){
    		value = price;
		}else{
			value = lastValue().getValue();
			value += (price - value) * this.p_multiplier;	
		}

    	addTimedValue(ds.lastValue().getTime(), value);
        return value;
    }
}
