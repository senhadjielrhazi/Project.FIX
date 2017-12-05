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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import quickfix.field.Side;
import org.marketcetera.exchange.common.OrderedMarketEventList;
import org.marketcetera.exchange.events.ClientCancelEvent;
import org.marketcetera.exchange.events.ClientFilledEvent;
import org.marketcetera.exchange.events.ClientMarketEvent;
import org.marketcetera.exchange.events.ClientNewPositionEvent;
import org.marketcetera.exchange.marketEvents.Bid;
import org.marketcetera.exchange.marketEvents.Fill;
import org.marketcetera.exchange.marketEvents.MarketEvent;
import org.marketcetera.exchange.marketEvents.MarketEventFactory;
import org.marketcetera.exchange.marketEvents.Offer;
import org.marketcetera.exchange.marketEvents.PartialFill;
import org.marketcetera.exchange.orderExecutionStrategy.IOrderExecutionStrategy;

/**
 * Client portfolio keeping track of orders placed, filled, cancelled by the client.
 * Can hold one asset, assumes that all entries are for the same asset.
 * Notifies on any changes made to the state of the client portfolio. Fires
 * events of type {@link ClientMarketEvent}.
 * 
 * Observes the {@link OrderBook} and tests all client positions for match against
 * new orders.
 * 
 * @author <a href="mailto:catsbf@dasch.dk">Daniel Schiermer</a>
 * @version 0.1
 *
 */
public class ClientPortfolio extends Observable implements Observer {
	private List<Bid> bids;
	private List<Offer> offers;
	private List<Fill> fills;
	private OrderBook orderBook;
	private final IOrderExecutionStrategy orderExecutionStrategy;

	/**
	 * Instantiates the object with the given {@link IOrderExecutionStrategy}
	 * @param orderExecutionStrategy Execution strategy used to match opposing
	 * orders.
	 */
	public ClientPortfolio(IOrderExecutionStrategy orderExecutionStrategy) {
		this.orderExecutionStrategy = orderExecutionStrategy;
		bids = new ArrayList<Bid>();
		offers = new ArrayList<Offer>();
		fills = new ArrayList<Fill>();
		orderBook = OrderBook.getInstance();
	}

	/**
	 * Add an open position to the portfolio.
	 * Notifies observers about the addition.
	 * @param marketEvent Event to add to the portfolio.
	 */
	public void addOpenPosition(MarketEvent marketEvent) {
		if(marketEvent instanceof Bid)
			addBid((Bid)marketEvent);
		else if(marketEvent instanceof Offer)
			addOffer((Offer)marketEvent);
		else
			return;

		orderBook.addMarketEvent(marketEvent);

		notifyChange(new ClientNewPositionEvent(marketEvent));
	}

	/**
	 * Add a {@link Bid} to the portfolio
	 * @param bid {@link Bid} to add
	 */
	private void addBid(Bid bid) {
		bids.add(bid);
	}

	/**
	 * Add an {@link Offer} to the portfolio
	 * @param offer {@link Offer} to add
	 */
	private void addOffer(Offer offer) {
		offers.add(offer);
	}

	/**
	 * Add a {@link Fill} to the portfolio.
	 * Notifies observers on the change.
	 * @param fill {@link Fill} to add
	 * @return Boolean indicating whether or not the open
	 * event for the {@link Fill} was deleted from the open
	 * positions
	 */
	public boolean addFill(Fill fill) {
		fills.add(fill);
		orderBook.addMarketEvent(fill);
		
		char side = fill.getSide();
		
		boolean isDeleted = false;
		if(side == Side.BUY)
			isDeleted  = deleteBid(fill.getClientOrderID());
		if(side == Side.SELL || side == Side.SELL_SHORT)
			isDeleted = deleteOffer(fill.getClientOrderID());

		notifyChange(new ClientFilledEvent(fill));
		
		return isDeleted;
	}

	/**
	 * Add a {@link PartialFill} to the portfolio.
	 * Notifies observers on the change.
	 * @param partialFill {@link PartialFill} to add
	 * event for the {@link Fill} was deleted from the open
	 * positions.
	 */
	public void addPartialFill(PartialFill partialFill) {
		fills.add(partialFill);

		orderBook.addMarketEvent(partialFill);

		updateOpenPosition(partialFill);

		notifyChange(new ClientFilledEvent(partialFill));
	}

	/**
	 * Updates the open position matching the {@link PartialFill},
	 * to the new quantities and average price.
	 * @param partialFill {@link PartialFill} that matches the open
	 * position to update.
	 */
	private void updateOpenPosition(PartialFill partialFill) {
		MarketEvent marketEvent = null;
		char side = partialFill.getSide();
		String clientOrderID = partialFill.getClientOrderID();
		int remainingQty = partialFill.getRemainingQty();
		double avgPrice = partialFill.getAvgPrice();
		int cumQty = partialFill.getCumQty();

		if(side == Side.BUY)
			marketEvent = getOpenBidPosition(clientOrderID);
		else if(side == Side.SELL || side == Side.SELL_SHORT)
			marketEvent = getOpenOfferPosition(clientOrderID);

		if(marketEvent == null) {
			//Add the market event to the order book (either bid or offer)
			marketEvent = MarketEventFactory.getMarketEvent(partialFill, side);

			if(side == Side.BUY)
				addBid((Bid)marketEvent);
			else if(side == Side.SELL || side == Side.SELL_SHORT)
				addOffer((Offer)marketEvent);
		}

		updateMarketEvent(marketEvent, remainingQty, avgPrice, cumQty);
	}

	/**
	 * Update the remaining quantity, average price and cumulative quantity.
	 * @param marketEvent {@link MarketEvent} to update
	 * @param remainingQty New remaining quantity
	 * @param avgPrice New average price
	 * @param cumQty New cumulative quantity
	 */
	private void updateMarketEvent(MarketEvent marketEvent, int remainingQty,
			double avgPrice, int cumQty) {
		marketEvent.setRemainingQty(remainingQty);
		marketEvent.setAvgPrice(avgPrice);
		marketEvent.setCumQty(cumQty);
	}

	/**
	 * Gets the existing client bid with the given order id.
	 * @param clOrdID Client order id.
	 * @return The open client bid order with the given order id,
	 * returns NULL if no match exist
	 */
	public Bid getOpenBidPosition(String clOrdID) {
		for(Bid bid : bids) {
			if(bid.getClientOrderID().equals(clOrdID))
				return bid;
		}

		return null;
	}

	/**
	 * Gets the existing client offer with the given order id.
	 * @param clOrdID Client order id.
	 * @return The open client offer order with the given order id,
	 * returns NULL if no match exist
	 */
	public Offer getOpenOfferPosition(String clOrdID) {
		for(Offer offer : offers) {
			if(offer.getClientOrderID().equals(clOrdID))
				return offer;
		}

		return null;
	}

	/**
	 * Deletes the bid with the given order id and notifies observers
	 * @param clientOrderID Id of the order to cancel/delete
	 */
	public void cancelBid(String clientOrderID) {
		Bid openBidPosition = getOpenBidPosition(clientOrderID);

		if(deleteBid(clientOrderID)) {
			notifyChange(new ClientCancelEvent(openBidPosition));
		}
	}

	/**
	 * Cancels the given offer by removing it from the open offers.
	 * Notifies observers on the cancel
	 * @param clientOrderID Order id of the offer to cancel.
	 */
	public void cancelOffer(String clientOrderID) {
		Offer openOfferPosition = getOpenOfferPosition(clientOrderID);

		if(deleteOffer(clientOrderID)) {
			notifyChange(new ClientCancelEvent(openOfferPosition));
		}
	}

	/**
	 * Delete the bid with the given client order id
	 * @param clOrdID Id of the bid to delete
	 * @return Boolean indicating whether or not a
	 * bid with the given client order id.
	 */
	private boolean deleteBid(String clOrdID) {
		boolean isDeleted = false;

		for(int i = 0; i < bids.size(); i++) {
			if(bids.get(i).getClientOrderID().equals(clOrdID))
			{
				bids.remove(i);
				isDeleted = true;
				break;
			}
		}

		return isDeleted;
	}

	/**
	 * Delete the offer with the given client order id
	 * @param clOrdID Id of the offer to delete
	 * @return Boolean indicating whether or not a
	 * offer with the given client order id.
	 */
	private boolean deleteOffer(String clOrdID) {
		boolean isDeleted = false;

		for(int i = 0; i < offers.size(); i++) {
			if(offers.get(i).getClientOrderID().equals(clOrdID))
			{
				offers.remove(i);
				isDeleted = true;
				break;
			}
		}

		return isDeleted;
	}

	/**
	 * Notifies observers on any change
	 * @param event Event to fire
	 */
	private void notifyChange(ClientMarketEvent event) {
		setChanged();
		notifyObservers(event);
	}

	/**
	 * Calls the correct methods based on event type.
	 */
	@Override
	public void update(Observable observable, Object event) {
		if(event instanceof Bid) {
			checkOffersForMatch(((Bid)event));
		} else if(event instanceof Offer) {
			checkBidsForMatch(((Offer)event));
		}
	}

	/**
	 * Update the given {@link MarketEvent} according to the {@link Fill}
	 * @param marketEvent {@link MarketEvent} to update
	 * @param fill {@link Fill} to update against.
	 * @return
	 */
	private boolean updateAccordingToMatch(MarketEvent marketEvent, Fill fill) {
		boolean isDeleted = false;

		if(fill != null)
		{
			if(fill instanceof PartialFill)
				addPartialFill((PartialFill)fill);
			else if(fill instanceof Fill)
				isDeleted = addFill(fill);
		}

		return isDeleted;
	}

	/**
	 * Check all open {@link Offer}s for match against the given {@link Bid}
	 * @param bid {@link Bid} to match against
	 */
	private void checkOffersForMatch(Bid bid) {
		int size = offers.size();

		for(int i = 0; i < size; i++) {
			Offer offer = offers.get(i);
			Fill fill = orderExecutionStrategy.checkOfferForExecution(offer,
					new OrderedMarketEventList<Bid>(Arrays.asList(bid)));
			boolean isDeleted = updateAccordingToMatch(offer, fill);

			if(isDeleted) {
				size--;
				i--;
			}

		}
	}

	/**
	 * Check all open {@link Bids}s for match against the given {@link Offer}
	 * @param offer {@link Offer} to match against
	 */
	private void checkBidsForMatch(Offer offer) {
		int size = bids.size();

		for(int i = 0; i < size; i++) {
			Bid bid = bids.get(i);
			Fill fill = orderExecutionStrategy.checkBidForExecution(bid,
					new OrderedMarketEventList<Offer>(Arrays.asList(offer)));
			boolean isDeleted = updateAccordingToMatch(bid, fill);

			if(isDeleted) {
				size--;
				i--;
			}

		}
	}

	/**
	 * Primarily for testing purposes. Bids should NEVER be added directly
	 * to this list, instead the method {@link #addOpenPosition(MarketEvent)}
	 * should be used.
	 * @return The list of open bid positions.
	 */
	public List<Bid> getBids() {
		return bids;
	}

	/**
	 * Primarily for testing purposes. Offers should NEVER be added directly
	 * to this list, instead the method {@link #addOpenPosition(MarketEvent)}
	 * should be used.
	 * @return The list of open offer positions.
	 */
	public List<Offer> getOffers() {
		return offers;
	}

	/**
	 * Primarily for testing purposes. Fills should NEVER be added directly
	 * to this list, instead the method {@link #addFill(Fill)}
	 * should be used.
	 * @return The list of open fills positions.
	 */
	public List<Fill> getFills() {
		return fills;
	}
}