package org.neurosystem.util.time;

public interface IPeriod {
	
	public String name();

	public long getInterval();

	default public int getNumOfUnits(IPeriod period) {
		return (int) (getInterval()/period.getInterval());
	}
}
