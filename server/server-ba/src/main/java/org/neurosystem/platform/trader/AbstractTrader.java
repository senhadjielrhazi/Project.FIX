package org.neurosystem.platform.trader;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import org.neurosystem.modules.marketdata.assets.IAsset;
import org.neurosystem.modules.marketdata.assets.basic.ISecurity;
import org.neurosystem.modules.marketdata.quote.IQuote;
import org.neurosystem.modules.neuroscience.dna.INucleic;
import org.neurosystem.platform.analysis.Analysis;
import org.neurosystem.platform.analysis.IAnalysis;
import org.neurosystem.platform.analysis.trend.ITrendKey.ITrend;
import org.neurosystem.platform.trade.ITrade;
import org.neurosystem.modules.riskmetric.pnl.IPnLKey;
import org.neurosystem.modules.riskmetric.pnl.PnLKey;
import org.neurosystem.modules.riskmetric.trade.ITradeKey;
import org.neurosystem.modules.riskmetric.trade.TradeKey;
import org.neurosystem.util.Parameters;
import org.neurosystem.util.basic.HasSide.Side;
import org.neurosystem.util.common.annotations.javax.Nonnull;
import org.neurosystem.util.misc.Pair;

public abstract class AbstractTrader implements ITrader {
	
	private final IAsset p_asset;
	private final IAnalysis p_analyis;
	
	private final List<Pair<INucleic, ITrade>> p_activeTrades;
	private final List<Pair<INucleic, ITradeKey>> p_closedTrades;

	protected abstract ITrade openTrade(@Nonnull IAsset asset, @Nonnull Side side, @Nonnull IQuote quote, @Nonnull IPnLKey target);
	protected abstract ITrade closeTrade(@Nonnull ITrade trade);

	public AbstractTrader(@Nonnull IAsset asset, @Nonnull ExecutorService executor) {
		this.p_asset = asset;
		
		this.p_analyis = new Analysis(asset, executor);
		
		this.p_activeTrades = new ArrayList<>();
		this.p_closedTrades = new ArrayList<>();
	}

	@Override
	public void onHistory(@Nonnull Map<ISecurity, IQuote> assetPrices) {
		getAnalysis().onHistory(assetPrices);
	}

	@Override
	public void onBar(@Nonnull Map<ISecurity, IQuote> assetPrices) {
		//Analysis and market data
		onHistory(assetPrices);

		IQuote quote = getAnalysis().getTrendKey().getQuote();
		INucleic dna = getAnalysis().getTrendKey().getNucleic();
		ITrend pTrend = getAnalysis().getTrendKey().getTrend();
		IPnLKey target = PnLKey.getBasicPNL();
		
		if(pTrend.isLong()){
			for (Iterator<Pair<INucleic, ITrade>> it = getActiveTrades().iterator(); 
				     it.hasNext();){
				Pair<INucleic, ITrade> entry = it.next();
				INucleic key = entry.getKey();
				ITrade trade = entry.getValue();
				
				if(!trade.getSide().isLong()){
					getClosedTrades().add(new Pair<>(key, new TradeKey(closeTrade(trade))));
					it.remove();    	
				}else{
					trade.onQuote(quote);
					
				    if(!trade.isActive()){
				    	getClosedTrades().add(new Pair<>(key, new TradeKey(closeTrade(trade))));
						it.remove();
					} 
				}
			}
			
			if(getActiveTrades().size() < Parameters.getNbMaxTrades()){
				getActiveTrades().add(new Pair<>(dna, openTrade(getAsset(), Side.LONG, quote, target)));
			}	
		}else if(pTrend.isShort()){
			for (Iterator<Pair<INucleic, ITrade>> it = getActiveTrades().iterator(); 
				     it.hasNext();){
				Pair<INucleic, ITrade> entry = it.next();
				INucleic key = entry.getKey();
				ITrade trade = entry.getValue();
				
				if(!trade.getSide().isShort()){
					getClosedTrades().add(new Pair<>(key, new TradeKey(closeTrade(trade))));
					it.remove();	    	
				}else{
					trade.onQuote(quote);
					
				    if(!trade.isActive()){
				    	getClosedTrades().add(new Pair<>(key, new TradeKey(closeTrade(trade))));
						it.remove();	    	
					} 
				}
			}
			
			if(getActiveTrades().size() < Parameters.getNbMaxTrades()){
				getActiveTrades().add(new Pair<>(dna, openTrade(getAsset(), Side.SHORT, quote, target)));		
			}	
		}else{
			for (Iterator<Pair<INucleic, ITrade>> it = getActiveTrades().iterator(); 
				     it.hasNext();){
				Pair<INucleic, ITrade> entry = it.next();
				INucleic key = entry.getKey();
				ITrade trade = entry.getValue();
				
				trade.onQuote(quote);
				
			    if(!trade.isActive()){
			    	getClosedTrades().add(new Pair<>(key, new TradeKey(closeTrade(trade))));
					it.remove();   	
				} 
			}
		}
	}
	
	@Override
	public void onStop() {
	}
	
	protected IAsset getAsset(){
		return this.p_asset;
	}
	
	protected List<Pair<INucleic, ITrade>> getActiveTrades(){
		return this.p_activeTrades;
	}
	
	protected List<Pair<INucleic, ITradeKey>> getClosedTrades(){
		return this.p_closedTrades;
	}
	
	protected IAnalysis getAnalysis(){
		return this.p_analyis;
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
	    
	    AbstractTrader other = (AbstractTrader) obj;
        if (!getAnalysis().equals(other.getAnalysis()))
            return false;
        
		return true;
	}
	
	/* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
	@Override
	public int hashCode() {
        final int prime = 31;
        int hash = 1;

        hash = prime * hash + getAnalysis().hashCode();
	    
        return hash;
	}
	
	/* (non-Javadoc)
    * @see java.lang.Object#toString()
    */
	@Override
	public String toString() {
       return String.format("%s", getAnalysis());
	}
}
