package org.marketcetera.server.ws;

import org.marketcetera.trade.OrderID;
import org.marketcetera.trade.UserID;
import org.marketcetera.util.misc.ClassVersion;
import quickfix.Message;

/**
 * An entry for trade information in a {@link ServerPersister} cache.
 */
@ClassVersion("$Id: TradeInfo.java 16154 2012-07-14 16:34:05Z colin $")
public interface TradeInfo
{
    /**
     * Returns the ID of the receiver's order.
     *
     * @return The ID.
     */
    public OrderID getOrderID();

    /**
     * Returns the parent order ID of the receiver's order.
     *
     * @return The ID. It may be null if the receiver's order is a
     * chain root.
     */
    public OrderID getOrigOrderID();

    /**
     * Returns the actor ID of the receiver's order.
     *
     * @return The ID.
     */
    public UserID getActorID();

    /**
     * Sets to the given value a flag indicating whether an execution
     * report (ack, or broker response) for the receiver's order has
     * been persisted.
     *
     * @param persisted The flag.
     */
    public void setERPersisted
        (boolean erPersisted);

    /**
     * Sets to the given value a flag indicating whether the Server is
     * expected to issue an ack for the receiver's order.
     *
     * @param ackExpected The flag.
     */
    public void setAckExpected
        (boolean ackExpected);

    /**
     * Sets to the given value a flag indicating whether the Server has
     * retrieved the ack principals for the receiver's order.
     *
     * @param ackProcessed The flag.
     */
    public void setAckProcessed
        (boolean ackProcessed);

    /**
     * Sets to the given value a flag indicating whether the Server is
     * expected to receive a response to the receiver's order.
     *
     * @param responseExpected The flag.
     */
    public void setResponseExpected
        (boolean responseExpected);
    
    /**
     * Sets to the given value a flag indicating whether the Server has
     * retrieved the response principals for the receiver's order.
     *
     * @param responseProcessed The flag.
     */
    public void setResponseProcessed
        (boolean responseProcessed);

    /**
     * Notifies the receiver that the given message has been processed.
     *
     * @param msg The message.
     */
    public void setMessageProcessed
        (Message msg);
}
