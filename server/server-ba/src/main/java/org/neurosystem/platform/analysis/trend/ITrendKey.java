package org.neurosystem.platform.analysis.trend;

import org.neurosystem.modules.marketdata.quote.IQuote;
import org.neurosystem.modules.neuroscience.dna.INucleic;

public interface ITrendKey {
	public enum ITrend {
		LONG,
		SHORT,
		FLAT;
		
		public boolean isLong(){
	        return (this == LONG);
	    }
		
		public boolean isShort(){
	        return (this == SHORT);
	    }
	}
	
	public ITrend getTrend();
	
	public IQuote getQuote();
	
	public INucleic getNucleic();
}
