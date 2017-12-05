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
package org.marketcetera.exchange.orderServer;

import java.util.Observable;

import java.util.Observer;

import quickfix.SessionID;
import quickfix.field.ExecTransType;
import quickfix.field.ExecType;
import quickfix.field.OrdRejReason;
import quickfix.field.OrdStatus;
import quickfix.fix44.ExecutionReport;
import quickfix.fix44.Message;
import org.marketcetera.exchange.backtesting.ClientPortfolio;
import org.marketcetera.exchange.events.ClientMarketEvent;
import org.marketcetera.exchange.marketEvents.Bid;
import org.marketcetera.exchange.marketEvents.Fill;
import org.marketcetera.exchange.marketEvents.MarketEvent;
import org.marketcetera.exchange.marketEvents.Offer;
import org.marketcetera.exchange.marketEvents.PartialFill;
import org.marketcetera.exchange.quickfix.FIXFactory;
import org.marketcetera.exchange.quickfix.FIXMessageManager;

/**
 * Execution Report Reporter that observers the client portfolio and reports all
 * executions (or additions of new orders) to the connected client on the given
 * session.
 * 
 * @author <a href="mailto:catsbf@dasch.dk">Daniel Schiermer</a>
 * @version 0.1
 *
 */
public class OrderReporter implements Observer {
	private SessionID sessionID;

	/**
	 * Creates an order reporter instance
	 * @param sessionID Session id
	 * @param clientPortfolio The client portfolio to observer
	 */
	public OrderReporter(SessionID sessionID, ClientPortfolio clientPortfolio) {
		this.sessionID = sessionID;
		clientPortfolio.addObserver(this);
	}

	/**
	 * Handles all events fired by the client portfolio
	 */
	@Override
	public void update(Observable observable, Object event) {
		MarketEvent marketEvent = ((ClientMarketEvent)event).getMarketEvent();
		
		if(marketEvent instanceof Bid) {
			onBid((Bid)marketEvent);
		} else if (marketEvent instanceof Offer) {
			onOffer((Offer)marketEvent);
		} else if (marketEvent instanceof PartialFill) {
			onPartialFill((PartialFill)marketEvent);
		} else if (marketEvent instanceof Fill) {
			onFill((Fill)marketEvent);
		}
	}

	/**
	 * Handles fill events
	 * @param fill
	 */
	private void onFill(Fill fill) {
		char orderStatus = OrdStatus.FILLED;
		sendMessage(fill, orderStatus);
	}

	/**
	 * Handles partial fill events
	 * @param partialFill
	 */
	private void onPartialFill(PartialFill partialFill) {
		char orderStatus = OrdStatus.PARTIALLY_FILLED;
		sendMessage(partialFill, orderStatus);
	}

	/**
	 * Handles bid events
	 * @param bid
	 */
	private void onBid(Bid bid) {
		char orderStatus = OrdStatus.NEW;
		sendMessage(bid, orderStatus);
	}

	/**
	 * Handles offer events
	 * @param offer
	 */
	private void onOffer(Offer offer) {
		char orderStatus = OrdStatus.NEW;
		sendMessage(offer, orderStatus);
	}

	private void sendMessage(MarketEvent marketEvent, char orderStatus) {
		char execTransType = ExecTransType.NEW;
		char execType = ExecType.NEW;
		
		ExecutionReport executionReport = FIXFactory.newExecutionReport(
				marketEvent.getClientOrderID(),
				marketEvent.getSymbol(),
				marketEvent.getSide(),
				marketEvent.getOrderQty(),
				marketEvent.getCumQty(),
				marketEvent.getExecQty(),
				marketEvent.getOrderPrice(),
				marketEvent.getAvgPrice(),
				marketEvent.getExecPrice(),
				marketEvent.getOrderType(),
				orderStatus,
				marketEvent.getRemainingQty(),
				marketEvent.getAccount(),
				execTransType, execType);

		FIXMessageManager.sendMessage(sessionID, executionReport);
	}
	
	public void sendMessage(Message message) {
		FIXMessageManager.sendMessage(sessionID, message);
	}
	
	public void sendRejectMessage(MarketEvent marketEvent, int ordRejReason) {
		ExecutionReport executionReport =
			FIXFactory.newExecutionReport(marketEvent,
					marketEvent.getOrderQty(), 0, OrdStatus.REJECTED, ExecTransType.NEW, ExecType.REJECTED);

		executionReport.set(new OrdRejReason(ordRejReason));
		FIXMessageManager.sendMessage(sessionID, executionReport);
	}
}