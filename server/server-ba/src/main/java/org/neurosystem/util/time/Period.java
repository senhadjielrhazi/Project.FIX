package org.neurosystem.util.time;

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.*;

import org.neurosystem.util.common.annotations.javax.concurrent.Immutable;

/**
 * Represents periods of bars. This class is used as enum in java 1.5 and later, but defined as class to allow creation of custom
 * period.
 */
@Immutable
public final class Period implements Comparable<Period>, Serializable, IPeriod {
    private static final long serialVersionUID = 1L;
    
    /**
     * {@link JFTimeZone} is used for candle calculation at data loading time. 
     * <p>
     * One can create period with JFTimeZone only through {@link #createCustomPeriod(Unit, int, JFTimeZone)} method.  
     */
    private final JFTimeZone jFTimeZone;
    
	/**
     * Defines period of tick. All methods usually returning interval or unit will return null or -1
     */
    public final static Period TICK;

    public final static Period ONE_SEC;

    public final static Period TWO_SECS;

    public final static Period TEN_SECS;

    public final static Period TWENTY_SECS;

    public final static Period THIRTY_SECS;

    public final static Period ONE_MIN;

    public final static Period FIVE_MINS;

    public final static Period TEN_MINS;

    public final static Period FIFTEEN_MINS;

    public final static Period TWENTY_MINS;

    public final static Period THIRTY_MINS;

    public final static Period ONE_HOUR;

    public final static Period FOUR_HOURS;

    public final static Period DAILY;
    
    public final static Period TWO_DAYS;
    
    public final static Period THREE_DAYS;
    
    @Deprecated
    public final static Period DAILY_SUNDAY_IN_MONDAY;
    
    @Deprecated
    public final static Period DAILY_SKIP_SUNDAY;

    public final static Period WEEKLY;
    
    public final static Period TWO_WEEKS;
    
    public final static Period MONTHLY;
    
    public final static Period TWO_MONTHS;
    
    public final static Period QUARTERLY;

    public final static Period HALF_YEAR;
    
    /**
     * This period is used only for price aggregation and tick bar base period indication. 
     * One can set base period for previously mentioned data types, by giving it to feed descriptor's  
     * constructor, which takes Period class as an argument, or by using a setter method ({@link com.dukascopy.api.feed.IFeedDescriptor#setPeriod(Period)}).
     */
    public final static Period INFINITY;

    public final static Period ONE_YEAR;

    public final static Period TWO_YEARS;
    
    public final static Period THREE_YEARS;
    
    public final static Period FOUR_YEARS;
    
    /**
     * Set of predefined periods
     */
    private final static Period[] PREDEFINED;

    /**
     * Contains a set of basic periods
     */
    private final static Period[] PREDEFINED_IND;

    /**
     * Contains custom periods to exclude creation of the new objects of the same period
     */
    private final static Set<Period> CUSTOM_PERIODS = new HashSet<>();

    private final int ordinal;

    private final String name;

    /**
     * Time unit
     * @deprecated use getUnit() instead
     */
    @Deprecated
    public final Unit unit;

    /**
     * Number of time units
     * @deprecated use getNumOfUnits() instead
     */
    @Deprecated
    public final int numOfUnits;

    /**
     * Time interval in milliseconds
     * @deprecated use getInterval() instead
     */
    @Deprecated
    public final long interval;

    static {
        int num = 0;
        TICK = new Period(num++, "TICK", null, -1);

        ONE_SEC = new Period(num++, "ONE_SEC", Unit.Second, 1);
        TWO_SECS = new Period(num++, "TWO_SECS", Unit.Second, 2);
        TEN_SECS = new Period(num++, "TEN_SECS", Unit.Second, 10);
        TWENTY_SECS = new Period(num++, "TWENTY_SECS", Unit.Second, 20);
        THIRTY_SECS = new Period(num++, "THIRTY_SECS", Unit.Second, 30);

        ONE_MIN = new Period(num++, "ONE_MIN", Unit.Minute, 1);
        FIVE_MINS = new Period(num++, "FIVE_MINS", Unit.Minute, 5);
        TEN_MINS = new Period(num++, "TEN_MINS", Unit.Minute, 10);
        FIFTEEN_MINS = new Period(num++, "FIFTEEN_MINS", Unit.Minute, 15);
        TWENTY_MINS = new Period(num++, "TWENTY_MINS", Unit.Minute, 20);
        THIRTY_MINS = new Period(num++, "THIRTY_MINS", Unit.Minute, 30);

        ONE_HOUR = new Period(num++, "ONE_HOUR", Unit.Hour, 1);
        FOUR_HOURS = new Period(num++, "FOUR_HOURS", Unit.Hour, 4);

        DAILY = new Period(num++, "DAILY", Unit.Day, 1);
        TWO_DAYS = new Period(num++, "TWO_DAYS", Unit.Day, 2);
        THREE_DAYS = new Period(num++, "THREE_DAYS", Unit.Day, 3);
        
        DAILY_SUNDAY_IN_MONDAY = new Period(DAILY.ordinal, "DAILY_SUNDAY_IN_MONDAY", Unit.Day, 1);        
        DAILY_SKIP_SUNDAY = new Period(DAILY.ordinal, "DAILY_SKIP_SUNDAY", Unit.Day, 1);
        WEEKLY = new Period(num++, "WEEKLY", Unit.Week, 1);
        TWO_WEEKS = new Period(num++, "TWO_WEEKS", Unit.Week, 2);
        
        MONTHLY = new Period(num++, "MONTHLY", Unit.Month, 1);
        TWO_MONTHS = new Period(num++, "TWO_MONTHS", Unit.Month, 2);
        QUARTERLY = new Period(num++, "QUARTERLY", Unit.Month, 3);
        HALF_YEAR = new Period(num++, "HALF_YEAR", Unit.Month, 6);
        
        ONE_YEAR = new Period(num++, "ONE_YEAR", Unit.Year, 1);
        TWO_YEARS = new Period(num++, "TWO_YEARS", Unit.Year, 2);
        THREE_YEARS = new Period(num++, "THREE_YEARS", Unit.Year, 3);
        FOUR_YEARS = new Period(num++, "FOUR_YEARS", Unit.Year, 4);
        
        INFINITY = new Period(num + 1, "INFINITY", Unit.Year, Integer.MAX_VALUE);

        PREDEFINED = new Period[num];
        PREDEFINED[TICK.ordinal] = TICK;
        PREDEFINED[ONE_SEC.ordinal] = ONE_SEC;
        PREDEFINED[TWO_SECS.ordinal] = TWO_SECS;
        PREDEFINED[TEN_SECS.ordinal] = TEN_SECS;
        PREDEFINED[TWENTY_SECS.ordinal] = TWENTY_SECS;
        PREDEFINED[THIRTY_SECS.ordinal] = THIRTY_SECS;
        PREDEFINED[ONE_MIN.ordinal] = ONE_MIN;
        PREDEFINED[FIVE_MINS.ordinal] = FIVE_MINS;
        PREDEFINED[TEN_MINS.ordinal] = TEN_MINS;
        PREDEFINED[FIFTEEN_MINS.ordinal] = FIFTEEN_MINS;
        PREDEFINED[TWENTY_MINS.ordinal] = TWENTY_MINS;
        PREDEFINED[THIRTY_MINS.ordinal] = THIRTY_MINS;
        PREDEFINED[ONE_HOUR.ordinal] = ONE_HOUR;
        PREDEFINED[FOUR_HOURS.ordinal] = FOUR_HOURS;
        PREDEFINED[DAILY.ordinal] = DAILY;
        PREDEFINED[TWO_DAYS.ordinal] = TWO_DAYS;
        PREDEFINED[THREE_DAYS.ordinal] = THREE_DAYS;
        PREDEFINED[WEEKLY.ordinal] = WEEKLY;
        PREDEFINED[TWO_WEEKS.ordinal] = TWO_WEEKS;
        PREDEFINED[MONTHLY.ordinal] = MONTHLY;
        PREDEFINED[TWO_MONTHS.ordinal] = TWO_MONTHS;
        PREDEFINED[QUARTERLY.ordinal] = QUARTERLY;
        PREDEFINED[HALF_YEAR.ordinal] = HALF_YEAR;
        PREDEFINED[ONE_YEAR.ordinal] = ONE_YEAR;
        PREDEFINED[TWO_YEARS.ordinal] = TWO_YEARS;
        PREDEFINED[THREE_YEARS.ordinal] = THREE_YEARS;
        PREDEFINED[FOUR_YEARS.ordinal] = FOUR_YEARS;
        
        PREDEFINED_IND = new Period[] { TICK, ONE_MIN, ONE_HOUR, DAILY };
    }

    /**
     * Creates "enum" periods
     */
    private Period(int ordinal, String name, Unit unit, int nUnits) {
        this.ordinal = ordinal;
        this.name = name;
        this.unit = unit;
        this.numOfUnits = nUnits;
        this.interval = unit == null ?
        		-1 
        		: (nUnits == Integer.MAX_VALUE && Unit.Year.equals(unit)) ? 
        				Long.MAX_VALUE - Long.MAX_VALUE % Unit.Day.getInterval() 
        				: unit.getInterval() * nUnits;  
        this.jFTimeZone = JFTimeZone.UTC;
    }

    /**
     * Returns time unit. Returns null for TICK
     * 
     * @return time unit
     */
    public final Unit getUnit() {
        return unit;
    }

    /**
     * Returns number of units. Returns -1 for TICK
     * 
     * @return number of units or -1 if it's TICK
     */
    public final int getNumOfUnits() {
        return numOfUnits;
    }

    /**
     * Returns interval in milliseconds. For TICK returns -1. For periods with variable length returns only one static value, MONTLY = 30 days,
     * ONE_YEAR = <code>(long) (365.24 * 24 * 60 * 60 * 1000L)</code>
     * @return interval in milliseconds or -1 if it's TICK
     */
    public final long getInterval() {
        return isTickBasedPeriod() ? -1 : interval;
    }

    @Override
	public final boolean equals(Object other) {
    	if (other == null || !(other instanceof Period)){
    		return false;
    	}
        return this.getInterval() == ((Period) other).getInterval() && this.jFTimeZone.equals(((Period)other).getJFTimeZone()) || isInfinity(this) && isInfinity((Period)other);
    }

    /**
     * Throws {@link CloneNotSupportedException}
     */
    public final Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException();
    }

    /**
     * Compares periods, but instead of using ordinal like enums, uses interval
     */
    public final int compareTo(Period another) {
        if (interval < another.interval) {
            return -1;
        } else if (interval > another.interval) {
            return 1;
        } else {
            return 0;
        }
    }

    public final int hashCode() {
        return (int) (interval + jFTimeZone.getReversedStandardOffset() + jFTimeZone.getReversedDaylightOffset());
    }

    public final String toString() {
    	if (Period.isInfinity(this)) {
    		return Period.INFINITY.name();
    	}
    	
        if (isTickBasedPeriod()) {
            return "Ticks";
        }

        if (getNumOfUnits() == 1) {
            switch (getUnit()) {
                case Millisecond:
                    return "Milliseconds";
                case Hour:
                    return "Hourly";
                case Day:
                    return "Daily";
                case Week:
                    return "Weekly";
                case Month:
                    return "Monthly";
                case Year:
                    return "Yearly";
                default:
            }
        }

        StringBuilder sb = new StringBuilder(10).append(getNumOfUnits()).append(" ").append(getUnit().getCompactDescription());
        if (getNumOfUnits() > 1) {
            sb.append("s");
        }

        return sb.toString();
    }

    public int ordinal() {
        return ordinal;
    }

    /**
     * Name of predefined constant exactly how it was defined or null if it's custom period
     * 
     * @return name of period or null for custom periods
     */
    public String name() {
        return name;
    }

    /**
     * Returns one of the predefined periods with specified name. Throws IllegalArgumentException if there is no predefined period with
     * specified name (all custom periods will throw it)
     * 
     * @param name name of the period constant, exactly how it defined
     * @return period
     */
    public static Period valueOf(String name) {
        if (name == null) {
            throw new NullPointerException("Name is null");
        }
        for (Period period : PREDEFINED) {
            if (period.name().equals(name)) {
                return period;
            }
        }
        if (Period.DAILY_SKIP_SUNDAY.name().equals(name)) {
            return Period.DAILY_SKIP_SUNDAY;
        } else if (Period.DAILY_SUNDAY_IN_MONDAY.name().equals(name)) {
            return Period.DAILY_SUNDAY_IN_MONDAY;
        } else if (Period.INFINITY.name().equals(name)) {
        	return Period.INFINITY;
        }
        throw new IllegalArgumentException("No enum const Period." + name);
    }

    /**
     * Returns array of predefined periods
     * 
     * @return predefined periods
     */
    public static Period[] values() {
        return PREDEFINED;
    }

    /**
     * Returns an array of basic periods
     * 
     * @return array of basic periods
     */
    public static Period[] valuesForIndicator() {
        return PREDEFINED_IND;
    }

    /**
     * Checks whether this periods time interval is smaller than the one of period
     * @param period period to compare with
     * @return  false if period time interval is smaller or is null. True if period time interval is bigger.
     */
    public boolean isSmallerThan(Period period) {
        return period != null && this.getInterval() < period.getInterval();
    }
    
    /**
     * @deprecated Please, use {@link #createCustomPeriod(Unit, int, JFTimeZone)}}
     */
    @Deprecated
    public static Period createCustomPeriod(String name, Unit unit, int unitsCount) {
    	return createCustomPeriod(unit, unitsCount);
    }
    
    /**
     * This method returns custom period with given {@link JFTimeZone}. If jFTimezone is null, then default is used (JFTimeZone.UTC).
     * <br>For units smaller than two hours, JFTimeZone can have time offsets no other than 0 (JFTimeZone.UTC).
     * If desired period is equal to one of the basic periods, then the appropriate basic period will be returned. 
     * <p>For the JFTimeZone impact on data loading, please see {@link JFTimeZone} javadocs.  
     * 
     * @param unit unit of custom period
     * @param unitsCount amount of units in custom period
     * @param jfTimeZone timezone of custom period
     * @return Period - a desired period.
     * @throws IllegalArgumentException if jFTimeZone has offset from UTC time other than 0 and the given unit is smaller than Unit.Day, or if the desired period is not compliant.
     */
    public static Period createCustomPeriod(Unit unit, int unitsCount, JFTimeZone jfTimeZone) {
    	//shift can be set to Unit.Day or larger, only!
    	if (jfTimeZone == null || unit == null) {
    		jfTimeZone = JFTimeZone.UTC;
    	}
    	if (!isTimeZoneValid(unit, unitsCount, jfTimeZone)) {
    		throw new IllegalArgumentException("JFTimeZone is not appropriate for this unit. Received Unit = " + unit + ", numOfUnits = " + unitsCount + ", JFTimeZone = " 
    				+ jfTimeZone + " with standard offset " + jfTimeZone.getReversedStandardOffset() + "(milliseconds) and daylight offset " + jfTimeZone.getReversedDaylightOffset()
    				+ "(milliseconds). Offsets cannot be other than 0 for periods with Unit smaller than Unit.Day.");
    	}
    	
    	//--------------
    	if (unit == null && unitsCount == -1) {
            return Period.TICK;
        }

    	checkPeriodName(unit);
    	
    	if (unitsCount <= 0) {
    		throw new IllegalArgumentException("Number of units should be greater than 0");
    	}
    	
    	if (INFINITY.getUnit().equals(unit) && INFINITY.getNumOfUnits() == unitsCount) {
    		return INFINITY;
    	}

        /*
         * Don't let to create the same periods again
         */
        long newInterval = unit.getInterval() * unitsCount;
        for (Period period : CUSTOM_PERIODS) {
            if (period.getInterval() == newInterval && period.getUnit() == unit && period.getJFTimeZone().equals(jfTimeZone)) {
                return period;
            }
        }
        for (Period period : PREDEFINED) {
            if (period.getInterval() == newInterval && period.getUnit() == unit && period.getJFTimeZone().equals(jfTimeZone)) {
                return period;
            }
        }

    	Period period = new Period(-1, null, unit, unitsCount, jfTimeZone);
    	/*
    	 * Period has to be compliant to the predefined period 
    	 */
    	boolean isPeriodCompliant = isPeriodCompliant(period);
    	if (!isPeriodCompliant) {
    		throw new IllegalArgumentException("Can not create period for arguments - " + unit + ", " + unitsCount + ", because they are not compliant to '" + Period.getCompliancyPeriod() + "' time period");
    	}

        CUSTOM_PERIODS.add(period);
        return period;
    }
    
    private Period(int ordinal, String name, Unit unit, int nUnits, JFTimeZone jFTimeZone) {
    	this.ordinal = ordinal;
        this.name = name;
        this.unit = unit;
        this.numOfUnits = nUnits;
        this.interval = unit == null ?
        		-1 
        		: (nUnits == Integer.MAX_VALUE && Unit.Year.equals(unit)) ? 
        				Long.MAX_VALUE - Long.MAX_VALUE % Unit.Day.getInterval() 
        				: unit.getInterval() * nUnits;
        
        this.jFTimeZone = jFTimeZone;
    }
    
    /**
     * Method creates custom Period. Note, that the desired period has to be compliant to Period.getCompliancyPeriod() period.
     * If desired period is not compliant, then IllegalArgumentException will be thrown.
     * If desired period is equal to one of the basic periods, the the appropriate basic period will be returned.
     * Returned period's shift will be 0. 
     * 
     * @param unit unit of custom period
     * @param unitsCount amount of units in custom period
     * @return Period
     */
    public static Period createCustomPeriod(Unit unit, int unitsCount) {
        return createCustomPeriod(unit, unitsCount, JFTimeZone.UTC);
    }
    
    /**
     * Returns correct instance when deserializing
     *
     * @return precreated instance
     */
    private Object readResolve() throws ObjectStreamException {
        for (Period period : PREDEFINED) {
            if (period.equals(this)) {
                return period;
            }
        }

        for (Period period : CUSTOM_PERIODS) {
            if (period.equals(this)) {
                return period;
            }
        }

        CUSTOM_PERIODS.add(this);
        return this;
    }

    /**
     * The method checks whether passed period is compliant to etalon period {DAILY}
     * If compliant - true is returned
     * If not compliant - false is returned
     * 
     * @param period period to check
     * @return boolean
     */
    public static boolean isPeriodCompliant(Period period) {
    	return isPeriodCompliant(period, getCompliancyPeriod());
    }

    /**
     * The method returns compliancy period
     * 
     * @return Period
     */
    public static Period getCompliancyPeriod() {
    	return Period.DAILY; 
    }
    
	private static boolean isPeriodCompliant(Period period, Period etalon) {
		if (etalon.getInterval() > period.getInterval()) {
            return etalon.getInterval() % period.getInterval() == 0;
		} else {
            return period.getInterval() % etalon.getInterval() == 0;
		}
	}

	/**
	 * The method checks whether the passed period is basic {TICK, ONE_MIN, ONE_HOUR, DAILY}
	 * If it is basic - appropriate basic period is returned
	 * If it is not basic - null is returned
	 * 
	 * @param period period to check
	 * @return Basic Period
	 */
	public static Period isPeriodBasic(Period period) {
	    return isPeriodBasic(period, false);
	}

    public static Period isPeriodBasic(Period period, boolean ignoreTimeZone) {     
        for (Period p : PREDEFINED_IND) {
            if (
            		(period.getUnit() == null && p.getUnit() == null || p.getUnit() == period.getUnit()) 
            		&& p.getNumOfUnits() == period.getNumOfUnits() && 
            		(ignoreTimeZone || period.getJFTimeZone().equals(p.getJFTimeZone()))
            ) {
                return p;
            }
        }
        return null;
    }

	/**
	 * Returns the biggest possible period for passed custom one. 
	 * The returned period   
	 * 
	 * @param period custom period
	 * @return biggest possible period
	 */
	public static Period getBasicPeriodForCustom(Period period) {
    	/*
    	 * Don't use Arrays.asList(), because sorting will mix target array also
    	 */
		List<Period> basicPeriods = new ArrayList<>();
        Collections.addAll(basicPeriods, Period.valuesForIndicator());
		
		/*
		 * Make sure periods are sorted by ascending 
		 */
		Collections.sort(basicPeriods, new Comparator<Period>() {
			@Override
			public int compare(Period o1, Period o2) {
				if (o1.getInterval() > o2.getInterval()) {
					return 1;
				} else {
                    return -1;
                }
			}
		});
		
		Period lastSuitablePeriod = Period.TICK;
		for (Period p : basicPeriods) {
            if (!p.isTickBasedPeriod() &&
                period != null &&
                p.getInterval() <= period.getInterval() &&
                period.getInterval() % p.getInterval() == 0
			) {
				lastSuitablePeriod = p;
			}
		}

		return lastSuitablePeriod;
	}
	
	/**
	 * The method generates all compliant custom periods used in JForex
	 * 
	 * @return List<Period>
	 */
	public static List<Period> generateAllCompliantPeriods() {
		List<Period> periods = new ArrayList<>();
		periods.addAll(generateCompliantPeriods(Unit.Second, 1, 59));
		periods.addAll(generateCompliantPeriods(Unit.Minute, 59));
		periods.addAll(generateCompliantPeriods(Unit.Hour, 12));
		periods.addAll(generateCompliantPeriods(Unit.Day, 6));
		periods.addAll(generateCompliantPeriods(Unit.Week, 4));
		periods.addAll(generateCompliantPeriods(Unit.Month, 11));
		return periods;
	}
	
	/**
	 * @deprecated Please, use {@link #generateCompliantPeriods(Unit, int)}
	 */
    @Deprecated
	public static List<Period> generateCompliantPeriods(String name, Unit unit, int maxUnitCount) {
		return generateCompliantPeriods(unit, 1, maxUnitCount);
	}

	/**
	 * The method generates and returns the list of compliant periods for passed Unit from 1 to maxUnitCount
	 * 
	 * @param unit unit of the period
	 * @param maxUnitCount maximum number of units to check
	 * @return List<Period>
	 */
	public static List<Period> generateCompliantPeriods(Unit unit, int maxUnitCount) {
		return generateCompliantPeriods(unit, 1, maxUnitCount);
	}
	
	/**
	 * @deprecated Please, use {@link #generateCompliantPeriods(Unit, int, int)}
	 */
    @Deprecated
	public static List<Period> generateCompliantPeriods(String name, Unit unit, int startUnitCount, int maxUnitCount) {
		return generateCompliantPeriods(unit, startUnitCount, maxUnitCount);
	}
	
	/**
	 * The method generates and returns the list of compliant periods for passed Unit from startUnitCount to maxUnitCount
	 * 
     * @param unit unit of the period
     * @param startUnitCount minimum number of units to check
     * @param maxUnitCount maximum number of units to check
	 * @return List<Period>
	 */
	public static List<Period> generateCompliantPeriods(Unit unit, int startUnitCount, int maxUnitCount) {
		List<Period> periods = new ArrayList<>();
		for (int i = startUnitCount; i <= maxUnitCount; i ++) {
			try {
				Period p = Period.createCustomPeriod(unit, i);
				periods.add(p);
			} catch (Throwable ignore) {}
		}
		return periods;
	}

	private static void checkPeriodName(Unit unit) {
		switch (unit) {
			case Millisecond:
			case Second:
			case Minute:
			case Hour:
			case Day:
			case Week:
			case Month:
			case Year:
                return;
		default:
			break;
		}
		throw new IllegalArgumentException("Unsupported Unit - " + unit);
	}

	@SuppressWarnings("serial")
	private static final Map<String, Unit> UNIT_MAP = new HashMap<String, Unit> () {{
		put(Unit.Month.getCompactDescription(), Unit.Month);
		put(Unit.Month.getCompactDescription() +"s", Unit.Month);

		put(Unit.Day.getCompactDescription(), Unit.Day);
		put(Unit.Day.getCompactDescription() +"s", Unit.Day);

		put(Unit.Hour.getCompactDescription(), Unit.Hour);
		put(Unit.Hour.getCompactDescription() +"s", Unit.Hour);

		put(Unit.Minute.getCompactDescription(), Unit.Minute);
		put(Unit.Minute.getCompactDescription() +"s", Unit.Minute);

		put(Unit.Second.getCompactDescription(), Unit.Second);
		put(Unit.Second.getCompactDescription() +"s", Unit.Second);
	}};

	@SuppressWarnings("serial")
	private static final Map<String, Unit> SINGULAR_UNITS = new HashMap<String, Unit> () {{
		put("Milliseconds", Unit.Millisecond);
		put("Hourly", Unit.Hour);
		put("Daily", Unit.Day);
		put("Weekly", Unit.Week);
		put("Monthly", Unit.Month);
		put("Yearly", Unit.Year);
	}};

	public static Period valueOfToString(String s) {
		if (s.equals("Ticks")) {
			return TICK;
		}

		Unit singularUnit = SINGULAR_UNITS.get(s);
		if (singularUnit != null) {
			return createCustomPeriod(singularUnit, 1);
		}

		String[] values = s.split(" ");
		if (values.length < 2) {
			return null;
		}

		int count = Integer.valueOf(values[0]);
		Unit unit = UNIT_MAP.get(values[1]);
		return createCustomPeriod(unit, count);
	}
	
	/**
	 * Returns period's standard time shift from GMT.
     * @deprecated
	 */
	@Deprecated
	public long getShift() {
		return jFTimeZone.getStandardOffset();
	}
	
	public JFTimeZone getJFTimeZone(){
		return jFTimeZone;
	}
	
	public boolean isTickBasedPeriod() {
	    return getUnit() == null;
	}
	
	/**
	 * Checks whether the given time zone's offset is valid for the given unit, e.g. for units smaller than two hours 
	 * JFTimeZone can have time offsets no other than 0 (JFTimeZone.UTC). 
	 * 
	 * @param unit Unit
     * @param numOfUnits Number of units
	 * @param timeZone Time zone
	 * @return true if the given time zone's offset is valid for the given period, false otherwise
	 */
	public static boolean isTimeZoneValid(Unit unit, int numOfUnits, JFTimeZone timeZone) {
		if (timeZone.getReversedStandardOffset() != 0 || timeZone.getReversedDaylightOffset() != 0) {
			if (unit == null) {
				return false; 
			}			
			if (Unit.Hour.getInterval() > unit.getInterval()) {
				return false;
			}
			if (Unit.Hour.equals(unit) && numOfUnits < 2) {
				return false;
			}			
		}
		return true;
	}
	
	/**
     * Checks whether the given period may have a nonzero offset, see {@link #isTimeZoneValid(Unit unit, int numOfUnits, JFTimeZone timeZone)} for more details
     * 
     * @param period Period
     * @return true if the given period may have a nonzero offset
     */
    public static boolean canHaveNonZeroOffset(Period period) {
        return period != null && period.compareTo(Period.ONE_HOUR) == 1;
    }

	public static boolean isInfinity(Period period) {
		if (period == null) {
			return false;
		}

		if (Period.INFINITY.name().equalsIgnoreCase(period.name)
			&& Period.INFINITY.getUnit().equals(period.getUnit())
			&& Period.INFINITY.getNumOfUnits() / 2 <= period.getNumOfUnits()
		) {
			return true;
		}

		return false;
	}
	
	public static void main(String args[]){
		System.out.println(Period.TWO_MONTHS.getNumOfUnits(Period.FOUR_HOURS));
	}
}
