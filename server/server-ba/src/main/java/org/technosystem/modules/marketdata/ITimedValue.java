package org.technosystem.modules.marketdata;

import org.neurosystem.util.basic.HasTime;
import org.neurosystem.util.basic.HasValue;
import org.neurosystem.util.common.annotations.j2objc.WeakOuter;
import org.neurosystem.util.common.base.Preconditions;
import org.neurosystem.util.misc.Pair;

public interface ITimedValue<V> extends HasTime, HasValue<V> {

	public default <M> ITimedValue<M> keySplit() {

		Preconditions.checkArgument((getValue() instanceof Pair<?, ?>), "Wrong type of TimedValue: %s",
				getValue().getClass());
		
		@SuppressWarnings("unchecked")
		final M split = ((Pair<M, M>) getValue()).getKey();
		final long time = getTime();

		@WeakOuter
		class TimedSplit implements ITimedValue<M> {
			@Override
			public long getTime() {
				return time;
			}

			@Override
			public M getValue() {
				return split;
			}
		}

		return new TimedSplit();
	}
	
	public default <M> ITimedValue<M> valueSplit() {

		Preconditions.checkArgument((getValue() instanceof Pair<?, ?>), "Wrong type of TimedValue: %s",
				getValue().getClass());
		
		@SuppressWarnings("unchecked")
		final M split = ((Pair<M, M>) getValue()).getValue();
		final long time = getTime();

		@WeakOuter
		class TimedSplit implements ITimedValue<M> {
			@Override
			public long getTime() {
				return time;
			}

			@Override
			public M getValue() {
				return split;
			}
		}

		return new TimedSplit();
	}
}
