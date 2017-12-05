package org.technosystem.modules.marketdata.assets;

import org.technosystem.modules.marketdata.quote.IQuote;
import org.neurosystem.util.common.annotations.javax.Nonnull;
import org.neurosystem.util.time.IPeriod;

public interface HasTickers {
	
	public void onValue(@Nonnull IPeriod period, @Nonnull ITicker tikcer, @Nonnull IQuote quote);
}
