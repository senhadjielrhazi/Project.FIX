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

import quickfix.field.Side;

/**
 * Bid market event
 * @see MarketEvent
 * 
 * @author <a href="mailto:catsbf@dasch.dk">Daniel Schiermer</a>
 * @version 0.1
 *
 */
public class Bid extends MarketEvent {
	
	/**
	 * @see MarketEvent#MarketEvent(double, int, int, String, Date, String, char, String, String)
	 */
	public Bid(double orderPrice, int orderQty, int remainingQty, String clientOrderID,
			Date transactTime, String clientID, char orderType, String symbol, String account) {
		super(orderPrice, orderQty, remainingQty, clientOrderID,
				transactTime, clientID, orderType, symbol, account);
		super.setSide(Side.BUY);
	}
	
	/**
	 * @see MarketEvent#MarketEvent(double, int, int, String, Date, String, char, String, String, Action)
	 */
	public Bid(double orderPrice, int orderQty, int remainingQty, String clientOrderID,
			Date transactTime, String clientID, char orderType, String symbol,
			String account, Action action) {
		super(orderPrice, orderQty, remainingQty, clientOrderID, transactTime,
				clientID, orderType, symbol, account, action);
		super.setSide(Side.BUY);
	}

	/**
	 * Creates a bid from the given market event
	 * @param marketEvent
	 */
	public Bid(MarketEvent marketEvent) {
		super(marketEvent.getOrderPrice(),
				marketEvent.getOrderQty(),
				marketEvent.getRemainingQty(),
				marketEvent.getClientOrderID(),
				marketEvent.getTransactTime(),
				marketEvent.getClientID(),
				marketEvent.getOrderType(),
				marketEvent.getSymbol(),
				marketEvent.getAccount(),
				marketEvent.getAction());
		super.setSide(Side.BUY);
	}

	@Override
	protected String getTypeAsString() {
		return "Buy";
	}
}