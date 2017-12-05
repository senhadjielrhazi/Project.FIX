package org.tradingsystem;

import java.util.*;
import com.dukascopy.api.*;

public interface IRefreshData {
	public List<Period> getRefPeriods();

	public void refreshMData(Period period, long barTime) throws JFException;
}
