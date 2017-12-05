package org.neurosystem.platform.trade;

import org.neurosystem.modules.marketdata.assets.IAsset;
import org.neurosystem.modules.marketdata.quote.IQuote;
import org.neurosystem.modules.riskmetric.pnl.IPnLKey;
import org.neurosystem.util.common.annotations.javax.Nonnull;

public abstract class AbstractTrade implements ITrade {
	
	//Immutable
	private final IAsset p_asset;
	private final long p_time;
	private final Side p_side;
	private final double p_price;
	private final IPnLKey p_target;
	
	private final int p_hashUID;
	
	//Mutable
	private boolean p_isActive;
	private double p_pnl;//PNL
	private double p_mae;//MAE
	private double p_mfe;//MFE
	
	public AbstractTrade(@Nonnull IAsset asset, @Nonnull Side side, @Nonnull long time, @Nonnull double price, @Nonnull IPnLKey target) {
		this.p_asset = asset;
		this.p_side = side;
		
		this.p_time = time;
		this.p_price = price; 
		this.p_target = target;
		
		this.p_isActive = true;
		this.p_pnl = 0.;
		this.p_mae = 0.;
		this.p_mfe = 0.;
		
		//Fast access UID
		this.p_hashUID = getHashUID();
	}

	private int getHashUID(){
		final int prime = 31;
        int hash = 1;
        
        hash = prime * hash + this.p_asset.hashCode();
        hash = prime * hash + this.p_side.hashCode();
        hash = prime * hash + Long.hashCode(this.p_time);
        hash = prime * hash + Double.hashCode(this.p_price);
        hash = prime * hash + this.p_target.hashCode();
        
	    return hash;
	}
	
	@Override
	public void onQuote(@Nonnull IQuote quote) {
		if(this.p_isActive){
			//Manage the StopLoss, TakeProfit 
			if(this.p_side.isLong()){
				this.p_pnl = (quote.getClose()-this.p_price)/this.p_asset.getPipValue();
				
				Double mae = (quote.getLow()-this.p_price)/this.p_asset.getPipValue();
				this.p_mae = Math.max(-mae, this.p_mae);
				
				Double mfe = (quote.getHigh()-this.p_price)/this.p_asset.getPipValue();
				this.p_mfe = Math.max(mfe, this.p_mfe);
			}else{
				this.p_pnl = (this.p_price-quote.getClose())/this.p_asset.getPipValue();
				
				Double mae = (this.p_price-quote.getHigh())/this.p_asset.getPipValue();
				this.p_mae = Math.max(-mae, this.p_mae);

				Double mfe = (this.p_price-quote.getLow())/this.p_asset.getPipValue();
				this.p_mfe = Math.max(mfe, this.p_mfe);
			}
			
			//StopLoss
			if(this.p_pnl < - this.p_target.getMAE()){
				this.p_pnl = - this.p_target.getMAE();
				this.p_isActive = false; 
				return;
			}
			
			//TakeProfit
			double tkp = (this.p_mae >= 2. * this.p_target.getMAE() / 3.)?
					(-this.p_target.getMAE() / 3.) : this.p_target.getMFE();
			if(this.p_pnl >= tkp){
				this.p_pnl = tkp;
				this.p_isActive = false; 
				return;
			}
		}
	}
	
	protected IAsset getAsset() {
		return this.p_asset;
	}
	
	@Override
	public Side getSide() {
		return this.p_side;
	}
	
	@Override
	public long getTime() {
		return this.p_time;
	}
	
	protected double getPrice() {
		return this.p_price;
	}
	
	protected IPnLKey getTarget() {
		return this.p_target;
	}
	
	@Override
	public boolean isActive() {
		return this.p_isActive;
	}
	
	@Override
	public void closeTrade() {
		this.p_isActive = false;
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
	    
	    AbstractTrade other = (AbstractTrade) obj;
	    if (this.p_time != other.p_time)
            return false;
        if (this.p_price != other.p_price)
            return false;
        if (!this.p_side.equals(other.p_side))
            return false;
        if (!this.p_asset.equals(other.p_asset))
            return false;
        if (!this.p_target.equals(other.p_target))
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
		return String.format("Time: %s, Price: %s, Side: %s, Risk: [ PNL: %s, MAE: %s, MFE: %s ], Asset: [ %s ], Target: [ %s ]", 
				formatedTime(), this.p_price, this.p_side, this.p_pnl, this.p_mae, this.p_mfe, this.p_asset, this.p_target);       
	}
}