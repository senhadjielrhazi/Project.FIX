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
package org.marketcetera.exchange;

import java.io.IOException;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import quickfix.ConfigError;
import quickfix.FieldConvertError;
import quickfix.RuntimeError;
import org.marketcetera.exchange.backtesting.ClientPortfolio;
import org.marketcetera.exchange.backtesting.OrderBook;
import org.marketcetera.exchange.backtesting.OrderBookSimulator;
import org.marketcetera.exchange.events.SimulationStoppedEvent;
import org.marketcetera.exchange.marketDataLoader.IMarketDataLoader;
import org.marketcetera.exchange.marketDataServer.MarketDataAcceptor;
import org.marketcetera.exchange.orderServer.OrderAcceptor;
import org.marketcetera.exchange.report.Report;

/**
 * Exchange object to start the exchange. Starts a FIX 4.4 server for the market data,
 * and FIX 4.4 server that accept orders.
 * 
 * Observes the simulator and stops the servers, and reports, when the simulation stops.
 * 
 * @author <a href="mailto:catsbf@dasch.dk">Daniel Schiermer</a>
 * @version 0.1
 * 
 */
public class CcfeaExchange implements Observer {

	private final boolean initializeMarketData;
	private final boolean runBacktest;
	private final IMarketDataLoader marketDataLoader;
	private final OrderAcceptor orderAcceptor;
	private final MarketDataAcceptor marketDataAcceptor;
	private final ClientPortfolio clientPortfolio;
	private final OrderBookSimulator orderBookSimulator;
	private final List<Report> reports;
	private static final Logger LOG = LoggerFactory.getLogger(CcfeaExchange.class);

	/**
	 * Initializes the object with the given parameters.
	 * @param runMarketDataLoader Boolean indication whether or not to run the
	 * market data loader (load data from csv files into the database). Should
	 * false after the first run.
	 * @param runBacktest Boolean indicating whether or not the backtest
	 * simulations should run (after data load, if that indicator is set to true).
	 * True if you want to run the simulation, false if you only want to load
	 * the data.
	 * @param orderAcceptor Instance of the FIX order acceptor server.
	 * @param marketDataAcceptor Instance of the FIX market data server.
	 * @param marketDataLoader The market data loader to use for loading market
	 * data from csv files into the database.
	 * @param reports A list of reports that should be generated during the backtest.
	 * @param clientPortfolio The client portfolio instance that should be used
	 * to manage the client positions. This is the portfolio that the reports observe.
	 * @param orderBookSimulator The simulator to use for the backtest.
	 */
	public CcfeaExchange(boolean runMarketDataLoader, boolean runBacktest,
			OrderAcceptor orderAcceptor,
			MarketDataAcceptor marketDataAcceptor,
			IMarketDataLoader marketDataLoader,
			List<Report> reports,
			ClientPortfolio clientPortfolio,
			OrderBookSimulator orderBookSimulator) {
		this.initializeMarketData = runMarketDataLoader;
		this.runBacktest = runBacktest;
		this.orderAcceptor = orderAcceptor;
		this.marketDataAcceptor = marketDataAcceptor;
		this.marketDataLoader = marketDataLoader;
		this.reports = reports;
		this.clientPortfolio = clientPortfolio;
		this.orderBookSimulator = orderBookSimulator;
		
		orderBookSimulator.addObserver(this);
		
		initializeReports(reports);
	}
	
	/**
	 * Adds the reports as observers to the given client portfolio
	 * @param reports The list of reports that should observe the portfolio
	 */
	private void initializeReports(List<Report> reports) {
		for(Report report : reports) {
			clientPortfolio.addObserver(report);
			orderBookSimulator.addObserver(report);
		}
	}
	
	/**
	 * Start the exchange, including the data load and FIX server, depending
	 * on the boolean indicators passes as class arguments
	 */
	public void runExchange() {
		if(initializeMarketData) {
			marketDataLoader.loadData();
		}

		if(runBacktest) {
			try {
				OrderBook.getInstance().addObserver(clientPortfolio);
				
				marketDataAcceptor.start();
				orderAcceptor.start();
				
				System.in.read();
			} catch (ConfigError e) {
				LOG.error("Quickfix configuration error: " + e.toString());
			} catch (RuntimeError e) {
				LOG.error("Quickfix runtime error: " + e.toString());
			} catch (FieldConvertError e) {
				LOG.error("Quickfix field conversion error: " + e.toString());
			} catch (IOException e) {
				LOG.error(e.toString());
			}
		}
	}
	
	/**
	 * Stops the FIX servers and the reports.
	 * Makes sure that the servers are stopped before the reports.
	 */
	private void stopServers() {
		marketDataAcceptor.stop();
		orderAcceptor.stop();
		
		for(Report report : reports)
			report.stop();
		
		System.exit(0);
	}

	/**
	 * Listens to the simulator, reacts when a stop event
	 * is received.
	 */
	@Override
	public void update(Observable arg0, Object arg1) {
		if(arg1 instanceof SimulationStoppedEvent)
			stopServers();
	}
}