package org.neurosystem.util.math.optimization.recursive;

import java.util.Arrays;
import java.util.Iterator;
import java.util.PrimitiveIterator.OfInt;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;

import org.neurosystem.util.common.base.Preconditions;

public class GenericIterator implements IGenericIterator {
	
	private final int p_size;
	private final int p_maxIncluded;
	private final double p_stepFactor;
	
	private final OfInt p_curIterator;
	private int p_bin;
	
	private Iterator<double[]> p_prvIterator;

	public GenericIterator(int size, int minIncluded, int maxIncluded, double stepFactor){		
		Preconditions.checkArgument(size > 1, "RecIterator size is too low: %s", size);
		Preconditions.checkArgument(maxIncluded >= minIncluded, "RecIterator min/max bounds wrong: [max: %s, min: %s]", maxIncluded, minIncluded);
		Preconditions.checkArgument(stepFactor > 0, "Number of steps less than 0");
		
		this.p_size = size;
		this.p_curIterator = IntStream.rangeClosed(minIncluded, maxIncluded).iterator();
		
		this.p_maxIncluded = maxIncluded;
		this.p_stepFactor = stepFactor;
	}
	
	public GenericIterator(int size, int maxIncluded, double stepFactor){		
		this(size, -maxIncluded, maxIncluded, stepFactor);
	}

	@Override
	public boolean hasNext() {
		return (this.p_curIterator.hasNext() || this.p_prvIterator.hasNext());
	}

	@Override
	public double[] next() {
		double[] newValues = new double[this.p_size];
		
		if((this.p_prvIterator == null || !this.p_prvIterator.hasNext()) && this.p_curIterator.hasNext()){
			this.p_bin = this.p_curIterator.next();
			
			if(this.p_size > 2){
				this.p_prvIterator = new GenericIterator(this.p_size - 1, this.p_maxIncluded-Math.abs(this.p_bin), this.p_stepFactor);
			}else{
				double value = (this.p_maxIncluded-Math.abs(this.p_bin)) * this.p_stepFactor;
				this.p_prvIterator = IGenericIterator.modify((value != 0)?DoubleStream.of(value, -value).iterator():DoubleStream.of(value).iterator());
			}
		}else if((this.p_prvIterator != null) && !this.p_prvIterator.hasNext() && !this.p_curIterator.hasNext()){
			throw(new IndexOutOfBoundsException());
		}
		
		newValues[0] = this.p_bin * this.p_stepFactor;
		double[] prvValues = this.p_prvIterator.next();
		for(int i = 1; i < this.p_size; i++){
			newValues[i] = prvValues[i-1];
		}
		
		return newValues;
	}
	
	public static void main(String[]args){
		GenericIterator gen = new GenericIterator(6, 0, 10, 0.1);
		while(gen.hasNext()){
			double[] value = gen.next();
			System.out.println("Generic: " + Arrays.toString(value));
		}
	}
}
