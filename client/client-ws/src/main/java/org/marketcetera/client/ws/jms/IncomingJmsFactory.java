package org.marketcetera.client.ws.jms;

import javax.jms.ConnectionFactory;
import javax.jms.ExceptionListener;
import javax.xml.bind.JAXBException;

import org.marketcetera.util.misc.ClassVersion;
import org.marketcetera.util.ws.ContextClassProvider;
import org.marketcetera.ws.ResponseMessage;
import org.springframework.jms.listener.SimpleMessageListenerContainer;
import org.springframework.jms.listener.adapter.MessageListenerAdapter;
import org.springframework.jms.support.converter.MessageConverter;

/**
 * A factory of incoming Spring-wrapped JMS connections (connection
 * handlers).
 *
 * @author tlerios@marketcetera.com
 * @since 1.0.0
 * @version $Id: IncomingJmsFactory.java 16614 2013-07-03 22:35:32Z colin $
 */

/* $License$ */

@ClassVersion("$Id: IncomingJmsFactory.java 16614 2013-07-03 22:35:32Z colin $")
public class IncomingJmsFactory
    extends JmsFactory
{

    // INSTANCE DATA.

    private final ExceptionListener mExceptionListener;
    private final ContextClassProvider mContextClassProvider;

    // CONSTRUCTORS.

    /**
     * Creates a new factory that uses the given standard JMS
     * connection factory to create connections, and directs
     * exceptions to the given listener, if any.
     *
     * @param connectionFactory The factory.
     * @param exceptionListener The listener. It may be null.
     */    

    public IncomingJmsFactory
        (ConnectionFactory connectionFactory,
         ContextClassProvider contextClassProvider,
         ExceptionListener exceptionListener)
    {
        super(connectionFactory);
        mContextClassProvider = contextClassProvider;
        mExceptionListener=exceptionListener;
    }


    // INSTANCE METHODS.
    
    /**
     * Gets the context class provider.
     *
     * @return The context context class provider.
     */

    public ContextClassProvider getContextClassProvider()
    {
        return mContextClassProvider;
    }
    
    /**
     * Returns the receiver's exception listener.
     *
     * @return The listener. It may be null.
     */

    public ExceptionListener getExceptionListener()
    {
        return mExceptionListener;
    }

    /**
     * Registers the given method of the given message handler for
     * messages that are received by the given incoming destination
     * (and of the given type). Replies to those messages are sent to
     * the given reply destination (and of the given type), if
     * any. The given message converter is used to convert messages
     * between the type used by the handler and the standard JMS
     * message type. Returns the Spring container of the handler
     * (listener) for manual bean lifecycle management.
     *
     * @param handler The message handler.
     * @param handlerMethod The name of the message handler's method.
     * @param inDstName The incoming destination name.
     * @param isInDstTopic True if the incoming destination is a topic.
     * @param replyDstName The reply destination name. It may be null.
     * @param isReplyDstTopic True if the reply destination is a topic.
     * @param messageConverter The converter.
     *
     * @return The container.
     */

    private SimpleMessageListenerContainer registerHandler
        (Object handler,
         String handlerMethod,
         String inDstName,
         boolean isInDstTopic,
         String replyDstName,
         boolean isReplyDstTopic,
         MessageConverter messageConverter)
    {
        MessageListenerAdapter adapter=new MessageListenerAdapter(handler);
        adapter.setDefaultListenerMethod(handlerMethod);
        adapter.setMessageConverter(messageConverter);
        if (replyDstName!=null) {
            if (isReplyDstTopic) {
                adapter.setDefaultResponseTopicName(replyDstName);
            } else {
                adapter.setDefaultResponseQueueName(replyDstName);
            }
        }

        SimpleMessageListenerContainer container=
            new SimpleMessageListenerContainer();
        container.setConnectionFactory(getConnectionFactory());
        container.setDestinationName(inDstName);
        container.setPubSubDomain(isInDstTopic);
        container.setMessageListener(adapter);
        if (getExceptionListener()!=null) {
            container.setExceptionListener(getExceptionListener());
        }
        container.afterPropertiesSet();
	container.start();
        return container;
    }

    /**
     * Registers the given message handler for messages that are
     * received by the given incoming destination (and of the given
     * type). The given message converter is used to convert messages
     * between the type used by the handler and the standard JMS
     * message type. Returns the Spring container of the handler
     * (listener) for manual bean lifecycle management.
     *
     * @param handler The message handler.
     * @param inDstName The incoming destination name.
     * @param isInDstTopic True if the incoming destination is a topic.
     * @param messageConverter The converter.
     *
     * @return The container.
     */

    private SimpleMessageListenerContainer registerHandler
        (MessageHandler<?> handler,
         String inDstName,
         boolean isInDstTopic,
         MessageConverter messageConverter)
    {
        return registerHandler
            (handler,"receiveMessage", //$NON-NLS-1$
             inDstName,isInDstTopic,
             null,false,messageConverter);
    }

    /**
     * Registers the given message handler for messages that are
     * received by the given incoming destination (and of the given
     * type). The handler is expected to operate on FIX Agnostic trade
     * messages transported using XML. Returns the Spring container of
     * the handler (listener) for manual bean lifecycle management.
     *
     * @param handler The message handler.
     * @param inDstName The incoming destination name.
     * @param isInDstTopic True if the incoming destination is a topic.
     * 
     * @return The container.
     *
     * @throws JAXBException Thrown if an error occurs in creating the
     * JMS/XML converter.
     */

    public SimpleMessageListenerContainer registerResponseHandler
        (MessageHandler<? extends ResponseMessage> handler,
         String inDstName,
         boolean isInDstTopic)
        throws JAXBException
    {
        return registerHandler
            (handler,inDstName,isInDstTopic,new XMLMessageConverter(getContextClassProvider()));
    }

    /**
     * Registers the given message handler for messages that are
     * received by the given incoming destination (and of the given
     * type). The handler is expected to operate on broker status
     * messages transported using XML. Returns the Spring container of
     * the handler (listener) for manual bean lifecycle management.
     *
     * @param handler The message handler.
     * @param inDstName The incoming destination name.
     * @param isInDstTopic True if the incoming destination is a topic.
     * 
     * @return The container.
     *
     * @throws JAXBException Thrown if an error occurs in creating the
     * JMS/XML converter.
     */

    public SimpleMessageListenerContainer registerRequestHandler
        (MessageHandler<? extends RequestEnvelope> handler,
         String inDstName,
         boolean isInDstTopic)
        throws JAXBException
    {
        return registerHandler
            (handler,inDstName,isInDstTopic,new XMLMessageConverter(getContextClassProvider()));
    }
}
