package org.neurosystem.platform.analysis;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import org.neurosystem.modules.marketdata.assets.IAsset;
import org.neurosystem.modules.marketdata.assets.basic.ISecurity;
import org.neurosystem.modules.marketdata.indicator.IIndicator;
import org.neurosystem.modules.marketdata.indicator.ITimedValue;
import org.neurosystem.modules.marketdata.indicator.basic.*;
import org.neurosystem.modules.marketdata.indicator.custom.GMMX;
import org.neurosystem.modules.marketdata.quote.IQuote;
import org.neurosystem.modules.neuroscience.classification.Classifier;
import org.neurosystem.modules.neuroscience.classification.IClassifier;
import org.neurosystem.modules.neuroscience.classification.IHeuristic;
import org.neurosystem.modules.neuroscience.dna.IGene;
import org.neurosystem.modules.neuroscience.dna.INucleic;
import org.neurosystem.modules.neuroscience.dna.Nucleic;
import org.neurosystem.platform.analysis.trend.ITrendKey;
import org.neurosystem.platform.analysis.trend.ITrendKey.ITrend;
import org.neurosystem.platform.analysis.trend.TrendKey;
import org.neurosystem.platform.trade.DemoTrade;
import org.neurosystem.platform.trade.ITrade;
import org.neurosystem.modules.riskmetric.pnl.IPnLKey;
import org.neurosystem.modules.riskmetric.pnl.PnLKey;
import org.neurosystem.modules.riskmetric.trade.ITradeKey;
import org.neurosystem.modules.riskmetric.trade.TradeKey;
import org.neurosystem.modules.riskmetric.win.IWinKey;
import org.neurosystem.modules.riskmetric.win.WinKey;
import org.neurosystem.util.Parameters;
import org.neurosystem.util.basic.HasSide.Side;
import org.neurosystem.util.basic.HasTime;
import org.neurosystem.util.common.annotations.javax.Nonnull;
import org.neurosystem.util.common.annotations.javax.Nullable;
import org.neurosystem.util.common.base.Objects;
import org.neurosystem.util.common.base.Preconditions;
import org.neurosystem.util.misc.Pair;

public class Analysis implements IAnalysis {

	private final IAsset p_asset;
	private final ExecutorService p_executor;
	
	private final static IPnLKey BASICPNL = PnLKey.getBasicPNL();
	private final static IWinKey BASICWIN =  WinKey.getBasicWin();
			
	private final List<Pair<INucleic, ITrade>> p_activeTrades;
	private final List<Pair<INucleic, ITradeKey>> p_closedTrades;
	
	private final int p_memory;	
	private final int p_timeBuffer;
	
	protected final List<IIndicator<?>> p_indicators;
	protected final List<String> p_labels;
	
	private final IIndicator<Double> p_ema6Ind, p_ema12Ind, p_ema24Ind, p_ema36Ind, p_ema48Ind, p_ema60Ind;
	private final IIndicator<Pair<Double, Double>> p_max1HInd, p_max4HInd, p_max8HInd, p_max1DInd, p_max1WInd;
	private final IIndicator<Pair<Double[], Double[]>> p_momInd;
	private final IIndicator<Double> p_rsiIndS, p_rsiIndL, p_stoIndS, p_stoIndL;
	private final IIndicator<Pair<Double, Double>> p_macdInd;
	private final IIndicator<Pair<Double,Double>> p_adpIndS, p_adpIndL;
	private final IIndicator<Pair<Double,Double>> p_udsIndS, p_udsIndL;
	private final IIndicator<Pair<Double, Double>> p_hls12Ind, p_hls24Ind, p_hls36Ind, p_hls48Ind;
	private final IIndicator<Double> p_atrInd;
	private final IIndicator<Pair<Double, Double>> p_emsdIndS, p_emsdIndL;
	private final IIndicator<Double> p_ocsIndS, p_ocsIndL;
	private final IIndicator<Double> p_cciIndS, p_cciIndL;
	
	//GMMX
	private final IIndicator<Pair<Double, Double>> p_ema12Mx1HInd, p_ema12Mx4HInd, p_ema24Mx4HInd;
	private final IIndicator<Pair<Double, Double>> p_rsiSMx1HInd, p_rsiSMx4HInd, p_rsiSMx8HInd, p_rsiSMx1DInd, p_rsiSMx1WInd;
	private final IIndicator<Pair<Double, Double>> p_rsiLMx1HInd, p_rsiLMx8HInd, p_rsiLMx1DInd, p_rsiLMx1WInd;
	private final IIndicator<Pair<Double, Double>> p_stoSMx1HInd, p_stoSMx4HInd, p_stoSMx8HInd;
	private final IIndicator<Pair<Double, Double>> p_stoLMx4HInd, p_stoLMx1DInd, p_stoLMx1WInd;
	private final IIndicator<Pair<Double, Double>> p_macdMx1HInd, p_macdMx4HInd, p_macdMx8HInd, p_macdMx1DInd, p_macdMx1WInd;
	private final IIndicator<Pair<Double, Double>> p_makyMx1HInd, p_makyMx8HInd, p_makyMx1WInd;
	private final IIndicator<Pair<Double, Double>> p_adpSMx1HInd;
	private final IIndicator<Pair<Double, Double>> p_cciSMx1HInd, p_cciSMx4HInd, p_cciSMx8HInd;
	
	//Input/Output
	private INucleic p_dna = null; 
	private IQuote p_quote = null;
	
	private long p_lastRefresh = 0L;
	private IHeuristic p_heuristics;
	
	public Analysis(@Nonnull IAsset asset, @Nullable ExecutorService executor) {
		this.p_asset = asset;
		this.p_executor = executor;

		//Trades
		this.p_activeTrades = new ArrayList<>();
		this.p_closedTrades = new ArrayList<>();
		
		this.p_memory = (int)getParams()[0];  
		this.p_timeBuffer = Parameters.getTimeBuffer();
		
		//Indicators
		this.p_indicators =  new ArrayList<>();
		this.p_labels = new ArrayList<>();
		
		//EMA
		int[] emaNB = (int[])getParams()[1];
		this.p_ema6Ind = new EMA(this.p_asset, emaNB[0]);
		this.p_indicators.add(this.p_ema6Ind);
		
		this.p_ema12Ind = new EMA(this.p_asset, emaNB[1]);
		this.p_indicators.add(this.p_ema12Ind);
		
		this.p_ema24Ind = new EMA(this.p_asset, emaNB[2]);
		this.p_indicators.add(this.p_ema24Ind);
		
		this.p_ema36Ind = new EMA(this.p_asset, emaNB[3]);
		this.p_indicators.add(this.p_ema36Ind);
		
		this.p_ema48Ind = new EMA(this.p_asset, emaNB[4]);
		this.p_indicators.add(this.p_ema48Ind);
		
		this.p_ema60Ind = new EMA(this.p_asset, emaNB[5]);
		this.p_indicators.add(this.p_ema60Ind);
		
		//MMX
		int[] mmxNB = (int[])getParams()[2];
		this.p_max1HInd = new MMX(this.p_asset, mmxNB[0]);
		this.p_indicators.add(this.p_max1HInd);
		
		this.p_max4HInd = new MMX(this.p_asset, mmxNB[1]);
		this.p_indicators.add(this.p_max4HInd);
		
		this.p_max8HInd = new MMX(this.p_asset, mmxNB[2]);
		this.p_indicators.add(this.p_max8HInd);
		
		this.p_max1DInd = new MMX(this.p_asset, mmxNB[3]);
		this.p_indicators.add(this.p_max1DInd);
		
		this.p_max1WInd = new MMX(this.p_asset, mmxNB[4]);
		this.p_indicators.add(this.p_max1WInd);
		
		//MOM
		int perMinMax = (int)getParams()[3];
		this.p_momInd = new MOM(this.p_asset, this.p_memory, perMinMax);
		this.p_indicators.add(this.p_momInd);
		
		//RSI & STO
		int[] oscNB = (int[])getParams()[4];
        this.p_rsiIndS = new RSI(this.p_asset, oscNB[0]);
        this.p_indicators.add(this.p_rsiIndS);
		
        this.p_rsiIndL = new RSI(this.p_asset, oscNB[1]);
        this.p_indicators.add(this.p_rsiIndL);
        
        this.p_stoIndS = new STO(this.p_asset, oscNB[0]);
        this.p_indicators.add(this.p_stoIndS);
		
        this.p_stoIndL = new STO(this.p_asset, oscNB[1]);
        this.p_indicators.add(this.p_stoIndL);
        
        //MACD
        int[] cdNB = (int[])getParams()[5];
        this.p_macdInd = new MACD(this.p_asset, cdNB[0], cdNB[1], cdNB[2]);
        this.p_indicators.add(this.p_macdInd);
        
        //ADP
        int[] adpNB = (int[])getParams()[6];
		this.p_adpIndS = new ADP(this.p_asset, adpNB[0]);
		this.p_indicators.add(this.p_adpIndS);
		
		this.p_adpIndL = new ADP(this.p_asset, adpNB[1]);
		this.p_indicators.add(this.p_adpIndL);
		
        //UDS
        int[] udsNB = (int[])getParams()[7];
        this.p_udsIndS = new UDS(this.p_asset, udsNB[0], udsNB[2]);
        this.p_indicators.add(this.p_udsIndS);
        
        this.p_udsIndL = new UDS(this.p_asset, udsNB[1], udsNB[2]);
        this.p_indicators.add(this.p_udsIndL);
        
        //HLS
        int[] hlsNB = (int[])getParams()[8];
		this.p_hls12Ind = new HLS(this.p_asset, hlsNB[0]);
		this.p_indicators.add(this.p_hls12Ind);
		
		this.p_hls24Ind = new HLS(this.p_asset, hlsNB[1]);
		this.p_indicators.add(this.p_hls24Ind);
		
		this.p_hls36Ind = new HLS(this.p_asset, hlsNB[2]);
		this.p_indicators.add(this.p_hls36Ind);
		
		this.p_hls48Ind = new HLS(this.p_asset, hlsNB[3]);
		this.p_indicators.add(this.p_hls48Ind);
		
		//ATR
		int atrNB = (int)getParams()[9];
		this.p_atrInd = new ATR(this.p_asset, atrNB);
		this.p_indicators.add(this.p_atrInd);
		
        //EMSD
		int emsdNB[] = (int[])getParams()[10];
        this.p_emsdIndS = new EMSD(this.p_asset, emsdNB[0]);
        this.p_indicators.add(this.p_emsdIndS);
        
        this.p_emsdIndL = new EMSD(this.p_asset, emsdNB[1]);
        this.p_indicators.add(this.p_emsdIndL);
		
        //OCS && CCI
        int[] occNB = (int[])getParams()[11];
        this.p_ocsIndS = new OCS(this.p_asset, occNB[0]);
        this.p_indicators.add(this.p_ocsIndS);
        
        this.p_ocsIndL = new OCS(this.p_asset, occNB[1]);
        this.p_indicators.add(this.p_ocsIndL);
        
  		this.p_cciIndS = new CCI(this.p_asset, occNB[0]);
  		this.p_indicators.add(this.p_cciIndS);
  		
  		this.p_cciIndL = new CCI(this.p_asset, occNB[1]);
  		this.p_indicators.add(this.p_cciIndL);
  		
  		//EMA MX
		this.p_ema12Mx1HInd = new GMMX((IIndicator<Double>)this.p_ema12Ind, mmxNB[0]);
		this.p_indicators.add(this.p_ema12Mx1HInd);
		
		this.p_ema12Mx4HInd = new GMMX((IIndicator<Double>)this.p_ema12Ind, mmxNB[1]);
		this.p_indicators.add(this.p_ema12Mx4HInd);
		
		this.p_ema24Mx4HInd = new GMMX((IIndicator<Double>)this.p_ema24Ind, mmxNB[1]);
		this.p_indicators.add(this.p_ema24Mx4HInd);
		
		//RSI && STO MX
		this.p_rsiSMx1HInd = new GMMX(this.p_rsiIndS, mmxNB[0]);
        this.p_indicators.add(this.p_rsiSMx1HInd);
        
		this.p_rsiSMx4HInd = new GMMX(this.p_rsiIndS, mmxNB[1]);
        this.p_indicators.add(this.p_rsiSMx4HInd);
        
		this.p_rsiSMx8HInd = new GMMX(this.p_rsiIndS, mmxNB[2]);
        this.p_indicators.add(this.p_rsiSMx8HInd);
        
		this.p_rsiSMx1DInd = new GMMX(this.p_rsiIndS, mmxNB[3]);
        this.p_indicators.add(this.p_rsiSMx1DInd);
        
		this.p_rsiSMx1WInd = new GMMX(this.p_rsiIndS, mmxNB[4]);
        this.p_indicators.add(this.p_rsiSMx1WInd);
        
		this.p_rsiLMx1HInd = new GMMX(this.p_rsiIndL, mmxNB[0]);
        this.p_indicators.add(this.p_rsiLMx1HInd);
        
		this.p_rsiLMx8HInd = new GMMX(this.p_rsiIndL, mmxNB[2]);
        this.p_indicators.add(this.p_rsiLMx8HInd);
        
		this.p_rsiLMx1DInd = new GMMX(this.p_rsiIndL, mmxNB[3]);
        this.p_indicators.add(this.p_rsiLMx1DInd);
        
		this.p_rsiLMx1WInd = new GMMX(this.p_rsiIndL, mmxNB[4]);
        this.p_indicators.add(this.p_rsiLMx1WInd);

		this.p_stoSMx1HInd = new GMMX(this.p_stoIndS, mmxNB[0]);
        this.p_indicators.add(this.p_stoSMx1HInd);
        
		this.p_stoSMx4HInd = new GMMX(this.p_stoIndS, mmxNB[1]);
        this.p_indicators.add(this.p_stoSMx4HInd);
        
		this.p_stoSMx8HInd = new GMMX(this.p_stoIndS, mmxNB[2]);
        this.p_indicators.add(this.p_stoSMx8HInd);
        
		this.p_stoLMx4HInd = new GMMX(this.p_stoIndL, mmxNB[1]);
        this.p_indicators.add(this.p_stoLMx4HInd);
        
		this.p_stoLMx1DInd = new GMMX(this.p_stoIndL, mmxNB[3]);
        this.p_indicators.add(this.p_stoLMx1DInd);
        
		this.p_stoLMx1WInd = new GMMX(this.p_stoIndL, mmxNB[4]);
        this.p_indicators.add(this.p_stoLMx1WInd);
		        
		//MACD MX		
		IIndicator<Double> macdValue = this.p_macdInd.valueSplit();
		this.p_macdMx1HInd = new GMMX(macdValue, mmxNB[0]);
		this.p_indicators.add(this.p_macdMx1HInd);
		
		this.p_macdMx4HInd = new GMMX(macdValue, mmxNB[1]);
		this.p_indicators.add(this.p_macdMx4HInd);
		
		this.p_macdMx8HInd = new GMMX(macdValue, mmxNB[2]);
		this.p_indicators.add(this.p_macdMx8HInd);
		
		this.p_macdMx1DInd = new GMMX(macdValue, mmxNB[3]);
		this.p_indicators.add(this.p_macdMx1DInd);
		
		this.p_macdMx1WInd = new GMMX(macdValue, mmxNB[4]);
		this.p_indicators.add(this.p_macdMx1WInd);
		
		IIndicator<Double> macdKey = this.p_macdInd.keySplit();
		this.p_makyMx1HInd = new GMMX(macdKey, mmxNB[0]);
		this.p_indicators.add(this.p_makyMx1HInd);
		
		this.p_makyMx8HInd = new GMMX(macdKey, mmxNB[2]);
		this.p_indicators.add(this.p_makyMx8HInd);
		
		this.p_makyMx1WInd = new GMMX(macdKey, mmxNB[4]);
		this.p_indicators.add(this.p_makyMx1WInd);
		
		//ADP MX
		IIndicator<Double> adpKey = this.p_adpIndS.keySplit();
		this.p_adpSMx1HInd = new GMMX(adpKey, mmxNB[0]);
        this.p_indicators.add(this.p_adpSMx1HInd);
        
        //CCI MX
  		this.p_cciSMx1HInd = new GMMX(this.p_cciIndS, mmxNB[0]);
    	this.p_indicators.add(this.p_cciSMx1HInd);
    	
  		this.p_cciSMx4HInd = new GMMX(this.p_cciIndS, mmxNB[1]);
    	this.p_indicators.add(this.p_cciSMx4HInd);
    	
    	this.p_cciSMx8HInd = new GMMX(this.p_cciIndS, mmxNB[2]);
    	this.p_indicators.add(this.p_cciSMx8HInd);
	}

	@Override
	public void onHistory(Map<ISecurity, IQuote> assetPrices) {
		//Calculate Index
		this.p_quote = this.p_asset.onValue(assetPrices);
		
		//Refresh Indicators
		for(IIndicator<?> indicator:this.p_indicators){
			indicator.calculate();
		}
		
		if(this.p_asset.size() < this.p_timeBuffer){
			return;
		}
		
		//Calculate key Nucleic	
		this.p_dna = calculate();
		
		if(this.p_lastRefresh == 0L){
			this.p_lastRefresh = this.p_quote.getTime();
		}
		
		//Generate trades
		if(HasTime.sameTimeZone(this.p_quote.getTime(), 30)){
			this.p_activeTrades.add(new Pair<>(this.p_dna, new DemoTrade(this.p_asset, Side.LONG, this.p_quote, BASICPNL)));
			this.p_activeTrades.add(new Pair<>(this.p_dna, new DemoTrade(this.p_asset, Side.SHORT, this.p_quote, BASICPNL)));
		}

		//Refresh demo trades
		for (Iterator<Pair<INucleic, ITrade>> it = this.p_activeTrades.iterator(); 
			     it.hasNext();){
			Pair<INucleic, ITrade> entry = it.next();
			INucleic key = entry.getKey();
			ITrade trade = entry.getValue();
			
			trade.onQuote(this.p_quote);
		    if(!trade.isActive() || !HasTime.timeZone(this.p_quote.getTime(), trade.getTime())){
		    	this.p_closedTrades.add(new Pair<>(key, new TradeKey(trade)));
		    	it.remove(); 
			}
		}
	}
	
	@Override
	public ITrendKey getTrendKey() {
		Preconditions.checkArgument(this.p_executor != null, "The service executor is not provided!");		
		
		//Classification
		if(this.p_quote.getTime() >= this.p_lastRefresh + Parameters.getClassificationFQ()){
			IClassifier classifier = new Classifier(this.p_dna.size(), this.p_executor);		
			
			for (Iterator<Pair<INucleic, ITradeKey>> it = this.p_closedTrades.iterator(); 
				     it.hasNext();){
				Pair<INucleic, ITradeKey> entry = it.next();
				ITradeKey trade = entry.getValue();

			    if(trade.getTime() >= this.p_quote.getTime() - Parameters.getHistoryBack()){
			    	classifier.addEntry(entry); 
				}else{
					it.remove();
				}
			}
			
			//Reset
			this.p_lastRefresh = this.p_quote.getTime();
			
			//Heuristics details
			this.p_heuristics = classifier.classification(BASICWIN);
		}
		
		//Forecast
		Integer tzValue = HasTime.timeZone(this.p_quote.getTime());
		INucleic muKey = this.p_heuristics.getMorphism().morph(this.p_dna);
		
		ITrend trend = ITrend.FLAT;
		if (Side.LONG == this.p_heuristics.getGenome().get(tzValue).get(muKey)) {
			trend = ITrend.LONG;
		}
		if (Side.SHORT == this.p_heuristics.getGenome().get(tzValue).get(muKey)) {
			trend = ITrend.SHORT;
		}
		
		return new TrendKey(trend, this.p_quote, this.p_dna);
	}
	
	@Override
	public List<Pair<INucleic, ITradeKey>> getClosedTrades(){
		return this.p_closedTrades;
	}
	
	private Object[] getParams() {
		return Parameters.getAnalysisParams();
	}
	
	private INucleic calculate() {
		IGene gene;
		List<IGene> genes = new ArrayList<>();
		this.p_labels.clear();
		
		//Params
        double[] udsLevel = (double[])getParams()[12];
        double[] maxLevel = (double[])getParams()[13];
		double[] priceLevel = (double[])getParams()[14];
		double rsiLevel = (double)getParams()[15];
        double ocsLevel = (double)getParams()[16];
		double emaLevel = (double)getParams()[17];
		double rocLevel = (double)getParams()[18];
        double cciLevel = (double)getParams()[19];

        int timeStep = 6;
        double atr = this.p_atrInd.lastValue().getValue();
        Double[] maxData = this.p_momInd.lastValue().getValue().getValue();
        Double[] minData = this.p_momInd.lastValue().getValue().getKey();
        List<IQuote> prices = this.p_asset.valueList(this.p_memory);
        
        /*************************************************OPTIMA*************************************************/
		gene = IGene.F;
		if(this.p_udsIndS.lastValue().getValue().getKey() > udsLevel[0] &&
				this.p_udsIndS.lastValue().getValue().getValue() >= this.p_udsIndS.valueBack(this.p_memory).getValue().getValue() &&
        		this.p_cciIndS.lastValue().getValue() <= this.p_cciIndS.valueBack(this.p_memory).getValue()){
			gene = IGene.U;
		}
		if(this.p_udsIndS.lastValue().getValue().getKey() < 100.-udsLevel[0] &&
				this.p_udsIndS.lastValue().getValue().getValue() <= this.p_udsIndS.valueBack(this.p_memory).getValue().getValue() &&
        		this.p_cciIndS.lastValue().getValue() >= this.p_cciIndS.valueBack(this.p_memory).getValue()){
			gene = IGene.D;
		}
		genes.add(gene);
		this.p_labels.add("XCOMB-4");
        
        gene = IGene.F;
        if(this.p_asset.valueBack(5*timeStep).getClose() < this.p_max1HInd.valueBack(5*timeStep).getValue().getValue() * (1 - maxLevel[0])
        		&& this.p_asset.valueBack(5*timeStep).getClose() >= this.p_max4HInd.valueBack(5*timeStep).getValue().getValue() * (1 - maxLevel[1])){
        	gene = IGene.D;
        }else if(this.p_asset.valueBack(5*timeStep).getClose() > this.p_max1HInd.valueBack(5*timeStep).getValue().getKey() * (1 + maxLevel[0])
        		&& this.p_asset.valueBack(5*timeStep).getClose() <= this.p_max4HInd.valueBack(5*timeStep).getValue().getKey() * (1 + maxLevel[1])){
        	gene = IGene.U;
        }else{
        	if(this.p_asset.valueBack(5*timeStep).getClose() < this.p_max4HInd.valueBack(5*timeStep).getValue().getValue() * (1 - maxLevel[1])){
            	gene = IGene.U;
            }
            if(this.p_asset.valueBack(5*timeStep).getClose() > this.p_max4HInd.valueBack(5*timeStep).getValue().getKey() * (1 + maxLevel[1])){
            	gene = IGene.D;
            }
        }
        genes.add(gene);
        this.p_labels.add("XTIME-10");
        
        gene = IGene.F;
        if(this.p_hls24Ind.lastValue().getValue().getKey() >= this.p_hls24Ind.valueBack(this.p_memory).getValue().getKey()  &&
        		this.p_hls24Ind.lastValue().getValue().getValue() < this.p_hls24Ind.valueBack(this.p_memory).getValue().getValue()){
        	gene = IGene.D;
        }
        if(this.p_hls24Ind.lastValue().getValue().getKey() <= this.p_hls24Ind.valueBack(this.p_memory).getValue().getKey()  &&
        		this.p_hls24Ind.lastValue().getValue().getValue() > this.p_hls24Ind.valueBack(this.p_memory).getValue().getValue()){
        	gene = IGene.U;
        }
        genes.add(gene);
        this.p_labels.add("HLS-5");
        
    	gene = IGene.F;
        if(this.p_max1HInd.lastValue().getValue().getValue() > this.p_max1HInd.valueBack(this.p_memory).getValue().getValue() &&
        		this.p_max1HInd.lastValue().getValue().getKey() >= this.p_max1HInd.valueBack(this.p_memory).getValue().getKey()){
        	gene = IGene.D;
        }
        if(this.p_max1HInd.lastValue().getValue().getValue() <= this.p_max1HInd.valueBack(this.p_memory).getValue().getValue() &&
        		this.p_max1HInd.lastValue().getValue().getKey() < this.p_max1HInd.valueBack(this.p_memory).getValue().getKey()){
        	gene = IGene.U;
        }
        genes.add(gene);
        this.p_labels.add("MMX-5");
        
        gene = IGene.F;
        if(this.p_macdInd.lastValue().getValue().getKey() > this.p_macdInd.lastValue().getValue().getValue() &&
        		this.p_macdInd.valueBack(1).getValue().getKey() <= this.p_macdInd.valueBack(1).getValue().getValue()){
        	gene = IGene.U;
        }
        if(this.p_macdInd.lastValue().getValue().getKey() < this.p_macdInd.lastValue().getValue().getValue() &&
        		this.p_macdInd.valueBack(1).getValue().getKey() >= this.p_macdInd.valueBack(1).getValue().getValue()){
        	gene = IGene.D;
        }
        genes.add(gene);
        this.p_labels.add("XMACD-0");
        
        gene = IGene.F;
        if(this.p_asset.lastValue().getClose() < this.p_ema36Ind.lastValue().getValue()*(1 - priceLevel[1]) &&
        		this.p_asset.valueBack(1).getClose() >= this.p_ema36Ind.valueBack(1).getValue()*(1 - priceLevel[1]) &&
        		this.p_asset.lastValue().getClose() > this.p_max4HInd.valueBack(1).getValue().getKey()){
        	gene = IGene.U;
        }
        if(this.p_asset.lastValue().getClose() > this.p_ema36Ind.lastValue().getValue()*(1 + priceLevel[1]) &&
        		this.p_asset.valueBack(1).getClose() <= this.p_ema36Ind.valueBack(1).getValue()*(1 + priceLevel[1]) &&
        		this.p_asset.lastValue().getClose() < this.p_max4HInd.valueBack(1).getValue().getValue()){
        	gene = IGene.D;
        }
        genes.add(gene);
        this.p_labels.add("XDIV-MA1");
        
		gene = IGene.F;
        if(this.p_asset.lastValue().getClose() > this.p_max1WInd.lastValue().getValue().getKey() * (1 + maxLevel[4])){
        	gene = IGene.D;
        }
        if(this.p_asset.lastValue().getClose() < this.p_max1WInd.lastValue().getValue().getValue() * (1 - maxLevel[4])){
        	gene = IGene.U;
        }
        genes.add(gene);
        this.p_labels.add("MMX-32");
        
        gene = IGene.F;
        if(this.p_stoIndL.lastValue().getValue() <= this.p_stoLMx4HInd.valueBack(1).getValue().getKey() &&
        		this.p_macdInd.lastValue().getValue().getValue() > this.p_macdMx4HInd.lastValue().getValue().getKey()){
        	gene = IGene.U;
        }
        if(this.p_stoIndL.lastValue().getValue() >= this.p_stoLMx4HInd.valueBack(1).getValue().getValue() &&
        		this.p_macdInd.lastValue().getValue().getValue() < this.p_macdMx4HInd.lastValue().getValue().getValue()){
        	gene = IGene.D;
        }
        genes.add(gene);
        this.p_labels.add("DIVMS-6");
        
        gene = IGene.F;
        if(this.p_ema12Ind.lastValue().getValue() > this.p_ema12Ind.valueBack(this.p_memory).getValue() &&
        		this.p_ema12Ind.valueBack(1).getValue() <= this.p_ema12Ind.valueBack(this.p_memory+1).getValue()){
        	gene = IGene.D;
        }
        if(this.p_ema12Ind.lastValue().getValue() < this.p_ema12Ind.valueBack(this.p_memory).getValue() &&
        		this.p_ema12Ind.valueBack(1).getValue() >= this.p_ema12Ind.valueBack(this.p_memory+1).getValue()){
        	gene = IGene.U;
        }
        genes.add(gene);
        this.p_labels.add("ROC-0");
        
        gene = IGene.F;
        if(this.p_stoIndS.lastValue().getValue() > this.p_stoIndL.lastValue().getValue() &&
        		this.p_stoIndS.valueBack(1).getValue() <= this.p_stoIndL.valueBack(1).getValue() &&
        		this.p_ema48Ind.lastValue().getValue() > this.p_ema48Ind.valueBack(this.p_memory).getValue() &&
        		this.p_hls48Ind.lastValue().getValue().getKey() >= this.p_hls48Ind.valueBack(this.p_memory).getValue().getKey()){
        	gene = IGene.D;
        }
        if(this.p_stoIndS.lastValue().getValue() < this.p_stoIndL.lastValue().getValue() &&
        		this.p_stoIndS.valueBack(1).getValue() >= this.p_stoIndL.valueBack(1).getValue() &&
        		this.p_ema48Ind.lastValue().getValue() < this.p_ema48Ind.valueBack(this.p_memory).getValue() &&
        		this.p_hls48Ind.lastValue().getValue().getKey() <= this.p_hls48Ind.valueBack(this.p_memory).getValue().getKey()){
        	gene = IGene.U;
        }
        genes.add(gene);
        this.p_labels.add("XCOMB-2");
        
    	gene = IGene.F;
        if(this.p_max1DInd.lastValue().getValue().getValue() > this.p_max1DInd.valueBack(this.p_memory).getValue().getValue() &&
        		this.p_max1DInd.lastValue().getValue().getKey() >= this.p_max1DInd.valueBack(this.p_memory).getValue().getKey()){
        	gene = IGene.D;
        }
        if(this.p_max1DInd.lastValue().getValue().getValue() <= this.p_max1DInd.valueBack(this.p_memory).getValue().getValue() &&
        		this.p_max1DInd.lastValue().getValue().getKey() < this.p_max1DInd.valueBack(this.p_memory).getValue().getKey()){
        	gene = IGene.U;
        }
        genes.add(gene);
        this.p_labels.add("MMX-8");
        
    	gene = IGene.F;
        if(this.p_rsiIndL.lastValue().getValue() <= this.p_rsiLMx1HInd.valueBack(1).getValue().getKey() &&
        		this.p_macdInd.lastValue().getValue().getValue() > this.p_macdMx1HInd.lastValue().getValue().getKey()){
        	gene = IGene.U;
        }
        if(this.p_rsiIndL.lastValue().getValue() >= this.p_rsiLMx1HInd.valueBack(1).getValue().getValue() &&
        		this.p_macdInd.lastValue().getValue().getValue() < this.p_macdMx1HInd.lastValue().getValue().getValue()){
        	gene = IGene.D;
        }
        genes.add(gene);
        this.p_labels.add("DIVMR-5");
        
		gene = IGene.F;
        if(this.p_ocsIndL.lastValue().getValue() > 0 &&
        		this.p_ocsIndL.lastValue().getValue() >= this.p_ocsIndL.valueBack(this.p_memory).getValue() &&
        		this.p_asset.lastValue().getClose() < this.p_max8HInd.lastValue().getValue().getValue() * (1 - maxLevel[2])){
        	gene = IGene.D;
        }
        if(this.p_ocsIndL.lastValue().getValue() < 0 &&
        		this.p_ocsIndL.lastValue().getValue() <= this.p_ocsIndL.valueBack(this.p_memory).getValue() &&
        		this.p_asset.lastValue().getClose() > this.p_max8HInd.lastValue().getValue().getKey() * (1 + maxLevel[2])){
        	gene = IGene.U;
        }
        genes.add(gene);
        this.p_labels.add("XDIV-OCS");
        
        gene = IGene.F;
        if(this.p_asset.lastValue().getClose() > (this.p_emsdIndL.lastValue().getValue().getKey() - 2 * this.p_emsdIndL.lastValue().getValue().getValue()) &&
        		this.p_asset.valueBack(1).getClose() <= (this.p_emsdIndL.valueBack(1).getValue().getKey() - 2 * this.p_emsdIndL.valueBack(1).getValue().getValue())){
        	gene = IGene.D;
        }
        if(this.p_asset.lastValue().getClose() < (this.p_emsdIndL.lastValue().getValue().getKey() + 2 * this.p_emsdIndL.lastValue().getValue().getValue()) &&
        		this.p_asset.valueBack(1).getClose() >= (this.p_emsdIndL.valueBack(1).getValue().getKey() + 2 * this.p_emsdIndL.valueBack(1).getValue().getValue())){
        	gene = IGene.U;
        }
        genes.add(gene);          
        this.p_labels.add("EMSD-5");
        
        gene = IGene.F;
        if(this.p_asset.lastValue().getClose() < (this.p_emsdIndS.lastValue().getValue().getKey() - 2 * this.p_emsdIndS.lastValue().getValue().getValue()) &&
        		this.p_asset.lastValue().getLow() > this.p_max1HInd.valueBack(1).getValue().getKey()){
        	gene = IGene.U;
        }
        if(this.p_asset.lastValue().getClose() > (this.p_emsdIndS.lastValue().getValue().getKey() + 2 * this.p_emsdIndS.lastValue().getValue().getValue()) &&
        		this.p_asset.lastValue().getHigh() < this.p_max1HInd.valueBack(1).getValue().getValue()){
        	gene = IGene.D;
        }
        genes.add(gene);
        this.p_labels.add("XEMSD-1");
        
        gene = IGene.F;
        if(this.p_rsiIndS.lastValue().getValue() > this.p_rsiIndL.lastValue().getValue() &&
        		this.p_rsiIndS.valueBack(1).getValue() <= this.p_rsiIndL.valueBack(1).getValue() && 
                this.p_ema6Ind.lastValue().getValue() > this.p_ema6Ind.valueBack(this.p_memory).getValue() &&
        		this.p_hls12Ind.lastValue().getValue().getValue() > this.p_hls12Ind.valueBack(this.p_memory).getValue().getValue()){
        	gene = IGene.D;
        }
        if(this.p_rsiIndS.lastValue().getValue() < this.p_rsiIndL.lastValue().getValue() &&
        		this.p_rsiIndS.valueBack(1).getValue() >= this.p_rsiIndL.valueBack(1).getValue() && 
                this.p_ema6Ind.lastValue().getValue() < this.p_ema6Ind.valueBack(this.p_memory).getValue() &&
        		this.p_hls12Ind.lastValue().getValue().getValue() < this.p_hls12Ind.valueBack(this.p_memory).getValue().getValue()){
        	gene = IGene.U;
        }
        genes.add(gene);
        this.p_labels.add("XCOMB-1");
        
		gene = IGene.F;
		if (((maxData[this.p_memory - 1] > maxData[0]) && (minData[this.p_memory - 1] > minData[0]))
				&& this.p_rsiIndS.lastValue().getValue() < rsiLevel) {
			gene = IGene.U;
		} else if (((minData[this.p_memory - 1] < minData[0]) && (maxData[this.p_memory - 1] < maxData[0]))
				&& this.p_rsiIndS.lastValue().getValue() > 100-rsiLevel) {
			gene = IGene.D;
		}
		genes.add(gene);
		this.p_labels.add("MOM-0");
        
    	gene = IGene.F;
        if(this.p_asset.lastValue().getClose() > this.p_max1DInd.lastValue().getValue().getKey() * (1 + maxLevel[3])){
        	gene = IGene.D;
        }
        if(this.p_asset.lastValue().getClose() < this.p_max1DInd.lastValue().getValue().getValue() * (1 - maxLevel[3])){
        	gene = IGene.U;
        }
        genes.add(gene);
        this.p_labels.add("MMX-31");
        
		gene = IGene.F;
        if(this.p_asset.lastValue().getClose() > this.p_max1DInd.lastValue().getValue().getKey() * (1 + maxLevel[3])){
        	if(this.p_asset.lastValue().getClose() >= this.p_ema60Ind.lastValue().getValue() ||
            		this.p_asset.valueBack(1).getClose() < this.p_ema60Ind.valueBack(1).getValue() ||
            		this.p_max1HInd.lastValue().getValue().getValue() <= this.p_max1HInd.valueBack(this.p_memory).getValue().getValue() ||
            		this.p_max1HInd.lastValue().getValue().getKey() < this.p_max1HInd.valueBack(this.p_memory).getValue().getKey()){
        		gene = IGene.D;
            }
        }else if(this.p_asset.lastValue().getClose() < this.p_max1DInd.lastValue().getValue().getValue() * (1 - maxLevel[3])){
        	if(this.p_asset.lastValue().getClose() <= this.p_ema60Ind.lastValue().getValue() ||
            		this.p_asset.valueBack(1).getClose() > this.p_ema60Ind.valueBack(1).getValue() ||
            		this.p_max1HInd.lastValue().getValue().getValue() > this.p_max1HInd.valueBack(this.p_memory).getValue().getValue() ||
            		this.p_max1HInd.lastValue().getValue().getKey() >= this.p_max1HInd.valueBack(this.p_memory).getValue().getKey()){
        		gene = IGene.U;
            }
        }else{
        	if(this.p_asset.lastValue().getClose() > this.p_ema60Ind.lastValue().getValue() &&
            		this.p_asset.valueBack(1).getClose() <= this.p_ema60Ind.valueBack(1).getValue() &&
            		this.p_max1HInd.lastValue().getValue().getValue() <= this.p_max1HInd.valueBack(this.p_memory).getValue().getValue() &&
            		this.p_max1HInd.lastValue().getValue().getKey() < this.p_max1HInd.valueBack(this.p_memory).getValue().getKey()){
            	gene = IGene.D;
            }
            if(this.p_asset.lastValue().getClose() < this.p_ema60Ind.lastValue().getValue() &&
            		this.p_asset.valueBack(1).getClose() >= this.p_ema60Ind.valueBack(1).getValue() &&
            		this.p_max1HInd.lastValue().getValue().getValue() > this.p_max1HInd.valueBack(this.p_memory).getValue().getValue() &&
            		this.p_max1HInd.lastValue().getValue().getKey() >= this.p_max1HInd.valueBack(this.p_memory).getValue().getKey()){
            	gene = IGene.U;
            }
        }
        genes.add(gene);
        this.p_labels.add("XMMX-3");

    	gene = IGene.F;
        if(this.p_asset.lastValue().getHigh() >= this.p_max4HInd.valueBack(1).getValue().getValue()){
        	gene = IGene.D;
        }
        if(this.p_asset.lastValue().getLow() <= this.p_max4HInd.valueBack(1).getValue().getKey()){
        	gene = IGene.U;
        }
        genes.add(gene);
        this.p_labels.add("MMX-1");
        
        gene = IGene.F;
		if(this.p_udsIndS.lastValue().getValue().getKey() < udsLevel[0] &&
				this.p_asset.lastValue().getHigh() >= this.p_max4HInd.valueBack(1).getValue().getValue() &&
        		this.p_macdInd.lastValue().getValue().getValue() < this.p_macdMx4HInd.lastValue().getValue().getValue()){
			gene = IGene.D;
		}
		if(this.p_udsIndS.lastValue().getValue().getKey() > 100.-udsLevel[0] &&
				this.p_asset.lastValue().getLow() <= this.p_max4HInd.valueBack(1).getValue().getKey() &&
        		this.p_macdInd.lastValue().getValue().getValue() > this.p_macdMx4HInd.lastValue().getValue().getKey()){
			gene = IGene.U;
		}
		genes.add(gene);
		this.p_labels.add("XDIV-UDS");
        
        gene = IGene.F;
        if(this.p_max8HInd.lastValue().getValue().getValue() > this.p_max8HInd.valueBack(this.p_memory).getValue().getValue() &&
        		this.p_max8HInd.lastValue().getValue().getKey() >= this.p_max8HInd.valueBack(this.p_memory).getValue().getKey() &&
        		this.p_asset.lastValue().getClose() > this.p_max1DInd.lastValue().getValue().getKey() * (1 + maxLevel[3])){
        	gene = IGene.D;
        }
        if(this.p_max8HInd.lastValue().getValue().getValue() <= this.p_max8HInd.valueBack(this.p_memory).getValue().getValue() &&
        		this.p_max8HInd.lastValue().getValue().getKey() < this.p_max8HInd.valueBack(this.p_memory).getValue().getKey() &&
        		this.p_asset.lastValue().getClose() < this.p_max1DInd.lastValue().getValue().getValue() * (1 - maxLevel[3])){
        	gene = IGene.U;
        }
        genes.add(gene);
        this.p_labels.add("XMMX-2");

        gene = IGene.F;
        if(this.p_asset.lastValue().getLow() <= this.p_max1HInd.valueBack(1).getValue().getKey() &&
        		this.p_macdInd.lastValue().getValue().getValue() > this.p_macdMx1HInd.lastValue().getValue().getKey() &&
        		this.p_asset.lastValue().getLow() < this.p_max4HInd.valueBack(1).getValue().getKey() &&
        		this.p_rsiIndS.lastValue().getValue() > this.p_rsiSMx4HInd.lastValue().getValue().getKey()){
        	gene = IGene.U;
        }
        if(this.p_asset.lastValue().getHigh() >= this.p_max1HInd.valueBack(1).getValue().getValue() &&
        		this.p_macdInd.lastValue().getValue().getValue() < this.p_macdMx1HInd.lastValue().getValue().getValue() &&
        		this.p_asset.lastValue().getHigh() > this.p_max4HInd.valueBack(1).getValue().getValue() &&
        		this.p_rsiIndS.lastValue().getValue() < this.p_rsiSMx4HInd.lastValue().getValue().getValue()){
        	gene = IGene.D;
        }
        genes.add(gene);
        this.p_labels.add("XDIV-RS0");
        
        gene = IGene.F;
        if(this.p_ema60Ind.lastValue().getValue() > this.p_ema60Ind.valueBack(this.p_memory).getValue() &&
        		this.p_ema60Ind.valueBack(1).getValue() <= this.p_ema60Ind.valueBack(this.p_memory).getValue() &&
        		this.p_asset.lastValue().getHigh() > this.p_max1HInd.valueBack(1).getValue().getValue()){
        	gene = IGene.D;
        }
        if(this.p_ema60Ind.lastValue().getValue() < this.p_ema60Ind.valueBack(this.p_memory).getValue() &&
        		this.p_ema60Ind.valueBack(1).getValue() >= this.p_ema60Ind.valueBack(this.p_memory).getValue() &&
        		this.p_asset.lastValue().getLow() < this.p_max1HInd.valueBack(1).getValue().getKey()){
        	gene = IGene.U;
        }
        genes.add(gene);
        this.p_labels.add("XDIV-MA0");
        
        gene = IGene.F;
        if(this.p_asset.lastValue().getClose() > (this.p_emsdIndS.lastValue().getValue().getKey() + 3 * this.p_emsdIndS.lastValue().getValue().getValue()) &&
        		this.p_emsdIndS.lastValue().getValue().getKey() > this.p_emsdIndS.valueBack(this.p_memory).getValue().getKey()){
        	gene = IGene.U;
        }
        if(this.p_asset.lastValue().getClose() < (this.p_emsdIndS.lastValue().getValue().getKey() - 3 * this.p_emsdIndS.lastValue().getValue().getValue()) &&
        		this.p_emsdIndS.lastValue().getValue().getKey() < this.p_emsdIndS.valueBack(this.p_memory).getValue().getKey()){
        	gene = IGene.D;
        }
        genes.add(gene);
        this.p_labels.add("EMSD-0");
        
        gene = IGene.F;
        if(this.p_asset.lastValue().getHigh() > this.p_max8HInd.valueBack(1).getValue().getValue() &&
        		this.p_max8HInd.lastValue().getValue().getValue() >= this.p_max8HInd.valueBack(this.p_memory).getValue().getValue()){
        	gene = IGene.D;
        }
        if(this.p_asset.lastValue().getLow() < this.p_max8HInd.valueBack(1).getValue().getKey() &&
        		this.p_max8HInd.lastValue().getValue().getKey() <= this.p_max8HInd.valueBack(this.p_memory).getValue().getKey()){
        	gene = IGene.U;
        }
        genes.add(gene);
        this.p_labels.add("MMX-12");
        
        gene = IGene.F;
        if(this.p_asset.lastValue().getClose() < (this.p_emsdIndL.lastValue().getValue().getKey() - 3 * this.p_emsdIndL.lastValue().getValue().getValue()) &&
        		this.p_asset.valueBack(1).getClose() >= (this.p_emsdIndL.valueBack(1).getValue().getKey() - 3 * this.p_emsdIndL.valueBack(1).getValue().getValue())){
        	gene = IGene.U;
        }
        if(this.p_asset.lastValue().getClose() > (this.p_emsdIndL.lastValue().getValue().getKey() + 3 * this.p_emsdIndL.lastValue().getValue().getValue()) &&
        		this.p_asset.valueBack(1).getClose() <= (this.p_emsdIndL.valueBack(1).getValue().getKey() + 3 * this.p_emsdIndL.valueBack(1).getValue().getValue())){
        	gene = IGene.D;
        }
        genes.add(gene);
        this.p_labels.add("EMSD-1");
        
        gene = IGene.F;
        if(this.p_asset.lastValue().getClose() < this.p_ema48Ind.lastValue().getValue() - emaLevel * atr &&
        		this.p_asset.valueBack(1).getClose() >= this.p_ema48Ind.valueBack(1).getValue() - emaLevel * atr &&
        		this.p_rsiIndS.lastValue().getValue() < this.p_rsiIndL.lastValue().getValue() &&
        		this.p_rsiIndS.valueBack(1).getValue() >= this.p_rsiIndL.valueBack(1).getValue()
                && this.p_rsiIndS.lastValue().getValue() < this.p_rsiIndS.valueBack(this.p_memory).getValue()){
        	gene = IGene.U;
        }
        if(this.p_asset.lastValue().getClose() > this.p_ema48Ind.lastValue().getValue() + emaLevel * atr &&
        		this.p_asset.valueBack(1).getClose() <= this.p_ema48Ind.valueBack(1).getValue() + emaLevel * atr &&
        		this.p_rsiIndS.lastValue().getValue() > this.p_rsiIndL.lastValue().getValue() &&
        		this.p_rsiIndS.valueBack(1).getValue() <= this.p_rsiIndL.valueBack(1).getValue()
                && this.p_rsiIndS.lastValue().getValue() > this.p_rsiIndS.valueBack(this.p_memory).getValue()){
        	gene = IGene.D;
        }
        genes.add(gene);        
        this.p_labels.add("RSI-3");

    	gene = IGene.F;
        if(this.p_max1WInd.lastValue().getValue().getValue() > this.p_max1WInd.valueBack(this.p_memory).getValue().getValue() &&
        		this.p_max1WInd.lastValue().getValue().getKey() >= this.p_max1WInd.valueBack(this.p_memory).getValue().getKey()){
        	gene = IGene.D;
        }
        if(this.p_max1WInd.lastValue().getValue().getValue() <= this.p_max1WInd.valueBack(this.p_memory).getValue().getValue() &&
        		this.p_max1WInd.lastValue().getValue().getKey() < this.p_max1WInd.valueBack(this.p_memory).getValue().getKey()){
        	gene = IGene.U;
        }
        genes.add(gene);
        this.p_labels.add("MMX-9");
        
        gene = IGene.F;
        if(this.p_rsiIndL.lastValue().getValue() <= this.p_rsiLMx1WInd.valueBack(1).getValue().getKey() &&
        		this.p_macdInd.lastValue().getValue().getValue() > this.p_rsiLMx1WInd.lastValue().getValue().getKey()){
        	gene = IGene.U;
        }
        if(this.p_rsiIndL.lastValue().getValue() >= this.p_rsiLMx1WInd.valueBack(1).getValue().getValue() &&
        		this.p_macdInd.lastValue().getValue().getValue() < this.p_rsiLMx1WInd.lastValue().getValue().getValue()){
        	gene = IGene.D;
        }
        genes.add(gene);
        this.p_labels.add("DIVMR-9");
		
        gene = IGene.F;
        if(this.p_ocsIndS.lastValue().getValue() > -ocsLevel && this.p_ocsIndS.valueBack(1).getValue() <= -ocsLevel){
        	gene = IGene.D;
        }
        if(this.p_ocsIndS.lastValue().getValue() < ocsLevel && this.p_ocsIndS.valueBack(1).getValue() >= ocsLevel){
        	gene = IGene.U;
        }
        genes.add(gene);
        this.p_labels.add("OCS-3");
        
    	gene = IGene.F;
        if(this.p_asset.lastValue().getLow() <= this.p_max1DInd.valueBack(1).getValue().getKey() &&
        		this.p_rsiIndS.lastValue().getValue() > this.p_rsiSMx1DInd.lastValue().getValue().getKey()){
        	gene = IGene.U;
        }
        if(this.p_asset.lastValue().getHigh() >= this.p_max1DInd.valueBack(1).getValue().getValue() &&
        		this.p_rsiIndS.lastValue().getValue() < this.p_rsiSMx1DInd.lastValue().getValue().getValue()){
        	gene = IGene.D;
        }
        genes.add(gene);
        this.p_labels.add("DIVRS-3");
        
        gene = IGene.F;
        if(this.p_asset.lastValue().getLow() <= this.p_max4HInd.valueBack(1).getValue().getKey() &&
        		this.p_macdInd.lastValue().getValue().getValue() > this.p_macdMx4HInd.lastValue().getValue().getKey() &&
        		!(this.p_asset.lastValue().getLow() <= this.p_max1HInd.valueBack(1).getValue().getKey() &&
                this.p_adpIndS.lastValue().getValue().getKey() > this.p_adpSMx1HInd.lastValue().getValue().getKey())){
        	gene = IGene.U;
        }
        if(this.p_asset.lastValue().getHigh() >= this.p_max4HInd.valueBack(1).getValue().getValue() &&
        		this.p_macdInd.lastValue().getValue().getValue() < this.p_macdMx4HInd.lastValue().getValue().getValue() &&
        		!(this.p_asset.lastValue().getHigh() >= this.p_max1HInd.valueBack(1).getValue().getValue() &&
                this.p_adpIndS.lastValue().getValue().getKey() < this.p_adpSMx1HInd.lastValue().getValue().getValue())){
        	gene = IGene.D;
        }
        genes.add(gene);
        this.p_labels.add("XDIV-ST1");
        
        gene = IGene.F;
		if (((maxData[this.p_memory - 1] > maxData[0]) && (minData[this.p_memory - 1] > minData[0]))
				&& this.p_ema12Ind.lastValue().getValue() > this.p_ema24Ind.lastValue().getValue()
				&& this.p_ema12Ind.valueBack(1).getValue() <= this.p_ema24Ind.valueBack(1).getValue()) {
			gene = IGene.D;
		} else if (((minData[this.p_memory - 1] < minData[0]) && (maxData[this.p_memory - 1] < maxData[0])
				&& this.p_ema12Ind.lastValue().getValue() < this.p_ema24Ind.lastValue().getValue()
				&& this.p_ema12Ind.valueBack(1).getValue() >= this.p_ema24Ind.valueBack(1).getValue())) {
			gene = IGene.U;
		}
		genes.add(gene);
		this.p_labels.add("MOM-1");
		
        gene = IGene.F;
        if(this.p_asset.lastValue().getClose() > this.p_max1WInd.lastValue().getValue().getKey() * (1 + maxLevel[4]) &&
        		this.p_asset.lastValue().getHigh() >= this.p_max1HInd.valueBack(1).getValue().getValue() &&
        		this.p_macdInd.lastValue().getValue().getValue() < this.p_macdMx1HInd.lastValue().getValue().getValue()){
        	gene = IGene.D;
        }
        if(this.p_asset.lastValue().getClose() < this.p_max1WInd.lastValue().getValue().getValue() * (1 - maxLevel[4]) &&
        		this.p_asset.lastValue().getLow() <= this.p_max1HInd.valueBack(1).getValue().getKey() &&
        		this.p_macdInd.lastValue().getValue().getValue() > this.p_macdMx1HInd.lastValue().getValue().getKey()){
        	gene = IGene.U;
        }
        genes.add(gene);
        this.p_labels.add("XDIV-CD0");
        
        
        /*************************************************BASIC*************************************************/
        gene = IGene.F;
        if((this.p_ema12Ind.lastValue().getValue() - this.p_ema12Ind.valueBack(this.p_memory).getValue())/this.p_ema12Ind.valueBack(this.p_memory).getValue() < -rocLevel){
        	gene = IGene.U;
        }
        if((this.p_ema12Ind.lastValue().getValue() - this.p_ema12Ind.valueBack(this.p_memory).getValue())/this.p_ema12Ind.valueBack(this.p_memory).getValue() > rocLevel){
        	gene = IGene.D;
        }
        genes.add(gene);
        this.p_labels.add("ROC-3");
		
    	gene = IGene.F;
        if((this.p_ema24Ind.lastValue().getValue() - this.p_ema24Ind.valueBack(this.p_memory).getValue())/this.p_ema24Ind.valueBack(this.p_memory).getValue() < -rocLevel){
        	gene = IGene.U;
        }
        if((this.p_ema24Ind.lastValue().getValue() - this.p_ema24Ind.valueBack(this.p_memory).getValue())/this.p_ema24Ind.valueBack(this.p_memory).getValue() > rocLevel){
        	gene = IGene.D;
        }
        genes.add(gene);
        this.p_labels.add("ROC-4"); 		
        
        gene = IGene.F;
        if((this.p_ema36Ind.lastValue().getValue() - this.p_ema36Ind.valueBack(this.p_memory).getValue())/this.p_ema36Ind.valueBack(this.p_memory).getValue() < -rocLevel){
        	gene = IGene.U;
        }
        if((this.p_ema36Ind.lastValue().getValue() - this.p_ema36Ind.valueBack(this.p_memory).getValue())/this.p_ema36Ind.valueBack(this.p_memory).getValue() > rocLevel){
        	gene = IGene.D;
        }
        genes.add(gene);
        this.p_labels.add("ROC-5");
        
        gene = IGene.F;
        if((this.p_ema48Ind.lastValue().getValue() - this.p_ema48Ind.valueBack(this.p_memory).getValue())/this.p_ema48Ind.valueBack(this.p_memory).getValue() < -rocLevel){
        	gene = IGene.U;
        }
        if((this.p_ema48Ind.lastValue().getValue() - this.p_ema48Ind.valueBack(this.p_memory).getValue())/this.p_ema48Ind.valueBack(this.p_memory).getValue() > rocLevel){
        	gene = IGene.D;
        }
        genes.add(gene);
        this.p_labels.add("ROC-6");
        
        gene = IGene.F;
        if((this.p_ema60Ind.lastValue().getValue() - this.p_ema60Ind.valueBack(this.p_memory).getValue())/this.p_ema60Ind.valueBack(this.p_memory).getValue() < -rocLevel){
        	gene = IGene.U;
        }
        if((this.p_ema60Ind.lastValue().getValue() - this.p_ema60Ind.valueBack(this.p_memory).getValue())/this.p_ema60Ind.valueBack(this.p_memory).getValue() > rocLevel){
        	gene = IGene.D;
        }
        genes.add(gene);
        this.p_labels.add("ROC-7");   
        
        gene = IGene.F;
        if(this.p_asset.lastValue().getClose() > (this.p_emsdIndL.lastValue().getValue().getKey() + 3 * this.p_emsdIndL.lastValue().getValue().getValue()) &&
        		this.p_rsiIndS.lastValue().getValue() > this.p_rsiIndS.valueBack(this.p_memory).getValue()){
        	gene = IGene.D;
        }
        if(this.p_asset.lastValue().getClose() < (this.p_emsdIndL.lastValue().getValue().getKey() - 3 * this.p_emsdIndL.lastValue().getValue().getValue()) &&
        		this.p_rsiIndS.lastValue().getValue() < this.p_rsiIndS.valueBack(this.p_memory).getValue()){
        	gene = IGene.U;
        }
        genes.add(gene); 
        this.p_labels.add("EMSD-6");
        
        gene = IGene.F;
        if(this.p_ema24Ind.lastValue().getValue() > this.p_ema24Ind.valueBack(this.p_memory).getValue() &&
        		this.p_ema36Ind.valueBack(1).getValue() <= this.p_ema36Ind.valueBack(this.p_memory+1).getValue()){
        	gene = IGene.D;
        }
        if(this.p_ema24Ind.lastValue().getValue() < this.p_ema24Ind.valueBack(this.p_memory).getValue() &&
        		this.p_ema24Ind.valueBack(1).getValue() >= this.p_ema24Ind.valueBack(this.p_memory+1).getValue()){
        	gene = IGene.U;
        }
        genes.add(gene);
        this.p_labels.add("ROC-1");
        
        gene = IGene.F;
        if(this.p_ema36Ind.lastValue().getValue() > this.p_ema36Ind.valueBack(this.p_memory).getValue() &&
        		this.p_ema36Ind.valueBack(1).getValue() <= this.p_ema36Ind.valueBack(this.p_memory+1).getValue()){
        	gene = IGene.D;
        }
        if(this.p_ema36Ind.lastValue().getValue() < this.p_ema36Ind.valueBack(this.p_memory).getValue() &&
        		this.p_ema36Ind.valueBack(1).getValue() >= this.p_ema36Ind.valueBack(this.p_memory+1).getValue()){
        	gene = IGene.U;
        }
        genes.add(gene);
        this.p_labels.add("ROC-2");

        gene = IGene.F;
        if(this.p_asset.lastValue().getClose() > this.p_ema12Ind.lastValue().getValue() &&
        		this.p_asset.valueBack(1).getClose() <= this.p_ema12Ind.valueBack(1).getValue()){
        	gene = IGene.D;
        }
        if(this.p_asset.lastValue().getClose() < this.p_ema12Ind.lastValue().getValue() &&
        		this.p_asset.valueBack(1).getClose() >= this.p_ema12Ind.valueBack(1).getValue()){
        	gene = IGene.U;
        }
        genes.add(gene);
        this.p_labels.add("PRC-0");
        
        gene = IGene.F;
        if(this.p_asset.lastValue().getClose() > this.p_ema24Ind.lastValue().getValue() &&
        		this.p_asset.valueBack(1).getClose() <= this.p_ema24Ind.valueBack(1).getValue()){
        	gene = IGene.D;
        }
        if(this.p_asset.lastValue().getClose() < this.p_ema24Ind.lastValue().getValue() &&
        		this.p_asset.valueBack(1).getClose() >= this.p_ema24Ind.valueBack(1).getValue()){
        	gene = IGene.U;
        }
        genes.add(gene);
        this.p_labels.add("PRC-1");
        
        gene = IGene.F;
        if(this.p_asset.lastValue().getClose() > this.p_ema48Ind.lastValue().getValue() &&
        		this.p_asset.valueBack(1).getClose() <= this.p_ema48Ind.valueBack(1).getValue()){
        	gene = IGene.D;
        }
        if(this.p_asset.lastValue().getClose() < this.p_ema48Ind.lastValue().getValue() &&
        		this.p_asset.valueBack(1).getClose() >= this.p_ema48Ind.valueBack(1).getValue()){
        	gene = IGene.U;
        }
        genes.add(gene);
        this.p_labels.add("PRC-2");
        
        gene = IGene.F;
        if(this.p_asset.lastValue().getClose() > this.p_ema60Ind.lastValue().getValue() &&
        		this.p_asset.valueBack(1).getClose() <= this.p_ema60Ind.valueBack(1).getValue()){
        	gene = IGene.D;
        }
        if(this.p_asset.lastValue().getClose() < this.p_ema60Ind.lastValue().getValue() &&
        		this.p_asset.valueBack(1).getClose() >= this.p_ema60Ind.valueBack(1).getValue()){
        	gene = IGene.U;
        }
        genes.add(gene);
        this.p_labels.add("PRC-3");
        
        gene = IGene.F;
        if(this.p_asset.lastValue().getClose() < this.p_ema12Ind.lastValue().getValue()*(1 - priceLevel[0]) &&
        		this.p_asset.valueBack(1).getClose() >= this.p_ema12Ind.valueBack(1).getValue()*(1 - priceLevel[0])){
        	gene = IGene.U;
        }
        if(this.p_asset.lastValue().getClose() > this.p_ema12Ind.lastValue().getValue()*(1 + priceLevel[0]) &&
        		this.p_asset.valueBack(1).getClose() <= this.p_ema12Ind.valueBack(1).getValue()*(1 + priceLevel[0])){
        	gene = IGene.D;
        }
        genes.add(gene);
        this.p_labels.add("PRC-4");
        
        gene = IGene.F;
        if(this.p_asset.lastValue().getClose() < this.p_ema48Ind.lastValue().getValue()*(1 - priceLevel[3]) &&
        		this.p_asset.valueBack(1).getClose() >= this.p_ema48Ind.valueBack(1).getValue()*(1 - priceLevel[3])){
        	gene = IGene.U;
        }
        if(this.p_asset.lastValue().getClose() > this.p_ema48Ind.lastValue().getValue()*(1 + priceLevel[3]) &&
        		this.p_asset.valueBack(1).getClose() <= this.p_ema48Ind.valueBack(1).getValue()*(1 + priceLevel[3])){
        	gene = IGene.D;
        }
        genes.add(gene);
        this.p_labels.add("PRC-5");
        
        gene = IGene.F;//POS
        if(this.p_asset.lastValue().getClose() < this.p_ema60Ind.lastValue().getValue()*(1 - priceLevel[4]) &&
        		this.p_asset.valueBack(1).getClose() >= this.p_ema60Ind.valueBack(1).getValue()*(1 - priceLevel[4])){
        	gene = IGene.U;
        }
        if(this.p_asset.lastValue().getClose() > this.p_ema60Ind.lastValue().getValue()*(1 + priceLevel[4]) &&
        		this.p_asset.valueBack(1).getClose() <= this.p_ema60Ind.valueBack(1).getValue()*(1 + priceLevel[4])){
        	gene = IGene.D;
        }
        genes.add(gene);
        this.p_labels.add("PRC-6");

        double dscore0N = (this.p_ema12Ind.lastValue().getValue()-this.p_ema6Ind.lastValue().getValue())/this.p_ema6Ind.lastValue().getValue();
        double dscore0O = (this.p_ema12Ind.valueBack(1).getValue()-this.p_ema6Ind.valueBack(1).getValue())/this.p_ema6Ind.valueBack(1).getValue();
        double dscore1N = (this.p_ema24Ind.lastValue().getValue()-this.p_ema12Ind.lastValue().getValue())/this.p_ema12Ind.lastValue().getValue();
        double dscore1O = (this.p_ema24Ind.valueBack(1).getValue()-this.p_ema12Ind.valueBack(1).getValue())/this.p_ema12Ind.valueBack(1).getValue();
        
        gene = IGene.F;//POS
        if(dscore0N > dscore1N && dscore0O <= dscore1O){
        	gene = IGene.U;
        }
        if(dscore0N < dscore1N && dscore0O >= dscore1O){
        	gene = IGene.D;
        }
        genes.add(gene);
        this.p_labels.add("EMA-0");
        
        dscore0N = (this.p_ema24Ind.lastValue().getValue()-this.p_ema12Ind.lastValue().getValue())/this.p_ema12Ind.lastValue().getValue();
        dscore0O = (this.p_ema24Ind.valueBack(1).getValue()-this.p_ema12Ind.valueBack(1).getValue())/this.p_ema12Ind.valueBack(1).getValue();
        dscore1N = (this.p_ema36Ind.lastValue().getValue()-this.p_ema24Ind.lastValue().getValue())/this.p_ema24Ind.lastValue().getValue();
        dscore1O = (this.p_ema36Ind.valueBack(1).getValue()-this.p_ema24Ind.valueBack(1).getValue())/this.p_ema24Ind.valueBack(1).getValue();
        
        gene = IGene.F;//POS
        if(dscore0N > dscore1N && dscore0O <= dscore1O){
        	gene = IGene.U;
        }
        if(dscore0N < dscore1N && dscore0O >= dscore1O){
        	gene = IGene.D;
        }
        genes.add(gene);
        this.p_labels.add("EMA-1");
        
       
        gene = IGene.F;
        boolean pLong = true, pShort = true; 
        List<ITimedValue<Double>> emas = this.p_ema24Ind.valueList(this.p_memory);
        for(int k = 0; k < emas.size(); k++){
            if(prices.get(k).getClose() >= emas.get(k).getValue()){
                pShort = false;
            }else{
                pLong = false;
            }
        }
        if(pLong && (prices.get(prices.size() -1).getClose() - emas.get(emas.size() -1).getValue()) > 
        (prices.get(0).getClose() - emas.get(0).getValue())){
        	gene = IGene.D;
        }
        if(pShort && (prices.get(prices.size() -1).getClose() - emas.get(emas.size() -1).getValue()) < 
        (prices.get(0).getClose() - emas.get(0).getValue())){
        	gene = IGene.U;
        }    
        genes.add(gene);
        this.p_labels.add("UPDW-1");
        
        gene = IGene.F;
        pLong = true; pShort = true;
        emas = this.p_ema36Ind.valueList(this.p_memory);
        for(int k = 0; k < emas.size(); k++){
            if(prices.get(k).getClose() >= emas.get(k).getValue()){
                pShort = false;
            }else{
                pLong = false;
            }
        }
        if(pLong && (prices.get(prices.size() -1).getClose() - emas.get(emas.size() -1).getValue()) > 
        (prices.get(0).getClose() - emas.get(0).getValue())){
        	gene = IGene.D;
        }
        if(pShort && (prices.get(prices.size() -1).getClose() - emas.get(emas.size() -1).getValue()) < 
        (prices.get(0).getClose() - emas.get(0).getValue())){
        	gene = IGene.U;
        }    
        genes.add(gene);
        this.p_labels.add("UPDW-2");
        
        gene = IGene.F;
        pLong = true; pShort = true;
        emas = this.p_ema48Ind.valueList(this.p_memory);
        for(int k = 0; k < emas.size(); k++){
            if(prices.get(k).getClose() >= emas.get(k).getValue()){
                pShort = false;
            }else{
                pLong = false;
            }
        }
        if(pLong && (prices.get(prices.size() -1).getClose() - emas.get(emas.size() -1).getValue()) > 
        (prices.get(0).getClose() - emas.get(0).getValue())){
        	gene = IGene.D;
        }
        if(pShort && (prices.get(prices.size() -1).getClose() - emas.get(emas.size() -1).getValue()) < 
        (prices.get(0).getClose() - emas.get(0).getValue())){
        	gene = IGene.U;
        }    
        genes.add(gene);
        this.p_labels.add("UPDW-3");
        
        gene = IGene.F;
        pLong = true; pShort = true;
        emas = this.p_ema60Ind.valueList(this.p_memory);
        for(int k = 0; k < emas.size(); k++){
            if(prices.get(k).getClose() >= emas.get(k).getValue()){
                pShort = false;
            }else{
                pLong = false;
            }
        }
        if(pLong && (prices.get(prices.size() -1).getClose() - emas.get(emas.size() -1).getValue()) > 
        (prices.get(0).getClose() - emas.get(0).getValue())){
        	gene = IGene.D;
        }
        if(pShort && (prices.get(prices.size() -1).getClose() - emas.get(emas.size() -1).getValue()) < 
        (prices.get(0).getClose() - emas.get(0).getValue())){
        	gene = IGene.U;
        }    
        genes.add(gene);
        this.p_labels.add("UPDW-4");
        
        gene = IGene.F;
        if(this.p_rsiIndS.lastValue().getValue() <= this.p_rsiSMx1WInd.valueBack(1).getValue().getKey() &&
        		this.p_macdInd.lastValue().getValue().getValue() > this.p_macdMx1WInd.lastValue().getValue().getKey()){
        	gene = IGene.U;
        }
        if(this.p_rsiIndS.lastValue().getValue() >= this.p_rsiSMx1WInd.valueBack(1).getValue().getValue() &&
        		this.p_macdInd.lastValue().getValue().getValue() < this.p_macdMx1WInd.lastValue().getValue().getValue()){
        	gene = IGene.D;
        }
        genes.add(gene);
        this.p_labels.add("DIVMR-4");
        
        gene = IGene.F;
        if(this.p_rsiIndL.lastValue().getValue() <= this.p_rsiLMx1DInd.valueBack(1).getValue().getKey() &&
        		this.p_macdInd.lastValue().getValue().getValue() > this.p_rsiLMx1DInd.lastValue().getValue().getKey()){
        	gene = IGene.U;
        }
        if(this.p_rsiIndL.lastValue().getValue() >= this.p_rsiLMx1DInd.valueBack(1).getValue().getValue() &&
        		this.p_macdInd.lastValue().getValue().getValue() < this.p_rsiLMx1DInd.lastValue().getValue().getValue()){
        	gene = IGene.D;
        }
        genes.add(gene);
        this.p_labels.add("DIVMR-8");

		
		gene = IGene.F;
        if(this.p_rsiIndS.lastValue().getValue() > this.p_rsiIndL.lastValue().getValue() &&
        		this.p_rsiIndS.valueBack(1).getValue() <= this.p_rsiIndL.valueBack(1).getValue()){
        	gene = IGene.D;
        }
        if(this.p_rsiIndS.lastValue().getValue() < this.p_rsiIndL.lastValue().getValue() &&
        		this.p_rsiIndS.valueBack(1).getValue() >= this.p_rsiIndL.valueBack(1).getValue()){
        	gene = IGene.U;
        }
        genes.add(gene);
        this.p_labels.add("XRSI-0");
        
        gene = IGene.F;
        if(this.p_rsiIndS.lastValue().getValue() > this.p_rsiIndL.lastValue().getValue() &&
        		this.p_rsiIndS.valueBack(1).getValue() <= this.p_rsiIndL.valueBack(1).getValue() &&
        		this.p_asset.lastValue().getClose() < this.p_ema12Ind.lastValue().getValue() - emaLevel * atr){
        	gene = IGene.U;
        }
        if(this.p_rsiIndS.lastValue().getValue() < this.p_rsiIndL.lastValue().getValue() &&
        		this.p_rsiIndS.valueBack(1).getValue() >= this.p_rsiIndL.valueBack(1).getValue() &&
        		this.p_asset.lastValue().getClose() > this.p_ema12Ind.lastValue().getValue() + emaLevel * atr){
        	gene = IGene.D;
        }
        genes.add(gene);
        this.p_labels.add("RSI-2");

        gene = IGene.F;
		if(this.p_stoIndL.lastValue().getValue() > rsiLevel &&
				this.p_stoIndL.valueBack(1).getValue() <= rsiLevel){
			gene = IGene.D;
		}
		if(this.p_stoIndL.lastValue().getValue() < 100-rsiLevel &&
				this.p_stoIndL.valueBack(1).getValue() >= 100-rsiLevel){
			gene = IGene.U;
		}
		genes.add(gene);
		this.p_labels.add("STO-1");
        
		gene = IGene.F;
        if(this.p_asset.lastValue().getClose() < (this.p_emsdIndS.lastValue().getValue().getKey() - 2 * this.p_emsdIndS.lastValue().getValue().getValue())){
        	gene = IGene.U;
        }
        if(this.p_asset.lastValue().getClose() > (this.p_emsdIndS.lastValue().getValue().getKey() + 2 * this.p_emsdIndS.lastValue().getValue().getValue())){
        	gene = IGene.D;
        }
        genes.add(gene);
        this.p_labels.add("EMSD-3");
        
        gene = IGene.F;//POS
        if(this.p_asset.lastValue().getClose() < (this.p_emsdIndL.lastValue().getValue().getKey() - 2 * this.p_emsdIndL.lastValue().getValue().getValue())){
        	gene = IGene.U;
        }
        if(this.p_asset.lastValue().getClose() > (this.p_emsdIndL.lastValue().getValue().getKey() + 2 * this.p_emsdIndL.lastValue().getValue().getValue())){
        	gene = IGene.D;
        }
        genes.add(gene);
        this.p_labels.add("EMSD-4");
        
        gene = IGene.F;
		if (this.p_adpIndS.lastValue().getValue().getKey() > this.p_adpIndS.lastValue().getValue().getValue()) {
			gene = IGene.D;
		}
		if (this.p_adpIndS.lastValue().getValue().getKey() < this.p_adpIndS.lastValue().getValue().getValue()) {
			gene = IGene.U;
		}
		genes.add(gene); 
		this.p_labels.add("ADP-0");
		
        gene = IGene.F;
		if (this.p_adpIndS.lastValue().getValue().getValue() > this.p_adpIndL.lastValue().getValue().getValue() &&
				this.p_adpIndS.valueBack(1).getValue().getValue() <= this.p_adpIndL.valueBack(1).getValue().getValue()) {
			gene = IGene.U;
		}
		if (this.p_adpIndS.lastValue().getValue().getValue() < this.p_adpIndL.lastValue().getValue().getValue() &&
				this.p_adpIndS.valueBack(1).getValue().getValue() >= this.p_adpIndL.valueBack(1).getValue().getValue()) {
			gene = IGene.D;
		}
		genes.add(gene); 
		this.p_labels.add("ADP-1"); 
        
		gene = IGene.F;
		if(this.p_udsIndS.lastValue().getValue().getKey() < udsLevel[0]){
			gene = IGene.D;
		}
		if(this.p_udsIndS.lastValue().getValue().getKey() > 100.-udsLevel[0]){
			gene = IGene.U;
		}
		genes.add(gene);
		this.p_labels.add("UDS-0");
		
		gene = IGene.F;
		if(this.p_udsIndL.lastValue().getValue().getKey() > (2.-udsLevel[1]) * this.p_udsIndL.lastValue().getValue().getValue()
				&& this.p_udsIndL.valueBack(1).getValue().getKey() <= (2.-udsLevel[1]) * this.p_udsIndL.valueBack(1).getValue().getValue()){
			gene = IGene.U;
		}
		if(this.p_udsIndL.lastValue().getValue().getKey() < udsLevel[1] * this.p_udsIndL.lastValue().getValue().getValue()
				&& this.p_udsIndL.valueBack(1).getValue().getKey() >= udsLevel[1] * this.p_udsIndL.valueBack(1).getValue().getValue()){
			gene = IGene.D;
		}
		genes.add(gene);
		this.p_labels.add("UDS-4");
		   
        gene = IGene.F;
        if(this.p_ocsIndS.lastValue().getValue() > 0. &&
        		this.p_ocsIndS.lastValue().getValue() >= this.p_ocsIndS.valueBack(this.p_memory).getValue()){
        	gene = IGene.D;
        }
        if(this.p_ocsIndS.lastValue().getValue() < 0. &&
        		this.p_ocsIndS.lastValue().getValue() <= this.p_ocsIndS.valueBack(this.p_memory).getValue()){
        	gene = IGene.U;
        }
        genes.add(gene);
        this.p_labels.add("OCS-1");
          
        gene = IGene.F;
        if(this.p_hls36Ind.lastValue().getValue().getKey() > this.p_hls36Ind.lastValue().getValue().getValue() &&
        		this.p_hls36Ind.valueBack(1).getValue().getKey() <= this.p_hls36Ind.valueBack(1).getValue().getValue()){
        	gene = IGene.D;
        }
        if(this.p_hls36Ind.lastValue().getValue().getKey() < this.p_hls36Ind.lastValue().getValue().getValue() &&
        		this.p_hls36Ind.valueBack(1).getValue().getKey() >= this.p_hls36Ind.valueBack(1).getValue().getValue()){
        	gene = IGene.U;
        }
        genes.add(gene);
        this.p_labels.add("HLS-2");
		
        gene = IGene.F;
        if(this.p_cciIndL.lastValue().getValue() > cciLevel && this.p_cciIndL.valueBack(1).getValue() <= cciLevel){
        	gene = IGene.D;
        }
        if(this.p_cciIndL.lastValue().getValue() < -cciLevel && this.p_cciIndL.valueBack(1).getValue() >= -cciLevel){
        	gene = IGene.U;
        }
        genes.add(gene);
        this.p_labels.add("CCI-1");
    
    	gene = IGene.F;
        if(this.p_max8HInd.lastValue().getValue().getValue() > this.p_max8HInd.valueBack(this.p_memory).getValue().getValue() &&
        		this.p_max8HInd.lastValue().getValue().getKey() >= this.p_max8HInd.valueBack(this.p_memory).getValue().getKey()){
        	gene = IGene.D;
        }
        if(this.p_max8HInd.lastValue().getValue().getValue() <= this.p_max8HInd.valueBack(this.p_memory).getValue().getValue() &&
        		this.p_max8HInd.lastValue().getValue().getKey() < this.p_max8HInd.valueBack(this.p_memory).getValue().getKey()){
        	gene = IGene.U;
        }
        genes.add(gene);
        this.p_labels.add("MMX-7");
        
    	gene = IGene.F;
        if(this.p_asset.lastValue().getHigh() > this.p_max4HInd.valueBack(1).getValue().getValue() &&
        		this.p_max4HInd.lastValue().getValue().getValue() >= this.p_max4HInd.valueBack(this.p_memory).getValue().getValue()){
        	gene = IGene.D;
        }
        if(this.p_asset.lastValue().getLow() < this.p_max4HInd.valueBack(1).getValue().getKey() &&
        		this.p_max4HInd.lastValue().getValue().getKey() <= this.p_max4HInd.valueBack(this.p_memory).getValue().getKey()){
        	gene = IGene.U;
        }
        genes.add(gene);
        this.p_labels.add("MMX-11");
        
        gene = IGene.F;
        if(this.p_asset.lastValue().getHigh() > this.p_max1WInd.valueBack(1).getValue().getValue() &&
        		this.p_max1WInd.lastValue().getValue().getValue() >= this.p_max1WInd.valueBack(this.p_memory).getValue().getValue()){
        	gene = IGene.D;
        }
        if(this.p_asset.lastValue().getLow() < this.p_max1WInd.valueBack(1).getValue().getKey() &&
        		this.p_max1WInd.lastValue().getValue().getKey() <= this.p_max1WInd.valueBack(this.p_memory).getValue().getKey()){
        	gene = IGene.U;
        }
        genes.add(gene);
        this.p_labels.add("MMX-14");
		
        gene = IGene.F;
        if(this.p_max4HInd.lastValue().getValue().getValue() > this.p_max4HInd.valueBack(1).getValue().getValue() &&
        		this.p_max4HInd.valueBack(1).getValue().getValue() <= this.p_max4HInd.valueBack(2).getValue().getValue() &&
        		this.p_max4HInd.lastValue().getValue().getKey() >= this.p_max4HInd.valueBack(1).getValue().getKey()){
        	gene = IGene.D;
        }
        if(this.p_max4HInd.lastValue().getValue().getKey() < this.p_max4HInd.valueBack(1).getValue().getKey() &&
        		this.p_max4HInd.valueBack(1).getValue().getKey() >= this.p_max4HInd.valueBack(2).getValue().getKey() &&
        		this.p_max4HInd.lastValue().getValue().getValue() <= this.p_max4HInd.valueBack(1).getValue().getValue()){
        	gene = IGene.U;
        }
        genes.add(gene);
        this.p_labels.add("MMX-16");
        
    	gene = IGene.F;
        if(this.p_max8HInd.lastValue().getValue().getValue() > this.p_max8HInd.valueBack(1).getValue().getValue() &&
        		this.p_max8HInd.valueBack(1).getValue().getValue() <= this.p_max8HInd.valueBack(2).getValue().getValue() &&
        		this.p_max8HInd.lastValue().getValue().getKey() >= this.p_max8HInd.valueBack(1).getValue().getKey()){
        	gene = IGene.D;
        }
        if(this.p_max8HInd.lastValue().getValue().getKey() < this.p_max8HInd.valueBack(1).getValue().getKey() &&
        		this.p_max8HInd.valueBack(1).getValue().getKey() >= this.p_max8HInd.valueBack(2).getValue().getKey() &&
        		this.p_max8HInd.lastValue().getValue().getValue() <= this.p_max8HInd.valueBack(1).getValue().getValue()){
        	gene = IGene.U;
        }
        genes.add(gene);
        this.p_labels.add("MMX-17");
        
    	gene = IGene.F;
        if(this.p_max1DInd.lastValue().getValue().getValue() > this.p_max1DInd.valueBack(1).getValue().getValue() &&
        		this.p_max1DInd.valueBack(1).getValue().getValue() <= this.p_max1DInd.valueBack(2).getValue().getValue() &&
        		this.p_max1DInd.lastValue().getValue().getKey() >= this.p_max1DInd.valueBack(1).getValue().getKey()){
        	gene = IGene.D;
        }
        if(this.p_max1DInd.lastValue().getValue().getKey() < this.p_max1DInd.valueBack(1).getValue().getKey() &&
        		this.p_max1DInd.valueBack(1).getValue().getKey() >= this.p_max1DInd.valueBack(2).getValue().getKey() &&
        		this.p_max1DInd.lastValue().getValue().getValue() <= this.p_max1DInd.valueBack(1).getValue().getValue()){
        	gene = IGene.U;
        }
        genes.add(gene);
        this.p_labels.add("MMX-18");
        
    	gene = IGene.F;
        if(this.p_max1WInd.lastValue().getValue().getValue() > this.p_max1WInd.valueBack(1).getValue().getValue() &&
        		this.p_max1WInd.valueBack(1).getValue().getValue() <= this.p_max1WInd.valueBack(2).getValue().getValue() &&
        		this.p_max1WInd.lastValue().getValue().getKey() >= this.p_max1WInd.valueBack(1).getValue().getKey()){
        	gene = IGene.D;
        }
        if(this.p_max1WInd.lastValue().getValue().getKey() < this.p_max1WInd.valueBack(1).getValue().getKey() &&
        		this.p_max1WInd.valueBack(1).getValue().getKey() >= this.p_max1WInd.valueBack(2).getValue().getKey() &&
        		this.p_max1WInd.lastValue().getValue().getValue() <= this.p_max1WInd.valueBack(1).getValue().getValue()){
        	gene = IGene.U;
        }
        genes.add(gene);
        this.p_labels.add("MMX-19");
		
        gene = IGene.F;
        if(this.p_max1HInd.lastValue().getValue().getValue() > this.p_max1HInd.valueBack(3).getValue().getValue() &&
        		this.p_max1HInd.valueBack(3).getValue().getValue() > this.p_max1HInd.valueBack(6).getValue().getValue() &&
        		this.p_max1HInd.lastValue().getValue().getKey() >= this.p_max1HInd.valueBack(2).getValue().getKey()&&
        		this.p_max1HInd.valueBack(2).getValue().getKey() >= this.p_max1HInd.valueBack(6).getValue().getKey()){
        	gene = IGene.D;
        }
        if(this.p_max1HInd.lastValue().getValue().getValue() <= this.p_max1HInd.valueBack(3).getValue().getValue() &&
        		this.p_max1HInd.valueBack(3).getValue().getValue() <= this.p_max1HInd.valueBack(6).getValue().getValue() &&
        		this.p_max1HInd.lastValue().getValue().getKey() < this.p_max1HInd.valueBack(3).getValue().getKey() &&
        		this.p_max1HInd.valueBack(3).getValue().getKey() < this.p_max1HInd.valueBack(6).getValue().getKey()){
        	gene = IGene.U;
        }
        genes.add(gene);
        this.p_labels.add("MMX-20");
        
    	gene = IGene.F;
        if(this.p_max4HInd.lastValue().getValue().getValue() > this.p_max4HInd.valueBack(3).getValue().getValue() &&
        		this.p_max4HInd.valueBack(3).getValue().getValue() > this.p_max4HInd.valueBack(6).getValue().getValue() &&
        		this.p_max4HInd.lastValue().getValue().getKey() >= this.p_max4HInd.valueBack(2).getValue().getKey()&&
        		this.p_max4HInd.valueBack(2).getValue().getKey() >= this.p_max4HInd.valueBack(6).getValue().getKey()){
        	gene = IGene.D;
        }
        if(this.p_max4HInd.lastValue().getValue().getValue() <= this.p_max4HInd.valueBack(3).getValue().getValue() &&
        		this.p_max4HInd.valueBack(3).getValue().getValue() <= this.p_max4HInd.valueBack(6).getValue().getValue() &&
        		this.p_max4HInd.lastValue().getValue().getKey() < this.p_max4HInd.valueBack(3).getValue().getKey() &&
        		this.p_max4HInd.valueBack(3).getValue().getKey() < this.p_max4HInd.valueBack(6).getValue().getKey()){
        	gene = IGene.U;
        }
        genes.add(gene);
        this.p_labels.add("MMX-21");
        
    	gene = IGene.F;
        if(this.p_max8HInd.lastValue().getValue().getValue() > this.p_max8HInd.valueBack(3).getValue().getValue() &&
        		this.p_max8HInd.valueBack(3).getValue().getValue() > this.p_max8HInd.valueBack(6).getValue().getValue() &&
        		this.p_max8HInd.lastValue().getValue().getKey() >= this.p_max8HInd.valueBack(2).getValue().getKey()&&
        		this.p_max8HInd.valueBack(2).getValue().getKey() >= this.p_max8HInd.valueBack(6).getValue().getKey()){
        	gene = IGene.D;
        }
        if(this.p_max8HInd.lastValue().getValue().getValue() <= this.p_max8HInd.valueBack(3).getValue().getValue() &&
        		this.p_max8HInd.valueBack(3).getValue().getValue() <= this.p_max8HInd.valueBack(6).getValue().getValue() &&
        		this.p_max8HInd.lastValue().getValue().getKey() < this.p_max8HInd.valueBack(3).getValue().getKey() &&
        		this.p_max8HInd.valueBack(3).getValue().getKey() < this.p_max8HInd.valueBack(6).getValue().getKey()){
        	gene = IGene.U;
        }
        genes.add(gene);
        this.p_labels.add("MMX-22");
		
        gene = IGene.F;
        if(this.p_max8HInd.lastValue().getValue().getValue() > this.p_max8HInd.valueBack(1).getValue().getValue() &&
        		this.p_max8HInd.valueBack(1).getValue().getValue() <= this.p_max8HInd.valueBack(2).getValue().getValue() &&
        		this.p_max8HInd.lastValue().getValue().getKey() >= this.p_max8HInd.valueBack(1).getValue().getKey() &&
        		this.p_max8HInd.valueBack(1).getValue().getKey() <= this.p_max8HInd.valueBack(2).getValue().getKey()){
        	gene = IGene.D;
        }
        if(this.p_max8HInd.lastValue().getValue().getKey() < this.p_max8HInd.valueBack(1).getValue().getKey() &&
        		this.p_max8HInd.valueBack(1).getValue().getKey() >= this.p_max8HInd.valueBack(2).getValue().getKey() &&
        		this.p_max8HInd.lastValue().getValue().getValue() <= this.p_max8HInd.valueBack(1).getValue().getValue() &&
        		this.p_max8HInd.valueBack(1).getValue().getValue() >= this.p_max8HInd.valueBack(2).getValue().getValue()){
        	gene = IGene.U;
        }
        genes.add(gene);
        this.p_labels.add("MMX-25");
        
        gene = IGene.F;
        if(this.p_max1DInd.lastValue().getValue().getValue() > this.p_max1DInd.valueBack(1).getValue().getValue() &&
        		this.p_max1DInd.valueBack(1).getValue().getValue() <= this.p_max1DInd.valueBack(2).getValue().getValue() &&
        		this.p_max1DInd.lastValue().getValue().getKey() >= this.p_max1DInd.valueBack(1).getValue().getKey() &&
        		this.p_max1DInd.valueBack(1).getValue().getKey() <= this.p_max1DInd.valueBack(2).getValue().getKey()){
        	gene = IGene.D;
        }
        if(this.p_max1DInd.lastValue().getValue().getKey() < this.p_max1DInd.valueBack(1).getValue().getKey() &&
        		this.p_max1DInd.valueBack(1).getValue().getKey() >= this.p_max1DInd.valueBack(2).getValue().getKey() &&
        		this.p_max1DInd.lastValue().getValue().getValue() <= this.p_max1DInd.valueBack(1).getValue().getValue() &&
        		this.p_max1DInd.valueBack(1).getValue().getValue() >= this.p_max1DInd.valueBack(2).getValue().getValue()){
        	gene = IGene.U;
        }
        genes.add(gene);
        this.p_labels.add("MMX-26");
        
        gene = IGene.F;
        if(this.p_max1WInd.lastValue().getValue().getValue() > this.p_max1WInd.valueBack(1).getValue().getValue() &&
        		this.p_max1WInd.valueBack(1).getValue().getValue() <= this.p_max1WInd.valueBack(2).getValue().getValue() &&
        		this.p_max1WInd.lastValue().getValue().getKey() >= this.p_max1WInd.valueBack(1).getValue().getKey() &&
        		this.p_max1WInd.valueBack(1).getValue().getKey() <= this.p_max1WInd.valueBack(2).getValue().getKey()){
        	gene = IGene.D;
        }
        if(this.p_max1WInd.lastValue().getValue().getKey() < this.p_max1WInd.valueBack(1).getValue().getKey() &&
        		this.p_max1WInd.valueBack(1).getValue().getKey() >= this.p_max1WInd.valueBack(2).getValue().getKey() &&
        		this.p_max1WInd.lastValue().getValue().getValue() <= this.p_max1WInd.valueBack(1).getValue().getValue() &&
        		this.p_max1WInd.valueBack(1).getValue().getValue() >= this.p_max1WInd.valueBack(2).getValue().getValue()){
        	gene = IGene.U;
        }
        genes.add(gene);
        this.p_labels.add("MMX-27");
        
        gene = IGene.F;
        if(this.p_asset.lastValue().getClose() > this.p_max1HInd.lastValue().getValue().getKey() * (1 + maxLevel[0])){
        	gene = IGene.D;
        }
        if(this.p_asset.lastValue().getClose() < this.p_max1HInd.lastValue().getValue().getValue() * (1 - maxLevel[0])){
        	gene = IGene.U;
        }
        genes.add(gene);
        this.p_labels.add("MMX-28");
        
    	gene = IGene.F;
        if(this.p_asset.lastValue().getClose() > this.p_max4HInd.lastValue().getValue().getKey() * (1 + maxLevel[1])){
        	gene = IGene.D;
        }
        if(this.p_asset.lastValue().getClose() < this.p_max4HInd.lastValue().getValue().getValue() * (1 - maxLevel[1])){
        	gene = IGene.U;
        }
        genes.add(gene);
        this.p_labels.add("MMX-29");
        
        gene = IGene.F;
        if(this.p_rsiIndS.lastValue().getValue() <= this.p_rsiSMx1HInd.valueBack(1).getValue().getKey() &&
        		this.p_ema12Ind.lastValue().getValue() > this.p_ema12Mx1HInd.valueBack(1).getValue().getKey()){
        	gene = IGene.U;
        }
        if(this.p_rsiIndS.lastValue().getValue() >= this.p_rsiSMx1HInd.valueBack(1).getValue().getValue() &&
        		this.p_ema12Ind.lastValue().getValue() < this.p_ema12Mx1HInd.valueBack(1).getValue().getValue()){
        	gene = IGene.D;
        }
        genes.add(gene);
        this.p_labels.add("DIVER-0");
        
        gene = IGene.F;
        if(this.p_rsiIndS.lastValue().getValue() <= this.p_rsiSMx4HInd.valueBack(1).getValue().getKey() &&
        		this.p_ema12Ind.lastValue().getValue() > this.p_ema12Mx4HInd.valueBack(1).getValue().getKey()){
        	gene = IGene.U;
        }
        if(this.p_rsiIndS.lastValue().getValue() >= this.p_rsiSMx4HInd.valueBack(1).getValue().getValue() &&
        		this.p_ema12Ind.lastValue().getValue() < this.p_ema12Mx4HInd.valueBack(1).getValue().getValue()){
        	gene = IGene.D;
        }
        genes.add(gene);
        this.p_labels.add("DIVER-2");
        
        gene = IGene.F;
        if(this.p_rsiIndS.lastValue().getValue() <= this.p_rsiSMx4HInd.valueBack(1).getValue().getKey() &&
        		this.p_ema24Ind.lastValue().getValue() > this.p_ema24Mx4HInd.valueBack(1).getValue().getKey()){
        	gene = IGene.U;
        }
        if(this.p_rsiIndS.lastValue().getValue() >= this.p_rsiSMx4HInd.valueBack(1).getValue().getValue() &&
        		this.p_ema24Ind.lastValue().getValue() < this.p_ema24Mx4HInd.valueBack(1).getValue().getValue()){
        	gene = IGene.D;
        }
        genes.add(gene);
        this.p_labels.add("DIVER-3");

        gene = IGene.F;
        if(this.p_asset.lastValue().getLow() <= this.p_max4HInd.valueBack(1).getValue().getKey() &&
        		this.p_macdInd.lastValue().getValue().getValue() > this.p_macdMx4HInd.lastValue().getValue().getKey()){
        	gene = IGene.U;
        }
        if(this.p_asset.lastValue().getHigh() >= this.p_max4HInd.valueBack(1).getValue().getValue() &&
        		this.p_macdInd.lastValue().getValue().getValue() < this.p_macdMx4HInd.lastValue().getValue().getValue()){
        	gene = IGene.D;
        }
        genes.add(gene);
        this.p_labels.add("DIVMC-1");
        
        gene = IGene.F;
        if(this.p_asset.lastValue().getLow() <= this.p_max8HInd.valueBack(1).getValue().getKey() &&
        		this.p_macdInd.lastValue().getValue().getValue() > this.p_macdMx8HInd.lastValue().getValue().getKey()){
        	gene = IGene.U;
        }
        if(this.p_asset.lastValue().getHigh() >= this.p_max8HInd.valueBack(1).getValue().getValue() &&
        		this.p_macdInd.lastValue().getValue().getValue() < this.p_macdMx8HInd.lastValue().getValue().getValue()){
        	gene = IGene.D;
        }
        genes.add(gene);
        this.p_labels.add("DIVMC-2");
        
        gene = IGene.F;
        if(this.p_asset.lastValue().getLow() <= this.p_max1WInd.valueBack(1).getValue().getKey() &&
        		this.p_macdInd.lastValue().getValue().getValue() > this.p_macdMx1WInd.lastValue().getValue().getKey()){
        	gene = IGene.U;
        }
        if(this.p_asset.lastValue().getHigh() >= this.p_max1WInd.valueBack(1).getValue().getValue() &&
        		this.p_macdInd.lastValue().getValue().getValue() < this.p_macdMx1WInd.lastValue().getValue().getValue()){
        	gene = IGene.D;
        }
        genes.add(gene);
        this.p_labels.add("DIVMC-4");
        
        gene = IGene.F;
        if(this.p_asset.lastValue().getLow() <= this.p_max8HInd.valueBack(1).getValue().getKey() &&
        		this.p_macdInd.lastValue().getValue().getKey() > this.p_makyMx8HInd.lastValue().getValue().getKey()){
        	gene = IGene.U;
        }
        if(this.p_asset.lastValue().getHigh() >= this.p_max8HInd.valueBack(1).getValue().getValue() &&
        		this.p_macdInd.lastValue().getValue().getKey() < this.p_makyMx8HInd.lastValue().getValue().getValue()){
        	gene = IGene.D;
        }
        genes.add(gene);
        this.p_labels.add("DIVMC-7");
        
        gene = IGene.F;
        if(this.p_asset.lastValue().getLow() <= this.p_max1WInd.valueBack(1).getValue().getKey() &&
        		this.p_macdInd.lastValue().getValue().getKey() > this.p_makyMx1WInd.lastValue().getValue().getKey()){
        	gene = IGene.U;
        }
        if(this.p_asset.lastValue().getHigh() >= this.p_max1WInd.valueBack(1).getValue().getValue() &&
        		this.p_macdInd.lastValue().getValue().getKey() < this.p_makyMx1WInd.lastValue().getValue().getValue()){
        	gene = IGene.D;
        }
        genes.add(gene);
        this.p_labels.add("DIVMC-9");
        
    	gene = IGene.F;
        if(this.p_asset.lastValue().getLow() <= this.p_max4HInd.valueBack(1).getValue().getKey() &&
        		this.p_rsiIndS.lastValue().getValue() > this.p_rsiSMx4HInd.lastValue().getValue().getKey()){
        	gene = IGene.U;
        }
        if(this.p_asset.lastValue().getHigh() >= this.p_max4HInd.valueBack(1).getValue().getValue() &&
        		this.p_rsiIndS.lastValue().getValue() < this.p_rsiSMx4HInd.lastValue().getValue().getValue()){
        	gene = IGene.D;
        }
        genes.add(gene);
        this.p_labels.add("DIVRS-1");
        
    	gene = IGene.F;
        if(this.p_asset.lastValue().getLow() <= this.p_max1WInd.valueBack(1).getValue().getKey() &&
        		this.p_rsiIndS.lastValue().getValue() > this.p_rsiSMx1WInd.lastValue().getValue().getKey()){
        	gene = IGene.U;
        }
        if(this.p_asset.lastValue().getHigh() >= this.p_max1WInd.valueBack(1).getValue().getValue() &&
        		this.p_rsiIndS.lastValue().getValue() < this.p_rsiSMx1WInd.lastValue().getValue().getValue()){
        	gene = IGene.D;
        }
        genes.add(gene);
        this.p_labels.add("DIVRS-4");
        
    	gene = IGene.F;
        if(this.p_asset.lastValue().getLow() <= this.p_max1WInd.valueBack(1).getValue().getKey() &&
        		this.p_rsiIndL.lastValue().getValue() > this.p_rsiLMx1WInd.lastValue().getValue().getKey()){
        	gene = IGene.U;
        }
        if(this.p_asset.lastValue().getHigh() >= this.p_max1WInd.valueBack(1).getValue().getValue() &&
        		this.p_rsiIndL.lastValue().getValue() < this.p_rsiLMx1WInd.lastValue().getValue().getValue()){
        	gene = IGene.D;
        }
        genes.add(gene);
        this.p_labels.add("DIVRS-9");

        gene = IGene.F;
        if(this.p_asset.lastValue().getLow() <= this.p_max1HInd.valueBack(1).getValue().getKey() &&
        		this.p_cciIndS.lastValue().getValue() > this.p_cciSMx1HInd.lastValue().getValue().getKey()){
        	gene = IGene.U;
        }
        if(this.p_asset.lastValue().getHigh() >= this.p_max1HInd.valueBack(1).getValue().getValue() &&
        		this.p_cciIndS.lastValue().getValue() < this.p_cciSMx1HInd.lastValue().getValue().getValue()){
        	gene = IGene.D;
        }
        genes.add(gene);
        this.p_labels.add("DIVCC-0");
        
        gene = IGene.F;
        if(this.p_asset.lastValue().getLow() <= this.p_max4HInd.valueBack(1).getValue().getKey() &&
        		this.p_cciIndS.lastValue().getValue() > this.p_cciSMx4HInd.lastValue().getValue().getKey()){
        	gene = IGene.U;
        }
        if(this.p_asset.lastValue().getHigh() >= this.p_max1HInd.valueBack(1).getValue().getValue() &&
        		this.p_cciIndS.lastValue().getValue() < this.p_cciSMx4HInd.lastValue().getValue().getValue()){
        	gene = IGene.D;
        }
        genes.add(gene);
        this.p_labels.add("DIVCC-1");
        
    	gene = IGene.F;
        if(this.p_rsiIndS.lastValue().getValue() <= this.p_rsiSMx8HInd.valueBack(1).getValue().getKey() &&
        		this.p_macdInd.lastValue().getValue().getValue() > this.p_macdMx8HInd.lastValue().getValue().getKey()){
        	gene = IGene.U;
        }
        if(this.p_rsiIndS.lastValue().getValue() >= this.p_rsiSMx8HInd.valueBack(1).getValue().getValue() &&
        		this.p_macdInd.lastValue().getValue().getValue() < this.p_macdMx8HInd.lastValue().getValue().getValue()){
        	gene = IGene.D;
        }
        genes.add(gene);
        this.p_labels.add("DIVMR-2");
        
        gene = IGene.F;
        if(this.p_rsiIndL.lastValue().getValue() <= this.p_rsiLMx8HInd.valueBack(1).getValue().getKey() &&
        		this.p_macdInd.lastValue().getValue().getValue() > this.p_macdMx8HInd.lastValue().getValue().getKey()){
        	gene = IGene.U;
        }
        if(this.p_rsiIndL.lastValue().getValue() >= this.p_rsiLMx8HInd.valueBack(1).getValue().getValue() &&
        		this.p_macdInd.lastValue().getValue().getValue() < this.p_macdMx8HInd.lastValue().getValue().getValue()){
        	gene = IGene.D;
        }
        genes.add(gene);
        this.p_labels.add("DIVMR-7");

    	gene = IGene.F;
        if(this.p_stoIndS.lastValue().getValue() <= this.p_stoSMx1HInd.valueBack(1).getValue().getKey() &&
        		this.p_macdInd.lastValue().getValue().getValue() > this.p_macdMx1HInd.lastValue().getValue().getKey()){
        	gene = IGene.U;
        }
        if(this.p_stoIndS.lastValue().getValue() >= this.p_stoSMx1HInd.valueBack(1).getValue().getValue() &&
        		this.p_macdInd.lastValue().getValue().getValue() < this.p_macdMx1HInd.lastValue().getValue().getValue()){
        	gene = IGene.D;
        }
        genes.add(gene);
        this.p_labels.add("DIVMS-0");
        
    	gene = IGene.F;
        if(this.p_stoIndS.lastValue().getValue() <= this.p_stoSMx4HInd.valueBack(1).getValue().getKey() &&
        		this.p_macdInd.lastValue().getValue().getValue() > this.p_macdMx4HInd.lastValue().getValue().getKey()){
        	gene = IGene.U;
        }
        if(this.p_stoIndS.lastValue().getValue() >= this.p_stoSMx4HInd.valueBack(1).getValue().getValue() &&
        		this.p_macdInd.lastValue().getValue().getValue() < this.p_macdMx4HInd.lastValue().getValue().getValue()){
        	gene = IGene.D;
        }
        genes.add(gene);
        this.p_labels.add("DIVMS-1");
        
    	gene = IGene.F;
        if(this.p_stoIndS.lastValue().getValue() <= this.p_stoSMx8HInd.valueBack(1).getValue().getKey() &&
        		this.p_macdInd.lastValue().getValue().getValue() > this.p_macdMx8HInd.lastValue().getValue().getKey()){
        	gene = IGene.U;
        }
        if(this.p_stoIndS.lastValue().getValue() >= this.p_stoSMx8HInd.valueBack(1).getValue().getValue() &&
        		this.p_macdInd.lastValue().getValue().getValue() < this.p_macdMx8HInd.lastValue().getValue().getValue()){
        	gene = IGene.D;
        }
        genes.add(gene);
        this.p_labels.add("DIVMS-2");
        
    	gene = IGene.F;
        if(this.p_stoIndL.lastValue().getValue() <= this.p_stoLMx1DInd.valueBack(1).getValue().getKey() &&
        		this.p_macdInd.lastValue().getValue().getValue() > this.p_macdMx1DInd.lastValue().getValue().getKey()){
        	gene = IGene.U;
        }
        if(this.p_stoIndL.lastValue().getValue() >= this.p_stoLMx1DInd.valueBack(1).getValue().getValue() &&
        		this.p_macdInd.lastValue().getValue().getValue() < this.p_macdMx1DInd.lastValue().getValue().getValue()){
        	gene = IGene.D;
        }
        genes.add(gene);
        this.p_labels.add("DIVMS-8");
        
    	gene = IGene.F;
        if(this.p_stoIndL.lastValue().getValue() <= this.p_stoLMx1WInd.valueBack(1).getValue().getKey() &&
        		this.p_macdInd.lastValue().getValue().getValue() > this.p_macdMx1WInd.lastValue().getValue().getKey()){
        	gene = IGene.U;
        }
        if(this.p_stoIndL.lastValue().getValue() >= this.p_stoLMx1WInd.valueBack(1).getValue().getValue() &&
        		this.p_macdInd.lastValue().getValue().getValue() < this.p_macdMx1WInd.lastValue().getValue().getValue()){
        	gene = IGene.D;
        }
        genes.add(gene);
        this.p_labels.add("DIVMS-9");
        
        gene = IGene.F;
        if(this.p_ema12Ind.lastValue().getValue() > this.p_ema12Ind.valueBack(this.p_memory).getValue() &&
        		this.p_ema12Ind.valueBack(1).getValue() <= this.p_ema12Ind.valueBack(this.p_memory).getValue() &&
        		!(this.p_rsiIndS.lastValue().getValue() <= this.p_rsiSMx1HInd.valueBack(1).getValue().getKey() &&
                this.p_macdInd.lastValue().getValue().getValue() > this.p_macdMx1HInd.lastValue().getValue().getKey())){
        	gene = IGene.D;
        }
        if(this.p_ema12Ind.lastValue().getValue() < this.p_ema12Ind.valueBack(this.p_memory).getValue() &&
        		this.p_ema12Ind.valueBack(1).getValue() >= this.p_ema12Ind.valueBack(this.p_memory).getValue() &&
        		!(this.p_rsiIndS.lastValue().getValue() >= this.p_rsiSMx1HInd.valueBack(1).getValue().getValue() &&
        		this.p_macdInd.lastValue().getValue().getValue() < this.p_macdMx1HInd.lastValue().getValue().getValue())){
        	gene = IGene.U;
        }
        genes.add(gene);
        this.p_labels.add("XDIV-CO0");  
        
        gene = IGene.F;
        if(this.p_rsiIndS.lastValue().getValue() <= rsiLevel){
        	if(this.p_rsiIndS.valueBack(1).getValue() <= this.p_rsiIndS.lastValue().getValue() 
        			|| this.p_ema24Ind.lastValue().getValue() > this.p_ema24Ind.valueBack(this.p_memory).getValue()){
        		gene = IGene.U;
            }
        }
        if(this.p_rsiIndS.lastValue().getValue() >= 100-rsiLevel){
        	if(this.p_rsiIndS.valueBack(1).getValue() >= this.p_rsiIndS.lastValue().getValue() 
        			|| this.p_ema24Ind.lastValue().getValue() < this.p_ema24Ind.valueBack(this.p_memory).getValue()){
        		gene = IGene.D;
        	}
        }
        genes.add(gene);
        this.p_labels.add("XRSI-0");
        
        gene = IGene.F;
        if(this.p_rsiIndS.lastValue().getValue() > this.p_rsiIndL.lastValue().getValue() &&
        		this.p_rsiIndS.valueBack(1).getValue() <= this.p_rsiIndL.valueBack(1).getValue() &&
        		this.p_ema48Ind.lastValue().getValue() > this.p_ema48Ind.valueBack(this.p_memory).getValue() &&
        		this.p_adpIndS.lastValue().getValue().getValue() > this.p_adpIndS.valueBack(this.p_memory).getValue().getValue()){
        	gene = IGene.D;
        }
        if(this.p_rsiIndS.lastValue().getValue() < this.p_rsiIndL.lastValue().getValue() &&
        		this.p_rsiIndS.valueBack(1).getValue() >= this.p_rsiIndL.valueBack(1).getValue() &&
        		this.p_ema48Ind.lastValue().getValue() < this.p_ema48Ind.valueBack(this.p_memory).getValue() &&
        		this.p_adpIndS.lastValue().getValue().getValue() < this.p_adpIndS.valueBack(this.p_memory).getValue().getValue()){
        	gene = IGene.U;
        }
        genes.add(gene);
        this.p_labels.add("XCOMB-0");

        gene = IGene.F;
        if(this.p_asset.lastValue().getClose() > (this.p_emsdIndL.lastValue().getValue().getKey() + 3 * this.p_emsdIndL.lastValue().getValue().getValue()) &&
        		this.p_ema6Ind.lastValue().getValue() > this.p_ema6Ind.valueBack(this.p_memory).getValue() &&
        		!(this.p_hls48Ind.lastValue().getValue().getKey() > this.p_hls48Ind.lastValue().getValue().getValue() &&
                this.p_hls48Ind.valueBack(1).getValue().getKey() <= this.p_hls48Ind.valueBack(1).getValue().getValue())){
        	gene = IGene.D;
        }
        if(this.p_asset.lastValue().getClose() < (this.p_emsdIndL.lastValue().getValue().getKey() - 3 * this.p_emsdIndL.lastValue().getValue().getValue()) &&
        		this.p_ema6Ind.lastValue().getValue() < this.p_ema6Ind.valueBack(this.p_memory).getValue() &&
        		!(this.p_hls48Ind.lastValue().getValue().getKey() < this.p_hls48Ind.lastValue().getValue().getValue() &&
                this.p_hls48Ind.valueBack(1).getValue().getKey() >= this.p_hls48Ind.valueBack(1).getValue().getValue())){
        	gene = IGene.U;
        }
        genes.add(gene); 
        this.p_labels.add("XCOMB-3");
        
        gene = IGene.F;
        pLong = true; pShort = true;
        emas = this.p_ema48Ind.valueList(this.p_memory);
        for(int k = 0; k < emas.size(); k++){
            if(prices.get(k).getClose() >= emas.get(k).getValue()){
                pShort = false;
            }else{
                pLong = false;
            }
        }
        if(pLong && (prices.get(prices.size() - 1).getClose() - emas.get(emas.size() - 1).getValue()) > 
        	(prices.get(0).getClose() - emas.get(0).getValue()) &&
        	this.p_macdInd.lastValue().getValue().getValue() > this.p_macdMx1HInd.lastValue().getValue().getKey()){
        	gene = IGene.D;
        }
        if(pShort && (prices.get(prices.size() - 1).getClose() - emas.get(emas.size() - 1).getValue()) < 
        		(prices.get(0).getClose() - emas.get(0).getValue()) &&
			this.p_macdInd.lastValue().getValue().getValue() < this.p_macdMx1HInd.lastValue().getValue().getValue()){
        	gene = IGene.U;
        }    
        genes.add(gene);
        this.p_labels.add("XDIV-CO1");
        
        gene = IGene.F;
        if(this.p_asset.lastValue().getClose() < this.p_ema60Ind.lastValue().getValue()*(1 - priceLevel[2]) &&
        		this.p_asset.valueBack(1).getClose() >= this.p_ema60Ind.valueBack(1).getValue()*(1 - priceLevel[2])){
        	gene = IGene.U;
        }
        if(this.p_asset.lastValue().getClose() > this.p_ema60Ind.lastValue().getValue()*(1 + priceLevel[2]) &&
        		this.p_asset.valueBack(1).getClose() <= this.p_ema60Ind.valueBack(1).getValue()*(1 + priceLevel[2])){
        	gene = IGene.D;
        }
        genes.add(gene);
        this.p_labels.add("XEMA-2");
        
        gene = IGene.F;
        if((this.p_ema6Ind.valueBack(timeStep).getValue() - this.p_ema6Ind.valueBack(timeStep+this.p_memory).getValue())/this.p_ema6Ind.valueBack(timeStep+this.p_memory).getValue() < -rocLevel
        	&& (this.p_ema6Ind.valueBack(8*timeStep).getValue() - this.p_ema6Ind.valueBack(8*timeStep+this.p_memory).getValue())/this.p_ema6Ind.valueBack(8*timeStep+this.p_memory).getValue() >= -rocLevel){
        	gene = IGene.D;
        }else if((this.p_ema6Ind.valueBack(timeStep).getValue() - this.p_ema6Ind.valueBack(timeStep+this.p_memory).getValue())/this.p_ema6Ind.valueBack(timeStep+this.p_memory).getValue() > rocLevel
        	&& (this.p_ema6Ind.valueBack(8*timeStep).getValue() - this.p_ema6Ind.valueBack(8*timeStep+this.p_memory).getValue())/this.p_ema6Ind.valueBack(8*timeStep+this.p_memory).getValue() <= rocLevel){
        	gene = IGene.U;
        }else{
        	if((this.p_ema6Ind.valueBack(8*timeStep).getValue() - this.p_ema6Ind.valueBack(8*timeStep+this.p_memory).getValue())/this.p_ema6Ind.valueBack(8*timeStep+this.p_memory).getValue() < -rocLevel){
            	gene = IGene.U;
            }
            if((this.p_ema6Ind.valueBack(8*timeStep).getValue() - this.p_ema6Ind.valueBack(8*timeStep+this.p_memory).getValue())/this.p_ema6Ind.valueBack(8*timeStep+this.p_memory).getValue() > rocLevel){
            	gene = IGene.D;
            }
        }
        genes.add(gene);
        this.p_labels.add("XTIME-1");
        
        gene = IGene.F;
        if(this.p_asset.lastValue().getLow() <= this.p_max8HInd.valueBack(1).getValue().getKey() &&
        		this.p_cciIndS.lastValue().getValue() > this.p_cciSMx8HInd.lastValue().getValue().getKey()){
        	gene = IGene.U;
        }
        if(this.p_asset.lastValue().getHigh() >= this.p_max8HInd.valueBack(1).getValue().getValue() &&
        		this.p_cciIndS.lastValue().getValue() < this.p_cciSMx8HInd.lastValue().getValue().getValue()){
        	gene = IGene.D;
        }
        genes.add(gene);
        this.p_labels.add("XDIV-CC0");
        
        gene = IGene.F;
        if(this.p_asset.lastValue().getClose() < this.p_max8HInd.valueBack(1).getValue().getValue() &&
        		this.p_asset.lastValue().getHigh() > this.p_max8HInd.valueBack(1).getValue().getValue() &&
        		this.p_asset.lastValue().getHigh() >= this.p_max1DInd.valueBack(1).getValue().getValue() &&
        		this.p_macdInd.lastValue().getValue().getValue() < this.p_macdMx1DInd.lastValue().getValue().getValue()){
        	gene = IGene.U;
        }
        if(this.p_asset.lastValue().getClose() > this.p_max8HInd.valueBack(1).getValue().getKey() &&
        		this.p_asset.lastValue().getLow() < this.p_max8HInd.valueBack(1).getValue().getKey() &&
        		this.p_asset.lastValue().getLow() <= this.p_max1DInd.valueBack(1).getValue().getKey() &&
        		this.p_macdInd.lastValue().getValue().getValue() > this.p_macdMx1DInd.lastValue().getValue().getKey()){
        	gene = IGene.D;
        }
        genes.add(gene);
        this.p_labels.add("XDIV-CD2"); 
        
        gene = IGene.F;
        if(this.p_max4HInd.lastValue().getValue().getValue() > this.p_max4HInd.valueBack(this.p_memory).getValue().getValue() &&
        		this.p_max4HInd.lastValue().getValue().getKey() >= this.p_max4HInd.valueBack(this.p_memory).getValue().getKey() &&
        		this.p_asset.lastValue().getHigh() >= this.p_max1HInd.valueBack(1).getValue().getValue() &&
        		this.p_macdInd.lastValue().getValue().getKey() < this.p_makyMx1HInd.lastValue().getValue().getValue()){
        	gene = IGene.D;
        }
        if(this.p_max4HInd.lastValue().getValue().getValue() <= this.p_max4HInd.valueBack(this.p_memory).getValue().getValue() &&
        		this.p_max4HInd.lastValue().getValue().getKey() < this.p_max4HInd.valueBack(this.p_memory).getValue().getKey() &&
        		this.p_asset.lastValue().getLow() <= this.p_max1HInd.valueBack(1).getValue().getKey() &&
        		this.p_macdInd.lastValue().getValue().getKey() > this.p_makyMx1HInd.lastValue().getValue().getKey()){
        	gene = IGene.U;
        }
        genes.add(gene);
        this.p_labels.add("XDIV-CD3");

        gene = IGene.F;
        if(this.p_rsiIndS.lastValue().getValue() > this.p_rsiIndL.lastValue().getValue() &&
        		this.p_rsiIndS.valueBack(1).getValue() <= this.p_rsiIndL.valueBack(1).getValue()
                && this.p_rsiIndS.lastValue().getValue() > this.p_rsiIndS.valueBack(this.p_memory).getValue()){
        	gene = IGene.D;
        }
        if(this.p_rsiIndS.lastValue().getValue() < this.p_rsiIndL.lastValue().getValue() &&
        		this.p_rsiIndS.valueBack(1).getValue() >= this.p_rsiIndL.valueBack(1).getValue()
                && this.p_rsiIndS.lastValue().getValue() < this.p_rsiIndS.valueBack(this.p_memory).getValue()){
        	gene = IGene.U;
        }
        genes.add(gene);
        this.p_labels.add("RSI-1");
        
        gene = IGene.F;
        if(this.p_stoIndS.lastValue().getValue() > this.p_stoIndL.lastValue().getValue() &&
        		this.p_stoIndS.valueBack(1).getValue() <= this.p_stoIndL.valueBack(1).getValue()){
        	gene = IGene.D;
        }
        if(this.p_stoIndS.lastValue().getValue() < this.p_stoIndL.lastValue().getValue() &&
        		this.p_stoIndS.valueBack(1).getValue() >= this.p_stoIndL.valueBack(1).getValue()){
        	gene = IGene.U;
        }
        genes.add(gene);
        this.p_labels.add("XSTO-0");
        
        gene = IGene.F;
        if(this.p_rsiIndS.lastValue().getValue() <= rsiLevel){
        	if(this.p_rsiIndS.valueBack(1).getValue() <= this.p_rsiIndS.lastValue().getValue() 
        			|| this.p_ema24Ind.lastValue().getValue() > this.p_ema24Ind.valueBack(this.p_memory).getValue()){
        		gene = IGene.U;
            }
        }
        if(this.p_rsiIndS.lastValue().getValue() >= 100-rsiLevel){
        	if(this.p_rsiIndS.valueBack(1).getValue() >= this.p_rsiIndS.lastValue().getValue() 
        			|| this.p_ema24Ind.lastValue().getValue() < this.p_ema24Ind.valueBack(this.p_memory).getValue()){
        		gene = IGene.D;
        	}
        }
        genes.add(gene);
        this.p_labels.add("XRSI-0");
        
        gene = IGene.F;
        if(this.p_asset.lastValue().getClose() < this.p_ema24Ind.lastValue().getValue()*(1 - priceLevel[1]) &&
        		this.p_asset.valueBack(1).getClose() >= this.p_ema24Ind.valueBack(1).getValue()*(1 - priceLevel[1])){
        	gene = IGene.U;
        }
        if(this.p_asset.lastValue().getClose() > this.p_ema24Ind.lastValue().getValue()*(1 + priceLevel[1]) &&
        		this.p_asset.valueBack(1).getClose() <= this.p_ema24Ind.valueBack(1).getValue()*(1 + priceLevel[1])){
        	gene = IGene.D;
        }
        genes.add(gene);
        this.p_labels.add("XPRC-4");
        
		return new Nucleic(genes.toArray(new IGene[genes.size()]));
	}
	
	/* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
	@Override
	public boolean equals(Object obj){
		if (obj instanceof Analysis) {
			Analysis that = (Analysis) obj;
			return (this.p_asset.equals(that.p_asset)) 
					&& Objects.equal(this.p_executor, that.p_executor);
		}
        
		return false;
	}
	
	/* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
	@Override
	public int hashCode() {
		return (this.p_asset.hashCode()) ^ 
				 ((this.p_executor == null) ? 0 : this.p_executor.hashCode());
	}
	
	 /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
	@Override
	public String toString() {
        return String.format("Asset: %s, Executor: %s", this.p_asset, this.p_executor);
	}
}
