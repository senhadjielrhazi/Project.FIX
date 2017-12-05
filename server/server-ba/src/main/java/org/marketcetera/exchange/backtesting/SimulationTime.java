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
import java.util.Calendar;

import org.marketcetera.exchange.common.Configuration;

/**
 * Defines the time used for all client generated time stamps in the simulation.
 * The internal simulation time is updated according to the replayed market events.
 * If you want the time stamps to match the internal time set "UseSimulationTime" to
 * true in config, else set to false. False causes user generated events to have
 * a time stamp corresponding to the actual current time, hence there will, possibly,
 * be huge time gaps.
 * 
 * @author <a href="mailto:catsbf@dasch.dk">Daniel Schiermer</a>
 * @version 0.1
 *
 */
public class SimulationTime {
	private static SimulationTime sInstance = new SimulationTime();
	private Calendar time = Calendar.getInstance();
	private boolean useSimulationTime = false;
	
	/**
	 * Creates the singleton object
	 */
	private SimulationTime() {
		this.useSimulationTime = Configuration.getInstance().getUseSimulationTime();
	}
	
	/**
	 * Get the singleton instance
	 * @return The {@link SimulationTime} instance
	 */
	public static SimulationTime getInstance() {	
        return sInstance;
    }
	
	/**
	 * Resets the object.
	 * NOTE: This is only for testing purposes.
	 */
	public void resetSimulationTime()
    {	
		sInstance = new SimulationTime();
    }
	
	/**
	 * Sets the simulation time
	 * @param dateTime Date-time the simulation should be set to
	 */
	public void setTime(Date dateTime) {
		time.setTime(dateTime);
	}
	
	/**
	 * Get the time used for client event time stamps.
	 * @param currentDateTime The alternative date (if configured not
	 * to use the simulation time)
	 * @return The time stamp to use.
	 */
	public Date getTime(Date currentDateTime) {
		if(useSimulationTime)
			return new Date(time.getTimeInMillis());
		else
			return currentDateTime;
	}
}