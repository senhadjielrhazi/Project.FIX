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
package org.marketcetera.exchange.common;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

/**
 * General configuration. Configuration values are fetced from the Spring
 * configuration.
 * 
 * @author <a href="mailto:catsbf@dasch.dk">Daniel Schiermer</a>
 * @version 0.1
 *
 */
public class Configuration {
	private DateFormat printDateFormat;
	private String symbol;
	private String serverID;
	private String clientID;
	private SpringBeanLoader springBeanLoader;
	private String databaseFullName;
	private String marketDataTableName;
	private Boolean useSimulationTime;
	private String jdbcDriver;
	private static Configuration sInstance = new Configuration();
	
	/**
	 * Create the singleton instance. Fetches the values from Spring configuration.
	 */
	private Configuration() {
		this.springBeanLoader = new SpringBeanLoader();
        this.printDateFormat = (SimpleDateFormat)springBeanLoader.getBean("PrintDateFormat");
        this.symbol = (String) springBeanLoader.getBean("Symbol");
        this.serverID = (String) springBeanLoader.getBean("ServerID");
        this.clientID = (String) springBeanLoader.getBean("ClientID");
        this.databaseFullName = (String) springBeanLoader.getBean("DatabaseFullName");
        this.marketDataTableName = (String) springBeanLoader.getBean("MarketDataTableName");
        this.useSimulationTime = (Boolean) springBeanLoader.getBean("UseSimulationTime");
        this.jdbcDriver = (String) springBeanLoader.getBean("JDBCDriver");
	}

	/**
	 * Get the singleton instance
	 * @return The configuration instance
	 */
	public static Configuration getInstance() {
        return sInstance;
    }
	
	/**
	 * This is only for testing purposes, should NEVER be used for
	 * for deployment.
	 * @param configuration
	 */
	public static void setInstance(Configuration configuration) {
        sInstance = configuration;
    }
	
	/**
	 * Get a Spring bean value from the bean with the given ID.
	 * @param beanId Id of the bean whose value to get.
	 * @return The value of the Spring bean with the given id.
	 */
	public Object getBean(String beanId) {
		return springBeanLoader.getBean(beanId);
    }

	/**
	 * Get the print data format. Should be used to format all dates that are printed anywhere.
	 * @return The date formatter used in prints.
	 */
	public DateFormat getPrintDateFormat() {
		return printDateFormat;
	}

	/**
	 * Get the configured symbol in upper case.
	 * @return The configured symbols as upper case
	 */
	public String getSymbol() {
		return symbol.toUpperCase();
	}

	/**
	 * Get the server ID
	 * @return Server ID
	 */
	public String getServerID() {
		return serverID;
	}

	/**
	 * Get the client ID
	 * @return Client ID
	 */
	public String getClientID() {
		return clientID;
	}

	/**
	 * Get the full database name, including path
	 * @return Full database name, including path
	 */
	public String getDatabaseFullName() {
		return databaseFullName;
	}

	/**
	 * Get the name of the market event database table
	 * @return The name of the market event database table
	 */
	public String getMarketDataTableName() {
		return marketDataTableName;
	}

	/**
	 * Get the boolean indication whether or not to use the simulation time
	 * for client events.
	 * @return Boolean indicating whether or not to use the simulation time
	 * for client events.
	 */
	public Boolean getUseSimulationTime() {
		return useSimulationTime;
	}

	/**
	 * Get the JDBC driver string, to use in classes that utilise the database
	 * @return
	 */
	public String getJdbcDriver() {
		return jdbcDriver;
	}
}