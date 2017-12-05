package org.neurosystem.modules.marketdata.indicator;

import java.util.*;

import org.neurosystem.modules.marketdata.AppliedPrice;
import org.neurosystem.modules.marketdata.IQuoteServer;
import org.neurosystem.util.basic.HasValues;
import org.neurosystem.util.common.annotations.javax.Nonnull;
import org.neurosystem.util.common.base.Objects;

/**
 * Base class for all classes implementing technical indicators.
 */
public abstract class Indicator<V> implements IIndicator<V> {
	
	private final List<ITimedValue<V>> p_values;
	private final IQuoteServer p_qs;
	private final AppliedPrice p_appliedPrice;
	
	public abstract V calculate();// must be implemented in subclasses.
	
	public Indicator(@Nonnull IQuoteServer qs) {
		this(qs, AppliedPrice.CLOSE);
    }
	
	public Indicator(@Nonnull IQuoteServer qs, @Nonnull AppliedPrice appliedPrice) {
		this.p_values = new ArrayList<>();
        this.p_qs = qs;
		this.p_appliedPrice = appliedPrice;
    }
	
	@Override
	public ITimedValue<V> get(int index) {
		return this.p_values.get(index);
	}

	@Override
	public int size() {
		return this.p_values.size();
	}

	@Override
	public List<ITimedValue<V>> subList(int from, int to) {
		return this.p_values.subList(from, to);
	}
	
	private void addTimedValue(Long time, V value) {
		if(this.p_values.size() >= easybites){
			this.p_values.remove(0);
		}
		this.p_values.add(new TimedValue<V>(time, value));
    }
	
	protected void addValue(V value) {
		addTimedValue(this.p_qs.lastValue().getTime(), value);
    }
	
	protected IQuoteServer quoteServer(){
		return this.p_qs;
	}

	protected AppliedPrice appliedPrice(){
		return this.p_appliedPrice;
	}
	
	/* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
	@Override
	public boolean equals(Object obj){
		if (obj instanceof Indicator) {
			Indicator<?> that = (Indicator<?>) obj;
			return (this.p_appliedPrice.equals(that.p_appliedPrice)) 
					&& Objects.equal(this.p_values, that.p_values);
		}
        
		return false;
	}
	
	/* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
	@Override
	public int hashCode() {
		return (this.p_appliedPrice.hashCode()) ^ 
				 ((this.p_values == null) ? 0 : this.p_values.hashCode());
	}
	
	 /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
	@Override
	public String toString() {
        return String.format("%s", HasValues.formatedValues(this.p_values));
	}
}
