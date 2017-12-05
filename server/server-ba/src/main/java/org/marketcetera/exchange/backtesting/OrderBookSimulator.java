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
package org.marketcetera.exchange.backtesting;

import java.util.Observable;

/**
 * Order book simulator loads the market events in the given time interval from the
 * database and replays them in order. All events are interpreted and changes the
 * cached order book accordingly.
 * 
 * Must notify observers when the simulation starts and stops.
 * It runs as a separate thread in order not to block all other application actions.
 * 
 * @author <a href="mailto:catsbf@dasch.dk">Daniel Schiermer</a>
 * @version 0.1
 *
 */
public abstract class OrderBookSimulator extends Observable implements Runnable {
	/**
	 * Start running the simulation.
	 * Fetches the market events from the database and runs a simulation
	 * of the historcal events.
	 */
	public abstract void runSimulation();
	
	/**
	 * Takes measures to stop the simulation (and the thread) at a fitting
	 * point.
	 */
	public abstract void stopSimulation();
}