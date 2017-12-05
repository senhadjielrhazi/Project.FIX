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
package org.marketcetera.exchange.databaseAdapter;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Database adapter interface for the market data loader.
 * 
 * @author <a href="mailto:catsbf@dasch.dk">Daniel Schiermer</a>
 * @version 0.1
 *
 */
public interface IDatabaseAdapter {
	/**
	 * Initializes the market data database
	 */
	void initializeMarketDataDatabase();
	
	/**
	 * Inserts a market event into the market event database with the given values.
	 * @param orderID Id of the order
	 * @param orderActionType Action type of the order
	 * @param orderQty Quantity of the order
	 * @param price Price of the order
	 * @param buySellInd Buy sell indicator
	 * @param messageSequenceNumber Message sequence number
	 * @param dateTime Date time (transaction time) of the event
	 * @throws SQLException SQLException if anything goes wrong with the insertion
	 */
	void insertMarketEvent(String orderID, char orderActionType,
			int orderQty, double price, char buySellInd, int messageSequenceNumber,
			Date dateTime) throws SQLException;
	
	/**
	 * Commits the inserted market events as a batch
	 * @throws SQLException SQLException if anything goes wrong with the commit
	 */
	void commitMarketEventInserts() throws SQLException;
	
	/**
	 * Gets the market events in the given date time interval
	 * @param startDateTime Start date time
	 * @param endDateTime End date time
	 * @return Resultset with all the market events from the given interval.
	 * @throws SQLException If anything goes wrong with the fetch.
	 */
	ResultSet getMarketEvents(Date startDateTime, Date endDateTime) throws SQLException;
}