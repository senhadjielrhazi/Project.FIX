package org.neurosystem.modules.marketdata.indicator.basic;

import org.neurosystem.modules.marketdata.AppliedPrice;
import org.neurosystem.modules.marketdata.IQuoteServer;
import org.neurosystem.modules.marketdata.indicator.Indicator;

/**
 * Commodity Channel Index
 */
public class CCI extends Indicator<Double> {
	
	private final int p_length;
    
    public CCI(IQuoteServer qs, int length) {
    	this(qs, length, AppliedPrice.TYPICAL);
    }

    public CCI(IQuoteServer qs, int length, AppliedPrice appliedPrice) {
    	super(qs, appliedPrice);
        this.p_length = length;
    }
    
    @Override
    public Double calculate() {
    	double value;
    	
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
        
        double tp = quoteServer().lastValue().getPrice(appliedPrice());
        value = (tp - mean)/(0.015 * stDev);
        
		addValue(value);
        return value;
    }
}
