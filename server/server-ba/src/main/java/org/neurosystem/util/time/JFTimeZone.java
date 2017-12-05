package org.neurosystem.util.time;

import java.util.TimeZone;

/**
 * This class represents time zone.
 * <p> When calculating DataType.TTIME_PERIOD_AGGREGATION candles, this time zone is used to shift two hour periods or larger (larger means periods with unit Unit.Hour (if numOfUnits >= 2) or larger)
 * candles according to time zone offset.
 * <p>Example:
 * <br>If one uses time zone EET (standard offset +2 UTC, daylight offset +3 UTC), then when time in this time zone
 * is 00:00, the GMT at that time is 22:00 or 21:00 (depends on winter/summer). So in this case, all Period.DAILY or larger candles will start at 22:00 or 21:00.
 * 
 * <p>
 * When calculating price aggregation bars or tick bars ( DataType.TICK_BAR, DataType.PRICE_RANGE_AGGREGATION, DataType.POINT_AND_FIGURE, DataType.RENKO),
 * this time zone is used to shift the base period start time. 
 * <br>Base period works as a starting point from which to calculate bars.
 * <p>Example:
 * <br>If one uses time zone EET (standard offset +2 UTC, daylight offset +3 UTC), then when time in this time zone is 00:00, 
 * the UTC at that time is 22:00 or 21:00 (depends on winter/summer). In this case all
 * price aggregation or tick bars will be calculated from Sunday 22:00 or 21:00. 
 *
 */
public enum JFTimeZone {
	
	LINT ("Pacific/Kiritimati"), 
	PHOT ("Pacific/Enderbury"), 
	MIT ("MIT"), 
	MHT ("Pacific/Majuro"), 
	NZ ("NZ"), 
	VLAT ("Asia/Vladivostok"), 
	AEST ("Australia/Queensland"), 
	AEDT ("AET"), 
	JST ("JST"), 
	CTT ("CTT"), 
	OMST ("Asia/Omsk"), 
	KGT ("Asia/Bishkek"), 
	YEKT("Asia/Yekaterinburg"),
	AZT ("Asia/Baku"), 
	MSK ("Europe/Moscow"), 
	EAT ("EAT"), 
	EET ("EET"), 
	CAT ("Africa/Maputo"), 
	CET ("CET"), 
	WAT ("Africa/Bangui"), 
	WET ("WET"), 
	UTC ("UTC"),
	EGT ("America/Scoresbysund"), 
	AZOT ("Atlantic/Azores"), 
	FNT ("America/Noronha"), 
	ART ("AGT"), 
	BRT ("BET"), 
	AST ("PRT"), 
	ADT ("Canada/Atlantic"), 
	EDT ("EST5EDT"), 
	EST ("EST"), 
	CDT ("CST"), 
	CST ("America/Guatemala"), 
	MDT ("MST7MDT"), 
	MST ("MST"), 
	PST ("Pacific/Pitcairn"), 
	PAST ("PST"), 
	AKST ("US/Alaska"), 
	GAMT ("Pacific/Gambier"), 
	HST ("US/Hawaii"), 
	HAST ("US/Aleutian"), 
	SST ("Pacific/Samoa"), 
	PACIFIC12 ("Etc/GMT+12"); 
	
	
	private long standardShift; //in milliseconds
	private long daylightShift; //in milliseconds
	private TimeZone timeZone;
	private String tzJavaName;
	private String name;
		
	private JFTimeZone(String tzJavaName){
		this.timeZone = TimeZone.getTimeZone(tzJavaName);
		this.tzJavaName = tzJavaName; 
		initShifts();
	}
	
	private void initShifts(){
		standardShift = timeZone.getRawOffset();
		daylightShift = standardShift + timeZone.getDSTSavings();		
		this.name = timeZone.getDisplayName();
	}
	
	public static JFTimeZone getByJavaName(String tzJavaName) {
	    JFTimeZone result = JFTimeZone.UTC; // default time zone
	    if (tzJavaName != null) {
    	    for (JFTimeZone timeZone : values()) {
    	        if (timeZone.getTzJavaName().equals(tzJavaName)) {
    	            result = timeZone;
    	            break;
    	        }
    	    }
	    }
	    return result;
	}
	
	public long getReversedStandardOffset(){
		return -standardShift;
	}
	
	public long getReversedDaylightOffset(){
		return -daylightShift;
	}
	
	public long getStandardOffset(){
		return standardShift;
	}
	
	public long getDaylightOffset(){
		return daylightShift;
	}
	
	public String getTzJavaName(){
		return tzJavaName;
	}
	
	public TimeZone getTimeZone(){
		return timeZone;
	}
	
	public String getName(){
		return name;
	}
}
