package org.marketcetera.server.sa.security;

import org.marketcetera.util.misc.ClassVersion;
import org.marketcetera.util.ws.stateful.SessionFactory;
import org.marketcetera.util.ws.stateless.StatelessClientContext;
import org.marketcetera.util.ws.tags.SessionId;

/**
 * A session factory for WS sessions.
 */
@ClassVersion("$Id")
class ClientSessionFactory implements SessionFactory<ClientSession> {

    /**
     * Creates a new session factory which uses the given JMS manager
     * to create reply topics, and which notifies the given user
     * manager when sessions are added/removed.
     */
    public ClientSessionFactory() {
    }

    @Override
    public ClientSession createSession
            (StatelessClientContext context,
             String user, SessionId id) {
        return new ClientSession();
    }

    @Override
    public void removedSession(ClientSession session) {
    }

	@Override
	public void addSession(ClientSession session) {
	}
}