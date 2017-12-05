package org.neurosystem.modules.neuroscience.classification;

import org.neurosystem.modules.neuroscience.dna.INucleic;
import org.neurosystem.modules.neuroscience.mutation.IMorphism;
import org.neurosystem.modules.riskmetric.trade.ITradeKey;
import org.neurosystem.modules.riskmetric.win.IWinKey;
import org.neurosystem.util.common.annotations.javax.Nonnull;
import org.neurosystem.util.misc.Pair;

public interface IClassifier {
	
	public int size();
	
	public void addEntry(@Nonnull Pair<INucleic, ITradeKey> entry);
	
	public IHeuristic classification(IWinKey winKey);
	
	public IHeuristic training(@Nonnull IMorphism morphism, @Nonnull IWinKey winKey);
}
