package org.marketcetera.server.ws.brokers;

import org.marketcetera.util.misc.ClassVersion;

/**
 * Handles logon actions to a broker.
 */
@ClassVersion("$Id: LogonAction.java 16154 2012-07-14 16:34:05Z colin $")
public interface LogonAction
{
    /**
     * Called upon successful logon to the given broker. 
     *
     * @param inBroker a <code>Broker</code> value
     */
    public void onLogon(Broker inBroker);
}
