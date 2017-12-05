package org.neurosystem.modules.riskmetric.trade;

import org.neurosystem.modules.riskmetric.pnl.IPnLKey;
import org.neurosystem.util.basic.HasTime;
import org.neurosystem.util.common.annotations.javax.Nonnull;
import org.neurosystem.util.common.annotations.javax.concurrent.Immutable;

@Immutable
public final class TradeKey implements ITradeKey {
	
	private final double p_pnl;//PNL
	private final double p_mae;//MAE
	private final double p_mfe;//MFE
	
	private final long p_time;
	private final Side p_side;
	
	private final int p_hashUID;
	
	public TradeKey(@Nonnull long time, @Nonnull Side side,   
			double pnl, double mae, double mfe){
		this.p_pnl = pnl;
		this.p_mae = mae;
		this.p_mfe = mfe;
		
		this.p_time = time;
		this.p_side = side;
		
		//Fast access UID
		this.p_hashUID = getHashUID();
	}
	
	public TradeKey(@Nonnull long time, @Nonnull Side side, @Nonnull IPnLKey pnlKey){
		this(time, side, pnlKey.getPNL(), pnlKey.getMAE(), pnlKey.getPNL());
	}
	
	public TradeKey(@Nonnull ITradeKey risk) {
		this(risk.getTime(), risk.getSide(),
				risk.getPNL(), risk.getMAE(), risk.getMFE());
	}

	private int getHashUID(){
		final int prime = 31;
		int hash = 1;
		
		hash = prime * hash + Double.hashCode(this.p_pnl);
		hash = prime * hash + Double.hashCode(this.p_mae);
		hash = prime * hash + Double.hashCode(this.p_mfe);
		hash = prime * hash + Long.hashCode(this.p_time);
		hash = prime * hash + this.p_side.hashCode();
		
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
	
	@Override
	public long getTime() {
		return this.p_time;
	}
	
	@Override
	public Side getSide() {
		return this.p_side;
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
	    
	    TradeKey other = (TradeKey) obj;
	    if (this.p_pnl != other.p_pnl)
            return false;
        if (this.p_mae != other.p_mae)
            return false;
        if (this.p_mfe != other.p_mfe)
            return false;
        if (this.p_time != other.p_time)
            return false;
        if (this.p_side.equals(other.p_side))
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
		return String.format("Time: %s, Side: %s, PnLKey: [ PNL: %s, MAE: %s, MFE: %s ]", 
				formatedTime(), this.p_side, this.p_pnl, this.p_mae, this.p_mfe);
	}
	
	public static ITradeKey valueOf(String regex){
		String[] split = regex.split("Time: |, Side: |, PnLKey: \\[ PNL: |, MAE: |, MFE: |\\]");
		
		long time = HasTime.parse(split[1]);
		Side side = Side.valueOf(split[2]);
		double pnl = Double.valueOf(split[3]);
		double mae = Double.valueOf(split[4]);
		double mfe = Double.valueOf(split[5]);
		
		return new TradeKey(time, side, pnl, mae, mfe);
	}
	
	public static ITradeKey flipsyde(ITradeKey risk){
		return new TradeKey(risk.getTime(), risk.getSide().flipsyde(),
				risk.getPNL(), risk.getMAE(), risk.getMFE());
	}
}
