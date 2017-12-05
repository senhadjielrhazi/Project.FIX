package org.marketcetera.server.ba.brokers;

import java.util.Set;

import org.marketcetera.brokers.BrokersStatus;
import org.marketcetera.trade.BrokerID;
import org.marketcetera.util.misc.ClassVersion;
import org.marketcetera.util.quickfix.SpringSessionSettings;

import quickfix.SessionID;

/**
 * The collective of all brokers interface.
 */
@ClassVersion("$Id: Brokers.java 16661 2013-08-22 17:51:57Z colin $")
public interface Brokers
{
    /**
     * Returns the status of the receiver's brokers.
     *
     * @return The status.
     */
    public BrokersStatus getStatus(String inUsername);

    /**
     * Returns the receiver's broker for the given QuickFIX/J session
     * ID. It logs an error and returns null if there is no broker for
     * the given ID.
     *
     * @param sessionID The ID.
     *
     * @return The broker. It may be null.
     */
    public Broker getBroker(SessionID sessionID);

    /**
     * Returns the receiver's trade broker for the given broker ID. It logs
     * an error and returns null if there is no broker for the given
     * ID.
     *
     * @param brokerID The ID.
     *
     * @return The broker. It may be null.
     */
    public Broker getTradeBroker(BrokerID brokerID);
    
    /**
     * Returns the receiver's data broker for the given broker ID. It logs
     * an error and returns null if there is no broker for the given
     * ID.
     *
     * @param brokerID The ID.
     *
     * @return The broker. It may be null.
     */
    public Broker getDataBroker(BrokerID brokerID);
    
    /**
     * Returns the configurations of the receiver's data brokers.
     *
     * @return The configurations.
     */
    public Set<Broker> getBrokers();
    
    /**
     * Returns the configuration of the QuickFIX/J session
     * settings.
     *
     * @return The configuration.
     */
    
    public SpringSessionSettings getSettings();
}
