package org.neurosystem.modules.marketdata.assets;

import java.util.Map;
import java.util.Set;

import org.neurosystem.modules.marketdata.IQuoteServer;
import org.neurosystem.modules.marketdata.assets.basic.ISecurity;
import org.neurosystem.modules.marketdata.quote.IQuote;
import org.neurosystem.util.common.annotations.javax.Nonnull;

public interface IAsset extends IQuoteServer, IGreeks {
	
	public double getPipValue();
	
	public Set<ISecurity> getSecurities();
	
	public IQuote onValue(@Nonnull Map<ISecurity, IQuote> secPrices);
}
