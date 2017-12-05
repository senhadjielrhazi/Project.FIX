package org.neurosystem.modules.neuroscience.mutation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.neurosystem.modules.neuroscience.dna.IGene;
import org.neurosystem.modules.neuroscience.dna.INucleic;
import org.neurosystem.modules.neuroscience.dna.Nucleic;
import org.neurosystem.util.basic.HasValues;
import org.neurosystem.util.common.annotations.javax.Nonnull;
import org.neurosystem.util.common.base.Objects;

public final class Morphism implements IMorphism {

	private final List<IMutation> p_mutations;
	
	private int p_size;
	
	public Morphism(){
		this.p_mutations = new ArrayList<>();
		
		//Fast access UID
		this.p_size = 0;
	}
	
	@Override
	public List<IMutation> getMutations() {
		return this.p_mutations;
	}

	@Override
	public void addMutation(@Nonnull IMutation mutation) {
		this.p_mutations.add(mutation);
		this.p_size++;
	}

	@Override
	public void reversion() {
		this.p_mutations.remove(this.p_size-1);
		this.p_size--;
	}	

	@Override
	public INucleic morph(@Nonnull INucleic dna) {
		if(this.p_size == 0){
			return dna;
		}
		
		if((this.p_size == 1) && (this.p_mutations.get(0) instanceof Deletion)) {
			List<Integer> deleted = this.p_mutations.get(0).getIndexes();
			
			IGene[] genes = new IGene[dna.size() - deleted.size()];
			int counter = 0;
			for(int index = 0; index < dna.size(); index++){
				if(!deleted.contains(index)){
					genes[counter] = dna.getGenes()[index];
					counter++;
				}
			}
			return new Nucleic(genes);
		}
		
		IGene[] genes = Arrays.copyOf(dna.getGenes(), dna.size());		
		for (int i = 0; i < this.p_size; i++) {
			this.p_mutations.get(i).set(genes);
		}
		
		return new Nucleic(genes);

	}
	
	/* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
	@Override
	public boolean equals(Object obj){
		if (obj instanceof Morphism) {
			Morphism that = (Morphism) obj;
			return (this.p_size == that.p_size) 
					&& Objects.equal(this.p_mutations, that.p_mutations);
		}
        
		return false;
	}
	
	/* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
	@Override
	public int hashCode() {
		return (Integer.hashCode(this.p_size)) ^ 
				 ((this.p_mutations == null) ? 0 : this.p_mutations.hashCode());
	}
	
	 /* (non-Javadoc)
    * @see java.lang.Object#toString()
    */
	@Override
	public String toString() {
       return String.format("%s", HasValues.formatedValues(this.p_mutations));
	}
}
