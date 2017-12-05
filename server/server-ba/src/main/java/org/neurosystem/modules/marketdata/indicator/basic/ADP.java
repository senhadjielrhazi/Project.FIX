package org.neurosystem.modules.marketdata.indicator.basic;

import org.neurosystem.modules.marketdata.IQuoteServer;
import org.neurosystem.modules.marketdata.indicator.Indicator;
import org.neurosystem.modules.marketdata.quote.IQuote;
import org.neurosystem.util.misc.Pair;

/**
 * Accumulation/Distribution Price Technical Indicator.
 */
public class ADP extends Indicator<Pair<Double,Double>> {
	
	private final double p_multiplier;
	
    public ADP(IQuoteServer qs, int length) {
		 super(qs);
	     this.p_multiplier = 2. / (length + 1.);
    }
	
    @Override
	public Pair<Double, Double> calculate() {
		Pair<Double, Double> value;
		double adCum, adEMA;

		IQuote quote = quoteServer().lastValue();
		double ad = (((quote.getClose() - quote.getLow()) - (quote.getHigh() - quote.getClose()))
				/ (quote.getHigh() - quote.getLow()));
		
		if (size() == 0) {
			adCum = ad;
			adEMA = ad;
		} else {
			adCum = lastValue().getValue().getKey() + ad;
			adEMA = lastValue().getValue().getValue();
			adEMA += (adCum - adEMA) * this.p_multiplier;
		}
		value = new Pair<Double, Double>(adCum, adEMA);

		addValue(value);
		return value;
	}
}
