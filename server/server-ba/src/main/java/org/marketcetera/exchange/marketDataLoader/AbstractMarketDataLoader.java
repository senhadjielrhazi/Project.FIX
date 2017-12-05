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

import org.marketcetera.exchange.databaseAdapter.IDatabaseAdapter;

/**
 * This class should be extended when implementing Tick Data Loaders. 
 * 
 * @author <a href="mailto:catsbf@dasch.dk">Daniel Schiermer</a>
 * @version 0.1
 *
 */
public abstract class AbstractMarketDataLoader implements IMarketDataLoader{
	protected IDatabaseAdapter dbAdapter;
	
	/**
	 * Creates the instance with the given {@link IDatabaseAdapter}
	 * @param databaseAdapter {@link IDatabaseAdapter} to use in the market data loader
	 */
	public AbstractMarketDataLoader(IDatabaseAdapter databaseAdapter) {
		this.dbAdapter = databaseAdapter;
	}
	
	/**
	 * Gets the SQLite connection and initializes the database
	 */
	@Override
	public void beforeDataLoad() {
		dbAdapter.initializeMarketDataDatabase();
	}
	
	/**
	 * Adds a market event to the market event database
	 */
	@Override
	public void addMarketEvent(String orderID, char orderActionType,
			int orderQty, double price, char buySellInd,
			int messageSequenceNumber, Date dateTime) throws SQLException {
		dbAdapter.insertMarketEvent(orderID, orderActionType, orderQty,
				price, buySellInd, messageSequenceNumber, dateTime);
	}

	/**
	 * Commits the batch of added market events.
	 */
	@Override
	public void commitMarketEventBatch() throws SQLException {
		dbAdapter.commitMarketEventInserts();
	}
}