package org.neurosystem.modules.marketdata.assets.basic;

import org.neurosystem.modules.marketdata.assets.IAsset;

public interface ISecurity extends IAsset {
	
	public String getSymbol();
	
	public double getMinUnits();

	public double roundLot(double size);
}
