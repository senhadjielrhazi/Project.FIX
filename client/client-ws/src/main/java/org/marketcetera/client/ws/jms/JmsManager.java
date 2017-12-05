package org.marketcetera.client.ws.jms;

import javax.jms.ConnectionFactory;
import javax.jms.ExceptionListener;
import org.marketcetera.util.misc.ClassVersion;
import org.marketcetera.util.ws.ContextClassProvider;
import org.marketcetera.util.ws.tags.SessionId;

/**
 * A Spring-wrapped JMS connection manager.
 *
 * @author tlerios@marketcetera.com
 * @since 1.0.0
 * @version $Id: JmsManager.java 16154 2012-07-14 16:34:05Z colin $
 */

/* $License$ */

@ClassVersion("$Id: JmsManager.java 16154 2012-07-14 16:34:05Z colin $")
public class JmsManager
{
	// INSTANCE DATA.

    private final IncomingJmsFactory mIncomingJmsFactory;
    private final OutgoingJmsFactory mOutgoingJmsFactory;

	 /**
     * The prefix of the topic on which the client receives server
     * replies.
     */
    private static final String REPLY_TOPIC_PREFIX=
        "server-messages-"; //$NON-NLS-1$

    /**
     * The queue on which the client places request for the server.
     */
    private static final String REQUEST_QUEUE=
        "server-commands"; //$NON-NLS-1$

    /**
     * Returns the topic name for replies sent from the server to the
     * client, given the session ID.
     *
     * @param id The ID.
     *
     * @return The topic name.
     */
    public static String getReplyTopicName(SessionId id)
    {
        return REPLY_TOPIC_PREFIX + id.getValue();
    }
    
    /**
     * Returns the topic name for request queue sent from the server to the
     * client.
     *
     * @return The topic name.
     */
    public static String getRequestTopicName()
    {
        return REQUEST_QUEUE;
    }

    // CONSTRUCTORS.

    /**
     * Creates a new manager that uses the given standard JMS
     * connection factories to create factories for Spring-wrapped
     * connections, and directs exceptions to the given listener, if
     * any.
     *
     * @param incomingCF The incoming factory.
     * @param outgoingCF The outgoing factory.
     * @param contextClassProvider The context ClassProvider.
     * @param exceptionListener The listener. It may be null.
     */

    public JmsManager
        (ConnectionFactory incomingCF,
         ConnectionFactory outgoingCF,
         ContextClassProvider contextClassProvider,
         ExceptionListener exceptionListener)
    {
        mIncomingJmsFactory=new IncomingJmsFactory
            (incomingCF,contextClassProvider,exceptionListener);
        mOutgoingJmsFactory=new OutgoingJmsFactory
        	(outgoingCF,contextClassProvider);
    }

    /**
     * Creates a new manager that uses the given standard JMS
     * connection factories to create factories for Spring-wrapped
     * connections. No custom exception listener is used.
     *
     * @param incomingCF The incoming factory.
     * @param outgoingCF The outgoing factory.
     */

    public JmsManager
        (ConnectionFactory incomingCF,
         ConnectionFactory outgoingCF,
         ContextClassProvider contextClassProvider)
    {
        this(incomingCF,outgoingCF,contextClassProvider,null);
    }


    // INSTANCE METHODS.

    /**
     * Returns the receiver's Spring-wrapped factory of incoming
     * connections.
     *
     * @return The factory.
     */

    public IncomingJmsFactory getIncomingJmsFactory()
    {
        return mIncomingJmsFactory;
    }

    /**
     * Returns the receiver's Spring-wrapped factory of outgoing
     * connections.
     *
     * @return The factory.
     */

    public OutgoingJmsFactory getOutgoingJmsFactory()
    {
        return mOutgoingJmsFactory;
    }
}
