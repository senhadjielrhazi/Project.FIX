package org.neurosystem.modules.marketdata.indicator;

import java.util.ArrayList;
import java.util.List;

import org.neurosystem.util.basic.HasValues;
import org.neurosystem.util.common.annotations.j2objc.WeakOuter;

@SuppressWarnings("unchecked")
public interface IIndicator<V> extends HasValues<ITimedValue<V>> {

	public V calculate();
	
	public default <M> IIndicator<M> keySplit() {
		
		final IIndicator<V> indicator = this;

		@WeakOuter
		class IndicatorSplit implements IIndicator<M> {

			@Override
			public ITimedValue<M> get(int index) {
				return indicator.get(index).keySplit();
			}

			@Override
			public int size() {
				return indicator.size();
			}

			@Override
			public List<ITimedValue<M>> subList(int from, int to) {
				final List<ITimedValue<M>> sub = new ArrayList<>();

				for(int index = from; index < to; index++){
					sub.add(indicator.get(index).keySplit());
				}
				
				return sub;
			}

			@Override
			public M calculate() {
				return (M) indicator.lastValue().keySplit().getValue();
			}
		}

		return new IndicatorSplit();
	}
	
	public default <M> IIndicator<M> valueSplit() {

		final IIndicator<V> indicator = this;

		@WeakOuter
		class IndicatorSplit implements IIndicator<M> {

			@Override
			public ITimedValue<M> get(int index) {
				return indicator.get(index).valueSplit();
			}

			@Override
			public int size() {
				return indicator.size();
			}

			@Override
			public List<ITimedValue<M>> subList(int from, int to) {
				final List<ITimedValue<M>> sub = new ArrayList<>();
				
				for(int index = from; index < to; index++){
					sub.add(indicator.get(index).valueSplit());
				}
				
				return sub;
			}

			@Override
			public M calculate() {
				return (M) indicator.lastValue().valueSplit().getValue();
			}
		}

		return new IndicatorSplit();
	}
}
