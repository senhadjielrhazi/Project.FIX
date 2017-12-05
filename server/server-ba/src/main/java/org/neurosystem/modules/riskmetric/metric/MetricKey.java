package org.neurosystem.modules.riskmetric.metric;

import org.neurosystem.util.common.annotations.javax.Nonnull;
import org.neurosystem.util.common.annotations.javax.concurrent.Immutable;

@Immutable
public final class MetricKey implements IMetricKey {
	
	private final double p_tnb;//TNB
	private final double p_pwp;//PWP
	
	private final double p_pnl;//PNL
	private final double p_mae;//MAE
	private final double p_mfe;//MFE
	
	private final int p_hashUID;
	
	public MetricKey(double tnb, double pwp, double pnl, double mae, double mfe){
		this.p_tnb = tnb;
		this.p_pwp = pwp;
		
		this.p_pnl = pnl;
		this.p_mae = mae;
		this.p_mfe = mfe;
		
		//Fast access UID
		this.p_hashUID = getHashUID();
	}
	
	public MetricKey(@Nonnull IMetricKey risk) {
		this(risk.getTNB(), risk.getPWP(), risk.getPNL(), risk.getMAE(), risk.getMFE());
	}

	private int getHashUID(){
		final int prime = 31;
		int hash = 1;
		
		hash = prime * hash + Double.hashCode(this.p_tnb);
		hash = prime * hash + Double.hashCode(this.p_pwp);
		hash = prime * hash + Double.hashCode(this.p_pnl);
		hash = prime * hash + Double.hashCode(this.p_mae);
		hash = prime * hash + Double.hashCode(this.p_mfe);
		
		return hash;
	}
	
	@Override
	public double getTNB() {
		return this.p_tnb;
	}
	
	@Override
	public double getPWP() {
		return this.p_pwp;
	}
	
	@Override
	public double getPNL() {
		return this.p_pnl;
	}
	
	@Override
	public double getMAE() {
		return this.p_mae;
	}
	
	@Override
	public double getMFE() {
		return this.p_mfe;
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
	    
	    MetricKey other = (MetricKey) obj;
	    if (this.p_tnb != other.p_tnb)
            return false;
        if (this.p_pwp != other.p_pwp)
            return false;
	    if (this.p_pnl != other.p_pnl)
            return false;
        if (this.p_mae != other.p_mae)
            return false;
        if (this.p_mfe != other.p_mfe)
            return false;
        
		return true;
	}
	
	/* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
	@Override
	public int hashCode() {
	    return this.p_hashUID;
	}
	
	 /* (non-Javadoc)
    * @see java.lang.Object#toString()
    */
	@Override
	public String toString() {
		return String.format("TNB: %s, PWP: %s, PNL: %s, MAE: %s, MFE: %s", 
				this.p_tnb, this.p_pwp, this.p_pnl, this.p_mae, this.p_mfe);
	}
	
	public static IMetricKey valueOf(String regex){
		String[] split = regex.split("TNB: |, PWP: |, PNL: |, MAE: |, MFE: ");
		
		double tnb = Double.valueOf(split[1]);
		double pwp = Double.valueOf(split[2]);
		double pnl = Double.valueOf(split[3]);
		double mae = Double.valueOf(split[4]);
		double mfe = Double.valueOf(split[5]);
		
		return new MetricKey(tnb, pwp, pnl, mae, mfe);
	}
}
