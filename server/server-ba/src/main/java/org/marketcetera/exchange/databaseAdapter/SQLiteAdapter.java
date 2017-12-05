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

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.marketcetera.exchange.common.Configuration;

/**
 * SQLite database adapter implementation.
 * 
 * @author <a href="mailto:catsbf@dasch.dk">Daniel Schiermer</a>
 * @version 0.1
 *
 */
public class SQLiteAdapter implements IDatabaseAdapter {
	private PreparedStatement insertMarketEvent;
	private Connection connection;
	private String dbNameMarketEvents;
	private static final Logger LOG = LoggerFactory.getLogger(SQLiteAdapter.class);

	/**
	 * Creates an instance and gets the connection and database name
	 */
	public SQLiteAdapter() {
		this.connection = SQLiteConnection.getInstance().getConnection();
		dbNameMarketEvents = Configuration.getInstance().getMarketDataTableName();
	}

	/**
	 * @see {@link IDatabaseAdapter#initializeMarketDataDatabase()}
	 */
	public void initializeMarketDataDatabase() {
		try {
			Class.forName("org.sqlite.JDBC");			

			Statement statement = connection.createStatement();

			//Delete tables if they already exist
			statement.execute("DROP TABLE IF EXISTS " + dbNameMarketEvents + ";");
			statement.execute("CREATE TABLE " + dbNameMarketEvents
					+ " (orderID TEXT NOT NULL, orderActionType CHAR(1), orderQty INTEGER, "
					+ "price DOUBLE, buySellInd CHAR(1), messageSequenceNumber INTEGER, "
					+ "dateTime DATE);");

			statement.execute("CREATE INDEX orderDateTimeIndex ON " + dbNameMarketEvents + "(dateTime);");
			statement.execute("CREATE INDEX orderMessageSeqIndex ON " + dbNameMarketEvents + "(messageSequenceNumber);");

			insertMarketEvent = connection.prepareStatement(
					"insert into " + dbNameMarketEvents + " values (?, ?, ?, ?, ?, ?, ?);");
		} catch (SQLException e) {
			LOG.error("Error resetting initializing the market data DB: " + e.toString());
		} catch (ClassNotFoundException e) {
			LOG.error("Error loading SQLite JDBC drivers: " + e.toString());
		}
	}

	/**
	 * @see {@link IDatabaseAdapter#insertMarketEvent(String, char, int, double, char, int, Date)}
	 */
	public void insertMarketEvent(String orderID, char orderActionType,
			int orderQty, double price, char buySellInd, int messageSequenceNumber,
			Date dateTime)
	throws SQLException {
			insertMarketEvent.setString(1, orderID);
			insertMarketEvent.setString(2, orderActionType + ""); //cast as string
			insertMarketEvent.setInt(3, orderQty);
			insertMarketEvent.setDouble(4, price);
			insertMarketEvent.setString(5, buySellInd + "");
			insertMarketEvent.setInt(6, messageSequenceNumber);
			insertMarketEvent.setDate(7, dateTime);

			insertMarketEvent.addBatch();
	}
	
	/**
	 * @see {@link IDatabaseAdapter#commitMarketEventInserts()}
	 */
	public void commitMarketEventInserts() throws SQLException {
		connection.setAutoCommit(false);
		insertMarketEvent.executeBatch();
		connection.setAutoCommit(true);
	}
	
	/**
	 * @see {@link IDatabaseAdapter#getMarketEvents(Date, Date)}
	 */
	public ResultSet getMarketEvents(Date startDateTime, Date endDateTime) throws SQLException {

		Statement selectStatement = connection.createStatement();
		String query = "SELECT * FROM " + dbNameMarketEvents + " me WHERE me.dateTime >= "
			+ startDateTime.getTime() + " AND me.dateTime <= " + endDateTime.getTime()
			+ " ORDER BY me.dateTime, me.messageSequenceNumber;";

		return selectStatement.executeQuery(query);
	}
}