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

import java.util.UUID;

/**
 * Unique Universal Identifier generator
 * 
 * @author <a href="mailto:catsbf@dasch.dk">Daniel Schiermer</a>
 * @version 0.1
 *
 */
public class UUIDGenerator {
	
	/**
	 * Generates a random {@link UUID} 
	 * @return Random {@link UUID}
	 */
	public static UUID getUUID() {
		return UUID.randomUUID();
	}
	
	/**
	 * Generates a random {@link UUID} and returns it as string 
	 * @return Random {@link UUID} as string
	 */
	public static String getUUIDString() {
		return getUUID().toString();
	}
}