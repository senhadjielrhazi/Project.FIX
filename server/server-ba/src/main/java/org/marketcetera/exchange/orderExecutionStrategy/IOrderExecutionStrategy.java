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
package org.marketcetera.exchange.orderExecutionStrategy;

import org.marketcetera.exchange.common.OrderedMarketEventList;
import org.marketcetera.exchange.marketEvents.Bid;
import org.marketcetera.exchange.marketEvents.Fill;
import org.marketcetera.exchange.marketEvents.Offer;

/**
 * Interface for the order execution strategy.
 * Defines the strategy used to match orders against each others.
 * 
 * @author <a href="mailto:catsbf@dasch.dk">Daniel Schiermer</a>
 * @version 0.1
 *
 */
public interface IOrderExecutionStrategy {
	
	/**
	 * Check the bid for execution (match) against the given list of offers.
	 * @param inBid Bid to check for execution
	 * @param offers Offers to check for execution against
	 * @return The fill (NULL if it was not filled).
	 */
	public Fill checkBidForExecution(Bid inBid, OrderedMarketEventList<Offer> offers);
	
	/**
	 * Check the offer for execution (match) against the given list of bids.
	 * @param inOffer Offer to check for execution
	 * @param bids Bids to check for execution against
	 * @return The fill (NULL if it was not filled).
	 */
	public Fill checkOfferForExecution(Offer inOffer, OrderedMarketEventList<Bid> bids);
}