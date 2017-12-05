/*
 * CATSBF CCFEA Algorithmic Trading Strategy Backtesting Framework
 * Copyright (C) 2011 Daniel Schiermer
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.marketcetera.exchange.orderServer;

import java.sql.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import quickfix.DoNotSend;
import quickfix.FieldNotFound;
import quickfix.IncorrectDataFormat;
import quickfix.IncorrectTagValue;
import quickfix.Message;
import quickfix.RejectLogon;
import quickfix.SessionID;
import quickfix.SessionSettings;
import quickfix.UnsupportedMessageType;
import quickfix.field.Account;
import quickfix.field.ClOrdID;
import quickfix.field.ExecTransType;
import quickfix.field.ExecType;
import quickfix.field.OrdRejReason;
import quickfix.field.OrdStatus;
import quickfix.field.OrdType;
import quickfix.field.OrderQty;
import quickfix.field.OrigClOrdID;
import quickfix.field.Price;
import quickfix.field.Side;
import quickfix.field.Symbol;
import quickfix.field.TimeInForce;
import quickfix.field.TransactTime;
import quickfix.fix44.ExecutionReport;
import quickfix.fix44.NewOrderSingle;
import quickfix.fix44.OrderCancelReject;
import org.marketcetera.exchange.backtesting.ClientPortfolio;
import org.marketcetera.exchange.backtesting.OrderBook;
import org.marketcetera.exchange.backtesting.SimulationTime;
import org.marketcetera.exchange.common.Configuration;
import org.marketcetera.exchange.marketEvents.Bid;
import org.marketcetera.exchange.marketEvents.Fill;
import org.marketcetera.exchange.marketEvents.MarketEvent;
import org.marketcetera.exchange.marketEvents.Offer;
import org.marketcetera.exchange.marketEvents.PartialFill;
import org.marketcetera.exchange.orderExecutionStrategy.IOrderExecutionStrategy;
import org.marketcetera.exchange.quickfix.FIXFactory;

/**
 * Quickfixj Application implementation for the order server.
 * All messages to the server goes through here and methods are implemented to
 * handle all supported messages.
 * FIX 4.4
 * 
 * @author <a href="mailto:catsbf@dasch.dk">Daniel Schiermer</a>
 * @version 0.1
 *
 */
public class OrderApplication extends quickfix.MessageCracker
implements quickfix.Application
{
	private ClientPortfolio clientPortfolio;
	private String clientID;
	private OrderReporter orderReporter;
	private IOrderExecutionStrategy orderExecutionStrategy;
	private SimulationTime simulationTime;
	private final Logger LOG = LoggerFactory.getLogger(this.getClass());

	/**
	 * Creates an instance of the server with the given session settings and uses
	 * the given order book simulator for the simulation.
	 * @param settings Session Settings
	 * @param orderExecutionStrategy Order execution strategy used to match orders
	 * @param clientPortfolio Client portfolio to use
	 */
	public OrderApplication(SessionSettings settings,
			IOrderExecutionStrategy orderExecutionStrategy,
			ClientPortfolio clientPortfolio)
	{
		this.orderExecutionStrategy = orderExecutionStrategy;
		this.clientPortfolio = clientPortfolio;
		this.clientID = Configuration.getInstance().getClientID();
		
		simulationTime = SimulationTime.getInstance();
		LOG.info("Order Server is being created...");
	}

	/**
	 * Called when a FIX message of type {@link NewOrderSingle} is received
	 * from a client. Only orders of type Good Till Cancel are accepted, and
	 * only for the symbol defined in the configuration file.
	 * Orders that do not comply with this are rejected.
	 * 
	 * An execution report is sent either confirming the fill, partial fill
	 * or placement of new order.
	 */
	public void onMessage(NewOrderSingle order, SessionID sessionID)
	throws FieldNotFound, UnsupportedMessageType, IncorrectTagValue
	{	
		MarketEvent marketEvent = getMarketEventFromMessage(order);
		String symbol = marketEvent.getSymbol();

		boolean isIllegalSymbol = false;
		//Check that the order comply with the configured symbol and is of type 'GTC'
		if((isIllegalSymbol = !symbol.equals(Configuration.getInstance().getSymbol()))
				|| order.getTimeInForce().getValue() != TimeInForce.GOOD_TILL_CANCEL) {
			//1 = Unknown symbol, 5 = unknown order
			int ordRejReason = isIllegalSymbol ? OrdRejReason.UNKNOWN_SYMBOL : OrdRejReason.UNKNOWN_ORDER;

			orderReporter.sendRejectMessage(marketEvent, ordRejReason);
			
			LOG.info("Rejected FIX order:\n" + order.toXML());
			return;
		}

		//1 = Market order, 2 = Limit order
		char side = order.getSide().getValue();

		Fill fill = null;

		if(side == Side.BUY)
			fill = orderExecutionStrategy.checkBidForExecution((Bid)marketEvent, OrderBook.getInstance().getOffers());
		else if(side == Side.SELL || side == Side.SELL_SHORT)
			fill = orderExecutionStrategy.checkOfferForExecution((Offer)marketEvent, OrderBook.getInstance().getBids());

		//If it was filled or partially filled add to client portfolio
		if(fill != null) {
			if(fill instanceof PartialFill) {
				clientPortfolio.addPartialFill((PartialFill)fill);
			}else if(fill instanceof Fill) {
				clientPortfolio.addFill(fill);
			}
		} else {
			clientPortfolio.addOpenPosition(marketEvent);
		}
	}

	/**
	 * Cancels the given order (if it exists in the client portfolio).
	 * If it is a partial filled order, the remaining part of the order
	 * is cancelled, but the filled part is not withdrawn.
	 * 
	 * An execution report is sent to confirm the order cancel.
	 */
	public void onMessage(quickfix.fix44.OrderCancelRequest order, SessionID sessionID)
	throws FieldNotFound, UnsupportedMessageType, IncorrectTagValue
	{	//Client Order ID of the existing order
		String origClOrdID = order.getOrigClOrdID().getValue();
		char execTransType = ExecTransType.CANCEL;
		char orderStatus = OrdStatus.CANCELED, execType = ExecType.CANCELED;
		char side = order.getSide().getValue();

		MarketEvent marketEvent;
		if(side == Side.BUY)
			marketEvent = clientPortfolio.getOpenBidPosition(origClOrdID);
		else
			marketEvent = clientPortfolio.getOpenOfferPosition(origClOrdID);

		//If position is null, there is no open position for the given clientID
		if(marketEvent != null) {
			if(side == Side.BUY)
				clientPortfolio.cancelBid(origClOrdID);
			else
				clientPortfolio.cancelOffer(origClOrdID);

			ExecutionReport executionReport = FIXFactory.newExecutionReport(
					marketEvent.getClientOrderID(), marketEvent.getSymbol(),
					side, marketEvent.getOrderQty(),
					marketEvent.getCumQty(), 0, marketEvent.getOrderPrice(),
					marketEvent.getAvgPrice(), marketEvent.getExecPrice(),
					marketEvent.getOrderType(),
					orderStatus, 0, marketEvent.getAccount(),
					execTransType, execType);

			executionReport.set(new OrigClOrdID(origClOrdID));

			orderReporter.sendMessage(executionReport);
		}
	}

	/**
	 * Handles messages of type {@link OrderCancelReject}
	 * NOT SUPPORTED
	 */
	public void onMessage(quickfix.fix44.OrderCancelReplaceRequest order,
			SessionID sessionID)
	throws FieldNotFound, UnsupportedMessageType, IncorrectTagValue
	{
		//TODO to be implemented in later release
	}

	/**
	 * Parses the given Quickfix message to a Market Event
	 * @param message Message to parse
	 * @return Parsed Market Event
	 * @throws FieldNotFound
	 */
	private MarketEvent getMarketEventFromMessage(Message message) throws FieldNotFound {
		long transactTime = 0;
		String clOrdID = null, symbol = null, account = null;
		int orderQty = 0, cumQty = 0;
		double orderPrice = 0, avgPrice = 0, executionPrice = 0;
		char orderType = 0, side = 0;

		//Account is optional		
		if(message.isSetField(Account.FIELD))
			account = message.getString(Account.FIELD);

		if(message.isSetField(Price.FIELD))
			orderPrice = message.getDouble(Price.FIELD); 

		orderQty = (int)message.getDouble(OrderQty.FIELD);
		clOrdID = message.getString(ClOrdID.FIELD);
		side = message.getChar(Side.FIELD);
		symbol = message.getString(Symbol.FIELD);
		transactTime = message.getUtcTimeStamp(TransactTime.FIELD).getTime();

		//Field from here not set in all accepted messages
		if(message.isSetField(OrdType.FIELD))
			orderType = message.getChar(OrdType.FIELD);

		MarketEvent marketEvent = null;
		if(side == Side.BUY) {
			marketEvent = new Bid(orderPrice,
					orderQty,
					orderQty,
					clOrdID,
					simulationTime.getTime(new Date(transactTime)),
					clientID,
					orderType,
					symbol,
					account);
		}
		else if (side == Side.SELL || side == Side.SELL_SHORT) {
			marketEvent = new Offer(orderPrice,
					orderQty,
					orderQty,
					clOrdID,
					simulationTime.getTime(new Date(transactTime)),
					clientID,
					orderType, symbol, account);
		}
			
		marketEvent.setAvgPrice(avgPrice);
		marketEvent.setCumQty(cumQty);
		marketEvent.setExecPrice(executionPrice);

		return marketEvent;
	}

	/**
	 * Notifies when a session is created
	 */
	public void onCreate(SessionID sessionID) {	}

	/**
	 * Notifies on valid logon
	 */
	public void onLogon(SessionID sessionID)
	{
		orderReporter = new OrderReporter(sessionID, clientPortfolio);
	}

	/**
	 * Notifies when the FIX session is no longer online
	 */
	public void onLogout(SessionID sessionID) {	}

	/**
	 * Allows for peaking at msgs from this apps FIX engine to
	 * the counter party
	 */
	public void toAdmin(Message msg, SessionID sessionID)
	{
	}

	/**
	 * Callback notify which is called when admin msgs are received by FIX from
	 * the counter party
	 */
	public void fromAdmin(Message msg, SessionID sessionID)
	throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue,
	RejectLogon
	{
	}

	/**
	 * Callback for app messages from this app, send to the counter party. 
	 */
	public void toApp(Message msg, SessionID sessionID)
	throws DoNotSend
	{
	}

	/**
	 * All app level messages comes through here. We need to crack the message
	 * and call the appropriate method.
	 */
	public void fromApp(Message msg, SessionID sessionID)
	throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue,
	UnsupportedMessageType
	{	// call base class message parser
		crack(msg, sessionID);
	}

	/**
	 * Sets the order reporter.
	 * NOTE This is only for testing purposes, and should not be used
	 * for anything else
	 * @param orderReporter The new order reporter
	 */
	public void setOrderReporter(OrderReporter orderReporter) {
		this.orderReporter = orderReporter;
	}
}