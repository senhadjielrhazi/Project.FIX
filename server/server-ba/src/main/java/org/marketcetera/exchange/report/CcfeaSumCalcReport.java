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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import quickfix.field.Side;

import org.marketcetera.exchange.common.CcfeaFileWriter;
import org.marketcetera.exchange.events.ClientCancelEvent;
import org.marketcetera.exchange.events.ClientFilledEvent;
import org.marketcetera.exchange.events.ClientNewPositionEvent;
import org.marketcetera.exchange.marketEvents.Fill;

/**
 * Report implementation that calculates:
 * - Total price of bought assets
 * - Total price of sold assets
 * - Total amount of bought assets
 * - Total amount of sold assets
 * - Average buy price
 * - Average sell price
 * - Total amount of trades
 * - Total profit (loss)
 * 
 * For client actions.
 * 
 * @author <a href="mailto:catsbf@dasch.dk">Daniel Schiermer</a>
 * @version 0.1
 * 
 * @see Report
 */
public class CcfeaSumCalcReport extends Report {
	private final DateFormat dateFormat;
	private CcfeaFileWriter ccfeaFileWriter;
	private List<Fill> fills = new ArrayList<Fill>();
	private double totalBoughtAmount;
	private double totalSoldAmount;
	private double totalBoughtPrice;
	private double totalSoldPrice;

	/**
	 * Creates a report placed at the given file path with the given base name.
	 * The given date format is used to print dates.
	 * @param filePath File path for the report
	 * @param fileBaseName Basename for the report, will get a suffix and
	 * an the given file extension
	 * @param fileExtension File extension for the report
	 * @param dateFormat Formatter used to print dates to the report
	 */
	public CcfeaSumCalcReport(String filePath, String fileBaseName, String fileExtension,
			DateFormat dateFormat) {
		super(filePath, fileBaseName, fileExtension);
		
		ccfeaFileWriter = new CcfeaFileWriter(filePath, fileBaseName, fileExtension);
		
		this.dateFormat = dateFormat;
	}
	
	/**
	 * Starts reporting. Adds a line stating the date and time
	 */
	@Override
	public void start() {
		 ccfeaFileWriter.addLine("Backtest started @ " + dateFormat.format(new Date()));
	}

	/**
	 * Calculates all the summarized data and adds it to the report.
	 * Adds a line stating the date and time the reporting was ended.
	 */
	@Override
	public void stop() {
		ccfeaFileWriter.addLine("\n++++++++++++++++++++++++");
		ccfeaFileWriter.addLine("Total price bought = " + totalBoughtPrice);
		ccfeaFileWriter.addLine("Total amount bought = " + totalBoughtAmount);
		ccfeaFileWriter.addLine("Average buy price = " + totalBoughtPrice / totalBoughtAmount);
		ccfeaFileWriter.addLine("++++++++++++++++++++++++\n");

		ccfeaFileWriter.addLine("++++++++++++++++++++++++");
		ccfeaFileWriter.addLine("Total price sold = " + totalSoldPrice);
		ccfeaFileWriter.addLine("Total amount sold = " + totalSoldAmount);
		ccfeaFileWriter.addLine("Average sell price = " + totalSoldPrice / totalSoldAmount);
		ccfeaFileWriter.addLine("++++++++++++++++++++++++\n");
		
		ccfeaFileWriter.addLine("Total amount of trades = "
				+ (totalBoughtAmount + totalSoldAmount) + "\n");
		
		ccfeaFileWriter.addLine("Total profit (loss) - based on filled orders = "
				+ (totalSoldPrice - totalBoughtPrice) + "\n");
				
		ccfeaFileWriter.addLine("Backtest ended @ " + dateFormat.format(new Date()));
		
		ccfeaFileWriter.close();
	}
	
	/**
	 * Updates the summarized values on new fill event
	 * @see Report#onClientFilledEvent(ClientFilledEvent)
	 */
	@Override
	public void onClientFilledEvent(ClientFilledEvent clientFilledEvent) {
		Fill fill = (Fill)clientFilledEvent.getMarketEvent();
		
		if(fill.getSide() == Side.BUY) {
			totalBoughtPrice += fill.getExecPrice() * fill.getExecQty();
			totalBoughtAmount +=fill.getExecQty();
		}
		else {
			totalSoldPrice += fill.getExecPrice() * fill.getExecQty();
			totalSoldAmount += fill.getExecQty();
		}
			fills.add(fill);
	}

	/**
	 * Do nothing on {@link ClientCancelEvent}, this report only looks at fills
	 */
	@Override
	public void onClientCancelPositionEvent(ClientCancelEvent clientCancelEvent) {		
	}
	
	/**
	 * Do nothing on {@link ClientNewPositionEvent}, this report only looks at fills
	 */
	@Override
	public void onClientNewPositionEvent(ClientNewPositionEvent clientNewPositionEvent) {
	}
}