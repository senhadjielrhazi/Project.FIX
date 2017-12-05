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
package org.marketcetera.exchange.marketDataLoader.sets;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.marketcetera.exchange.common.Configuration;
import org.marketcetera.exchange.databaseAdapter.SQLiteConnection;

/**
 * SETS SQLite database adapter.
 * 
 * @author <a href="mailto:catsbf@dasch.dk">Daniel Schiermer</a>
 * @version 0.1
 *
 */
public class SETSSQLiteAdapter {
	Connection connection;
	private final String TMP_TRADE_TABLE = "TEMP_TRADE_REPORTS";
	private final String TMP_ORDER_TABLE = "TEMP_ORDER_DETAILS";
	private final String TMP_HISTORY_TABLE = "TEMP_ORDER_HISTORY";
	private PreparedStatement insertTradeReport;
	private PreparedStatement insertDetails;
	private PreparedStatement insertHistory;
	private static final Logger LOG = LoggerFactory.getLogger(SETSSQLiteAdapter.class);

	/**
	 * Create the instance
	 * @throws ClassNotFoundException
	 * @throws SQLException
	 */
	public SETSSQLiteAdapter() throws ClassNotFoundException, SQLException {
		Class.forName(Configuration.getInstance().getJdbcDriver());
		this.connection = SQLiteConnection.getInstance().getConnection();
		initialize();
	}
	
	/**
	 * Initializes the SETS temporary tables and prepare the insert statements.
	 * @throws SQLException
	 */
	protected void initialize() throws SQLException {
		setUpTempTables();
		prepareStatements();
	}

	/**
	 * Set up the temporary tables
	 * @throws SQLException
	 */
	protected void setUpTempTables() throws SQLException {
		Statement statement = connection.createStatement();
		
		//Trade reports
		statement.execute("CREATE TEMP TABLE " + TMP_TRADE_TABLE + " (tradeCode TEXT NOT NULL, price DOUBLE);");
		statement.execute("CREATE INDEX tradeCodeIndex ON " + TMP_TRADE_TABLE + "(tradeCode);");

		//Order details
		statement.execute("CREATE TEMP TABLE " + TMP_ORDER_TABLE
				+ " (orderID TEXT NOT NULL, buySellInd CHAR(1), price DOUBLE, "
				+ "orderQty INTEGER, dateTime DATE, messageSequenceNumber INTEGER, "
				+ "PRIMARY KEY (orderID));");
		statement.execute("CREATE INDEX detalisCodeIndex ON " + TMP_ORDER_TABLE + "(orderID);");

		//Order history
		statement.executeUpdate("CREATE TEMP TABLE " + TMP_HISTORY_TABLE
				+ " (orderID TEXT NOT NULL, tradeCode TEXT, orderActionType CHAR(1), orderQty INTEGER, "
				+ "buySellInd CHAR(1), messageSequenceNumber INTEGER, dateTime DATE, price DOUBLE, "
				+ "marketMechanismType TEXT, "
				+ "FOREIGN KEY(orderID) REFERENCES "
				+ TMP_ORDER_TABLE + "(orderID));");
		statement.execute("CREATE INDEX historyCodeIndex ON " + TMP_HISTORY_TABLE + "(orderID);");
		
		LOG.info("SETS temp tables created!");
	}

	/**
	 * Prepare the insert statements to temp tables.
	 * @throws SQLException
	 */
	protected void prepareStatements() throws SQLException {
		insertTradeReport = connection.prepareStatement(
				"insert into " + TMP_TRADE_TABLE + " values (?, ?);");

		insertDetails = connection.prepareStatement(
				"insert into " + TMP_ORDER_TABLE + " values (?, ?, ?, ?, ?, ?);");

		insertHistory = connection.prepareStatement(
				"insert into " + TMP_HISTORY_TABLE + " values (?, ?, ?, ?, ?, ?, ?, ?, ?);");
	}
	
	/**
	 * Insert trade report entry
	 * @param tradeCode Trade code of the trade report
	 * @param price Price of the trade
	 * @throws SQLException
	 */
	protected void insertTradeReport(String tradeCode, double price) throws SQLException {
		insertTradeReport.setString(1, tradeCode);
		insertTradeReport.setDouble(2, price);

		insertTradeReport.addBatch();
	}
	
	/**
	 * Commit batch of trade reports.
	 * @throws SQLException
	 */
	protected void commitTradeReports() throws SQLException {
		connection.setAutoCommit(false);
		insertTradeReport.executeBatch();
		connection.setAutoCommit(true);
	}
	
	/**
	 * Insert an order detail entry to temp table
	 * @param orderID ID of the order
	 * @param buySellInd Buy sell indicator
	 * @param price Price of the order
	 * @param aggregateSize Aggregate size of the order
	 * @param dateTime Transaction time
	 * @param messageSequenceNumber Message sequence number
	 * @throws SQLException
	 */
	protected void insertOrderDetail(String orderID, char buySellInd, double price,
                int aggregateSize, Date dateTime, int messageSequenceNumber) throws SQLException {
		insertDetails.setString(1, orderID);
		insertDetails.setString(2, buySellInd + ""); //cast as string
		insertDetails.setDouble(3, price);
		insertDetails.setInt(4, aggregateSize);
		insertDetails.setDate(5, dateTime);
		insertDetails.setInt(6, messageSequenceNumber);

		insertDetails.addBatch();
	}
	
	/**
	 * Commit batch of order details
	 * @throws SQLException
	 */
	protected void commitOrderDetails() throws SQLException {
		connection.setAutoCommit(false);
		insertDetails.executeBatch();
		connection.commit();
		connection.setAutoCommit(true);
	}
	
	/**
	 * Insert an order history entry to temp table
	 * @param orderID ID of the order
	 * @param tradeCode trade code for the order history
	 * @param orderActionType Action type
	 * @param tradeSize size of the order
	 * @param buySellInd Buy sell indicator
	 * @param messageSequenceNumber Message sequence number
	 * @param dateTime Transaction time
	 * @param marketMechanismType Market mechanism type
	 * @throws SQLException
	 */
	protected void insertOrderHistory(String orderID, String tradeCode, char orderActionType,
            int tradeSize, char buySellInd, int messageSequenceNumber,
            Date dateTime, String marketMechanismType) throws SQLException {
        	insertHistory.setString(1, orderID);
        	insertHistory.setString(2, tradeCode);
        	insertHistory.setString(3, orderActionType + ""); //cast as string
        	insertHistory.setInt(4, tradeSize);
        	insertHistory.setString(5, buySellInd + "");
        	insertHistory.setInt(6, messageSequenceNumber);
        	insertHistory.setDate(7, dateTime);
        	insertHistory.setDouble(8, 0.0);
        	insertHistory.setString(9, marketMechanismType);

        	insertHistory.addBatch();
	}
	
	/**
	 * Commit batch of inserted order history entries
	 * @throws SQLException
	 */
	protected void commitOrderHistory() throws SQLException {
		connection.setAutoCommit(false);
		insertHistory.executeBatch();
		connection.commit();
		connection.setAutoCommit(true);
	}
	
	/**
	 * Update the prices on the order history entries and the order detail entries
	 * @throws SQLException
	 */
	protected void updatePrices() throws SQLException {
		connection.setAutoCommit(false);
		
		updateHistoryPrices();
		updateDetailPrices();
		
		connection.commit();
		connection.setAutoCommit(false);
		
		LOG.info("Prices in 'OrderDetails' and 'OrderHistory' are updated!");
	}
	
	/**
	 * Update prices of order history elements from order details
	 * @throws SQLException
	 */
	private void updateHistoryPrices() throws SQLException {
		Statement statement = connection.createStatement();

		//Update all Limit orders
		statement.execute("UPDATE " + TMP_HISTORY_TABLE
				+ " SET price = (SELECT " + TMP_ORDER_TABLE + ".price FROM "
				+ TMP_ORDER_TABLE
				+ " WHERE " + TMP_ORDER_TABLE + ".orderID = "
				+ TMP_HISTORY_TABLE + ".orderID) WHERE "
				+ TMP_HISTORY_TABLE + ".marketMechanismType = 'LO';");
		
		//Update all market orders
		statement.execute("UPDATE " + TMP_HISTORY_TABLE
				+ " SET price = (SELECT " + TMP_TRADE_TABLE + ".price FROM "
				+ TMP_TRADE_TABLE + " WHERE "
				+ TMP_TRADE_TABLE + ".tradeCode = "
				+ TMP_HISTORY_TABLE + ".tradeCode) WHERE "
				+ TMP_HISTORY_TABLE + ".marketMechanismType = 'MO' AND "
				+ TMP_HISTORY_TABLE + ".orderActionType != 'D';");
	}
	
	/**
	 * Update prices of order detail elements from trade orders (market events need
	 * to get their price updated)
	 * @throws SQLException
	 */
	private void updateDetailPrices() throws SQLException {
		Statement statement = connection.createStatement();
		
		statement.execute("UPDATE "
				+ TMP_ORDER_TABLE + " SET price = (SELECT "
				+ TMP_TRADE_TABLE + ".price FROM "
				+ TMP_TRADE_TABLE + ", " + TMP_HISTORY_TABLE + " WHERE "
				+ TMP_HISTORY_TABLE + ".orderID = "
				+ TMP_ORDER_TABLE + ".orderID AND "
				+ TMP_HISTORY_TABLE + ".tradeCode = "
				+ TMP_TRADE_TABLE + ".tradeCode) WHERE "
				+ TMP_ORDER_TABLE + ".price = 0;");
	}
	
	/**
	 * Get all order history entries
	 * @return All order history entries
	 * @throws SQLException
	 */
	protected ResultSet getOrderHistory() throws SQLException {
		Statement statement = connection.createStatement();
		
		String query = "SELECT * FROM " + TMP_HISTORY_TABLE + ";";

		return statement.executeQuery(query);
	}
	
	/**
	 * Get all order detail entries
	 * @return All order detail entries
	 * @throws SQLException
	 */
	protected ResultSet getOrderDetails() throws SQLException {
		Statement statement = connection.createStatement();
		
		String query = "SELECT * FROM " + TMP_ORDER_TABLE + ";";

		return statement.executeQuery(query);
	}
}