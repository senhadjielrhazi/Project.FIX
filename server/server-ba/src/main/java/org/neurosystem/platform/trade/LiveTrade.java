package org.neurosystem.platform.trade;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.neurosystem.modules.marketdata.assets.IAsset;
import org.neurosystem.modules.marketdata.assets.basic.ISecurity;
import org.neurosystem.modules.marketdata.quote.IQuote;
import org.neurosystem.platform.api.IBrokersAPI;
import org.neurosystem.modules.riskmetric.pnl.IPnLKey;
import org.neurosystem.util.common.annotations.javax.Nonnull;

public class LiveTrade extends AbstractTrade {
	
	private final IBrokersAPI p_brokersAPI;
	private final List<String> p_labels;
	private final String p_comment;
	private final Map<ISecurity, Double> p_deltas;
	
	public LiveTrade(@Nonnull IBrokersAPI brokersAPI, @Nonnull String comment, 
			@Nonnull IAsset asset, @Nonnull Side side, @Nonnull long time, @Nonnull double price, @Nonnull IPnLKey target) {
		super(asset, side,  time, price, target);
		
		this.p_brokersAPI = brokersAPI;
		this.p_comment = comment;
		this.p_deltas = new HashMap<>();
		this.p_labels = new ArrayList<>();
		
		openTrade();
	}
	
	public LiveTrade(@Nonnull IBrokersAPI brokersAPI, @Nonnull String comment, 
			@Nonnull IAsset asset, @Nonnull Side side, @Nonnull IQuote quote, @Nonnull IPnLKey target) {
		this(brokersAPI, comment, asset, side, quote.getTime(), quote.getClose(), target);
	}
	
	@Override
	public void onQuote(IQuote quote) {
		super.onQuote(quote);
		
		if(isActive()){
			openTrade();
		}
	}
	
	@Override
	public boolean isActive() {
		if(!super.isActive())
			return false;
		
		boolean isOpen = false;
		for(String label:this.p_labels){
			if(this.p_brokersAPI.isOpen(label)){
				isOpen = true;
				break;
			}
		}
		
		return isOpen;
	}

	//Incremental trading execution here
	@Override
	public void openTrade() {
		double side = getSide().isLong()?1.:-1.;
		
		for (Iterator<Entry<ISecurity, Double>> it = getAsset().getDeltas().entrySet().iterator(); 
			     it.hasNext();){
		    Entry<ISecurity, Double> entry = it.next();
		    ISecurity security = entry.getKey();
		    double deltaA = side * entry.getValue();
		    
		    if(!this.p_deltas.containsKey(security)){
		    	this.p_deltas.put(security, 0.);
		    }
		    
		    double deltaT = this.p_deltas.get(security);
		    double lots = security.roundLot(Math.abs(deltaA-deltaT));		    
		    
		    if(lots > 0){
		    	if(deltaA > deltaT){
		    		this.p_labels.add(this.p_brokersAPI.submitOrder(OrderType.BUYMARKET, security, lots, 0, getTarget().getMAE(), getTarget().getMFE(), this.p_comment + ", deltaA: " + deltaA + ", deltaT: " + deltaT));
		    	}else{
		    		this.p_labels.add(this.p_brokersAPI.submitOrder(OrderType.SELLMARKET, security, lots, 0, getTarget().getMAE(), getTarget().getMFE(), this.p_comment + ", deltaA: " + deltaA + ", deltaT: " + deltaT));
		    	}
		    	this.p_deltas.put(security, deltaT + Math.signum(deltaA - deltaT) * lots);
		    }
		}
	}

	@Override
	public void closeTrade() {
		super.closeTrade();
		for(String label:this.p_labels){
			this.p_brokersAPI.closeOrder(label);
		}
	}
}
