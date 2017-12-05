package org.neurosystem.client.api.brokers.dk;

import org.neurosystem.util.log.Priority;
import org.neurosystem.client.api.brokers.dk.DKAPI;

import com.dukascopy.api.*;

/**
 * Rules about weight:
 * 
 * 1) The weight is the trading unit for a the main Asset
 * 
 * 2) Long/Short is subject to sign of the weight
 * 
 * 3) the objective is to have equal PnL from a basket.
 * 
 * Meaning a USD/CHF and USD/NOK should have the same amount in USD
 * 
 * the PnL would be different in CHF and NOK but equal in USD for equal moves %
 * 
 * EUR, GBP, CHF, NOK, SEK the weight would be (-0.009, -0.007, 0.01, 0.01, 0.01) 
 * 
 * then normalised the the unite.
 * 
 */
@RequiresFullAccess
@Library("server-ba-2.4.3.jar;")
public class TradingStrategy extends DKAPI {
	
	private String p_tradeLabel = "TSO";
    private int p_slippage = 5;
    private Priority p_level = Priority.ALL;
    
    public TradingStrategy() {
    	super();
    }

    @Override
    public void onStart(IContext context) {
    	super.onStart(context);
    	
    	//Single asset [EUR] vs [USD]
    	addTradingAssets(instrumentToSecurity(0.01, Instrument.EURUSD));
    	
    	
    	//Put on single asset
    	//addTradingAssets(new OptionAsset(0.014, false, new SecurityAsset(1.0, new Security("EUR/USD", 0.001))));//[USD] vs [EUR]
    	
    	//Basket on single assets
    	/*addTradingAssets(new BasketAsset(0.007, new SecurityAsset(-0.6, new Security("EUR/USD", 0.001)), 
    	                new SecurityAsset(-0.5, new Security("GBP/USD", 0.001)),
    	                new SecurityAsset(0.7, new Security("USD/CHF", 0.001)), 
    	                new SecurityAsset(0.7, new Security("USD/SEK", 0.001)), 
    	                new SecurityAsset(0.7, new Security("USD/NOK", 0.001))));///USD vs [EUR,GBP,CHF,NOK,SEK]
    	
    	//Pair assets
    	addTradingAssets(new BasketAsset(0.014, new SecurityAsset(0.60, new Security("EUR/USD", 0.001)), 
        		new SecurityAsset(0.40, new Security("GBP/USD", 0.001))));//[USD] vs [EUR, GBP]
    	
    	//Pair on calls on single assets
    	addTradingAssets(new DispAsset(0.014, true, new SecurityAsset(0.60, new Security("EUR/USD", 0.001)), 
        		new SecurityAsset(0.40, new Security("GBP/USD", 0.001))));//[USD] vs [EUR, GBP]
    	
    	//Pair assets
    	addTradingAssets(new BasketAsset(0.007, new SecurityAsset(0.50, new Security("EUR/USD", 0.001)), 
        		new SecurityAsset(0.50, new Security("EUR/GBP", 0.001))));//[EUR] vs [USD, GBP]
    	
    	//Put on a pair of assets
    	addTradingAssets(new OptionAsset(0.014, false, new BasketAsset(1.0, new SecurityAsset(0.50, new Security("EUR/USD", 0.001)), 
        		new SecurityAsset(0.50, new Security("EUR/GBP", 0.001)))));//[EUR] vs [USD, GBP]
        
        /*addInstrument(Instrument.XAGUSD, 0.0004);
        addInstrument(Instrument.XAUUSD, 0.00001);
        addInstrument(Instrument.AUDCAD, 0.011);
        addInstrument(Instrument.AUDCHF, 0.011);
        addInstrument(Instrument.AUDJPY, 0.011);
        addInstrument(Instrument.AUDNZD, 0.011);
        addInstrument(Instrument.AUDUSD, 0.011);
        addInstrument(Instrument.CADCHF, 0.01);
        addInstrument(Instrument.CADJPY, 0.01);
        addInstrument(Instrument.CHFJPY, 0.009);
        addInstrument(Instrument.EURAUD, 0.007);
        addInstrument(Instrument.EURCAD, 0.007);
        addInstrument(Instrument.EURCHF, 0.007);
        addInstrument(Instrument.EURGBP, 0.007);
        addInstrument(Instrument.EURJPY, 0.007);
        addInstrument(Instrument.EURNZD, 0.007);
        addInstrument(Instrument.EURUSD, 0.007);
        addInstrument(Instrument.GBPAUD, 0.006);
        addInstrument(Instrument.GBPCAD, 0.006);
        addInstrument(Instrument.GBPCHF, 0.006);
        addInstrument(Instrument.GBPJPY, 0.006);
        addInstrument(Instrument.GBPNZD, 0.006);
        addInstrument(Instrument.GBPUSD, 0.006);
        addInstrument(Instrument.NZDCAD, 0.012);
        addInstrument(Instrument.NZDCHF, 0.012);
        addInstrument(Instrument.NZDJPY, 0.012);
        addInstrument(Instrument.NZDUSD, 0.012);
        addInstrument(Instrument.USDCAD, 0.01);
        addInstrument(Instrument.USDCHF, 0.01);
        addInstrument(Instrument.USDJPY, 0.01);*/
        
        super.postStart();
    }

	@Override
    public void onBar(Instrument instrument, Period period, IBar askBar,
            IBar bidBar) {
        super.onBar(instrument, period, askBar, bidBar);
    }
    
    protected String getLabel() {
        return this.p_tradeLabel;
    }

    protected double getSlippage() {
        return this.p_slippage;
    }
    
    protected Priority getPriority() {
		return this.p_level;
	}
}
