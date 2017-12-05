package org.technosystem.modules.marketdata.server;

import java.util.ArrayList;
import java.util.List;

import org.technosystem.modules.marketdata.indicator.IIndicator;
import org.technosystem.modules.marketdata.quote.IQuote;
import org.neurosystem.util.basic.HasValues;
import org.neurosystem.util.time.IPeriod;

public interface IDataServer extends HasValues<IQuote> {
	
	public final static int easybites = 2000;
	
	public void onQuote(IPeriod period, IQuote quote);
	
	public IPeriod getPeriod() ;
	
	public void subscribe(IIndicator<?> indicator);
	
	public void subscribe(IIndicator<?>... indicators);
	
	public default List<IQuote> valueList(long timeBack) {
		final List<IQuote> values = new ArrayList<>();
		
		long time = lastValue().getTime() - timeBack;
		for(int index = 0; index < size(); index++){
			IQuote value = valueForward(index);
			
			if(value.getTime() >= time){
				values.add(value);
			}
		}

		return values;
	}
}
