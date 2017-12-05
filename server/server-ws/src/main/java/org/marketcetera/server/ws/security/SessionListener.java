package org.marketcetera.server.ws.security;

import org.marketcetera.util.misc.ClassVersion;

/**
 * A listener for added and deleted sessions.
 */

@ClassVersion("$Id: SessionListener.java")
public interface SessionListener {
	
    /**
     * Adds the given session to the receiver.
     *
     * @param session The session.
     */
    public void addSession
        (ClientSession session);
    
    /**
     * Removes the given session from the receiver.
     *
     * @param session The session.
     */
    public void removedSession
        (ClientSession session);
}
