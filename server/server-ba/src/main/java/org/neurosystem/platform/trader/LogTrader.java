package org.neurosystem.platform.trader;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import org.neurosystem.modules.marketdata.assets.IAsset;
import org.neurosystem.modules.marketdata.assets.basic.ISecurity;
import org.neurosystem.modules.marketdata.quote.IQuote;
import org.neurosystem.modules.neuroscience.dna.INucleic;
import org.neurosystem.platform.api.IBrokersAPI;
import org.neurosystem.modules.riskmetric.trade.ITradeKey;
import org.neurosystem.util.common.annotations.javax.Nonnull;
import org.neurosystem.util.misc.Pair;

public class LogTrader extends LiveTrader {
	
	public LogTrader(@Nonnull IBrokersAPI brokersAPI, @Nonnull IAsset asset, @Nonnull ExecutorService executor) {
		super(brokersAPI, asset, executor);	
	}
	
	@Override
	public void onBar(@Nonnull Map<ISecurity, IQuote> assetPrices) {		
		for(Iterator<Pair<INucleic, ITradeKey>> it = getAnalysis().getClosedTrades().iterator();it.hasNext();){ 
			Pair<INucleic, ITradeKey> entry = it.next();
			INucleic dna = entry.getKey();
			ITradeKey trade = entry.getValue();
			
			getBrokersAPI().trace(trade.toString() + ";" + dna.toString());
		}
		
		getBrokersAPI().stopBrokers();
	}
}
