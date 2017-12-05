package org.marketcetera.server.sa.config;

import org.marketcetera.util.log.I18NLoggerProxy;
import org.marketcetera.util.log.I18NMessage0P;
import org.marketcetera.util.log.I18NMessageProvider;
import org.marketcetera.util.misc.ClassVersion;

/**
 * The internationalization constants used by this package.
 */
@ClassVersion("$Id: Messages.java 16154 2012-07-14 16:34:05Z colin $")
public interface Messages
{
    /**
     * The message provider.
     */
    static final I18NMessageProvider PROVIDER=
        new I18NMessageProvider("sa_config"); //$NON-NLS-1$

    /**
     * The logger.
     */
    static final I18NLoggerProxy LOGGER=
        new I18NLoggerProxy(PROVIDER);

    /*
     * The messages.
     */
    static final I18NMessage0P NO_MODULE_MANAGER=
        new I18NMessage0P(LOGGER,"no_module_manager"); //$NON-NLS-1$
    static final I18NMessage0P NO_CLASS_LOADER=
        new I18NMessage0P(LOGGER,"no_class_loader"); //$NON-NLS-1$
    static final I18NMessage0P NO_SESSION_MANAGER=
        new I18NMessage0P(LOGGER,"no_session_manager"); //$NON-NLS-1$
    static final I18NMessage0P NO_DATA_PUBLISHER=
        new I18NMessage0P(LOGGER,"no_data_publisher"); //$NON-NLS-1$
}
