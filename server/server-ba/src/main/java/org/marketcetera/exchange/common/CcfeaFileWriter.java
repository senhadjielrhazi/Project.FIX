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
package org.marketcetera.exchange.common;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Basic file writer that creates a file in the given position and adds lines to the file.
 * Checks that the file with the given name does not exist already and ads a suffix (integer)
 * so it will not override existing files.
 * 
 * @author <a href="mailto:catsbf@dasch.dk">Daniel Schiermer</a>
 * @version 0.1
 *
 */
public class CcfeaFileWriter {
	private String fileAbsolutePath;
	private FileWriter fileWriter;
	private BufferedWriter bufferedWriter;
	private static final Logger LOG = LoggerFactory.getLogger(CcfeaFileWriter.class);
	private final String fileBaseName;
	private final String fileExtension;

	/**
	 * Creates a file writer with the given parameters
	 * @param filePath The base path for the report
	 * @param fileBaseName Basename for the report, will get a suffix and
	 * an .txt extension
	 */
	public CcfeaFileWriter(String filePath, String fileBaseName, String fileExtension) {
		this.fileBaseName = fileBaseName;
		this.fileExtension = fileExtension;
		this.fileAbsolutePath = generateFilePath(filePath, fileBaseName);

		try {
			fileWriter = new FileWriter(this.fileAbsolutePath);
			bufferedWriter = new BufferedWriter(fileWriter);
		} catch (IOException e) {
			LOG.error("Problem creating file buffers for the '" + fileBaseName
					+ "' report: " + e.toString());
		}
	}

	/**
	 * Generates the absolute (full) file path for the report. Figures
	 * out what suffix should be applied in order not to override existing files.
	 * @param filePath The base path for the report
	 * @param fileBaseName Basename for the report, will get a suffix and
	 * the given file extension
	 * @return Absolute file path for the file
	 */
	private String generateFilePath(String filePath, String fileBaseName) {
		File file, folder;
		String newFilePath = null;
		int suffix = 1;

		do {
			//Create the folder if it does not exist
			folder = new File(filePath);
			if(!folder.exists())
				folder.mkdir();

			newFilePath = filePath + fileBaseName + suffix + "." + fileExtension;
			file = new File(newFilePath);
			suffix++;
		} while (file.exists());

		return newFilePath;	
	}

	/**
	 * Adds the given line to the file
	 * @param lineToAdd The line to be added to file
	 */
	public void addLine(String lineToAdd) {
		try {
			bufferedWriter.write(lineToAdd);
			bufferedWriter.newLine();
		} catch (IOException e) {
			LOG.error("Problem occured while writing the line '"
					+ lineToAdd + "' to the '" + fileBaseName
					+ "' report: " + e.toString());
		}
	}

	/**
	 * Closes the file. If this method is not called, nothing is saved to the file.
	 */
	public void close() {
		try {
			//Close the output stream
			if(bufferedWriter != null) {
				bufferedWriter.close();
				fileWriter.close();
			}
		} catch (IOException e) {
			LOG.error("Problem with closing the file for the '" + fileBaseName
					+ "' report: " + e.toString());
		}
	}
}