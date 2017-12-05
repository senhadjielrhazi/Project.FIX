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
import java.sql.DriverManager;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.marketcetera.exchange.common.Configuration;

/**
 * Singleton holding the database connection.
 * 
 * @author <a href="mailto:catsbf@dasch.dk">Daniel Schiermer</a>
 * @version 0.1
 *
 */
public class SQLiteConnection {
	private static Connection connection = null;
	private static SQLiteConnection sInstance = new SQLiteConnection();
	private static String databaseFullName;
	private static final Logger LOG = LoggerFactory.getLogger(SQLiteConnection.class);

	/**
	 * Creates the singleton instance
	 */
	private SQLiteConnection() {
		try {
			Class.forName(Configuration.getInstance().getJdbcDriver());
			initializeDBConnection();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			LOG.error("Error loading SQLite JDBC drivers: " + e.toString());
		}
	}
	
	/**
	 * Initializes the database connection if it is non-initialized or
	 * is closed.
	 */
	private static void initializeDBConnection() {
		databaseFullName = Configuration.getInstance().getDatabaseFullName();
		try {
			if(connection == null || connection.isClosed()) {
				connection =
					DriverManager.getConnection("jdbc:sqlite:" + databaseFullName);
			}
		} catch (SQLException e) {
			e.printStackTrace();
			LOG.error("Database initialization failed: " + e.toString());
		}
	}
	
	/**
	 * This is only for testing purposes, should NEVER be used for
	 * for deployment.
	 */
	public static void resetConnection() {
		try {
			if(connection != null || !connection.isClosed()) {
				connection.close();
				initializeDBConnection();
			}
		} catch (SQLException e) {
			LOG.error("Error resetting the DB connection: " + e.toString());
		}
	}

	/**
	 * Get the SQLite db connection
	 * @return The connection to the database.
	 */
	public static SQLiteConnection getInstance() {
		return sInstance;
	}

	/**
	 * Get the database connection.
	 */
	public Connection getConnection() {
		return connection;
	}
}