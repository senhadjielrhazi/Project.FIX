package org.technosystem.modules.marketdata;

import org.neurosystem.util.common.annotations.javax.Nonnull;
import org.neurosystem.util.common.annotations.javax.concurrent.Immutable;

@Immutable
public final class TimedValue<V> implements ITimedValue<V> {

	private final long p_time;
    private final V p_value;
    
	private int p_hashUID = 0;
	
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
    
    private int getHashUID(){
        final int prime = 31;
        int hash = 1;
        
        hash = prime * hash + this.p_value.hashCode();
        hash = prime * hash + Long.hashCode(this.p_time);
        
	    return hash;
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
	    
	    TimedValue<?> other = (TimedValue<?>) obj;
	    if (!this.p_value.equals(other.p_value))
            return false;
        if (this.p_time != other.p_time)
            return false;
        
		return true;
	}
	
	/* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
	@Override
	public int hashCode() {
		if(this.p_hashUID == 0){    	
			this.p_hashUID = getHashUID();
		}
	    return this.p_hashUID;
	}
	
	 /* (non-Javadoc)
    * @see java.lang.Object#toString()
    */
	@Override
	public String toString() {
		return String.format("Time: %s, Value: %s", formatedTime(), this.p_value);
	}
}
