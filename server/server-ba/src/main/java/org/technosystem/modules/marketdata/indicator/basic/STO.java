package org.technosystem.modules.marketdata.indicator.basic;

import org.technosystem.modules.marketdata.indicator.Indicator;
import org.technosystem.modules.marketdata.server.IDataServer;

public class STO extends Indicator<Double> {

	/**
	 * Stochastic oscillator %K
	 * About: http://en.wikipedia.org/wiki/Stochastic_oscillator
	 */
	private static final long serialVersionUID = 1L;
	
	private final int p_length;

    public STO(int length) {
    	 super();
         this.p_length = length;
    }
    
    @Override
    public Double calculate(IDataServer ds) {
    	double value = 0;

        int nbBar = Math.min(ds.size(), this.p_length);
        
        double max = ds.lastValue().getHigh();
        double min = ds.lastValue().getLow();
        double last = ds.lastValue().getClose();
        for (int bar = 0; bar < nbBar; bar++) {
        	max = Math.max(ds.valueBack(bar).getHigh(), max);
        	min = Math.min(ds.valueBack(bar).getLow(), min);
        }
        value = ((last-min) / (max-min)) * 100.;
        
        addTimedValue(ds.lastValue().getTime(), value);
        return value;
    }
}
