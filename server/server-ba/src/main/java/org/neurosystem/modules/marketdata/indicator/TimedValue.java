package org.neurosystem.modules.marketdata.indicator;

import org.neurosystem.util.common.annotations.javax.Nonnull;
import org.neurosystem.util.common.base.Objects;

public final class TimedValue<V> implements ITimedValue<V> {

	private final long p_time;
    private final V p_value;
    
    public TimedValue(@Nonnull long time, @Nonnull V value) {
    	this.p_time = time;
    	this.p_value = value;
    }
    
    @Override
    public long getTime() {
        return this.p_time;
    }
    
    @Override
    public V getValue() {
        return this.p_value;
    }
      
    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
	@Override
	public boolean equals(Object obj){
		if (obj instanceof TimedValue) {
			TimedValue<?> that = (TimedValue<?>) obj;
			return (this.p_time == that.p_time) 
					&& Objects.equal(this.p_value, that.p_value);
		}
        
		return false;
	}
	
	/* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
	@Override
	public int hashCode() {
		 return (Long.hashCode(this.p_time)) ^ 
				 ((this.p_value == null) ? 0 : this.p_value.hashCode());
	}
	
	 /* (non-Javadoc)
    * @see java.lang.Object#toString()
    */
	@Override
	public String toString() {
		return String.format("Time: %s, Value: %s", formatedTime(), this.p_value);
	}
}
