package org.neurosystem.modules.neuroscience.mutation;

import java.util.Arrays;

import org.neurosystem.modules.neuroscience.dna.IGene;
import org.neurosystem.util.common.annotations.javax.concurrent.Immutable;

@Immutable
public final class Substitution extends Mutation {

	private final Integer p_uno, p_dos;
	
	public Substitution(Integer uno, Integer dos) {
		super("Substitution", Arrays.asList(uno, dos));
		
		this.p_uno = uno;
		this.p_dos = dos;
	}

	@Override
	public void set(IGene[] genes) {
		genes[this.p_uno] = IGene.fromValue(Math.signum(genes[this.p_uno].value() + genes[this.p_dos].value()));
	}
}