package org.marketcetera.server.ws;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import org.marketcetera.info.SessionInfo;
import org.marketcetera.persist.PersistenceException;
import org.marketcetera.trade.UserID;
import org.marketcetera.util.log.SLF4JLoggerProxy;
import org.marketcetera.util.misc.ClassVersion;
import org.marketcetera.util.ws.stateful.SessionFactory;
import org.marketcetera.util.ws.stateful.SessionHolder;
import org.marketcetera.util.ws.stateful.SessionManager;
import org.marketcetera.util.ws.tags.SessionId;
import org.marketcetera.ws.ResponseMessage;

import com.google.common.collect.Sets;

import org.marketcetera.server.ws.security.ClientSession;
import org.marketcetera.server.ws.security.SessionListener;
import org.marketcetera.ws.server.security.SimpleUser;
import org.marketcetera.ws.server.security.SingleSimpleUserQuery;

/**
 * A manager of the set of connected users (active sessions). It also
 * routes Server replies to the clients.
 */
@ClassVersion("$Id: ServerManager.java 16154 2012-07-14 16:34:05Z colin $")
public class ServerManager extends SessionManager<ClientSession> implements SessionListener
{
    // INSTANCE DATA.
    private final Map<UserID,Set<ClientSession>> mUserIDMap;

    // CONSTRUCTORS.
    /**
     * Creates a new server manager whose sessions are created by the
     * given factory, and which have the given lifespan, in ms.
     * @param sessionFactory The session factory. It may be null.
     * @param sessionLife The lifespan. Use {@link
     * #INFINITE_SESSION_LIFESPAN} for an infinite lifespan.
     */
    public ServerManager(SessionFactory<ClientSession> sessionFactory,
            long sessionLife)
    {
    	super(sessionFactory,sessionLife);    	
        mUserIDMap=new ConcurrentHashMap<UserID,Set<ClientSession>>();
    }

    // INSTANCE METHODS.
    /**
     * Returns the receiver's map of user IDs to (one or more)
     * sessions. Individual sessions are removed when they become
     * inactive; map entries remain in this map for as long as at
     * least one session is active for a user.
     *
     * @return The map.
     */
    private Map<UserID,Set<ClientSession>> getUserIDMap()
    {
        return mUserIDMap;
    }
    
    /**
     * Returns the receiver's set of user IDs. A user
     * belongs in this set for as long as they are
     * with one or more active sessions.
     *
     * @return The set.
     */
	public Collection<UserID> getUserIDs() {
		return getUserIDMap().keySet();
	}
	
    /**
     * Updates the receiver's data structures to reflect the current
     * user definitions in the database.
     */
    public synchronized void sync()
    {
        Set<UserID> allUserIDs=Sets.newHashSet(getUserIDs());
        for (UserID userID:allUserIDs) {
            // Assume user is nonexistent/inactive.
            SimpleUser user=null;
            try {
                user=new SingleSimpleUserQuery(userID.getValue()).fetch();
            } catch (PersistenceException ex) {
                // Ignored: user remains null.
            }
            if ((user!=null) && (!user.isActive())) {
            	// User is nonexistent/inactive: remove all their sessions.
                Set<ClientSession> sessions=getUserIDMap().get(userID);
                for (ClientSession s:sessions) {
                    remove(s.getSessionId());
                }
            }
        }
        logStatus();
    }

    /**
     * Adds the given session to the receiver.
     *
     * @param session The session.
     */
    @Override
    public synchronized void addSession
        (ClientSession session)
    {
        SimpleUser user=session.getUser();
        UserID userID=user.getUserID();
        Set<ClientSession> sessions=getUserIDMap().get(userID);
        if (sessions==null) {
            sessions=new CopyOnWriteArraySet<ClientSession>();
            getUserIDMap().put(userID,sessions);
            System.out.println("Added Session: " + session);
        }
        sessions.add(session);
        logStatus();
    }

    /**
     * Removes the given session from the receiver.
     *
     * @param session The session.
     */
    @Override
    public synchronized void removedSession
        (ClientSession session)
    {
        UserID userID=session.getUser().getUserID();
        Set<ClientSession> sessions=getUserIDMap().get(userID);
        sessions.remove(session);
        if (sessions.size()==0) {
            getUserIDMap().remove(userID);
            System.out.println("Removed Session: " + session);
        }
        logStatus();
    }      

    /**
     * Returns the session information associated with the given
     * session ID.
     *
     * @param sessionId The session ID.
     *
     * @return The information. It may be null if the session has expired.
     */
    public SessionInfo getSessionInfo
        (SessionId sessionId)
    {
        SessionHolder<ClientSession> holder=get(sessionId);
        if (holder==null) {
            return null;
        }
        return holder.getSession().getSessionInfo();
    }
    
    /**
     * Sends the given message to all the sessions managed by
     * the receiver.
     *
     * @param msg The message.
     */
    public void convertAndBroadcast
        (ResponseMessage msg)
    {
		for (UserID userID:getUserIDMap().keySet()) {
    		convertAndSend(msg, userID);
        }
    }
    
    /**
     * Sends the given message to the actor's sessions managed by
     * the receiver.
     *
     * @param msg The message.
     * @param actorID the actor ID
     */
    public void convertAndSend
        (ResponseMessage msg, UserID actorID)
    {
        // Sessions for the actor
        Set<ClientSession> sessions=getUserIDMap().get(actorID);
        if (sessions!=null) {
            for (ClientSession s:sessions) {
                s.convertAndSend(msg);
            }
        }
    }
    
    /**
     * Logs the receiver's status.
     */
    public void logStatus()
    {
        SLF4JLoggerProxy.debug(this,"User ID map"); //$NON-NLS-1$
        for (Map.Entry<UserID,Set<ClientSession>> e:
             getUserIDMap().entrySet()) {
            SLF4JLoggerProxy.debug
                (this," User ID: {}",e.getKey()); //$NON-NLS-1$
            for (ClientSession s:e.getValue()) {
                SLF4JLoggerProxy.debug(this,"  {}",s); //$NON-NLS-1$
            }
        }
    }
}
