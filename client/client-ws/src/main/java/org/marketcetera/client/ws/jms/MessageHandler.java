package org.marketcetera.client.ws.jms;

import org.marketcetera.util.misc.ClassVersion;

/**
 * A type-safe message handler.
 *
 * @author tlerios@marketcetera.com
 * @since 1.0.0
 * @version $Id: MessageHandler.java 16154 2012-07-14 16:34:05Z colin $
 */

/* $License$ */

@ClassVersion("$Id: MessageHandler.java 16154 2012-07-14 16:34:05Z colin $")
public interface MessageHandler<T>
{
    /**
     * Handles the given message. Changing the name of this method
     * requires changing the implementation of {@link
     * IncomingJmsFactory} as well.
     *
     * @param msg The message.
     */

    void receiveMessage
        (T msg);
}
