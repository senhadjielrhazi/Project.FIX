package org.tradingsystem.trade;

import org.tradingsystem.IStrategyTools;

import com.dukascopy.api.*;
import com.dukascopy.api.IEngine.OrderCommand;

public class Trade{
	// Instrument parameters
	private Instrument p_instrument;
	private IStrategyTools p_tools;
	private IHistory p_history;
	private OfferSide p_side;
	private Object lock = new Object();
	
	private boolean p_longShort;
	private boolean p_active = true;
	private double p_trailStop = 0.005;
	private double p_price;
	private String p_label;
	
	public Trade(IStrategyTools tools, Instrument instrument, 
			Period period, long inBarTime, boolean longShort, double size, String comment) throws JFException{
		p_instrument = instrument;
		p_tools = tools;
		
		// Initialisation
		p_history = p_tools.getHistory();
		p_side = p_tools.getSide();
		p_longShort = longShort;
		
		p_price = p_history.getBars(p_instrument, period, p_side, Filter.WEEKENDS, 1, inBarTime, 0).get(0).getClose();
		
		if(longShort){
			p_label = p_tools.submitOrder(OrderCommand.BUY, p_instrument, size, 0, 0, 0, comment);  
		}else{
			p_label = p_tools.submitOrder(OrderCommand.SELL, p_instrument, size, 0, 0, 0, comment);  
		}
	}

	public void refreshMData(Period period, long barTime) throws JFException {
		synchronized (lock) {
			double price = p_history.getBars(p_instrument, period, p_side, Filter.WEEKENDS, 1, barTime, 0).get(0).getClose();
			if(isLong()){
				if(price < p_price*(1-p_trailStop)){
					closeTrade(); 
					return;
				}
			}else{
				if(price > p_price*(1+p_trailStop)){
					closeTrade(); 
					return;
				}
			}
		}
	}

	public boolean isLong() {
		return p_longShort;
	}
	
	public boolean isActive() {
		return p_active;
	}
	
	public void closeTrade()throws JFException{
		p_active = false;
		p_tools.closeOrder(p_label); 
    }

}
