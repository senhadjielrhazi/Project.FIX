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

import quickfix.field.OrdType;
import org.marketcetera.exchange.common.OrderedMarketEventList;
import org.marketcetera.exchange.marketEvents.Bid;
import org.marketcetera.exchange.marketEvents.Fill;
import org.marketcetera.exchange.marketEvents.MarketEvent;
import org.marketcetera.exchange.marketEvents.Offer;
import org.marketcetera.exchange.marketEvents.PartialFill;

/**
 * Ccfea order execution strategy implementation.
 * 
 * Full Match:
 * On a bid order, a full match (fill) happens when the order book contains one or more opposing
 * offers that can fill the bid, this means that a number of offers have a cumulative size larger
 * than the size of the bid, and the prices of the offers are all less than or equal to the bid
 * price. It is a partial match (partial fill) if there exist one or more opposing offers where
 * the price is less than or equal to the bid price and the cumulative size is less than the
 * bid size.
 * 
 * Similar for offer orders.
 * 
 * Limit orders
 * The strategy executes the bid at the best price possible, hence the execution price will
 * always be less than or equal to the bid price.
 * An offer price is always equal to the offer price.
 * 
 * 
 * Market orders
 * Market orders are matched with the most competitive limit orders on the opposing order
 * book. Market orders are always executed, assuming that there exist opposing orders with
 * a cumulative size larger than, or equal, to the market order.
 * If the order cannot be filled, it is either partially filled or placed as a new order in the
 * order book, and will be filled or partially filled as soon new matching orders arrives.
 *
 * The execution price is calculated as the average price matched at the opposing orders.
 * 
 * Fills does not have any effect on the orders that are matched against.
 * 
 * @author <a href="mailto:catsbf@dasch.dk">Daniel Schiermer</a>
 * @version 0.1
 */
public class CcfeaOrderExecutionStrategy implements IOrderExecutionStrategy {
	
	/**
	 * Checks the given bid for execution against the list of offers.
	 * The bid is executed according to the rules explained in the {@link CcfeaOrderExecutionStrategy}
	 * JavaDoc.
	 * @param inBid Bid to check for execution
	 * @param offers List of offers to check for execution against
	 * @return A {@link Fill} or {@link PartialFill} if the order is filled or partially
	 * fill. If not NULL is returned
	 */
	public Fill checkBidForExecution(Bid inBid, OrderedMarketEventList<Offer> offers) {
		double bidPrice =
			inBid.getOrderType() != OrdType.MARKET ? inBid.getOrderPrice() : Double.POSITIVE_INFINITY;
			
		int remainingQty = inBid.getRemainingQty();
		double cumPrice = 0;
		int cumQty = 0;
		
		for(MarketEvent offer : offers) {
			//If the price of the offer matches (or is better)
			if(offer.getOrderPrice() <= bidPrice && !offer.getClientID().equals(inBid.getClientID())) {
				//If the size of the offer is greater or equals
				if(offer.getRemainingQty() >= remainingQty) {
					//We can execute a complete fill
					cumPrice += offer.getOrderPrice() * remainingQty;
					cumQty += remainingQty;
					
					return getFill(inBid, bidPrice, cumPrice, cumQty);
				} else {
					//The offer price matches, but the size does not
					//we need to look deeper into the order book
					int filledQty = offer.getRemainingQty();
					cumPrice += offer.getOrderPrice() * filledQty;
					cumQty += filledQty;
					remainingQty -= filledQty;
				}
			} else
				break; //The list is ordered so if the price does not match, no other will
		}
		//The price does not match, hence we get a partial fill, if we had any matches
		if(cumPrice > 0 && cumQty > 0) {
			return getPartialFill(inBid, bidPrice, cumPrice, cumQty, remainingQty);
		}
		
		return null;
	}	
	
	/**
	 * Checks the given offer for execution against the list of bids.
	 * The offer is executed according to the rules explained in the {@link CcfeaOrderExecutionStrategy}
	 * JavaDoc.
	 * @param inOffer Offer to check for execution
	 * @param bids List of bids to check for execution against
	 * @return A {@link Fill} or {@link PartialFill} if the order is filled or partially
	 * fill. If not NULL is returned
	 */
	public Fill checkOfferForExecution(Offer inOffer, OrderedMarketEventList<Bid> bids) {
		boolean isMarketOrder = inOffer.getOrderType() == OrdType.MARKET ? true : false;
		
		double offerPrice =	isMarketOrder ? Double.NEGATIVE_INFINITY : inOffer.getOrderPrice();
		
		int remainingQty = inOffer.getRemainingQty();
		double cumPrice = 0;
		int cumQty = 0;
		
		for(MarketEvent bid : bids) {
			//If it is a market order we execute at the bids price, else the 
			double execPrice = isMarketOrder ? bid.getOrderPrice() : offerPrice;
			//If the price of the offer matches (or is better)
			if(bid.getOrderPrice() >= offerPrice && !bid.getClientID().equals(inOffer.getClientID())) {
				//If the size of the bid is greater or equals
				if(bid.getRemainingQty() >= remainingQty) {
					//We can execute a complete fill
					//The price is always the price in the offer
					
					cumPrice += execPrice * remainingQty;
					cumQty += remainingQty;

					return getFill(inOffer, offerPrice, cumPrice, cumQty);
				} else {
					//The bid price matches, but the size does not
					//we need to look further in the order book
					cumPrice += execPrice * bid.getRemainingQty();
					cumQty += bid.getRemainingQty();
					remainingQty -= bid.getRemainingQty();
				}
			} else
				break; //The list is ordered so if the price does not match, no other will
		}
		
		//The price does not match, hence we get a partial fill, if we had any matches
		if(cumPrice > 0 && cumQty > 0) {
			return getPartialFill(inOffer, offerPrice, cumPrice, cumQty, remainingQty);
		}
		
		return null;
	}
	
	/**
	 * Get a {@link PartialFill} from the given market event and other parameters
	 * @param marketEvent The market event to convert to {@link PartialFill}
	 * @param price The execution price
	 * @param cumPrice The cumulative price of the execution
	 * @param cumQty The execution quantity
	 * @param remainingQty The quantity remaining to be executed 
	 * @return {@link PartialFill} with the given values
	 */
	private PartialFill getPartialFill(MarketEvent marketEvent, double price,
			double cumPrice, int cumQty, int remainingQty) {
		double executionPrice = cumPrice/cumQty;
		double avgPrice = getAvgPrice(marketEvent, cumPrice, cumQty);
		
		PartialFill partialFill = new PartialFill(price,
				marketEvent.getOrderQty(),
				marketEvent.getClientOrderID(),
				marketEvent.getTransactTime(),
				marketEvent.getClientID(),
				marketEvent.getOrderType(),
				marketEvent.getSymbol(),
				marketEvent.getAccount(),
				marketEvent.getCumQty() + cumQty,
				cumQty,
				executionPrice,
				remainingQty,
				marketEvent.getSide());
		
		partialFill.setAvgPrice(avgPrice);

		return partialFill;
	}
	
	/**
	 * Get a {@link Fill} from the given market event and other parameters
	 * @param marketEvent The market event to convert to {@link Fill}
	 * @param price The execution price
	 * @param cumPrice The cumulative price of the execution
	 * @param cumQty The execution quantity 
	 * @return {@link Fill} with the given values
	 */
	private Fill getFill(MarketEvent marketEvent, double price, double cumPrice, int cumQty) {
		double executionPrice = cumPrice/cumQty;
		double avgPrice = getAvgPrice(marketEvent, cumPrice, cumQty);
		
		Fill fill = new Fill(price,
				marketEvent.getOrderQty(),
				marketEvent.getClientOrderID(),
				marketEvent.getTransactTime(),
				marketEvent.getClientID(),
				marketEvent.getOrderType(),
				marketEvent.getSymbol(),
				marketEvent.getAccount(),
				marketEvent.getCumQty() + cumQty,
				cumQty,
				executionPrice,
				marketEvent.getSide());
		
		fill.setAvgPrice(avgPrice);

		return fill;
	}
	
	/**
	 * Calculates the average price for the given {@link MarketEvent}
	 * @param marketEvent {@link MarketEvent} to calculate average price for
	 * @param cumPrice The cumulative execution price for current execution
	 * @param cumQty The cumulative exection quantity for current execution
	 * @return The average price executed at.
	 */
	private double getAvgPrice(MarketEvent marketEvent, double cumPrice, int cumQty) {
		double currentAvgPrice = marketEvent.getAvgPrice();
		double currentCumQty = marketEvent.getCumQty();
		double avgPrice =
			(currentAvgPrice * currentCumQty + cumPrice) / (cumQty + currentCumQty);
		
		return avgPrice;
	}
}