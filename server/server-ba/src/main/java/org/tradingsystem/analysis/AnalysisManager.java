package org.tradingsystem.analysis;

import java.util.*;

import org.tradingsystem.IStrategyTools;

import com.dukascopy.api.*;

public class AnalysisManager{
	// Instrument parameters
	private Instrument p_instrument;
	private IStrategyTools p_tools;
	//private IHistory p_history;
	private Object lock = new Object();
	
	private int p_perTotal = 300;
	
	// Analysis
	private List<AnalysisPath> p_activAnalysis = new ArrayList<AnalysisPath>();
	private List<AnalysisPath> p_stopedAnalysis = new ArrayList<AnalysisPath>();
	
	public AnalysisManager(IStrategyTools tools, Instrument instrument) throws JFException {
		p_instrument = instrument;
		p_tools = tools;
	}

	public void refreshMData(Period period, long barTime, List<List<Double>> aiDataVectors) throws JFException {
		synchronized (lock) {
			// Manage existing
			for(Iterator<AnalysisPath> itr = p_activAnalysis.iterator();itr.hasNext();){  
				AnalysisPath element = itr.next(); 
                if(element.isActive()){
                   element.refreshMData(period, barTime, aiDataVectors); 
                }else{
                	//p_tools.printOut("StopedUpdate: "  + p_tools.getTimeFormated(barTime) + "  " + p_activAnalysis.size() + "  " + p_stpedAnalysis.size());
                	p_stopedAnalysis.add(element);
                    itr.remove(); 
                }
            }
            
			boolean foundL = false, foundS = false;
            for(AnalysisPath data:p_activAnalysis){
                if(data.isLong()){
                    foundL = true;
                }else{
                	foundS = true;
                }
            }
            if(!foundL){
	            // Create new ones
				AnalysisPath analysisL = new AnalysisPath(p_tools, p_instrument, period, barTime, aiDataVectors, true);
				p_activAnalysis.add(analysisL);
            }
            if(!foundS){
	            // Create new ones
				AnalysisPath analysisS = new AnalysisPath(p_tools, p_instrument, period, barTime, aiDataVectors, false);
				p_activAnalysis.add(analysisS);
            }
            
			// Managing limited past 
			if(p_stopedAnalysis.size() > p_perTotal){
				p_stopedAnalysis.remove(0);
	        }
		}
	}
	
	public List<AnalysisPath> getStopedAnalysis(){
		return p_stopedAnalysis;
	}

	public List<AnalysisPath> getActiveAnalysis() {
		return p_activAnalysis;
	}
}
