package org.marketcetera.client.ws.jms;

import java.io.StringWriter;
import java.io.StringReader;

import javax.jms.*;
import javax.xml.bind.*;

import org.marketcetera.util.log.I18NBoundMessage1P;
import org.marketcetera.util.log.SLF4JLoggerProxy;
import org.marketcetera.util.misc.ClassVersion;
import org.marketcetera.util.ws.ContextClassProvider;
import org.marketcetera.ws.ResponseMessage;
import org.springframework.jms.support.converter.MessageConversionException;
import org.springframework.jms.support.converter.MessageConverter;
import org.apache.commons.lang.ObjectUtils;

/**
 * Converts messaging objects to an XML representation that can be
 * sent over JMS.  This class is not meant to be used by clients of
 * this package.
 */
@ClassVersion("$Id: XMLMessageConverter.java 16841 2014-02-20 19:59:04Z colin $")
public class XMLMessageConverter implements MessageConverter {
    /**
     * Creates an instance.
     *
     * @throws JAXBException if there were errors initializing the
     * XML marshalling / unmarshalling system.
     */
    public XMLMessageConverter(ContextClassProvider context) throws JAXBException {
        mContext = JAXBContext.newInstance
            (context.getContextClasses());
    }

    /**
     * Converts a JMS Message to a messaging object.
     *
     * @param message the received JMS message. It should be of type
     * {@link javax.jms.ObjectMessage}.
     *
     * @return the messaging object converted from the supplied JMS message.
     *
     * @throws javax.jms.JMSException if there were errors extracting the contents
     * of the JMS message.
     * @throws org.springframework.jms.support.converter.MessageConversionException if there were errors converting
     * the contents of the JMS message to a messaging object.
     */
    @Override
    public Object fromMessage(Message message)
            throws JMSException, MessageConversionException {
        SLF4JLoggerProxy.debug(this, "Converting from JMS {}", message);  //$NON-NLS-1$
        if(message instanceof TextMessage) {
            Object object = null;
            try {
                object = fromXML(((TextMessage) message).getText());
            } catch (JAXBException e) {
                throw new MessageConversionException(new I18NBoundMessage1P(
                        Messages.ERROR_CONVERTING_MESSAGE_TO_OBJECT,
                        ObjectUtils.toString(object)).getText(), e);
            }
            if((object instanceof RequestEnvelope) ||
               (object instanceof ResponseMessage)) {
                return object;
            } else {
                throw new MessageConversionException(new I18NBoundMessage1P(
                        Messages.UNEXPECTED_MESSAGE_RECEIVED,
                        ObjectUtils.toString(object)).getText());
            }
        } else {
            throw new MessageConversionException(new I18NBoundMessage1P(
                    Messages.UNEXPECTED_MESSAGE_RECEIVED,
                    ObjectUtils.toString(message)).getText());
        }
	}

    /**
     * Converts a messaging object to a JMS Message.
     *
     * @param object the message to be converted. It should either be
     * an order or a report.
     * @param session the JMS Session instance.
     *
     * @return the JMS message.
     *
     * @throws javax.jms.JMSException if there were errors serializing the
     * messaging object.
     * @throws org.springframework.jms.support.converter.MessageConversionException if the supplied object was not
     * an acceptable messaging object.
     */
    @Override
    public Message toMessage(Object object, Session session)
            throws JMSException, MessageConversionException {
        SLF4JLoggerProxy.debug(this, "Converting to JMS {}", object);  //$NON-NLS-1$
        if ((object instanceof RequestEnvelope) ||
            (object instanceof ResponseMessage)) {
            try {
                TextMessage message = session.createTextMessage(toXML(object));
                //Set the type property for interoperability with .NET client.
                message.setStringProperty(JMS_TYPE_PROPERTY,
                		object.getClass().getSimpleName());
                return message;
            } catch (JAXBException e) {
                throw new MessageConversionException(new I18NBoundMessage1P(
                        Messages.ERROR_CONVERTING_OBJECT_TO_MESSAGE,
                        ObjectUtils.toString(object)).getText(), e);
            }
        } else {
            throw new MessageConversionException(new I18NBoundMessage1P(
                    Messages.UNEXPECTED_MESSAGE_TO_SEND,
                    ObjectUtils.toString(object)).getText());
        }
    }

    /**
     * Marshall the supplied object to XML.
     *
     * @param inObject the object that needs to be marshalled.
     *
     * @return the XML representation.
     *
     * @throws JAXBException if there were was an error marshalling the object
     * to XML.
     */
    public String toXML(Object inObject) throws JAXBException {
        Marshaller marshaller = getMarshaller();
        StringWriter writer = new StringWriter();
        marshaller.marshal(inObject, writer);
        //no need to close or flush the writer as they do nothing.
        return writer.toString();
    }

    /**
     * Unmarshall the supplied object from XML to an instance.
     *
     * @param inXML the object in XML.
     *
     * @return the unmarshalled object instance.
     *
     * @throws JAXBException if there were was an error unmarshalling the
     * object from XML.
     */
    public Object fromXML(String inXML) throws JAXBException {
        Unmarshaller unMarshaller = getUnMarshaller();
        StringReader reader = new StringReader(inXML);
        return unMarshaller.unmarshal(reader);
    }

    /**
     * Returns the underlying JAXB context for testing.
     *
     * @return the underlying JAXB context.
     */
    public JAXBContext getContext() {
        return mContext;
    }

    /**
     * Gets cached copy of the marshaller to use for marshalling objects to XML.
     *
     * @return the marshaller to use for marshalling objects to XML.
     *
     * @throws JAXBException if there were errors getting the marshaller.
     */
    private Marshaller getMarshaller() throws JAXBException {
        Marshaller m = mMarshallers.get();
        if(m == null) {
            m = mContext.createMarshaller();
            mMarshallers.set(m);
        }
        return m;
    }

    /**
     * Gets the cached copy of the unmarshaller to use for unmarshalling objects
     * from XML.
     *
     * @return the unmarshaller to use for unmarshalling objects from XML.
     *
     * @throws JAXBException if there were errors getting the unmarshaller.
     */
    private Unmarshaller getUnMarshaller() throws JAXBException {
        Unmarshaller u = mUnmarshallers.get();
        if(u == null) {
            u = mContext.createUnmarshaller();
            mUnmarshallers.set(u);
        }
        return u;
    }

    private final ThreadLocal<Marshaller> mMarshallers =
            new ThreadLocal<Marshaller>();
    private final ThreadLocal<Unmarshaller> mUnmarshallers =
            new ThreadLocal<Unmarshaller>();
    private final JAXBContext mContext;
    private static final String JMS_TYPE_PROPERTY = "metc_type";  //$NON-NLS-1$
}
