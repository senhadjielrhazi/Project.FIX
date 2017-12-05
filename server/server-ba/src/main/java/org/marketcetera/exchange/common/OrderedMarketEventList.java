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

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import org.marketcetera.exchange.marketEvents.MarketEvent;

/**
 * List implementation that accepts {@link MarketEvent}s and orders them either
 * ascending or descending according to price.
 * 
 * @author <a href="mailto:catsbf@dasch.dk">Daniel Schiermer</a>
 * @version 0.1
 *
 * @param <M> MarketEvent type of the list
 */
public class OrderedMarketEventList<M extends MarketEvent> extends LinkedList<M> {
	private static final long serialVersionUID = 1L;

	/**
	 * ASC is used for offers
	 * DESC is used for bids
	 */
	public static enum Order {ASC, DESC}
	private MarketEventComparator<M> marketEventComp;
	
	/**
	 * Create an instance of the list with market events order
	 * according to the given order.
	 * @param order Order to sort the market events by
	 */
	public OrderedMarketEventList(Order order) {
		this.marketEventComp = new MarketEventComparator<M>(order);
	}
	
	/**
	 * Create an instance of the ordered market events with all the elements
	 * passed as argument. Assumes Descending order.
	 * @param marketEvents
	 */
	public OrderedMarketEventList(List<M> marketEvents) {
		this.marketEventComp = new MarketEventComparator<M>(Order.DESC);
		
		for(M marketEvent : marketEvents) {
			this.add(marketEvent);
		}
	}

	/**
	 * Adds a market event to the list, sorted by price.
	 * If the list is to be sorted ascending, the market event with the
	 * lowest price is on top (index 0). If the price equals another
	 * market event's price, the newest event is placed after the existing.
	 * 
	 * If the list is to be sorted descending, the market event with the
	 * highest price is on top (index 0). If the price equals another
	 * market event's price, the newest event is placed after the existing.
	 * 
	 * @param bid The market event that is added to the list
	 */
	@Override
	public boolean add(M marketEvent) {
			for(int i = 0; i < super.size(); i++) {
				int comparedVal = marketEventComp.compare(marketEvent, super.get(i));
				
				if(comparedVal == -1) {
					super.add(i, marketEvent);
					return true;
				} else if (comparedVal == 0) {
					if(super.size() == i+1) {	//Insert at the end
						super.add(marketEvent);
						return true;
					}else if(super.size() > i+1 &&
							marketEventComp.compare(marketEvent, super.get(i+1)) != 0) {
						super.add(i+1, marketEvent);
						return true;
					}
				}
			}
			
			super.add(marketEvent);
			return true;
	}
	
	/**
	 * A comparator that compares market events by price.
	 * @see {@link java.util.Comparator} for more info.
	 * @author Daniel Schiermer
	 *
	 * @param <M> Market event type.
	 */
	private static class MarketEventComparator<M extends MarketEvent> implements Comparator<MarketEvent> {
		private Order order;

		/**
		 * Define the order in which the market events are to be sorted
		 * @param order Ascending or Descending
		 */
		public MarketEventComparator(Order order) {
			this.order = order;
		}

		/**
		 * Compares the two market events according to the order specified in the
		 * constructor
		 * @see {@link java.util.Comparator#compare(Object, Object)} for more info.
		 * @see {@link #orderAscending(MarketEvent, MarketEvent)} for ascending order
		 * @see {@link #orderDescending(MarketEvent, MarketEvent)} for descending order
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
}