package prd.arb.index;

import org.marketcetera.strategy.java.Strategy;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.FlowLayout;
import java.text.DecimalFormat;
import java.util.Map;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.Spring;
import javax.swing.SpringLayout;

import org.marketcetera.event.TickEvent;
import org.marketcetera.marketdata.MarketDataRequestBuilder;
import org.marketcetera.trade.BrokerID;
import org.marketcetera.trade.Equity;

import com.google.common.collect.Maps;

import org.marketcetera.marketdata.Content;

/**
 * Strategy that receives market data
 */
public class MarketData extends Strategy {
    private static final String SYMBOL = "US500"; //Symbol
    /**
     * Executed when the strategy is started.
     * Use this method to set up data flows
     * and other initialization tasks.
     */
    @Override
    public void onStart() {
        //equity
        requestMarketData(MarketDataRequestBuilder.newRequest().
                withInstrument(new Equity(SYMBOL)).
                withBrokerID(new BrokerID("DK")).
                withContent(Content.TICK).create());
        
        //equity
        requestMarketData(MarketDataRequestBuilder.newRequest().
                withInstrument(new Equity(SYMBOL)).
                withBrokerID(new BrokerID("LM")).
                withContent(Content.TICK).create());
        
        //equity
        requestMarketData(MarketDataRequestBuilder.newRequest().
                withInstrument(new Equity(SYMBOL)).
                withBrokerID(new BrokerID("FE")).
                withContent(Content.TICK).create());
        
        getWatchList().put(SYMBOL, new FieldEntry(SYMBOL));     
        
        mFrame = new JFrame("ArbWatcher");
        final JPanel compsToExperiment = new JPanel();
        FlowLayout experimentLayout = new FlowLayout(FlowLayout.CENTER, 20, 5);
        compsToExperiment.setLayout(experimentLayout);
        experimentLayout.setAlignment(FlowLayout.TRAILING);

        //Create the panel and populate it.
        JPanel panel = new JPanel(new SpringLayout());
        for(FieldEntry entry:getWatchList().values()){
        	panel.add(entry.getName());
        	
        	panel.add(entry.dkBidSz());
        	panel.add(entry.dkBidPz());
        	panel.add(entry.dkAskPz());
        	panel.add(entry.dkAskSz());
        	
        	panel.add(entry.lmBidSz());
        	panel.add(entry.lmBidPz());
        	panel.add(entry.lmAskPz());
        	panel.add(entry.lmAskSz());
        	
        	panel.add(entry.feBidSz());
        	panel.add(entry.feBidPz());
        	panel.add(entry.feAskPz());
        	panel.add(entry.feAskSz());
        	
        	panel.add(entry.getBidSz());
        	panel.add(entry.getBidPz());
        	panel.add(entry.getAskPz());
        	panel.add(entry.getAskSz());
        }
        
        //Lay out the panel.
        makeGrid(panel, getWatchList().size(), 17, 5, 5, 5, 5);
        
        //Set up the content pane.
        panel.setOpaque(true); //content panes must be opaque
        compsToExperiment.add(panel);
        
        JScrollPane jScrollPane = new JScrollPane(compsToExperiment);
	     // only a configuration to the jScrollPane...
	     jScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
	     jScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
     
        mFrame.getContentPane().add(jScrollPane, BorderLayout.CENTER);

        //Display the window.
        mFrame.setSize(600, 900);//mFrame.pack();
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
    
	private int mCounter = 0;
	private double mPnL = 0.;
	
    /**
     * Executed when the strategy receives a Tick event.
     *
     * @param inTick the tick event.
     */
    @Override
    public void onTick(TickEvent inTick) {
    	//warn("Received TickEvent:" + inTick);
    	
    	String symbol = inTick.getInstrument().getSymbol();
    	BrokerID bID = inTick.getBrokerID();
    	FieldEntry field = getWatchList().get(symbol);
		if(field != null){
			DecimalFormat formatSz = new DecimalFormat("#,###.00");
			DecimalFormat formatPz = new DecimalFormat("#0.00000");
			
			if(bID.getValue().equals("DK")){
				DetailEntry entry = getDetailEntry().get(bID);
				if(entry == null){
					entry = new DetailEntry();
					double mid = 0.5*(inTick.getBid().getPrice().doubleValue() + inTick.getAsk().getPrice().doubleValue());
					entry.setPrice(mid);
					entry.setSize(1000000.0);
					getDetailEntry().put(bID, entry);
				}
				
				field.dkBidSz().setText(formatSz.format(inTick.getBid().getSize().doubleValue() * entry.getSize()));
				field.dkBidPz().setText(formatPz.format(inTick.getBid().getPrice().doubleValue() - entry.getPrice()));

				field.dkAskPz().setText(formatPz.format(inTick.getAsk().getPrice().doubleValue() - entry.getPrice()));
				field.dkAskSz().setText(formatSz.format(inTick.getAsk().getSize().doubleValue() * entry.getSize()));
			}
			
			if(bID.getValue().equals("LM")){
				DetailEntry entry = getDetailEntry().get(bID);
				if(entry == null){
					entry = new DetailEntry();
					double mid = 0.5*(inTick.getBid().getPrice().doubleValue() + inTick.getAsk().getPrice().doubleValue());
					entry.setPrice(mid);
					entry.setSize(1.0);
					getDetailEntry().put(bID, entry);
				}
				
				field.lmBidSz().setText(formatSz.format(inTick.getBid().getSize().doubleValue() * entry.getSize()));
				field.lmBidPz().setText(formatPz.format(inTick.getBid().getPrice().doubleValue() - entry.getPrice()));

				field.lmAskPz().setText(formatPz.format(inTick.getAsk().getPrice().doubleValue() - entry.getPrice()));
				field.lmAskSz().setText(formatSz.format(inTick.getAsk().getSize().doubleValue() * entry.getSize()));
			}
			
			if(bID.getValue().equals("FE")){
				DetailEntry entry = getDetailEntry().get(bID);
				if(entry == null){
					entry = new DetailEntry();
					double mid = 0.5*(inTick.getBid().getPrice().doubleValue() + inTick.getAsk().getPrice().doubleValue());
					entry.setPrice(mid);
					entry.setSize(1.0);
					getDetailEntry().put(bID, entry);
				}
				
				field.feBidSz().setText(formatSz.format(inTick.getBid().getSize().doubleValue() * entry.getSize()));
				field.feBidPz().setText(formatPz.format(inTick.getBid().getPrice().doubleValue() - entry.getPrice()));

				field.feAskPz().setText(formatPz.format(inTick.getAsk().getPrice().doubleValue() - entry.getPrice()));
				field.feAskSz().setText(formatSz.format(inTick.getAsk().getSize().doubleValue() * entry.getSize()));
			}
			
			String bidID = "dk";
			field.getBidSz().setText(field.dkBidSz().getText());
			field.getBidPz().setText(field.dkBidPz().getText());
			if(Double.valueOf(field.lmBidPz().getText()) > Double.valueOf(field.getBidPz().getText())){
				bidID = "lm";
				field.getBidSz().setText(field.lmBidSz().getText());
				field.getBidPz().setText(field.lmBidPz().getText());
			}
			if(Double.valueOf(field.feBidPz().getText()) > Double.valueOf(field.getBidPz().getText())){
				bidID = "fe";
				field.getBidSz().setText(field.feBidSz().getText());
				field.getBidPz().setText(field.feBidPz().getText());
			}
			
			String askID = "dk";
			field.getAskSz().setText(field.dkAskSz().getText());
			field.getAskPz().setText(field.dkAskPz().getText());
			if(Double.valueOf(field.lmAskPz().getText()) < Double.valueOf(field.getAskPz().getText())){
				askID = "lm";
				field.getAskSz().setText(field.lmAskSz().getText());
				field.getAskPz().setText(field.lmAskPz().getText());
			}
			if(Double.valueOf(field.feAskPz().getText()) < Double.valueOf(field.getAskPz().getText())){
				askID = "fe";
				field.getAskSz().setText(field.feAskSz().getText());
				field.getAskPz().setText(field.feAskPz().getText());
			}
			
			field.getBidPz().setBackground(Color.lightGray);
			field.getAskPz().setBackground(Color.lightGray);
			double pnl = Double.valueOf(field.getBidPz().getText()) - Double.valueOf(field.getAskPz().getText());
			if(pnl > 0.5){
				field.getBidPz().setBackground(new Color(146, 205, 0));
				field.getAskPz().setBackground(new Color(146, 205, 0));
				
				mCounter++;
				mPnL += pnl;
				warn("Positive: " + mCounter + "; " + symbol + "; TPnL=" + mPnL +"; PnL=" + pnl + "; " + bidID +" ; Bid=" + Double.valueOf(field.getBidPz().getText()) + "; Ask=" + Double.valueOf(field.getAskPz().getText()) + "; " + askID);
			}
			
		}
    }

	/**
	 * The main frame
	 */
	private JFrame mFrame;
	
	private Map<String, FieldEntry> mWatchList = Maps.newHashMap();
	
	/**
	 * Place holder of all the market watcher
	 * 
	 * @return the map
	 */
	private Map<String, FieldEntry> getWatchList()
	{
		return mWatchList;
	}
	
	private Map<BrokerID, DetailEntry> mDetailEntry = Maps.newHashMap();
	
	/**
	 * Place holder of all the market watcher
	 * 
	 * @return the map
	 */
	private Map<BrokerID, DetailEntry> getDetailEntry()
	{
		return mDetailEntry;
	}
	
	private class DetailEntry
	{
		private double mSize;
		private double mPrice;
		
		public double getSize() {
			return mSize;
		}
		public void setSize(double mSize) {
			this.mSize = mSize;
		}
		public double getPrice() {
			return mPrice;
		}
		public void setPrice(double mPrice) {
			this.mPrice = mPrice;
		}
	}
	
	private class FieldEntry
	{
    	private final JTextField m_Name;
        private final JTextField dk_BidSz;
        private final JTextField dk_BidPz;
        private final JTextField dk_AskPz;
        private final JTextField dk_AskSz;
        
        private final JTextField lm_BidSz;
        private final JTextField lm_BidPz;
        private final JTextField lm_AskPz;
        private final JTextField lm_AskSz;
        
        private final JTextField fe_BidSz;
        private final JTextField fe_BidPz;
        private final JTextField fe_AskPz;
        private final JTextField fe_AskSz;
        
        private final JTextField m_BidSz;
        private final JTextField m_BidPz;
        private final JTextField m_AskPz;
        private final JTextField m_AskSz;
        
        private FieldEntry(String name){
        	m_Name = new JTextField(name);
        	dk_BidSz = new JTextField("0.00");
        	dk_BidPz = new JTextField("0.00000");

        	dk_AskPz = new JTextField("0.00000");
        	dk_AskSz = new JTextField("0.00");
        	
        	lm_BidSz = new JTextField("0.00");
        	lm_BidPz = new JTextField("0.00000");

        	lm_AskPz = new JTextField("0.00000");
        	lm_AskSz = new JTextField("0.00");
        	
        	fe_BidSz = new JTextField("0.00");
        	fe_BidPz = new JTextField("0.00000");

        	fe_AskPz = new JTextField("0.00000");
        	fe_AskSz = new JTextField("0.00");
        	
        	m_BidSz = new JTextField("0.00");
        	m_BidPz = new JTextField("0.00000");

        	m_AskPz = new JTextField("0.00000");
        	m_AskSz = new JTextField("0.00");
        }

		public JTextField getName() {
			return m_Name;
		}

		public JTextField dkBidSz() {
			return dk_BidSz;
		}

		public JTextField dkBidPz() {
			return dk_BidPz;
		}

		public JTextField dkAskPz() {
			return dk_AskPz;
		}

		public JTextField dkAskSz() {
			return dk_AskSz;
		}
		
		public JTextField lmBidSz() {
			return lm_BidSz;
		}

		public JTextField lmBidPz() {
			return lm_BidPz;
		}

		public JTextField lmAskPz() {
			return lm_AskPz;
		}

		public JTextField lmAskSz() {
			return lm_AskSz;
		}
		
		public JTextField feBidSz() {
			return fe_BidSz;
		}

		public JTextField feBidPz() {
			return fe_BidPz;
		}

		public JTextField feAskPz() {
			return fe_AskPz;
		}

		public JTextField feAskSz() {
			return fe_AskSz;
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
