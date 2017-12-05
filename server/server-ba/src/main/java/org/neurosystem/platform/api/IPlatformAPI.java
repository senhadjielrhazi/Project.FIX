package org.neurosystem.platform.api;

import java.util.Map;

import org.neurosystem.modules.marketdata.assets.IAsset;
import org.neurosystem.modules.marketdata.assets.basic.ISecurity;
import org.neurosystem.modules.marketdata.quote.IQuote;
import org.neurosystem.util.common.annotations.javax.Nonnull;

public interface IPlatformAPI {

	public void addTradingAsset(@Nonnull IAsset asset);
	
	public void onHistory(@Nonnull Map<ISecurity, IQuote> assetPrices);
	
	public void onBar(@Nonnull Map<ISecurity, IQuote> assetPrices);

	public void onStop();
}
