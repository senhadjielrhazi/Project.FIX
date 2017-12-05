package org.neurosystem.util.math.optimization;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.neurosystem.util.common.base.Preconditions;
import org.neurosystem.util.math.optimization.recursive.GenericIterator;

public class TrendMaximum implements IOptimizer {

	private final List<List<Double>> p_matrix;
	
	private final double p_multiplier;
	private final int p_holdingPeriod;
	
	private final int p_dimension;
	private final int p_numBins;
	
	private final double[] p_target;
	private final double p_targetWeight;
	
	public TrendMaximum(List<List<Double>> matrix, double[] target, double targetWeight) {
    	this(matrix, IOptimizer.easyBins(matrix.size()), target, targetWeight);
	}

	public TrendMaximum(List<List<Double>> matrix, int nbBins, double[] target, double targetWeight) {
		this.p_multiplier = 2./15.;
		this.p_holdingPeriod = 48;
		
		this.p_dimension = matrix.size();
		this.p_matrix = matrix;
        
        this.p_numBins = nbBins;
        
        Preconditions.checkArgument(target.length == this.p_dimension, "Individual size is diffrent from problem dimension: %s vs %s", target.length, this.p_dimension);
        this.p_target = target;
        this.p_targetWeight = targetWeight;
	}
	
	@Override
    public double evaluate(double[] individual) {
		Preconditions.checkArgument(individual.length == this.p_dimension, "Individual size is diffrent from problem dimension: %s vs %s", individual.length, this.p_dimension);
		
		//Prices
		double price = 0, nb = 0;
		List<Double> dataVect = new ArrayList<>();
		for(int i = 0; i < this.p_matrix.size(); i++){
			List<Double> prices = this.p_matrix.get(i);
			double beta = individual[i];
			if(i == 0){
				for(int j = 0; j < prices.size(); j++){
					dataVect.add(prices.get(j) * beta);
					price += (j == 0) ? 0. : Math.abs(prices.get(j) - prices.get(j-1)); nb++;
				}
			}else{
				for(int j = 0; j < prices.size(); j++){
					dataVect.set(j, dataVect.get(j) + (prices.get(j) * beta));
					price += (j == 0) ? 0. : Math.abs(prices.get(j) - prices.get(j-1)); nb++;
				}
			}
		}
		price /= nb;
		
		//EMA
		double ema = 0.;
		List<Double> emaVect = new ArrayList<>();
		for(int i = 0; i < dataVect.size(); i++){
			ema += (i == 0) ? dataVect.get(i) : ((dataVect.get(i) - ema) * this.p_multiplier);
			emaVect.add(ema);
		}

		//PNL vs MAE
		double retValue = 0;
		double varValue = 0.01;
		for(int i = 0; i < dataVect.size() - this.p_holdingPeriod; i++){
			if(dataVect.get(i) > emaVect.get(i)){
				retValue += dataVect.get(i + this.p_holdingPeriod) - dataVect.get(i);
				double mae = 0;
				for(int j = 0; j < this.p_holdingPeriod; j++){
					mae = Math.max(-(dataVect.get(i + j) - dataVect.get(i)), mae);
				}
				varValue += mae;
			}else {
				retValue += dataVect.get(i) - dataVect.get(i + this.p_holdingPeriod);
				double mae = 0;
				for(int j = 0; j < this.p_holdingPeriod; j++){
					mae = Math.max(-(dataVect.get(i) - dataVect.get(i + j)), mae);
				}
				varValue += mae;
			}
		}

    	return (retValue / varValue) + (this.p_targetWeight * price * distance(individual, this.p_target));
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