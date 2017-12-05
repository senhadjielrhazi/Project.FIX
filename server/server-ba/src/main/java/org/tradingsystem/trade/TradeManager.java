package org.tradingsystem.trade;

import java.util.*;

import org.tradingsystem.IStrategyTools;
import org.tradingsystem.analysis.AnalysisManager;
import org.tradingsystem.analysis.AnalysisPath;

import com.dukascopy.api.*;

public class TradeManager{
	// Instrument parameters
	private Instrument p_instrument;
	private IStrategyTools p_tools;
	private double p_size;
	private Object lock = new Object();
	
	// Trades
	private List<Trade> p_activTrades = new ArrayList<Trade>();
	
	public TradeManager(IStrategyTools tools, Instrument instrument, double size) throws JFException {
		p_instrument = instrument;
		p_tools = tools;
		p_size = size;
	}

	public void refreshMData(Period period, long barTime, 
			List<List<Double>> allAIDataVector, AnalysisManager p_aiAnalysis) throws JFException {
		synchronized (lock) {
			boolean longEntry = false, shortEntry = false;
			boolean longExit = false, shortExit = false;
			
			int longEntryNB = 0, shortEntryNB = 0;
			int longExitNB = 0, shortExitNB = 0;
			
			for(AnalysisPath path:p_aiAnalysis.getStopedAnalysis()){
				if(path.getLossOPState().equalDataVector(allAIDataVector)
						&& path.getLossOPState().getTime() < path.getProfitOPState().getTime()){
					if(path.isLong()){
						longEntry = true;
						longEntryNB++;
					}else{
						shortEntry = true;
						shortEntryNB++;
					}
				}
				if(path.getProfitOPState().equalDataVector(allAIDataVector)){
					if(path.isLong()){
						longExit = true;
						longExitNB++;
					}else{
						shortExit = true;
						shortExitNB++;
					}
				}
			}
				
			String comment = "  " + getMapFormated(allAIDataVector)
			+ " " + longEntryNB + "  " + shortEntryNB 
			+ " " + longExitNB + "  " + shortExitNB;
			
			//Cleanup the trade list
			for(Iterator<Trade> itr = p_activTrades.iterator();itr.hasNext();){  
				Trade element = itr.next(); 
                if(element.isActive()){
                   element.refreshMData(period, barTime); 
                }else{
                    itr.remove(); 
                }
            }
			
			//Exit trades 
			for(Iterator<Trade> itr = p_activTrades.iterator();itr.hasNext();){  
				Trade element = itr.next(); 
				if(longExit && !shortExit && element.isLong()){
					element.closeTrade(); 
                	itr.remove(); 
				}else if(shortExit && !longExit && !element.isLong()){
					element.closeTrade();  
					itr.remove(); 
				}
            }
			
			if(p_activTrades.size() == 0){
				//Enter trade
				if(longEntry && !shortEntry){
					Trade trade = new Trade(p_tools, p_instrument, period, barTime, true, p_size, comment);
					p_activTrades.add(trade);
				}else if(shortEntry && !longEntry){
					Trade trade = new Trade(p_tools, p_instrument, period, barTime, false, p_size, comment);
					p_activTrades.add(trade);
				}
			}
		}	
	}
	
	public String getListFormated(List<Double> arr){
		String str = "[ ";
        for (int r = 0; r < arr.size(); r++) {
            str += arr.get(r) + "; ";
        }
        return str+"]";
	}
	
	public String getMapFormated(List<List<Double>> arr){
		String str = "[ ";
        for (int r = 0; r < arr.size(); r++) {
            str += getListFormated(arr.get(r)) + "; ";
        }
        return str+"]";
	}
}
