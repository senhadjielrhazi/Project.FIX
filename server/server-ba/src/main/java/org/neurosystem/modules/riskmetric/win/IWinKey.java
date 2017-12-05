package org.neurosystem.modules.riskmetric.win;

public interface IWinKey {
	
	public double getTNB();
	
	public double getPNL();
	
	public double getPWP();
	
	public default boolean compare(IWinKey winKey) {
		if(getPNL() >= winKey.getPNL() && getPWP() >= winKey.getPWP()
				&& getTNB() >= winKey.getTNB()){
			return true;
		}
		return false;
	}
}
