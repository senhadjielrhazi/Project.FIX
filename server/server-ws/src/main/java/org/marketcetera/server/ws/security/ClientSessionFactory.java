package org.marketcetera.server.ws.security;

import java.util.Deque;

import javax.xml.bind.JAXBException;

import org.marketcetera.info.SystemInfo;
import org.marketcetera.persist.PersistenceException;
import org.marketcetera.util.except.I18NRuntimeException;
import org.marketcetera.util.log.I18NBoundMessage1P;
import org.marketcetera.util.misc.ClassVersion;
import org.marketcetera.util.ws.stateful.SessionFactory;
import org.marketcetera.util.ws.stateless.StatelessClientContext;
import org.marketcetera.util.ws.tags.SessionId;
import org.marketcetera.client.ws.jms.JmsManager;
import org.marketcetera.ws.server.security.SimpleUser;
import org.marketcetera.ws.server.security.SingleSimpleUserQuery;
import org.springframework.jms.core.JmsOperations;

import com.google.common.collect.Lists;

/**
 * WS session factory.
 */
@ClassVersion("$Id: ClientSessionFactory.java 16154 2012-07-14 16:34:05Z colin $")
public class ClientSessionFactory
    implements SessionFactory<ClientSession>
{
    // INSTANCE DATA.
    private final SystemInfo mSystemInfo;
    private final JmsManager mJmsManager;
    private final Deque<SessionListener> mListeners;
    
    // CONSTRUCTORS.

    /**
     * Creates a new session factory which uses the given system
     * information to create session information, which uses the given
     * JMS manager to create reply topics, and which notifies the
     * given user manager when sessions are added/removed.
     *
     * @param systemInfo The system information.
     * @param jmsManager The JMS manager.
     */
    public ClientSessionFactory
        (SystemInfo systemInfo,
         JmsManager jmsManager)
    {
        mSystemInfo=systemInfo;
        mJmsManager=jmsManager;
        mListeners=Lists.newLinkedList();
    }

    // INSTANCE METHODS.
    public void addSessionListener(SessionListener inListener) 
    {
        synchronized (mListeners) {
        	mListeners.addFirst(inListener);
        }
    }

    public void removeSessionListener(SessionListener inListener) 
    {
        synchronized (mListeners) {
        	mListeners.removeFirstOccurrence(inListener);
        }
    }
    
    /**
     * Returns the receiver's system information.
     *
     * @return The information.
     */
    public SystemInfo getSystemInfo()
    {
        return mSystemInfo;         
    }

    /**
     * Returns the receiver's JMS manager.
     *
     * @return The manager.
     */
    private JmsManager getJmsManager()
    {
        return mJmsManager;
    }

    // SessionFactory.
    @Override
    public ClientSession createSession
        (StatelessClientContext context,
         String user, SessionId id)
    {
        JmsOperations jmsOps;
        SimpleUser dbUser;
        String topicName=JmsManager.getReplyTopicName(id);
        try {
            jmsOps=getJmsManager().getOutgoingJmsFactory().createJmsTemplate
                (topicName,true);
        } catch (JAXBException ex) {
            throw new I18NRuntimeException
                (ex,new I18NBoundMessage1P
                 (Messages.CANNOT_CREATE_REPLY_TOPIC,topicName));
        }
        try {
            dbUser=(new SingleSimpleUserQuery(user)).fetch();
        } catch (PersistenceException ex) {
            throw new I18NRuntimeException
                (ex,new I18NBoundMessage1P
                 (Messages.CANNOT_RETRIEVE_USER,user));
        }
        
        ClientSession session=new ClientSession
            (getSystemInfo(),id,dbUser,jmsOps);
        addSession(session);
        
        return session;
    }


	@Override
	public void addSession(ClientSession session) {
        synchronized (mListeners) {
	        for(SessionListener listener: mListeners) {
	        	listener.addSession(session);
	        }
        }
	}
	
    @Override
    public void removedSession(ClientSession session)
    {
        for(SessionListener listener: mListeners) {
        	listener.removedSession(session);
        }
    }
}
