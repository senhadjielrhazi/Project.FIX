package org.neurosystem.modules.marketdata.indicator.basic;

import org.neurosystem.modules.marketdata.IQuoteServer;
import org.neurosystem.modules.marketdata.indicator.Indicator;
import org.neurosystem.util.misc.Pair;

/**
 * High Low Stick
 * 
 * High to close and low to close moving average of the period.
 */
public class HLS extends Indicator<Pair<Double,Double>> {

	private final double p_multiplier;
	
    public HLS(IQuoteServer qs, int length) {
    	 super(qs);
         this.p_multiplier = 2. / (length + 1.);
    }

    @Override
    public Pair<Double,Double> calculate() {
    	Pair<Double,Double> value;
        
    	double high = quoteServer().lastValue().getHigh() - quoteServer().lastValue().getClose();
    	double low = quoteServer().lastValue().getClose() - quoteServer().lastValue().getLow();
        
    	if(size() == 0){
        	value =  new Pair<Double, Double>(low, high);
		}else{
			double keyL = lastValue().getValue().getKey();
			keyL += (low - keyL) * this.p_multiplier;
			
			double keyH = lastValue().getValue().getValue();
			keyH += (high - keyH) * this.p_multiplier;
			
			value =  new Pair<Double, Double>(keyL, keyH);
		}

		addValue(value);
        return value;
    }
}
