package org.marketcetera.server.ba.brokers.spring;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.marketcetera.brokers.BrokerStatus;
import org.marketcetera.brokers.BrokersStatus;
import org.marketcetera.server.ba.brokers.Broker;
import org.marketcetera.server.ba.brokers.Brokers;
import org.marketcetera.server.ba.brokers.Messages;
import org.marketcetera.trade.BrokerID;
import org.marketcetera.util.except.I18NException;
import org.marketcetera.util.misc.ClassVersion;
import org.marketcetera.util.quickfix.SpringSessionDescriptor;
import org.marketcetera.util.quickfix.SpringSessionSettings;
import org.springframework.beans.factory.InitializingBean;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import quickfix.SessionID;

/**
 * The collective configuration of all brokers.
 */
@ClassVersion("$Id: FIXBrokers.java 16154 2012-07-14 16:34:05Z colin $")
public class SpringBrokers
    implements Brokers, InitializingBean
{
	// CLASS DATA
    private Set<Broker> mTradeBrokers;
    private Set<Broker> mDataBrokers;
    private Map<BrokerID,Broker> mTradeBrokerMap;
    private Map<BrokerID,Broker> mDataBrokerMap;
    private Map<SessionID,Broker> mSessionIDMap;
    private SpringSessionSettings mSettings;
    
    // INSTANCE METHODS.
    /**
     * Returns the status of the receiver's brokers.
     *
     * @return The status.
     */
    public BrokersStatus getStatus(String inUsername)
    {
        List<BrokerStatus> listTrade = Lists.newArrayList();
        for(Broker b : getTradeBrokers()) {
            if(b.isUserAllowed(inUsername)) {
            	listTrade.add(b.getStatus());
            }
        }
        
        List<BrokerStatus> listData = Lists.newArrayList();
        for(Broker b : getDataBrokers()) {
            if(b.isUserAllowed(inUsername)) {
            	listData.add(b.getStatus());
            }
        }
        
        return new BrokersStatus(listTrade, listData);
    }
    
    /**
     * Returns the receiver's broker for the given QuickFIX/J session
     * ID. It logs an error and returns null if there is no broker for
     * the given ID.
     *
     * @param sessionID The ID.
     *
     * @return The broker. It may be null.
     */
    public Broker getBroker
        (SessionID sessionID)
    {
        Broker b=mSessionIDMap.get(sessionID);
        if (b==null) {
            Messages.INVALID_SESSION_ID.error(this,sessionID);
        }
        return b;
    }

    /**
     * Returns the receiver's broker for the given broker ID. It logs
     * an error and returns null if there is no broker for the given
     * ID.
     *
     * @param brokerID The ID.
     *
     * @return The broker. It may be null.
     */
    public Broker getTradeBroker
        (BrokerID brokerID)
    {
        Broker b=mTradeBrokerMap.get(brokerID);
        if (b==null) {
            Messages.INVALID_BROKER_ID.error(this,brokerID);
        }
        return b;
    }
    
    /**
     * Returns the receiver's broker for the given broker ID. It logs
     * an error and returns null if there is no broker for the given
     * ID.
     *
     * @param brokerID The ID.
     *
     * @return The broker. It may be null.
     */
    public Broker getDataBroker
        (BrokerID brokerID)
    {
        Broker b=mDataBrokerMap.get(brokerID);
        if (b==null) {
            Messages.INVALID_BROKER_ID.error(this,brokerID);
        }
        return b;
    }

    /**
     * Sets the configurations of the receiver's brokers to the given
     * ones.
     *
     * @param brokers The configurations.
     */
    public void setTradeBrokers
        (Set<Broker> brokers)
    {
        mTradeBrokers=brokers;
    }

    /**
     * Returns the configurations of the receiver's brokers.
     *
     * @return The configurations.
     */
    public Set<Broker> getTradeBrokers()
    {
        return mTradeBrokers;
    }

    /**
     * Sets the configurations of the receiver's brokers to the given
     * ones.
     *
     * @param brokers The configurations.
     */
    public void setDataBrokers
        (Set<Broker> brokers)
    {
        mDataBrokers=brokers;
    }

    /**
     * Returns the configurations of the receiver's brokers.
     *
     * @return The configurations.
     */
    public Set<Broker> getDataBrokers()
    {
        return mDataBrokers;
    }
    
    /**
     * Returns the configurations of the receiver's all brokers.
     *
     * @return The configurations.
     */
	public Set<Broker> getBrokers() {
		return Sets.union(mTradeBrokers, mDataBrokers);
	}
	
    /**
     * Sets the configuration of the QuickFIX/J session
     * settings to the given one.
     *
     * @param sessionSettings The configuration.
     */
    public void setSettings
        (SpringSessionSettings settings)
    {
        mSettings=settings;
    }

    /**
     * Returns the configuration of the QuickFIX/J session
     * settings.
     *
     * @return The configuration.
     */
    public SpringSessionSettings getSettings()
    {
        return mSettings;
    }
    
    // InitializingBean.
    @Override
    public void afterPropertiesSet()
        throws I18NException
    {
        if (getSettings()==null) {
            throw new I18NException(Messages.NO_SETTINGS);
        }
        if (getTradeBrokers()==null) {
            throw new I18NException(Messages.NO_BROKERS);
        }
        if (getDataBrokers()==null) {
            throw new I18NException(Messages.NO_BROKERS);
        }
        
        List<SpringSessionDescriptor> descriptors=Lists.newArrayList();
        for (Broker b:getTradeBrokers()) {
        	descriptors.add(b.getDescriptor());
        }
        for (Broker b:getDataBrokers()) {
        	descriptors.add(b.getDescriptor());
        }
        getSettings().setDescriptors(descriptors);
        
        mTradeBrokerMap = Maps.newHashMap();
        mDataBrokerMap = Maps.newHashMap();
        mSessionIDMap = Maps.newHashMap();
        for (Broker b:getTradeBrokers()) {
            mTradeBrokerMap.put(b.getBrokerID(),b);
            mSessionIDMap.put(b.getSessionID(),b);
        }
        for (Broker b:getDataBrokers()) {
            mDataBrokerMap.put(b.getBrokerID(),b);
            mSessionIDMap.put(b.getSessionID(),b);
        }
    }
}
