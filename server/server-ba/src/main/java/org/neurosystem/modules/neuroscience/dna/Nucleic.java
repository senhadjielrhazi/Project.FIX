package org.neurosystem.modules.neuroscience.dna;

import java.util.Arrays;

import org.neurosystem.util.common.annotations.javax.Nonnull;
import org.neurosystem.util.common.annotations.javax.concurrent.Immutable;

@Immutable
public final class Nucleic implements INucleic {

	private final int length;
	private final IGene[] p_genes;
	
	private final int p_hashUID;
	
	public Nucleic(@Nonnull IGene[] genes) {
		this.p_genes = genes;
		
		//Fast access UID
		this.length = genes.length;
		this.p_hashUID = getHashUID();
	}
	
	private int getHashUID(){
        final int prime = 31;
        int hash = 1;
		
		for (int i = 0; i < this.length; i++) {
			hash = prime * hash + this.p_genes[i].code();
		}
		
		return hash;
    }
	
	@Override
	public IGene[] getGenes() {
		return this.p_genes;
	}
	
	@Override
	public int size() {
		return this.length;
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
	    
	    Nucleic other = (Nucleic) obj;
	    if(this.length != other.length)
    		return false;
	    
	    return Arrays.equals(this.p_genes, other.p_genes);
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
		StringBuilder str = new StringBuilder(size());
		for(IGene gene:this.p_genes){
			str.append(gene.code());
		}
		
		return str.toString();
	}
		
	public static INucleic valueOf(String regex){
		IGene[] genes = new IGene[regex.length()];
		
		for(int index = 0; index < regex.length(); index++){
			genes[index] = IGene.fromCode(regex.charAt(index));
		}
		
		return new Nucleic(genes);
	}
	
	public static INucleic flipsyde(INucleic dna){
		IGene[] genes = new IGene[dna.size()];
		
		for(int index = 0; index < dna.size(); index++){
			genes[index] = IGene.fromValue(-dna.getGenes()[index].value());
		}
		
		return new Nucleic(genes);
	}
}
