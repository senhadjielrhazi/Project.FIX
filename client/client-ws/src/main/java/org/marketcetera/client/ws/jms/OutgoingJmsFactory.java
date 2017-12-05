package org.marketcetera.client.ws.jms;

import javax.jms.ConnectionFactory;
import javax.xml.bind.JAXBException;
import org.marketcetera.util.misc.ClassVersion;
import org.marketcetera.util.ws.ContextClassProvider;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.support.converter.MessageConverter;

/**
 * A factory of outgoing Spring-wrapped JMS connections (Spring JMS
 * connection templates).
 *
 * @author tlerios@marketcetera.com
 * @since 1.0.0
 * @version $Id: OutgoingJmsFactory.java 16154 2012-07-14 16:34:05Z colin $
 */

/* $License$ */

@ClassVersion("$Id: OutgoingJmsFactory.java 16154 2012-07-14 16:34:05Z colin $")
public class OutgoingJmsFactory
    extends JmsFactory
{

    // INSTANCE DATA.

    private final ContextClassProvider mContextClassProvider;

    // CONSTRUCTORS.
    
    /**
     * Creates a new factory that uses the given standard JMS
     * connection factory to create connections.
     *
     * @param connectionFactory The factory.
     */    

    public OutgoingJmsFactory
        (ConnectionFactory connectionFactory, 
        		ContextClassProvider contextClassProvider)
    {
        super(connectionFactory);
        mContextClassProvider = contextClassProvider;
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
     * Returns a new Spring JMS connection template for the given
     * destination (and of the given type). The given message
     * converter is used to convert messages from the type used by the
     * producer to the standard JMS message type.
     *
     * @param dstName The destination name.
     * @param isDstTopic True if the destination is a topic.
     * @param messageConverter The converter.
     *
     * @return The connection template.
     */

    private JmsTemplate createJmsTemplate
        (String dstName,
         boolean isDstTopic,
         MessageConverter messageConverter)
    {
        JmsTemplate template=new JmsTemplate(getConnectionFactory());
        template.setDefaultDestinationName(dstName);
        template.setPubSubDomain(isDstTopic);
        template.setMessageConverter(messageConverter);
        template.afterPropertiesSet();
        return template;
    }
    
    /**
     * Returns a new Spring JMS connection template for the given
     * destination (and of the given type), and for a producer which
     * emits FIX Agnostic trade messages, request message envelopes, or
     * replay messages, transported using XML.
     *
     * @param dstName The destination name.
     * @param isDstTopic True if the destination is a topic.
     *
     * @return The connection template.
     *
     * @throws JAXBException Thrown if an error occurs in creating the
     * JMS/XML converter.
     */
    public JmsTemplate createJmsTemplate
        (String dstName,
         boolean isDstTopic)
        throws JAXBException
    {
        return createJmsTemplate
            (dstName,isDstTopic,new XMLMessageConverter(getContextClassProvider()));
    }
}
