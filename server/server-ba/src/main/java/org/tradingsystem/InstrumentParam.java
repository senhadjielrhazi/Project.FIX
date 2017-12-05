package org.tradingsystem;

import java.util.*;

import org.tradingsystem.analysis.AnalysisManager;
import org.tradingsystem.states.*;
import org.tradingsystem.trade.TradeManager;

import com.dukascopy.api.*;

public class InstrumentParam implements IRefreshData{
	// Instrument parameters
	private Instrument p_instrument;
	private double p_size;
	private IStrategyTools p_tools;
	private IHistory p_history;
	private OfferSide p_side;
	private List<Period> p_refperiods = new ArrayList<Period>();
	
	// AI trades
	private TradeManager p_aiTrades = null;
	
	//AI analysis
    private AnalysisManager p_aiAnalysis = null;
		
	//AI states
	private IState p_aiStates = null;
	
	
	//Past simulated
	private int p_perTotal = 3*20;//3Mo
	private boolean p_tradeLive = false;
	
	InstrumentParam(IStrategyTools tools, Instrument instrument, double size)
			throws JFException {
		p_instrument = instrument;
		p_size = size;
		p_tools = tools;
		
		// Initialisation
		p_history = tools.getHistory();
		p_side = tools.getSide();
				
		tools.printOut("Start Loading: " + p_instrument);
		//Create analysis
		p_aiAnalysis = new AnalysisManager(p_tools, p_instrument);
		
		//Create states
		p_aiStates = new T1State(p_tools, p_instrument);
		
		p_aiTrades = new TradeManager(p_tools, p_instrument, p_size);
		
		//Refresh periods
		p_refperiods.addAll(p_aiStates.getRefPeriods());
		
		//Simulate past
		generateSimulPast();
		
		//Start Trading
		p_tradeLive = true;
		tools.printOut("Done Loading: " + p_instrument);
		
		tools.getInstrumentParamList().put(instrument, this);
	}

	public Instrument getInstrument() {
		return p_instrument;
	}

	public double getSize() {
		return p_size;
	}
	
	@Override
	public void refreshMData(Period period, long barTime) throws JFException {
		if(p_aiStates.getRefPeriods().contains(period)){
			//Create states
			p_aiStates.refreshMData(period, barTime);
		}
		if(p_aiStates.getStatePeriod().equals(period)){
			//Manage analysis
			p_aiAnalysis.refreshMData(period, barTime, p_aiStates.getAllAIDataVector());
			
			//Manage trades
			if(p_tradeLive){
				p_aiTrades.refreshMData(period, barTime, 
						p_aiStates.getAllAIDataVector(), p_aiAnalysis);
			}
		}
	}
	
	@Override
	public List<Period> getRefPeriods() {
		return p_refperiods;
	}
	
	private void generateSimulPast() throws JFException {
		int index1 = getRefPeriods().size()-1;
		
		long endTime = p_history.getPreviousBarStart(Period.DAILY, p_history.getPreviousBarStart(Period.DAILY, p_history.getStartTimeOfCurrentBar(p_instrument, Period.DAILY)));
		//p_history.getStartTimeOfCurrentBar(p_instrument, getRefPeriods().get(index0));
		long startTime = p_history.getTimeForNBarsBack(Period.DAILY, endTime, p_perTotal);
		
		p_tools.printOut("generateSimulPast: "  + p_tools.getTimeFormated(startTime)+ " to "  + p_tools.getTimeFormated(endTime));
		generateSimulPastSub(index1, startTime, endTime);
		
		/*for(AnalysisPath data:p_aiAnalysis.getStpedAnalysis()){
			p_tools.printOut("AnalysisPath: "  + data.to_String());
		}*/
	}
	
	private void generateSimulPastSub(int index, long inStartTime, long inEndTime) throws JFException{
		if(index<0){return;}
		Period period = getRefPeriods().get(index);
		
		//Complete period
		long startTime = inStartTime;
		long endTime = getEndTimeSpecPeriod(period, Math.min(inEndTime,
        		p_history.getStartTimeOfCurrentBar(p_instrument, period)));
		List<IBar> bars = p_history.getBars(p_instrument, period, p_side, Filter.WEEKENDS, startTime, endTime); 
        for(int j = 0; j < bars.size()-1; j++){
        	generateSimulPastSub(index-1, bars.get(j).getTime(), bars.get(j+1).getTime());
        	refreshMData(period, bars.get(j).getTime());
        }
        
        //Fragments
        inStartTime = bars.get(bars.size()-1).getTime();
        if(inEndTime > inStartTime){
        	generateSimulPastSub(index-1, inStartTime, inEndTime);
        } 
	}
	
	long getEndTimeSpecPeriod(Period period, long inEndTime) throws JFException{
		long PrevEndTime = p_history.getPreviousBarStart(period, inEndTime);
		long NextEndTime = p_history.getNextBarStart(period, PrevEndTime);
		
		if(inEndTime == PrevEndTime || inEndTime == NextEndTime){
			return inEndTime;
		}else{
			return PrevEndTime;
		}
	}
}
