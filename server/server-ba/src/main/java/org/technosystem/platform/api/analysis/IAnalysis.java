package org.technosystem.platform.api.analysis;

import org.technosystem.modules.marketdata.assets.HasTickers;
import org.technosystem.modules.marketdata.assets.ITicker;
import org.technosystem.modules.marketdata.server.IDataServer;

public interface IAnalysis extends HasTickers {
	
	public IPrediction getPrediction(ITicker ticker);
	
	public IDataServer[] getDataServer(ITicker ticker);
}