package org.neurosystem.util.math.optimization;

import static org.neurosystem.util.common.base.Preconditions.checkArgument;

import java.util.Iterator;

import org.neurosystem.util.math.optimization.operator.IComparator;
import org.neurosystem.util.math.optimization.operator.MaxOperator;
import org.neurosystem.util.math.optimization.operator.MinOperator;

public class OptimizerFactory {
	public enum DEType{
		MINIMA,
		MAXIMA;
		
		public boolean isMinima(){
			return this.equals(MINIMA);
		}
	};
	
	public static double[] optimizeToDouble(DEType type, IOptimizer problem) {
		IComparator comp = type.isMinima()?new MinOperator():new MaxOperator();
		
		Iterator<double[]> population = problem.getPopulation();
		checkArgument(population.hasNext(), "Cannot take optimization problem, no population.");
		
		//Initialisation
		double[] indOptimal = population.next();
		double fitOptimal = problem.evaluate(indOptimal);
		
		while(population.hasNext()){
			double[] individual = population.next();
			double fitness = problem.evaluate(individual);
			
			if(comp.compare(fitness, fitOptimal)){
				indOptimal = individual;
				fitOptimal = fitness;
			}
		}

		return indOptimal;
	}
}