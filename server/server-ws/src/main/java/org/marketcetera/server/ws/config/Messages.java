package org.marketcetera.server.ws.config;

import org.marketcetera.util.log.I18NLoggerProxy;
import org.marketcetera.util.log.I18NMessage0P;
import org.marketcetera.util.log.I18NMessage1P;
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
        new I18NMessageProvider("ws_config"); //$NON-NLS-1$

    /**
     * The logger.
     */
    static final I18NLoggerProxy LOGGER=
        new I18NLoggerProxy(PROVIDER);

    /*
     * The messages.
     */
    static final I18NMessage0P NO_BROKERS=
        new I18NMessage0P(LOGGER,"no_brokers"); //$NON-NLS-1$
    static final I18NMessage0P NO_INCOMING_CONNECTION_FACTORY=
        new I18NMessage0P(LOGGER,"no_incoming_connection_factory"); //$NON-NLS-1$
    static final I18NMessage0P NO_OUTGOING_CONNECTION_FACTORY=
        new I18NMessage0P(LOGGER,"no_outgoing_connection_factory"); //$NON-NLS-1$
    static final I18NMessage0P NO_CONTEXT_CLASS_PROVIDER=
            new I18NMessage0P(LOGGER,"no_context_class_provider"); //$NON-NLS-1$
    static final I18NMessage0P NO_ID_FACTORY=
        new I18NMessage0P(LOGGER,"no_id_factory"); //$NON-NLS-1$
    static final I18NMessage0P NO_REPORT_HISTORY_SERVICE=
        new I18NMessage0P(LOGGER,"no_report_history_service"); //$NON-NLS-1$
    static final I18NMessage1P CANNOT_RETRIEVE_JOBS_USER =
            new I18NMessage1P(LOGGER, "cannot_retrieve_jobs_user");  //$NON-NLS-1$
    static final I18NMessage0P NO_JOBS_USER=
            new I18NMessage0P(LOGGER,"no_jobs_user"); //$NON-NLS-1$
    static final I18NMessage0P NO_BASIC_JOBS=
            new I18NMessage0P(LOGGER,"no_basic_jobs"); //$NON-NLS-1$
    static final I18NMessage0P NO_DATA_SOURCE=
            new I18NMessage0P(LOGGER,"no_data_source"); //$NON-NLS-1$
    
    static final I18NMessage0P NO_INSTRUMENTS=
            new I18NMessage0P(LOGGER,"no_instruments"); //$NON-NLS-1$
    static final I18NMessage0P NO_BROKER=
            new I18NMessage0P(LOGGER,"no_broker"); //$NON-NLS-1$
    static final I18NMessage0P NO_PERIOD=
            new I18NMessage0P(LOGGER,"no_period"); //$NON-NLS-1$
    static final I18NMessage0P NO_HISTORY_BACK=
            new I18NMessage0P(LOGGER,"no_history_back"); //$NON-NLS-1$
    
    static final I18NMessage1P JOBS_ERROR_RUN=
            new I18NMessage1P(LOGGER,"jobs_error_run"); //$NON-NLS-1$
}
