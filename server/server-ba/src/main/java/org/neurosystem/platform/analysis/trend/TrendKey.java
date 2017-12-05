package org.neurosystem.platform.analysis.trend;

import org.neurosystem.modules.marketdata.quote.IQuote;
import org.neurosystem.modules.marketdata.quote.Quote;
import org.neurosystem.modules.neuroscience.dna.INucleic;
import org.neurosystem.modules.neuroscience.dna.Nucleic;
import org.neurosystem.util.common.annotations.javax.Nonnull;
import org.neurosystem.util.common.annotations.javax.concurrent.Immutable;

@Immutable
public final class TrendKey implements ITrendKey {
	
	private final ITrend p_trend;
	private final IQuote p_quote;
	private final INucleic p_dna;
	
	private final int p_hashUID;
	
	public TrendKey(@Nonnull ITrend trend, @Nonnull IQuote quote, @Nonnull INucleic dna){
		this.p_trend = trend;
		this.p_quote = quote;
		this.p_dna = dna;
		
		//Fast access UID
		this.p_hashUID = getHashUID();
	}

	private int getHashUID(){
		final int prime = 31;
        int hash = 1;
        
        hash = prime * hash + this.p_trend.hashCode();
        hash = prime * hash + this.p_quote.hashCode();
        hash = prime * hash + this.p_dna.hashCode();
        
	    return hash;
	}
	
	@Override
	public ITrend getTrend() {
		return this.p_trend;
	}
	
	@Override
	public IQuote getQuote() {
		return this.p_quote;
	}
	
	@Override
	public INucleic getNucleic() {
		return this.p_dna;
	}
	
	/* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
	@Override
	public boolean equals(Object obj){
		if (obj == this){
	        return true;
	    }
	    if (obj == null){
	        return false;
	    }
	    if (getClass() != obj.getClass()){
            return false;
	    }
	    
	    TrendKey other = (TrendKey) obj;
	    if (!getTrend().equals(other.getTrend()))
            return false;
	    if (!getQuote().equals(other.getQuote()))
            return false;
	    if (!getNucleic().equals(other.getNucleic()))
            return false;
        
		return true;
	}
	
	/* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
	@Override
	public int hashCode() {
	    return this.p_hashUID;
	}
	
	 /* (non-Javadoc)
    * @see java.lang.Object#toString()
    */
	@Override
	public String toString() {
		return String.format("Trend: %s, Quote: [ %s ], DNA: %s", 
				getTrend(), getQuote(), getNucleic());
	}
	
	public static ITrendKey valueOf(String regex){
		String[] split = regex.split("Trend: |, Quote: \\[ | \\], DNA: |$");

		ITrend trend = ITrend.valueOf(split[1]);
		IQuote quote = Quote.valueOf(split[2]);
		INucleic dna = Nucleic.valueOf(split[3]);
		
		return new TrendKey(trend, quote, dna);
	}
}
