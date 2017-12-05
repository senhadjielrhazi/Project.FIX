package org.tradingsystem.states;

import java.util.*;

import org.tradingsystem.IStrategyTools;

import com.dukascopy.api.*;
import com.dukascopy.api.IIndicators.AppliedPrice;

public class T1State extends AbstractState {
	//Indicators
	private Object[] p_params= {new Integer[]{7, 14, 28, 56}, 4, 10.0, 1, 5, 20.0, 14};
    
	public T1State(IStrategyTools tools, Instrument instrument) throws JFException {
		super(tools, instrument);
		
		p_aiObject = new T2State(tools, instrument);
		p_statePeriod = Period.FIVE_MINS;

		p_refperiods.add(p_statePeriod);
		p_refperiods.addAll(getAIObject().getRefPeriods());
	}
	
	@Override
	public void generateIndicators(long barTime) throws JFException{
		getAIDataVector().clear();
		
		//Params
        Integer[] p_emaNB = (Integer[])p_params[0];
        int p_perCycle = (Integer)p_params[1];
        double p_pErr = (Double)p_params[2];
        int p_halfSR = (Integer)p_params[3];
        int p_rsiS = (Integer)p_params[4];
        double p_rsiLevel = (Double)p_params[5];
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
        
        //SR LEVELS
        List <Double> p_SupL = new ArrayList <Double>();
        List <Double> p_ResL = new ArrayList <Double>();
        long currBarTime = p_history.getPreviousBarStart(Period.DAILY, barTime);
        IBar D1bar = p_history.getBars(p_instrument, Period.DAILY, p_side, Filter.WEEKENDS, 1, currBarTime, 0).get(0);
        double H = D1bar.getHigh();
        double C = D1bar.getClose();
        double L = D1bar.getLow();
        double P = (H + C + L) / 3.0;
        p_ResL.add(P);
        p_SupL.add(P);
        for(int i = 0; i < p_halfSR; i++){
            p_ResL.add(p_ResL.get(2*i) + P - L);
            p_ResL.add(p_ResL.get(2*i+1) + H - P);
            p_SupL.add(p_SupL.get(2*i) + P - H);
            p_SupL.add(p_SupL.get(2*i+1) + L - P);  
        }
       
        //EMAs Position
        for(int j = 0; j < p_emaNB.length-1; j++){
	        score = 0.0;
	        if(emaList.get(j).get(p_perCycle-1)>emaList.get(j+1).get(p_perCycle-1)){        
	        	score = 1.0;
	        }else{       
	        	score = -1.0;
	        }
	        getAIDataVector().add(score);
        }
        
        //EMA Cross over
        for(int j = 0; j < p_emaNB.length-1; j++){
	        score = 0.0;
	        if(emaList.get(j).get(p_perCycle-1)>emaList.get(j+1).get(p_perCycle-1)){
	            if(emaList.get(j).get(p_perCycle-2)<emaList.get(j+1).get(p_perCycle-2)){        
	            	score = 1.5;
	            }
	        }else if(emaList.get(j).get(p_perCycle-1)<emaList.get(j+1).get(p_perCycle-1)){
	            if(emaList.get(j).get(p_perCycle-2)>emaList.get(j+1).get(p_perCycle-2)){        
	            	score = -1.5;
	            }
	        }
	        getAIDataVector().add(score); 
        }
        
        //EMAs Slopes
        for(int j = 0; j < p_emaNB.length; j++){
	        score = 0.0;
	        if(emaList.get(j).get(p_perCycle-1)>emaList.get(j).get(0)){     
	        	score = 1.0;
	        }else{     
	        	score = -1.0;
	        }
	        getAIDataVector().add(score);
        }
        
        List<IBar> SRbars = p_history.getBars(p_instrument, p_statePeriod, p_side, Filter.WEEKENDS, p_perCycle, barTime, 0);
        boolean bounce_s, bounce_r;
        //Suport-Resistence
        score = 0.0;
        bounce_s = false; bounce_r = false;
        for(int j = 0; j < p_perCycle; j++){
            //Long
        	for(int i = 1; i < p_SupL.size(); i++){
	            if((SRbars.get(j).getLow() < p_SupL.get(i) && SRbars.get(j).getClose() > p_SupL.get(i)) || Math.abs(SRbars.get(j).getLow() - p_SupL.get(i)) < p_pErr*p_pip){
	            	bounce_s = true;     
	            }
        	}
            //Short
        	for(int i = 1; i < p_ResL.size(); i++){
	            if((SRbars.get(j).getHigh() > p_ResL.get(i) && SRbars.get(j).getClose() < p_ResL.get(i)) || Math.abs(SRbars.get(j).getHigh() - p_ResL.get(i)) < p_pErr*p_pip){
	            	bounce_r = true;     
	            }
        	}
        }
        if(emaList.get(1).get(p_perCycle-1)>emaList.get(2).get(p_perCycle-1) 
            && (bounce_s && !bounce_r)){   
        	score = 1.0;
        }else if(emaList.get(1).get(p_perCycle-1)<emaList.get(2).get(p_perCycle-1) 
            && (bounce_r && !bounce_s)){    
        	score = -1.0;
        }
        getAIDataVector().add(score);
        
        //High-Low
        score = 0.0;
        bounce_s = false; bounce_r = false;
        for(int j = 0; j < p_perCycle; j++){
            //Long
            if((SRbars.get(j).getLow()<emaList.get(2).get(j) && SRbars.get(j).getClose()>emaList.get(2).get(j)) || Math.abs(SRbars.get(j).getLow()-emaList.get(2).get(j))<p_pErr*p_pip){
            	bounce_s = true;     
            }
            //Short
            if((SRbars.get(j).getHigh()>emaList.get(2).get(j)&& SRbars.get(j).getClose()<emaList.get(2).get(j)) || Math.abs(SRbars.get(j).getHigh()-emaList.get(2).get(j))<p_pErr*p_pip){
            	bounce_r = true;     
            }
        }
        if(emaList.get(1).get(p_perCycle-1)>emaList.get(2).get(p_perCycle-1) 
            && (bounce_s && !bounce_r)){   
        	score = 1.0;
        }else if(emaList.get(1).get(p_perCycle-1)<emaList.get(2).get(p_perCycle-1) 
            && (bounce_r && !bounce_s)){    
        	score = -1.0;
        }
        getAIDataVector().add(score); 
        
        //RSI OVERSOLD/OVERBOUGHT
        score = 0.0;
        Object[] dataR = p_IIndicators.calculateIndicator(p_instrument, p_statePeriod, new OfferSide[] {p_side}, "RSI",
                new AppliedPrice[] { p_AppliedPrice }, new Object[] { p_rsiS }, Filter.WEEKENDS, 1, barTime, 0);   
        double rsiS = ((double[]) dataR[0])[0];
        if(rsiS <= p_rsiLevel){
        	score = 1.0;
        }else if(rsiS >= 100 - p_rsiLevel){
        	score = -1.0;
        }
        getAIDataVector().add(score); 
        
        //MEANREVERSION
        score = 0.0;
        Object[] dataA = p_IIndicators.calculateIndicator(p_instrument, p_statePeriod, new OfferSide[] {p_side}, "ATR",
                new AppliedPrice[] { p_AppliedPrice }, new Object[] { p_atrS }, Filter.WEEKENDS, 1, barTime, 0);   
        double atrS = ((double[]) dataA[0])[0];
        score = Math.min(3.0, Math.max(-3.0, 0.5*Math.round((emaList.get(2).get(p_perCycle-1)-emaList.get(0).get(p_perCycle-1))/atrS)));
        getAIDataVector().add(score); 
	}
}
