package org.neurosystem.modules.marketdata.indicator.custom;

import org.neurosystem.modules.marketdata.indicator.IIndicator;
import org.neurosystem.util.misc.Pair;

/**
 * Generic MinMax of an Indicator.
 */
public class GMMX extends IndComp<Pair<Double, Double>> {

	private final IIndicator<Double> p_ind;
	private final int p_length;

    public GMMX(IIndicator<Double> ind, int length) {
    	 super();
    	 this.p_ind = ind;
         this.p_length = length;
    }	
	
    @Override
    public Pair<Double, Double> calculate() {
    	Pair<Double, Double> value;
    	
    	int nbBar = Math.min(this.p_ind.size(), this.p_length);
    	
    	double min = this.p_ind.lastValue().getValue();
    	double max = min;
        for (int bar = 0; bar < nbBar; bar++) {
        	min = Math.min(min, this.p_ind.valueBack(bar).getValue());
        	max = Math.max(max, this.p_ind.valueBack(bar).getValue());
        }
        value = new Pair<Double, Double>(min, max);
        
		addTimedValue(this.p_ind.lastValue().getTime(), value);
        return value;
    }
}