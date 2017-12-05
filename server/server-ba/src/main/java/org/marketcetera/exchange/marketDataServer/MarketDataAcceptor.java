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

import java.io.FileNotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import quickfix.ConfigError;
import quickfix.DefaultMessageFactory;
import quickfix.FieldConvertError;
import quickfix.FileStoreFactory;
import quickfix.LogFactory;
import quickfix.MessageFactory;
import quickfix.MessageStoreFactory;
import quickfix.RuntimeError;
import quickfix.ScreenLogFactory;
import quickfix.SessionSettings;
import quickfix.SocketAcceptor;
import org.marketcetera.exchange.backtesting.OrderBookSimulator;
import org.marketcetera.exchange.quickfix.FIXConfigurationLoader;

/**
 * FIX market data acceptor. Initializes the FIX server accepting market data
 * requests.
 * 
 * @author <a href="mailto:catsbf@dasch.dk">Daniel Schiermer</a>
 * @version 0.1
 *
 */
public class MarketDataAcceptor
{
	private SocketAcceptor acceptor;
	private static SessionSettings sessionSettings;
	private final OrderBookSimulator orderBookSimulator;
	private final Logger LOG = LoggerFactory.getLogger(MarketDataAcceptor.class);

	/**
	 * Create an instance
	 * @param configFilePath File path for the DIX server configuration
	 * @param orderBookSimulator Simulator used to replay the order book on market data request.
	 * @throws ConfigError
	 * @throws FileNotFoundException
	 */
	public MarketDataAcceptor(String configFilePath, OrderBookSimulator orderBookSimulator)
	throws ConfigError, FileNotFoundException
	{
		this.orderBookSimulator = orderBookSimulator;
		sessionSettings =
			FIXConfigurationLoader.loadFixConfiguration(configFilePath);
	}

	/**
	 * Initializes the socket acceptor, from Application class, message and log factories.
	 * @param settings SessionSettings for the current session
	 * @throws ConfigError
	 * @throws FieldConvertError
	 */
	private void initializeSocketAcceptor() throws ConfigError, FieldConvertError
	{
		MarketDataApplication app = new MarketDataApplication(sessionSettings, orderBookSimulator);
		MessageStoreFactory msgStoreFactory = new FileStoreFactory(sessionSettings);
		LogFactory logFactory = new ScreenLogFactory(true, true, true);
		MessageFactory msgFactory = new DefaultMessageFactory();

		acceptor = new SocketAcceptor(app, msgStoreFactory, sessionSettings,
				logFactory, msgFactory);
	}

	/**
	 * Initializes the socket and starts the server.
	 * @throws RuntimeError
	 * @throws ConfigError
	 * @throws FieldConvertError
	 */
	public void start() throws RuntimeError, ConfigError, FieldConvertError
	{
		initializeSocketAcceptor();

		acceptor.start();

		LOG.info("Market Data Server is running...");
	}

	/**
	 * Stops the server completely. Tries to log out any connected clients
	 * first.
	 */
	public void stop()
	{
		acceptor.stop();
	}
}