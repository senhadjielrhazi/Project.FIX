package org.neurosystem.modules.marketdata.indicator.basic;

import org.neurosystem.modules.marketdata.AppliedPrice;
import org.neurosystem.modules.marketdata.IQuoteServer;
import org.neurosystem.modules.marketdata.indicator.Indicator;

/**
 * Relative Strength Index. Implemented up to this specification:
 *
 * The RSI calculation appears in its original and derived form. Average Up
 * and Average Down are calculated using a simple average method for the
 * initial observation. NOTE: The initial observation is the first date shown
 * on the scrolling graph, which may or may not be seen. Subsequent values are
 * computed using these initial values in conjunction with a damping factor to
 * smooth out extreme points. The RSI equation and two averaging methods are
 * presented below.
 * RSI = 100 - [ 100/(1 + [Avg Up/Avg Dn])]
 * where
 * Avg Up: Sum of all changes for advancing periods divided by the total
 * number of RSI periods.
 * 
 * Avg Dn: Sum of all changes for declining periods divided by the total
 * number of RSI periods.
 * 
 * Subsequent RSI calculations are based on up and down sums calculated as
 * follows:
 * 
 * RSI = 100 - [100/(1 + [Next Avg Up/Next Avg Dn])]
 * 
 * Next Avg Up = [([Previous Avg Up * (RSI periods - 1)]) + today's up
 * close]/(RSI periods)
 * Next Avg Dn = [([Previous Avg Dn * (RSI periods - 1)]) + today's dn
 * close]/(RSI periods)
 * NOTE: If there is no up or down close, today's up/dn close is zero.
 */
public class RSI extends Indicator<Double> {	

	private final double p_multiplier;
    private double p_avgUt, p_avgDt ;
	
    public RSI(IQuoteServer qs, int length) {
    	this(qs, length, AppliedPrice.CLOSE);
    }

    public RSI(IQuoteServer qs, int length, AppliedPrice appliedPrice) {
    	super(qs, appliedPrice);
    	this.p_multiplier = 2. / (length + 1.);
    }
	
    @Override
    public Double calculate() {
    	double value = 0.;
		
		if(size() == 0){
			this.p_avgUt = 0.01;
			this.p_avgDt = 0.01;
		}else{
			double change = quoteServer().lastValue().getPrice(appliedPrice()) -
					quoteServer().valueBack(1).getPrice(appliedPrice());
			
			this.p_avgUt += (Math.max(change, 0.) - this.p_avgUt) * this.p_multiplier;
			this.p_avgDt += (Math.max(-change, 0.) - this.p_avgDt) * this.p_multiplier;
		}
		value = 100. - 100. / ( 1. + (this.p_avgUt / this.p_avgDt));

		addValue(value);
        return value;
    }
}
