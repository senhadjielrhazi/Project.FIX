package org.neurosystem.util;

import org.neurosystem.util.common.annotations.javax.concurrent.Immutable;
import org.neurosystem.util.time.IPeriod;
import org.neurosystem.util.time.Period;

@Immutable
public class Parameters {
	
	//Market data periods
	private static final IPeriod p_barPeriod = Period.FIVE_MINS;
	public static IPeriod getBarPeriod() {
		return p_barPeriod;
	}
	
	/*private static final IPeriod p_tickPeriod = Period.FIVE_MINS;
	public static IPeriod getTickPeriod() {
		return p_tickPeriod;
	}*/
	
	//Trading details
	private static final int p_nbMaxTrades = 4;
	public static int getNbMaxTrades() {
		return p_nbMaxTrades;
	}
	
	//Historical data
	private static final Period p_historyBack = Period.HALF_YEAR;
	public static long getHistoryBack() {
		return p_historyBack.getInterval();
	}
	
	//Classification
	private static final int p_maxExecutors = 20;
	public static int getMaxExecutors() {
		return p_maxExecutors;
	}
	
	private static final IPeriod p_setsPeriod = Period.MONTHLY;
	public static long getClassificationFQ() {
		return p_setsPeriod.getInterval();
	}
	
	//StopLoss
	private static final double[] p_basicPNL = {32.5, 150., 50.};//{32.5, 100., 25.}
	public static double[] getBasicPNL() {
		return p_basicPNL;
	}
	
	private static final double[] p_basicWin = {20, 15., 0.55};
	public static double[] getBasicWin() {
		return p_basicWin;
	}

	//Analysis
	private static final IPeriod p_timeBuffer = Period.TWO_DAYS;
	public static int getTimeBuffer(){
		return p_timeBuffer.getNumOfUnits(p_barPeriod);
	}
	
	private static final Object[] p_analysisParams = {
			4,//0
			new int[]{6, 12, 24, 36, 48, 60},//1
			new int[]{12, 12*4, 12*8, 12*24, 12*24*5},//2	
			12*24,//3				
			new int[]{6, 14},//4
			new int[]{12, 26, 9},//5
			new int[]{6, 24},//6
			new int[]{12, 24, 6},//7
			new int[]{12, 24, 36, 48},//8
			12,//9
			new int[]{24, 12*24},//10
			new int[]{12, 24},//11
			new double[]{30., 0.90},//12
			new double[] {0.0025, 0.005, 0.0075, 0.010, 0.0125},//13
			new double[]{0.0005, 0.0005, 0.0005, 0.00025, 0.0001},//14
			30.,//15
			0.0025,//16
			0.77,//17		
			0.0005,//18
			120.,//19
	};
	
	public static Object[] getAnalysisParams() {
		return p_analysisParams;
	}


	
	
	
	

	
	
	
	//{20, 0.0025, 0.575};
	



	
	


	

	

	



	
	public static int getClassificationNB() {
		return (int) (p_historyBack.getInterval()/p_setsPeriod.getInterval());
	}
	



	/*
	{10, 0.0020, 0.55};{10, 0.0010, 0.65};
	
	private static final double[] p_basicOpt = {0.00001, 0.0005, 0.00075, 0.375, 0.20};
	
	public static double[] getBasicOpt(){
		return p_basicOpt;
	}
	public static void main(String[] args) {
		System.out.println(Arrays.toString((int[])getAnalysisParams()[22]));
		
		System.out.println(Arrays.toString((double[])getAnalysisParams()[24]));
	}
	*/
}