package org.technosystem.modules.marketdata.indicator;

import java.util.*;

import org.technosystem.modules.marketdata.AppliedPrice;
import org.technosystem.modules.marketdata.ITimedValue;
import org.technosystem.modules.marketdata.TimedValue;
import org.neurosystem.util.basic.HasValues;


/**
 * Base class for all classes implementing technical indicators.
 */
public abstract class Indicator<V> extends ArrayList<ITimedValue<V>> implements IIndicator<V> {
	
	/**
	 * The abstract indicator series of values
	 */
	protected static final long serialVersionUID = 1L;
	
	private final AppliedPrice p_appliedPrice;
	
	public Indicator() {
		this(AppliedPrice.CLOSE);
    }
	
	public Indicator(AppliedPrice appliedPrice) {
		super();
		this.p_appliedPrice = appliedPrice;
    }
	
	protected void addTimedValue(Long time, V value) {
		add(new TimedValue<V>(time, value));
    }

	protected AppliedPrice getAppliedPrice(){
		return this.p_appliedPrice;
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
	    
	    return super.equals(obj);
	}
	
	/* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
	@Override
	public int hashCode() {
        final int prime = 31;

	    return prime  + super.hashCode();
	}
	
	 /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
	@Override
	public String toString() {
        return String.format("%s", HasValues.formatedValues(this));
	}
}
