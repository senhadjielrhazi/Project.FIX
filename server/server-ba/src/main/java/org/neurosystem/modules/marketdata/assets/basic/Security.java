package org.neurosystem.modules.marketdata.assets.basic;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.neurosystem.modules.marketdata.quote.IQuote;
import org.neurosystem.util.common.annotations.javax.Nonnull;
import org.neurosystem.util.common.annotations.javax.concurrent.Immutable;
import org.neurosystem.util.common.base.Preconditions;
import org.neurosystem.util.common.collect.ImmutableMap;
import org.neurosystem.util.common.collect.ImmutableSet;

@Immutable
public final class Security implements ISecurity {

	private final double p_amount;
	private final String p_symbol;
	private final double p_minUnits;
	private final double p_pips;
	
	private final Set<ISecurity> p_securities;
	private final Map<ISecurity, Double> p_deltas;
	public final List<IQuote> p_values;
	
	private final int p_hashUID;
	
	public Security(@Nonnull double amount, @Nonnull String symbol, @Nonnull double pips, @Nonnull double minUnits){
    	Preconditions.checkArgument(amount > 0., "Trade amount can't be null or negative: %s", amount);
		this.p_amount = amount;
		
		this.p_symbol = symbol;
		this.p_pips = pips;
		this.p_minUnits = minUnits;
		
		this.p_securities = ImmutableSet.of(this);
		this.p_deltas = ImmutableMap.of(this, amount);
		this.p_values = new ArrayList<>();
		
		//Fast access UID
		this.p_hashUID = getHashUID();
	}
	
	public Security(@Nonnull String symbol, @Nonnull double pips, @Nonnull double minUnits){
		this(1., symbol, pips, minUnits);
	}
	
	public Security(@Nonnull Security security) {
		this(security.getAmount(), security.getSymbol(), security.getPipValue(), security.getMinUnits());
	}

	private int getHashUID(){
        final int prime = 31;
        int hash = 1;
        
        hash = prime * hash + this.p_symbol.hashCode();
        hash = prime * hash + Double.hashCode(this.p_pips);
        hash = prime * hash + Double.hashCode(this.p_minUnits);
        hash = prime * hash + Double.hashCode(this.p_amount);
        
	    return hash;
	}

	@Override
	public IQuote onValue(Map<ISecurity, IQuote> secPrices) {
		Preconditions.checkArgument(secPrices.containsKey(this), "Security missing: %s", this);
		
		if(this.p_values.size() >= easybites){
			this.p_values.remove(0);
		}
		this.p_values.add(secPrices.get(this));
		
		return lastValue();
	}
	
	@Override
	public Map<ISecurity, Double> getDeltas() {
		return this.p_deltas;
	}

	@Override
	public Set<ISecurity> getSecurities() {
		return this.p_securities;
	}

	@Override
	public IQuote get(int index) {
		return this.p_values.get(index);
	}

	@Override
	public int size() {
		return this.p_values.size();
	}

	@Override
	public List<IQuote> subList(int from, int to) {
		return this.p_values.subList(from, to);
	}

	@Override
	public String getSymbol() {
		return this.p_symbol;
	}

	@Override
	public double getPipValue() {
		return this.p_pips;
	}
	
	@Override
	public double getMinUnits() {
		return this.p_minUnits;
	}
	
	@Override
	public double roundLot(double lot) {
        // rounding to nearest half
		double unitsMultiplier = 1./ this.p_minUnits;
        int rounded = (int) (lot * unitsMultiplier * 10 + 0.5);
        rounded = (int) (rounded / 10d + 0.5d);
        
        return (rounded) / 1d / unitsMultiplier;
    }
	
	private double getAmount() {
		return this.p_amount;
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
	    
	    Security other = (Security) obj;
	    if (!this.p_symbol.equals(other.p_symbol))
            return false;
	    if (this.p_pips != other.p_pips)
            return false;
        if (this.p_minUnits != other.p_minUnits)
            return false;
        if (this.p_amount != other.p_amount)
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
       return String.format("Amount: %s, Symbol: %s, Pips: %s, MinUnits: %s", this.p_amount, this.p_symbol, this.p_pips, this.p_minUnits);
	}
}
