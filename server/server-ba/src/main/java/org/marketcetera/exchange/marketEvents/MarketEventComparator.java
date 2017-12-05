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

import java.util.Comparator;

/**
 * A {@link Comparator} which compares two {@link MarketEvent} objects
 * based on price. An order is passed as argument to specify whether the
 * price should be compared ascending or descending.
 *
 * @author <a href="mailto:catsbf@dasch.dk">Daniel Schiermer</a>
 * @version 0.1
 *
 */
public class MarketEventComparator implements Comparator<MarketEvent> {
	public static enum Order {ASC, DESC}
	private Order order;

	/**
	 * Initializes the object with the given order.
	 * @param order The order of that the objects should be compared against.
	 */
	public MarketEventComparator(Order order) {
		this.order = order;
	}

	/**
	 * Compares the two objects {@link Comparator#compare(Object, Object)}
	 * @return An integer of value -1, 0, 1 depending on the Order of
	 * the comparator.
	 * @see {@link MarketEventComparator#orderAscending(MarketEvent, MarketEvent)} for
	 * return values on type Ascending
	 * @see {@link MarketEventComparator#orderDescending(MarketEvent, MarketEvent)} for
	 * return values on type Descending
	 */
	@Override
	public int compare(MarketEvent marketEvent1, MarketEvent marketEvent2) {
		if(order == Order.ASC)
			return orderAscending(marketEvent1, marketEvent2);
		else
			return orderDescending(marketEvent1, marketEvent2);
	}

	/**
	 * Compares the two market events according to ascending order, by order price.
	 * If marketEvent1 is less than marketEvent2, -1 is returned,
	 * if they are equal 0 is returned, and if marketEvent2 is greater than marketEvent1
	 * +1 is returned.
	 * @param marketEvent1 First market event
	 * @param marketEvent2 Second market event
	 * @return a negative integer, zero, or a positive integer as the first
	 * argument is less than, equal to, or greater than the second.
	 */
	private int orderAscending(MarketEvent marketEvent1, MarketEvent marketEvent2) {
		//a negative integer, zero, or a positive integer as the first argument is less than, equal to, or greater than the second.
		if(marketEvent1.getOrderPrice() < marketEvent2.getOrderPrice())
			return -1;
		else if(marketEvent1.getOrderPrice() == marketEvent2.getOrderPrice())
			return 0;
		else
			return 1;
	}

	/**
	 * Compares the two market events according to descending order, by order price.
	 * If marketEvent1 is greater than marketEvent2, -1 is returned,
	 * if they are equal 0 is returned, and if marketEvent2 is less than marketEvent1
	 * +1 is returned.
	 * @param marketEvent1 First market event
	 * @param marketEvent2 Second market event
	 * @return a negative integer, zero, or a positive integer as the first
	 * argument is less than, equal to, or greater than the second.
	 */
	private int orderDescending(MarketEvent marketEvent1, MarketEvent marketEvent2) {
		//a negative integer, zero, or a positive integer as the second argument greater than, equal to, or less than the second.
		if(marketEvent1.getOrderPrice() > marketEvent2.getOrderPrice())
			return -1;
		else if(marketEvent1.getOrderPrice() == marketEvent2.getOrderPrice())
			return 0;
		else
			return 1;
	}
}