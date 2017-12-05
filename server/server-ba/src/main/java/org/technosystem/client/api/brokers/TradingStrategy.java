package org.technosystem.client.api.brokers;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.FlowLayout;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.Spring;
import javax.swing.SpringLayout;

import org.technosystem.modules.marketdata.assets.ITicker;
import org.technosystem.modules.marketdata.assets.Ticker;
import org.technosystem.modules.marketdata.quote.IQuote;
import org.technosystem.modules.marketdata.quote.Quote;
import org.technosystem.platform.api.analysis.Analysis;
import org.technosystem.platform.api.analysis.IAnalysis;
import org.technosystem.platform.api.analysis.IPrediction;
import org.technosystem.util.Parameters;
import org.neurosystem.util.basic.HasTime;
import org.neurosystem.util.log.Priority;
import org.neurosystem.util.misc.Pair;
import org.neurosystem.util.time.IPeriod;

import com.dukascopy.api.*;
import com.google.common.collect.Maps;

@RequiresFullAccess
@Library("server-ba-2.4.3.jar;guava-16.0.1.jar;")
public class TradingStrategy implements IStrategy {

	private final Map<Instrument, ITicker> p_tickers;
	private final IAnalysis p_analysis;
	protected final Instrument p_refInstrument;
	private final int p_pattern = 6;
	
	private IContext p_context;
	private IConsole p_console;
	private IHistory p_history;
	
	private SimpleDateFormat gmtSdf = new SimpleDateFormat("dd-MM-yy HH:mm:ss");
    
    /**
	 * The main frame
	 */
	private JFrame mFrame;
	
	private Map<Instrument, FieldEntry> mWatchList = Maps.newHashMap();
	
	private class FieldEntry
	{
    	private final JTextField m_Name;
        private final JTextField m_MidPz;
        
        private final JTextField m_MinPz;
        private final JTextField m_MaxPz;
        private final List<JTextField> m_PatternPz;
        
        private final JTextField m_Ema;
        private final JTextField m_Rsi;
        private final JTextField m_Scr;
        
        private FieldEntry(String name){
        	m_Name = new JTextField(name);
        	m_MidPz = new JTextField("00.00000");
        	
        	m_MinPz = new JTextField("00.00000");
        	m_MaxPz = new JTextField("00.00000");
        	m_PatternPz = new ArrayList<>();
        	for(int i = 0; i < p_pattern; i++){
        		m_PatternPz.add(new JTextField("00.00000"));
        	}
        	
        	m_Ema = new JTextField("000.00");
        	m_Rsi = new JTextField("000.00");
        	
        	m_Scr = new JTextField("000.00");
        }

		public JTextField getName() {
			return m_Name;
		}

		public JTextField getMidPz() {
			return m_MidPz;
		}

		public JTextField getMinPz() {
			return m_MinPz;
		}

		public JTextField getMaxPz() {
			return m_MaxPz;
		}
		
		public List<JTextField> getPatternPz() {
			return m_PatternPz;
		}

		public JTextField getEMA() {
			return m_Ema;
		}

		public JTextField getRSI() {
			return m_Rsi;
		}		

		public JTextField getScore() {
			return m_Scr;
		}

		public void update(Instrument instrument, double barMID, IPrediction prediction) {
			DecimalFormat formatPz = new DecimalFormat("#0.0000");
			getMidPz().setText(formatPz.format(barMID));
			
			//Patterns
			List<IQuote> prices = prediction.getForecast();
			double curP = prices.get(0).getClose();
            double maxP = curP;
            double minP = curP;
			for(int i = 0; i < prices.size(); i++){
            	maxP = Math.max(maxP, prices.get(i).getClose());
            	minP = Math.min(minP, prices.get(i).getClose());
				getPatternPz().get(i).setText(formatPz.format(prices.get(i).getClose()));
			}
			getMinPz().setText(formatPz.format(minP));
			getMaxPz().setText(formatPz.format(maxP));
			
			//Screen
			double rsi = (barMID - minP) /(maxP - minP);
    		getMinPz().setBackground(Color.lightGray);
    		getMaxPz().setBackground(Color.lightGray);
    		getMidPz().setBackground(Color.lightGray);
			if(maxP > minP + 50 * instrument.getPipValue()){
            	if(rsi > 0.7){
            		getMinPz().setBackground(new Color(146, 205, 0));
            		getMaxPz().setBackground(new Color(255, 113, 126));
            		
            		getMidPz().setBackground(new Color(255, 113, 126));
            	}else if(rsi < 0.3){  
            		getMinPz().setBackground(new Color(146, 205, 0));
            		getMaxPz().setBackground(new Color(255, 113, 126));
            		
            		getMidPz().setBackground(new Color(146, 205, 0));
            	}
            }
			
			//Signal
			double scEMA = prediction.getStrength();
			double scRSI = prediction.getStochastic();
			formatPz = new DecimalFormat("#0.00");
			getEMA().setText(formatPz.format(scEMA));
			getRSI().setText(formatPz.format(scRSI));
			
			//Screen
			if(scEMA >= 2){
				getEMA().setBackground(new Color(146, 205, 0));
			}else if(scEMA <= -2){
				getEMA().setBackground(new Color(255, 113, 126));
			}else{
				getEMA().setBackground(Color.lightGray);
			}
			
			//Screen
			if(scRSI <= 25){
				getRSI().setBackground(new Color(146, 205, 0));
			}else if(scRSI >= 75){
				getRSI().setBackground(new Color(255, 113, 126));
			}else{
				getRSI().setBackground(Color.lightGray);
			}
			
			double score = 0.0;
            for(double dataScore:prediction.getDataScore()){
            	score += Math.round(100.0*dataScore*100.0)/100.0; 
            }
            getScore().setText(formatPz.format(score));
            
            //Screen
			if(score >= 0.7){
				getScore().setBackground(new Color(146, 205, 0));
			}else if(score <= -0.7){
				getScore().setBackground(new Color(255, 113, 126));
			}else{
				getScore().setBackground(Color.lightGray);
			}
			
			//Buy-Sell
			if((maxP > minP + 50 * instrument.getPipValue()) && (rsi > 0.7) && (scRSI >= 75) && score <= 0){//Sell
				getName().setBackground(new Color(255, 113, 126));
			} else if((maxP > minP + 50 * instrument.getPipValue()) && (rsi < 0.3) && (scRSI <= 25) && score >= 0){//Buy
				getName().setBackground(new Color(146, 205, 0));
			} else{
				getName().setBackground(Color.lightGray);
			}
		}		
    }
	
	/**
	 * Place holder of all the market watcher
	 * 
	 * @return the map
	 */
	private Map<Instrument, FieldEntry> getWatchList()
	{
		return mWatchList;
	}
	
	@SuppressWarnings("unchecked")
	public TradingStrategy() {
		this.p_tickers = new HashMap<>();
		this.p_refInstrument = Instrument.EURUSD;
		
		//EUR
		Instrument eurusdI = Instrument.EURUSD;
		ITicker eurusdT = new Ticker(0.008, eurusdI.toString(), eurusdI.getPipValue(), 0.0001);
		this.p_tickers.put(eurusdI, eurusdT);
		this.mWatchList.put(eurusdI, new FieldEntry(eurusdI.name()));
		
		Instrument eurgbpI = Instrument.EURGBP;
		ITicker eurgbpT = new Ticker(0.008, eurgbpI.toString(), eurgbpI.getPipValue(), 0.0001);
		this.p_tickers.put(eurgbpI, eurgbpT);
		this.mWatchList.put(eurgbpI, new FieldEntry(eurgbpI.name()));
		
		Instrument eurchfI = Instrument.EURCHF;
		ITicker eurchfT = new Ticker(0.008, eurchfI.toString(), eurchfI.getPipValue(), 0.0001);
		this.p_tickers.put(eurchfI, eurchfT);
		this.mWatchList.put(eurchfI, new FieldEntry(eurchfI.name()));
		
		Instrument euraudI = Instrument.EURAUD;
		ITicker euraudT = new Ticker(0.008, euraudI.toString(), euraudI.getPipValue(), 0.0001);
		this.p_tickers.put(euraudI, euraudT);
		this.mWatchList.put(euraudI, new FieldEntry(euraudI.name()));
		
		Instrument eurnzdI = Instrument.EURNZD;
		ITicker eurnzdT = new Ticker(0.008, eurnzdI.toString(), eurnzdI.getPipValue(), 0.0001);
		this.p_tickers.put(eurnzdI, eurnzdT);
		this.mWatchList.put(eurnzdI, new FieldEntry(eurnzdI.name()));
		
		Instrument eurcadI = Instrument.EURCAD;
		ITicker eurcadT = new Ticker(0.008, eurcadI.toString(), eurcadI.getPipValue(), 0.0001);
		this.p_tickers.put(eurcadI, eurcadT);
		this.mWatchList.put(eurcadI, new FieldEntry(eurcadI.name()));
		
		Instrument eurjpyI = Instrument.EURJPY;
		ITicker eurjpyT = new Ticker(0.008, eurjpyI.toString(), eurjpyI.getPipValue(), 0.0001);
		this.p_tickers.put(eurjpyI, eurjpyT);
		this.mWatchList.put(eurjpyI, new FieldEntry(eurjpyI.name()));
		
		//GBP
		Instrument gbpusdI = Instrument.GBPUSD;
		ITicker gbpusdT = new Ticker(0.007, gbpusdI.toString(), gbpusdI.getPipValue(), 0.0001);
		this.p_tickers.put(gbpusdI, gbpusdT);
		this.mWatchList.put(gbpusdI, new FieldEntry(gbpusdI.name()));
		
		Instrument gbpchfI = Instrument.GBPCHF;
		ITicker gbpchfT = new Ticker(0.007, gbpchfI.toString(), gbpchfI.getPipValue(), 0.0001);
		this.p_tickers.put(gbpchfI, gbpchfT);
		this.mWatchList.put(gbpchfI, new FieldEntry(gbpchfI.name()));
		
		Instrument gbpaudI = Instrument.GBPAUD;
		ITicker gbpaudT = new Ticker(0.007, gbpaudI.toString(), gbpaudI.getPipValue(), 0.0001);
		this.p_tickers.put(gbpaudI, gbpaudT);
		this.mWatchList.put(gbpaudI, new FieldEntry(gbpaudI.name()));
		
		Instrument gbpnzdI = Instrument.GBPNZD;
		ITicker gbpnzdT = new Ticker(0.007, gbpnzdI.toString(), gbpnzdI.getPipValue(), 0.0001);
		this.p_tickers.put(gbpnzdI, gbpnzdT);
		this.mWatchList.put(gbpnzdI, new FieldEntry(gbpnzdI.name()));
		
		Instrument gbpcadI = Instrument.GBPCAD;
		ITicker gbpcadT = new Ticker(0.007, gbpcadI.toString(), gbpcadI.getPipValue(), 0.0001);
		this.p_tickers.put(gbpcadI, gbpcadT);
		this.mWatchList.put(gbpcadI, new FieldEntry(gbpcadI.name()));
		
		Instrument gbpjpyI = Instrument.GBPJPY;
		ITicker gbpjpyT = new Ticker(0.007, gbpjpyI.toString(), gbpjpyI.getPipValue(), 0.0001);
		this.p_tickers.put(gbpjpyI, gbpjpyT);
		this.mWatchList.put(gbpjpyI, new FieldEntry(gbpjpyI.name()));
		
		//AUD
		Instrument audusdI = Instrument.AUDUSD;
		ITicker audusdT = new Ticker(0.013, audusdI.toString(), audusdI.getPipValue(), 0.0001);
		this.p_tickers.put(audusdI, audusdT);
		this.mWatchList.put(audusdI, new FieldEntry(audusdI.name()));
		
		Instrument audchfI = Instrument.AUDCHF;
		ITicker audchfT = new Ticker(0.013, audchfI.toString(), audchfI.getPipValue(), 0.0001);
		this.p_tickers.put(audchfI, audchfT);
		this.mWatchList.put(audchfI, new FieldEntry(audchfI.name()));
		
		Instrument audnzdI = Instrument.AUDNZD;
		ITicker audnzdT = new Ticker(0.013, audnzdI.toString(), audnzdI.getPipValue(), 0.0001);
		this.p_tickers.put(audnzdI, audnzdT);
		this.mWatchList.put(audnzdI, new FieldEntry(audnzdI.name()));
		
		Instrument audcadI = Instrument.AUDCAD;
		ITicker audcadT = new Ticker(0.013, audcadI.toString(), audcadI.getPipValue(), 0.0001);
		this.p_tickers.put(audcadI, audcadT);
		this.mWatchList.put(audcadI, new FieldEntry(audcadI.name()));
		
		Instrument audjpyI = Instrument.AUDJPY;
		ITicker audjpyT = new Ticker(0.013, audjpyI.toString(), audjpyI.getPipValue(), 0.0001);
		this.p_tickers.put(audjpyI, audjpyT);
		this.mWatchList.put(audjpyI, new FieldEntry(audjpyI.name()));
		
		//NZD
		Instrument nzdusdI = Instrument.NZDUSD;
		ITicker nzdusdT = new Ticker(0.014, nzdusdI.toString(), nzdusdI.getPipValue(), 0.0001);
		this.p_tickers.put(nzdusdI, nzdusdT);
		this.mWatchList.put(nzdusdI, new FieldEntry(nzdusdI.name()));
		
		Instrument nzdchfI = Instrument.NZDCHF;
		ITicker nzdchfT = new Ticker(0.014, nzdchfI.toString(), nzdchfI.getPipValue(), 0.0001);
		this.p_tickers.put(nzdchfI, nzdchfT);
		this.mWatchList.put(nzdchfI, new FieldEntry(nzdchfI.name()));
		
		Instrument nzdcadI = Instrument.NZDCAD;
		ITicker nzdcadT = new Ticker(0.014, nzdcadI.toString(), nzdcadI.getPipValue(), 0.0001);
		this.p_tickers.put(nzdcadI, nzdcadT);
		this.mWatchList.put(nzdcadI, new FieldEntry(nzdcadI.name()));
		
		Instrument nzdjpyI = Instrument.NZDJPY;
		ITicker nzdjpyT = new Ticker(0.014, nzdjpyI.toString(), nzdjpyI.getPipValue(), 0.0001);
		this.p_tickers.put(nzdjpyI, nzdjpyT);
		this.mWatchList.put(nzdjpyI, new FieldEntry(nzdjpyI.name()));
		
		//USD
		Instrument usdcadI = Instrument.USDCAD;
		ITicker usdcadT = new Ticker(0.01, usdcadI.toString(), usdcadI.getPipValue(), 0.0001);
		this.p_tickers.put(usdcadI, usdcadT);
		this.mWatchList.put(usdcadI, new FieldEntry(usdcadI.name()));
		
		Instrument usdchfI = Instrument.USDCHF;
		ITicker usdchfT = new Ticker(0.01, usdchfI.toString(), usdchfI.getPipValue(), 0.0001);
		this.p_tickers.put(usdchfI, usdchfT);
		this.mWatchList.put(usdchfI, new FieldEntry(usdchfI.name()));
		
		Instrument usdjpyI = Instrument.USDJPY;
		ITicker usdjpyT = new Ticker(0.01, usdjpyI.toString(), usdjpyI.getPipValue(), 0.0001);
		this.p_tickers.put(usdjpyI, usdjpyT);
		this.mWatchList.put(usdjpyI, new FieldEntry(usdjpyI.name()));
		
		//CAD
		Instrument cadchfI = Instrument.CADCHF;
		ITicker cadchfT = new Ticker(0.013, cadchfI.toString(), cadchfI.getPipValue(), 0.0001);
		this.p_tickers.put(cadchfI, cadchfT);
		this.mWatchList.put(cadchfI, new FieldEntry(cadchfI.name()));
		
		Instrument cadjpyI = Instrument.CADJPY;
		ITicker cadjpyT = new Ticker(0.013, cadjpyI.toString(), cadjpyI.getPipValue(), 0.0001);
		this.p_tickers.put(cadjpyI, cadjpyT);
		this.mWatchList.put(cadjpyI, new FieldEntry(cadjpyI.name()));
		
		//CHF
		Instrument chfjpyI = Instrument.CHFJPY;
		ITicker chfjpyT = new Ticker(0.009, chfjpyI.toString(), chfjpyI.getPipValue(), 0.0001);
		this.p_tickers.put(chfjpyI, chfjpyT);
		this.mWatchList.put(chfjpyI, new FieldEntry(chfjpyI.name()));
		
		List<ITicker> tickers = Arrays.asList(
				eurusdT, eurgbpT, eurchfT, euraudT, eurnzdT, eurcadT, eurjpyT,
				gbpusdT, gbpchfT, gbpaudT, gbpnzdT, gbpcadT, gbpjpyT,
				audusdT, audchfT, audnzdT, audcadT, audjpyT,
				nzdusdT, nzdchfT, nzdcadT, nzdjpyT,
				usdcadT, usdchfT, usdjpyT,
				cadchfT, cadjpyT,
				chfjpyT);
		Pair<IPeriod, IPeriod>[] dbPeriods = (Pair<IPeriod, IPeriod>[]) Parameters.getDBPeriods().toArray(new Pair<?, ?>[Parameters.getDBPeriods().size()]);
		Object[] dbParams = {new Object[]{new int[]{5, 14, 30}, 3, 35.}, new int[]{6, 6}, new Object[]{14, new int[]{6, 12, 4*12, 12*12}}};
		
		this.p_analysis = new Analysis(tickers, dbPeriods, dbParams);
	}
	
	protected Instrument getRefInstrument() {
		return this.p_refInstrument;
	}
	
	public void log(String message, Priority level) {
		p_console.getOut().println(message);
	}
	
	private void loadHistoricalData() {
		try {
			List<Pair<IPeriod, IPeriod>> periods = Parameters.getDBPeriods();
	
			// Total history
			long timeBack = 0L;
			for (Pair<IPeriod, IPeriod> pair : periods) {
				timeBack += pair.getValue().getInterval();
			}

			long startTime = this.p_history.getStartTimeOfCurrentBar(getRefInstrument(), 
					Period.valueOf(periods.get(0).getKey().name())) - timeBack;
			
			log("Loading Quotes... " + this.p_tickers.size() +", Time: " + HasTime.formatedTime(startTime), Priority.INFO);
			long timeB = System.currentTimeMillis();
			
			for (Pair<IPeriod, IPeriod> pair : periods) {
				Period period = Period.valueOf(pair.getKey().name());
				long endTime = Math.min(startTime + pair.getValue().getInterval(), this.p_history.getStartTimeOfCurrentBar(getRefInstrument(), 
	                    period));
				print("Time: " + HasTime.formatedTime(startTime) + "; " + HasTime.formatedTime(endTime));
				List<IBar> refBars = this.p_history.getBars(getRefInstrument(), period, OfferSide.BID, Filter.WEEKENDS,
						startTime, endTime);
				for (int i = 0; i < refBars.size(); i++) {
					long barTime = refBars.get(i).getTime();
					IBar bar = refBars.get(i);
					
					if (HasTime.isTradingTime(barTime) && (bar.getHigh() > bar.getLow())) {
						for (Iterator<Entry<Instrument, ITicker>> it = this.p_tickers
								.entrySet().iterator(); it.hasNext();) {
							Entry<Instrument, ITicker> entry = it.next();
							Instrument instrument = entry.getKey();
							ITicker ticker = entry.getValue();

							List<IBar> bars = this.p_history.getBars(instrument, period, OfferSide.BID, Filter.WEEKENDS, 1,
									barTime, 0);
							IQuote quote = barToQuote(bars.get(0));
							this.p_analysis.onValue(pair.getKey(), ticker, quote);
						}
					}
				}
				
				startTime = endTime + pair.getKey().getInterval();
			}

			long timeA = System.currentTimeMillis();
			log("Done loading... " + Math.round((timeA - timeB) / 1000) + "s", Priority.INFO);
		} catch (Exception e) {
			log("Load historical data error: " + e, Priority.ERROR);
		}
    }
	
	protected IQuote barToQuote(IBar bar) {
		return new Quote(bar.getTime(), 
				bar.getOpen(), bar.getHigh(), bar.getLow(), bar.getClose());
	}
	
	@Override
	public void onStart(IContext context) throws JFException {
		this.p_context = context;
		this.p_console = context.getConsole();
		this.p_history = context.getHistory();
		
		mFrame = new JFrame("MktWatcher");
        final JPanel compsToExperiment = new JPanel();
        FlowLayout experimentLayout = new FlowLayout(FlowLayout.CENTER, 20, 5);
        compsToExperiment.setLayout(experimentLayout);
        experimentLayout.setAlignment(FlowLayout.TRAILING);

        //Create the panel and populate it.
        JPanel panel = new JPanel(new SpringLayout());
        for(FieldEntry entry:getWatchList().values()){
        	panel.add(entry.getName());entry.getName().setEditable(false);
        	panel.add(entry.getMidPz());entry.getMidPz().setEditable(false);
        	
        	panel.add(entry.getMinPz());entry.getMinPz().setEditable(false);
        	panel.add(entry.getMaxPz());entry.getMaxPz().setEditable(false);
        	for(JTextField field:entry.getPatternPz()){
        		panel.add(field);field.setEditable(false);
        	}
        	
        	panel.add(entry.getEMA());entry.getEMA().setEditable(false);
        	panel.add(entry.getRSI());entry.getRSI().setEditable(false);
        	panel.add(entry.getScore());entry.getScore().setEditable(false);
        }
        
        //Lay out the panel.
        makeGrid(panel, getWatchList().size(), 7 + p_pattern, 5, 5, 5, 5);
        
        //Set up the content pane.
        panel.setOpaque(true); //content panes must be opaque
        compsToExperiment.add(panel);
        
        JScrollPane jScrollPane = new JScrollPane(compsToExperiment);
	     // only a configuration to the jScrollPane...
	     jScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
	     jScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
     
        mFrame.getContentPane().add(jScrollPane, BorderLayout.CENTER);

        //Display the window.
        mFrame.setSize(800, 800);//mFrame.pack();
        mFrame.setVisible(true);
        
		loadHistoricalData();
	}
	
	@Override
	public void onBar(Instrument inInstrument, Period inPeriod, IBar askBar, IBar bidBar) throws JFException {
		long barTime = bidBar.getTime();
		if (HasTime.isTradingTime(barTime) && (bidBar.getHigh() > bidBar.getLow())) {
			if(inPeriod.name().equals(Parameters.getBarPeriod().name())){
				/*for (Iterator<Entry<Instrument, ITicker>> it = this.p_tickers
						.entrySet().iterator(); it.hasNext();) {
					Entry<Instrument, ITicker> entry = it.next();
					Instrument instrument = entry.getKey();
					ITicker ticker = entry.getValue();

					List<IBar> bars = this.p_history.getBars(instrument, inPeriod, OfferSide.BID, Filter.WEEKENDS, 1,
							barTime, 0);
					IQuote quote = barToQuote(bars.get(0));

					tickersPrice.put(ticker, quote);
				}*/
				List<IBar> bars = this.p_history.getBars(inInstrument, inPeriod, OfferSide.BID, Filter.WEEKENDS, 1,
						barTime, 0);
				IQuote quote = barToQuote(bars.get(0));
				this.p_analysis.onValue(Parameters.getBarPeriod(), this.p_tickers.get(inInstrument), quote);
			
				for (Iterator<Entry<Instrument, ITicker>> it = this.p_tickers.entrySet().iterator(); it.hasNext();){
					Entry<Instrument, ITicker> entry = it.next();
					Instrument instrument = entry.getKey();
					ITicker ticker = entry.getValue();
					
					IPrediction prediction = this.p_analysis.getPrediction(ticker);
					
					IBar barBid = this.p_history.getBars(instrument, inPeriod, OfferSide.BID, Filter.WEEKENDS, 1, barTime,0).get(0);
	                IBar barAsk = this.p_history.getBars(instrument, inPeriod, OfferSide.BID, Filter.WEEKENDS, 1, barTime,0).get(0);
					double barMID = ticker.roundPZ((barBid.getClose() + barAsk.getClose()) / 2.0);
					this.mWatchList.get(instrument).update(instrument, barMID, prediction);
				}
			}
		}
	}
	
    private void print(Object o) {
        p_console.getOut().println(o);
    }
    
	@Override
	public void onMessage(IMessage message) throws JFException {
		if (message != null){
            if(message.getType() == IMessage.Type.ORDER_CLOSE_OK){
                IOrder lastOne = message.getOrder();
                print("Order: "+lastOne.getLabel()+ "  "+ lastOne.getInstrument()+ "  "+ lastOne.getOrderCommand()
                                    + " Pips: " + lastOne.getProfitLossInPips()
                                    + " PnL: " + lastOne.getProfitLossInUSD() 
                                    + " LV: " + p_context.getAccount().getUseOfLeverage()
                                    + " CloseP: "+lastOne.getClosePrice() 
                                    + " OpenP: "+lastOne.getOpenPrice() 
                                    + " CloseT: " + gmtSdf.format(new Date(lastOne.getCloseTime())) 
                                    + " OpenT: " + gmtSdf.format(new Date(lastOne.getFillTime()))
                                    + " pipVal: " + lastOne.getInstrument().getPipValue() 
                                    + lastOne.getComment());
            }
                                
        }
	}

	@Override
	public void onTick(Instrument instrument, ITick tick) throws JFException {
	}
	
	@Override
	public void onAccount(IAccount account) throws JFException {
	}

	@Override
	public void onStop() throws JFException {
        if(mFrame != null){
        	mFrame.dispose();
        }
	}
	
	public static void makeGrid(Container parent, int rows, int cols, int initialX, int initialY, int xPad, int yPad) {
		SpringLayout layout;
		try {
			layout = (SpringLayout) parent.getLayout();
		} catch (ClassCastException exc) {
			System.err.println("The first argument to makeGrid must use SpringLayout.");
			return;
		}

		Spring xPadSpring = Spring.constant(xPad);
		Spring yPadSpring = Spring.constant(yPad);
		Spring initialXSpring = Spring.constant(initialX);
		Spring initialYSpring = Spring.constant(initialY);
		int max = rows * cols;

		// Calculate Springs that are the max of the width/height so that all
		// cells have the same size.
		Spring maxWidthSpring = layout.getConstraints(parent.getComponent(0)).getWidth();
		Spring maxHeightSpring = layout.getConstraints(parent.getComponent(0)).getHeight();
		for (int i = 1; i < max; i++) {
			SpringLayout.Constraints cons = layout.getConstraints(parent.getComponent(i));

			maxWidthSpring = Spring.max(maxWidthSpring, cons.getWidth());
			maxHeightSpring = Spring.max(maxHeightSpring, cons.getHeight());
		}

		// Apply the new width/height Spring. This forces all the
		// components to have the same size.
		for (int i = 0; i < max; i++) {
			SpringLayout.Constraints cons = layout.getConstraints(parent.getComponent(i));

			cons.setWidth(maxWidthSpring);
			cons.setHeight(maxHeightSpring);
		}

		// Then adjust the x/y constraints of all the cells so that they
		// are aligned in a grid.
		SpringLayout.Constraints lastCons = null;
		SpringLayout.Constraints lastRowCons = null;
		for (int i = 0; i < max; i++) {
			SpringLayout.Constraints cons = layout.getConstraints(parent.getComponent(i));
			if (i % cols == 0) { // start of new row
				lastRowCons = lastCons;
				cons.setX(initialXSpring);
			} else { // x position depends on previous component
				cons.setX(Spring.sum(lastCons.getConstraint(SpringLayout.EAST), xPadSpring));
			}

			if (i / cols == 0) { // first row
				cons.setY(initialYSpring);
			} else { // y position depends on previous row
				cons.setY(Spring.sum(lastRowCons.getConstraint(SpringLayout.SOUTH), yPadSpring));
			}
			lastCons = cons;
		}

		// Set the parent's size.
		SpringLayout.Constraints pCons = layout.getConstraints(parent);
		pCons.setConstraint(SpringLayout.SOUTH,
				Spring.sum(Spring.constant(yPad), lastCons.getConstraint(SpringLayout.SOUTH)));
		pCons.setConstraint(SpringLayout.EAST,
				Spring.sum(Spring.constant(xPad), lastCons.getConstraint(SpringLayout.EAST)));
	}
}