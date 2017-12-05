package org.tradingsystem.states;

import java.util.*;

import org.tradingsystem.IStrategyTools;

import com.dukascopy.api.*;
import com.dukascopy.api.IIndicators.AppliedPrice;

public abstract class AbstractState implements IState{
	protected Instrument p_instrument;
	protected double p_pip;
	protected IStrategyTools p_tools;
	protected IHistory p_history;
	protected OfferSide p_side;
	protected AppliedPrice p_AppliedPrice;
	protected IIndicators p_IIndicators;
	private Object lock = new Object();

	// Spec State
	protected Period p_statePeriod;
	protected List<Period> p_refperiods;
	protected IState p_aiObject;	
	protected List<Double> p_stateVector;
	
	public AbstractState(IStrategyTools tools, Instrument instrument) throws JFException {
		p_instrument = instrument;
		p_pip = p_instrument.getPipValue();
		p_tools = tools;

		// Initialisation
		p_history = tools.getHistory();
		p_side = tools.getSide();
		p_AppliedPrice = tools.getAppliedPrice();
		p_IIndicators = tools.getIIndicators();
		p_stateVector = new ArrayList<Double>();
		p_refperiods = new ArrayList<Period>();
	}

	@Override
	public void refreshMData(Period period, long barTime) throws JFException {
		synchronized (lock) {
			if(getStatePeriod().equals(period)){
				generateIndicators(barTime);
				//p_tools.printOut("refreshMData: " + period + "  "+ p_tools.getTimeFormated(barTime) + "  " + p_tools.getListFormated(getAIDataVector()));	
			}
			if(getAIObject() != null){
				getAIObject().refreshMData(period, barTime);
			}
		}
	}
	
	@Override
	public Period getStatePeriod() {
		return p_statePeriod;
	}
	
	@Override
	public List<Period> getRefPeriods(){
        return p_refperiods;
    }
	
	@Override
	public IState getAIObject() {
		return p_aiObject;
	}
	
	@Override
	public List<Double> getAIDataVector(){
		return p_stateVector;
	}
	
	@Override
	public List<List<Double>> getAllAIDataVector(){
		List<List<Double>> aiDataVectors = new ArrayList<List<Double>>();
		aiDataVectors.add(getAIDataVector());
		if(getAIObject() != null){
			aiDataVectors.addAll(getAIObject().getAllAIDataVector());
		}
		return aiDataVectors;
	}
}
