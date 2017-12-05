package org.tradingsystem.states;

import java.util.*;

import org.tradingsystem.IStrategyTools;

import com.dukascopy.api.*;
import com.dukascopy.api.IIndicators.AppliedPrice;

public class T3State extends AbstractState{
	//Indicators
	private Object[] p_params= {new Integer[]{7, 14, 28, 56}, 4, 6, 5, 40.0, 20.0, 14};
    
	public T3State(IStrategyTools tools, Instrument instrument) throws JFException {
		super(tools, instrument);
		
		p_aiObject = null;
		p_statePeriod = Period.FOUR_HOURS;

		p_refperiods.add(p_statePeriod);
		//p_refperiods.addAll(getAIObject().getRefPeriods());
	}

	@Override
	public void generateIndicators(long barTime) throws JFException{
		getAIDataVector().clear();
		
		//Params
        Integer[] p_emaNB = (Integer[])p_params[0];
        int p_perCycle = (Integer)p_params[1];
        int p_perMinMax = (Integer)p_params[2];
        int p_rsiS = (Integer)p_params[3];
        double p_rsiLevel0 = (Double)p_params[4];
        double p_rsiLevel1 = (Double)p_params[5];
        int p_atrS = (Integer)p_params[6];
        double score;
        
        //EMA
        List<List<Double>> emaList = new ArrayList<List<Double>>();
        for(int j = 0; j < p_emaNB.length; j++){
            int emaNB = p_emaNB[j];
            Object[] data = p_IIndicators.calculateIndicator(p_instrument, p_statePeriod, new OfferSide[] {p_side}, "EMA",
            new AppliedPrice[] { p_AppliedPrice }, new Object[] { emaNB }, Filter.WEEKENDS, p_perCycle , barTime, 0); 
            double[] ema = (double[]) data[0]; 
            emaList.add(p_tools.convertToList(ema));        
        }
        
        //EMAs Position
        score = 0.0;
        if(emaList.get(0).get(p_perCycle-1)>emaList.get(1).get(p_perCycle-1)
            && emaList.get(1).get(p_perCycle-1)>emaList.get(2).get(p_perCycle-1)){        
        	score = 1.0;
        }else if(emaList.get(0).get(p_perCycle-1)<emaList.get(1).get(p_perCycle-1)
            && emaList.get(1).get(p_perCycle-1)<emaList.get(2).get(p_perCycle-1)){       
        	score = -1.0;
        }
        getAIDataVector().add(score);

        //EMA Cross over
        score = 0.0;
        if(emaList.get(0).get(p_perCycle-1)>emaList.get(1).get(p_perCycle-1)
            && emaList.get(1).get(p_perCycle-1)>emaList.get(2).get(p_perCycle-1)){
            if(!(emaList.get(0).get(p_perCycle-2)>emaList.get(1).get(p_perCycle-2)
                    && emaList.get(1).get(p_perCycle-2)>emaList.get(2).get(p_perCycle-2))){        
            	score = 1.5;
            }
        }else if(emaList.get(0).get(p_perCycle-1)<emaList.get(1).get(p_perCycle-1)
            && emaList.get(1).get(p_perCycle-1)<emaList.get(2).get(p_perCycle-1)){
            if(!(emaList.get(0).get(p_perCycle-2)<emaList.get(1).get(p_perCycle-2)
                    && emaList.get(1).get(p_perCycle-2)<emaList.get(2).get(p_perCycle-2))){        
            	score = -1.5;
            }
        }
        getAIDataVector().add(score); 
        
        //EMAs Slopes
        score = 0.0;
        for(int j = 1; j < p_emaNB.length; j++){
	        if(emaList.get(j).get(p_perCycle-1)>emaList.get(j).get(0)){     
	        	score += 1.0;
	        }else if(emaList.get(j).get(p_perCycle-1)<emaList.get(j).get(0)){     
	        	score += -1.0;
	        }
        }
        getAIDataVector().add(score); 
        
        //MOMENTUM
        score = 0.0;
        double[] maxData = new double [p_perCycle];
        double[] minData = new double [p_perCycle];
        List<IBar> bars = p_history.getBars(p_instrument, p_statePeriod, p_side, Filter.WEEKENDS, p_perCycle*p_perMinMax, barTime, 0); 
        for(int i = 0; i < p_perCycle; i++){
            int k0 = i*p_perMinMax;
            double max = bars.get(k0).getHigh();
            double min = bars.get(k0).getLow();
            for(int j = 1; j < p_perMinMax; j++){
                int k = k0 + j;
                max = Math.max(max, bars.get(k).getHigh());
                min = Math.min(min, bars.get(k).getLow());
            }
            maxData[i] = max;
            minData[i] = min;
        } 
        if(((maxData[p_perCycle-1]>maxData[0]) && (minData[p_perCycle-1]>minData[0]))){
        	score = 1.5;
        }else if(((minData[p_perCycle-1]<minData[0]) && (maxData[p_perCycle-1]<maxData[0]))){
        	score = -1.5;
        }
        getAIDataVector().add(score); 
         
        //RSI OVERSOLD/OVERBOUGHT
        score = 0.0;
        Object[] dataR = p_IIndicators.calculateIndicator(p_instrument, p_statePeriod, new OfferSide[] {p_side}, "RSI",
                new AppliedPrice[] { p_AppliedPrice }, new Object[] { p_rsiS }, Filter.WEEKENDS, 1, barTime, 0);   
        double rsiS = ((double[]) dataR[0])[0];
        if(rsiS <= p_rsiLevel0){
        	score = 1.0;
        	if(rsiS <= p_rsiLevel1){
            	score = 1.5;
            }
        }else if(rsiS >= 100 - p_rsiLevel0){
        	score = -1.0;
        	if(rsiS >= 100 - p_rsiLevel1){
            	score = -1.5;
            }
        }
        getAIDataVector().add(score);
        
        //MEANREVERSION
        score = 0.0;
        for(int j = 0; j < p_emaNB.length; j++){
	        Object[] dataA = p_IIndicators.calculateIndicator(p_instrument, p_statePeriod, new OfferSide[] {p_side}, "ATR",
	                new AppliedPrice[] { p_AppliedPrice }, new Object[] { p_atrS }, Filter.WEEKENDS, 1, barTime, 0);   
	        double atrS = ((double[]) dataA[0])[0];
	        score += Math.min(3.0, Math.max(-3.0, 0.5*Math.round((emaList.get(j).get(p_perCycle-1)-emaList.get(0).get(p_perCycle-1))/atrS)));
        }
        getAIDataVector().add(score);
	}
}
