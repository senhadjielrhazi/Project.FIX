package org.technosystem.client.api.brokers.pieces;

import java.util.*;
import java.util.Map.Entry;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.FlowLayout;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;

import com.dukascopy.api.*;
import com.dukascopy.api.IEngine.OrderCommand;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.Spring;
import javax.swing.SpringLayout;

@RequiresFullAccess
public class TripleScreen implements IStrategy {
    private IEngine engine;
    private IConsole console;
    private IHistory history;
    private IContext context;
    private final int threadNum = 10;
    private ExecutorService executor;
    private final int p_pattern = 6;
    private Object lock = new Object();
    
    private Map<Instrument, PatternObject> m_InstrumentParams = new HashMap<>();
    private SimpleDateFormat gmtSdf = new SimpleDateFormat("dd-MM-yy HH:mm:ss");
    
    private String m_TradeLabel = "TTSEMA";
    private int m_Slippage = 5;
    private int m_Tradenb = 0;
    
    /**
	 * The main frame
	 */
	private JFrame mFrame;
	
	private Map<Instrument, FieldEntry> mWatchList = new HashMap<>();
	
	private class FieldEntry
	{
    	private final JTextField m_Name;
        
        private final JTextField m_MinPz;
        private final JTextField m_MaxPz;
        private final List<JTextField> m_PatternPz;
        
        private FieldEntry(String name){
        	m_Name = new JTextField(name);
        	
        	m_MinPz = new JTextField("00.00000");
        	m_MaxPz = new JTextField("00.00000");
        	m_PatternPz = new ArrayList<>();
        	for(int i = 0; i < p_pattern + 1; i++){
        		m_PatternPz.add(new JTextField("00.00000"));
        	}
        }

		public JTextField getName() {
			return m_Name;
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

		public void update(Instrument instrument, List<Double> prices) {
			DecimalFormat formatPz = new DecimalFormat("#0.0000");

			//Patterns
			double barMID = prices.get(0);
            double maxP = barMID;
            double minP = barMID;
			for(int i = 0; i < prices.size(); i++){
            	maxP = Math.max(maxP, prices.get(i));
            	minP = Math.min(minP, prices.get(i));
				getPatternPz().get(i).setText(formatPz.format(prices.get(i)));
			}
			getMinPz().setText(formatPz.format(minP));
			getMaxPz().setText(formatPz.format(maxP));
			
			//Screen
			double rsi = (barMID - minP) /(maxP - minP);
    		getMinPz().setBackground(Color.lightGray);
    		getMaxPz().setBackground(Color.lightGray);
			if(maxP > minP + 50 * instrument.getPipValue()){
            	if(rsi > 0.7){
            		getMinPz().setBackground(new Color(146, 205, 0));
            		getMaxPz().setBackground(new Color(255, 113, 126));
            	}else if(rsi < 0.3){  
            		getMinPz().setBackground(new Color(146, 205, 0));
            		getMaxPz().setBackground(new Color(255, 113, 126));
            	}
            }
			
			//Buy-Sell
			if((maxP > minP + 50 * instrument.getPipValue()) && (rsi > 0.7)){//Sell
				getName().setBackground(new Color(255, 113, 126));
			} else if((maxP > minP + 50 * instrument.getPipValue()) && (rsi < 0.3)){//Buy
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
	
    @SuppressWarnings("unused")
	public void onStart(IContext context) throws JFException {
        this.engine = context.getEngine();
        this.console = context.getConsole();
        this.history = context.getHistory();
        this.context = context;
        
        gmtSdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        
        // Prepare to execute multi-thread
        executor = Executors.newFixedThreadPool(threadNum);
        
        PatternObject AUDCAD = new PatternObject(Instrument.AUDCAD, 4, 0.01);
        PatternObject AUDCHF = new PatternObject(Instrument.AUDCHF, 4, 0.01);
        PatternObject AUDJPY = new PatternObject(Instrument.AUDJPY, 4, 0.01);
        PatternObject AUDNZD = new PatternObject(Instrument.AUDNZD, 4, 0.01);
        PatternObject AUDUSD = new PatternObject(Instrument.AUDUSD, 4, 0.01);
        PatternObject CADCHF = new PatternObject(Instrument.CADCHF, 4, 0.01);
        PatternObject CADJPY = new PatternObject(Instrument.CADJPY, 4, 0.01);
        PatternObject CHFJPY = new PatternObject(Instrument.CHFJPY, 4, 0.009);
        PatternObject EURAUD = new PatternObject(Instrument.EURAUD, 4, 0.008);
        PatternObject EURCAD = new PatternObject(Instrument.EURCAD, 4, 0.008);
        PatternObject EURCHF = new PatternObject(Instrument.EURCHF, 4, 0.008);
        PatternObject EURGBP = new PatternObject(Instrument.EURGBP, 4, 0.008);
        PatternObject EURJPY = new PatternObject(Instrument.EURJPY, 4, 0.008);
        PatternObject EURNZD = new PatternObject(Instrument.EURNZD, 4, 0.008);
        PatternObject EURUSD = new PatternObject(Instrument.EURUSD, 4, 0.008);
        PatternObject GBPAUD = new PatternObject(Instrument.GBPAUD, 4, 0.006);
        PatternObject GBPCAD = new PatternObject(Instrument.GBPCAD, 4, 0.006);
        PatternObject GBPCHF = new PatternObject(Instrument.GBPCHF, 4, 0.006);
        PatternObject GBPJPY = new PatternObject(Instrument.GBPJPY, 4, 0.006);
        PatternObject GBPNZD = new PatternObject(Instrument.GBPNZD, 4, 0.006);
        PatternObject GBPUSD = new PatternObject(Instrument.GBPUSD, 4, 0.006);
        PatternObject NZDCAD = new PatternObject(Instrument.NZDCAD, 4, 0.012);
        PatternObject NZDCHF = new PatternObject(Instrument.NZDCHF, 4, 0.012);
        PatternObject NZDJPY = new PatternObject(Instrument.NZDJPY, 4, 0.012);
        PatternObject NZDUSD = new PatternObject(Instrument.NZDUSD, 4, 0.012);
        PatternObject USDCAD = new PatternObject(Instrument.USDCAD, 4, 0.01);
        PatternObject USDCHF = new PatternObject(Instrument.USDCHF, 4, 0.01);
        PatternObject USDJPY = new PatternObject(Instrument.USDJPY, 4, 0.01);
        
        mFrame = new JFrame("MktWatcher");
        final JPanel compsToExperiment = new JPanel();
        FlowLayout experimentLayout = new FlowLayout(FlowLayout.CENTER, 20, 5);
        compsToExperiment.setLayout(experimentLayout);
        experimentLayout.setAlignment(FlowLayout.TRAILING);

        //Create the panel and populate it.
        JPanel panel = new JPanel(new SpringLayout());
        for(FieldEntry entry:getWatchList().values()){
        	panel.add(entry.getName());entry.getName().setEditable(false);
        	
        	panel.add(entry.getMinPz());entry.getMinPz().setEditable(false);
        	panel.add(entry.getMaxPz());entry.getMaxPz().setEditable(false);
        	for(JTextField field:entry.getPatternPz()){
        		panel.add(field);field.setEditable(false);
        	}
        }
        
        for(Entry<Instrument, PatternObject> entry:m_InstrumentParams.entrySet())
        {
        	Instrument instrument = entry.getKey();
        	PatternObject po = entry.getValue();
            
            ForecastObject fo = po.getForecast();  
            List<Double> prices = fo.p_forecastData;
            
            this.mWatchList.get(instrument).update(instrument, prices);
        }
        
        //Lay out the panel.
        makeGrid(panel, getWatchList().size(), 4 + p_pattern, 5, 5, 5, 5);
        
        //Set up the content pane.
        panel.setOpaque(true); //content panes must be opaque
        compsToExperiment.add(panel);
        
        JScrollPane jScrollPane = new JScrollPane(compsToExperiment);
	     // only a configuration to the jScrollPane...
	     jScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
	     jScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
     
        mFrame.getContentPane().add(jScrollPane, BorderLayout.CENTER);

        //Display the window.
        mFrame.setSize(635, 765);//mFrame.pack();
        mFrame.setVisible(true); 
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

    public void onAccount(IAccount account) throws JFException {
    }

    public void onMessage(IMessage message) throws JFException {
        if (message != null){
            if(message.getType() == IMessage.Type.ORDER_CLOSE_OK){
                IOrder lastOne = message.getOrder();
                print("Exit: "+lastOne.getLabel() + "  " + lastOne.getInstrument() + "  " + lastOne.getOrderCommand()
                                    + " Pips: " + lastOne.getProfitLossInPips()
                                    + " PnL: " + lastOne.getProfitLossInUSD() 
                                    + " LV: " + context.getAccount().getUseOfLeverage()
                                    + " CloseP: " + lastOne.getClosePrice() 
                                    + " OpenP: " + lastOne.getOpenPrice() 
                                    + " CloseT: " + gmtSdf.format(new Date(lastOne.getCloseTime())) 
                                    + " OpenT: " + gmtSdf.format(new Date(lastOne.getFillTime()))
                                    + " pipVal: " + lastOne.getInstrument().getPipValue() 
                                    + lastOne.getComment());
            }              
        }
    }

    public void onStop() throws JFException {
    	executor.shutdown();
        
    	if(mFrame != null){
        	mFrame.dispose();
        }
    	mFrame = null;
    }

    public void onTick(Instrument instrument, ITick tick) throws JFException {
    }
    
    public void onBar(Instrument instrument, Period period, IBar askBar, IBar bidBar) throws JFException {
        synchronized(lock){
            if(!m_InstrumentParams.keySet().contains(instrument) || !isValidTime(askBar.getTime())){
                return;
            }
            
            // PATTERN MATCHER                
            if(m_InstrumentParams.get(instrument).getPeriod() == period){
                m_InstrumentParams.get(instrument)
                            .addPastBar(instrument, bidBar.getTime());    

                // Prices
                double barMID = roundPip(instrument, (bidBar.getClose() + askBar.getClose()) / 2.0);
                double barHIGH = roundPip(instrument,(bidBar.getHigh() + askBar.getHigh()) / 2.0);
                double barLOW = roundPip(instrument,(bidBar.getLow() + askBar.getLow()) / 2.0);
                double pip = instrument.getPipValue();
                
                String comment = "  " + instrument + "  " + pip + " M: " + barMID + " ,H: " + barHIGH + " ,L: " + barLOW;
                
                PatternObject po = m_InstrumentParams.get(instrument);
                ForecastObject fo = po.getForecast();  
                comment += " , PM: " + po.getPeriod() + " " + fo.to_String();
                List<Double> prices = fo.p_forecastData;
                
                this.mWatchList.get(instrument).update(instrument, prices);
                
                double curP = prices.get(0);
                double maxP = curP;
                double minP = curP;
                for(Double p:prices){
                	maxP = Math.max(maxP, p);
                	minP = Math.min(minP, p);
                }
                   
                maxP = roundPip(instrument, maxP * barMID/curP);
                minP = roundPip(instrument, minP * barMID/curP);
                
                double filledPosition = getFilledPosition(instrument);
                if(maxP > minP + 50 * instrument.getPipValue()){
                	double rsi = (barMID - minP) /(maxP - minP);
                	if(rsi > 0.7){
                    	closeOpenedPosition(instrument, true); 
                    	
                    	if(filledPosition > 0){
                            closeFilledPosition(instrument); 
                            filledPosition = 0.0;
                        }
                    	
                    	if(filledPosition == 0){
                    		 print("Entry: " + m_TradeLabel+(m_Tradenb + 1) + "  " + instrument + "  " + OrderCommand.SELLLIMIT + " " + maxP + "; TP: " + minP + "; " + Arrays.toString(prices.toArray()));
                        	submitOrder(OrderCommand.SELLLIMIT, instrument, m_InstrumentParams.get(instrument).getSize(), maxP, 0, minP, comment);
                        }
                	}else if(rsi < 0.3){
                    	closeOpenedPosition(instrument, false); 
                    	
                    	if(filledPosition < 0){
                            closeFilledPosition(instrument); 
                            filledPosition = 0.0;
                        }
                    	
                    	if(filledPosition == 0){
                    		print("Entry: " + m_TradeLabel+(m_Tradenb + 1) + "  " + instrument + "  " + OrderCommand.BUYLIMIT + " " + minP + "; TP: " + maxP + "; " + Arrays.toString(prices.toArray()));
                        	submitOrder(OrderCommand.BUYLIMIT, instrument, m_InstrumentParams.get(instrument).getSize(), minP, 0, maxP, comment);
                        }
                	}
                }
            }
        }              
    }
    
    // Instrument position
    private Double getFilledPosition(Instrument instrument)
            throws JFException {
        double volume = 0.00;
        for (IOrder order : engine.getOrders(instrument)) {
            if (order.getState() == IOrder.State.FILLED
                    && order.getLabel().startsWith(m_TradeLabel)) {
                volume += (order.isLong() ? 1.00 : (-1.00)) * order.getAmount();
            }
        }
        return volume;
    }

    // Close Instrument positions
    private void closeFilledPosition(Instrument instrument)
            throws JFException {
        for (IOrder order : engine.getOrders(instrument)) {
            if (order.getState() == IOrder.State.FILLED
                    && order.getLabel().startsWith(m_TradeLabel)) {
                order.close();
            }
        }
    }
    
    // Close Instrument positions
    private void closeOpenedPosition(Instrument instrument, boolean longShort)
            throws JFException {
        for (IOrder order : engine.getOrders(instrument)) {
            if (order.getState() == IOrder.State.OPENED
                    && order.getLabel().startsWith(m_TradeLabel) 
                    && order.isLong() == longShort) {
            	
            	print("Cancel: " + order.getLabel() + "  " + instrument + "  " + order.getOrderCommand() + " " + order.getOpenPrice() + "; TP: " + order.getTakeProfitPrice());
                order.close();
            }
        }
    }

    // Submits an order at market price with SL and TP.
    private IOrder submitOrder(OrderCommand orderCmd, Instrument instrument, double orderAmount, double price, double stopLossPrice , double takeProfitPrice, String comment) throws JFException {     
        IOrder order = engine.submitOrder(m_TradeLabel+(++m_Tradenb), instrument, orderCmd, orderAmount, price, m_Slippage, stopLossPrice, takeProfitPrice, 0, comment);
        return order;
    }

    // Pip rounding
    private double roundPip(Instrument instrument, double value) {
        // rounding to nearest half, 0, 0.5, or 1
        double pipsMultiplier = 1/ instrument.getPipValue();
        int rounded = (int) (value * pipsMultiplier * 10 + 0.5);
        rounded = (int) ((2*rounded) / 10d + 0.5d);
        value = (rounded) / 2d;
        value /= pipsMultiplier;
        return value;
    }

    // Working hours
    private boolean isValidTime(long barTime) throws JFException {            
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(barTime);
        boolean friday = cal.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY;
        boolean saturday = cal.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY;
        boolean sunday = cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY;
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        
        if (saturday || (friday && hour >= 22) || (sunday && hour <= 21)) {
            return false;
        }
        return true;
    }
    
    private static String arrayToString(List<Double> arr) {
        String str = "[ ";
        for (int r = 0; r < arr.size(); r++) {
            str += String.format("%.5f; ", arr.get(r));
        }
        return str+"]";
    }
    
    private void print(Object o) {
        console.getOut().println(o);
    }
    
// PATTERN OBJECT
/***************************************************************************************************************************/
    private class PatternObject{
        // Instrument parameters
        private double p_size;
        private int p_barsnb;
        private Instrument p_instrument;
        
        private final Period p_perPeriod = Period.ONE_HOUR;
        private final int p_pattern = 6;
        
        List<Double> p_pastBars = new ArrayList<>();
        
        PatternObject(Instrument instrument, int years, double size) throws JFException{
            p_size = size;
            p_barsnb = years * 365 * 24;
            p_instrument = instrument;
            
            // Prepare to execute and store the Futures
            addAllPastBars(instrument, p_barsnb);
                 
            m_InstrumentParams.put(instrument, this);   
            mWatchList.put(instrument, new FieldEntry(instrument.name()));
        }   

        public double getSize(){
            return p_size;
        }

        public Period getPeriod(){
            return p_perPeriod;
        }
        
        public List<Double> getPastBars(){
            return p_pastBars;
        }
        
        public void addPastBar(Instrument instrument, long currBarTime) throws JFException{
            List<IBar> bidBars = history.getBars(instrument, p_perPeriod, OfferSide.BID, Filter.WEEKENDS, 1, currBarTime, 0);
            List<IBar> askBars = history.getBars(instrument, p_perPeriod, OfferSide.ASK, Filter.WEEKENDS, 1, currBarTime, 0);
            double bid = (bidBars.get(0).getClose()+bidBars.get(0).getHigh()+bidBars.get(0).getLow())/3.0;
            double ask = (askBars.get(0).getClose()+askBars.get(0).getHigh()+askBars.get(0).getLow())/3.0;
            p_pastBars.remove(0);
            p_pastBars.add((bid+ask)/2.0); 
        }
        
        public void addAllPastBars(Instrument instrument, int initbarsnb) throws JFException{
            List<Double> pastBars = new ArrayList<Double>();
            long currBarTime = history.getPreviousBarStart(p_perPeriod, history
             .getLastTick(instrument).getTime());
             
            List<IBar> bidBars = history.getBars(instrument, p_perPeriod, OfferSide.BID, Filter.WEEKENDS, initbarsnb, currBarTime, 0);
            List<IBar> askBars = history.getBars(instrument, p_perPeriod, OfferSide.ASK, Filter.WEEKENDS, initbarsnb, currBarTime, 0);
            for(int i = 0; i < bidBars.size(); i++){
                double bid = (bidBars.get(i).getClose()+bidBars.get(i).getHigh()+bidBars.get(i).getLow())/3.0;
                double ask = (askBars.get(i).getClose()+askBars.get(i).getHigh()+askBars.get(i).getLow())/3.0;
                pastBars.add((bid+ask)/2.0); 
            }
            
            p_pastBars.addAll(pastBars);
        }
        
        public int getPatternNB(){
            return p_pattern;
        }
        
        private List<Double> getNormalisedPast(List<Double> currBars, List<Double> pastBars) {
            double a = (currBars.get(currBars.size()-1)/pastBars.get(0));
            
            List<Double> normalisedPast = new ArrayList<>();
            for(int i = 0; i< pastBars.size(); i++){
                normalisedPast.add(roundPip(p_instrument, a*pastBars.get(i)));                                   
            }
            
            return normalisedPast;
        }
        
        public List<Double> getEntropyList(final List<Double> inBars) throws JFException{
            List<FutureTask<List<Double>>> taskList = new ArrayList<>();
            
            final int nbbars = inBars.size();
            final List<Double> currBars = inBars.subList(nbbars-getPatternNB(), nbbars);
            final int nb = nbbars-getPatternNB()-getPatternNB();
            final int nbK = Math.round(nb/threadNum);
            for(int j = 0; j < threadNum; j++){
               final int j0 = j;
               FutureTask<List<Double>> futureTaskPmFwd = new FutureTask<List<Double>>(new Callable<List<Double>>() {
                    @Override
                    public List<Double> call() throws JFException{
                        List<Double> entropyList = new ArrayList<>();
                        for (int i = Math.min(j0*nbK, nb-1); i < Math.min((j0+1)*nbK, nb); i++) {
                            double entropy = 0;
                            List<Double> pastBars = inBars.subList(i, i+getPatternNB());
                           
                            for (int k = 0; k < currBars.size(); k++) {
                                entropy += Math.abs(currBars.get(k)-pastBars.get(k)) / currBars.size();
                            }
                            entropyList.add(entropy);
                         }

                         return entropyList;
                    }
                });
                taskList.add(futureTaskPmFwd);
                executor.execute(futureTaskPmFwd);                 
           }
           
           List<Double> entropyList = new ArrayList<Double>();
           for (int j = 0; j < taskList.size(); j++) {
                FutureTask<List<Double>> futureTask = taskList.get(j);
                try{
                    entropyList.addAll(futureTask.get());
                }catch(Exception e){{print("ERR getEntropyList: "+e);}}
            }
            taskList = null;
            
            return entropyList;
        }
        
        public ForecastObject getForecast() throws JFException{
            List<Double> allPastBars = getPastBars();
            
            // Calculate entropies
            List<Double> entropyList = getEntropyList(allPastBars);
            double entropy = entropyList.get(0);
            int index = 0;
            for(int k = 0; k < entropyList.size(); k++){
                if(entropyList.get(k) < entropy){
                    entropy=entropyList.get(k);
                    index = k;
                }
            }

            List<Double> currBars = allPastBars.subList(allPastBars.size()-getPatternNB(), allPastBars.size());
            List<Double> pastBars = allPastBars.subList(index+getPatternNB(), index+getPatternNB()+getPatternNB()+1);
            List<Double> dataForecast  = getNormalisedPast(currBars, pastBars); 
            
           return new ForecastObject(dataForecast, entropy, index);           
        }
    }
    
// FORECAST OBJECT
/***************************************************************************************************************************/    
    private class ForecastObject{
        private int p_index;
        private List<Double>p_forecastData = new ArrayList<>();
        private double p_entropy;
        
        public ForecastObject(List<Double> dataForecast, double entropy, int index){
            p_index = index;
            p_forecastData.addAll(dataForecast);
            p_entropy = entropy;
        }
        
        public String to_String(){
            String print = Math.round(p_entropy*1000000.00)/1000000.00 + "  " + p_index + "  " + arrayToString(p_forecastData);
            return print;
        }
    }
}