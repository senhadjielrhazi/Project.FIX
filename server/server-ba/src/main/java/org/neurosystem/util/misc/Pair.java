package org.neurosystem.util.misc;

import org.neurosystem.util.common.annotations.javax.Nonnull;
import org.neurosystem.util.common.base.Objects;

/**
 * Strongly-typed object Pair.
 * 
 * @version $Id: Pair.java 16482 2013-02-05 19:56:42Z$
 * @since 0.1.0
 */

public final class Pair<T1, T2> {
    
	private final T1 p_key;
    private final T2 p_value;

	/**
	 * Create a new Pair instance.
	 *
	 * @param key a <code>T1</code> value or null
	 * @param value a <code>T2</code> value or null
	 */
	public Pair(@Nonnull T1 key, @Nonnull T2 value) {
		this.p_key = key;
		this.p_value = value;
	}
	
	public static <T> Pair<T, T> asPair(T k, T v){
		return new Pair<>(k, v);
	}
	
    /**
     * Get the key member value.
     *
     * @return a <code>T1</code> value
     */
    public T1 getKey() {
        return this.p_key;
    }
    
    /**
     * Get the value member value.
     *
     * @return a <code>T2</code> value
     */
    public T2 getValue() {
        return this.p_value;
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Pair) {
			Pair<?, ?> that = (Pair<?, ?>) obj;
			return Objects.equal(this.p_key, that.p_key) 
					&& Objects.equal(this.p_value, that.p_value);
		}
		
		return false;
	}
	
	/* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
	@Override
	public int hashCode() {
	    return ((this.p_key == null) ? 0 : this.p_key.hashCode()) ^ 
	    		((this.p_value == null) ? 0 : this.p_value.hashCode());
	}
	
	/* (non-Javadoc)
    * @see java.lang.Object#toString()
    */
	@Override
	public String toString() {
       return String.format("Key: %s, Value: %s", this.p_key, this.p_value);
	}
}
