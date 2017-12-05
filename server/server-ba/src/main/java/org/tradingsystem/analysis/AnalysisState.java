package org.tradingsystem.analysis;

import java.util.ArrayList;
import java.util.List;

public class AnalysisState implements IAnalysisState{

	private long p_time;
	private double p_price;
	private double p_pnl;
	private List<List<Double>> p_aiData = new ArrayList<List<Double>>();

	public AnalysisState(long barTime, double price,
			List<List<Double>> aiDataVectors) {
		p_pnl = 0.0;
		updateParameters(barTime, price, p_pnl, aiDataVectors);
	}
	
	@Override
	public void updateParameters(long barTime, double price, double pnl,
			List<List<Double>> aiDataVectors) {
		p_time = barTime;
		p_price = price;
		p_pnl = pnl;
		p_aiData.clear();
		for(List<Double> dataList:aiDataVectors){
			List<Double> data = new ArrayList<Double>();
			data.addAll(dataList);
			p_aiData.add(data);
		}
	}
	
	@Override
	public long getTime() {
		return p_time;
	}
	
	@Override
	public double getPrice() {
		return p_price;
	}
	
	@Override
	public List<List<Double>> getAIData() {
		return p_aiData;
	}

	@Override
	public double getPnL() {
		return p_pnl;
	}

	@Override
	public String to_String() {
		String data = "";
		data = " P: " + p_price
				+ " PnL: " + p_pnl + " VECT: " + getMapFormated(p_aiData);
		return data;
	}
	
	public String getListFormated(List<Double> arr){
		String str = "[ ";
        for (int r = 0; r < arr.size(); r++) {
            str += arr.get(r) + "; ";
        }
        return str+"]";
	}
	
	public String getMapFormated(List<List<Double>> arr){
		String str = "[ ";
        for (int r = 0; r < arr.size(); r++) {
            str += getListFormated(arr.get(r)) + "; ";
        }
        return str+"]";
	}

	@Override
	public boolean equalDataVector(List<List<Double>> allAIDataVector) {
		if(getAIData().size() != allAIDataVector.size()){return false;}
		for(int i = 0; i < allAIDataVector.size(); i++){
			if(getAIData().get(i).size() != allAIDataVector.get(i).size()){return false;}
			for(int j = 0; j < allAIDataVector.get(i).size(); j++){
				if(!allAIDataVector.get(i).get(j).equals(getAIData().get(i).get(j))){
					return false;
				}
			}
		}
		return true;
	}
}