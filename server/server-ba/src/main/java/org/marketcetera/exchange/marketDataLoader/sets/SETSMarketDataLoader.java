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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.marketcetera.exchange.common.Configuration;
import org.marketcetera.exchange.databaseAdapter.IDatabaseAdapter;
import org.marketcetera.exchange.marketDataLoader.AbstractMarketDataLoader;
import org.marketcetera.exchange.marketDataLoader.IMarketDataLoader;
import org.marketcetera.util.common.csv.CSVParser;

/**
 * SETS market data loader implementation of the {@link IMarketDataLoader}.
 * @see {@link IMarketDataLoader}.
 * 
 * @author <a href="mailto:catsbf@dasch.dk">Daniel Schiermer</a>
 * @version 0.1
 *
 */
public class SETSMarketDataLoader extends AbstractMarketDataLoader {

	private DateFormat dateformat;
	private String detailsFilePath;
	private String historyFilePath;
	private final String tradeReportsFilePath;
	private SETSSQLiteAdapter setsSqliteAdapter;
	private static final Logger LOG = LoggerFactory.getLogger(SETSMarketDataLoader.class);

	/**
	 * Creates an instance of the SETS market data loader
	 * @param dateformat Used to parse the csv date times
	 * @param detailsFilePath File path to the SETS order details CSV file 
	 * @param historyFilePath File path to the SETS order history CSV file
	 * @param tradeReportsFilePath File path to the SETS trade reports CSV file
	 * @param databaseAdapter The database adapter to use in the data loader
	 * @throws ClassNotFoundException If the SQLite JDBC driver cannot be loaded
	 */
	public SETSMarketDataLoader(DateFormat dateformat, String detailsFilePath,
			String historyFilePath, String tradeReportsFilePath, IDatabaseAdapter databaseAdapter)
	throws ClassNotFoundException {
		super(databaseAdapter);
		Class.forName(Configuration.getInstance().getJdbcDriver());
		this.dateformat = dateformat;
		this.detailsFilePath = detailsFilePath;
		this.historyFilePath = historyFilePath;
		this.tradeReportsFilePath = tradeReportsFilePath;
	}

	/**
	 * Loads the market events into the database
	 */
	public void loadData() {
		try {
			beforeDataLoad(); //Initialization

			setsSqliteAdapter = new SETSSQLiteAdapter();
			
			LOG.info("Starting data load...");
			populateTempTables();

			populateMarketEventsTable();

			LOG.info("Data load is done!");
		} catch (SQLException e) {
			LOG.error("SQL exception occured while loading data: " + e.toString());
		} catch (ClassNotFoundException e) {
			LOG.error("Problem loading SQLite JDBC drivers: " + e.toString());
		} catch (NumberFormatException e) {
			LOG.error(e.toString());
		} catch (IOException e) {
			LOG.error(e.toString());
		} catch (ParseException e) {
			LOG.error(e.toString());
		}
	}

	/**
	 * Populate the temporary tables
	 * @throws NumberFormatException
	 * @throws SQLException
	 * @throws IOException
	 * @throws ParseException
	 * @throws ClassNotFoundException
	 */
	private void populateTempTables()
	throws NumberFormatException, SQLException, IOException, ParseException, ClassNotFoundException {
		LOG.info("Populating temp tables...");

		loadTradeReports();
		loadOrderDetails();
		loadOrderHistory();
		setsSqliteAdapter.updatePrices();

		LOG.info("Populated temp tables!");
	}

	/**
	 * Populates the Market Events table using the interface method
	 * {@link #addMarketEvent(String, char, int, double, char, int, Date)}.
	 * with the values from the OrderDetails and OrderHistory temp tables.
	 * NOTE: This is definitely not the most efficient way of doing this,
	 * it would be more efficient to write queries that inserts the entries
	 * directly into the table, but this way it conforms to the interface.
	 * @throws SQLException If anything goes wrong with the SQLite integration.
	 */
	private void populateMarketEventsTable() throws SQLException {
		LOG.info("Populating Market Events table...");

		ResultSet resultSet = setsSqliteAdapter.getOrderDetails();
		insertMarketEvents(resultSet, false);

		resultSet = setsSqliteAdapter.getOrderHistory();
		insertMarketEvents(resultSet, true);

		LOG.info("Populated Market Events table!");
	}

	/**
	 * Inserts all the market events in the results passed
	 * @param results Market events from the temp tables
	 * @param hasActionType Indicator whether or not market events has an action type
	 * @throws SQLException
	 */
	private void insertMarketEvents(ResultSet results, boolean hasActionType) throws SQLException {
		while(results.next()) {
			String orderID = results.getString("orderID");
			char buySellInd = results.getString("buySellInd").charAt(0);
			double price = results.getDouble("price");
			Date dateTime = results.getDate("dateTime");
			int messageSequenceNumber = results.getInt("messageSequenceNumber");
			int orderQty = results.getInt("orderQty");

			char orderActionType = '0';

			if(hasActionType) {
				orderActionType = results.getString("orderActionType").charAt(0);
			}

			addMarketEvent(orderID, orderActionType, orderQty, price,
					buySellInd, messageSequenceNumber, dateTime);
		}
		results.close();
		commitMarketEventBatch();
	}

	/**
	 * Load the order history csv file market events
	 * @throws NumberFormatException
	 * @throws IOException
	 * @throws ClassNotFoundException
	 * @throws SQLException
	 * @throws ParseException
	 */
	private void loadOrderHistory()
	throws NumberFormatException, IOException, ClassNotFoundException, SQLException, ParseException {
		if(checkFileExists(historyFilePath) == false)
			return;

		CSVParser reader = loadFile(historyFilePath);

		String [] line;
		while ((line = reader.getLine()) != null) {
			String dateTimeString = line[13] + " " + line[14];
			Date dateTime = new Date(dateformat.parse(dateTimeString).getTime());

			setsSqliteAdapter.insertOrderHistory(line[0], line[4], line[1].charAt(0),
					Integer.parseInt(line[3]), line[10].charAt(0),
					Integer.parseInt(line[12]), dateTime, line[11]);
		}

		setsSqliteAdapter.commitOrderHistory();
		reader.close();

		LOG.info("Inserted all OrderHistory entries!");
	}

	/**
	 * Load the order details csv file market events
	 * @throws SQLException
	 * @throws NumberFormatException
	 * @throws IOException
	 * @throws ParseException
	 */
	private void loadOrderDetails() throws SQLException, NumberFormatException, IOException, ParseException {
		if(checkFileExists(detailsFilePath) == false)
			return;

		CSVParser reader = loadFile(detailsFilePath);

		String [] line;
		while ((line = reader.getLine()) != null) {
			String dateTimeString = line[14] + " " + line[15];
			Date dateTime = new Date(dateformat.parse(dateTimeString).getTime());

			setsSqliteAdapter.insertOrderDetail(line[0], line[7].charAt(0),
					Double.parseDouble(line[10]), Integer.parseInt(line[11]),
					dateTime, Integer.parseInt(line[16]));
		}

		setsSqliteAdapter.commitOrderDetails();
		reader.close();

		LOG.info("Inserted all OrderDetail entries!");
	}

	/**
	 * Load the trade reports csv file market events
	 * @throws SQLException
	 * @throws NumberFormatException
	 * @throws IOException
	 * @throws ParseException
	 */
	private void loadTradeReports() throws SQLException, NumberFormatException, IOException, ParseException {
		if(checkFileExists(tradeReportsFilePath) == false)
			return;

		CSVParser reader = loadFile(tradeReportsFilePath);

		String [] line;
		while ((line = reader.getLine()) != null) {
			String tradeCode = line[5];
			double price = Double.parseDouble(line[6]);

			setsSqliteAdapter.insertTradeReport(tradeCode, price);
		}

		setsSqliteAdapter.commitTradeReports();

		reader.close();

		LOG.info("Inserted all TradeReports entries!");
	}

	/**
	 * Check if the file with the given file path exists.
	 * @param filepath Filepath of the file to check for existence.
	 * @return boolean indicating whether or not the file exists.
	 */
	private boolean checkFileExists(String filepath) {
		if(filepath != null) {
			File file = new File(filepath);
			if (file.exists())
				return true;
		}
		return false;
	}

	/**
	 * Loads the csv file with the given file path
	 * @param filepath File path of the file to load
	 * @return CSVReader for the file path.
	 */
	private CSVParser loadFile(String filepath) {
		CSVParser reader = null;

		try {
			reader = new CSVParser(new FileReader(filepath));
		} catch (FileNotFoundException e) {
			LOG.error("Could not find the CSV file to load with file path: " + filepath);
		}

		return reader;
	}
}