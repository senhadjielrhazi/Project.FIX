package org.technosystem.util;

import java.util.Arrays;
import java.util.List;

import org.neurosystem.util.common.annotations.javax.concurrent.Immutable;
import org.neurosystem.util.misc.Pair;
import org.neurosystem.util.time.IPeriod;
import org.neurosystem.util.time.Period;

@Immutable
public class Parameters {

	//Historical market data
	private static final List<Pair<IPeriod, IPeriod>> p_dbPeriods = Arrays.asList(Pair.asPair(Period.FOUR_HOURS, Period.TWO_MONTHS), 
			Pair.asPair(Period.ONE_HOUR, Period.FOUR_YEARS), Pair.asPair(Period.FIVE_MINS, Period.THREE_DAYS));
	public static List<Pair<IPeriod, IPeriod>> getDBPeriods() {
		return p_dbPeriods;
	}	
	
	private static final IPeriod p_barPeriod = p_dbPeriods.get(p_dbPeriods.size() - 1).getKey();
	public static IPeriod getBarPeriod() {
		return p_barPeriod;
	}
	
	private static final IPeriod p_historyBack = Period.FOUR_YEARS;
	public static long getHistoryBack() {
		return p_historyBack.getInterval();
	}
}