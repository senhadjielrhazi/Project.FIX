package org.neurosystem.modules.neuroscience.classification;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.neurosystem.modules.neuroscience.dna.INucleic;
import org.neurosystem.modules.neuroscience.mutation.IMorphism;
import org.neurosystem.modules.riskmetric.metric.IMetricKey;

import org.neurosystem.util.basic.HasSide.Side;
import org.neurosystem.util.common.annotations.javax.Nonnull;
import org.neurosystem.util.common.annotations.javax.concurrent.Immutable;

@Immutable
public final class Heuristic implements IHeuristic {
	
	private final IMorphism p_morphism;
	private final Map<Integer, Map<INucleic, Side>> p_genome;	
	private final IMetricKey p_keyMetric;
	
	private final int p_hashUID;
	
	public Heuristic(@Nonnull IMorphism morphism, @Nonnull Map<Integer, Map<INucleic, Side>> keys, @Nonnull IMetricKey keyMetric){
		this.p_morphism = morphism;
		this.p_genome = keys;
		this.p_keyMetric = keyMetric;
		
		//Fast access UID
		this.p_hashUID = getHashUID();
	}

	private int getHashUID(){
		final int prime = 31;
		int hash = 1;

		hash = prime * hash + this.p_morphism.hashCode();
		for(Iterator<Entry<Integer, Map<INucleic, Side>>> it = this.p_genome.entrySet().iterator();it.hasNext();){ 
			Entry<Integer, Map<INucleic, Side>> entry = it.next();
			
			hash = prime * hash + entry.getKey().hashCode();
			hash = prime * hash + entry.getValue().hashCode();
		}
		hash = prime * hash + this.p_keyMetric.hashCode();
		
		return hash;
	}
	
	@Override
	public IMorphism getMorphism() {
		return this.p_morphism;
	}
	
	@Override
	public Map<Integer, Map<INucleic, Side>> getGenome() {
		return this.p_genome;
	}
	
	@Override
	public double getTNB() {
		return this.p_keyMetric.getTNB();
	}
	
	@Override
	public double getPWP() {
		return this.p_keyMetric.getPWP();
	}
	
	@Override
	public double getPNL() {
		return this.p_keyMetric.getPNL();
	}
	
	@Override
	public double getMAE() {
		return this.p_keyMetric.getMAE();
	}
	
	@Override
	public double getMFE() {
		return this.p_keyMetric.getMFE();
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
	    
	    Heuristic other = (Heuristic) obj;
	    if (!this.p_genome.equals(other.p_genome))
            return false;
	    if (!this.p_keyMetric.equals(other.p_keyMetric))
            return false;
        
		return true;
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
		return String.format("TPL: %s, KNB: %s, TNB: %s, PWP: %s, PNL: %s, MAE: %s, MFE: %s", 
				getTPL(), getKNB(), getTNB(), getPWP(), getPNL(), getMAE(), getMFE());
	}
}
