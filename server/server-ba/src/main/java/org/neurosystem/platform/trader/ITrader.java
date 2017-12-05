package org.neurosystem.platform.trader;

import java.util.Map;

import org.neurosystem.modules.marketdata.assets.basic.ISecurity;
import org.neurosystem.modules.marketdata.quote.IQuote;
import org.neurosystem.util.common.annotations.javax.Nonnull;

public interface ITrader {

	public void onHistory(@Nonnull Map<ISecurity, IQuote> assetPrices);

	public void onBar(@Nonnull Map<ISecurity, IQuote> assetPrices);

	public void onStop();
}
