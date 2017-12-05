package org.neurosystem.modules.marketdata.indicator.basic;

import org.neurosystem.modules.marketdata.AppliedPrice;
import org.neurosystem.modules.marketdata.IQuoteServer;
import org.neurosystem.modules.marketdata.indicator.Indicator;
import org.neurosystem.util.misc.Pair;

/**
 * Ups Down Statics Indicator.
 */
public class UDS extends Indicator<Pair<Double,Double>> {

	private final int p_length;
	private final double p_multiplier;
	
    public UDS(IQuoteServer qs, int length, int smooth) {
    	this(qs, length, smooth, AppliedPrice.CLOSE);
    }
    
    public UDS(IQuoteServer qs, int length, int smooth, AppliedPrice appliedPrice) {
   	 	super(qs);
        this.p_length = length;
        this.p_multiplier = 2. / (smooth + 1.);
   }

    @Override
    public Pair<Double,Double> calculate() {
    	Pair<Double, Double> value;
    	double smoothEMA;
    	
    	int nbBar = Math.min(quoteServer().size(), this.p_length);
    	
    	double up = 0., dw = 0., ft = 0.01;
        for (int bar = 1; bar < nbBar; bar++) {
        	double p0 = quoteServer().valueBack(bar-1).getPrice(appliedPrice());
        	double p1 = quoteServer().valueBack(bar).getPrice(appliedPrice());
        	
        	if(p1 > p0){
        		up++;
        	}else if(p1 < p0){
        		dw++;
        	}else{
        		ft++;
        	}
        }
        double hg = 50.*(up - dw)/(up + dw + ft) + 50.;
        
        if(size() == 0){
        	smoothEMA = hg;
		}else{
			smoothEMA = lastValue().getValue().getValue();
			smoothEMA += (hg - smoothEMA) * this.p_multiplier;
		}
        value =  new Pair<Double, Double>(hg, smoothEMA);
        
		addValue(value);
        return value;
    }
}
