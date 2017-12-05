package org.neurosystem.util.basic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public interface HasValues<V> {
	
	public final static int easybites = 2000;
	
	public V get(int index);
	
	public int size();
	
	public List<V> subList(int from, int to);
	
	public default List<V> valueList(int nbBack) {
		if(size() < nbBack){
			final List<V> values = new ArrayList<>();
			
			for(int i = 0; i < nbBack-size(); i++){
				values.add(firstValue());
			}
			for(int i = nbBack-size(); i < nbBack; i++){
				values.add(get(i-nbBack+size()));
			}
			return values;
		}
		
		return subList(size()-nbBack, size());
	}

	public default V valueForward(int nb){
		return get(nb);
	}
	
	public default V valueBack(int nb){
		return get(Math.max(size()-nb-1, 0));
	}
	
	public default V firstValue(){
		return get(0);
	}
	
	public default V lastValue(){
		return get(size()-1);
	}
	
	public static String formatedValues(List<?> values){
	    return Arrays.toString(values.toArray());
	}
}
