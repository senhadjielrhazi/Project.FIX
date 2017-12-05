package org.neurosystem.modules.neuroscience.classification;

import java.util.Map;

import org.neurosystem.modules.neuroscience.dna.INucleic;
import org.neurosystem.modules.neuroscience.mutation.IMorphism;
import org.neurosystem.modules.riskmetric.metric.IMetricKey;
import org.neurosystem.util.basic.HasSide.Side;

public interface IHeuristic extends IMetricKey {
	
	public IMorphism getMorphism();
	
	public Map<Integer, Map<INucleic, Side>> getGenome();
	
	public default double getKNB() {
		return getGenome().size();
	}
}