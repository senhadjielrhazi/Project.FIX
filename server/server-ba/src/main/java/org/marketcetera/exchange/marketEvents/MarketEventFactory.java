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
package org.marketcetera.exchange.marketEvents;

import quickfix.field.Side;

/**
 * Factory to parse the given market event as either a {@link Bid} or {@link Offer}
 * event based on the side
 *
 * @author <a href="mailto:catsbf@dasch.dk">Daniel Schiermer</a>
 * @version 0.1
 *
 */
public class MarketEventFactory {

	/**
	 * Get market event of the correct type based on the side.
	 * @param marketEvent The market event to parse
	 * @param side The side of the order (buy/sell/sell_short)
	 * @return The parsed market event.
	 */
	public static MarketEvent getMarketEvent(MarketEvent marketEvent, char side) {
		if(side == Side.BUY)
			return new Bid(marketEvent);
		else if(side == Side.SELL || side == Side.SELL_SHORT)
			return new Offer(marketEvent);
		else
			return null;
	}
}