package org.neurosystem.modules.marketdata;

import java.util.ArrayList;
import java.util.List;

import org.neurosystem.modules.marketdata.quote.IQuote;
import org.neurosystem.util.basic.HasValues;

public interface IQuoteServer extends HasValues<IQuote> {
	
	public default List<IQuote> valueList(long period) {
		final List<IQuote> values = new ArrayList<>();
		
		long time = lastValue().getTime() - period;
		for(int index = 0; index < size(); index++){
			IQuote quote = valueForward(index);
			
			if(quote.getTime() >= time){
				values.add(quote);
			}
		}

		return values;
	}
	
	public default List<Double> valueList(long period, AppliedPrice appliedPrice) {
		final List<Double> values = new ArrayList<>();
		
		long time = lastValue().getTime() - period;
		for(int index = 0; index < size(); index++){
			IQuote quote = valueForward(index);
			
			if(quote.getTime() >= time){
				values.add(quote.getPrice(appliedPrice));
			}
		}
		
		return values;
	}
}
