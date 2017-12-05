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
package org.marketcetera.exchange.marketDataServer;

import java.util.Observable;
import java.util.Observer;

import quickfix.SessionID;
import quickfix.field.MDEntryType;
import quickfix.field.MDReqID;
import quickfix.field.Symbol;
import quickfix.fix44.MarketDataIncrementalRefresh;
import org.marketcetera.exchange.backtesting.OrderBook;
import org.marketcetera.exchange.marketEvents.Bid;
import org.marketcetera.exchange.marketEvents.Fill;
import org.marketcetera.exchange.marketEvents.Offer;
import org.marketcetera.exchange.marketEvents.PartialFill;
import org.marketcetera.exchange.quickfix.FIXFactory;
import org.marketcetera.exchange.quickfix.FIXMessageManager;

/**
 * Market Data Reporter that observers the order book and reports all
 * new market data (changes in the order book) to the connected
 * client on the given session.
 * 
 * @author <a href="mailto:catsbf@dasch.dk">Daniel Schiermer</a>
 * @version 0.1
 *
 */
public class MarketDataReporter implements Observer {
	private SessionID sessionID;
	private Symbol symbol;
	private MDReqID mdReqID;

	/**
	 * Creates a reporter instance with for the given session,
	 * with the given symbol and Market Data Request ID
	 * @param sessionID ID of the session to report to
	 * @param symbol Symbol to report market data about
	 * @param mdReqID Market Data Request ID
	 */
	public MarketDataReporter(SessionID sessionID, Symbol symbol, MDReqID mdReqID) {
		this.sessionID = sessionID;
		this.mdReqID = mdReqID;
		this.symbol = symbol;

		OrderBook.getInstance().addObserver(this);
	}

	/**
	 * Handle the events coming from the order book
	 */
	@Override
	public void update(Observable arg0, Object arg1) {
		if(arg1 instanceof Bid) {
			onBidEvent((Bid)arg1);
		} else if (arg1 instanceof Offer) {
			onOfferEvent((Offer)arg1);
		} else if (arg1 instanceof PartialFill || arg1 instanceof Fill) {
			onFillEvent((Fill)arg1);
		}
	}

	/**
	 * Reports a bid event
	 * @param bid Bid event to report
	 */
	private void onBidEvent(Bid bid) {
		//Set the type indicator to 0, according to FIX Bid type
		sendMessage(FIXFactory.newMarketEventToIncrementalRefresh(bid, symbol, mdReqID, MDEntryType.BID));
	}

	/**
	 * Reports an offer event
	 * @param offer Offer event to report
	 */
	private void onOfferEvent(Offer offer) {
		//Set the type indicator to 1, according to FIX Offer type
		sendMessage(FIXFactory.newMarketEventToIncrementalRefresh(offer, symbol, mdReqID, MDEntryType.OFFER));
	}

	/**
	 * Reports a fill (or partial fill)
	 * @param fill Fill (or partial fill) to report
	 */
	private void onFillEvent(Fill fill) {
		//Set the type indicator to 2, according to FIX Trade type
		sendMessage(FIXFactory.newMarketEventToIncrementalRefresh(fill, symbol, mdReqID, MDEntryType.TRADE));
	}

	/**
	 * Sends the {@link MarketDataIncrementalRefresh} message to the client.
	 * @param incrementalRefresh {@link MarketDataIncrementalRefresh} to send.
	 */
	private void sendMessage(MarketDataIncrementalRefresh incrementalRefresh) {
		FIXMessageManager.sendMessage(sessionID, incrementalRefresh);
	}
}