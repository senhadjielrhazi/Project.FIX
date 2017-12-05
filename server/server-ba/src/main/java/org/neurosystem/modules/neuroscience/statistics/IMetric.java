package org.neurosystem.modules.neuroscience.statistics;

import org.neurosystem.modules.riskmetric.metric.IMetricKey;
import org.neurosystem.modules.riskmetric.trade.ITradeKey;
import org.neurosystem.modules.riskmetric.win.IWinKey;
import org.neurosystem.util.common.annotations.javax.Nonnull;

public interface IMetric extends IMetricKey, IWinKey {

	public void addEntry(@Nonnull ITradeKey trade);

	public void addMetric(@Nonnull IMetricKey metric);
}
