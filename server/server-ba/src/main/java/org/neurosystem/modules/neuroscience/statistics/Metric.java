package org.neurosystem.modules.neuroscience.statistics;

import org.neurosystem.modules.riskmetric.metric.IMetricKey;
import org.neurosystem.modules.riskmetric.trade.ITradeKey;
import org.neurosystem.util.common.annotations.javax.Nonnull;

public class Metric implements IMetric {
	
	private double p_tnb = 0.001;
	private double p_wnb = 0.0;
	
	private double p_pnl = 0.0;
	private double p_mae = 0.0;
	private double p_mfe = 0.0;
	
	public Metric(){
	}
	
	@Override
	public void addEntry(@Nonnull ITradeKey trade) {		
		this.p_tnb++;
		if(trade.getPNL() > 0){
			this.p_wnb++;
		}
		
		this.p_pnl += trade.getPNL();
		this.p_mae += trade.getMAE();
		this.p_mfe += trade.getMFE();
	}

	@Override
	public void addMetric(@Nonnull IMetricKey metric) {
		double tnb = metric.getTNB();
		double wnb = metric.getPWP() * tnb;
		
		double pnl = metric.getPNL() * tnb;
		double mae = metric.getMAE() * tnb;
		double mfe = metric.getMFE() * tnb;
		
		this.p_tnb += tnb;
		this.p_wnb += wnb;
		
		this.p_pnl += pnl;
		this.p_mae += mae;
		this.p_mfe += mfe;
	}
	
	@Override
	public double getTNB() {
		return this.p_tnb;
	}
	
	@Override
	public double getPWP() {
		return this.p_wnb/this.p_tnb;
	}

	@Override
	public double getPNL() {
		return this.p_pnl/this.p_tnb;
	}

	@Override
	public double getMAE() {
		return this.p_mae/this.p_tnb;
	}

	@Override
	public double getMFE() {
		return this.p_mfe/this.p_tnb;
	}
	
	/* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
	@Override
	public boolean equals(Object obj){
		if (obj == this){
	        return true;
	    }
	    if (obj == null){
	        return false;
	    }
	    if (getClass() != obj.getClass()){
            return false;
	    }
	    
	    IMetric other = (IMetric) obj;
	    if (getTNB() != other.getTNB())
            return false;
	    if (getPWP() != other.getPWP())
            return false;
	    if (getPNL() != other.getPNL())
            return false;
        if (getMAE() != other.getMAE())
            return false;
        if (getMFE() != other.getMFE())
            return false;
        
		return true;
	}

	/* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
	@Override
	public int hashCode() {
        final int prime = 31;
        int result = 1;
        
        result = prime * result + Double.hashCode(getTNB());
        result = prime * result + Double.hashCode(getPWP());
        result = prime * result + Double.hashCode(getPNL());
        result = prime * result + Double.hashCode(getMAE());
        result = prime * result + Double.hashCode(getMFE());

	    return result;
	}
	
	 /* (non-Javadoc)
    * @see java.lang.Object#toString()
    */
	@Override
	public String toString() {
       return String.format("TPL: %s, TNB: %s, PWP: %s, PNL: %s, MAE: %s, MFE: %s", //$NON-NLS-1$
       		getTPL(), getTNB(), getPWP(), getPNL(), getMAE(), getMFE());
	}
}
