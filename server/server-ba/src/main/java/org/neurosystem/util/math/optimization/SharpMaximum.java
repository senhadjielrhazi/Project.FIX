package org.neurosystem.util.math.optimization;

import java.util.Iterator;
import java.util.List;

import org.neurosystem.util.common.base.Preconditions;
import org.neurosystem.util.common.math.DoubleMath;
import org.neurosystem.util.math.optimization.recursive.GenericIterator;

public class SharpMaximum implements IOptimizer {

	private final double[][] p_covarianceMx;
	private final double[] p_returns;
	
	private final int p_dimension;
	private final int p_numBins;
	
	private final double[] p_target;
	private final double p_targetWeight;
	
	public SharpMaximum(List<List<Double>> matrix, double[] target, double targetWeight) {
    	this(matrix, IOptimizer.easyBins(matrix.size()), target, targetWeight);
	}

	public SharpMaximum(List<List<Double>> matrix, int nbBins, double[] target, double targetWeight) {
		this.p_dimension = matrix.size();
		this.p_covarianceMx = DoubleMath.covarianceMx(matrix);
        this.p_returns = DoubleMath.returnMx(matrix);
        
        this.p_numBins = nbBins;
        
        Preconditions.checkArgument(target.length == this.p_dimension, "Individual size is diffrent from problem dimension: %s vs %s", target.length, this.p_dimension);
        this.p_target = target;
        this.p_targetWeight = calculateWeight(targetWeight);
	}

	private double calculateWeight(double targetWeight) {
		double varValue = 0.01, retValue = 0;
		
		for (int i = 0; i < this.p_dimension; i++) {
			varValue += Math.abs(this.p_covarianceMx[i][i]);
    		for (int j = i+1; j < this.p_dimension; j++) {
    			varValue += 2. * Math.abs(this.p_covarianceMx[i][j]);
            }
    		retValue += Math.abs(this.p_returns[i]);
        }
		
		return targetWeight * retValue / varValue;
	}
	
	@Override
    public double evaluate(double[] individual) {
		Preconditions.checkArgument(individual.length == this.p_dimension, "Individual size is diffrent from problem dimension: %s vs %s", individual.length, this.p_dimension);
		
        double varValue  = 0.01, retValue = 0.;
    	for (int i = 0; i < this.p_dimension; i++) {
    		varValue += this.p_covarianceMx[i][i] * individual[i] * individual[i];
    		for (int j = i+1; j < this.p_dimension; j++) {
    			varValue += 2. * this.p_covarianceMx[i][j] * individual[i] * individual[j];
            }
    		retValue += this.p_returns[i] * individual[i];
        }
    	//Unsigned
		varValue = Math.abs(varValue);
		
    	if(DoubleMath.fuzzyEquals(varValue, 0.01, easyprecision))
    		return (Math.abs(retValue) / varValue);
    	
    	return (Math.abs(retValue) / varValue) + (this.p_targetWeight * distance(individual, this.p_target));
    }

	@Override
    public int getDimension() {
        return this.p_dimension;
    }

	@Override
	public Iterator<double[]> getPopulation() {
		return new GenericIterator(this.p_dimension, 0, this.p_numBins, 1./this.p_numBins);
	}
}