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
package org.marketcetera.exchange.marketDataLoader;

import java.sql.Date;
import java.sql.SQLException;
import java.sql.Time;

/**
 * Market data loader interface defining the methods to use in any data loader.
 * 
 * @author <a href="mailto:catsbf@dasch.dk">Daniel Schiermer</a>
 * @version 0.1
 *
 */
public interface IMarketDataLoader {
	/**
	 * Method to be called before starting the data load. Sets up the
	 * data base and prepared statements.
	 */
	void beforeDataLoad();
	
	/**
	 * Adds a market event to the Market Event database. Note that the event is not committed
	 * to the database. The {@link #commitMarketEventBatch()} has to be called to commit the added
	 * Market Events.
	 * @param orderID Unique id reference for the order (multiple events can exist for the same order)
	 * @param orderActionType Type describing the action to take.
	 * Supported actions: D = Delete, E = Expired, P = Partial match, M = Full match,
	 * T = Transaction limit, Null = either buy or sell limit order
	 * @param orderQty Quantity of the order, in case of a Partial match this is the matched quantity.
	 * @param price Price of the order, in case of a Partial match this is the executed price.
	 * @param buySellInd Indicates whether it is a B = buy or S = sell order.
	 * @param messageSequenceNumber MessageSequenceNumber of the order.
	 * @param date Date of the market event.
	 * @param time Time of the market event.
	 * @throws SQLException 
	 */
	void addMarketEvent(String orderID, char orderActionType, int orderQty,
			double price, char buySellInd, int messageSequenceNumber,
			Date dateTime) throws SQLException;
	
	/**
	 * Executes the batch of added market events. All market events added so far using
	 * {@link #addMarketEvent(String, char, int, double, char, int, Date, Time)}, will
	 * be committed to the database. This method should be called at least one time
	 * (when all market events are added).
	 * @throws SQLException 
	 */
	void commitMarketEventBatch() throws SQLException;
	
	/**
	 * Method implements functionality to populate the Market Events database, with all
	 * entries and all relevant attributes.
	 */
	void loadData();
}