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

import java.sql.Date;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

import quickfix.field.Side;
import org.marketcetera.exchange.backtesting.OrderBook;
import org.marketcetera.exchange.common.CcfeaFileWriter;
import org.marketcetera.exchange.events.ClientCancelEvent;
import org.marketcetera.exchange.events.ClientFilledEvent;
import org.marketcetera.exchange.events.ClientNewPositionEvent;
import org.marketcetera.exchange.events.SimulationStartedEvent;
import org.marketcetera.exchange.marketEvents.Fill;
import org.marketcetera.exchange.marketEvents.PartialFill;

/**
 * Report created specifically for the sample MACrossingStrategy to calculate a number
 * of statistics.
 * 
 * @author <a href="mailto:catsbf@dasch.dk">Daniel Schiermer</a>
 * @version 0.1
 *
 */
public class CcfeaSampleMAStrategyReport extends Report {
	private SummaryStatistics returnsSumStats;
	private CcfeaFileWriter reportFileWriter, csvFileWriter, priceFileWriter;
	private DateFormat dateFormat;
	private boolean isLatestTradeBuy = false;
	private double latestBuyPrice = 0, latestReturnPct = 0, latestReturn = 0;
	private double cumReturnPct = 0, cumReturn = 0;
	private List<Double> cumReturns;

	public CcfeaSampleMAStrategyReport(String reportPath, String reportBaseName,
			String reportExtension, String csvFileBaseName, DateFormat dateFormat) {
		super(reportPath, reportBaseName, reportExtension);

		reportFileWriter = new CcfeaFileWriter(reportPath, reportBaseName, reportExtension);

		csvFileWriter = new CcfeaFileWriter(reportPath, csvFileBaseName, "csv");
		priceFileWriter = new CcfeaFileWriter(reportPath, "priceLog", "csv");

		this.dateFormat = dateFormat;
		this.cumReturns = new ArrayList<Double>();

		//Container to calculate statistics for the returns
		returnsSumStats = new SummaryStatistics();
		
		OrderBook.getInstance().addObserver(this);
	}

	/**
	 * Starts reporting. Adds a line stating the date and time
	 */
	@Override
	public void start() {
		reportFileWriter.addLine("Backtest started @ "
				+ dateFormat.format(new Date(System.currentTimeMillis())));
	}

	/**
	 * 
	 * Adds the statistics to the report.
	 */
	@Override
	public void stop() {
		reportFileWriter.addLine("####################################");

		reportFileWriter.addLine("Return Distribution Statistics:");
		reportFileWriter.addLine("Max return: " + Double.toString(returnsSumStats.getMax()));
		reportFileWriter.addLine("Min return: " + Double.toString(returnsSumStats.getMin()));
		reportFileWriter.addLine("Mean of returns: " + Double.toString(returnsSumStats.getMean()));
		reportFileWriter.addLine("Standard deviation of returns: " + Double.toString(returnsSumStats.getStandardDeviation()));
		reportFileWriter.addLine("Variance of returns: " + Double.toString(returnsSumStats.getVariance()));
		reportFileWriter.addLine("Sum of returns: " + Double.toString(returnsSumStats.getSum()));

		reportFileWriter.addLine("####################################");

		applyMaxDrawDown();

		reportFileWriter.addLine("Backtest ended @ "
				+ dateFormat.format(new Date(System.currentTimeMillis())));

		reportFileWriter.close();
		csvFileWriter.close();
		priceFileWriter.close();
	}
	
	/**
	 * Override the update method because we also observe the order book.
	 */
	@Override
	public void update(Observable o, Object arg) {
		if (arg instanceof SimulationStartedEvent) {
			start();
		} else if (arg instanceof ClientFilledEvent) {
			onClientFilledEvent((ClientFilledEvent)arg);
		} else if (arg instanceof Fill || arg instanceof PartialFill) {
			onTrade((Fill)arg);
		}
	}

	/**
	 * Observe the orderbook and generate a csv file with all the price information
	 * @param fill
	 */
	private void onTrade(Fill fill) {
		priceFileWriter.addLine(Double.toString(fill.getExecPrice()));
	}

	/**
	 * Calculate the maximum drawdown and its duration and add it to the report.
	 */
	private void applyMaxDrawDown() {
		double mdd = 0;
		double peak = Double.NEGATIVE_INFINITY;
		double ldd = Double.NEGATIVE_INFINITY;
		int lastMaxIndex = 0;
		int ddStart = 0;
		int ddEnd = 0;

		for(int i = 0; i < cumReturns.size(); i++) {
			double tempReturn = cumReturns.get(i);

			if(tempReturn > peak) {
				peak = tempReturn;
				lastMaxIndex = i + 1;
			} else {
				ldd = 100.0 * (peak - tempReturn) / peak;

				if(ldd > mdd) {
					mdd = ldd;
					ddStart = lastMaxIndex;
					ddEnd = i + 1;
				}
			}
		}

		reportFileWriter.addLine("Max Draw Down Calculations");
		reportFileWriter.addLine("Max Draw Down (in percent): " + mdd);
		reportFileWriter.addLine("Max Draw Down Start Index: " + ddStart);
		reportFileWriter.addLine("Max Draw Down End Index: " + ddEnd);
		reportFileWriter.addLine("Max Draw Down Duration (no of trades): " + (ddEnd - ddStart));
	}
	

	/**
	 * Update the calculates on fills. Assumes that buy and sells are done in turn.
	 * Logs all information as a CSV fil.
	 */
	@Override
	public void onClientFilledEvent(ClientFilledEvent clientFilledEvent) {
		Fill fill = (Fill)clientFilledEvent.getMarketEvent();

		double execPrice = fill.getExecPrice();

		if(fill.getSide() == Side.BUY) {
			isLatestTradeBuy = true;
			latestBuyPrice = execPrice;
		} else if(fill.getSide() == Side.SELL) {
			//We had a buy trade and now a sell trade -> a profit/loss
			if(isLatestTradeBuy) {
				//calculate the return as a percentage of the initial investment
				latestReturn = execPrice - latestBuyPrice;
				double returnPercent = latestReturn / latestBuyPrice;
				//add the profit/loss to the statistics
				returnsSumStats.addValue(returnPercent);

				latestReturnPct = returnPercent;
				cumReturn += latestReturn;
				cumReturnPct += latestReturnPct;

				csvFileWriter.addLine(Double.toString(cumReturnPct) + "," + Double.toString(cumReturn)
						+ "," + Double.toString(latestReturnPct) + "," + Double.toString(latestReturn));

				cumReturns.add(cumReturnPct);
			}

			isLatestTradeBuy = false;
		}
	}

	/**
	 * Do nothing on {@link ClientFilledEvent}
	 */
	@Override
	public void onClientCancelPositionEvent(ClientCancelEvent clientCancelEvent) {

	}

	/**
	 * Do nothing on {@link ClientFilledEvent}
	 */
	@Override
	public void onClientNewPositionEvent(
			ClientNewPositionEvent clientNewPositionEvent) {
	}
}