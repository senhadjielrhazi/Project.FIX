package org.neurosystem.platform.trade;

/**
 * Specifies type of the order
 */
public enum OrderType {
    /**
     *  Buy by current market price. You can specify price and slippage, if current market price at execution moment
     *  (when order reaches server) is worse than specified price, and slippage is not big enough to execute order by current market price,
     *  then order will be rejected
     */
    BUYMARKET,
    /**
     *  Sell by current market price. You can specify price and slippage, if current market price at execution moment
     *  (when order reaches server) is worse than specified price, and slippage is not big enough to execute order by current market price,
     *  then order will be rejected
     */
    SELLMARKET,
    /**
     * Buy when ask price is <= specified price
     */
    BUYLIMIT,
    /**
     * Sell when bid price is >= specified price
     */
    SELLLIMIT;
    
    /**
     * Returns true if order is LONG and false if order is SHORT
     * @return true if order is LONG and false if order is SHORT
     */
    public boolean isLong() {
        return this == BUYMARKET || this == BUYLIMIT;
    }

    /**
     * Returns true if order is SHORT and false if order is LONG
     * @return true if order is SHORT and false if order is LONG
     */
    public boolean isShort() {
        return !isLong();
    }
    
    /**
     * Returns true if order is one of LIMIT orders
     * 
     * @return true if LIMIT order, false otherwise
     */
    public boolean isConditional() {
        return this != BUYMARKET && this != SELLMARKET; 
    }
}
