package org.neurosystem.platform;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.neurosystem.modules.marketdata.assets.IAsset;
import org.neurosystem.modules.marketdata.assets.basic.ISecurity;
import org.neurosystem.modules.marketdata.quote.IQuote;
import org.neurosystem.platform.api.IBrokersAPI;
import org.neurosystem.platform.api.IPlatformAPI;
import org.neurosystem.platform.trader.ITrader;
import org.neurosystem.platform.trader.*;
import org.neurosystem.util.Parameters;
import org.neurosystem.util.common.annotations.javax.Nonnull;
import org.neurosystem.util.log.Priority;

public class PlatformAPI implements IPlatformAPI {

	private final IBrokersAPI p_brokersAPI;
	private final Object p_lock = new Object();
	private final Map<IAsset, ITrader> p_traders;
	private final ExecutorService p_executor;
	
	public PlatformAPI(@Nonnull IBrokersAPI brokersAPI) {
		this.p_brokersAPI = brokersAPI;
		this.p_traders = new HashMap<>();
		
		this.p_executor = Executors.newWorkStealingPool(Parameters.getMaxExecutors());
	}

	@Override
	public void addTradingAsset(@Nonnull IAsset asset) {
		synchronized (lock()) {
			if(!getTraders().containsKey(asset)){
				ITrader trader = new LiveTrader /*LiveTrader/LogTrader/GATrader*/(getBrokersAPI(), asset, executor());
				getTraders().put(asset, trader);
				
				getBrokersAPI().log("Added trading asset: " + asset, Priority.INFO);
			}
		}
	}

	@Override
	public void onHistory(@Nonnull Map<ISecurity, IQuote> assetPrices) {
		synchronized (lock()) {
			for(ITrader trader:getTraders().values()){
				trader.onHistory(assetPrices);
			}
		}
	}

	@Override
	public void onBar(@Nonnull Map<ISecurity, IQuote> assetPrices) {
		synchronized (lock()) {
			for(ITrader trader:getTraders().values()){				
				trader.onBar(assetPrices);
			}
		}
	}

	@Override
	public void onStop() {
		synchronized (lock()) {
			for(ITrader trader:getTraders().values()){				
				trader.onStop();
			}
		}
	}
	
	private Object lock() {
		return this.p_lock;
	}
	
	private IBrokersAPI getBrokersAPI() {
		return this.p_brokersAPI;
	}
	
	private Map<IAsset, ITrader> getTraders() {
		return this.p_traders;
	}
	
	private ExecutorService executor(){
		return this.p_executor;
	}
}
