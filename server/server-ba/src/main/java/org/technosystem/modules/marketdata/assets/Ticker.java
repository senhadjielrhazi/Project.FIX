package org.technosystem.modules.marketdata.assets;

import org.neurosystem.util.common.annotations.javax.Nonnull;
import org.neurosystem.util.common.annotations.javax.concurrent.Immutable;
import org.neurosystem.util.common.base.Preconditions;

@Immutable
public final class Ticker implements ITicker {

	private final double p_units;
	private final String p_symbol;
	private final String p_base;
	private final String p_term;

	private final double p_pips;
	private final double p_minUnits;

	private int p_hashUID = 0;

	public Ticker(@Nonnull double units, @Nonnull String symbol, @Nonnull String base, @Nonnull String term, @Nonnull double pips,
			@Nonnull double minUnits) {
		Preconditions.checkArgument(units > 0., "Ticker units can't be null or negative: %s", units);//TODO check sz vs minsz
		
		this.p_units = units;
		this.p_symbol = symbol;
		this.p_base = base;
		this.p_term = term;

		this.p_pips = pips;
		this.p_minUnits = minUnits;
	}

	public Ticker(@Nonnull double amount, @Nonnull String symbol, @Nonnull double pips, @Nonnull double minUnits) {
		Preconditions.checkArgument(amount > 0., "Ticker amount can't be null or negative: %s", amount);
		
		String[] split = symbol.split("/");
		Preconditions.checkArgument(split.length == 2, "Bad symbol: %s", symbol);
		
		this.p_units = amount;
		this.p_symbol = symbol;
		this.p_base = split[0];
		this.p_term = split[1];

		this.p_pips = pips;
		this.p_minUnits = minUnits;
	}

	public Ticker(@Nonnull ITicker ticker) {
		this(ticker.getUnits(), ticker.getSymbol(), ticker.getBase(), ticker.getTerm(), ticker.getPipValue(),
				ticker.getMinUnits());
	}

	private int getHashUID() {
		final int prime = 31;
		int hash = 1;
		
		hash = prime * hash + Double.hashCode(this.p_units);
		hash = prime * hash + this.p_symbol.hashCode();
		hash = prime * hash + this.p_base.hashCode();
		hash = prime * hash + this.p_term.hashCode();
		hash = prime * hash + Double.hashCode(this.p_pips);
		hash = prime * hash + Double.hashCode(this.p_minUnits);

		return hash;
	}
	
	@Override
	public double getUnits() {
		return this.p_units;
	}
	
	@Override
	public String getSymbol() {
		return this.p_symbol;
	}

	@Override
	public String getBase() {
		return this.p_base;
	}

	@Override
	public String getTerm() {
		return this.p_term;
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
	public double roundSZ(double size) {
		// rounding to nearest half
		double unitsMultiplier = 1. / this.p_minUnits;
		int rounded = (int) (size * unitsMultiplier * 10 + 0.5);
		rounded = (int) (rounded / 10d + 0.5d);

		return (rounded) / 1d / unitsMultiplier;
	}

	@Override
	public double roundPZ(double price) {
		// rounding to nearest half, 0, 0.5, or 1
		double pipsMultiplier = 1. / this.p_pips;
		int rounded = (int) (price * pipsMultiplier * 10 + 0.5);
		rounded = (int) ((2 * rounded) / 10d + 0.5d);

		return (rounded) / 2d / pipsMultiplier;
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

		Ticker other = (Ticker) obj;
		if (this.p_units != other.p_units)
			return false;
		if (!this.p_symbol.equals(other.p_symbol))
			return false;
		if (!this.p_base.equals(other.p_base))
			return false;
		if (!this.p_term.equals(other.p_term))
			return false;
		if (this.p_pips != other.p_pips)
			return false;
		if (this.p_minUnits != other.p_minUnits)
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
		return String.format("Units: %s, Symbol: %s, Base: %s, Term: %s, Pips: %s, MinUnits: %s", this.p_units, this.p_symbol, this.p_base,
				this.p_term, this.p_pips, this.p_minUnits);
	}
}
