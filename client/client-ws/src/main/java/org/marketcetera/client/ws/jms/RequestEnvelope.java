package org.marketcetera.client.ws.jms;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlElementRefs;
import javax.xml.bind.annotation.XmlRootElement;

import org.marketcetera.marketdata.MarketDataCancel;
import org.marketcetera.marketdata.MarketDataRequest;
import org.marketcetera.trade.FIXOrderImpl;
import org.marketcetera.trade.OrderBaseImpl;
import org.marketcetera.util.misc.ClassVersion;
import org.marketcetera.util.ws.tags.SessionId;
import org.marketcetera.ws.RequestMessage;

/**
 * A trade message envelope, used to send an {@link RequestMessage} instance
 * over JMS as part of an existing Web Services session.
 *
 * @author tlerios@marketcetera.com
 * @since 1.5.0
 * @version $Id: RequestEnvelope.java 16154 2012-07-14 16:34:05Z colin $
 */

/* $License$ */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement
@ClassVersion("$Id: RequestEnvelope.java 16154 2012-07-14 16:34:05Z colin $")
public class RequestEnvelope
{

    // INSTANCE DATA.
    @XmlElementRefs(value={
        @XmlElementRef(type=OrderBaseImpl.class),
        @XmlElementRef(type=FIXOrderImpl.class),
        @XmlElementRef(type=MarketDataRequest.class),
        @XmlElementRef(type=MarketDataCancel.class)
    })
    private final RequestMessage mRequest;
    private final SessionId mSessionId;


    // CONSTRUCTORS.
    /**
     * Creates a new envelope with the given request and
     * session ID.
     *
     * @param request The request.
     * @param sessionId The session ID.
     */
    public RequestEnvelope
        (RequestMessage request,
         SessionId sessionId)
    {
    	mRequest=request;
        mSessionId=sessionId;
    }

    /**
     * Creates a new envelope. This empty constructor is intended for
     * use by JAXB.
     */
    protected RequestEnvelope()
    {
    	mRequest=null;
        mSessionId=null;
    }


    // INSTANCE METHODS.
    /**
     * Returns the receiver's request.
     *
     * @return The request.
     */
    public RequestMessage getRequest()
    {
        return mRequest;
    }

    /**
     * Returns the receiver's session ID.
     *
     * @return The ID.
     */
    public SessionId getSessionId()
    {
        return mSessionId;
    }


    // Object.
    @Override
    public String toString()
    {
        return Messages.REQUEST_ENVELOPE_TO_STRING.getText
            (String.valueOf(getRequest()),
             String.valueOf(getSessionId()));
    }
}
