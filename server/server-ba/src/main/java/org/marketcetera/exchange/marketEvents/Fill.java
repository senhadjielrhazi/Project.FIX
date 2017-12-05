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

/**
 * Fill market event.
 * Parent class for the {@link PartialFill}.
 * Has a few more compulsory attributes compared to the general {@link MarketEvent}.
 * 
 * @author <a href="mailto:catsbf@dasch.dk">Daniel Schiermer</a>
 * @version 0.1
 *
 * @see MarketEvent
 * 
 */
public class Fill extends MarketEvent {
	
	/**
	 * Create an instance of the event. Has the default Action = NEW.
	 * @param orderPrice Price of the order (market event)
	 * @param orderQty Quantity of the order
	 * @param clientOrderID Unique id of the event
	 * @param transactTime Transaction time - time of transaction
	 * @param clientID Id of the client (or server) placing the order
	 * @param orderType Type of the order (either LIMIT or MARKET).
	 * @param symbol Symbol the order belongs to
	 * @param account The account from which the order was made.
	 * @param cumQty Cumulative quantity of the fill, should be the same as order quantity
	 * for Fill and the amount filled so far for PartialFil
	 * @param execQty The execution quantity, should be the same as order quantity
	 * for Fill and the amount filled in the current partial fill for PartialFil
	 * @param execPrice The last execution price
	 * @param side The side of the order that was filled
	 */
	public Fill(double orderPrice, int orderQty, String clientOrderID,
			Date transactTime, String clientID, char orderType,
			String symbol, String account, int cumQty, int execQty, double execPrice,
			char side) {
		//remainingQty = 0 due to the fill
		super(orderPrice, orderQty, 0, clientOrderID, transactTime, clientID, orderType,
				symbol, account);
		
		super.setCumQty(cumQty);
		super.setExecQty(execQty);
		super.setExecPrice(execPrice);
		super.setSide(side);
	}

	@Override
	protected String getTypeAsString() {
		return "Fill";
	}
}