package org.neurosystem.util.math.optimization.operator;

public class MinOperator implements IComparator {

	@Override
	public boolean compare(double newBin, double oldBin) {
		return (newBin < oldBin);
	}
}
