package org.neurosystem.modules.neuroscience.mutation;

import java.util.List;

import org.neurosystem.modules.neuroscience.dna.IGene;
import org.neurosystem.util.common.annotations.javax.Nonnull;

public interface IMutation {
	
	public String getName();
	
	public List<Integer> getIndexes();
	
	public void set(@Nonnull IGene[] genes) ;
}
