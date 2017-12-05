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

import java.util.Observable;
import java.util.Observer;

import org.marketcetera.exchange.events.ClientCancelEvent;
import org.marketcetera.exchange.events.ClientFilledEvent;
import org.marketcetera.exchange.events.ClientNewPositionEvent;
import org.marketcetera.exchange.events.SimulationStartedEvent;

/**
 * Report interface which let you create observable reports. The methods allows you
 * to do initialization, add a line to the report and post-processing.
 * One could create reports that write to a file, prints to console or a database.
 * 
 * @author <a href="mailto:catsbf@dasch.dk">Daniel Schiermer</a>
 * @version 0.1
 */
public abstract class Report implements Observer {
	private String reportPath;
	private String reportBaseName;
	private final String fileExtension;

	/**
	 * Creates an instance of the report
	 * @param reportPath The base path for the report
	 * @param reportBaseName Basename for the report, will get a suffix and
	 * an the given file extension
	 * @param fileExtension The extension of the report
	 */
	public Report(String reportPath, String reportBaseName, String fileExtension) {
		this.reportPath = reportPath;
		this.reportBaseName = reportBaseName;
		this.fileExtension = fileExtension;
	}

	/**
	 * Handles events
	 */
	public void update(Observable o, Object arg) {
		//We cannot use the stop event to close the report
		//because the FIX servers has to be stopped before
		//the reports
		if (arg instanceof SimulationStartedEvent) {
			start();
		} else if (arg instanceof ClientNewPositionEvent) {
			onClientNewPositionEvent((ClientNewPositionEvent)arg);
		} else if (arg instanceof ClientCancelEvent) {
			onClientCancelPositionEvent((ClientCancelEvent)arg);
		} else if (arg instanceof ClientFilledEvent) {
			onClientFilledEvent((ClientFilledEvent)arg);
		}
	}

	/**
	 * Method that is called when the reporting is started
	 */
	public abstract void start();
	
	/**
	 * Method that is called when the reporting is stopped
	 */
	public abstract void stop();
	
	/**
	 * Method called on {@link ClientFilledEvent}
	 * @param clientFilledEvent Event fired
	 */
	public abstract void onClientFilledEvent(ClientFilledEvent clientFilledEvent);
	
	/**
	 * Method called on {@link ClientCancelEvent}
	 * @param clientCancelEvent Event fired
	 */
	public abstract void onClientCancelPositionEvent(ClientCancelEvent clientCancelEvent);
	
	/**
	 * Method called on {@link ClientNewPositionEvent}
	 * @param clientNewPositionEvent Event fired
	 */
	public abstract void onClientNewPositionEvent(ClientNewPositionEvent clientNewPositionEvent);

	/**
	 * Get the base report path
	 * @return Base report path
	 */
	public String getReportPath() {
		return reportPath;
	}

	/**
	 * Get the report base name
	 * @return Report base name
	 */
	public String getReportBaseName() {
		return reportBaseName;
	}

	/**
	 * Get the report file extension
	 * @return Report file extension.
	 */
	public String getFileExtension() {
		return fileExtension;
	}
}