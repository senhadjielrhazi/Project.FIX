package org.neurosystem.modules.marketdata.indicator.basic;

import org.neurosystem.modules.marketdata.AppliedPrice;
import org.neurosystem.modules.marketdata.IQuoteServer;
import org.neurosystem.modules.marketdata.indicator.Indicator;
import org.neurosystem.util.misc.Pair;

/**
 * Bollinger Band [Sma, StDev]
 */
public class EMSD extends Indicator<Pair<Double, Double>> {
	
	private final int p_length;
    
    public EMSD(IQuoteServer qs, int length) {
    	this(qs, length, AppliedPrice.CLOSE);
    }

    public EMSD(IQuoteServer qs, int length, AppliedPrice appliedPrice) {
    	super(qs, appliedPrice);
        this.p_length = length;
    }
    
    @Override
    public Pair<Double, Double> calculate() {
        Pair<Double, Double> value;
    	
        int nbBar = Math.min(quoteServer().size(), this.p_length);
    	
    	double meanSquare = 0, mean = 0;
        for (int bar = 0; bar < nbBar; bar++) {
        	double price = quoteServer().valueBack(bar).getPrice(appliedPrice());
        	mean += price / nbBar;
        	meanSquare += price * price / nbBar;
        }
        
        double stDev = 0.001;
        if(meanSquare > mean * mean){
        	stDev = Math.sqrt(meanSquare - mean * mean);
        }
        value = new Pair<Double, Double>(mean, stDev);
        
		addValue(value);
        return value;
    }
}
