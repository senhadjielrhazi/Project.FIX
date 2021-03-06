package org.neurosystem.platform.api;

import org.neurosystem.modules.marketdata.assets.basic.ISecurity;
import org.neurosystem.platform.trade.OrderType;
import org.neurosystem.util.common.annotations.javax.Nonnull;
import org.neurosystem.util.log.ILogger;

public interface IBrokersAPI extends ILogger {
	
	public void closeOrder(@Nonnull String label);
	
	public String submitOrder(@Nonnull OrderType ordertype, @Nonnull ISecurity security,
			@Nonnull double orderAmount, double price, @Nonnull String comment);

	public String submitOrder(OrderType ordertype, ISecurity security,
			double orderAmount, double price, double stp, double tkp, String comment);
	
	public void stopBrokers();

	public boolean isOpen(String label);	
}
