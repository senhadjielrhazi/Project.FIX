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
package org.marketcetera.exchange.backtesting;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import quickfix.field.OrdType;
import quickfix.field.Side;
import org.marketcetera.exchange.common.Configuration;
import org.marketcetera.exchange.databaseAdapter.IDatabaseAdapter;
import org.marketcetera.exchange.events.SimulationEvent;
import org.marketcetera.exchange.events.SimulationStartedEvent;
import org.marketcetera.exchange.events.SimulationStoppedEvent;
import org.marketcetera.exchange.marketEvents.Bid;
import org.marketcetera.exchange.marketEvents.Fill;
import org.marketcetera.exchange.marketEvents.MarketEvent;
import org.marketcetera.exchange.marketEvents.Offer;
import org.marketcetera.exchange.marketEvents.PartialFill;

/**
 * Order book simulator loads the market events in the given time interval from the
 * database and replays them in order. All events are interpreted and changes the
 * cached order book accordingly.
 * 
 * Notifies observers when the simulation starts and stops.
 * It runs as a separate thread in order not to block all other application actions.
 * 
 * @author <a href="mailto:catsbf@dasch.dk">Daniel Schiermer</a>
 * @version 0.1
 *
 */
public class CcfeaOrderBookSimulator extends OrderBookSimulator {
	private OrderBook orderBook;
	private IDatabaseAdapter dbAdapter;
	private ResultSet marketEventResults;
	private Date startDateTime;
	private Date endDateTime;
	private Calendar simulationDelayTime;
	private boolean continueReplaying;
	private String serverID;
	private String symbol;
	private String account;
	private int simulationDelay;
	private SimulationTime simulationTime;
	private static final Logger LOG = LoggerFactory.getLogger(CcfeaOrderBookSimulator.class);

	/**
	 * Creates a simulation object with the given start and end dates. The market events
	 * are fetched from the database using the passed database adapter. The delay indicates
	 * how long the thread should sleep after each simulated second. Should be at least a
	 * few hundred milliseconds, so the simulation does not keep up all invocations with the
	 * order book.
	 * @param simulationDates Start and end date times for the simulation.
	 * @param simulationDelay Amount of time the thread is to sleep for each simulated second.
	 * Should be at least a few hundred milliseconds, so the simulation does not keep up
	 * all invocations with the order book.
	 * @param databaseAdapter Database adapter to use to fetch market events.
	 */
	public CcfeaOrderBookSimulator(SimulationDateTimeInterval simulationDates, int simulationDelay,
			IDatabaseAdapter databaseAdapter) {
		this.startDateTime = simulationDates.getStartDateTime();
		this.endDateTime = simulationDates.getEndDateTime();
		this.simulationDelay = simulationDelay;

		simulationDelayTime = Calendar.getInstance();
		simulationDelayTime.setTime(startDateTime);
		simulationDelayTime.add(Calendar.SECOND, 1);

		simulationTime = SimulationTime.getInstance();
		setSimulationTime(this.startDateTime);

		dbAdapter = databaseAdapter;
		orderBook = OrderBook.getInstance();
		serverID = Configuration.getInstance().getServerID();
		symbol = Configuration.getInstance().getSymbol();
		account = serverID;
	}

	/**
	 * Starts the simulation by replaying the market events given in the result set.
	 * Notifies observers when the simulation starts and stops.
	 * @param results Market events as fetched from the database
	 * @throws SQLException
	 * @throws InterruptedException
	 */
	@Override
	public void runSimulation() {
		try {
			notifyChange(new SimulationStartedEvent());

			continueReplaying = true;
			Calendar tempCal = Calendar.getInstance();

			LOG.info("Running simulation...");

			while (marketEventResults.next() && continueReplaying) {
				MarketEvent marketEvent = null;

				String clientOrderID = marketEventResults.getString("orderID");
				String orderActionTypeString = marketEventResults.getString("orderActionType");
				int orderQty = marketEventResults.getInt("orderQty");
				double price = marketEventResults.getDouble("price");
				String buySellInd = marketEventResults.getString("buySellInd");
				char side = buySellInd.equals("B") ? Side.BUY : Side.SELL;
				Date dateTime = marketEventResults.getDate("dateTime");
				char orderType = OrdType.LIMIT; 
				tempCal.setTime(dateTime);

				setSimulationTime(dateTime);

				if(tempCal.get(Calendar.SECOND) == simulationDelayTime.get(Calendar.SECOND)) {
					Thread.sleep(simulationDelay);
					simulationDelayTime.add(Calendar.SECOND, 1);
				}

				char orderActionType = 0;
				if(orderActionTypeString != null) {
					orderActionType = orderActionTypeString.charAt(0);
				}

				switch (orderActionType)
				{
				case 'D':   //Delete
					marketEvent = getMarketEventFromActionTypeAndBuySellInd(
							buySellInd, orderActionType, price,
							orderQty, clientOrderID, dateTime, orderType);

					orderBook.deleteMarketEvent(marketEvent);
					break;
				case 'E':   //Expired
					marketEvent = getMarketEventFromActionTypeAndBuySellInd(
							buySellInd, orderActionType, price,
							orderQty, clientOrderID, dateTime, orderType);

					orderBook.deleteMarketEvent(marketEvent);
					break;
				case 'P':   //Partial match
					marketEvent =
						new PartialFill(price, orderQty, clientOrderID, dateTime, serverID, orderType,
								symbol, account, orderQty, orderQty, price, 0, side);

					orderBook.addMarketEvent(marketEvent);
					break;
				case 'M':   //Full Match
					marketEvent =
						new Fill(price, orderQty, clientOrderID, dateTime, serverID, orderType,
								symbol, account, orderQty, orderQty, price, side);

					orderBook.addMarketEvent(marketEvent);
					break;
				case 'T':   //Transaction Limit
					//Do nothing, ignore.
					break;
				default:   //Order (bid or offer)
					marketEvent = getMarketEventFromActionTypeAndBuySellInd(
							buySellInd, orderActionType, price,
							orderQty, clientOrderID, dateTime, orderType);

					orderBook.addMarketEvent(marketEvent);
					break;
				}
			}

			marketEventResults.close();

			LOG.info("Stopped running simulation...");

			notifyChange(new SimulationStoppedEvent());

		} catch (SQLException e) {
			LOG.error("A SQL Exception occured during order book simulation: " + e.toString());
		} catch (InterruptedException e) {
			LOG.error("The order book simulation thread was wrongly interrupted: " + e.toString());
		}
	}

	/**
	 * Sets the simulation time. Should be kept up to date during
	 * simulation.
	 * @param time The current time of the simulation (last replayed
	 * market event)
	 */
	private void setSimulationTime(Date time) {
		simulationTime.setTime(time);
	}

	/**
	 * Gets a market event based on the action type and buy-sell indicator.
	 * @param buySellInd Buy sell indicator, 'B' for buy, 'S' for sell
	 * @param actionType Action type indication whether it is supposed to be a delete event
	 * @param price Price of the event
	 * @param orderQty Order quantity of the event
	 * @param clientOrderID Unique orer id
	 * @param transactTime Transaction time of the order
	 * @param orderType Type of the order
	 * @return A matching market event.
	 */
	private MarketEvent getMarketEventFromActionTypeAndBuySellInd(
			String buySellInd, char actionType, double price, int orderQty,
			String clientOrderID, Date transactTime, char orderType) {

		MarketEvent marketEvent = null;

		if(buySellInd.equals("B")) {
			marketEvent = new Bid(price, orderQty, orderQty,
					clientOrderID, transactTime, serverID,
					orderType, symbol, account);
		}
		else if(buySellInd.equals("S")) {
			marketEvent = new Offer(price, orderQty, orderQty,
					clientOrderID, transactTime, serverID, 
					orderType, symbol, account);
		}

		//Set the action of the order
		marketEvent.setAction((actionType == 'D' || actionType == 'E') ?
				MarketEvent.Action.DELETE : MarketEvent.Action.ADD);

		return marketEvent;
	}

	/**
	 * Sets the indicator that specifies whether or not the simulation should continue to,
	 * and thereby stops the simulation at the next fitting point. 
	 */
	public void stopSimulation() {
		this.continueReplaying = false;
	}

	/**
	 * Start the thread. Fetches the market events for the simulation interval
	 * and starts the simulation.
	 */
	@Override
	public void run() {
		try {
			this.marketEventResults = dbAdapter.getMarketEvents(startDateTime , endDateTime);
			runSimulation();

		} catch (SQLException e) {
			LOG.error("SQL error while fetching market events for simulation: " + e.toString());
		}
	}

	/**
	 * Notify servers about updates
	 * @param simulationEvent Event to fire.
	 */
	private void notifyChange(SimulationEvent simulationEvent) {
		setChanged();
		notifyObservers(simulationEvent);
	}
}