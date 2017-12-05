package org.marketcetera.client.sa;

import org.marketcetera.util.misc.ClassVersion;
import org.marketcetera.ws.client.ClientParameters;


/**
 * Factory for creating remote connections to the Strategy Agent.
 */
@ClassVersion("$Id: SAClientFactory.java 16901 2014-05-11 16:14:11Z colin $")
public class SAClientFactory
{
    /**
     * Creates an <code>SAClient</code> instance.
     *
     * @param inParameters an <code>ClientParameters</code> value
     * @return an <code>SAClient</code> value
     */
    public SAClient create(ClientParameters inParameters)
    {
        return new SAClientImpl(inParameters);
    }
    /**
     * Returns the singleton factory instance that can be used to
     * create clients to communicate with the remote strategy agents.
     *
     * @return the singleton factory instance.
     */
    public static SAClientFactory getInstance()
    {
        return sInstance;
    }
    
    /**
     * Creates an instance.
     */
    protected SAClientFactory() {}
    /**
     * The singleton factory instance.
     */
    private final static SAClientFactory sInstance = new SAClientFactory();
}
