package org.marketcetera.client.sa.jms;

import org.marketcetera.module.ModuleInfo;
import org.marketcetera.util.misc.ClassVersion;
import org.marketcetera.util.ws.ContextClassProvider;

/**
 * Provides context classes for marshalling and unmarshalling SAClient messages.
 */
@ClassVersion("$Id: SAContextClassProvider.java 16901 2014-05-11 16:14:11Z colin $")
public class SAContextClassProvider
        implements ContextClassProvider
{
    /* (non-Javadoc)
     * @see org.marketcetera.util.ws.ContextClassProvider#getContextClasses()
     */
    @Override
    public Class<?>[] getContextClasses()
    {
        return CLASSES;
    }
    /**
     * class list to return
     */
    private static final Class<?>[] CLASSES = new Class<?>[] { ModuleInfo.class, StrategyParameters.class };
    /**
     * instance value
     */
    public static final SAContextClassProvider INSTANCE = new SAContextClassProvider();
}
