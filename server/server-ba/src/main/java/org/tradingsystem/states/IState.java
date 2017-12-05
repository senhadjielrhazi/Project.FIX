package org.tradingsystem.states;

import java.util.List;

import org.tradingsystem.IRefreshData;

import com.dukascopy.api.*;

public interface IState extends IRefreshData {
	public Period getStatePeriod();

	public IState getAIObject();
	
	public List<Double> getAIDataVector();
	
	public List<List<Double>> getAllAIDataVector();
	
	public void generateIndicators(long barTime) throws JFException;
}