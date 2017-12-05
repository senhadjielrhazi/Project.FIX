package org.neurosystem.modules.marketdata.assets;

import java.util.Map;

import org.neurosystem.modules.marketdata.assets.basic.ISecurity;

public interface IGreeks {

	public Map<ISecurity, Double> getDeltas();
}
