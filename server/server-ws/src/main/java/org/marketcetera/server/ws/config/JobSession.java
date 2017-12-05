package org.marketcetera.server.ws.config;

import java.util.Deque;
import java.util.concurrent.TimeUnit;

import org.marketcetera.info.SystemInfo;
import org.marketcetera.util.log.SLF4JLoggerProxy;
import org.marketcetera.util.misc.ClassVersion;
import org.marketcetera.util.ws.stateful.SessionHolder;
import org.marketcetera.util.ws.tags.SessionId;
import org.marketcetera.ws.client.ClientParameters;
import org.marketcetera.ws.client.DataReceiver;

import com.google.common.collect.Lists;

import org.marketcetera.server.ws.security.ClientSession;
import org.marketcetera.ws.server.CoreExecutors;
import org.marketcetera.ws.server.security.SimpleUser;

/**
 * The session information maintained for each client.
 */
@ClassVersion("$Id: ClientSession.java 16154 2012-07-14 16:34:05Z colin $")
public class JobSession extends ClientSession
{
    // INSTANCE DATA.
    private final Deque<DataReceiver> mListeners;
    
    // CONSTRUCTOR.
    /**
     * Creates a new session which uses the given system information
     * to create its session information, and retains the given
     * session ID, the given user associated with the session.
     *
     * @param holder The session holder.
     * @param systemInfo The system information.
     * @param sessionId The session ID.
     * @param user The user.
     */
    public JobSession
        (SessionHolder<ClientSession> holder, 
         SystemInfo systemInfo,
         SessionId sessionId,
         SimpleUser user)
    {
    	super(systemInfo, sessionId, user, null);
    	mListeners=Lists.newLinkedList();
    	
    	CoreExecutors.scheduleAtFixedRate(new Runnable() {
        	// The enclosing run takes care of marking
            // the session as active.
        	@Override
			public void run() {
				holder.markAccess();
                SLF4JLoggerProxy.debug
                    (this,"Sending heartbeat for: {}", //$NON-NLS-1$
                     getSessionId());
			}
        },0,ClientParameters.DEFAULT_HEARTBEAT_INTERVAL,TimeUnit.MILLISECONDS);
    }

    // INSTANCE METHODS.
    public void addDataReceiver(DataReceiver inListener) 
    {
        synchronized (mListeners) {
        	mListeners.addFirst(inListener);
        }
    }

    public void removeDataReceiver(DataReceiver inListener) 
    {
        synchronized (mListeners) {
        	mListeners.removeFirstOccurrence(inListener);
        }
    }
    
    /**
     * Sends the given message to the the receiver.
     *
     * @param message The message.
     */
    @Override
    public void convertAndSend(Object message)
    {
        synchronized (mListeners) {
	    	for(DataReceiver listener: mListeners) {
		    	listener.receiveData(message);
	    	}
        }
    }
}
