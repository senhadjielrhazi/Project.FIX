package org.neurosystem.modules.marketdata.indicator.basic;

import org.neurosystem.modules.marketdata.IQuoteServer;
import org.neurosystem.modules.marketdata.indicator.Indicator;

/**
 * Stochastic oscillator %K
 * About: http://en.wikipedia.org/wiki/Stochastic_oscillator
 */
public class STO extends Indicator<Double> {

	private final int p_length;

    public STO(IQuoteServer qs, int length) {
    	 super(qs);
         this.p_length = length;
    }
    
    @Override
    public Double calculate() {
    	double value = 0;

        int nbBar = Math.min(quoteServer().size(), this.p_length);
        
        double max = quoteServer().lastValue().getHigh();
        double min = quoteServer().lastValue().getLow();
        double last = quoteServer().lastValue().getClose();
        for (int bar = 0; bar < nbBar; bar++) {
        	max = Math.max(quoteServer().valueBack(bar).getHigh(), max);
        	min = Math.min(quoteServer().valueBack(bar).getLow(), min);
        }
        value = ((last-min) / (max-min)) * 100.;
        
        addValue(value);
        return value;
    }
}
