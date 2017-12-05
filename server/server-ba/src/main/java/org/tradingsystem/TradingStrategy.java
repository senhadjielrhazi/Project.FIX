package org.tradingsystem;

import com.dukascopy.api.*;
import org.tradingsystem.AbstractAlgoStrategy;

/*@RequiresFullAccess
@Library("server-ba-2.4.3.jar;")*/
public class TradingStrategy extends AbstractAlgoStrategy {

	@Override
	public void onStart(IContext context) throws JFException {
		super.onStart(context);

		@SuppressWarnings("unused")
		InstrumentParam EURUSD = new InstrumentParam(this, Instrument.EURUSD, 0.007);
	}

	@Override
	public void onBar(Instrument instrument, Period period, IBar askBar,
			IBar bidBar) throws JFException {
		super.onBar(instrument, period, askBar, bidBar);
	}
}