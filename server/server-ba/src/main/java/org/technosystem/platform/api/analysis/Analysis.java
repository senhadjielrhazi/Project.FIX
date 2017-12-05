package org.technosystem.platform.api.analysis;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.technosystem.modules.marketdata.assets.ITicker;
import org.technosystem.modules.marketdata.indicator.IIndicator;
import org.technosystem.modules.marketdata.indicator.Indicator;
import org.technosystem.modules.marketdata.indicator.basic.EMA;
import org.technosystem.modules.marketdata.indicator.basic.STO;
import org.technosystem.modules.marketdata.quote.IQuote;
import org.technosystem.modules.marketdata.quote.Quote;
import org.technosystem.modules.marketdata.server.DataServer;
import org.technosystem.modules.marketdata.server.IDataServer;
import org.neurosystem.util.common.base.Preconditions;
import org.neurosystem.util.misc.Pair;
import org.neurosystem.util.time.IPeriod;

public class Analysis implements IAnalysis {
	
	private final Object[] p_dbParams;
	//{new int[]{10, 2}, 
	//new Object[]{new int[]{5, 14, 30}, 3, 35.}, 
	//new Object[]{6, new int[]{6, 12, 4*12, 12*12}}}
	
	private final Map<ITicker, IDataServer[]> p_dbServers;
	private final Map<ITicker, IIndicator<?>[]> p_strengthInd;//EMA/STO
	private final Map<ITicker, IIndicator<Double>> p_stoInd;//STO
	
	public Analysis(List<ITicker> tickers, Pair<IPeriod, IPeriod>[] dbPeriods, Object[] dbParams){
		Preconditions.checkArgument(dbPeriods.length == 3, "Data Base periods messed-up : %s", Arrays.toString(dbPeriods));
		
		this.p_dbParams = dbParams;
		
		this.p_dbServers = new HashMap<>();
		this.p_strengthInd = new HashMap<>();
		
		this.p_stoInd = new HashMap<>();
		
		for(ITicker ticker:tickers){
			if(!this.p_dbServers.containsKey(ticker)){
				IDataServer[] servers = new IDataServer[3];

				//4H TS-EMA/STO*****************************************************************************/
				IPeriod barPeriod4H = dbPeriods[0].getKey(), hisPeriod4H = dbPeriods[0].getValue();
				servers[0] = new DataServer(barPeriod4H, hisPeriod4H.getNumOfUnits(barPeriod4H));

				final Object[] params4H = (Object[])this.p_dbParams[0];
				
				int[] strengthParams = (int[])params4H[0];
				IIndicator<?>[] strengthInd = {new EMA(strengthParams[0]), new EMA(strengthParams[1]), new EMA(strengthParams[2]),
						new STO(strengthParams[0]), new STO(strengthParams[1]), new STO(strengthParams[2])};
				this.p_strengthInd.put(ticker, strengthInd);
				servers[0].subscribe(strengthInd);
				
				
				//1H Pattern Matcher*********************************************************************/
				IPeriod barPeriod1H = dbPeriods[1].getKey(), hisPeriod1H = dbPeriods[1].getValue();
				servers[1] = new DataServer(barPeriod1H, hisPeriod1H.getNumOfUnits(barPeriod1H));
				
				
				//5M Data Score*****************************************************************************/
				IPeriod barPeriod5M = dbPeriods[2].getKey(), hisPeriod5M = dbPeriods[2].getValue();
				servers[2] = new DataServer(barPeriod5M, hisPeriod5M.getNumOfUnits(barPeriod5M));
				
				final Object[] params5M = (Object[])this.p_dbParams[2];
				int stoNB = (int)params5M[0];
				
				Indicator<Double> stoInd = new STO(stoNB);
				this.p_stoInd.put(ticker, stoInd);
				servers[2].subscribe(stoInd);
				
				this.p_dbServers.put(ticker, servers);
			}
		}
	}
	
	@Override
	public void onValue(IPeriod period, ITicker ticker, IQuote quote) {
		Preconditions.checkArgument(this.p_dbServers.containsKey(ticker), "Ticker missing: %s", ticker);
		
		IDataServer[] dataServers = this.p_dbServers.get(ticker);
		for(IDataServer dataServer:dataServers){
			dataServer.onQuote(period, quote);
		}
	}

	@Override
	public IPrediction getPrediction(ITicker ticker) {
		
		/****************************************Signal EMA/RSI -- 4Hours******************************************************/
		final Object[] params4H = (Object[])this.p_dbParams[0];
		final int memory = (int) params4H[1];
		final double stoLevel = (double) params4H[2];
		
		double strength = getStrength(this.p_strengthInd.get(ticker), memory, stoLevel);
		
		
		/****************************************PatternMatcher -- 1Hour******************************************************/
		final int[] pmParams = (int[])this.p_dbParams[1];
		final int dnaSZ = pmParams[0];
		final int predSZ = pmParams[1];
		
		final IDataServer dataServer = this.p_dbServers.get(ticker)[1];
		final int dsSZ = dataServer.size();
		
		final int strIdx = 0;
		final int endIdx = dsSZ - dnaSZ - predSZ;
		
		Double entropyOpt = null;
		Integer indexOpt = null;
		
		IQuote qLast = dataServer.get(dsSZ - 1);
		for(int index = strIdx; index < endIdx; index++){
			double entropy = 0;
			double normFact = qLast.getTypical() 
					/ dataServer.get(index + dnaSZ - 1).getTypical();
            
			for (int k = 0; k < dnaSZ - 1; k++) {
                entropy += Math.abs(normFact * dataServer.get(k + index).getTypical() 
                		- dataServer.get(dsSZ - dnaSZ + k).getTypical()) / (dnaSZ - 1);
            }
            
            if(entropyOpt == null){
            	entropyOpt = entropy;
            	indexOpt = index;
            }else{
            	if(entropyOpt > entropy){
                	entropyOpt = entropy;
                	indexOpt = index;
            	}
            }
		}
		
		IQuote qOptLast = dataServer.get(indexOpt + dnaSZ - 1);
		List<IQuote> forecast = new ArrayList<>();
		double normFact = qLast.getClose() 
				/ qOptLast.getClose();
		
		forecast.add(Quote.times(1., qOptLast, normFact));
		for (int k = 0; k < predSZ-1; k++) {
			forecast.add(Quote.times(1., dataServer.get(indexOpt + dnaSZ + k), normFact));
		}
		
		
		/****************************************Data Score -- 5Minutes*********************************************************/
		final Object[] params5M = (Object[])this.p_dbParams[2];
		final int[] lapParams = (int[])params5M[1];
		
		List<Double> scores = new ArrayList<>();
		for(Integer lap:lapParams){
			
			Map<String, Double> ccys = new HashMap<>();
			Map<String, Double> nbs = new HashMap<>();
			for (Entry<ITicker, IDataServer[]> entry : this.p_dbServers.entrySet()){           
				ITicker key = entry.getKey();
				IDataServer ds = entry.getValue()[2];
				
				double keyClose0 = ds.lastValue().getClose();       
	            double keyClose1 = ds.valueBack(lap).getClose();
	            double keyScore = (keyClose0 - keyClose1) / keyClose1;
	            
	            String ccy0 = key.getBase();
	            if(!ccys.containsKey(ccy0)){
		            ccys.put(ccy0, keyScore);
		            nbs.put(ccy0, 1.);
	            }else{
	            	ccys.put(ccy0, ccys.get(ccy0) + keyScore);
	            	nbs.put(ccy0, nbs.get(ccy0) + 1.);
	            }
	            
	            String ccy1 = key.getTerm();
	            if(!ccys.containsKey(ccy1)){
		            ccys.put(ccy1, -keyScore);
		            nbs.put(ccy1, 1.);
	            }else{
	            	ccys.put(ccy1, ccys.get(ccy1) - keyScore);
	            	nbs.put(ccy1, nbs.get(ccy1) + 1.);
	            }
	        }

			double keyScore0 = ccys.get(ticker.getBase()) / nbs.get(ticker.getBase());
            double keyScore1 = ccys.get(ticker.getTerm()) / nbs.get(ticker.getTerm());
            
            scores.add(keyScore0-keyScore1);
		}

		Map<String, Double> ccys = new HashMap<>();
		Map<String, Double> nbs = new HashMap<>();
		for (Entry<ITicker, IIndicator<Double>> entry : this.p_stoInd.entrySet()){           
            double keyScore = entry.getValue().lastValue().getValue();       
            ITicker key = entry.getKey();
            
            String ccy0 = key.getBase();
            if(!ccys.containsKey(ccy0)){
            	ccys.put(ccy0, keyScore);
	            nbs.put(ccy0, 1.);
            }else{
            	ccys.put(ccy0, ccys.get(ccy0) + keyScore);
            	nbs.put(ccy0, nbs.get(ccy0) + 1.);
            }
            
            String ccy1 = key.getTerm();
            if(!ccys.containsKey(ccy1)){
            	ccys.put(ccy1, (100. - keyScore));
	            nbs.put(ccy1, 1.);
            }else{
            	ccys.put(ccy1, ccys.get(ccy1) + (100. - keyScore));
            	nbs.put(ccy1, nbs.get(ccy1) + 1.);
            }
        }

		double keyScore0 = ccys.get(ticker.getBase()) / nbs.get(ticker.getBase());
        double keyScore1 = ccys.get(ticker.getTerm()) / nbs.get(ticker.getTerm());
        double stochastic = (keyScore0 + (100.-keyScore1)) / 2.;		
		
		return new Prediction(forecast, entropyOpt, qOptLast.getTime(), 
				strength, scores, stochastic);
	}
	
	@SuppressWarnings("unchecked")
	private static double getStrength(IIndicator<?>[] indicators, int memory, double stoLevel) {
		final IIndicator<Double> emaSInd = (IIndicator<Double>) indicators[0], 
				emaMInd = (IIndicator<Double>) indicators[1], emaLInd = (IIndicator<Double>) indicators[2];
		
		final IIndicator<Double> stoSInd = (IIndicator<Double>) indicators[3], 
				stoMInd = (IIndicator<Double>) indicators[4], stoLInd = (IIndicator<Double>) indicators[5];
		
		double score = 0.0;

        //Signal CrossOver
        double signal0 = 0;
        if(emaSInd.lastValue().getValue() > emaMInd.lastValue().getValue() 
        		&& emaSInd.valueBack(1).getValue() <= emaMInd.valueBack(1).getValue()){
        	signal0 += 1;
        }else if(emaSInd.lastValue().getValue() < emaMInd.lastValue().getValue() 
        		&& emaSInd.valueBack(1).getValue() >= emaMInd.valueBack(1).getValue()){
        	signal0 += -1;
        }
        if(emaMInd.lastValue().getValue() > emaLInd.lastValue().getValue() 
        		&& emaMInd.valueBack(1).getValue() <= emaLInd.valueBack(1).getValue()){
        	signal0 += 1;
        }else if(emaMInd.lastValue().getValue() < emaLInd.lastValue().getValue() 
        		&& emaMInd.valueBack(1).getValue() >= emaLInd.valueBack(1).getValue()){
        	signal0 += -1;
        }
        score += Math.signum(signal0);
        
        //Signal Slope
        double signal1 = 0;
        if(emaSInd.lastValue().getValue() > emaSInd.valueBack(memory).getValue() 
        		&& emaMInd.lastValue().getValue() > emaMInd.valueBack(memory).getValue()){
        	signal1 -= 1;
        }else if(emaSInd.lastValue().getValue() < emaSInd.valueBack(memory).getValue() 
        		&& emaMInd.lastValue().getValue() < emaMInd.valueBack(memory).getValue()){
        	signal1 -= -1;
        }
        if(emaMInd.lastValue().getValue() > emaMInd.valueBack(memory).getValue() 
        		&& emaLInd.lastValue().getValue() > emaLInd.valueBack(memory).getValue()){
        	signal1 += 1;
        }else if(emaMInd.lastValue().getValue() < emaMInd.valueBack(memory).getValue() 
        		&& emaLInd.lastValue().getValue() < emaLInd.valueBack(memory).getValue()){
        	signal1 += -1;
        }
        score += Math.signum(signal1);
        
        //Signal Strength
        double signal2 = 0;
        if(isMonotone(emaSInd, memory, true)){
        	signal2 -= 1;
        }else if(isMonotone(emaSInd, memory, false)){
        	signal2 -= -1;
        }
        if(isMonotone(emaLInd, memory, true)){
        	signal2 += 1;
        }else if(isMonotone(emaLInd, memory, false)){
        	signal2 += -1;
        }
        score += Math.signum(signal2);
        
        //Signal Acceleration
        double signal3 = 0;
        if((emaSInd.lastValue().getValue() - emaMInd.lastValue().getValue()) > (emaSInd.valueBack(1).getValue() - emaMInd.valueBack(1).getValue())
        		&& emaSInd.lastValue().getValue() > emaSInd.valueBack(1).getValue()){
        	signal3 -= 1;
        }else if((emaSInd.lastValue().getValue() - emaMInd.lastValue().getValue()) < (emaSInd.valueBack(1).getValue() - emaMInd.valueBack(1).getValue())
        		&& emaSInd.lastValue().getValue() < emaSInd.valueBack(1).getValue()){
        	signal3 -= -1;
        }
        if((emaMInd.lastValue().getValue() - emaLInd.lastValue().getValue()) > (emaMInd.valueBack(1).getValue() - emaLInd.valueBack(1).getValue())
        		&& emaMInd.lastValue().getValue() > emaMInd.valueBack(1).getValue()){
        	signal3 -= 1;
        }else if((emaMInd.lastValue().getValue() - emaLInd.lastValue().getValue()) < (emaMInd.valueBack(1).getValue() - emaLInd.valueBack(1).getValue())
        		&& emaMInd.lastValue().getValue() < emaMInd.valueBack(1).getValue()){
        	signal3 -= -1;
        }
        score += Math.signum(signal3);
        
        //Signal Diff
        double signal4 = 0;
        if(stoSInd.lastValue().getValue() > stoMInd.lastValue().getValue()){
        	signal4 -= 1;
        }else if(stoSInd.lastValue().getValue() < stoMInd.lastValue().getValue()){
        	signal4 -= -1;
        }
        if(stoMInd.lastValue().getValue() > stoLInd.lastValue().getValue()){
        	signal4 -= 1;
        }else if(stoMInd.lastValue().getValue() < stoLInd.lastValue().getValue()){
        	signal4 -= -1;
        }
        score += Math.signum(signal4);
        
        //Signal Strength
        double signal5 = 0;
        if(isMonotone(stoSInd, memory, true)){
        	signal5 -= 1;
        }else if(isMonotone(stoSInd, memory, false)){
        	signal5 -= -1;
        }
        if(isMonotone(stoMInd, memory, true)){
        	signal5 -= 1;
        }else if(isMonotone(stoMInd, memory, false)){
        	signal5 -= -1;
        }
        if(isMonotone(stoLInd, memory, true)){
        	signal5 -= 1;
        }else if(isMonotone(stoLInd, memory, false)){
        	signal5 -= -1;
        }
        score += Math.signum(signal5);
        
        //Signal OverBought/OverSold
        double signal6 = 0;
        if(stoSInd.lastValue().getValue() <= stoLevel){
        	signal6 += 1;
        }else if(stoSInd.lastValue().getValue() >= 100. - stoLevel){
        	signal6 += -1;
        }
        if(stoMInd.lastValue().getValue() <= stoLevel){
        	signal6 += 1;
        }else if(stoMInd.lastValue().getValue() >= 100. - stoLevel){
        	signal6 += -1;
        }
        score += Math.signum(signal6);        
        
        return score;
    }
	
	private static boolean isMonotone(IIndicator<Double> ind, int memory, boolean longShort) {
		for(int i = 0; i < memory; i++){
            if(longShort){
                if(ind.valueBack(i).getValue() <= ind.valueBack(i + 1).getValue()){
                    return false;
                }
            }else{
                if(ind.valueBack(i).getValue() >= ind.valueBack(i + 1).getValue()){
                    return false;
                }
            }
        }
        return true;
	}
	
	public IDataServer[] getDataServer(ITicker ticker){
		return this.p_dbServers.get(ticker);
	}
}
