package org.technosystem.modules.marketdata.assets;

public interface ITicker {
	
	public double getUnits();
	
	public String getSymbol();
	
	public String getBase();

	public String getTerm();
	
	public double getPipValue();
	
	public double getMinUnits();
	
	public double roundSZ(double size);
	
	public double roundPZ(double price);
	
	public default double roundPZ(double initP, double pips){
		return roundPZ(initP + (pips * getPipValue()));
	}
}
