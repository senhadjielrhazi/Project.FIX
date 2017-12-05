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

import java.util.LinkedList;
import java.util.List;
import java.util.Observable;

import quickfix.field.Side;
import org.marketcetera.exchange.common.OrderedMarketEventList;
import org.marketcetera.exchange.common.OrderedMarketEventList.Order;
import org.marketcetera.exchange.marketEvents.Bid;
import org.marketcetera.exchange.marketEvents.Fill;
import org.marketcetera.exchange.marketEvents.MarketEvent;
import org.marketcetera.exchange.marketEvents.Offer;
import org.marketcetera.exchange.marketEvents.PartialFill;

/**
 * Order book keeping the current state of the order book. Notifies on any
 * changes in the order book.
 * 
 * @author <a href="mailto:catsbf@dasch.dk">Daniel Schiermer</a>
 * @version 0.1
 * 
 * NOTE: The order book could probably be left out in the current, limited, version, but
 * when the implementation is updated to send an initial state of the order book as 
 * "MarketDataFullSnapshotRefresh" the order book is necessary.
 */
public class OrderBook extends Observable {
	private OrderedMarketEventList<Offer> offers;
	private OrderedMarketEventList<Bid> bids;
	private List<Fill> trades;
	private static OrderBook sInstance = new OrderBook();

	private OrderBook() {
		initialize();
	}
	
	private void initialize() {
		this.offers = new OrderedMarketEventList<Offer>(Order.ASC);
		this.bids = new OrderedMarketEventList<Bid>(Order.DESC);
		this.trades = new LinkedList<Fill>();
	}
	
	public static OrderBook getInstance()
    {	
        return sInstance;
    }
	
	/**
	 * This is only for testing purposes, should NEVER be used for
	 * for deployment.
	 * @param configuration
	 */
	public static void setInstance(OrderBook orderBook) {
        sInstance = orderBook;
    }
	
	/**
	 * Adds an offer to the list of offer. The list is sorted by price,
	 * having the one with the lowest price on top.
	 * @param offer The offer that is added to the list
	 */
	private void deleteOffer(String orderCode) {
		for(int i = 0; i < offers.size(); i++) {
			if(offers.get(i).getClientOrderID().equals(orderCode)) {
				offers.remove(i);
				break;
			}
		}
	}
	
	private void deleteBid(String orderCode) {
		for(int i = 0; i < bids.size(); i++) {
			if(bids.get(i).getClientOrderID().equals(orderCode)) {
				bids.remove(i);
				break;
			}
		}
	}
	
	public synchronized void deleteMarketEvent(MarketEvent marketEvent) {
		String orderCode = null;
		
		if(marketEvent != null)
			orderCode = marketEvent.getClientOrderID();
		
		if(marketEvent instanceof Bid)
			deleteBid(orderCode);
		else if (marketEvent instanceof Offer)
			deleteOffer(orderCode);
		else
			return;
		
		marketEvent.setAction(MarketEvent.Action.DELETE);
		
		notifyChange(marketEvent);
	}

	private void addFill(Fill fill) {
		char side = fill.getSide();
		String clientOrderID = fill.getClientOrderID();
		
		if(side == Side.BUY)
			deleteBid(clientOrderID);
		else if(side == Side.SELL || side == Side.SELL_SHORT)
			deleteOffer(clientOrderID);
		
		trades.add(fill);
	}

	private void addPartialFill(PartialFill partialFill) {
		String orderID = partialFill.getClientOrderID();
		int tradeSize = partialFill.getOrderQty();
		double price = partialFill.getOrderPrice();
		char side = partialFill.getSide();
		
		if(side == Side.BUY)
			updateBid(orderID, tradeSize, price);
		else if(side == Side.SELL)
			updateOffer(orderID, tradeSize, price);
		
		trades.add(partialFill);
	}

	private void updateBid(String orderCode, int tradeSize, double price) {
		for(int i = 0; i < bids.size(); i++) {
			if(bids.get(i).getClientOrderID().equals(orderCode)) {
				updateMarketEvent(bids.get(i), tradeSize, price);
				break;
			}
		}
	}

	private void updateOffer(String orderCode, int tradeSize, double price) {
		for(int i = 0; i < offers.size(); i++) {
			if(offers.get(i).getClientOrderID().equals(orderCode)) {
				updateMarketEvent(offers.get(i), tradeSize, price);
				break;
			}
		}
	}
	
	private void updateMarketEvent(MarketEvent marketEvent, int tradeSize, double price) {
		marketEvent.subtractFromRemainingQty(tradeSize);
		marketEvent.addToCumQty(tradeSize);
	}
	
	public synchronized void addMarketEvent(MarketEvent marketEvent) {
		if(marketEvent instanceof Bid)
			bids.add((Bid)marketEvent);
		else if(marketEvent instanceof Offer)
			offers.add((Offer)marketEvent);
		else if(marketEvent instanceof PartialFill)
			addPartialFill((PartialFill)marketEvent);
		else if(marketEvent instanceof Fill)
			addFill((Fill)marketEvent);
		
		notifyChange(marketEvent);
	}
	
	/**
	 * Get the highest bid price currently in the order book.
	 * If the bids are empty -1 is returned.
	 * @return The highest bid price, -1 if no bids are recorded
	 */
	public double getHighestBidPrice() {
		if(bids.size() > 0)
			return bids.get(0).getOrderPrice();
	
		return -1;
	}
	
	/**
	 * Get the lowest offer price currently in the order book.
	 * If the offers are empty -1 is returned.
	 * @return The lowest offer price, -1 if no offer are recorded
	 */
	public double getLowestOfferPrice() {
		if(offers.size() > 0)
			return offers.get(0).getOrderPrice();
	
		return -1;
	}

	/**
	 * Get the list of offers.
	 * Note that one should never add offers directly to this list,
	 * but rather use {@link #addOffer(Offer)}
	 * @return List of offers
	 */
	public OrderedMarketEventList<Offer> getOffers() {
		return offers;
	}

	/**
	 * Get the list of bids.
	 * Note that one should never add bids directly to this list,
	 * but rather use {@link #addBid(Bid)}
	 * @return List of bids
	 */
	public OrderedMarketEventList<Bid> getBids() {
		return bids;
	}

	/**
	 * Get the list of trades.
	 * Note that one should never add trades directly to this list,
	 * but rather use {@link #addFill(Trade)}
	 * @return List of trades
	 */
	public List<Fill> getFills() {
		return trades;
	}
	
	private void notifyChange(MarketEvent marketEvent) {
		setChanged();
		notifyObservers(marketEvent);
	}
}