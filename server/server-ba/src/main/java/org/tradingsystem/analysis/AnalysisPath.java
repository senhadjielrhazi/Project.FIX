package org.tradingsystem.analysis;

import java.util.*;

import org.tradingsystem.IStrategyTools;

import com.dukascopy.api.*;

public class AnalysisPath{
	// Instrument parameters
	private Instrument p_instrument;
	private double p_pip;
	private IStrategyTools p_tools;
	private IHistory p_history;
	private OfferSide p_side;
	private Object lock = new Object();
	
	private IAnalysisState p_entry;
	private IAnalysisState p_profitOP;
	private IAnalysisState p_lossOP;
	private IAnalysisState p_exit;
	
	private boolean p_longShort;
	private boolean p_active = true;
	private double p_trailStop = 0.005;
	
	public AnalysisPath(IStrategyTools tools, Instrument instrument, 
			Period period, long inBarTime,
			List<List<Double>> aiDataVectors, boolean longShort) throws JFException{
		p_instrument = instrument;
		p_pip = p_instrument.getPipValue();
		p_tools = tools;
		
		// Initialisation
		p_history = p_tools.getHistory();
		p_side = p_tools.getSide();
		p_longShort = longShort;
		
		double price = p_history.getBars(p_instrument, period, p_side, Filter.WEEKENDS, 1, inBarTime, 0).get(0).getClose();
		long barTime = p_history.getNextBarStart(period, inBarTime);
		
		p_entry = new AnalysisState(barTime, price, aiDataVectors);
		p_profitOP = new AnalysisState(barTime, price, aiDataVectors);
		p_lossOP = new AnalysisState(barTime, price, aiDataVectors);
		p_exit = new AnalysisState(barTime, price, aiDataVectors);
	}

	public void refreshMData(Period period, long barTime, List<List<Double>> aiDataVectors) throws JFException {
		synchronized (lock) {
			double price = p_history.getBars(p_instrument, period, p_side, Filter.WEEKENDS, 1, barTime, 0).get(0).getClose();
			if(isLong()){
				double pnl = (price-getEntryState().getPrice())/p_pip;
				if(price < getProfitOPState().getPrice()*(1-p_trailStop)){
					getExitState().updateParameters(barTime, price, pnl, aiDataVectors);
					p_active = false;
					return;
				}
				if(price > getProfitOPState().getPrice()){
					getProfitOPState().updateParameters(barTime, price, pnl, aiDataVectors);
				}
				if(price < getLossOPState().getPrice()){
					getLossOPState().updateParameters(barTime, price, pnl, aiDataVectors);
				}
			}else{
				double pnl = (getEntryState().getPrice()-price)/p_pip;
				if(price > getProfitOPState().getPrice()*(1+p_trailStop)){
					getExitState().updateParameters(barTime, price, pnl, aiDataVectors);
					p_active = false;
					return;
				}
				if(price < getProfitOPState().getPrice()){
					getProfitOPState().updateParameters(barTime, price, pnl, aiDataVectors);
				}
				if(price > getLossOPState().getPrice()){
					getLossOPState().updateParameters(barTime, price, pnl, aiDataVectors);
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

	public IAnalysisState getEntryState() {
		return p_entry;
	}
	
	public IAnalysisState getProfitOPState() {
		return p_profitOP;
	}
	
	public IAnalysisState getLossOPState() {
		return p_lossOP;
	}

	public IAnalysisState getExitState() {
		return p_exit;
	}

	public String to_String() {
		String data = p_longShort+ " ENTRY: " + getEntryState().to_String() + " EXIT: " + getExitState().to_String()
				+ " PROFIT: " + getProfitOPState().to_String() + " LOSS: " + getLossOPState().to_String();
		return data;
	}
}
