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
package org.marketcetera.exchange.marketDataServer;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import quickfix.ConfigError;
import quickfix.DoNotSend;
import quickfix.FieldConvertError;
import quickfix.FieldNotFound;
import quickfix.Group;
import quickfix.IncorrectDataFormat;
import quickfix.IncorrectTagValue;
import quickfix.Message;
import quickfix.RejectLogon;
import quickfix.SessionID;
import quickfix.SessionSettings;
import quickfix.UnsupportedMessageType;
import quickfix.field.MDReqID;
import quickfix.field.NoRelatedSym;
import quickfix.field.SubscriptionRequestType;
import quickfix.field.Symbol;
import quickfix.fix44.MarketDataRequest;
import quickfix.fix44.ResendRequest;
import org.marketcetera.exchange.backtesting.OrderBook;
import org.marketcetera.exchange.backtesting.OrderBookSimulator;
import org.marketcetera.exchange.common.Configuration;

/**
 * Quickfixj Application implementation for the market data server.
 * All messages to the server goes through here and methods are implemented to
 * handle all supported messages.
 * FIX 4.4
 * 
 * @author <a href="mailto:catsbf@dasch.dk">Daniel Schiermer</a>
 * @version 0.1
 * 
 */
public class MarketDataApplication extends quickfix.MessageCracker
implements quickfix.Application
{
	private OrderBookSimulator orderBookSimulator;
	private Thread orderBookPlaybackThread;
	private boolean isSimulationRunning = false;
	private MarketDataReporter marketDataReporter;
	private static final Logger LOG = LoggerFactory.getLogger(MarketDataApplication.class);

	/**
	 * Creates an instance of the server with the given session settings and uses
	 * the given order book simulator for the simulation.
	 * @param settings Session Settings
	 * @param orderBookSimulator Order book simulator to use for order book replay
	 * @throws ConfigError
	 * @throws FieldConvertError
	 */
	public MarketDataApplication(SessionSettings settings, OrderBookSimulator orderBookSimulator)
	throws ConfigError, FieldConvertError {
		this.orderBookSimulator = orderBookSimulator;
		LOG.info("Market Data Server is starting...");
	}

	/**
	 * Handles {@link ResendRequest}
	 * NOT SUPPORTED
	 */
	public void onMessage(quickfix.fix44.ResendRequest order,
			SessionID sessionID)
	throws FieldNotFound, UnsupportedMessageType, IncorrectTagValue
	{
		//TODO to be implemented in later release
	}

	/**
	 * Handles {@link MarketDataRequest}'s. Starts the historic data
	 * simulation if the subscription request is of type
	 * {@link quickfix.field.SubscriptionRequestType.SNAPSHOT_PLUS_UPDATES}.
	 * Stops a running simulation (if any) if it the subscription request
	 * is of type
	 * {@link quickfix.field.SubscriptionRequestType.DISABLE_PREVIOUS_SNAPSHOT_PLUS_UPDATE_REQUEST}.
	 * Requests are only accepted if the symbol corresponds to the one configured.
	 * Other request are ignored.
	 */
	public void onMessage(quickfix.fix44.MarketDataRequest request,
			SessionID sessionID)
	throws FieldNotFound, UnsupportedMessageType, IncorrectTagValue
	{
		SubscriptionRequestType subscriptionRequestType = request.getSubscriptionRequestType();
		
		List<Group> groups = request.getGroups(NoRelatedSym.FIELD);

		Symbol symbol = null;
		if(groups.size() > 0)
			symbol = new Symbol(groups.get(0).getString(Symbol.FIELD));

		if(symbol != null &&
				!symbol.getValue().toUpperCase().equals(Configuration.getInstance().getSymbol())) {
			LOG.info("Ignored market data request due to illegal symbol.");
			return;
		}
		
		//Start simulation when the subscription is of type Snapshot-Plus-Updates
		if(subscriptionRequestType.getValue() ==
			SubscriptionRequestType.SNAPSHOT_PLUS_UPDATES) {

			if(!isSimulationRunning) {
				isSimulationRunning = true;
				
				LOG.info("Starting Simulation");
				
				MDReqID mdReqID = request.getMDReqID();
				
				marketDataReporter = new MarketDataReporter(sessionID, symbol, mdReqID);
				OrderBook.getInstance().addObserver(marketDataReporter);
				
				orderBookPlaybackThread = new Thread(orderBookSimulator);
				orderBookPlaybackThread.start();
			}
		//Stop the simulation if the request is to disable the market data. 
		} else if(subscriptionRequestType.getValue() ==
			SubscriptionRequestType.DISABLE_PREVIOUS_SNAPSHOT_PLUS_UPDATE_REQUEST) {
			//Stop the running simulation, if any, otherwise return
			if(orderBookPlaybackThread != null) {
				orderBookSimulator.stopSimulation();
				isSimulationRunning = false;
				
				LOG.info("Stopping Simulation");
			}
			return;
		} else
		{
			LOG.info("Ignored market data request due to illegal subscription type.");
		}
	}

	/**
	 * Default message handler
	 */
	public void onMessage(Message message, SessionID sessionID)
	throws FieldNotFound, UnsupportedMessageType, IncorrectTagValue
	{
	}

	/**
	 * Notifies when a session is created.
	 */
	public void onCreate(SessionID sessionID)
	{
	}

	/**
	 * Notifies on valid logon
	 */
	public void onLogon(SessionID sessionID)
	{
	}

	/**
	 * Notifies when a connections is closed
	 */
	public void onLogout(SessionID sessionID)
	{
	}

	/**
	 * Notifies on admin messages to the connected client
	 */
	public void toAdmin(Message msg, SessionID sessionID)
	{
	}

	/**
	 * Notifies on admin messages from the connected client
	 */
	public void fromAdmin(Message msg, SessionID sessionID)
	throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue,
	RejectLogon
	{
	}

	/**
	 * Messages to client goes through here.
	 */
	public void toApp(Message msg, SessionID sessionID)
	throws DoNotSend
	{

	}

	/**
	 * Messages from client goes through here.
	 */
	public void fromApp(Message msg, SessionID sessionID)
	throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue,
	UnsupportedMessageType
	{	//Parse the message (base class method)
		crack(msg, sessionID);
	}
}