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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import quickfix.SessionSettings;

/**
 * Methods related to load configuration files and create SessionSettings.
 * 
 * @author <a href="mailto:catsbf@dasch.dk">Daniel Schiermer</a>
 * @version 0.1
 */
public class FIXConfigurationLoader {
	/**
	 * Loads configuration from given file path.
	 * @param configFilePath file path of config file.
	 * @return SessionSettings instance generated from config file
	 * @throws FileNotFoundException
	 */
	public static SessionSettings loadFixConfiguration(String configFilePath)
	throws FileNotFoundException
	{
		SessionSettings settings = null;
		try{
			InputStream inputStream = new FileInputStream(configFilePath);
			settings = new SessionSettings(inputStream);
			inputStream.close();
		}
		catch(Exception e){
			e.printStackTrace();
		}
		return settings;
	}
}