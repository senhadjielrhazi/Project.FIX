package org.neurosystem.modules.neuroscience.mutation;

import java.util.List;

import org.neurosystem.util.common.annotations.javax.Nonnull;
import org.neurosystem.util.common.annotations.javax.concurrent.Immutable;
import org.neurosystem.util.common.base.Objects;

@Immutable
public abstract class Mutation implements IMutation {

	private final String p_name;
	private final List<Integer> p_indexes;
	
	public Mutation(@Nonnull String name, @Nonnull List<Integer> indexes) {
		this.p_name = name;
		this.p_indexes = indexes;
	}
	
	@Override
	public String getName() {
		return this.p_name;
	}
	
	@Override
	public List<Integer> getIndexes() {
		return this.p_indexes;
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Mutation) {
			Mutation that = (Mutation) obj;
			return (this.p_name.equals(that.p_name)) 
					&& Objects.equal(this.p_indexes, that.p_indexes);
		}
        
		return false;
	}
	
	/* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
	@Override
	public int hashCode() {
		return (this.p_name.hashCode()) ^ 
				 ((this.p_indexes == null) ? 0 : this.p_indexes.hashCode());
	}
	
	 /* (non-Javadoc)
    * @see java.lang.Object#toString()
    */
	@Override
	public String toString() {
		return String.format("%s: %s", this.p_name, this.p_indexes);
	}
}
