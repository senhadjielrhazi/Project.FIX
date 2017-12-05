package org.neurosystem.modules.marketdata.indicator.basic;

import org.neurosystem.modules.marketdata.IQuoteServer;
import org.neurosystem.modules.marketdata.indicator.Indicator;
import org.neurosystem.util.misc.Pair;

/**
 * MinMax of the period.
 */
public class MMX extends Indicator<Pair<Double, Double>> {

	private final int p_length;

    public MMX(IQuoteServer qs, int length) {
    	 super(qs);
         this.p_length = length;
    }

    @Override
    public Pair<Double, Double> calculate() {
    	Pair<Double, Double> value;
    	
    	int nbBar = Math.min(quoteServer().size(), this.p_length);
    	
    	double min = quoteServer().lastValue().getLow();
    	double max = quoteServer().lastValue().getHigh();
        for (int bar = 0; bar < nbBar; bar++) {
        	min = Math.min(min, quoteServer().valueBack(bar).getLow());
        	max = Math.max(max, quoteServer().valueBack(bar).getHigh());
        }
        value = new Pair<Double, Double>(min, max);
        
		addValue(value);
        return value;
    }
}
