package org.tradingsystem;

import java.util.*;

public enum TradingTime {
	NOUPDATING, UPDATING, TRADING, CLOSING;
	public boolean isUpdating() {
		return (this == UPDATING || this == TRADING || this == CLOSING);
	}

	public boolean isTrading() {
		return (this == TRADING);
	}

	public boolean isClosing() {
		return (this == CLOSING);
	}

	public static TradingTime isValidTime(long barTime) {
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(barTime);
		boolean friday = cal.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY;
		boolean saturday = cal.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY;
		boolean sunday = cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY;
		int hour = cal.get(Calendar.HOUR_OF_DAY);

		if (saturday || (friday && hour >= 22) || (sunday && hour <= 21)) {
			return NOUPDATING;
		}

		if ((hour >= 19) || (hour < 7)) {
			return UPDATING;
		}

		if ((hour >= 15) && (hour < 19)) {
			return CLOSING;
		}

		return TRADING;
	}
}