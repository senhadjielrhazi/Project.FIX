package org.tradingsystem;

import java.util.*;
import java.text.SimpleDateFormat;

import com.dukascopy.api.*;
import com.dukascopy.api.IEngine.OrderCommand;
import com.dukascopy.api.IIndicators.AppliedPrice;

public abstract class AbstractAlgoStrategy implements IStrategy, IStrategyTools {
	private IEngine engine;
	private IConsole console;
	private IHistory history;
	private IContext context;
	private IIndicators indicators;
	//private IUserInterface userInterface;
	private SimpleDateFormat gmtSdf = new SimpleDateFormat("dd-MM-yy HH:mm:ss");

    private String m_TradeLabel = "AAS_";
    private int m_Slippage = 5;
    private int m_Tradenb = 0;
    
	private OfferSide m_Side = OfferSide.BID;
	private AppliedPrice m_AppliedPrice = AppliedPrice.CLOSE;
	private Map<Instrument, InstrumentParam> m_instrumentParams = null;

	public void onStart(IContext context) throws JFException {
		this.engine = context.getEngine();
		this.console = context.getConsole();
		this.history = context.getHistory();
		this.context = context;
		this.indicators = context.getIndicators();
		//this.userInterface = context.getUserInterface();
		this.gmtSdf.setTimeZone(TimeZone.getTimeZone("GMT"));

		m_instrumentParams = new HashMap<Instrument, InstrumentParam>();
	}

	public void onAccount(IAccount account) throws JFException {
	}

	public void onMessage(IMessage message) throws JFException {
		if (message != null) {
			IOrder lastOne = message.getOrder();
			if (lastOne != null) {
				if (lastOne.getState() == IOrder.State.CANCELED) {
					// Order scalp cancelled
					return;
				} else {
					if (message.getType() == IMessage.Type.ORDER_CLOSE_OK) {
						printOut("Order: " + lastOne.getLabel()
								+ "  " + lastOne.getInstrument()
								+ "  " + lastOne.getOrderCommand()
								+ " Pips: " + lastOne.getProfitLossInPips()
								+ " PnL: " + lastOne.getProfitLossInUSD()
								+ " EQ: " + context.getAccount().getEquity()
								+ " LV: " + context.getAccount().getUseOfLeverage()
								+ " CloseP: " + lastOne.getClosePrice()
								+ " OpenP: " + lastOne.getOpenPrice()
								+ " CloseT: " + gmtSdf.format(new Date(lastOne.getCloseTime()))
								+ " OpenT: " + gmtSdf.format(new Date(lastOne.getFillTime()))
								+ " pipVal: " + lastOne.getInstrument().getPipValue() + " "
								+ lastOne.getComment());
					}
				}
			}
		}
	}

	public void onStop() throws JFException {
        for (IOrder lastOne : engine.getOrders()) {
            if (lastOne.getState() == IOrder.State.FILLED
                    && lastOne.getLabel().startsWith(m_TradeLabel)) {
            	printOut("Order: " + lastOne.getLabel()
						+ "  " + lastOne.getInstrument()
						+ "  " + lastOne.getOrderCommand()
						+ " Pips: " + lastOne.getProfitLossInPips()
						+ " PnL: " + lastOne.getProfitLossInUSD()
						+ " EQ: " + context.getAccount().getEquity()
						+ " LV: " + context.getAccount().getUseOfLeverage()
						+ " CloseP: " + lastOne.getClosePrice()
						+ " OpenP: " + lastOne.getOpenPrice()
						+ " CloseT: " + gmtSdf.format(new Date(lastOne.getCloseTime()))
						+ " OpenT: " + gmtSdf.format(new Date(lastOne.getFillTime()))
						+ " pipVal: " + lastOne.getInstrument().getPipValue() + " "
						+ lastOne.getComment());
            }
        }
	}

	public void onTick(Instrument instrument, ITick tick) throws JFException {
	}

	public void onBar(Instrument instrument, Period period, IBar askBar,
			IBar bidBar) throws JFException {
		long barTime = bidBar.getTime();
		if (TradingTime.isValidTime(barTime).isUpdating()) {
			if (m_instrumentParams.keySet().contains(instrument)) {
				InstrumentParam instrumentParams = m_instrumentParams
						.get(instrument);
				if(instrumentParams.getRefPeriods().contains(period)){
					instrumentParams.refreshMData(period, barTime);
				}
			}
		}
	}

	@Override
	public void printOut(Object o) {
		console.getOut().println(o);
	}
	
	@Override
	public double roundPip(Instrument instrument, double value) {// Pip rounding
		// rounding to nearest half, 0, 0.5, or 1
		double pipsMultiplier = 1 / instrument.getPipValue();
		int rounded = (int) (value * pipsMultiplier * 10 + 0.5);
		rounded = (int) ((2 * rounded) / 10d + 0.5d);
		value = (rounded) / 2d;
		value /= pipsMultiplier;
		return value;
	}

	@Override
	public Map<Instrument, InstrumentParam> getInstrumentParamList() {
		return m_instrumentParams;
	}

	@Override
	public IHistory getHistory() {
		return this.history;
	}

	@Override
	public OfferSide getSide() {
		return this.m_Side;
	}
	
	@Override
	public AppliedPrice getAppliedPrice(){
		return this.m_AppliedPrice;
	}
	
	@Override
	public IIndicators getIIndicators(){
		return this.indicators;
	}
	
	@Override
	public List<Double> convertToList(double[] dataIn){
        List <Double> dataOut = new ArrayList <Double>();
        for(int i = 0; i < dataIn.length; i++){
            dataOut.add(dataIn[i]);
        }
        return dataOut;
    }
	
	@Override
	public String getTimeFormated(long barTime){
		return gmtSdf.format(barTime);
	}
	
	@Override
	public String getListFormated(List<?> arr){
		String str = "[ ";
        for (int r = 0; r < arr.size(); r++) {
            str += arr.get(r) + "; ";
        }
        return str+"]";
	}
	
	@Override
	public Double getFilledPosition(Instrument instrument)
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

	@Override
	public void closeFilledPosition(Instrument instrument)
            throws JFException {
        for (IOrder order : engine.getOrders(instrument)) {
            if (order.getState() == IOrder.State.FILLED
                    && order.getLabel().startsWith(m_TradeLabel)) {
                order.close();
            }
        }
    }
	
	@Override
	public void closeOrder(String label)
            throws JFException {        
        for (IOrder order : engine.getOrders()) {
            if(order.getLabel().equals(label) 
            && (order.getState() == IOrder.State.FILLED || order.getState() == IOrder.State.OPENED)) {
                order.close();
            }
        }
    }
    
	@Override
	public String submitOrder(OrderCommand orderCmd, Instrument instrument, double orderAmount, 
    	      double price, double stopLossPrice , double takeProfitPrice, String comment) throws JFException {  
		String label = m_TradeLabel+(++m_Tradenb);
        engine.submitOrder(label , instrument, orderCmd, orderAmount, price, m_Slippage, stopLossPrice, takeProfitPrice, 0, comment);
        return label;
    }
}