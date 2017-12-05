package org.marketcetera.server.ws.brokers.spring;

import org.marketcetera.server.ws.brokers.Broker;
import org.marketcetera.server.ws.brokers.LogonAction;
import org.marketcetera.server.ws.brokers.LogoutAction;
import org.marketcetera.server.ws.brokers.Messages;
import org.marketcetera.ws.server.filters.MessageModifierManager;
import org.marketcetera.ws.server.filters.MessageRouteManager;
import org.marketcetera.trade.BrokerID;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.marketcetera.brokers.BrokerStatus;
import org.marketcetera.brokers.algo.BrokerAlgoSpec;
import org.marketcetera.quickfix.FIXDataDictionary;
import org.marketcetera.quickfix.FIXMessageFactory;
import org.marketcetera.quickfix.FIXMessageUtil;
import org.marketcetera.quickfix.FIXVersion;
import org.marketcetera.quickfix.messagefactory.FIXMessageAugmentor;
import org.marketcetera.util.except.I18NException;
import org.marketcetera.util.log.SLF4JLoggerProxy;
import org.marketcetera.util.misc.ClassVersion;
import org.marketcetera.util.quickfix.AnalyzedMessage;
import org.marketcetera.util.quickfix.SpringSessionDescriptor;
import org.springframework.beans.factory.InitializingBean;

import quickfix.DataDictionary;
import quickfix.Message;
import quickfix.Session;
import quickfix.SessionID;
import quickfix.SessionNotFound;

/**
 * The configuration of a single FIX broker.
 */
@ClassVersion("$Id: FIXBroker.java 16752 2013-11-14 02:54:13Z colin $")
public class SpringBroker implements Broker, InitializingBean
{
	// CLASS DATA
    private static final String HEARTBEAT_CATEGORY=
    		Broker.class.getName()+".HEARTBEATS"; //$NON-NLS-1$
    
    private String mName;
	private BrokerID mBrokerID;
	private boolean mLoggedOn;
	private FIXDataDictionary mDataDictionary;
    private MessageModifierManager mModifiers;
    private MessageRouteManager mRoutes;
    private MessageModifierManager mPreSendModifiers;
    private MessageModifierManager mResponseModifiers;
    private Collection<LogonAction> logonActions = new ArrayList<>();
    private Collection<LogoutAction> logoutActions = new ArrayList<>();
    private Set<String> userWhitelist;
    private Set<String> userBlacklist;
    private Set<BrokerAlgoSpec> brokerAlgos;
    private Map<String,BrokerAlgoSpec> brokerAlgoMap;
    
	private SpringSessionDescriptor mSessionDescriptor;
    private boolean mFixLogoutRequired;
    
    // INSTANCE METHODS.   
 	/**
 	 * Send a message to the broker's session.
 	 *  
      * @param msg The message.
      * 
      * @return boolean success
 	 */
     @Override
     public final boolean sendToTarget(Message msg) throws SessionNotFound {
 		return Session.sendToTarget(msg, getSessionID());
 	}
 	
     /**
      * Logs the given message, analyzed using the receiver's data
      * dictionary, at the debugging level.
      *
      * @param msg The message.
      */
     @Override
     public final void logMessage
         (Message msg)
     {
         Object category=(FIXMessageUtil.isHeartbeat(msg)?
                          HEARTBEAT_CATEGORY:this);
         if (SLF4JLoggerProxy.isDebugEnabled(category)) {
             Messages.ANALYZED_MESSAGE.debug
                 (category,
                  new AnalyzedMessage(getDataDictionary(),msg).toString());
         }        
     }
     
	 /**
     * Returns the receiver's status.
     *
     * @return The status.
     */
    @Override
    public final BrokerStatus getStatus()
    {
        return new BrokerStatus(getName(),
                                getBrokerID(),
                                getLoggedOn(),
                                getBrokerAlgosAsMap() == null ? null : new HashSet<>(getBrokerAlgosAsMap().values()));
    }
    
    /**
     * Sets the receiver's name to the given value.
     *
     * @param name The name.
     */
    public void setName
        (String name)
    {
        mName=name;
    }

    /**
     * Returns the receiver's name.
     *
     * @return The name.
     */
    @Override
    public final String getName()
    {
        return mName;
    }
    
    /**
     * Returns the receiver's broker ID.
     *
     * @return The ID.
     */
    @Override
    public final BrokerID getBrokerID()
    {
        return mBrokerID;
    } 
    
    /**
     * Sets the receiver's broker ID to the given string form value.
     *
     * @param id The ID.
     */
    public void setBrokerID(String brokerID)
    {
    	mBrokerID=new BrokerID(brokerID);
    }
    
    /**
     * Sets the receiver's logon flag to the given value. This method
     * is synchronized to ensure that all threads will see the most
     * up-to-date value for the flag.
     *
     * @param loggedOn The flag.
     */
    @Override
    public synchronized void setLoggedOn
        (boolean loggedOn)
    {
        mLoggedOn=loggedOn;
    }

    /**
     * Returns the receiver's logon flag. This method is synchronized
     * to ensure that all threads will see the most up-to-date value
     * for the flag.
     *
     * @return The flag.
     */
    @Override
    public final synchronized boolean getLoggedOn()
    {
        return mLoggedOn;
    }
	
    /**
     * Returns the receiver's QuickFIX/J data dictionary.
     *
     * @return The dictionary.
     */
    @Override
    public final DataDictionary getDataDictionary()
    {
        return getSession().getDataDictionary();
    } 
    
    /**
     * Returns the receiver's FIX data dictionary.
     *
     * @return The dictionary.
     */
    @Override
    public final synchronized FIXDataDictionary getFIXDataDictionary()
    {
        if (mDataDictionary==null) {
            mDataDictionary=new FIXDataDictionary(getDataDictionary());
        }
        return mDataDictionary;
    }
 
    /**
     * Returns the receiver's FIX message factory.
     *
     * @return The factory.
     */
    @Override
    public final FIXMessageFactory getFIXMessageFactory()
    {
        return getFIXVersion().getMessageFactory();
    }
    
    /**
     * Returns the receiver's FIX message augmentor.
     *
     * @return The augmentor.
     */
    @Override
    public final FIXMessageAugmentor getFIXMessageAugmentor()
    {
        return getFIXMessageFactory().getMsgAugmentor();
    }
    
    /**
     * Sets the receiver's message modifier manager to the given one.
     *
     * @param modifiers The manager. It may be null.
     */
    public void setModifiers
        (MessageModifierManager modifiers)
    {
        mModifiers=modifiers;
    }

    /**
     * Returns the receiver's message modifier manager.
     *
     * @return The manager. It may be null.
     */
    @Override
    public final MessageModifierManager getModifiers()
    {
        return mModifiers;
    }

    /**
     * Sets the receiver's route manager to the given one.
     *
     * @param routes The manager. It may be null.
     */
    public void setRoutes
        (MessageRouteManager routes)
    {
        mRoutes=routes;
    }

    /**
     * Returns the receiver's route manager.
     *
     * @return The manager. It may be null.
     */
    @Override
    public final MessageRouteManager getRoutes()
    {
        return mRoutes;
    }

    /**
     * Sets the receiver's pre-sending message modifier manager to the
     * given one.
     *
     * @param preSendModifiers The manager. It may be null.
     */
    public void setPreSendModifiers
        (MessageModifierManager preSendModifiers)
    {
        mPreSendModifiers=preSendModifiers;
    }

    /**
     * Returns the receiver's pre-sending message modifier manager.
     *
     * @return The manager. It may be null.
     */
    @Override
    public final MessageModifierManager getPreSendModifiers()
    {
        return mPreSendModifiers;
    }

    /**
     * Sets the receiver's response message modifier manager to the
     * given one.
     *
     * @param responseModifiers The manager. It may be null.
     */
    public void setResponseModifiers
        (MessageModifierManager responseModifiers)
    {
        mResponseModifiers=responseModifiers;
    }

    /**
     * Returns the receiver's response message modifier manager.
     *
     * @return The manager. It may be null.
     */
    @Override
    public final MessageModifierManager getResponseModifiers()
    {
        return mResponseModifiers;
    }
    
    /**
     * Get the logonActions value.
     *
     * @return a <code>List&lt;LogonAction&gt;</code> value
     */
    @Override
    public final Collection<LogonAction> getLogonActions()
    {
        return logonActions;
    }
    
    /**
     * Sets the logonActions value.
     *
     * @param a <code>Collection&lt;LogonAction&gt;</code> value
     */
    public void setLogonActions(Collection<LogonAction> inLogonActions)
    {
        logonActions = inLogonActions;
    }
    
    /**
     * Get the logoutActions value.
     *
     * @return a <code>Collection&lt;LogoutAction&gt;</code> value
     */
    @Override
    public final Collection<LogoutAction> getLogoutActions()
    {
        return logoutActions;
    }
    
    /**
     * Sets the logoutActions value.
     *
     * @param a <code>Collection&lt;LogoutAction&gt;</code> value
     */
    public void setLogoutActions(Collection<LogoutAction> inLogoutActions)
    {
        logoutActions = inLogoutActions;
    }

    /**
     * Get the userWhitelist value.
     *
     * @return a <code>Set&lt;String&gt;</code> value
     */
    public Set<String> getUserWhitelist()
    {
        return userWhitelist;
    }
    
    /**
     * Sets the userWhitelist value.
     *
     * @param inUserWhitelist a <code>Set&lt;String&gt;</code> value
     */
    public void setUserWhitelist(Set<String> inUserWhitelist)
    {
        userWhitelist = inUserWhitelist;
    }
    
    /**
     * Get the userBlacklist value.
     *
     * @return a <code>Set&lt;String&gt;</code> value
     */
    public Set<String> getUserBlacklist()
    {
        return userBlacklist;
    }
    
    /**
     * Sets the userBlacklist value.
     *
     * @param inUserBlacklist a <code>Set&lt;String&gt;</code> value
     */
    public void setUserBlacklist(Set<String> inUserBlacklist)
    {
        userBlacklist = inUserBlacklist;
    }
    
    /**
     * Indicates if the given user is allowed access to this broker or not.
     *
     * @param inUsername a <code>String</code> value
     * @return a <code>boolean</code> value
     */
    @Override
    public final boolean isUserAllowed(String inUsername)
    {
        if(userBlacklist != null && userBlacklist.contains(inUsername)) {
            return false;
        }
        return userWhitelist == null || userWhitelist.contains(inUsername);
    }
    
    /**
     * Get the brokerAlgos value.
     *
     * @return a <code>Map&lt;String,BrokerAlgoSpec&gt;</code> value
     */
    @Override
    public final Map<String,BrokerAlgoSpec> getBrokerAlgosAsMap()
    {
        return brokerAlgoMap;
    }
    
    /**
     * Sets the brokerAlgos value.
     *
     * @param inBrokerAlgos a <code>Set<BrokerAlgoSpec></code> value
     */
    public void setBrokerAlgos(Set<BrokerAlgoSpec> inBrokerAlgos)
    {
        brokerAlgos = inBrokerAlgos;
        if(inBrokerAlgos == null) {
            brokerAlgoMap = null;
        } else {
            brokerAlgoMap = new HashMap<String,BrokerAlgoSpec>();
            for(BrokerAlgoSpec algoSpec : brokerAlgos) {
                brokerAlgoMap.put(algoSpec.getName(),
                                  algoSpec);
            }
        }
    }
    
    /**
     * Get the brokerAlgos value.
     *
     * @return a <code>Set&lt;BrokerAlgoSpec&gt;</code> value
     */
    public Set<BrokerAlgoSpec> getBrokerAlgos()
    {
        return brokerAlgos;
    }
	
	/**
     * Returns the receiver's QuickFIX/J session ID.
     *
     * @return The ID.
     */
    @Override
    public final SessionID getSessionID()
    {
        return getDescriptor().getQSessionID();
    }

    /**
     * Returns the receiver's QuickFIX/J session.
     *
     * @return The session.
     */
    protected Session getSession()
    {
        return Session.lookupSession(getSessionID());
    }

    /**
     * Returns the receiver's FIX version.
     *
     * @return The version.
     */
    @Override
    public final FIXVersion getFIXVersion()
    {
        return FIXVersion.getFIXVersion(getSessionID().getBeginString());
    }

    /**
     * Sets the configuration of the receiver's QuickFIX/J session
     * descriptor to the given one.
     *
     * @param sessionDescriptor The configuration.
     */
    public void setDescriptor
        (SpringSessionDescriptor sessionDescriptor)
    {
        mSessionDescriptor=sessionDescriptor;
    }

    /**
     * Returns the configuration of the receiver's QuickFIX/J session
     * descriptor.
     *
     * @return The configuration.
     */
    @Override
    public final SpringSessionDescriptor getDescriptor()
    {
        return mSessionDescriptor;
    }

    /**
     * Indicates if the broker requires FIX logout on disconnect.
     *
     * @return a <code>boolean</code> value
     */
    @Override
    public final boolean getFixLogoutRequired()
    {
        return mFixLogoutRequired;
    }

    /**
     * Sets if the broker requires FIX logout on disconnect.
     *
     * @param a <code>boolean</code> value
     */
    public void setFixLogoutRequired(boolean inLogout)
    {
        mFixLogoutRequired = inLogout;
    }

    // Object.
    @Override
    public final String toString()
    {
        return Messages.BROKER_STRING.getText
            (getBrokerID().getValue(),getName(),getSessionID());
    } 
    
    // InitializingBean.
    @Override
    public void afterPropertiesSet()
        throws I18NException
    {
        if (getName()==null) {
            throw new I18NException(Messages.NO_NAME);
        }
        if (getBrokerID()==null) {
            throw new I18NException(Messages.NO_ID);
        }
        if (getDescriptor()==null) {
            throw new I18NException(Messages.NO_DESCRIPTOR);
        }
    }
}
