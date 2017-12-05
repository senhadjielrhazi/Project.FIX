package org.neurosystem.modules.riskmetric.pnl;

import org.neurosystem.util.Parameters;
import org.neurosystem.util.common.annotations.javax.Nonnull;
import org.neurosystem.util.common.annotations.javax.concurrent.Immutable;

@Immutable
public final class PnLKey implements IPnLKey {
	
	private final double p_pnl;//PNL
	private final double p_mae;//MAE
	private final double p_mfe;//MFE
	
	private final int p_hashUID;
	
	public PnLKey(double pnl, double mae, double mfe){
		this.p_pnl = pnl;
		this.p_mae = mae;
		this.p_mfe = mfe;
		
		//Fast access UID
		this.p_hashUID = getHashUID();
	}
	
	public PnLKey(@Nonnull IPnLKey risk) {
		this(risk.getPNL(), risk.getMAE(), risk.getMFE());
	}

	private int getHashUID(){
		final int prime = 31;
		int hash = 1;
		
		hash = prime * hash + Double.hashCode(this.p_pnl);
		hash = prime * hash + Double.hashCode(this.p_mae);
		hash = prime * hash + Double.hashCode(this.p_mfe);
		
		return hash;
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
	    
	    PnLKey other = (PnLKey) obj;
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
		return String.format("PNL: %s, MAE: %s, MFE: %s", this.p_pnl, this.p_mae, this.p_mfe);
	}
	
	public static IPnLKey valueOf(String regex){
		String[] split = regex.split("PNL: |, MAE: |, MFE: ");
		
		double pnl = Double.valueOf(split[1]);
		double mae = Double.valueOf(split[2]);
		double mfe = Double.valueOf(split[3]);
		
		return new PnLKey(pnl, mae, mfe);
	}
	
	public static IPnLKey getBasicPNL(){
		double pnl = Parameters.getBasicPNL()[0];
		double mae = Parameters.getBasicPNL()[1];
		double mfe = Parameters.getBasicPNL()[2];
		
		return new PnLKey(pnl, mae, mfe);
	}
}
