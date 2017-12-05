package prd.arb.index;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.FlowLayout;
import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.Spring;
import javax.swing.SpringLayout;

import org.marketcetera.core.time.Period;
import org.marketcetera.event.TickEvent;
import org.marketcetera.event.info.InstrumentInfo;
import org.marketcetera.marketdata.Content;
import org.marketcetera.marketdata.DataReferenceKey;
import org.marketcetera.marketdata.MarketDataRequestBuilder;
import org.marketcetera.strategy.java.Strategy;
import org.marketcetera.trade.BrokerID;
import org.marketcetera.trade.ExecutionReport;
import org.marketcetera.trade.Instrument;
import org.marketcetera.trading.MarketUtil;
import org.marketcetera.trading.arbitrage.index.BrokerEntry;
import org.marketcetera.trading.arbitrage.IBrokerEntry;
import org.marketcetera.trading.arbitrage.IMarketEntry;
import org.marketcetera.trading.arbitrage.IMarketSide;
import org.marketcetera.trading.arbitrage.index.MarketEntry;
import com.google.common.collect.Maps;

/**
 * Strategy that arb market data cross brokers
 */
public class ArbStrategyView extends Strategy {
	
	/**
	 * Place holder of all the broker entries
	 */
	private final Map<DataReferenceKey, IBrokerEntry> mDataExch = Maps.newHashMap();
	
	/**
	 * Place holder of all the composite markets
	 */
	private final Map<String, IMarketEntry> mMarketArbs = Maps.newHashMap();
	
	/**
	 * States the Strategy mode
	 */
    private StrategyMode mStrategyMode = StrategyMode.TR_HALT;
	
    /**
	 * The EMA bid/ask spread multiplier
	 */
	private final static double TRADE_BIDASK_MULT = 0.20;//0.25
	
	/**
	 * The EMA slippage multiplier
	 */
	private final static double TRADE_SLIPPAGE_MULT = 1.5;//3.0
	
	/**
	 * The maximum time allowed for stale data
	 */
	private final static long TRADE_TIME_LAP = 30*1000;
	
	/**
	 * The number of days to track trades done by the strategy
	 */
	private final static Period TRADE_MAX_DAYNB = Period.WEEKLY;
	
	/**
	 * The main frame
	 */
	private JFrame mFrame;
	
	private Map<String, FieldEntry> mWatchList = Maps.newHashMap();
	
	private class FieldEntry
	{
    	private final JTextField m_Name;
        private final JTextField m_BidSz;
        private final JTextField m_BidPz;
        private final JTextField m_AskPz;
        private final JTextField m_AskSz;
        
        private FieldEntry(String name){
        	m_Name = new JTextField(name);
        	m_BidSz = new JTextField("000000.0");
        	m_BidPz = new JTextField("0000.000");

        	m_AskPz = new JTextField("0000.000");
        	m_AskSz = new JTextField("000000.0");
        }

		public JTextField getName() {
			return m_Name;
		}

		public JTextField getBidSz() {
			return m_BidSz;
		}

		public JTextField getBidPz() {
			return m_BidPz;
		}

		public JTextField getAskPz() {
			return m_AskPz;
		}

		public JTextField getAskSz() {
			return m_AskSz;
		}
    }
	
	/**
	 * Place holder of all the market watcher
	 * 
	 * @return the map
	 */
	private Map<String, FieldEntry> getWatchList()
	{
		return mWatchList;
	}
	
	/**
	 * Place holder of all the broker market entries
	 * 
	 * @return the map
	 */
	private Map<DataReferenceKey, IBrokerEntry> getDataExch()
	{
		return mDataExch;
	}
	
	/**
	 * Place holder of all the market arb entries
	 * 
	 * @return the map
	 */
	private Map<String, IMarketEntry> getMarketArbs()
	{
		return mMarketArbs;
	}
	
	/**
	 * States the callback reasons
	 */
	private static enum Reason
	{
		TECH_INIT(3*60*1000),
		MD_UPDATE(3*60*1000);
		
		/**
	     * Create a new Reason instance.
	     *
	     * @param inDelay
	     */
	    private Reason(long inDelay)
	    {
	    	delay = inDelay;
	    }
	    
		public long getDelay() {
			return delay;
		}

		/**
	     * the delay in ms
	     */
	    private final long delay;
	};
    
	/**
	 * States the Strategy mode
	 */
	private static enum StrategyMode
    {
		TR_HALT,
    	TR_OPTRUN
    };
    
	/**
	 * Creates the data reference key
	 */
	private static class DataKeyFactory
	{
		static DataReferenceKey createKey(BrokerID brokerID, Instrument instrument)
		{
			return new DataReferenceKey(brokerID, instrument);
		}
		
		static DataReferenceKey createKey(TickEvent inTick)
		{
			return new DataReferenceKey(inTick.getBrokerID(), inTick.getInstrument());
		}
	}
	
	/**
     * Gets the report history origin date to use for the order history.
     * 
     * <p>Strategies may override this method to return a date. For performance
     * reasons, it is best to use the most recent date possible. The default is
     * to return the first second of the current day.
     * 
     * <p>All strategies in the same strategy agent share the same order history manager.
     * The report history origin date can be set only by the first strategy to run.
     *
     * @return a <code>Date</code> value
     */
	@Override
    protected final Date getReportHistoryOriginDate()
    {
        Calendar dateGenerator = Calendar.getInstance();
        dateGenerator.set(Calendar.HOUR_OF_DAY,
                          0);
        dateGenerator.set(Calendar.MINUTE,
                          0);
        dateGenerator.set(Calendar.SECOND,
                          0);
        dateGenerator.set(Calendar.MILLISECOND,
                          0);
        
        Date originDate = new Date(dateGenerator.getTime().getTime() 
        							- TRADE_MAX_DAYNB.getInterval());
        return originDate;
    }
	
    /**
     * Executed when the strategy is started.
     * Use this method to set up data flows
     * and other initialization tasks.
     */
    @Override
    public void onStart()
    {
    	warn("ArbStrategy Staring...!");
        
        //Load brokers
        BrokerID[] brokers = MarketUtil.getBrokers(getDataBrokers(), getTradeBrokers());
        
    	//Load instruments and infos
    	for(BrokerID brokerID:brokers){
			Set<Instrument> instruments = getInstruments(brokerID);
			if(instruments != null){
    			for(Instrument instrument:instruments){
    				InstrumentInfo instrumentInfo = getInstrumentInfo(brokerID, instrument);		        				
					DataReferenceKey key = DataKeyFactory.createKey(brokerID, instrument);
    				if(getDataExch().containsKey(key)){
    					throw new IllegalArgumentException("Key alreay in map: Broker= " + brokerID + "; Instrument= " + instrument);
    				}
    				getDataExch().put(key, new BrokerEntry(brokerID, instrument, instrumentInfo, TRADE_BIDASK_MULT, TRADE_SLIPPAGE_MULT));
    				
    				warn("Adding: Broker= " + brokerID + "; Instrument= " + instrument + "; Info= " + instrumentInfo);
    			}
			}
    	}
    	
    	//Build market broker entries
		for(IBrokerEntry entry:getDataExch().values()){
			Instrument instrument = entry.getInstrument();
			
			String symbol = instrument.getSymbol();
			if(!getMarketArbs().containsKey(symbol)){
				getWatchList().put(symbol, new FieldEntry(symbol));
				getMarketArbs().put(symbol, new MarketEntry(instrument, getDataExch().values(), TRADE_TIME_LAP));
    		}
		}
    	
    	//Request market data
        for(IBrokerEntry entry:getDataExch().values()){
    		requestMarketData(MarketDataRequestBuilder.newRequest().
                        withInstrument(entry.getInstrument()).
                        withBrokerID(entry.getBrokerID()).
                        withContent(Content.TICK).create());
        }
        
        //Market data analysis setup 
        Reason reason = Reason.TECH_INIT;
        requestCallbackAfter(reason.getDelay(), reason);
        
        mFrame = new JFrame("ArbWatcher");
        final JPanel compsToExperiment = new JPanel();
        FlowLayout experimentLayout = new FlowLayout(FlowLayout.CENTER, 20, 5);
        compsToExperiment.setLayout(experimentLayout);
        experimentLayout.setAlignment(FlowLayout.TRAILING);

        //Create the panel and populate it.
        JPanel panel = new JPanel(new SpringLayout());
        for(FieldEntry entry:getWatchList().values()){
        	panel.add(entry.getName());
        	panel.add(entry.getBidSz());
        	panel.add(entry.getBidPz());
        	panel.add(entry.getAskPz());
        	panel.add(entry.getAskSz());
        }
        
        //Lay out the panel.
        makeGrid(panel, getWatchList().size(), 5, 5, 5, 5, 5);
        
        //Set up the content pane.
        panel.setOpaque(true); //content panes must be opaque
        compsToExperiment.add(panel);
        
        JScrollPane jScrollPane = new JScrollPane(compsToExperiment);
	     // only a configuration to the jScrollPane...
	     jScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
	     jScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
     
        mFrame.getContentPane().add(jScrollPane, BorderLayout.CENTER);

        //Display the window.
        mFrame.setSize(350, 450);//mFrame.pack();
        mFrame.setVisible(true);
    }
    
    public static void makeGrid(Container parent, 
			int rows, int cols, int initialX, int initialY, int xPad, int yPad) 
	{
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

    /**
     * Executed when the strategy receives a Tick event.
     *
     * @param inTick the tick event.
     */
    @Override
    public void onTick(TickEvent inTick) 
    {
    	DataReferenceKey key = DataKeyFactory.createKey(inTick);
		IBrokerEntry entry = getDataExch().get(key);
		if(entry != null)
		{
			entry.onTick(inTick);
			
			for(Entry<String, IMarketEntry> marketEntry:getMarketArbs().entrySet()){
				String symbol = marketEntry.getKey();
				IMarketEntry market = marketEntry.getValue();

				FieldEntry field = getWatchList().get(symbol);
				if(field != null){
					DecimalFormat formatSz = new DecimalFormat("#,###.0");
					DecimalFormat formatPz = new DecimalFormat("#0.000");
					
					IMarketSide bid = market.getBid();
					field.getBidSz().setText(formatSz.format(bid.getSize()));
					field.getBidPz().setText(formatPz.format(bid.getPrice()));
					
					IMarketSide ask = market.getAsk();
					field.getAskPz().setText(formatPz.format(ask.getPrice()));
					field.getAskSz().setText(formatSz.format(ask.getSize()));
			
					field.getBidPz().setBackground(Color.lightGray);
					field.getAskPz().setBackground(Color.lightGray);
					if(bid.getPrice() > ask.getPrice() && ask.getSize() != 0){
						field.getBidPz().setBackground(new Color(146, 205, 0));
						field.getAskPz().setBackground(new Color(146, 205, 0));
						
						mCounter++;
						double pnl = MarketUtil.getPnL(bid, ask);
						mPnL += pnl;
						warn("Positive: " + mCounter + "; " + symbol + "; TPnL=" + mPnL +"; PnL=" + pnl + "; Entry:Bid=" + bid + "; Ask=" + ask);
					}
				}
			}
		}

    }
    
	private int mCounter = 0;
	private double mPnL = 0.;
		
    /**
     * Executed when the strategy receives a callback requested via
     * {@link #requestCallbackAt(java.util.Date, Object)} or
     * {@link #requestCallbackAfter(long, Object)}. All timer
     * callbacks come with the data supplied when requesting callback,
     * as an argument.
     *
     * @param inData the callback data
     */
    @Override
    public void onCallback(Object inData) 
    {
    	if(inData == null)
    		return;
    	
    	if(inData.equals(Reason.MD_UPDATE))
    	{	 
    		StrategyMode mode = mStrategyMode;
    		mStrategyMode = StrategyMode.TR_HALT;
			for(IBrokerEntry market: getDataExch().values()){
				market.onCallback();
			}
			mStrategyMode = mode;
    	} else if(inData.equals(Reason.TECH_INIT)){            
            onCallback(Reason.MD_UPDATE);
			
            //Setup market data init 
            Reason reason = Reason.MD_UPDATE;
            requestCallbackEvery(reason.getDelay(), reason.getDelay(), reason);
    	}

    }
    
    /**
     * Executed when the strategy receives an execution report.
     *
     * @param inReport the execution report.
     */
    @Override
    public void onExecutionReport(ExecutionReport inReport) 
    {
    	warn("Received Execution Report:" + inReport);
    }
    
    /**
     * Executed when the strategy is stopped.
     */
    @Override
    public void onStop() 
    {
        warn("ArbStrategy Stoping...!");
        
        if(mFrame != null){
        	mFrame.dispose();
        }
    }
}
