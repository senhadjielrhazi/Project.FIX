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
package org.marketcetera.exchange.quickfix;

import java.sql.Date;

import quickfix.field.Account;
import quickfix.field.AvgPx;
import quickfix.field.ClOrdID;
import quickfix.field.CumQty;
import quickfix.field.ExecID;
import quickfix.field.ExecType;
import quickfix.field.LastPx;
import quickfix.field.LastQty;
import quickfix.field.LeavesQty;
import quickfix.field.MDEntryDate;
import quickfix.field.MDEntryPx;
import quickfix.field.MDEntryRefID;
import quickfix.field.MDEntrySize;
import quickfix.field.MDEntryTime;
import quickfix.field.MDEntryType;
import quickfix.field.MDReqID;
import quickfix.field.MDUpdateAction;
import quickfix.field.OrdStatus;
import quickfix.field.OrdType;
import quickfix.field.OrderID;
import quickfix.field.OrderQty;
import quickfix.field.Price;
import quickfix.field.Side;
import quickfix.field.Symbol;
import quickfix.field.TransactTime;
import quickfix.fix44.ExecutionReport;
import quickfix.fix44.MarketDataIncrementalRefresh;
import org.marketcetera.exchange.backtesting.SimulationTime;
import org.marketcetera.exchange.common.UUIDGenerator;
import org.marketcetera.exchange.marketEvents.Fill;
import org.marketcetera.exchange.marketEvents.MarketEvent;
import org.marketcetera.exchange.marketEvents.PartialFill;

/**
 * Factory to create Quickfixj messages from market events, or a list of parameters.
 * 
 * @author <a href="mailto:catsbf@dasch.dk">Daniel Schiermer</a>
 * @version 0.1
 *
 */
public class FIXFactory {

	/**
	 * Creates a new {@link MarketDataIncrementalRefresh} from the values of the given
	 * market event and the remaining parameters.
	 * @param marketEvent To parse to {@link MarketDataIncrementalRefresh} 
	 * @param symbol Symbol the execution report is for
	 * @param mdReqID Market data request id
	 * @param typeIndicator Type indicator
	 * @return New {@link MarketDataIncrementalRefresh} from the given parameters.
	 * 
	 * @see MarketDataIncrementalRefresh
	 */
	public static MarketDataIncrementalRefresh newMarketEventToIncrementalRefresh(MarketEvent marketEvent,
			Symbol symbol, MDReqID mdReqID, char typeIndicator) {
		//The ordinal matches the correct MDUpdateAction
		char updateAction = Character.forDigit(marketEvent.getAction().ordinal(), 10);
		double price = marketEvent.getOrderPrice();
		int orderQty = marketEvent.getOrderQty();  
		
		if(marketEvent instanceof Fill || marketEvent instanceof PartialFill) {
			price = marketEvent.getExecPrice();
			orderQty = marketEvent.getExecQty();
		}
		
		return generateIncrementalRefresh(
				mdReqID, updateAction, typeIndicator, price,
				orderQty, marketEvent.getTransactTime(),
				symbol, marketEvent.getClientOrderID());
	}

	/**
	 * Generates a {@link MarketDataIncrementalRefresh} from the given parameters
	 * @param mdReqID Market data request id
	 * @param updateAction Update action
	 * @param entryType Entry type
	 * @param price Price
	 * @param tradeSize Trade size (order quantity)
	 * @param transactTime Transaction time
	 * @param symbol Symbol
	 * @param entryRefID Entry reference ID, that the execution report is for
	 * @return New {@link MarketDataIncrementalRefresh}
	 * 
	 * @see MarketDataIncrementalRefresh
	 */
	private static MarketDataIncrementalRefresh generateIncrementalRefresh(
			MDReqID mdReqID, char updateAction, char entryType,
			double price, double tradeSize, Date transactTime,
			Symbol symbol, String entryRefID) {

		MarketDataIncrementalRefresh incrementalRefresh = new MarketDataIncrementalRefresh();
		incrementalRefresh.set(mdReqID);

		MarketDataIncrementalRefresh.NoMDEntries group =
			new MarketDataIncrementalRefresh.NoMDEntries();

		group.set(new MDUpdateAction(updateAction));
		group.set(new MDEntryType(entryType));
		
		group.set(new MDEntryPx(price));		
		group.set(new MDEntrySize(tradeSize));
		group.set(new MDEntryDate(transactTime));
		group.set(new MDEntryTime(transactTime));
		group.set(symbol);
		group.set(new MDEntryRefID(entryRefID));
		incrementalRefresh.addGroup(group);

		return incrementalRefresh;
	}
	
	/**
	 * Creates a new {@link ExecutionReport} for the given market event and remaining
	 * parameters
	 * @param marketEvent Market event
	 * @param remainingQty Remaining quantity
	 * @param execQty Execution quantity
	 * @param orderStatus Order status
	 * @param execTransType Execution transaction type
	 * @param execType Execution type
	 * @return new {@link ExecutionReport}
	 * 
	 * @see ExecutionReport
	 */
	public static ExecutionReport newExecutionReport(MarketEvent marketEvent,
			int remainingQty, int execQty, char orderStatus, char execTransType, char execType)
	{
		char side = marketEvent.getSide();

		return newExecutionReport(marketEvent.getClientOrderID(), marketEvent.getSymbol(),
				side, marketEvent.getOrderQty(),
				marketEvent.getCumQty(), execQty, marketEvent.getOrderPrice(),
				marketEvent.getAvgPrice(), marketEvent.getExecPrice(),
				marketEvent.getOrderType(), orderStatus,
				remainingQty, marketEvent.getAccount(),
				execTransType, execType);
	}

	/**
	 * Get a new {@link ExecutionReport} from the given parameters
	 * @param clOrdID Client order id
	 * @param symbol Symbol
	 * @param side Side
	 * @param orderQty Order quantity 
	 * @param cumQty Cumulative Quantity
	 * @param execQty Execution Quantity
	 * @param price Price
	 * @param avgPrice Average price
	 * @param executionPrice Execution Price
	 * @param orderType Order type
	 * @param orderStatus Order status
	 * @param remainingQty Remaining quantity
	 * @param account Account
	 * @param execTransType Execution transaction type
	 * @param execType Execution type
	 * @return New {@link ExecutionReport}
	 * 
	 * @see ExecutionReport
	 */
	public static ExecutionReport newExecutionReport(String clOrdID,
			String symbol, char side, int orderQty, int cumQty, int execQty, double price,
			double avgPrice, double executionPrice, char orderType,
			char orderStatus, double remainingQty, String account,
			char execTransType, char execType)
	{
		ExecutionReport executionReport =
			new ExecutionReport(new OrderID(UUIDGenerator.getUUIDString()),
					new ExecID(UUIDGenerator.getUUIDString()),
					new ExecType(execType),
					new OrdStatus(orderStatus),
					new Side(side),
					new LeavesQty(remainingQty),
					new CumQty(cumQty),
					new AvgPx(avgPrice));

		executionReport.set(new Symbol(symbol)); //Set the symbol
		executionReport.set(new ClOrdID(clOrdID)); //Set the client order id
		executionReport.set(new OrderQty(orderQty));
		executionReport.set(new OrdType(orderType));// 1 = Market, 2 = Limit (the only ones this Exchange support)
		executionReport.set( //Transaction time
				new TransactTime(SimulationTime.getInstance().getTime(new Date(System.currentTimeMillis()))));
		//Execution price, should be the same as AvgPx for complete fills,
		//should be updated on consecutive fill/partial fills
		executionReport.set(new LastPx(executionPrice));
		executionReport.set(new LastQty(execQty)); //Quantity of shares bought/sold on this (last) fill.

		//The price offered in the order, will be 0 or infinite for market orders
		if(!Double.isInfinite(price))
			executionReport.set(new Price(price));
		else
			executionReport.set(new Price(0.0));

		if(account != null)
			executionReport.set(new Account(account));

		return executionReport;
	}
	
	/**
	 * Get the side as string
	 * @param side Side as char
	 * @return Side as string
	 */
	public static String getSideAsString(char side) {
		switch (side) {
		case Side.BUY:
			return "BUY";
		case Side.SELL:
			return "SELL";
		case Side.SELL_SHORT:
			return "SELL_SHORT";
		default:
			return "N/A";
		}
	}
}