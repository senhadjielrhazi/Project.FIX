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
package org.marketcetera.exchange.report;

import java.text.DateFormat;
import java.util.Date;

import org.marketcetera.exchange.common.CcfeaFileWriter;
import org.marketcetera.exchange.events.ClientCancelEvent;
import org.marketcetera.exchange.events.ClientFilledEvent;
import org.marketcetera.exchange.events.ClientNewPositionEvent;

/**
 * Report implementation that logs all trades made by the client.
 *  
 * @author <a href="mailto:catsbf@dasch.dk">Daniel Schiermer</a>
 * @version 0.1
 * 
 * @see Report
 */
public class CcfeaTradeLogReport extends Report {
	private DateFormat dateFormat;
	private CcfeaFileWriter ccfeaFileWriter;

	/**
	 * Creates a report placed at the given file path with the given base name.
	 * The given date format is used to print dates.
	 * @param filePath File path for the report
	 * @param fileBaseName Basename for the report, will get a suffix and
	 * an teh given file extension
	 * @param fileExtension File extension for the report
	 * @param dateFormat Formatter used to print dates to the report
	 */
	public CcfeaTradeLogReport(String filePath, String fileBaseName, String fileExtension,
			DateFormat dateFormat) {
		super(filePath, fileBaseName, fileExtension);
		ccfeaFileWriter = new CcfeaFileWriter(filePath, fileBaseName, fileExtension);
		this.dateFormat = dateFormat;
	}

	/**
	 * Add a line to the report
	 * @param lineToAdd The line to add
	 */
	public void addLine(String lineToAdd) {
		ccfeaFileWriter.addLine(lineToAdd);
	}

	/**
	 * Stops reporting, adds a line stating the date and time, and then closes the file.
	 */
	@Override
	public void stop() {
		addLine("Backtest ended @ " + dateFormat.format(new Date()));
		ccfeaFileWriter.close();
	}

	/**
	 * Starts reporting. Adds a line stating the date and time
	 */
	@Override
	public void start() {
		//Add the start date and time for the back test
		addLine("Backtest started @ " + dateFormat.format(new Date()));
	}

	/**
	 * @see Report#onClientFilledEvent(ClientFilledEvent)
	 */
	@Override
	public void onClientFilledEvent(ClientFilledEvent clientFilledEvent) {
		addLine("Filled Position: \n" + clientFilledEvent.getMarketEvent().toString());
	}
	
	/**
	 * @see Report#onClientCancelPositionEvent(ClientCancelEvent)
	 */
	@Override
	public void onClientCancelPositionEvent(ClientCancelEvent clientCancelEvent) {
		addLine("Cancelled Position: \n" + clientCancelEvent.getMarketEvent().toString());
	}
	
	/**
	 * @see Report#onClientNewPositionEvent(ClientNewPositionEvent)
	 */
	@Override
	public void onClientNewPositionEvent(ClientNewPositionEvent clientNewPositionEvent) {
		addLine("New Position: \n" + clientNewPositionEvent.getMarketEvent().toString());
	}
}