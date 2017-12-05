package org.neurosystem.platform.trader;

import java.util.concurrent.ExecutorService;

import org.neurosystem.modules.marketdata.assets.IAsset;
import org.neurosystem.modules.marketdata.quote.IQuote;
import org.neurosystem.platform.trade.DemoTrade;
import org.neurosystem.platform.trade.ITrade;
import org.neurosystem.modules.riskmetric.pnl.IPnLKey;
import org.neurosystem.util.basic.HasSide.Side;
import org.neurosystem.util.common.annotations.javax.Nonnull;

public class DemoTrader extends AbstractTrader {
	
	public DemoTrader(@Nonnull IAsset asset, @Nonnull ExecutorService executor) {
		super(asset, executor);	
	}
	
	@Override
	protected ITrade openTrade(@Nonnull IAsset asset, @Nonnull Side side, @Nonnull IQuote quote, @Nonnull IPnLKey target) {
		return new DemoTrade(asset, side, quote, target);
	}
	
	@Override
	protected ITrade closeTrade(@Nonnull ITrade trade) {
		trade.closeTrade();
		return trade;
	}
}
