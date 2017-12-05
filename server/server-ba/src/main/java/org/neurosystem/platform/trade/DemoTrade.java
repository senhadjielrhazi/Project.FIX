package org.neurosystem.platform.trade;

import org.neurosystem.modules.marketdata.assets.IAsset;
import org.neurosystem.modules.marketdata.quote.IQuote;
import org.neurosystem.modules.riskmetric.pnl.IPnLKey;
import org.neurosystem.util.common.annotations.javax.Nonnull;

public class DemoTrade extends AbstractTrade {

	public DemoTrade(@Nonnull IAsset asset, @Nonnull Side side, @Nonnull long time, @Nonnull double price, @Nonnull IPnLKey target) {
		super(asset, side, time, price, target);
	}	
	
	public DemoTrade(@Nonnull IAsset asset, @Nonnull Side side, @Nonnull IQuote quote, @Nonnull IPnLKey target) {
		this(asset, side, quote.getTime(), quote.getClose(), target);
	}
	
	@Override
	public void openTrade() {
	}

	@Override
	public void closeTrade() {
		super.closeTrade();
	}
}