package org.technosystem.modules.marketdata.quote;

import org.technosystem.modules.marketdata.AppliedPrice;
import org.neurosystem.util.basic.HasTime;

public interface IQuote extends HasTime {

	public double getOpen();
	
	public double getHigh();
	
	public double getLow();
	
	public double getClose();
	
	public double getMedian();
	
	public double getTypical();
	
	public default double getPrice(AppliedPrice choice){
		switch (choice) {
		case OPEN:
			return getOpen();
		case HIGH:
			return getHigh();
		case LOW:
			return getLow();
		case CLOSE:
			return getClose();
		case MEDIAN:
			return getMedian();
		case TYPICAL:
			return getTypical();
		default:
			return getClose();
		}
	}
}