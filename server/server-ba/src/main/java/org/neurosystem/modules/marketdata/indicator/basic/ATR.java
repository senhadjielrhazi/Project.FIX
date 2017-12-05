package org.neurosystem.modules.marketdata.indicator.basic;

import org.neurosystem.modules.marketdata.IQuoteServer;
import org.neurosystem.modules.marketdata.indicator.Indicator;

/**
 * Average True Range:  http://en.wikipedia.org/wiki/Average_true_range
 *
 * NOTE: following the convention of EMA.java,
 *       we will set the initial value to the close.
 *
 *       true_range = max( high, close_prev ) - min( low, close_prev )
 *       ATR        = EMA( true_range )
 *
 *       the length parameter refers to the EMA multiplier
 *
 *       Due to close_prev in the equation, we can only start from the
 *       second tick.
 *
 * ---------------------------------------------------------------------
 */
public class ATR extends Indicator<Double> {
    
	private final double p_multiplier;

	public ATR(IQuoteServer qs, int length) {
		 super(qs);
	     this.p_multiplier = 2. / (length + 1.);
    }
	
    @Override
    public Double calculate() {
    	double value = 0.;

    	double high = quoteServer().lastValue().getHigh();
    	double low = quoteServer().lastValue().getLow();
		double last = quoteServer().valueBack(1).getClose();
    	double trueRange = 0.;
    	
    	if(size() == 0){
    		trueRange = high-low;
    		value += trueRange;
		}else{
			trueRange = Math.max(high, last) - Math.min(low, last);
			value = lastValue().getValue();
			value += (trueRange - value) * this.p_multiplier;
		}
    	
		addValue(value);
        return value;
    }
}
