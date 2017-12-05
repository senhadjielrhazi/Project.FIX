package org.technosystem.platform.api.analysis;

import java.util.List;

import org.technosystem.modules.marketdata.quote.IQuote;
import org.neurosystem.util.basic.HasTime;

public interface IPrediction extends HasTime {
	
	public List<IQuote> getForecast();
	
	public double getError();

	public double getStrength();

	public List<Double> getDataScore();

	public double getStochastic();
}
