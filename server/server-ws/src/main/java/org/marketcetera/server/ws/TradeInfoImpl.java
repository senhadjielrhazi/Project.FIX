package org.marketcetera.server.ws;

import org.marketcetera.trade.OrderID;
import org.marketcetera.trade.UserID;
import org.marketcetera.util.misc.ClassVersion;

import quickfix.Message;

/**
 * An entry for trade information in a {@link ServerPersister} cache.
 */
@ClassVersion("$Id: TradeInfoImpl.java 16154 2012-07-14 16:34:05Z colin $")
public class TradeInfoImpl
		implements TradeInfo
{
    // INSTANCE DATA.
    private Boolean mERPersisted;
    private Boolean mAckExpected;
    private Boolean mAckProcessed;
    private Boolean mResponseExpected;
    private Boolean mResponseProcessed;
    
    private final ServerPersister mCache;
    private final OrderID mOrderID;
    private final OrderID mOrigOrderID;
    private final UserID mActorID;

    // CONSTRUCTORS.
    /**
     * Creates a new cache entry of the given cache for an order with
     * the given self and parent IDs, and the given actor ID. The
     * viewer ID of the new entry is either marked unknown-at-present
     * (if this order is a child of an earlier one), or set to the
     * actor ID (if it's the root of a chain).
     *
     * @param cache The cache.
     * @param orderID The order ID.
     * @param origOrderID The parent order ID. It may be null for
     * orders that are chain roots.
     * @param actorID The actor ID.
     */
    public TradeInfoImpl
        (ServerPersister cache,
         OrderID orderID,
         OrderID origOrderID,
         UserID actorID)
    {
    	mCache=cache;
        mOrderID=orderID;
        mOrigOrderID=origOrderID;
        mActorID=actorID;
    }

    // INSTANCE METHODS.
    /**
     * Returns the receiver's cache.
     *
     * @return The cache.
     */
    protected ServerPersister getCache()
    {
        return mCache;
    }

    // TradeInfo.
    @Override
    public OrderID getOrderID()
    {
        return mOrderID;
    }

    @Override
    public OrderID getOrigOrderID()
    {
        return mOrigOrderID;
    }

    @Override
    public UserID getActorID()
    {
        return mActorID;
    }
    
    @Override
    public void setMessageProcessed
        (Message msg) {}
    
    /**
     * Returns a flag indicating whether an execution report (ack, or
     * broker response) for the receiver's order has been persisted.
     *
     * @return The flag. It may be null to indicate that it is yet
     * unknown whether that condition has been met.
     */
    protected synchronized Boolean getERPersisted()
    {
        return mERPersisted;
    }

    /**
     * Returns a flag indicating whether the Server is expected to issue
     * an ack for the receiver's order.
     *
     * @return The flag. It may be null to indicate that it is yet
     * unknown whether that condition has been met.
     */
    protected synchronized Boolean getAckExpected()
    {
        return mAckExpected;
    }

    /**
     * Returns a flag indicating whether the Server has retrieved the ack
     * principals for the receiver's order.
     *
     * @return The flag. It may be null to indicate that it is yet
     * unknown whether that condition has been met.
     */
    protected synchronized Boolean getAckProcessed()
    {
        return mAckProcessed;
    }

    /**
     * Returns a flag indicating whether the Server is expected to
     * receive a response to the receiver's order.
     *
     * @return The flag. It may be null to indicate that it is yet
     * unknown whether that condition has been met.
     */
    protected synchronized Boolean getResponseExpected()
    {
        return mResponseExpected;
    }

    /**
     * Returns a flag indicating whether the Server has retrieved the
     * response principals for the receiver's order.
     *
     * @return The flag. It may be null to indicate that it is yet
     * unknown whether that condition has been met.
     */
    protected synchronized Boolean getResponseProcessed()
    {
        return mResponseProcessed;
    }

    /**
     * Checks whether the receiver can be safely removed from the
     * cache. This doesn't mean that the receiver <em>should be
     * removed</em>; it may remain in the cache for performance
     * reasons.
     *
     * @return True if so.
     */
    protected synchronized boolean mayRemove()
    {
        return
            // Has been persisted hence future lookups can rely on the
            // database, or
            ((getERPersisted()==Boolean.TRUE) ||
             // ... a response is not needed, hence...
             ((getResponseExpected()==Boolean.FALSE) &&
              // ... all we need is to get done with the ack (whether
              // not needed or needed and has been processed).
              ((getAckExpected()==Boolean.FALSE) ||
               ((getAckExpected()==Boolean.TRUE) &&
                (getAckProcessed()==Boolean.TRUE)))));
    }

    /**
     * Checks whether the receiver should be removed from the
     * cache. The receiver is retained until it is both unnecessary
     * (confirmed via {@link #mayRemove()} and offers no significant
     * performance benefits.
     *
     * @return True if so.
     */
    protected synchronized boolean shouldRemove()
    {
        return
            // No longer necessary, and...
            (mayRemove() &&
             // ... ack is not needed or is needed and has been
             // processed, and...
             ((getAckExpected()==Boolean.FALSE) ||
              ((getAckExpected()==Boolean.TRUE) &&
               (getAckProcessed()==Boolean.TRUE))) &&
             // ... response is not needed or is needed and has been
             // processed.
             ((getResponseExpected()==Boolean.FALSE) ||
              ((getResponseExpected()==Boolean.TRUE) &&
               (getResponseProcessed()==Boolean.TRUE))));
    }

    /**
     * Checks whether the receiver should be removed from the
     * cache, and, if so, effects such removal.
     */
    protected synchronized void removeIfDesired()
    {
        if (shouldRemove()) {
            getCache().remove(getOrderID());
        }
    }

    // TradeInfoBase.
    @Override
    public synchronized void setERPersisted
        (boolean erPersisted)
    {
        mERPersisted=erPersisted;
        removeIfDesired();
    }

    @Override
    public synchronized void setAckExpected
        (boolean ackExpected)
    {
        mAckExpected=ackExpected;
        removeIfDesired();
    }

    @Override
    public synchronized void setAckProcessed
        (boolean ackProcessed)
    {
        mAckProcessed=ackProcessed;
        removeIfDesired();
    }

    @Override
    public synchronized void setResponseExpected
        (boolean responseExpected)
    {
        mResponseExpected=responseExpected;
        removeIfDesired();
    }
    
    @Override
    public synchronized void setResponseProcessed
        (boolean responseProcessed)
    {
        mResponseProcessed=responseProcessed;
        removeIfDesired();
    }
}
