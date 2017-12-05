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

import java.sql.Date;
import java.text.DateFormat;

import org.marketcetera.exchange.common.Configuration;
import org.marketcetera.exchange.quickfix.FIXFactory;

/**
 * Market event describing an event happening in the market, such as
 * {@link Bid}, {@link Offer}, {@link PartialFill}, {@link Fill}.
 * It has a number of cumpolsory attributes, and a number of optional
 * attributes, primarily used for {@link PartialFill} and {@link Fill}.
 * 
 * @author <a href="mailto:catsbf@dasch.dk">Daniel Schiermer</a>
 * @version 0.1
 *
 */
public abstract class MarketEvent {
	public static enum Action { ADD, CHANGE, DELETE }

	private double orderPrice;
	private int orderQty;
	private int remainingQty;
	private String clientOrderID;
	private Date transactTime;
	private Action action;
	private char side;
	private final String clientID;
	private final String symbol;
	private final String account;

	/**
	 * The following fields are optional, they are used when the event is part of a partial fill
	 */
	private int cumQty = 0;
	private int execQty = 0;
	private double avgPrice = 0;
	private double execPrice = 0;
	private final char orderType;

	/**
	 * Create an instance of the event. Has the default Action = NEW.
	 * @param orderPrice Price of the order (market event)
	 * @param orderQty Quantity of the order
	 * @param remainingQty Remaining quantity of the order - equal to orderQuantity in the
	 * case of a {@link Bid} and an {@link Offer}, 0 for a {@link Fill} and equal to the amount
	 * remaining to be filled for a {@link PartialFill}. Any {@link Bid}s or {@link Offer}s that
	 * are part of a partial fill should have this value updated. 
	 * @param clientOrderID Unique id of the event
	 * @param transactTime Transaction time - time of transaction
	 * @param clientID Id of the client (or server) placing the order
	 * @param orderType Type of the order (either LIMIT or MARKET).
	 * @param symbol Symbol the order belongs to
	 * @param account The account from which the order was made.
	 */
	public MarketEvent(double orderPrice, int orderQty, int remainingQty, String clientOrderID,
			Date transactTime, String clientID, char orderType, String symbol,
			String account) {
		this.orderPrice = orderPrice;
		this.orderQty = orderQty;
		this.remainingQty = remainingQty;
		this.clientOrderID = clientOrderID;
		this.transactTime = transactTime;
		this.clientID = clientID;
		this.orderType = orderType;
		this.symbol = symbol;
		this.account = account;
		this.action = Action.ADD;
	}

	/**
	 * Create an instance of the event.
	 * @param orderPrice Price of the order (market event)
	 * @param orderQty Quantity of the order
	 * @param remainingQty Remaining quantity of the order - equal to orderQuantity in the
	 * case of a {@link Bid} and an {@link Offer}, 0 for a {@link Fill} and equal to the amount
	 * remaining to be filled for a {@link PartialFill}. Any {@link Bid}s or {@link Offer}s that
	 * are part of a partial fill should have this value updated. 
	 * @param clientOrderID Unique id of the event
	 * @param transactTime Transaction time - time of transaction
	 * @param clientID Id of the client (or server) placing the order
	 * @param orderType Type of the order (either LIMIT or MARKET).
	 * @param symbol Symbol the order belongs to
	 * @param account The account from which the order was made.
	 * @param action Action of the event (ADD, CHANGE, DELETE)
	 */
	public MarketEvent(double orderPrice, int orderQty, int remainingQty, String clientOrderID,
			Date transactTime, String clientID, char orderType, String symbol,
			String account, Action action) {
		this(orderPrice, orderQty, remainingQty, clientOrderID, transactTime,
				clientID, orderType, symbol, account);
		this.action = action;
	}

	/**
	 * Indicates what type the trade is according to the FIX specification
	 * @return The order type according to the FIX spec.
	 */
	public char getOrderType() {
		return orderType;
	}

	/**
	 * Char indicating the FIX value of the side of the market event.
	 * @return Char indicating the side according to the FIX definition.
	 */
	public char getSide() {
		return side;
	}

	/**
	 * Set the order quantity
	 * @param orderQty New order qty value
	 */
	public void setOrderQty(int orderQty) {
		this.orderQty = orderQty;
	}

	/**
	 * Gets the action
	 * @return action of the event
	 */
	public Action getAction() {
		return action;
	}

	/**
	 * Sets the action
	 * @param action Action of the event
	 */
	public void setAction(Action action) {
		this.action = action;
	}

	/**
	 * Gets the order price
	 * @return The order price
	 */
	public double getOrderPrice() {
		return orderPrice;
	}

	/**
	 * Sets the order price
	 * @param orderPrice Price of the order
	 */
	public void setOrderPrice(double orderPrice) {
		this.orderPrice = orderPrice;
	}

	/**
	 * Gets the client id
	 * @return The client id
	 */
	public String getClientID() {
		return clientID;
	}

	/**
	 * Gets the order quantity
	 * @return The order quantity
	 */
	public int getOrderQty() {
		return orderQty;
	}

	/**
	 * Gets the client order id
	 * @return The client order id
	 */
	public String getClientOrderID() {
		return clientOrderID;
	}

	/**
	 * Gets the transaction time
	 * @return transaction time
	 */
	public Date getTransactTime() {
		return transactTime;
	}

	/**
	 * Get the symbol in upper case
	 * @return The symbol in upper case
	 */
	public String getSymbol() {
		return symbol.toUpperCase();
	}

	/**
	 * Gets the account
	 * @return The account
	 */
	public String getAccount() {
		return account;
	}

	/**
	 * Gets the cumulative quantity
	 * @return Cumulative quantity
	 */
	public int getCumQty() {
		return cumQty;
	}

	/**
	 * Sets the cumulative quantity
	 * @param cumQty New value of the cumulative quantity
	 */
	public void setCumQty(int cumQty) {
		this.cumQty = cumQty;
	}

	/**
	 * Adds the given amount to the cumulative quantity
	 * @param addedCumQty Amount to add to the cumulative quantity
	 */
	public void addToCumQty(int addedCumQty) {
		this.cumQty += addedCumQty;
	}

	/**
	 * Gets the average price
	 * @return The average price
	 */
	public double getAvgPrice() {
		return avgPrice;
	}

	/**
	 * Sets the average price
	 * @param avgPrice New average price value
	 */
	public void setAvgPrice(double avgPrice) {
		this.avgPrice = avgPrice;
	}

	/**
	 * Gets the execution price
	 * @return The execution price
	 */
	public double getExecPrice() {
		return execPrice;
	}

	/**
	 * Sets the execution price to the given value
	 * @param execPrice New execution price value
	 */
	public void setExecPrice(double execPrice) {
		this.execPrice = execPrice;
	}

	/**
	 * Set the side to the given value
	 * @param side New side value
	 */
	public void setSide(char side) {
		this.side = side;
	}

	/**
	 * Get the execution quantity
	 * @return Execution quantity
	 */
	public int getExecQty() {
		return execQty;
	}

	/**
	 * Set the execution quantity to the new value
	 * @param execQty New execution quantity value
	 */
	public void setExecQty(int execQty) {
		this.execQty = execQty;
	}

	/**
	 * Gets the remaining quantity
	 * @return The remaining quantity
	 */
	public int getRemainingQty() {
		return remainingQty;
	}

	/**
	 * Sets the remaining quantity
	 * @param remainingQty New remaining quantity
	 */
	public void setRemainingQty(int remainingQty) {
		this.remainingQty = remainingQty;
	}

	/**
	 * Subtract the given amount from the remaining quantity
	 * @param subtractQty Amout to subtract from remaining quantity
	 */
	public void subtractFromRemainingQty(int subtractQty) {
		this.remainingQty -= subtractQty;
	}

	/**
	 * Get the market event type as string
	 * @return The market event type as string
	 */
	protected abstract String getTypeAsString();

	/**
	 * Prints all fields in a readable way
	 */
	@Override
	public String toString() {
		DateFormat dateFormat = Configuration.getInstance().getPrintDateFormat();
		return "Type: " + getTypeAsString()
		+ "Transaction Time: " + dateFormat.format(transactTime)
		+ ", Client Order ID: " + clientOrderID
		+ ", Symbol: " + symbol 
		+ ", Side: " + FIXFactory.getSideAsString(side)
		+ ", Order Quantity: " + orderQty
		+ ", Cumulative Quantity: " + cumQty
		+ ", Order Price: " + orderPrice
		+ ", Average Price: " + avgPrice
		+ ", Execution Price: " + execPrice
		+ ", Account: " + account
		+ ", Client ID: " + clientID;
	}
}