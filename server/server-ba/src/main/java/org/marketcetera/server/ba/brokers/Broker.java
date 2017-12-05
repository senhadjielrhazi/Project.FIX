package org.marketcetera.server.ba.brokers;

import java.util.Collection;
import java.util.Map;

import org.marketcetera.brokers.BrokerStatus;
import org.marketcetera.brokers.algo.BrokerAlgoSpec;
import org.marketcetera.quickfix.FIXDataDictionary;
import org.marketcetera.quickfix.FIXMessageFactory;
import org.marketcetera.quickfix.FIXVersion;
import org.marketcetera.ws.server.filters.MessageModifierManager;
import org.marketcetera.ws.server.filters.MessageRouteManager;
import org.marketcetera.quickfix.messagefactory.FIXMessageAugmentor;
import org.marketcetera.server.ba.brokers.api.ClientKernel;
import org.marketcetera.trade.BrokerID;
import org.marketcetera.util.misc.ClassVersion;
import org.marketcetera.util.quickfix.SpringSessionDescriptor;

import quickfix.DataDictionary;
import quickfix.Message;
import quickfix.SessionID;
import quickfix.SessionNotFound;

/**
 * The single broker interface.
 */
@ClassVersion("$Id: Broker.java 16752 2013-11-14 02:54:13Z colin $")
public interface Broker
{
    /**
     * Returns the receiver's status.
     *
     * @return The status.
     */
    public BrokerStatus getStatus();

    /**
     * Returns the receiver's name.
     *
     * @return The name.
     */
    public String getName();
    
    /**
     * Returns the receiver's broker ID.
     *
     * @return The ID.
     */
    public BrokerID getBrokerID();

    /**
     * Sets the receiver's logon flag to the given value. This method
     * is synchronized to ensure that all threads will see the most
     * up-to-date value for the flag.
     *
     * @param loggedOn The flag.
     */
    public void setLoggedOn(boolean loggedOn);

    /**
     * Returns the receiver's logon flag. This method is synchronized
     * to ensure that all threads will see the most up-to-date value
     * for the flag.
     *
     * @return The flag.
     */
    public boolean getLoggedOn();
    
	/**
	 * Send a message to the broker's session.
	 *  
     * @param msg The message.
     * 
     * @return boolean success
	 */
	boolean sendToTarget(Message msg) 
			throws SessionNotFound;
	
	/**
	 * Receive a message to the broker's session.
	 *  
     * @param msg The message.
     * @param sessionID the receiver of the response.
	 */
	public void receiveMessage(Message msg, SessionID sessionID);
	
    /**
     * Returns the receiver's QuickFIX/J data dictionary.
     *
     * @return The dictionary.
     */
    public DataDictionary getDataDictionary();
    
    /**
     * Returns the receiver's FIX data dictionary.
     *
     * @return The dictionary.
     */
    public FIXDataDictionary getFIXDataDictionary();
    
    /**
     * Returns the receiver's FIX message factory.
     *
     * @return The factory.
     */
    public FIXMessageFactory getFIXMessageFactory();
    
    /**
     * Returns the receiver's FIX message augmentor.
     *
     * @return The augmentor.
     */
    public FIXMessageAugmentor getFIXMessageAugmentor();
    
    /**
     * Logs the given message, analyzed using the receiver's data
     * dictionary, at the debugging level.
     *
     * @param msg The message.
     */
    public void logMessage(Message msg);
    
    /**
     * Returns the receiver's message modifier manager.
     *
     * @return The manager. It may be null.
     */
    public MessageModifierManager getModifiers();

    /**
     * Returns the receiver's route manager.
     *
     * @return The manager. It may be null.
     */
    public MessageRouteManager getRoutes();

    /**
     * Returns the receiver's pre-sending message modifier manager.
     *
     * @return The manager. It may be null.
     */
    public MessageModifierManager getPreSendModifiers();

    /**
     * Returns the receiver's response message modifier manager.
     *
     * @return The manager. It may be null.
     */
    public MessageModifierManager getResponseModifiers();
    
    /**
     * Get the logonActions value.
     *
     * @return a <code>List&lt;LogonAction&gt;</code> value
     */
    public Collection<LogonAction> getLogonActions();
    
    /**
     * Get the logoutActions value.
     *
     * @return a <code>Collection&lt;LogoutAction&gt;</code> value
     */
    public Collection<LogoutAction> getLogoutActions();

    /**
     * Indicates if the given user is allowed access to this broker or not.
     *
     * @param inUsername a <code>String</code> value
     * @return a <code>boolean</code> value
     */
    public boolean isUserAllowed(String inUsername);
    
    /**
     * Get the brokerAlgos value.
     *
     * @return a <code>Map&lt;String,BrokerAlgoSpec&gt;</code> value
     */
    public Map<String,BrokerAlgoSpec> getBrokerAlgosAsMap();
    
    /**
     * Returns the receiver's QuickFIX/J session ID.
     *
     * @return The ID.
     */
    public SessionID getSessionID();

    /**
     * Returns the receiver's FIX version.
     *
     * @return The version.
     */
    public FIXVersion getFIXVersion();

    /**
     * Returns the configuration of the receiver's QuickFIX/J session
     * descriptor.
     *
     * @return The configuration.
     */
    public SpringSessionDescriptor getDescriptor();

    /**
     * Indicates if the broker requires FIX logout on disconnect.
     *
     * @return a <code>boolean</code> value
     */
    public boolean getFixLogoutRequired();
    
    /**
     * Gets the API Client Kernel.
     * 
     * @return a <code>ClientKernel</code> value
     */
	public ClientKernel getClientKernel();
}
