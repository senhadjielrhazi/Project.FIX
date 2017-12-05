package org.neurosystem.modules.neuroscience.mutation;

import java.util.Arrays;

import org.neurosystem.modules.neuroscience.dna.IGene;
import org.neurosystem.util.common.annotations.javax.concurrent.Immutable;

@Immutable
public final class Antimorphic extends Mutation {

	private final Integer p_uno, p_dos;
	
	public Antimorphic(Integer uno, Integer dos) {
		super("Antimorphic", Arrays.asList(uno, dos));
		
		this.p_uno = uno;
		this.p_dos = dos;
	}

	@Override
	public void set(IGene[] genes) {
		if(genes[this.p_uno].value() * genes[this.p_dos].value() > 0.){
			genes[this.p_uno] = IGene.F;
		}
	}
}