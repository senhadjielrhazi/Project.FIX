package org.neurosystem.client.api.brokers.dk;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.neurosystem.modules.marketdata.assets.IAsset;
import org.neurosystem.modules.marketdata.assets.basic.ISecurity;
import org.neurosystem.modules.marketdata.assets.basic.Security;
import org.neurosystem.modules.marketdata.quote.IQuote;
import org.neurosystem.modules.marketdata.quote.Quote;
import org.neurosystem.platform.PlatformAPI;
import org.neurosystem.platform.api.IBrokersAPI;
import org.neurosystem.platform.api.IPlatformAPI;
import org.neurosystem.platform.trade.OrderType;
import org.neurosystem.util.Parameters;
import org.neurosystem.util.basic.HasTime;
import org.neurosystem.util.log.Priority;

import com.dukascopy.api.Filter;
import com.dukascopy.api.IAccount;
import com.dukascopy.api.IBar;
import com.dukascopy.api.IContext;
import com.dukascopy.api.IEngine;
import com.dukascopy.api.IHistory;
import com.dukascopy.api.IMessage;
import com.dukascopy.api.IOrder;
import com.dukascopy.api.IOrder.State;
import com.dukascopy.api.IStrategy;
import com.dukascopy.api.ITick;
import com.dukascopy.api.Instrument;
import com.dukascopy.api.JFException;
import com.dukascopy.api.OfferSide;
import com.dukascopy.api.Period;
import com.dukascopy.api.IEngine.OrderCommand;

public abstract class DKAPI implements IStrategy, IBrokersAPI {

	/**************************************************API-Engine*********************************************/
	private final IPlatformAPI p_platformAPI;
	private final Map<ISecurity, IQuote> p_prices;
	private final Map<ISecurity, Instrument> p_instruments;
	
	public DKAPI(){
		this.p_platformAPI = new PlatformAPI(this);
		this.p_prices = new HashMap<>();
		this.p_instruments = new HashMap<>();
	}
	
    protected Map<ISecurity, IQuote> getPrices() {
    	return this.p_prices;
	}
    
	protected Map<ISecurity, Instrument> getInstruments() {
		return this.p_instruments;
	}
	
    protected IPlatformAPI getPlatformAPI(){
		return this.p_platformAPI;
	}
    
    protected void addTradingAssets(IAsset asset) {
		getPlatformAPI().addTradingAsset(asset);
		
		for(ISecurity security:asset.getSecurities()){
			Instrument instrument = Instrument.valueOf(security.getSymbol());
			if(!getInstruments().containsKey(security)){
				getInstruments().put(security, instrument);
			}
			if(getRefInstrument() == null){
				this.p_refInstrument = instrument;
			}
    	}
	}

    protected void postStart() {
   		//Load trade history
   		loadLiveTrades();
   		
   		//Load historical data
   		loadHistoricalData();
   	}
    
	/**************************************************Orders***********************************************/
	private int p_tradenb = 0;
	
	protected abstract String getLabel();
	protected abstract double getSlippage();
	
	private int getTradeNB() {
		return (++this.p_tradenb);
	}
	
    private void setTradeNB(int nb) {
    	this.p_tradenb = nb;
	}
	
    @Override
	public void closeOrder(String label) {
		try {
			for (IOrder order : getEngine().getOrders()) {
			    if(order.getLabel().equals(label) && !order.getState().equals(State.CLOSED)){
			    	order.close();
			    }
			}
		} catch (JFException e) {
			log("Close order error: " + e, Priority.ERROR);
		}
	}
    
    @Override
	public boolean isOpen(String label) {
    	boolean isOpen = false;
		try {
			for (IOrder order : getEngine().getOrders()) {
			    if(order.getLabel().equals(label) && !order.getState().equals(State.CLOSED)){
			    	isOpen = true;
			    	break;
			    }
			}
		} catch (Exception e) {
			log("isOpen order error: " + e, Priority.ERROR);
		}
		
		return isOpen;
	}

    @Override
	public String submitOrder(OrderType ordertype, ISecurity security,
			double orderAmount, double price, String comment) {
		String label = getLabel() + getTradeNB();
		
		Instrument instrument = Instrument.valueOf(security.getSymbol());
		OrderCommand orderCmd = getOrderCommand(ordertype);
		
		try {
			getEngine().submitOrder(label, instrument, orderCmd, orderAmount, price, getSlippage(), 0, 0, 0, comment);
		} catch (JFException e) {
			log("Submit order error: " + e, Priority.ERROR);
		}
        return label;
	}

    @Override
	public String submitOrder(OrderType ordertype, ISecurity security,
			double orderAmount, double price, double stp, double tkp, String comment) {
		String label = getLabel() + getTradeNB();
		
		Instrument instrument = Instrument.valueOf(security.getSymbol());
		OrderCommand orderCmd = getOrderCommand(ordertype);
		
		try {
			double tkPrice = 0., stPrice = 0.;
			if(ordertype.isLong()){
				tkPrice = getHistory().getLastTick(instrument).getAsk() + tkp * instrument.getPipValue();
				stPrice = getHistory().getLastTick(instrument).getBid() - stp * instrument.getPipValue();
			}else{
				tkPrice = getHistory().getLastTick(instrument).getBid() - tkp * instrument.getPipValue();
				stPrice = getHistory().getLastTick(instrument).getAsk() + stp * instrument.getPipValue();
			}
			getEngine().submitOrder(label, instrument, orderCmd, orderAmount, price, getSlippage(), stPrice, tkPrice, 0, comment);
		} catch (JFException e) {
			log("Submit order error: " + e, Priority.ERROR);
		}
        return label;
	}

    
    private OrderCommand getOrderCommand(OrderType ordertype) {
		switch (ordertype) {
		case BUYMARKET:
			return OrderCommand.BUY;	
		case SELLMARKET:
			return OrderCommand.SELL;
		case BUYLIMIT:
			return OrderCommand.BUYLIMIT;
		case SELLLIMIT:
			return OrderCommand.SELLLIMIT;
		}
		
		return null;
	}
    
	/**************************************************Logging***********************************************/
    protected abstract Priority getPriority();
	
	@Override
	public void log(String message, Priority level) {
		if(getPriority().isAllowed(level)){
			getLoggerStream().println(message);
		}
	}
	
	public void trace(String message) {
		getTraceStream().println(message);
	}
	
	/**************************************************API-DUK*********************************************/
	private IContext p_context;
	private IEngine p_engine;
	private PrintStream p_loggerStream;
	private PrintStream p_traceStream;
	private IAccount p_account;
	private IHistory p_history;
	
	private final OfferSide p_Side = OfferSide.BID;
	protected Instrument p_refInstrument;
	
	@Override
	public void onStart(IContext context) {
		this.p_context = context;
		this.p_engine = context.getEngine();
		this.p_loggerStream = context.getConsole().getOut();
		this.p_account = context.getAccount();
		this.p_history = context.getHistory();
	}

	@Override
	public void onBar(Instrument inInstrument, Period inPeriod, IBar askBar, IBar bidBar) {
		long barTime = bidBar.getTime();
		if (HasTime.isTradingTime(barTime) && (bidBar.getHigh() > bidBar.getLow())) {
			if(inPeriod.name().equals(Parameters.getBarPeriod().name())){
				if (getRefInstrument().equals(inInstrument)) {				
					getPrices().clear();
					try {
						for (Iterator<Entry<ISecurity, Instrument>> it = getInstruments().entrySet().iterator(); 
							     it.hasNext();){
							Entry<ISecurity, Instrument> entry = it.next();
							Instrument instrument = entry.getValue();
							ISecurity security = entry.getKey();
							List<IBar> bars = getHistory().getBars(instrument, inPeriod, getSide(), Filter.WEEKENDS, 1, barTime,0);
							
							IQuote quote = barToQuote(bars.get(0));
							getPrices().put(security, quote);
						}
	
						getPlatformAPI().onBar(getPrices());
					} catch (JFException e) {
						log("onBar error: " + e, Priority.ERROR);
					}
				}
			}
		}
	}

	@Override
	public void onMessage(IMessage message) throws JFException {
		if (message != null) {
			IOrder lastOne = message.getOrder();
			if (lastOne != null) {
				if ((lastOne.getState() != IOrder.State.CANCELED) && (message.getType() == IMessage.Type.ORDER_CLOSE_OK)) {
					log(printOrder(lastOne), Priority.WARN);
				}
			}
		}
	}

	@Override
	public void stopBrokers() {
		try {
			for (IOrder lastOne : getEngine().getOrders()) {
	            if (lastOne.getState() == IOrder.State.FILLED
	                    && lastOne.getLabel().startsWith(getLabel())) {
	            	log(printOrder(lastOne), Priority.WARN);
	            }
	        }
		} catch (JFException e) {
			log("Stop brokers error: " + e, Priority.ERROR);
		}
		
		getContext().stop();
		closeTraceStream();
	}
	
	@Override
	public void onStop() throws JFException {
		for (IOrder lastOne : getEngine().getOrders()) {
            if (lastOne.getState() == IOrder.State.FILLED
                    && lastOne.getLabel().startsWith(getLabel())) {
            	log(printOrder(lastOne), Priority.WARN);
            }
        }
		
		getPlatformAPI().onStop();
		closeTraceStream();
	}

	@Override
	public void onTick(Instrument instrument, ITick tick) throws JFException {
	}
	
	@Override
	public void onAccount(IAccount account) throws JFException {
	}
	
	private String printOrder(IOrder order){
		String data = "Order: " + order.getLabel()
				+ "  " + order.getInstrument()
				+ "  " + order.getOrderCommand()
				+ " Pips: " + order.getProfitLossInPips()
				+ " PnL: " + order.getProfitLossInUSD()
				+ " EQ: " + getAccount().getEquity()
				+ " LV: " + getAccount().getUseOfLeverage()
				+ " SZ: " + order.getAmount()
				+ " CloseP: " + order.getClosePrice()
				+ " OpenP: " + order.getOpenPrice()
				+ " CloseT: " + HasTime.formatedTime(order.getCloseTime())
				+ " OpenT: " + HasTime.formatedTime(order.getFillTime())
				+ " pipVal: " + order.getInstrument().getPipValue() + " "
				+ order.getComment();
		
		return data;
	}

	protected Instrument getRefInstrument() {
		return this.p_refInstrument;
	}
	
	private void loadHistoricalData() {
        Period period = Period.valueOf(Parameters.getBarPeriod().name());
        long timeBack = Parameters.getHistoryBack();
		
		log("Loading Quotes... " + getInstruments().size(), Priority.INFO);
		long timeB = System.currentTimeMillis();
    	try {
    		long endTime = getHistory().getStartTimeOfCurrentBar(getRefInstrument(), period);
    		long startTime = endTime - timeBack;

			List<IBar> refBars = getHistory().getBars(getRefInstrument(), period, getSide(), Filter.WEEKENDS, startTime, endTime);
			for(int i = 0; i < refBars.size(); i++){
				getPrices().clear();
				
				long barTime = refBars.get(i).getTime();
				IBar bar =  refBars.get(i);
				if(HasTime.isTradingTime(barTime) && (bar.getHigh() > bar.getLow())){
					for (Iterator<Entry<ISecurity, Instrument>> it = getInstruments().entrySet().iterator(); 
						     it.hasNext();){
						Entry<ISecurity, Instrument> entry = it.next();
						Instrument instrument = entry.getValue();
						ISecurity security = entry.getKey();
						
		                List<IBar> bars = getHistory().getBars(instrument, period, getSide(), Filter.WEEKENDS, 1, barTime,0);
						IQuote quote = barToQuote(bars.get(0));
							
						getPrices().put(security, quote);
					}
				
					getPlatformAPI().onHistory(getPrices());
				}
			}
    		
			long timeA = System.currentTimeMillis();
			log("Done loading... " + Math.round((timeA-timeB)/1000) + "s", Priority.INFO);
    	} catch (Exception e) {
			log("Load historical data error: " + e, Priority.ERROR);
		}
    }	
	
	private void loadLiveTrades() {
		int nb = 0;
		try {
			for (IOrder order : getEngine().getOrders()) {
			    if (order.getState() == IOrder.State.FILLED
			            && order.getLabel().startsWith(getLabel())) {
			    	String label = order.getLabel();
			    	
			    	/*
			    	Instrument instrument = order.getInstrument();
			    	long time = order.getFillTime();
			    	boolean longShort = order.isLong();
			    	double size = order.getAmount();
			    	String comment = order.getComment();
			    	getSystemEngine().onTrade(instrument, label, time, longShort, size, comment);*/
			    	
			    	int nbTrade = Integer.parseInt(label.replace(getLabel(), ""));
			    	nb = Math.max(nb, nbTrade);    
			    }
			}
			
			setTradeNB(nb);
			log("Label: " + getLabel() + " , nbTrade: " + nb, Priority.INFO);
		} catch (JFException e) {
			log("Load live trades error: " + e, Priority.ERROR);
		}
	}
	
	protected IQuote barToQuote(IBar bar) {
		return new Quote(bar.getTime(), 
				bar.getOpen(), bar.getHigh(), bar.getLow(), bar.getClose());
	}

	protected ISecurity instrumentToSecurity(double amount, Instrument instrument) {
		return new Security(amount, instrument.name(), instrument.getPipValue(), 1000./1000000.);
	}
	
	private IEngine getEngine() {
		return this.p_engine;
	}
	
	protected IContext getContext() {
		return this.p_context;
	}
	
	private IAccount getAccount() {
		return this.p_account;
	}
	
	protected PrintStream getLoggerStream() {
		return this.p_loggerStream;
	}
	
	protected PrintStream getTraceStream() {
		if(this.p_traceStream == null){
			try {
				int serial = Math.round((float)(Math.random() * 10000));
				this.p_traceStream = new PrintStream(new FileOutputStream("dk-api-log-" + serial + ".txt"));
			} catch (FileNotFoundException e) {
				log("Trace stream error: " + e, Priority.ERROR);
			}
		}
		
		return this.p_traceStream;
	}
	
	
	private void closeTraceStream() {
		if(this.p_traceStream != null){
			this.p_traceStream.flush();
			this.p_traceStream.close();
		}
	}
	
	protected IHistory getHistory() {
		return this.p_history;
	}
	
	protected OfferSide getSide() {
		return this.p_Side;
	}
}
