package org.neurosystem.util.math.optimization.operator;

public class MaxOperator implements IComparator {

	@Override
	public boolean compare(double newBin, double oldBin) {
		return (newBin > oldBin);
	}
}
