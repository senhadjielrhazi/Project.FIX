package org.neurosystem.util.math.optimization;

import java.util.Iterator;

public interface IOptimizer {

	public final static double easyprecision = 0.00001;
	
	public double evaluate(double[] individual);

	public int getDimension();

	public Iterator<double[]> getPopulation();
	
	public default double distance(double[] individual, double[] target) {
		double distance = 0;
		for(int i = 0; i < individual.length; i++){
			distance += Math.pow(individual[i] - target[i], 2);
		}
		return distance;
	}
	
	public static int easyBins(int dim) {
		if(dim <= 3){
			return 40;
		}else if (dim <= 5){
			return 10;
		}
		
		return 5;
	}
}
