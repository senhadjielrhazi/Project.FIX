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
package org.marketcetera.exchange.orderServer;

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
import org.marketcetera.exchange.backtesting.ClientPortfolio;
import org.marketcetera.exchange.orderExecutionStrategy.IOrderExecutionStrategy;
import org.marketcetera.exchange.quickfix.FIXConfigurationLoader;

/**
 * FIX order acceptor. Initializes the FIX server accepting orders on the exchange.
 * 
 * @author <a href="mailto:catsbf@dasch.dk">Daniel Schiermer</a>
 * @version 0.1
 *
 */
public class OrderAcceptor
{
	private SocketAcceptor acceptor;
	private SessionSettings sessionSettings;
	private IOrderExecutionStrategy orderExecutionStrategy;
	private ClientPortfolio clientPortfolio;
	private final Logger LOG = LoggerFactory.getLogger(OrderAcceptor.class);

	/**
	 * Create an instance with the given parameters
	 * @param orderExecutionStrategy Order execution strategy used to match orders
	 * @param clientPortfolio Client portfolio
	 * @param configFilePath File path for server configuration
	 * @throws ConfigError
	 * @throws FieldConvertError
	 * @throws FileNotFoundException
	 */
	public OrderAcceptor(IOrderExecutionStrategy orderExecutionStrategy,
			ClientPortfolio clientPortfolio,
			String configFilePath)
	throws ConfigError, FieldConvertError, FileNotFoundException
	{
			this.orderExecutionStrategy = orderExecutionStrategy;
			this.clientPortfolio = clientPortfolio;
			sessionSettings = FIXConfigurationLoader.loadFixConfiguration(configFilePath);
	}

	/**
	 * Initializes the socket acceptor, from Application class, message and log factories.
	 * @param settings SessionSettings for the current session
	 * @throws ConfigError
	 * @throws FieldConvertError
	 */
	private void initializeSocketAcceptor() 
	throws ConfigError, FieldConvertError
	{
		OrderApplication app = new OrderApplication(sessionSettings,
				orderExecutionStrategy, clientPortfolio);
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
		LOG.info("Order Server is running...");
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