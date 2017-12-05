package org.neurosystem.modules.riskmetric.metric;

import org.neurosystem.modules.riskmetric.pnl.IPnLKey;
import org.neurosystem.util.common.annotations.javax.Nonnull;

public interface IMetricKey extends IPnLKey {
	
	public double getTNB();
	
	public double getPWP();
	
	public default double getTPL(){
		return getTNB() * getPNL();
	}
	
	public static IMetricKey sum(@Nonnull IMetricKey metricA, @Nonnull IMetricKey metricB) {
		double tnbA = metricA.getTNB();
		double wnbA = metricA.getPWP() * tnbA;
		
		double pnlA = metricA.getPNL() * tnbA;
		double maeA = metricA.getMAE() * tnbA;
		double mfeA = metricA.getMFE() * tnbA;
		
		double tnbB = metricB.getTNB();
		double wnbB = metricB.getPWP() * tnbB;
		
		double pnlB = metricB.getPNL() * tnbB;
		double maeB = metricB.getMAE() * tnbB;
		double mfeB = metricB.getMFE() * tnbB;
		
		double tnb = tnbA + tnbB;
		double pwp = (wnbA + wnbB) / tnb;
		double pnl = (pnlA + pnlB) / tnb;
		double mae = (maeA + maeB) / tnb;
		double mfe = (mfeA + mfeB) / tnb;
		
		return new MetricKey(tnb, pwp, pnl, mae, mfe);
	}
}
