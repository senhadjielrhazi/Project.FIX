package org.neurosystem.modules.neuroscience.mutation;

import java.util.List;

import org.neurosystem.modules.neuroscience.dna.INucleic;
import org.neurosystem.util.common.annotations.javax.Nonnull;

public interface IMorphism {

	public void addMutation(@Nonnull IMutation mutation);
	
	public void reversion();

	public List<IMutation> getMutations();
	
	public INucleic morph(@Nonnull INucleic dna);
}
