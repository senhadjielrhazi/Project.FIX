package org.neurosystem.modules.riskmetric.win;

import org.neurosystem.util.Parameters;
import org.neurosystem.util.common.annotations.javax.Nonnull;
import org.neurosystem.util.common.annotations.javax.concurrent.Immutable;

@Immutable
public final class WinKey implements IWinKey {
	
	private final double p_tnb;//MAE
	private final double p_pnl;//PNL
	private final double p_pwp;//MFE
	
	private final int p_hashUID;
	
	public WinKey(double tnb, double pnl, double pwp){
		this.p_tnb = tnb;
		this.p_pnl = pnl;
		this.p_pwp = pwp;
		
		//Fast access UID
		this.p_hashUID = getHashUID();
	}
	
	public WinKey(@Nonnull IWinKey risk) {
		this(risk.getTNB(), risk.getPNL(), risk.getPWP());
	}

	private int getHashUID(){
		final int prime = 31;
		int hash = 1;
		
		hash = prime * hash + Double.hashCode(this.p_tnb);
		hash = prime * hash + Double.hashCode(this.p_pnl);
		hash = prime * hash + Double.hashCode(this.p_pwp);
		
		return hash;
	}
	
	@Override
	public double getTNB() {
		return this.p_tnb;
	}
	
	@Override
	public double getPNL() {
		return this.p_pnl;
	}
	
	@Override
	public double getPWP() {
		return this.p_pwp;
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
	    
	    WinKey other = (WinKey) obj;
	    if (this.p_tnb != other.p_tnb)
            return false;
        if (this.p_pnl != other.p_pnl)
            return false;
        if (this.p_pwp != other.p_pwp)
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
		return String.format("TNB: %s, PNL: %s, PWP: %s", this.p_tnb, this.p_pnl, this.p_pwp);
	}
	
	public static IWinKey valueOf(String regex){
		String[] split = regex.split("TNB: |, PNL: |, PWP: ");
		
		double tnb = Double.valueOf(split[1]);
		double pnl = Double.valueOf(split[2]);
		double pwp = Double.valueOf(split[3]);
		
		return new WinKey(tnb, pnl, pwp);
	}
	
	public static IWinKey getBasicWin(){
		double tnb = Parameters.getBasicWin()[0];
		double pnl = Parameters.getBasicWin()[1];
		double pwp = Parameters.getBasicWin()[2];

		return new WinKey(tnb, pnl, pwp);
	}
}
