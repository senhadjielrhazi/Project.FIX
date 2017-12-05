package org.neurosystem.modules.marketdata.indicator.basic;

import java.util.ArrayList;
import java.util.List;

import org.neurosystem.modules.marketdata.IQuoteServer;
import org.neurosystem.modules.marketdata.indicator.Indicator;
import org.neurosystem.modules.marketdata.quote.IQuote;
import org.neurosystem.util.misc.Pair;

/**
 * Momentum of the period.
 */
public class MOM extends Indicator<Pair<Double[], Double[]>> {

	private final int p_length, p_perMinMax;
	private final List<IQuote> p_bars;

    public MOM(IQuoteServer qs, int length, int perMinMax) {
    	 super(qs);
         this.p_length = length;
         this.p_perMinMax = perMinMax;
         this.p_bars = new ArrayList<>();
    }
	
    @Override
	public Pair<Double[], Double[]> calculate() {
		Pair<Double[], Double[]> value;

		Double[] maxData = new Double[this.p_length];
		Double[] minData = new Double[this.p_length];

		if (this.p_bars.size() < this.p_length * this.p_perMinMax) {
			this.p_bars.add(quoteServer().lastValue());

			double max = this.p_bars.get(0).getHigh();
			double min = this.p_bars.get(0).getLow();
			for (int k = 1; k < this.p_bars.size(); k++) {
				max = Math.max(max, this.p_bars.get(k).getHigh());
				min = Math.min(min, this.p_bars.get(k).getLow());
			}
			for (int index = 0; index < this.p_length; index++) {
				maxData[index] = max;
				minData[index] = min;
			}
		} else {
			this.p_bars.add(quoteServer().lastValue());
			this.p_bars.remove(0);

			for (int index = 0; index < this.p_length; index++) {
				int k0 = index * this.p_perMinMax;
				double max = this.p_bars.get(k0).getHigh();
				double min = this.p_bars.get(k0).getLow();
				for (int j = 1; j < this.p_perMinMax; j++) {
					int k = k0 + j;
					max = Math.max(max, this.p_bars.get(k).getHigh());
					min = Math.min(min, this.p_bars.get(k).getLow());
				}
				maxData[index] = max;
				minData[index] = min;
			}
		}
		value = new Pair<Double[], Double[]>(minData, maxData);

		addValue(value);
		return value;
	}
}
