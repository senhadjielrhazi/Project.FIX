package org.tradingsystem;

import java.util.List;
import java.util.Map;

import com.dukascopy.api.*;
import com.dukascopy.api.IEngine.OrderCommand;
import com.dukascopy.api.IIndicators.AppliedPrice;

public interface IStrategyTools {
	public void printOut(Object o);

	public String getTimeFormated(long barTime);

	public String getListFormated(List<?> list);

	public Map<Instrument, InstrumentParam> getInstrumentParamList();

	public IHistory getHistory();

	public IIndicators getIIndicators();

	public OfferSide getSide();

	public AppliedPrice getAppliedPrice();

	public double roundPip(Instrument instrument, double value);

	public List<Double> convertToList(double[] dataIn);

	Double getFilledPosition(Instrument instrument) throws JFException;
	
	void closeFilledPosition(Instrument instrument) throws JFException;
	
	void closeOrder(String label) throws JFException;
	
	String submitOrder(OrderCommand orderCmd, Instrument instrument,
			double orderAmount, double price, double stopLossPrice,
			double takeProfitPrice, String comment) throws JFException;
}
