package org.neurosystem.modules.marketdata;

/**
 * Used to specify which price to use for indicator calculation
 * 
 */
public enum AppliedPrice {
    /**
     * Close price
     */
    CLOSE("Close price"),
    /**
     * Open price
     */
    OPEN("Open price"),
    /**
     * High price
     */
    HIGH("High price"),
    /**
     * Low price
     */
    LOW("Low price"),
    /**
     * Median price (HL/2)
     */
    MEDIAN("Median price (HL/2)"),
    /**
     * Typical price (HLC/3)
     */
    TYPICAL("Typical price (HLC/3)");
    
    private String text;
    
    private AppliedPrice(String text) {
        this.text = text;
    }
    
    @Override
    public String toString() {
        return text;
    }
}