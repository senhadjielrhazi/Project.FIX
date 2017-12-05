package org.marketcetera.server.ba.brokers.api;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.persistence.Transient;

import org.marketcetera.core.IDFactory;
import org.marketcetera.core.InMemoryIDFactory;
import org.marketcetera.core.publisher.ISubscriber;
import org.marketcetera.core.time.Period;
import org.marketcetera.event.DepthEvent;
import org.marketcetera.event.Event;
import org.marketcetera.event.QuoteEvent;
import org.marketcetera.event.TickEvent;
import org.marketcetera.event.TradeEvent;
import org.marketcetera.marketdata.MarketDepth;
import org.marketcetera.marketdata.SimulatedExchange;
import org.marketcetera.marketdata.SimulatedExchange.Token;
import org.marketcetera.quickfix.FIXMessageUtil;
import org.marketcetera.server.ba.brokers.Broker;
import org.marketcetera.server.ba.brokers.Messages;
import org.marketcetera.server.ba.brokers.spring.SpringBroker;
import org.marketcetera.trade.Equity;
import org.marketcetera.trade.FIXUtil;
import org.marketcetera.trade.Instrument;
import org.marketcetera.util.except.I18NException;
import org.marketcetera.util.log.I18NBoundMessage2P;
import org.marketcetera.util.misc.ClassVersion;
import org.marketcetera.util.ws.wrappers.MapWrapper;
import org.marketcetera.ws.MarshallerFactory;
import org.marketcetera.ws.server.BasicJob;

import com.google.common.collect.Maps;
import com.mchange.v1.util.UnexpectedException;

import quickfix.FieldNotFound;
import quickfix.Group;
import quickfix.Message;
import quickfix.Session;
import quickfix.SessionID;
import quickfix.field.ClOrdID;
import quickfix.field.ExecType;
import quickfix.field.LeavesQty;
import quickfix.field.MDEntryDate;
import quickfix.field.MDEntryPx;
import quickfix.field.MDEntrySize;
import quickfix.field.MDEntryType;
import quickfix.field.MDMkt;
import quickfix.field.MDReqID;
import quickfix.field.MsgType;
import quickfix.field.NoMDEntries;
import quickfix.field.NoRelatedSym;
import quickfix.field.OrdStatus;
import quickfix.field.OrdType;
import quickfix.field.OrderQty;
import quickfix.field.OrigClOrdID;
import quickfix.field.Price;
import quickfix.field.SendingTime;
import quickfix.field.Side;
import quickfix.field.SubscriptionRequestType;
import quickfix.field.Symbol;
import quickfix.field.TimeInForce;
import quickfix.field.TransactTime;
import quickfix.field.XmlData;
import quickfix.field.XmlDataLen;

/**
 * The configuration of a BG broker client.
 */
@ClassVersion("$Id: BGClient.java 16752 2013-11-14 02:54:13Z colin $")
public class BGClient implements ClientKernel, ISubscriber
{
	// CLASS DATA
	private Broker dataBroker;
	private Broker tradeBroker;
	
	@Transient
	private SimulatedExchange mExchange;
	
	@Transient
	private Map<SessionID,Broker> mSessionIDMap=Maps.newConcurrentMap();
    
    /**
     * Mapping unique instrument to request message. 
     */
    private final Map<Instrument,Message> mInstrumentMap=Maps.newConcurrentMap();
    
    /**
     * Mapping unique instrument to a MDReqID. 
     */
    private final Map<String,Instrument> mReqIDMap=Maps.newConcurrentMap();
    /**
     * Mapping unique instrument to a Token list. 
     */
    private final Map<Instrument,List<Token>> mTokenMap=Maps.newConcurrentMap();
    
    /**
     * Mapping unique OrderId to request message. 
     */
    private final Map<String,Message> mOpenOrders=Maps.newConcurrentMap();
    
    /**
     * the client lock
     */
	private final Object mLock = new Object();
	
	/**
	 * The ececution ID factory
	 */
	private final InMemoryIDFactory mIDFactory = new InMemoryIDFactory(100, "bg");
	
    // INSTANCE METHODS.   
	/**
     * Sets the receiver's data Broker.
     *
     * @param inDataBroker The data Broker.
     */
	public void setDataBroker(SpringBroker inDataBroker) {
		this.dataBroker = inDataBroker;
		inDataBroker.setClientKernel(this);
	}
    
    /**
     * Returns the receiver's data Broker.
     *
     * @return The data Broker.
     */
    public Broker getDataBroker() {
		return dataBroker;
	}
    
	/**
     * Sets the receiver's trade Broker.
     *
     * @param inTradeSession The trade Broker.
     */
	public void setTradeBroker(SpringBroker inTradeBroker) {
		this.tradeBroker = inTradeBroker;
		inTradeBroker.setClientKernel(this);
	}
    
    /**
     * Returns the receiver's trade Broker.
     *
     * @return The trade Broker.
     */
    public Broker getTradeBroker() {
		return tradeBroker;
	}
    
    /**
     * Random exchange
     */
    private SimulatedExchange getExchange(){
    	if(mExchange == null){
    		mExchange = new SimulatedExchange(getDataBroker().getBrokerID());
    	}
    	return mExchange;
    }
    
    /**
     * Returns the receiver's broker for the given QuickFIX/J session
     * ID. It logs an error and returns null if there is no broker for
     * the given ID.
     *
     * @param sessionID The ID.
     *
     * @return The broker. It may be null.
     */
    public Broker getBroker
        (SessionID sessionID)
    {
        Broker b=mSessionIDMap.get(sessionID);
        if (b==null) {
            Messages.INVALID_SESSION_ID.error(this,sessionID);
        }
        return b;
    }
    
    /**
     * Returns the receiver's in-memory map of instrument to server request message.
     *
     * @return The map.
     */
    private Map<Instrument,Message> getInstrumentMap()
    {
        return mInstrumentMap;
    }
    
    /**
     * Returns the receiver's in-memory map of MDReqID to request instrument.
     *
     * @return The map.
     */
    private Map<String,Instrument> getReqIDMap()
    {
        return mReqIDMap;
    }
    
    /**
     * Returns the receiver's in-memory map of Token list to request instrument.
     *
     * @return The map.
     */
    private Map<Instrument,List<Token>> getTokenMap()
    {
        return mTokenMap;
    }
    
    /**
     * Returns the receiver's in-memory map of OrderId to server order message.
     *
     * @return The map.
     */
    private Map<String,Message> getOpenOrdersMap()
    {
        return mOpenOrders;
    }
    
	/**
     * Returns the client lock.
     *
     * @return The lock.
     */
	private Object lock() {
		return mLock;
	}
	
    /**
     * Returns the receiver's ID generation factory.
     *
     * @return The factory.
     */
    public IDFactory getIDFactory()
    {
        return mIDFactory;
    }
    
	/**
	 * Receive a message to the broker's session.
	 *  
     * @param msg The message.
     * @param sessionID the receiver of the response.
	 */
 	@Override
	public void receiveMessage(Message msg, SessionID sessionID) 
 	{
 		Broker b = getBroker(sessionID);
 		
 		//If wrong broker send reject
 		if(b == null){
 			Session session = Session.lookupSession(sessionID);
 			Message reject = session.getMessageFactory().create(MsgType.REJECT, Messages.RH_UNKNOWN_BROKER.getText());
 			session.send(reject);		
 		}
 		
 		//If disconnected send reject
 		if(!isRunning()){
 			Session session = Session.lookupSession(sessionID);
 			Message reject = session.getMessageFactory().create(MsgType.REJECT, Messages.DK_ERROR_CONNECTION.getText());
 			session.send(reject);
 		}
 		
 		//Process the FIX message
 		synchronized (lock()) {
 			try {
		    	requestFIXToAPI(msg);
 			} catch (Throwable t) {
 				Session session = Session.lookupSession(sessionID);
 	 			Message reject = session.getMessageFactory().create(MsgType.REJECT, Messages.ERROR_REQUEST_PROCESS.getText(msg, t));
 	 			session.send(reject);
			}
 		}
 	}
    
    // InitializingBean.
    @Override
    public void afterPropertiesSet()
        throws I18NException
    {
        if (getDataBroker()==null) {
            throw new I18NException(Messages.NO_DATA_BROKER);
        }
        if (getTradeBroker()==null) {
            throw new I18NException(Messages.NO_TRADE_BROKER);
        }
        
        mSessionIDMap.put(getDataBroker().getSessionID(),getDataBroker());
        mSessionIDMap.put(getTradeBroker().getSessionID(),getTradeBroker());
    }

    /**
     * Starts the client, initialisation from spring only
     */
	public void start() {
	}

	/**
	 * Indicates if the Client is running
	 * 
	 * @return a <code>Boolean</code> value.
	 */
	public boolean isRunning() 
	{
		return true;
	}
	
   /*********************************************************FIX/DK************************************************/
    /**
     * Process a Request from FIX.
     * @param msg a <code>Message</code> value containing the initial request message
	 * @throws Exception 
     */
    private void requestFIXToAPI(Message msg) 
    		throws Exception 
    {
    	synchronized (lock()) {
    		Broker b = getTradeBroker();
    		
			if (FIXMessageUtil.isOrderSingle(msg)) {
				Instrument instrument = new Equity(msg
						.getString(Symbol.FIELD));
				String orderID = msg.getString(ClOrdID.FIELD);
				
				getExchange().subscribeToTicks(instrument, this);
				
				getOpenOrdersMap().put(orderID, msg);
				
				
				String execID = getIDFactory().getNext();
				String clOrderID = orderID;
				String inAccount = "bgAccount";	
				
				char ordStatus = 0;
				char executionType = 0;
				char side = msg.getChar(Side.FIELD);
				char orderType = msg.getChar(OrdType.FIELD);
				char timeInForce = msg.getChar(TimeInForce.FIELD);
				
				BigDecimal orderQty = BigDecimal.valueOf(msg.getDouble(OrderQty.FIELD));
				BigDecimal orderPrice = BigDecimal.ZERO;
				if (orderType == OrdType.LIMIT) {
					orderPrice = BigDecimal.valueOf(msg.getDouble(Price.FIELD));
				}
				
				Date sendingTime = new Date();
				Date transactTime = FIXUtil.getTransactTime(msg);
				
				String inText = null;
				BigDecimal cumQty = BigDecimal.ZERO;
				BigDecimal avgPrice = BigDecimal.ZERO;
		
				Message exeReport = null;
				
				ordStatus = OrdStatus.NEW;
				executionType = ExecType.NEW;
				exeReport = b.getFIXMessageFactory().newExecutionReport(orderID, clOrderID,
						execID, ordStatus, side, orderQty, orderPrice, cumQty, avgPrice,
						cumQty, avgPrice, instrument, inAccount, inText);
				exeReport.getHeader().setField(new SendingTime(sendingTime));
				exeReport.setField(new TransactTime(transactTime));
				exeReport.setField(new OrdType(orderType));
				exeReport.setField(new TimeInForce(timeInForce));
				exeReport.setField(new ExecType(executionType));
				exeReport.setField(new LeavesQty(orderQty));
		
				b.sendToTarget(exeReport);
			} else if (FIXMessageUtil.isCancelRequest(msg)) {
				String origOrderId = msg.getString(OrigClOrdID.FIELD);
				Message message = getOpenOrdersMap().remove(origOrderId);
				
				if(message != null){
					String execID = getIDFactory().getNext();
					String clOrderID = message.getString(ClOrdID.FIELD);
					String inAccount = "bgAccount";		
					
					char ordStatus = 0;
					char executionType = 0;
					char side = message.getChar(Side.FIELD);
					char orderType = message.getChar(OrdType.FIELD);
					char timeInForce = message.getChar(TimeInForce.FIELD);
					
					BigDecimal orderQty = BigDecimal.valueOf(message.getDouble(OrderQty.FIELD));
					BigDecimal orderPrice = BigDecimal.ZERO;
					if (orderType == OrdType.LIMIT) {
						orderPrice = BigDecimal.valueOf(msg.getDouble(Price.FIELD));
					}
					Instrument instrument = new Equity(message
							.getString(Symbol.FIELD));
					Date sendingTime = new Date();
					Date transactTime = FIXUtil.getTransactTime(message);
					
					String inText = null;
					BigDecimal cumQty = BigDecimal.ZERO;
					BigDecimal avgPrice = BigDecimal.ZERO;
					
					ordStatus = OrdStatus.CANCELED;
					executionType = ExecType.CANCELED;
					Message exeReport = b.getFIXMessageFactory().newExecutionReport(origOrderId, clOrderID,
							execID, ordStatus, side, orderQty, orderPrice, cumQty, avgPrice,
							cumQty, avgPrice, instrument, inAccount, inText);
					exeReport.setField(new OrigClOrdID(origOrderId));
					exeReport.getHeader().setField(new SendingTime(sendingTime));
					exeReport.setField(new TransactTime(transactTime));
					exeReport.setField(new OrdType(orderType));
					exeReport.setField(new TimeInForce(timeInForce));
					exeReport.setField(new ExecType(executionType));
					exeReport.setField(new LeavesQty(orderQty));
					
					b.sendToTarget(exeReport);
				}
			} else if (FIXMessageUtil.isCancelReplaceRequest(msg)) {
				String origOrderId = msg.getString(OrigClOrdID.FIELD);
				Message message = getOpenOrdersMap().remove(origOrderId);
				if(message != null){
					String execID = getIDFactory().getNext();
					String clOrderID = message.getString(ClOrdID.FIELD);
					String inAccount = "bgAccount";		
					
					char ordStatus = 0;
					char executionType = 0;
					char side = message.getChar(Side.FIELD);
					char orderType = message.getChar(OrdType.FIELD);
					char timeInForce = message.getChar(TimeInForce.FIELD);
					
					BigDecimal orderQty = BigDecimal.valueOf(message.getDouble(OrderQty.FIELD));
					BigDecimal orderPrice = BigDecimal.ZERO;
					if (orderType == OrdType.LIMIT) {
						orderPrice = BigDecimal.valueOf(msg.getDouble(Price.FIELD));
					}
					Instrument instrument = new Equity(message
							.getString(Symbol.FIELD));
					Date sendingTime = new Date();
					Date transactTime = FIXUtil.getTransactTime(message);
					
					String inText = null;
					BigDecimal cumQty = BigDecimal.ZERO;
					BigDecimal avgPrice = BigDecimal.ZERO;
					
					ordStatus = OrdStatus.CANCELED;
					executionType = ExecType.CANCELED;
					Message exeReport = b.getFIXMessageFactory().newExecutionReport(origOrderId, clOrderID,
							execID, ordStatus, side, orderQty, orderPrice, cumQty, avgPrice,
							cumQty, avgPrice, instrument, inAccount, inText);
					exeReport.setField(new OrigClOrdID(origOrderId));
					exeReport.getHeader().setField(new SendingTime(sendingTime));
					exeReport.setField(new TransactTime(transactTime));
					exeReport.setField(new OrdType(orderType));
					exeReport.setField(new TimeInForce(timeInForce));
					exeReport.setField(new ExecType(executionType));
					exeReport.setField(new LeavesQty(orderQty));
					
					b.sendToTarget(exeReport);
				}
				
				String orderID = msg.getString(ClOrdID.FIELD);
				getOpenOrdersMap().put(orderID, msg);
				
				Instrument instrument = new Equity(msg
						.getString(Symbol.FIELD));
				
				getExchange().subscribeToTicks(instrument, this);
				getOpenOrdersMap().put(orderID, msg);
				
				
				String execID = getIDFactory().getNext();
				String clOrderID = orderID;
				String inAccount = "bgAccount";	
				
				char ordStatus = 0;
				char executionType = 0;
				char side = msg.getChar(Side.FIELD);
				char orderType = msg.getChar(OrdType.FIELD);
				char timeInForce = msg.getChar(TimeInForce.FIELD);
				
				BigDecimal orderQty = BigDecimal.valueOf(msg.getDouble(OrderQty.FIELD));
				BigDecimal orderPrice = BigDecimal.valueOf(msg.getDouble(Price.FIELD));
				Date sendingTime = new Date();
				Date transactTime = FIXUtil.getTransactTime(msg);
				
				String inText = null;
				BigDecimal cumQty = BigDecimal.ZERO;
				BigDecimal avgPrice = BigDecimal.ZERO;
		
				Message exeReport = null;
				
				ordStatus = OrdStatus.NEW;
				executionType = ExecType.NEW;
				exeReport = b.getFIXMessageFactory().newExecutionReport(orderID, clOrderID,
						execID, ordStatus, side, orderQty, orderPrice, cumQty, avgPrice,
						cumQty, avgPrice, instrument, inAccount, inText);
				exeReport.getHeader().setField(new SendingTime(sendingTime));
				exeReport.setField(new TransactTime(transactTime));
				exeReport.setField(new OrdType(orderType));
				exeReport.setField(new TimeInForce(timeInForce));
				exeReport.setField(new ExecType(executionType));
				exeReport.setField(new LeavesQty(orderQty));
		
				b.sendToTarget(exeReport);
				
			}else if(FIXMessageUtil.isMarketDataRequest(msg)){
				SubscriptionRequestType type = FIXUtil.getSubscriptionType(msg);
				if(type == null){
					throw new I18NException(new I18NBoundMessage2P(Messages.ERROR_REQUEST_UNKNOWN, msg, "Type is null"));
				}
				if(type.getValue() == SubscriptionRequestType.DISABLE_PREVIOUS_SNAPSHOT_PLUS_UPDATE_REQUEST) {
					// Cancel market data
					try{
						String reqID = msg.getString(MDReqID.FIELD);
						Instrument instrument = getReqIDMap().remove(reqID);
						if(instrument  != null){
							getInstrumentMap().remove(instrument);
							List<Token> tokens = getTokenMap().remove(instrument);
							if(tokens != null){
								for(Token token:tokens){
									getExchange().cancel(token);
								}
							}
						}
					}catch(FieldNotFound ex){
						throw new I18NException(new I18NBoundMessage2P(Messages.ERROR_REQUEST_PROCESS, msg, ex));
					}
				} else{ 
					// Request Market data
					try{
						String reqID = msg.getString(MDReqID.FIELD);
						List<Group> groups = msg.getGroups(NoRelatedSym.FIELD);
						
						if(groups.size() != 1){
							throw new UnexpectedException("NoRelatedSym group size: " + groups.size());
						}
						
						for (Group group:groups) {
							String symbol = group.getString(Symbol.FIELD);
							if (symbol != null) {;
								Instrument instrument = new Equity(symbol);
								getInstrumentMap().put(instrument, msg);
								getReqIDMap().put(reqID, instrument);
								getTokenMap().put(instrument, Arrays.asList(
												getExchange().subscribeToTrades(instrument, this),
												getExchange().subscribeToTicks(instrument, this),
												getExchange().subscribeToDepths(instrument, this)));
							}
						}
					}catch(FieldNotFound ex){
						throw ex;
					}
				}
				/*if(!instruments.isEmpty()){
					getClient().setSubscribedInstruments(instruments);
				}*/
			} else if(FIXMessageUtil.isXMLMessage(msg)){
				if(!msg.getHeader().isSetField(XmlData.FIELD)){
					throw new I18NException(new I18NBoundMessage2P(Messages.ERROR_REQUEST_UNKNOWN, msg, "XmlData field is missing"));
				}
				String inXML = msg.getHeader().getString(XmlData.FIELD);
				Object inObject = MarshallerFactory.fromXML(inXML);
				
				if(!(inObject instanceof MapWrapper<?,?>)){
					throw new I18NException(new I18NBoundMessage2P(Messages.ERROR_REQUEST_UNKNOWN, msg, "Input is not a MapWrapper"));
				}
				Map<?,?> inputs = ((MapWrapper<?,?>)inObject).getMap();
				Object input0 = inputs.get(BasicJob.INSTRUMENT);
				if(input0 == null || !(input0 instanceof Instrument)){
					throw new I18NException(new I18NBoundMessage2P(Messages.ERROR_REQUEST_UNKNOWN, msg, "Instument is missing"));
				}
				Instrument instrument = (Instrument)input0;
				Object input1 = inputs.get(BasicJob.PERIOD);
				if(input1 == null || !(input1 instanceof org.marketcetera.core.time.Period)){
					throw new I18NException(new I18NBoundMessage2P(Messages.ERROR_REQUEST_UNKNOWN, msg, "Period is missing"));
				}
				org.marketcetera.core.time.Period period = (org.marketcetera.core.time.Period) inputs.get(BasicJob.PERIOD);
				Object input2 = inputs.get(BasicJob.HISTORY_BACK);
				if(input2 == null || !(input2 instanceof Long)){
					throw new I18NException(new I18NBoundMessage2P(Messages.ERROR_REQUEST_UNKNOWN, msg, "History length is missing"));
				}
				long historyBack = (long) input2;
				loadHistoricalData(instrument, period, historyBack);
			} else{
				throw new I18NException(new I18NBoundMessage2P(Messages.ERROR_REQUEST_UNKNOWN, msg, "Message type not supported"));
			}
    	}
	}

    /*
     * (non-Javadoc)
     * @see org.marketcetera.core.publisher.ISubscriber#isInteresting(java.lang.Object)
     */
	@Override
	public boolean isInteresting(Object inData) {
		return false;
	}

	/*
	 * (non-Javadoc)
	 * @see org.marketcetera.core.publisher.ISubscriber#publishTo(java.lang.Object)
	 */
	@Override
	public void publishTo(Object inData) 
	{
		Broker b = getDataBroker();
		
		if(!(inData instanceof Event)){
			return;
		}
		Event event = (Event)inData;
		Instrument instrument = event.getInstrument();
		
		if(event instanceof TickEvent){
			try {
				settleOrders((TickEvent)inData);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		// Only events from FIX are supported
		Message request = getInstrumentMap().get(instrument);
		if(request == null){
			return;
		}
		
		try{
			String reqID = request.getString(MDReqID.FIELD);
			
			Message msg = b.getFIXMessageFactory().newMarketDataFullRefreshEmpty();
			msg.setField(new quickfix.field.MDReqID(reqID));
			msg.setField(new Symbol(instrument.toString()));
			
			int depth = FIXUtil.getMarketDepth(request);
			if(depth == MarketDepth.TOTAL_VIEW.getValue()){
				depth = Integer.MAX_VALUE;
			}
			
			if(event instanceof TradeEvent){
				TradeEvent tradeEvent = (TradeEvent)event;
				Group group = b.getFIXMessageFactory().createGroup(
						MsgType.MARKET_DATA_SNAPSHOT_FULL_REFRESH, NoMDEntries.FIELD);

				group.setField(new MDEntryPx(tradeEvent.getTrade().getPrice()));
				group.setField(new MDEntrySize(tradeEvent.getTrade().getSize()));
				group.setField(new MDEntryDate(new Date(tradeEvent.getTime())));
				group.setField(new MDMkt(tradeEvent.getTrade().getExchange()));
				group.setField(new MDEntryType(MDEntryType.TRADE));
				msg.addGroup(group);
			}else if(event instanceof TickEvent){
				TickEvent tickEvent = (TickEvent)event;
				//ASK
				Group group = b.getFIXMessageFactory().createGroup(
						MsgType.MARKET_DATA_SNAPSHOT_FULL_REFRESH, NoMDEntries.FIELD);

				group.setField(new MDEntryPx(tickEvent.getAsk().getPrice()));
				group.setField(new MDEntrySize(tickEvent.getAsk().getSize()));
				group.setField(new MDEntryDate(new Date(tickEvent.getTime())));
				group.setField(new MDMkt(tickEvent.getAsk().getExchange()));
				group.setField(new MDEntryType(MDEntryType.OFFER));
				msg.addGroup(group);
				
				//BID
				group = b.getFIXMessageFactory().createGroup(
						MsgType.MARKET_DATA_SNAPSHOT_FULL_REFRESH, NoMDEntries.FIELD);

				group.setField(new MDEntryPx(tickEvent.getBid().getPrice()));
				group.setField(new MDEntrySize(tickEvent.getBid().getSize()));
				group.setField(new MDEntryDate(new Date(tickEvent.getTime())));
				group.setField(new MDMkt(tickEvent.getBid().getExchange()));
				group.setField(new MDEntryType(MDEntryType.BID));
				msg.addGroup(group);
			}else if(event instanceof DepthEvent){
				DepthEvent depthEvent = (DepthEvent)event;
				for(int i = 0; i < Math.min(depth, depthEvent.getAsks().size()); i++){
					Group group = b.getFIXMessageFactory().createGroup(
							MsgType.MARKET_DATA_SNAPSHOT_FULL_REFRESH, NoMDEntries.FIELD);

					group.setField(new MDEntryPx(depthEvent.getAsks().get(i).getPrice()));
					group.setField(new MDEntrySize(depthEvent.getAsks().get(i).getSize()));
					group.setField(new MDEntryDate(new Date(depthEvent.getTime())));
					group.setField(new MDMkt(depthEvent.getAsks().get(i).getExchange()));
					group.setField(new MDEntryType(MDEntryType.OFFER));
					msg.addGroup(group);
				}
				
				for(int i = 0; i < Math.min(depth, depthEvent.getBids().size()); i++){
					Group group = b.getFIXMessageFactory().createGroup(
							MsgType.MARKET_DATA_SNAPSHOT_FULL_REFRESH, NoMDEntries.FIELD);

					group.setField(new MDEntryPx(depthEvent.getBids().get(i).getPrice()));
					group.setField(new MDEntrySize(depthEvent.getBids().get(i).getSize()));
					group.setField(new MDEntryDate(new Date(depthEvent.getTime())));
					group.setField(new MDMkt(depthEvent.getBids().get(i).getExchange()));
					group.setField(new MDEntryType(MDEntryType.BID));
					msg.addGroup(group);
				}
			}else{
				return;
			}
			
			b.sendToTarget(msg);
		}catch (Exception e) {
			Messages.ERROR_RESPONSE_DATA.error(this, e, instrument, inData);
		}
	}
	
	private void settleOrders(TickEvent inData) throws Exception {
		Broker b = getDataBroker();
		
		for(Iterator<Entry<String, Message>> it = getOpenOrdersMap().entrySet().iterator(); it.hasNext();){
			Entry<String, Message> entry = it.next();
			String orderID = entry.getKey();
			Message msg = entry.getValue();
			
			Instrument instrument = new Equity(msg
					.getString(Symbol.FIELD));
			
			char side = msg.getChar(Side.FIELD);
			char orderType = msg.getChar(OrdType.FIELD);
			
			BigDecimal orderQty = BigDecimal.valueOf(msg.getDouble(OrderQty.FIELD));
			BigDecimal orderPrice = BigDecimal.ZERO;
			if (orderType == OrdType.LIMIT) {
				orderPrice = BigDecimal.valueOf(msg.getDouble(Price.FIELD));
			}
			
			boolean filled = false;
			
			if(!inData.getInstrument().equals(instrument)){
				return;
			}
			
			if(orderType == OrdType.MARKET){
				orderPrice = (side == Side.BUY)?inData.getAsk().getPrice():
					inData.getBid().getPrice();
				filled = true;
			} else if(orderType == OrdType.LIMIT){
				if((side == Side.BUY) && orderPrice.compareTo(inData.getAsk().getPrice())>=0){
					filled = true;
				}
				if((side == Side.SELL) && orderPrice.compareTo(inData.getBid().getPrice())<=0){
					filled = true;
				}
			}
			
			if(filled){
				String execID = getIDFactory().getNext();
				String clOrderID = orderID;
				String inAccount = "bgAccount";	
				
				char ordStatus = 0;
				char executionType = 0;
				char timeInForce = msg.getChar(TimeInForce.FIELD);
				
				
				Date sendingTime = new Date();
				Date transactTime = FIXUtil.getTransactTime(msg);
				
				String inText = null;
				BigDecimal cumQty = BigDecimal.ZERO;
				BigDecimal avgPrice = BigDecimal.ZERO;

				Message exeReport = null;
				
				//if(message.getOrder().getRequestedAmount()==message.getOrder().getAmount()){
				ordStatus = OrdStatus.FILLED;
				executionType = ExecType.FILL;
				/*}else{
					//ordStatus = OrdStatus.PARTIALLY_FILLED;
					//executionType = ExecType.PARTIAL_FILL;
				//}*/
				avgPrice = orderPrice;
				cumQty = orderQty;
				exeReport = b.getFIXMessageFactory().newExecutionReport(orderID, clOrderID,
						execID, ordStatus, side, orderQty, orderPrice, cumQty, avgPrice,
						cumQty, avgPrice, instrument, inAccount, inText);
				exeReport.getHeader().setField(new SendingTime(sendingTime));
				exeReport.setField(new TransactTime(transactTime));
				exeReport.setField(new OrdType(orderType));
				exeReport.setField(new TimeInForce(timeInForce));
				exeReport.setField(new ExecType(executionType));
				exeReport.setField(new LeavesQty(BigDecimal.ZERO));
				
				it.remove();
				b.sendToTarget(exeReport);
			}
			
		}		
	}

    /**
     * Load the historical data for the instrument and periods requested
     * @param instrument
     * @param period
     * @param historyBack
     */
	private void loadHistoricalData(Instrument inInstrument,
			Period inPeriod, long inHistoryBack) 
	{
		
		Broker b = getDataBroker();
		try {
			//Load historical data from the broker
			System.out.println("Loading Quotes for " + inInstrument);	
			long timeB = System.currentTimeMillis();
			List<QuoteEvent> eventList = getExchange().getHistory(inInstrument, inPeriod, inHistoryBack);
			
			for(QuoteEvent quoteEvent:eventList){
				Message msg = getDataBroker().getFIXMessageFactory()
						.newXMLMessageEmpty();
			
				String xml = MarshallerFactory.toXML(quoteEvent);
				msg.getHeader().setField(new XmlData(xml));
				msg.getHeader().setField(new XmlDataLen(xml.length()));
				
				b.sendToTarget(msg);
			}
		
			long timeA = System.currentTimeMillis();
			System.out.println("Done loading... " + Math.round((timeA-timeB)/1000) + "s");
    	} catch (Throwable e) {
    		throw new UnexpectedException("Load historical data error: " + e);
		}
	}
}
