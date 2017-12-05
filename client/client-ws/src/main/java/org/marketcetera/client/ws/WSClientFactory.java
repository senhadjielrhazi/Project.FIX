package org.marketcetera.client.ws;

import org.marketcetera.util.misc.ClassVersion;
import org.marketcetera.ws.client.ClientInitException;
import org.marketcetera.ws.client.ConnectionException;

/**
 * Factory used to create a {@link WSClient} object.
 */
@ClassVersion("$Id: WSClientFactory.java 16154 2012-07-14 16:34:05Z colin $")
public interface WSClientFactory
{
    /**
     * Gets a <code>WSClient</code>.
     * 
     * @return a <code>WSClient</code> value
     * @throws ConnectionException if there were errors connecting to the server.
     * @throws ClientInitException if the client is already initialized.
     */
    public WSClient getClient()
            throws ClientInitException, ConnectionException;
}