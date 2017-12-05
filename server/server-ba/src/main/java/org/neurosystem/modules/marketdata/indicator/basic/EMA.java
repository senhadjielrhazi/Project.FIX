package org.neurosystem.modules.marketdata.indicator.basic;

import org.neurosystem.modules.marketdata.AppliedPrice;
import org.neurosystem.modules.marketdata.IQuoteServer;
import org.neurosystem.modules.marketdata.indicator.Indicator;

/**
 * Exponential Moving Average.
 */
public class EMA extends Indicator<Double> {
	
	private final double p_multiplier;
	
    public EMA(IQuoteServer qs, int length) {
        this(qs, length, AppliedPrice.CLOSE);
    }

	public EMA(IQuoteServer qs, int length, AppliedPrice appliedPrice) {
		 super(qs, appliedPrice);
	     this.p_multiplier = 2. / (length + 1.);
    }
	
    @Override
    public Double calculate() {
    	double value = 0.;
    	
    	double price = quoteServer().lastValue().getPrice(appliedPrice());
    	if(size() == 0){
    		value = price;
		}else{
			value = lastValue().getValue();
			value += (price - value) * this.p_multiplier;
		}

		addValue(value);
        return value;
    }
}
