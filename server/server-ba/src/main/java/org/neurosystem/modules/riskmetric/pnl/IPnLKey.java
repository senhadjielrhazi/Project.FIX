package org.neurosystem.modules.riskmetric.pnl;

/**
 * HasRisk
 *
 * NOTE: following the convention of HasRisk.java,
 *       we will set the initial value to the initial price.
 *
 *	LONG:
 *       PNL = (Pf-P0)/P0
 *       MAE = (P0-Pmin)/P0
 *       MFE = (Pmax-P0)/P0
 *       
 *       
 *	SHORT:
 *		PNL = (P0-Pf)/P0
 *		MAE = (Pmax-P0)/P0
 *		MFE = (P0-Pmin)/P0
 *
 * ---------------------------------------------------------------------
 */
public interface IPnLKey {
	
	public double getPNL();
	
	public double getMAE();
	
	public double getMFE();
}
