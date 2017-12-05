package org.neurosystem.util.basic;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public interface HasTime {
	
	SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yy HH:mm:ss");
    
	public long getTime();
	
	public default String formatedTime() {
        return dateFormat.format(new Date(getTime()));
    }
	
	public static String formatedTime(long time) {
        return dateFormat.format(new Date(time));
    }
	
	public static long parse(String source) {
        try {
			return dateFormat.parse(source).getTime();
		} catch (Exception e) {
			return 0L;
		}
    }
	
	public static boolean isTradingTime(long time) {
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(time);
		
		boolean friday = cal.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY;
		boolean saturday = cal.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY;
		boolean sunday = cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY;
		int hour = cal.get(Calendar.HOUR_OF_DAY);
		
		if ((friday && hour >= 20) || saturday || (sunday && hour <= 22)) {
			return false;
		}

		return true;
	}
	
	public static int timeZone(long time) {
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(time);
		int hour = cal.get(Calendar.HOUR_OF_DAY);

		return Math.round(hour / 4);
	}

	public static boolean timeZone(long tzone, long sZone){
		return timeZone(tzone) == timeZone(sZone);
	}
	
	public static boolean sameTimeZone(long time, long mins) {
		return timeZone(time, time + (mins * 60000));
	}
}
