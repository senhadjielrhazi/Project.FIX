package org.neurosystem.modules.marketdata.indicator.custom;

import java.util.ArrayList;
import java.util.List;

import org.neurosystem.modules.marketdata.indicator.IIndicator;
import org.neurosystem.modules.marketdata.indicator.ITimedValue;
import org.neurosystem.modules.marketdata.indicator.TimedValue;
import org.neurosystem.util.basic.HasValues;
import org.neurosystem.util.common.base.Objects;

/**
 * Base class for all classes implementing composite technical indicators.
 */
public abstract class IndComp<V> implements IIndicator<V> {
	
	private final List<ITimedValue<V>> p_values;
	
	public abstract V calculate();// must be implemented in subclasses.
	
	public IndComp() {
		this.p_values = new ArrayList<>();
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
	
	protected void addTimedValue(long time, V value) {
		if(this.p_values.size() >= easybites){
			this.p_values.remove(0);
		}
		this.p_values.add(new TimedValue<V>(time, value));
    }
	
	/* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
	@Override
	public boolean equals(Object obj){
		if (obj instanceof IndComp) {
			IndComp<?> that = (IndComp<?>) obj;
			return Objects.equal(this.p_values, that.p_values);
		}
        
		return false;
	}
	
	/* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
	@Override
	public int hashCode() {
		return ((this.p_values == null) ? 0 : this.p_values.hashCode());
	}
	
	 /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
	@Override
	public String toString() {
        return String.format("%s", HasValues.formatedValues(this.p_values));
	}
}
