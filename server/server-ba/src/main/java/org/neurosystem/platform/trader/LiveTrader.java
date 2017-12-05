package org.neurosystem.platform.trader;

import java.util.concurrent.ExecutorService;

import org.neurosystem.modules.marketdata.assets.IAsset;
import org.neurosystem.modules.marketdata.quote.IQuote;
import org.neurosystem.platform.api.IBrokersAPI;
import org.neurosystem.platform.trade.ITrade;
import org.neurosystem.platform.trade.LiveTrade;
import org.neurosystem.modules.riskmetric.pnl.IPnLKey;
import org.neurosystem.util.basic.HasSide.Side;
import org.neurosystem.util.common.annotations.javax.Nonnull;

public class LiveTrader extends AbstractTrader {

	private final IBrokersAPI p_brokersAPI;
	
	public LiveTrader(@Nonnull IBrokersAPI brokersAPI, @Nonnull IAsset asset, @Nonnull ExecutorService executor) {
		super(asset, executor);	
		this.p_brokersAPI = brokersAPI;
	}
	
	@Override
	protected ITrade openTrade(@Nonnull IAsset asset, @Nonnull Side side, @Nonnull IQuote quote, @Nonnull IPnLKey target) {
		String comment = makeComment(asset, side, quote, target);
		return new LiveTrade(getBrokersAPI(), comment, asset, side, quote, target);
	}
	
	@Override
	protected ITrade closeTrade(@Nonnull ITrade trade) {
		trade.closeTrade();
		return trade;
	}
	
	protected IBrokersAPI getBrokersAPI() {
		return this.p_brokersAPI;
	}
	
	private String makeComment(@Nonnull IAsset asset, @Nonnull Side side, @Nonnull IQuote quote, @Nonnull IPnLKey target) {
		return String.format("Asset: %s, Quote: [ %s ], Side: %s, Target: [ %s ]", asset, quote, side, target);
	}
}
