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
package org.marketcetera.exchange.backtesting;

import java.sql.Date;
import java.text.DateFormat;
import java.text.ParseException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wrapper for the dates to make the Spring configuration dates more readable
 * 
 * @author <a href="mailto:catsbf@dasch.dk">Daniel Schiermer</a>
 * @version 0.1
 *
 */
public class SimulationDateTimeInterval {

	private final String startDateTime;
	private final String endDateTime;
	private final DateFormat dateFormatter;
	private static final Logger LOG = LoggerFactory.getLogger(SimulationDateTimeInterval.class);

	/**
	 * Create a time interval with the given dates/times.
	 * @param startDateTime Start date and time of the simulation.
	 * @param endDateTime End date and time of the simulation.
	 * @param dateFormatter Formatter specifying how the dates should be parsed.
	 */
	public SimulationDateTimeInterval(String startDateTime, String endDateTime,
			DateFormat dateFormatter) {
		this.startDateTime = startDateTime;
		this.endDateTime = endDateTime;
		this.dateFormatter = dateFormatter;
	}

	/**
	 * Get the simulation start date and time
	 * @return The simulation start date and time
	 */
	public Date getStartDateTime() {
		return getDateTimeFromString(startDateTime);
	}

	/**
	 * Get the simulation end date and time
	 * @return The simulation end date and time
	 */
	public Date getEndDateTime() {
		return getDateTimeFromString(endDateTime);
	}
	
	/**
	 * Parse the given string to {@link Date}, using the formatter
	 * given as class argument.
	 * @param inDateTime DateTime as string
	 * @return The date and time as {@link Date} object. Returns null if it cannot be parsed.
	 */
	private Date getDateTimeFromString(String inDateTime) {
		Date dateTime = null;
		try {
			dateTime = new Date(dateFormatter.parse(inDateTime).getTime());
		} catch (ParseException e) {
			LOG.error("Error occured while parsing a date time object from string: " + e.toString());
		}
		return dateTime;
	}
}