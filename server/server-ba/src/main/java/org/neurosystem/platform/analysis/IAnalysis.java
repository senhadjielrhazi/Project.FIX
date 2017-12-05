package org.neurosystem.platform.analysis;

import java.util.List;
import java.util.Map;

import org.neurosystem.modules.marketdata.assets.basic.ISecurity;
import org.neurosystem.modules.marketdata.quote.IQuote;
import org.neurosystem.modules.neuroscience.dna.INucleic;
import org.neurosystem.platform.analysis.trend.ITrendKey;
import org.neurosystem.modules.riskmetric.trade.ITradeKey;
import org.neurosystem.util.common.annotations.javax.Nonnull;
import org.neurosystem.util.misc.Pair;

public interface IAnalysis {
	
	public void onHistory(@Nonnull Map<ISecurity, IQuote> assetPrices);
	
	public ITrendKey getTrendKey();
	
	public List<Pair<INucleic, ITradeKey>> getClosedTrades();
}
