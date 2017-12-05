package org.neurosystem.platform.trade;

import org.neurosystem.modules.marketdata.quote.IQuote;
import org.neurosystem.modules.riskmetric.trade.ITradeKey;
import org.neurosystem.util.common.annotations.javax.Nonnull;

public interface ITrade extends ITradeKey {
	
	public void onQuote(@Nonnull IQuote quote);
	
	public void openTrade();
	
	public void closeTrade();
	
	public boolean isActive();
}
