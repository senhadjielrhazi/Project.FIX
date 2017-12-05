package org.marketcetera.yahoo;

import org.marketcetera.util.misc.ClassVersion;
import org.springframework.context.Lifecycle;

/**
 * Provides access to the Yahoo data source.
 */
@ClassVersion("$Id: YahooClient.java 16154 2012-07-14 16:34:05Z colin $")
interface YahooClient
        extends Lifecycle
{
    /**
     * Logs in to the Yahoo data source with the given credentials.
     *
     * @param inURL a <code>String</code> value
     * @return a <code>boolean</code> value
     */
    boolean login(String inURL);
    /**
     * Logs out from the Yahoo data source.
     */
    void logout();
    /**
     * Indicates if the connection is currently logged in or not. 
     *
     * @return a <code>boolean</code> value
     */
    boolean isLoggedIn();
    /**
     * Executes the given request.
     *
     * @param inRequest a <code>YahooRequest</code> value
     */
    void request(YahooRequest inRequest);
    /**
     * Cancels th given request.
     *
     * @param inRequest a <code>YahooRequest</code> value
     */
    void cancel(YahooRequest inRequest);
    /**
     * Gets the current count of requests. 
     *
     * @return a <code>long</code> value
     */
    long getRequestCounter();
    /**
     * Resets the count of requests.
     */
    void resetRequestcounter();
}
