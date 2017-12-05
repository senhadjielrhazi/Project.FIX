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

import org.marketcetera.exchange.common.Configuration;

/**
 * Main program entry
 * 
 * @author <a href="mailto:catsbf@dasch.dk">Daniel Schiermer</a>
 * @version 0.1
 *
 */
public class Main {

	/**
	 * Application entry point.
	 * Loads the {@link CcfeaExchange} from the Spring configuration.
	 * @param args
	 */
	public static void main(String[] args) {
		CcfeaExchange ccfeaExchange = (CcfeaExchange) Configuration.getInstance().getBean("CcfeaExchange");
		ccfeaExchange.runExchange();
	}
}