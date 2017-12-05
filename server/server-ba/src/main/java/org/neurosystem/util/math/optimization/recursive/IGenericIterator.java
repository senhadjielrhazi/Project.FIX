package org.neurosystem.util.math.optimization.recursive;

import java.util.Iterator;

public interface IGenericIterator extends Iterator<double[]> {

	public static Iterator<double[]> modify(Iterator<Double> iterator){
		return new Iterator<double[]>() {
			@Override
			public boolean hasNext() {
				return iterator.hasNext();
			}
			@Override
			public double[] next() {
				return new double[]{iterator.next().doubleValue()};
			}
		};
	}
}
