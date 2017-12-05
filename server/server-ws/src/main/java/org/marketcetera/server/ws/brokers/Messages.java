package org.marketcetera.server.ws.brokers;

import org.marketcetera.util.log.I18NLoggerProxy;
import org.marketcetera.util.log.I18NMessage0P;
import org.marketcetera.util.log.I18NMessage1P;
import org.marketcetera.util.log.I18NMessage2P;
import org.marketcetera.util.log.I18NMessage3P;
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
        new I18NMessageProvider("ws_brokers"); //$NON-NLS-1$

    /**
     * The logger.
     */
    static final I18NLoggerProxy LOGGER=
        new I18NLoggerProxy(PROVIDER);

    /*
     * The messages.
     */
    static final I18NMessage0P NO_DESCRIPTOR=
        new I18NMessage0P(LOGGER,"no_descriptor"); //$NON-NLS-1$
    static final I18NMessage0P NO_NAME=
        new I18NMessage0P(LOGGER,"no_name"); //$NON-NLS-1$
    static final I18NMessage0P NO_ID=
        new I18NMessage0P(LOGGER,"no_id"); //$NON-NLS-1$
    static final I18NMessage0P NO_SETTINGS=
        new I18NMessage0P(LOGGER,"no_settings"); //$NON-NLS-1$
    static final I18NMessage0P NO_BROKERS=
        new I18NMessage0P(LOGGER,"no_brokers"); //$NON-NLS-1$

    static final I18NMessage1P INVALID_SESSION_ID=
        new I18NMessage1P(LOGGER,"invalid_session_id"); //$NON-NLS-1$
    static final I18NMessage1P INVALID_BROKER_ID=
        new I18NMessage1P(LOGGER,"invalid_broker_id"); //$NON-NLS-1$
    static final I18NMessage1P ANALYZED_MESSAGE=
        new I18NMessage1P(LOGGER,"analyzed_message"); //$NON-NLS-1$
    static final I18NMessage3P BROKER_STRING=
        new I18NMessage3P(LOGGER,"broker_string"); //$NON-NLS-1$
    
    static final I18NMessage0P DK_ERROR_CONNECTION=
            new I18NMessage0P(LOGGER,"dk_error_connection"); //$NON-NLS-1$
    static final I18NMessage0P DK_OK_CONNECTION=
            new I18NMessage0P(LOGGER,"dk_ok_connection"); //$NON-NLS-1$
    static final I18NMessage0P DK_OK_STRATEGY=
            new I18NMessage0P(LOGGER,"dk_ok_strategy"); //$NON-NLS-1$
    static final I18NMessage0P DK_ERROR_DISCONNECTED=
            new I18NMessage0P(LOGGER,"dk_error_disconnected"); //$NON-NLS-1$
    static final I18NMessage0P DK_NO_USERNAME=
            new I18NMessage0P(LOGGER,"dk_no_username"); //$NON-NLS-1$
    static final I18NMessage0P DK_NO_PASSWORD=
            new I18NMessage0P(LOGGER,"dk_no_password"); //$NON-NLS-1$
    static final I18NMessage0P DK_NO_JNLP_URL=
            new I18NMessage0P(LOGGER,"dk_no_jnlp_url"); //$NON-NLS-1$
    
    static final I18NMessage1P DK_ERROR_REQUEST_UNKNOWN=
            new I18NMessage1P(LOGGER,"dk_error_request_unknown"); //$NON-NLS-1$
    static final I18NMessage1P DK_ERROR_REQUEST_PROCESS=
            new I18NMessage1P(LOGGER,"dk_error_request_process"); //$NON-NLS-1$
    static final I18NMessage1P DK_ERROR_RESPONSE_TRADE=
            new I18NMessage1P(LOGGER,"dk_error_response_trade"); //$NON-NLS-1$
    static final I18NMessage2P DK_ERROR_RESPONSE_DATA=
            new I18NMessage2P(LOGGER,"dk_error_response_data"); //$NON-NLS-1$
    static final I18NMessage1P DK_ERROR_TRADE_NOTFOUND=
            new I18NMessage1P(LOGGER,"dk_error_trade_notfound"); //$NON-NLS-1$
    
    static final I18NMessage0P NO_ID_FACTORY=
            new I18NMessage0P(LOGGER,"no_id_factory"); //$NON-NLS-1$
    static final I18NMessage0P NO_REPORT_HISTORY_SERVICE=
            new I18NMessage0P(LOGGER,"no_report_history_service"); //$NON-NLS-1$
    static final I18NMessage0P NO_INSTRUMENTS=
            new I18NMessage0P(LOGGER,"no_instruments"); //$NON-NLS-1$
    static final I18NMessage0P NO_BROKER=
            new I18NMessage0P(LOGGER,"no_broker"); //$NON-NLS-1$
    static final I18NMessage0P NO_PERIOD=
            new I18NMessage0P(LOGGER,"no_period"); //$NON-NLS-1$
    static final I18NMessage0P NO_HISTORY_BACK=
            new I18NMessage0P(LOGGER,"no_history_back"); //$NON-NLS-1$
}
