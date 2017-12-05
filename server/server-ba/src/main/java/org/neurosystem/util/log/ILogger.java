package org.neurosystem.util.log;

public interface ILogger {
	
	public void log(String message, Priority level);
	
	public void trace(String message);
}
