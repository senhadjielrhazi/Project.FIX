package org.neurosystem.modules.marketdata.indicator.basic;

import org.neurosystem.modules.marketdata.IQuoteServer;
import org.neurosystem.modules.marketdata.indicator.Indicator;

/**
* Open Close Stick
 * 
 * Open to close moving average of the period.
 */
public class OCS extends Indicator<Double> {

	private final double p_multiplier;
	
    public OCS(IQuoteServer qs, int length) {
    	 super(qs);
         this.p_multiplier = 2. / (length + 1.);
    }

    @Override
    public Double calculate() {
    	double value = 0.;
        
    	double diff = quoteServer().lastValue().getClose() - quoteServer().lastValue().getOpen();
        if(size() == 0){
        	value = diff;
		}else{
			value = lastValue().getValue();
			value += (diff - value) * this.p_multiplier;
		}

		addValue(value);
        return value;
    }
}
