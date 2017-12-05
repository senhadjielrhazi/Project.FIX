package org.neurosystem.modules.marketdata.indicator.basic;

import org.neurosystem.modules.marketdata.AppliedPrice;
import org.neurosystem.modules.marketdata.IQuoteServer;
import org.neurosystem.modules.marketdata.indicator.Indicator;
import org.neurosystem.util.misc.Pair;

/**
 * MACD
 */
public class MACD extends Indicator<Pair<Double,Double>> {
	
	private final double p_fastMultip, p_slowMultip, p_triggerMultip;
    private double p_fastEMA, p_slowEMA, p_triggerEMA;
    
    public MACD(IQuoteServer qs, int fastLength, int slowLength, int trigger) {
    	this(qs, fastLength, slowLength, trigger, AppliedPrice.CLOSE);
    }

    public MACD(IQuoteServer qs, int fastLength, int slowLength, int trigger, AppliedPrice appliedPrice) {
    	super(qs, appliedPrice);
        
        this.p_fastMultip = 2. / (fastLength + 1.);
        this.p_slowMultip = 2. / (slowLength + 1.);
        this.p_triggerMultip = 2. / (trigger + 1.);
    }
    
    @Override
    public Pair<Double, Double> calculate() {
    	Pair<Double, Double> value;
    	
    	double price = quoteServer().lastValue().getPrice(appliedPrice());
    	if(size() == 0){
    		this.p_fastEMA = price;
    		this.p_slowEMA = price;
    		this.p_triggerEMA = 0;
		}else{
			this.p_fastEMA += (price - this.p_fastEMA) * this.p_fastMultip;
			this.p_slowEMA += (price - this.p_slowEMA) * this.p_slowMultip;
			this.p_triggerEMA += ((this.p_fastEMA - this.p_slowEMA) - this.p_triggerEMA) * this.p_triggerMultip;
		}
        value = new Pair<Double, Double>(this.p_fastEMA - this.p_slowEMA, this.p_triggerEMA);

        addValue(value);
        return value;
    }
}
