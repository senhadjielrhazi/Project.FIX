package org.neurosystem.modules.neuroscience.classification;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.FutureTask;

import org.neurosystem.modules.neuroscience.dna.INucleic;
import org.neurosystem.modules.neuroscience.mutation.Deletion;
import org.neurosystem.modules.neuroscience.mutation.IMorphism;
import org.neurosystem.modules.neuroscience.mutation.Morphism;
import org.neurosystem.modules.neuroscience.statistics.IMetric;
import org.neurosystem.modules.neuroscience.statistics.Metric;
import org.neurosystem.modules.riskmetric.metric.IMetricKey;
import org.neurosystem.modules.riskmetric.trade.ITradeKey;
import org.neurosystem.modules.riskmetric.win.IWinKey;
import org.neurosystem.util.basic.HasSide.Side;
import org.neurosystem.util.basic.HasTime;
import org.neurosystem.util.common.annotations.javax.Nonnull;
import org.neurosystem.util.common.collect.Maps;
import org.neurosystem.util.misc.Pair;

public class Classifier implements IClassifier {
	
	private final int p_geneSZ;
	private final ExecutorService p_executor;
	
	private final Map<Integer, Map<INucleic, Pair<IMetric, IMetric>>> p_metrics;
	
	public Classifier(@Nonnull int geneSZ, @Nonnull ExecutorService executor) {
		this.p_geneSZ = geneSZ;
		this.p_executor = executor;
		
		this.p_metrics = new HashMap<>();
	}

	@Override
	public int size() {
		return getMetrics().size();
	}

	private Map<Integer, Map<INucleic, Pair<IMetric, IMetric>>> getMetrics() {
		return this.p_metrics;
	}

	private ExecutorService executor() {
		return this.p_executor;
	}
	
	protected int getGeneSZ() {
		return this.p_geneSZ;
	}
	
	@Override
	public void addEntry(@Nonnull Pair<INucleic, ITradeKey> entry) {
		INucleic key = entry.getKey();
		ITradeKey trade = entry.getValue();
		Integer tzValue = HasTime.timeZone(trade.getTime());
		
		Map<INucleic, Pair<IMetric, IMetric>> metrics;
		if(!this.p_metrics.containsKey(tzValue)){
			metrics = new HashMap<>();
			this.p_metrics.put(tzValue, metrics);
		}else{
			metrics = this.p_metrics.get(tzValue);
		}
		
		Pair<IMetric, IMetric> pair;
		if(!metrics.containsKey(key)){
			pair = new Pair<>(new Metric(), new Metric());
			metrics.put(key, pair);
		}else{
			pair = metrics.get(key);
		}
		
		if(trade.getSide().isLong()) {
			pair.getKey().addEntry(trade);
		}else {
			pair.getValue().addEntry(trade);
		}
	}

	@Override
	public IHeuristic classification(IWinKey winKey) {
		IMorphism morphism = new Morphism();

		List<Integer> prvSureKeys = new ArrayList<>(), deletedValues = new ArrayList<>();
		for(Integer idx = 0; idx < getGeneSZ(); idx++){
			if(idx < getGeneSZ() / 3.){
				prvSureKeys.add(idx);
			}else{
				deletedValues.add(idx);
			}
		}
		if(deletedValues.size() != 0){
			morphism.addMutation(new Deletion(deletedValues));
		}
		
		IHeuristic prvResult = training(morphism, winKey);
		
		/*log("");
		log(String.format("I: %s, Mutation: %s", prvResults, morphism));*/
		
		//Selection
		boolean sucess = false;
		for(Integer idx = 0; idx < getGeneSZ(); idx++){
			if(prvSureKeys.contains(idx)){
				prvSureKeys.remove(idx);
				deletedValues.add(idx);
				
				morphism = new Morphism();
				if(deletedValues.size() != 0){
					morphism.addMutation(new Deletion(deletedValues));
				}
				
				IHeuristic newResult = training(morphism, winKey);
				if(compare(newResult, prvResult)){
					prvResult = newResult;
					sucess = true;
					
					/*log("");
					log(String.format("D: %s, Mutation: %s", prvResults, morphism));*/
				}else{
					prvSureKeys.add(idx);
					deletedValues.remove(idx);
				}
			}else{
				prvSureKeys.add(idx);
				deletedValues.remove(idx);
				
				morphism = new Morphism();
				if(deletedValues.size() != 0){
					morphism.addMutation(new Deletion(deletedValues));
				}
				
				IHeuristic newResult = training(morphism, winKey);
				if(compare(newResult, prvResult)){
					prvResult = newResult;
					sucess = true;
					
					/*log("");
					log(String.format("A: %s, Mutation: %s", prvResults, morphism));*/
				}else{
					prvSureKeys.remove(idx);
					deletedValues.add(idx);
				}
			}
			
			if(idx.equals(getGeneSZ() - 1) && sucess){
				sucess = false;
				idx = 0;
			}
		}
	
		/*log("");
		log(String.format("A: %s, Mutation: %s", prvResult, morphism));*/
		
		return prvResult;
	}
	
	@Override
	public IHeuristic training(@Nonnull IMorphism morphism, @Nonnull IWinKey winKey) {
		Map<Integer, Map<INucleic, Pair<IMetric, IMetric>>> metrics =  deCodeInput(executor(), morphism, getMetrics());
		
		Map<Integer, Map<INucleic, Side>> keys = new HashMap<>();
		IMetric keyMetric = new Metric();

		for (Iterator<Entry<Integer, Map<INucleic, Pair<IMetric, IMetric>>>> itz = metrics.entrySet().iterator(); itz.hasNext();) {
			Entry<Integer, Map<INucleic, Pair<IMetric, IMetric>>> entrytz = itz.next();
			Integer tzValue = entrytz.getKey();
			
			Map<INucleic, Side> sides;
			if(!keys.containsKey(tzValue)){
				sides = new HashMap<>();
				keys.put(tzValue, sides);
			}else{
				sides = keys.get(tzValue);
			}
			
			for (Iterator<Entry<INucleic, Pair<IMetric, IMetric>>> itn = entrytz.getValue().entrySet().iterator(); itn.hasNext();) {
				Entry<INucleic, Pair<IMetric, IMetric>> entryn = itn.next();
				INucleic key = entryn.getKey();
				Pair<IMetric, IMetric> pair = entryn.getValue();
				
				if (pair.getKey().compare(winKey)) {
					if (!pair.getValue().compare(winKey)) {
						sides.put(key, Side.LONG);
						keyMetric.addMetric(pair.getKey());
					}
				}else{
					if (pair.getValue().compare(winKey)) {
						sides.put(key, Side.SHORT);
						keyMetric.addMetric(pair.getValue());
					}
				}
			}
		}

		return new Heuristic(morphism, keys, keyMetric);
	}
	
	private static final Map<Integer, Map<INucleic, Pair<IMetric, IMetric>>> deCodeInput(ExecutorService executor, IMorphism morphism,
			Map<Integer, Map<INucleic, Pair<IMetric, IMetric>>> input) {
		
		List<FutureTask<Entry<Integer, Map<INucleic, Pair<IMetric, IMetric>>>>> taskList = new ArrayList<>();
		for (Iterator<Entry<Integer, Map<INucleic, Pair<IMetric, IMetric>>>> itr = input.entrySet().iterator(); itr.hasNext();) {
			Entry<Integer, Map<INucleic, Pair<IMetric, IMetric>>> entry = itr.next();
			Integer tzValue = entry.getKey();
			Map<INucleic, Pair<IMetric, IMetric>> entryMap = entry.getValue();
			
			FutureTask<Entry<Integer, Map<INucleic, Pair<IMetric, IMetric>>>> task = new FutureTask<>(
					new Callable<Entry<Integer, Map<INucleic, Pair<IMetric, IMetric>>>>() {
						@Override
						public Entry<Integer, Map<INucleic, Pair<IMetric, IMetric>>> call() {
							Map<INucleic, Pair<IMetric, IMetric>> decodMap = new HashMap<>();
							Pair<IMetric, IMetric> pair;

							for (Iterator<Entry<INucleic, Pair<IMetric, IMetric>>> it = entryMap.entrySet()
									.iterator(); it.hasNext();) {
								Entry<INucleic, Pair<IMetric, IMetric>> entry = it.next();

								INucleic newKey = morphism.morph(entry.getKey());
								if (!decodMap.containsKey(newKey)) {
									pair = new Pair<>(new Metric(), new Metric());
									decodMap.put(newKey, pair);
								} else {
									pair = decodMap.get(newKey);
								}

								pair.getKey().addMetric(entry.getValue().getKey());
								pair.getValue().addMetric(entry.getValue().getValue());
							}
							return Maps.immutableEntry(tzValue, decodMap);
						}
					});
			taskList.add(task);
			executor.execute(task);
		}

		Map<Integer, Map<INucleic, Pair<IMetric, IMetric>>> output = new HashMap<>();
		try {
			for (FutureTask<Entry<Integer, Map<INucleic, Pair<IMetric, IMetric>>>> task : taskList) {
				Entry<Integer, Map<INucleic, Pair<IMetric, IMetric>>> entry = task.get();
				output.put(entry.getKey(), entry.getValue());
			}
		} catch (Exception e) {
			System.out.println("DeCodeInput error: " + e);
			e.printStackTrace(System.out);
		}
		taskList.clear();
		
		return output;
	}
	
	private boolean compare(IMetricKey newSum, IMetricKey prvSum) {
		double epsilon = 1., omega = 0.01;
		if((newSum.getPWP() > prvSum.getPWP() + epsilon) && (newSum.getTPL() > prvSum.getTPL() + epsilon)){
			return true;
		}
		if((newSum.getPWP() > prvSum.getPWP() - epsilon) && (newSum.getTPL() > prvSum.getTPL() + omega)){
			return true;
		}

		return false;
	}

	protected static void log(String message) {
		System.out.println(message);
		System.err.println(message);
	}
}
