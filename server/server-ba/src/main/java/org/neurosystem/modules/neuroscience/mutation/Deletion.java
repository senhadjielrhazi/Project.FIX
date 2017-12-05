package org.neurosystem.modules.neuroscience.mutation;

import java.util.Arrays;
import java.util.List;

import org.neurosystem.modules.neuroscience.dna.IGene;
import org.neurosystem.util.common.annotations.javax.Nonnull;
import org.neurosystem.util.common.annotations.javax.concurrent.Immutable;

@Immutable
public final class Deletion extends Mutation {
	
	public Deletion(@Nonnull List<Integer> indexes) {
		super("Deletion", indexes);
	}

	public Deletion(@Nonnull Integer... indexes) {
		super("Deletion", Arrays.asList(indexes));
	}
	
	@Override
	public void set(IGene[] genes) {
		for(Integer index:getIndexes()){
			genes[index] = IGene.F;
		}
	}
}
