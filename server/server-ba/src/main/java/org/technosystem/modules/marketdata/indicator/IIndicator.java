package org.technosystem.modules.marketdata.indicator;

import org.technosystem.modules.marketdata.ITimedValue;
import org.technosystem.modules.marketdata.server.IDataServer;
import org.neurosystem.util.basic.HasValues;

public interface IIndicator<V> extends HasValues<ITimedValue<V>> {

	public V calculate(IDataServer ds);
}
