package org.marketcetera.client.ws.jms;

import org.marketcetera.util.misc.ClassVersion;
import org.marketcetera.util.log.*;

/* $License$ */
/**
 * Internationalized messages used by this package.
 *
 * @author anshul@marketcetera.com
 * @version $Id: Messages.java 16154 2012-07-14 16:34:05Z colin $
 * @since 1.0.0
 */
@ClassVersion("$Id: Messages.java 16154 2012-07-14 16:34:05Z colin $")
public interface Messages {
    /**
     * The message provider
     */
    static final I18NMessageProvider PROVIDER =
            new I18NMessageProvider("wsclient_jms",  //$NON-NLS-1$ 
                    Messages.class.getClassLoader());
    /**
     * The message logger.
     */
    static final I18NLoggerProxy LOGGER =
            new I18NLoggerProxy(PROVIDER);
            
    static final I18NMessage1P UNEXPECTED_MESSAGE_TO_SEND =
            new I18NMessage1P(LOGGER, "unexpected_message_to_send");   //$NON-NLS-1$
    static final I18NMessage1P UNEXPECTED_MESSAGE_RECEIVED =
            new I18NMessage1P(LOGGER, "unexpected_message_received");   //$NON-NLS-1$
    static final I18NMessage1P ERROR_CONVERTING_MESSAGE_TO_OBJECT =
            new I18NMessage1P(LOGGER, "error_converting_message_to_object");   //$NON-NLS-1$
    static final I18NMessage1P ERROR_CONVERTING_OBJECT_TO_MESSAGE =
            new I18NMessage1P(LOGGER, "error_converting_object_to_message");   //$NON-NLS-1$
    static final I18NMessage1P ERROR_JMS_MESSAGE_CONVERSION = 
        new I18NMessage1P(LOGGER,"error_jms_message_conversion"); //$NON-NLS-1$
    
    static final I18NMessage2P REQUEST_ENVELOPE_TO_STRING =
            new I18NMessage2P(LOGGER,"request_envelope_to_string"); //$NON-NLS-1$
}
