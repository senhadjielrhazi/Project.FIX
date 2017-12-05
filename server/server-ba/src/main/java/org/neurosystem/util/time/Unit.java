package org.neurosystem.util.time;

/**
 * Defines time units
 * 
 */
public enum Unit {

    Millisecond(1L, "ms", "MilliSec", "Millisecond"),
    Second(1000L, "s", "Sec", "Second"),
    Minute(60 * 1000L, "m", "Min", "Minute"),
    Hour(60 * 60 * 1000L, "h", "Hour", "Hourly"),
    Day(24 * 60 * 60 * 1000L, "D", "Day", "Daily"),
    Week(7 * 24 * 60 * 60 * 1000L, "W", "Week", "Weekly"),
    Month(30 * 24 * 60 * 60 * 1000L, "M", "Month", "Monthly"),
    Year((long) (365.25 * 24 * 60 * 60 * 1000L), "Y", "Year", "Yearly");
	
    private long interval;
    private String shortDescription;
    private String compactDescription;
    private String longDescription;

    private Unit(long interval, String shortDescription, String compactDescription, String longDescription) {
        this.interval = interval;
        this.shortDescription = shortDescription;
        this.compactDescription = compactDescription;
        this.longDescription = longDescription;
    }
    
    /**
     * Returns interval of time unit in milliseconds
     * 
     * @return interval of time unit in milliseconds
     */
    public final long getInterval() {
        return interval;
    }
    
    /**
     * Returns short description, usually one or two characters long
     * 
     * @return short description
     */
    public String getShortDescription() {
        return shortDescription;
    }
    
    /**
     * Returns compact description
     * 
     * @return compact description
     */
    public String getCompactDescription() {
        return compactDescription;
    }
    
    /**
     * Returns long description
     * 
     * @return long description
     */
    public String getLongDescription() {
        return longDescription;
    }
}
