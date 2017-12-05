package org.technosystem.platform.api.analysis;

import java.util.List;

import org.technosystem.modules.marketdata.quote.IQuote;
import org.neurosystem.util.common.annotations.javax.concurrent.Immutable;

@Immutable
public final class Prediction implements IPrediction {

	private final List<IQuote> p_forecast;
	private final double p_error;
	private final long p_time;
	
	private final double p_strength;
	
	private final List<Double> p_dataScore;
	private final double p_stochastic;
	
	private int p_hashUID = 0;

	public Prediction(List<IQuote> forecast, Double error, long time, 
			double strength, List<Double> dataScore, double stochastic) {
		this.p_forecast = forecast;
		this.p_error = error;
		this.p_time = time;
		
		this.p_strength = strength;
		
		this.p_dataScore = dataScore;
		this.p_stochastic= stochastic;
	}

	private int getHashUID() {
		final int prime = 31;
		int hash = 1;

		hash = prime * hash + this.p_forecast.hashCode();
		hash = prime * hash + Double.hashCode(this.p_error);
		hash = prime * hash + Long.hashCode(this.p_time);

		hash = prime * hash + Double.hashCode(this.p_strength);
		
		hash = prime * hash + this.p_dataScore.hashCode();
		hash = prime * hash + Double.hashCode(this.p_stochastic);
		
		return hash;
	}
	
	@Override
	public long getTime() {
		return this.p_time;
	}

	@Override
	public List<IQuote> getForecast() {
		return this.p_forecast;
	}
	
	@Override
	public double getError() {
		return this.p_error;
	}

	@Override
	public double getStrength() {
		return this.p_strength;
	}
	
	@Override
	public List<Double> getDataScore() {
		return this.p_dataScore;
	}
	
	@Override
	public double getStochastic() {
		return this.p_stochastic;
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}

		Prediction other = (Prediction) obj;
		if (this.p_time != other.p_time)
			return false;
		if (!this.p_forecast.equals(other.p_forecast))
			return false;
		if (this.p_error != other.p_error)
			return false;
		
		if (this.p_strength != other.p_strength)
			return false;
		
		if (!this.p_dataScore.equals(other.p_dataScore))
			return false;
		if (this.p_stochastic != other.p_stochastic)
			return false;
		
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		if (this.p_hashUID == 0) {
			this.p_hashUID = getHashUID();
		}
		return this.p_hashUID;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return String.format("Time: %s, Error: %s, Forecast: %s, Strength: %s, DataScore: %s, STO: %s", 
				formatedTime(), this.p_error, this.p_forecast,
				this.p_strength, this.p_dataScore, this.p_stochastic);
	}
}
