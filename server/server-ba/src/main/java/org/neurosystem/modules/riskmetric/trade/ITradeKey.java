package org.neurosystem.modules.riskmetric.trade;

import org.neurosystem.modules.riskmetric.pnl.IPnLKey;
import org.neurosystem.util.basic.HasSide;
import org.neurosystem.util.basic.HasTime;

public interface ITradeKey extends IPnLKey, HasSide, HasTime {
	
}
