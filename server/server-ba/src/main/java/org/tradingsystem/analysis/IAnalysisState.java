package org.tradingsystem.analysis;

import java.util.*;

public interface IAnalysisState {
	public void updateParameters(long barTime, double price, double pnl,
			List<List<Double>> aiDataVectors);

	public long getTime();

	public double getPrice();

	public List<List<Double>> getAIData();
	
	public double getPnL();

	public String to_String();

	public boolean equalDataVector(List<List<Double>> allAIDataVector);
}
