package org.marketcetera.server.ba.brokers.api;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.persistence.Transient;

import org.marketcetera.core.CoreException;
import org.marketcetera.core.IDFactory;
import org.marketcetera.core.InMemoryIDFactory;
import org.marketcetera.event.Quote;
import org.marketcetera.event.QuoteEvent;
import org.marketcetera.event.impl.QuoteEventBuilder;
import org.marketcetera.marketdata.MarketDepth;
import org.marketcetera.quickfix.FIXMessageUtil;
import org.marketcetera.server.ba.brokers.Broker;
import org.marketcetera.server.ba.brokers.Messages;
import org.marketcetera.server.ba.brokers.spring.SpringBroker;
import org.marketcetera.trade.Currency;
import org.marketcetera.trade.Equity;
import org.marketcetera.trade.FIXUtil;
import org.marketcetera.util.except.I18NException;
import org.marketcetera.util.log.I18NBoundMessage1P;
import org.marketcetera.util.log.I18NBoundMessage2P;
import org.marketcetera.util.log.SLF4JLoggerProxy;
import org.marketcetera.util.misc.ClassVersion;
import org.marketcetera.util.primitives.BigDecimalUtils;
import org.marketcetera.util.ws.wrappers.MapWrapper;
import org.marketcetera.ws.MarshallerFactory;
import org.marketcetera.ws.server.BasicJob;
import org.marketcetera.ws.server.CoreExecutors;

import com.dukascopy.api.Filter;
import com.dukascopy.api.IAccount;
import com.dukascopy.api.IBar;
import com.dukascopy.api.IContext;
import com.dukascopy.api.IEngine;
import com.dukascopy.api.IHistory;
import com.dukascopy.api.IMessage;
import com.dukascopy.api.IMessage.Reason;
import com.dukascopy.api.IMessage.Type;
import com.dukascopy.api.IOrder;
import com.dukascopy.api.IOrder.State;
import com.dukascopy.api.IStrategy;
import com.dukascopy.api.ITick;
import com.dukascopy.api.Instrument;
import com.dukascopy.api.JFException;
import com.dukascopy.api.OfferSide;
import com.dukascopy.api.Period;
import com.dukascopy.api.system.ClientFactory;
import com.dukascopy.api.system.IClient;
import com.dukascopy.api.system.ISystemListener;
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
import quickfix.field.MDReqID;
import quickfix.field.MsgType;
import quickfix.field.NoMDEntries;
import quickfix.field.NoRelatedSym;
import quickfix.field.OrdRejReason;
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
 * The configuration of a DK broker client.
 */
@ClassVersion("$Id: DKClient.java 16752 2013-11-14 02:54:13Z colin $")
public class DKClient implements ClientKernel, Runnable
{
	// CLASS DATA
    private String mJnlpurl;
	private String mUsername;
	private String mPassword;
	private Broker dataBroker;
	private Broker tradeBroker;
	
	@Transient
	private IClient mDKClient;
	
	@Transient
	private DKStrategy mDKStrategy;
	
	@Transient
	private Map<SessionID,Broker> mSessionIDMap=Maps.newConcurrentMap();
	
    /**
     * indicates how frequently to check for market data server (in ms)
     */
    private static final long BROKER_CHECK_FREQUENCY = 45000;
    
    /**
     * The label of the execution
     */
    private static final String DK_LABEL = "dk_";
    
    /**
     * Mapping unique instrument to request message. 
     */
    private final Map<Instrument,Message> mInstrumentMap=Maps.newConcurrentMap();
    
    /**
     * Mapping unique instrument to a MDReqID. 
     */
    private final Map<String,Instrument> mReqIDMap=Maps.newConcurrentMap();
    
    /**
     * the client lock
     */
	private final Object mLock = new Object();
	
	/**
	 * The ececution ID factory
	 */
	private final InMemoryIDFactory mIDFactory = new InMemoryIDFactory(100, "dk");
	
    // INSTANCE METHODS.   
	/**
     * Sets the broker's url to the given value.
     *
     * @param url The URL.
     */
    public void setJnlpurl
        (String url)
    {
        mJnlpurl=url;
    }

    /**
     * Returns the broker's url.
     *
     * @return The URL.
     */
    public String getJnlpurl()
    {
        return mJnlpurl;
    }
	
	/**
     * Sets the broker's username to the given value.
     *
     * @param username The name.
     */
    public void setUsername
        (String username)
    {
        mUsername=username;
    }

    /**
     * Returns the broker's username.
     *
     * @return The name.
     */
    public String getUsername()
    {
        return mUsername;
    }
    
	/**
     * Sets the broker's password to the given value.
     *
     * @param password The password.
     */
    public void setPassword
        (String password)
    {
        mPassword=password;
    }

    /**
     * Returns the broker's password.
     *
     * @return The password.
     */
    public String getPassword()
    {
        return mPassword;
    }
    
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
     * Dukacsopy broker client
     */
    private IClient getClient(){
    	return mDKClient;
    }
    
    /**
     * Returns the broker's strategy.
     *
     * @return The strategy.
     */
    private DKStrategy getDKStrategy()
    {
        return mDKStrategy;
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
    private Map<Instrument, Message> getInstrumentMap()
    {
        return mInstrumentMap;
    }
    
    /**
     * Returns the receiver's in-memory map of MDReqID to request instrument.
     *
     * @return The map.
     */
    private Map<String, Instrument> getReqIDMap()
    {
        return mReqIDMap;
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
	 			Future<Void> future = getDKStrategy().getContext().executeTask(new Callable<Void>() {
		            public Void call() throws Exception {
		            	requestFIXToAPI(msg);
		            	return null;
		            }
			    });
			    future.get();
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
        if (getJnlpurl()==null) {
            throw new I18NException(Messages.DK_NO_JNLP_URL);
        }
    	if (getUsername()==null) {
            throw new I18NException(Messages.DK_NO_USERNAME);
        }
        if (getPassword()==null) {
            throw new I18NException(Messages.DK_NO_PASSWORD);
        }
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
		synchronized (lock()) {
			mDKStrategy=new DKStrategy();
	    	try {
		    	//Initiate the client
				mDKClient = ClientFactory.getDefaultInstance();
		
		    	//Set the listener that will receive system events
				getClient().setSystemListener(new ISystemListener() {
					Long mProcessId;
		        	@Override
		        	public void onStart(long processId) {}
		
					@Override
					public void onStop(long processId) {}
		
					@Override
					public void onConnect() {
						if(!getClient().getStartedStrategies()
								.containsValue(getDKStrategy())){
					        mProcessId = getClient().startStrategy(getDKStrategy());
							//Start running strategy
							Messages.DK_OK_STRATEGY.info(this);
						}
						Messages.DK_OK_CONNECTION.info(this);
					}
		
					@Override
					public void onDisconnect() {
						Messages.DK_ERROR_DISCONNECTED.info(this);
						if(mProcessId != null){
							getClient().stopStrategy(mProcessId);
							mProcessId = null;
						}
					}
				});
	    	
				CoreExecutors.scheduleAtFixedRate(this, 0,
						BROKER_CHECK_FREQUENCY, TimeUnit.MILLISECONDS);
	    	}catch(Throwable e){
	    		throw new CoreException(new I18NException(e, Messages.DK_ERROR_CONNECTION));
	    	}
		}
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		synchronized (lock()) {
			try {
				//Check connected
				//Sync the broker status 
				if(!getClient().isConnected()) {
		    		//Connect to the server using jnlp, user name and password
		    		SLF4JLoggerProxy.debug(DKClient.class, "Connecting to Dukascopy..."); 
		    		getClient().connect(mJnlpurl, mUsername, mPassword);
		    		
		            //wait for it to connect
		            int i = 200; //wait max twenty seconds
		            while (i > 0 && !getClient().isConnected()) {
		                Thread.sleep(100);
		                i--;
		            }
		        }
			}catch(Throwable ignore){}	
		}
	}

	/**
	 * Indicates if the Client is running
	 * 
	 * @return a <code>Boolean</code> value.
	 */
	public boolean isRunning() 
	{
		synchronized (lock()) {
			if(getClient() == null)
			{
				return false;
			}
			if(!getClient().isConnected())
			{
				return false;
			}
			if(getDKStrategy() == null)
			{
				return false;
			}
			if(getDKStrategy().getContext() == null)
			{
				return false;
			}
			return true;
		}
	}
	
    private class DKStrategy implements IStrategy 
    {
    	private IContext mContext;
    	
		@Override
		public void onStart(IContext context) 
				throws JFException 
		{
			mContext = context;
		}
		
		private IContext getContext() 
		{
			return mContext;
		}

    	private IEngine getEngine() 
    	{
			return getContext().getEngine();
		}
    	
		private IAccount getAccount() 
		{
			return getContext().getAccount();
		}
		
		private IHistory getHistory() 
		{
			return getContext().getHistory();
		}
		
		@Override
		public void onTick(Instrument instrument, ITick tick) 
				throws JFException 
		{
			try {
				dataAPIToFIX(instrument, tick);
			} catch (Exception e) {
				Messages.ERROR_RESPONSE_DATA.error(this, e, instrument, tick);
			}
		}

		@Override
		public void onMessage(IMessage message) 
				throws JFException 
		{
			try {
				if (message.getType().equals(Type.ORDER_SUBMIT_OK)
						|| message.getType().equals(Type.ORDER_FILL_OK)
						|| message.getType().equals(Type.ORDER_SUBMIT_REJECTED)
						|| message.getType().equals(Type.ORDER_FILL_REJECTED)
						|| message.getType().equals(Type.ORDER_CHANGED_OK)
						|| message.getType().equals(Type.ORDER_CLOSE_OK)
						|| message.getType().equals(Type.ORDER_CLOSE_REJECTED)
						|| message.getType().equals(Type.ORDER_CHANGED_REJECTED)) {
					tradeAPIToFIX(message);
				}
			} catch (Exception e) {
				Messages.DK_ERROR_RESPONSE_TRADE.error(this, e, message);
			}
		}

		@Override
		public void onBar(Instrument instrument, Period period, IBar askBar, IBar bidBar) 
				throws JFException {}
		
		@Override
		public void onAccount(IAccount account) 
				throws JFException {}

		@Override
		public void onStop() 
				throws JFException {}
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
		//Instrument
		Set<Instrument> instruments = getClient().getSubscribedInstruments();

		if (FIXMessageUtil.isOrderSingle(msg)) {
			Instrument instrument = Instrument.fromString(msg
					.getString(Symbol.FIELD));
			if(!instruments.contains(instrument)){
				instruments.add(instrument);
				getClient().setSubscribedInstruments(instruments);
			}
			
			double amount = msg.getDouble(OrderQty.FIELD);
			IEngine.OrderCommand orderCommand = null;
			String orderId = msg.getString(ClOrdID.FIELD);
			String newLabel = DK_LABEL + orderId;
			char side = msg.getChar(Side.FIELD);
			char orderType = msg.getChar(OrdType.FIELD);
			char timeInForce = msg.getChar(TimeInForce.FIELD);
			long goodTillTime = 0L;

			if (orderType == OrdType.LIMIT) {
				double price = msg.getDouble(Price.FIELD);
				orderCommand = (side == Side.BUY) ? IEngine.OrderCommand.BUYLIMIT
						: IEngine.OrderCommand.SELLLIMIT;

				if (timeInForce == TimeInForce.DAY) {
					goodTillTime = System.currentTimeMillis() + Period.DAILY.getInterval();
				} else if ((timeInForce == TimeInForce.IMMEDIATE_OR_CANCEL)
						|| (timeInForce == TimeInForce.FILL_OR_KILL)) {
					goodTillTime = System.currentTimeMillis() + Period.ONE_MIN.getInterval();
				}
				getDKStrategy().getEngine().submitOrder(newLabel, instrument,
						orderCommand, amount, price, 0., 0., 0., goodTillTime, orderId);
			} else {
				orderCommand = (side == Side.BUY) ? IEngine.OrderCommand.BUY
						: IEngine.OrderCommand.SELL;
				getDKStrategy().getEngine().submitOrder(newLabel, instrument,
						orderCommand, amount, 0., 0., 0., 0., goodTillTime, orderId);
			}
		} else if (FIXMessageUtil.isCancelRequest(msg)) {
			String prevLabel = DK_LABEL + msg.getString(OrigClOrdID.FIELD);
			String newLabel = DK_LABEL + msg.getString(ClOrdID.FIELD);
			IOrder iOrder = getDKStrategy().getEngine().getOrder(prevLabel);
			if(iOrder != null){
				iOrder.setLabel(newLabel);
				iOrder.close();
			}
		} else if (FIXMessageUtil.isCancelReplaceRequest(msg)) {
			Instrument instrument = Instrument.fromString(msg
					.getString(Symbol.FIELD));
			if(!instruments.contains(instrument)){
				instruments.add(instrument);
				getClient().setSubscribedInstruments(instruments);
			}
			
			double price = msg.getDouble(Price.FIELD);
			double amount = msg.getDouble(OrderQty.FIELD);
			char side = msg.getChar(Side.FIELD);
			String prevLabel = DK_LABEL + msg.getString(OrigClOrdID.FIELD);
			
			String orderId = msg.getString(ClOrdID.FIELD);
			String newLabel = DK_LABEL + orderId;
			
			IOrder iOrder = getDKStrategy().getEngine().getOrder(prevLabel);
			if (iOrder == null) {
				throw new I18NException(new I18NBoundMessage1P(Messages.DK_ERROR_TRADE_NOTFOUND, msg));
			}

			iOrder.setLabel(newLabel);
			iOrder.close();
			
			IEngine.OrderCommand orderCommand = (side == Side.BUY) ? IEngine.OrderCommand.BUYLIMIT
					: IEngine.OrderCommand.SELLLIMIT;
			char timeInForce = msg.getChar(TimeInForce.FIELD);
			long goodTillTime = 0L;
			
			if (timeInForce == TimeInForce.DAY) {
				goodTillTime = System.currentTimeMillis() + Period.DAILY.getInterval();
			} else if ((timeInForce == TimeInForce.IMMEDIATE_OR_CANCEL)
					|| (timeInForce == TimeInForce.FILL_OR_KILL)) {
				goodTillTime = System.currentTimeMillis() + Period.ONE_MIN.getInterval();
			}
			getDKStrategy().getEngine().submitOrder(newLabel, instrument,
					orderCommand, amount, price, 0., 0., 0., goodTillTime, orderId);
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
						instruments.remove(instrument);
						getInstrumentMap().remove(instrument);
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
						if (symbol != null) {
							Instrument instrument = Instrument.fromString(symbol);
							instruments.add(instrument);
							getInstrumentMap().put(instrument, msg);
							getReqIDMap().put(reqID, instrument);
						}
					}
				}catch(FieldNotFound ex){
					throw ex;
				}
			}
			if(!instruments.isEmpty()){
				getClient().setSubscribedInstruments(instruments);
			}
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
			if(input0 == null || !(input0 instanceof org.marketcetera.trade.Instrument)){
				throw new I18NException(new I18NBoundMessage2P(Messages.ERROR_REQUEST_UNKNOWN, msg, "Instument is missing"));
			}
			org.marketcetera.trade.Instrument instrument = (org.marketcetera.trade.Instrument)input0;
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

	/**
     * Process a tick from the broker.
     * 
	 * @param instrument the api instrument
	 * @param tick the market data
	 * 
	 * @throws FieldNotFound  
	 */
	private void dataAPIToFIX(Instrument instrument, ITick tick) 
			throws FieldNotFound 
	{
		Broker b = getDataBroker();
		
		// Only events from FIX are supported
		Message request = getInstrumentMap().get(instrument);
		if(request == null){
			return;
		}
		
		try{
			String reqID = request.getString(MDReqID.FIELD);
			
			//Get the depth
			int depth = FIXUtil.getMarketDepth(request);
			if(depth == MarketDepth.TOTAL_VIEW.getValue()){
				depth = Math.max(tick.getAsks().length, 
							tick.getBids().length);
			}
			
			Message msg = b.getFIXMessageFactory().newMarketDataFullRefreshEmpty();
			msg.setField(new quickfix.field.MDReqID(reqID));
			msg.setField(new Symbol(instrument.toString()));
			
			for(int i = 0; i < Math.min(depth, tick.getAsks().length); i++){
				Group group = b.getFIXMessageFactory().createGroup(
						MsgType.MARKET_DATA_SNAPSHOT_FULL_REFRESH, NoMDEntries.FIELD);

				group.setField(new MDEntryPx(BigDecimal.valueOf(tick.getAsks()[i])));
				group.setField(new MDEntrySize(BigDecimal.valueOf(tick.getAskVolumes()[i])));
				group.setField(new MDEntryDate(new Date(tick.getTime())));
				group.setField(new MDEntryType(MDEntryType.OFFER));
				msg.addGroup(group);
			}
			
			for(int i = 0; i < Math.min(depth, tick.getBids().length); i++){
				Group group = b.getFIXMessageFactory().createGroup(
						MsgType.MARKET_DATA_SNAPSHOT_FULL_REFRESH, NoMDEntries.FIELD);
				
				group.setField(new MDEntryPx(BigDecimal.valueOf(tick.getBids()[i])));
				group.setField(new MDEntrySize(BigDecimal.valueOf(tick.getBidVolumes()[i])));
				group.setField(new MDEntryDate(new Date(tick.getTime())));
				group.setField(new MDEntryType(MDEntryType.BID));
				msg.addGroup(group);
			}
			b.sendToTarget(msg);
		}catch (Exception e) {
			Messages.ERROR_RESPONSE_DATA.error(this, e, instrument, tick);
		}
	}
	
	/**
     * Process a trade from the broker.
     * @param message a <code>IMessage</code> value containing the Dukascopy execution report
     * 
	 * @throws FieldNotFound 
     */
	private void tradeAPIToFIX(IMessage message) 
			throws FieldNotFound 
	{
		try{
			// Only trades from FIX are supported
			if (!message.getOrder().getLabel().startsWith(DK_LABEL)) {
				return;
			}
			
			Broker b = getTradeBroker();
			
			String orderID = message.getOrder().getId();
			String execID = getIDFactory().getNext();
			String clOrderID = message.getOrder().getLabel().replaceFirst(DK_LABEL, "");
			String inAccount = getDKStrategy().getAccount().getAccountId();		
			
			char ordStatus = 0;
			char executionType = 0;
			char side = (message.getOrder().getOrderCommand()
					.equals(IEngine.OrderCommand.BUYLIMIT))
					|| (message.getOrder().getOrderCommand()
					.equals(IEngine.OrderCommand.BUY)) ? Side.BUY: Side.SELL;
			char orderType = (message.getOrder().getOrderCommand()
					.equals(IEngine.OrderCommand.BUYLIMIT))
					|| (message.getOrder().getOrderCommand()
					.equals(IEngine.OrderCommand.SELLLIMIT)) ? OrdType.LIMIT: OrdType.MARKET;
	
			char timeInForce = (message.getOrder().getGoodTillTime() == 0L)? TimeInForce.GOOD_TILL_CANCEL:
				((message.getOrder().getGoodTillTime() >= Period.ONE_MIN.getInterval())?TimeInForce.DAY:TimeInForce.IMMEDIATE_OR_CANCEL);
			
			BigDecimal orderQty = BigDecimal.valueOf(message.getOrder().getRequestedAmount());
			BigDecimal orderPrice = BigDecimal.valueOf(message.getOrder().getOpenPrice());
			
			org.marketcetera.trade.Instrument instrument = null;
			try{
				instrument = new Currency(message.getOrder().getInstrument().toString());
			}catch(Exception e){
				instrument = new Equity(message.getOrder().getInstrument().toString());
			}
			
			Date sendingTime = new Date();
			Date transactTime = new Date(message.getOrder().getCreationTime());
			
			String inText = null;
			BigDecimal cumQty = BigDecimal.ZERO;
			BigDecimal avgPrice = BigDecimal.ZERO;
			OrdRejReason rejReason = new OrdRejReason(OrdRejReason.OTHER);
	
			Message exeReport = null;
			switch (message.getType()) {
				case ORDER_SUBMIT_OK: {
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
					break;
				}
				case ORDER_FILL_OK: {
					if(message.getOrder().getRequestedAmount()==message.getOrder().getAmount()){
						ordStatus = OrdStatus.FILLED;
						executionType = ExecType.FILL;
					}else{
						ordStatus = OrdStatus.PARTIALLY_FILLED;
						executionType = ExecType.PARTIAL_FILL;
					}
					avgPrice = orderPrice;
					cumQty = BigDecimal.valueOf(message.getOrder().getAmount());
					exeReport = b.getFIXMessageFactory().newExecutionReport(orderID, clOrderID,
							execID, ordStatus, side, orderQty, orderPrice, cumQty, avgPrice,
							cumQty, avgPrice, instrument, inAccount, inText);
					exeReport.getHeader().setField(new SendingTime(sendingTime));
					exeReport.setField(new TransactTime(transactTime));
					exeReport.setField(new OrdType(orderType));
					exeReport.setField(new TimeInForce(timeInForce));
					exeReport.setField(new ExecType(executionType));
					exeReport.setField(new LeavesQty(BigDecimal.ZERO));
					break;
				}
				case ORDER_CLOSE_OK: {
					if (message.getOrder().getState().equals(State.CANCELED)) {
						String originalOrderID = message.getOrder().getComment();
						ordStatus = OrdStatus.CANCELED;
						executionType = ExecType.CANCELED;
						exeReport = b.getFIXMessageFactory().newExecutionReport(orderID, clOrderID,
								execID, ordStatus, side, orderQty, orderPrice, cumQty, avgPrice,
								cumQty, avgPrice, instrument, inAccount, inText);
						exeReport.setField(new OrigClOrdID(originalOrderID));
						exeReport.getHeader().setField(new SendingTime(sendingTime));
						exeReport.setField(new TransactTime(transactTime));
						exeReport.setField(new OrdType(orderType));
						exeReport.setField(new TimeInForce(timeInForce));
						exeReport.setField(new ExecType(executionType));
						exeReport.setField(new LeavesQty(orderQty));
					} else {
						return;
					}
					break;
				}
				case ORDER_SUBMIT_REJECTED: {
					executionType = ExecType.REJECTED;
					inText = Arrays.toString(message.getReasons().toArray()).toString();
					exeReport = b.getFIXMessageFactory().newRejectExecutionReport(orderID, clOrderID,
							execID, side, orderQty, cumQty, avgPrice, instrument,
							rejReason, inAccount, inText);
					exeReport.getHeader().setField(new SendingTime(sendingTime));
					exeReport.setField(new TransactTime(transactTime));
					exeReport.setField(new OrdType(orderType));
					exeReport.setField(new TimeInForce(timeInForce));
					exeReport.setField(new ExecType(executionType));
					exeReport.setField(new LeavesQty(orderQty));
					break;
				}
				case ORDER_FILL_REJECTED: {
					executionType = ExecType.REJECTED;
					inText = Arrays.toString(message.getReasons().toArray()).toString();
					exeReport = b.getFIXMessageFactory().newRejectExecutionReport(orderID, clOrderID,
							execID, side, orderQty, cumQty, avgPrice, instrument,
							rejReason, inAccount, inText);
					exeReport.getHeader().setField(new SendingTime(sendingTime));
					exeReport.setField(new TransactTime(transactTime));
					exeReport.setField(new OrdType(orderType));
					exeReport.setField(new TimeInForce(timeInForce));
					exeReport.setField(new ExecType(executionType));
					exeReport.setField(new LeavesQty(orderQty));
					break;
				}
				case ORDER_CLOSE_REJECTED: {
					String originalOrderID = message.getOrder().getComment();
					executionType = ExecType.REJECTED;
					inText = Arrays.toString(message.getReasons().toArray()).toString();
					exeReport = b.getFIXMessageFactory().newRejectExecutionReport(orderID, clOrderID,
							execID, side, orderQty, cumQty, avgPrice, instrument,
							rejReason, inAccount, inText);
					exeReport.setField(new OrigClOrdID(originalOrderID));
					exeReport.getHeader().setField(new SendingTime(sendingTime));
					exeReport.setField(new TransactTime(transactTime));
					exeReport.setField(new OrdType(orderType));
					exeReport.setField(new TimeInForce(timeInForce));
					exeReport.setField(new ExecType(executionType));
					exeReport.setField(new LeavesQty(orderQty));
					break;
				}
				case ORDER_CHANGED_OK:
					if(message.getReasons().contains(Reason.ORDER_CHANGED_PRICE) || 
							message.getReasons().contains(Reason.ORDER_CHANGED_AMOUNT)){
						String originalOrderID = message.getOrder().getComment();
						ordStatus = OrdStatus.REPLACED;
						executionType = ExecType.REPLACE;
						exeReport = b.getFIXMessageFactory().newExecutionReport(orderID, clOrderID,
								execID, ordStatus, side, orderQty, orderPrice, cumQty, avgPrice,
								cumQty, avgPrice, instrument, inAccount, inText);
						exeReport.setField(new OrigClOrdID(originalOrderID));
						exeReport.getHeader().setField(new SendingTime(sendingTime));
						exeReport.setField(new TransactTime(transactTime));
						exeReport.setField(new OrdType(orderType));
						exeReport.setField(new TimeInForce(timeInForce));
						exeReport.setField(new ExecType(executionType));
						exeReport.setField(new LeavesQty(orderQty));
						break;
					}
				case ORDER_CHANGED_REJECTED: 
				default:
					return;
			}
			b.sendToTarget(exeReport);
		}catch (Exception e) {
			Messages.DK_ERROR_RESPONSE_TRADE.error(this, e, message);
		}
	}

	
    /**
     * Load the historical data for the instrument and periods requested
     * @param instrument
     * @param period
     * @param historyBack
     */
	private void loadHistoricalData(org.marketcetera.trade.Instrument instrument,
			org.marketcetera.core.time.Period inPeriod, long historyBack) 
	{
		
		Broker b = getDataBroker();
		
		//Instrument
		Set<Instrument> instruments = getClient().getSubscribedInstruments();
				
		Period period = Period.valueOf(inPeriod.name());
		Instrument dkInstrument = Instrument.fromString(instrument.getSymbol());
		if(!instruments.contains(dkInstrument)){
			instruments.add(dkInstrument);
			getClient().setSubscribedInstruments(instruments);
		}
		
		//Load historical data from the broker
		System.out.println("Loading Quotes for " + dkInstrument);
		long timeB = System.currentTimeMillis();
    	try {
    		long endTime = getDKStrategy().getHistory().getStartTimeOfCurrentBar(dkInstrument, period);
    		long startTime = endTime - historyBack;
			List<IBar> bidBars = getDKStrategy().getHistory().getBars(dkInstrument, period, OfferSide.BID, Filter.WEEKENDS, startTime, endTime);
			List<IBar> askBars = getDKStrategy().getHistory().getBars(dkInstrument, period, OfferSide.ASK, Filter.WEEKENDS, startTime, endTime);
			
			if(bidBars == null || askBars == null || bidBars.size() != askBars.size()){
				//Give it an other try
				bidBars = getDKStrategy().getHistory().getBars(dkInstrument, period, OfferSide.BID, Filter.WEEKENDS, startTime, endTime);
				askBars = getDKStrategy().getHistory().getBars(dkInstrument, period, OfferSide.ASK, Filter.WEEKENDS, startTime, endTime);
				
				if(bidBars.size() != askBars.size()){
					throw new UnexpectedException("IBar bid size: " + bidBars.size() + " vs ask size: " + askBars.size());
				}
			}
			
			for(int i = 0; i < bidBars.size(); i++){
				long barTime = bidBars.get(i).getTime();
				IBar bidBar =  bidBars.get(i);
				IBar askBar =  askBars.get(i);
				if((bidBar.getHigh() > bidBar.getLow())){
					BigDecimal inOpen = BigDecimalUtils.precision(
							askBar.getOpen(), bidBar.getOpen(), (askBar.getOpen() + bidBar.getOpen())*0.5);
					BigDecimal inHigh = BigDecimalUtils.precision(
							askBar.getHigh(), bidBar.getHigh(), (askBar.getHigh() + bidBar.getHigh())*0.5);
					BigDecimal inLow = BigDecimalUtils.precision(
							askBar.getLow(), bidBar.getLow(), (askBar.getLow() + bidBar.getLow())*0.5);
					BigDecimal inClose = BigDecimalUtils.precision(
							askBar.getClose(), bidBar.getClose(), (askBar.getClose() + bidBar.getClose())*0.5);
					BigDecimal inVolume = BigDecimalUtils.precision(
							askBar.getVolume(), bidBar.getVolume(), (askBar.getVolume() + bidBar.getVolume())*0.5);
					BigDecimal inSpread = BigDecimalUtils.precision(
							askBar.getOpen(), askBar.getClose(), (askBar.getOpen() - bidBar.getOpen())*0.5 
							+ (askBar.getClose() - bidBar.getClose())*0.5);
					
					Quote quote = new Quote(barTime, 
							inOpen, inHigh, inLow, inClose, inVolume, inSpread);
					
					QuoteEvent quoteEvent = QuoteEventBuilder.quoteEvent()
										.withBrokerID(getDataBroker().getBrokerID())
										.withInstrument(instrument)
										.withQuote(quote)
										.withPeriod(inPeriod)
										.create();
					
					Message msg = getDataBroker().getFIXMessageFactory()
							.newXMLMessageEmpty();
				
					String xml = MarshallerFactory.toXML(quoteEvent);
					msg.getHeader().setField(new XmlData(xml));
					msg.getHeader().setField(new XmlDataLen(xml.length()));
					
					b.sendToTarget(msg);
				}
			}
    		
			long timeA = System.currentTimeMillis();
			System.out.println("Done loading... " + Math.round((timeA-timeB)/1000) + "s");
    	} catch (Throwable e) {
    		throw new UnexpectedException("Load historical data error: " + e);
		}
	}
}
