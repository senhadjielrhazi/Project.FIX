package org.marketcetera.server.ws.brokers;

import org.marketcetera.util.misc.ClassVersion;

/**
 * Handles logout actions to a broker.
 */
@ClassVersion("$Id: LogoutAction.java 16154 2012-07-14 16:34:05Z colin $")
public interface LogoutAction
{
    /**
     * Called upon logout from the given broker. 
     *
     * @param inBroker a <code>Broker</code> value
     */
    public void onLogout(Broker inBroker);
}
