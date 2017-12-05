package org.marketcetera.server.ba.config;

import java.util.List;

import org.marketcetera.server.ba.brokers.Brokers;
import org.marketcetera.ws.server.filters.BasicFilter;
import org.marketcetera.ws.server.filters.BasicFilterNoop;
import org.marketcetera.ws.server.filters.MessageFilter;
import org.marketcetera.ws.server.filters.MessageFilterNoop;
import org.marketcetera.util.except.I18NException;
import org.marketcetera.util.misc.ClassVersion;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;

/**
 * The application's Spring-based configuration. A global singleton
 * instance of this class is created by Spring during the creation of
 * the application's {@link ApplicationContext}, and it contains all
 * end-user configuration of the application.
 */
@ClassVersion("$Id: SpringConfig.java 16664 2013-08-23 23:06:00Z colin $")
public class SpringConfig
    implements InitializingBean
{
    // CLASS DATA.
    private static SpringConfig sSingleton;

    // INSTANCE DATA.
    private Brokers mBrokers;
    private MessageFilter mSupportedMessages;
    private List<BasicFilter> mAllowedRequests;
    
    // CONSTRUCTORS.
    /**
     * Creates a new application configuration, which becomes the
     * global singleton.
     */
    public SpringConfig()
    {
        mSupportedMessages=new MessageFilterNoop();
        setSingleton(this);
    }

    /**
     * Creates a new application configuration with the given
     * properties. The new configuration becomes the global singleton.
     *
     * @param brokers The broker configurations.
     * @param supportedMessages The filter of supported messages.
     * @param allowedOrders The filter of allowed orders.
     * @param serverHost The host name for web services.
     * @param serverPort The port for web services.
     * @param serverSessionLife The web services session lifetime, in s
     * @param incomingCF The connection factory for incoming connections.
     * @param outgoingCF The connection factory for outgoing connections.
     * @param contextClassProvider The context ClassProvider.
     * @param idFactory The ID generation factory.
     * @param historyService The history service provider.
     * @param jobsUser The jobs user.
     * @param basicJobs The list of user jobs.
     * @param dataSource the data source.
     */
    public SpringConfig
        (Brokers brokers,
         MessageFilter supportedMessages,
         List<BasicFilter> allowedRequests)
        throws I18NException
    {
        setBrokers(brokers);
        setSupportedMessages(supportedMessages);
        setAllowedRequests(allowedRequests);

        afterPropertiesSet();
        setSingleton(this);
    }
    
    // CLASS METHODS.
    /**
     * Sets the global singleton configuration to the given one.
     *
     * @param singleton The configuration. It may be null.
     */
    public static void setSingleton
        (SpringConfig singleton)
    {
        sSingleton=singleton;
    }

    /**
     * Returns the global singleton configuration.
     *
     * @return The configuration. It may be null.
     */
    public static SpringConfig getSingleton()
    {
        return sSingleton;
    }


    // INSTANCE METHODS.
    /**
     * Sets the receiver's broker configurations to the given ones. A
     * non-null value should be set during the receiver's
     * initialization.
     *
     * @param brokers The configurations.
     */
    public void setBrokers
        (Brokers brokers)
    {
        mBrokers=brokers;
    }

    /**
     * Returns the receiver's broker configurations.
     *
     * @return The configurations.
     */
    public Brokers getBrokers()
    {
        return mBrokers;
    }

    /**
     * Sets the receiver's filter of supported messages to the given
     * one. If this method is not called during initialization, the
     * filter defaults to a {@link MessageFilterNoop}.
     *
     * @param supportedMessages The filter.
     */
    public void setSupportedMessages
        (MessageFilter supportedMessages)
    {
        mSupportedMessages=supportedMessages;
    }

    /**
     * Returns the receiver's filter of supported messages.
     *
     * @return The filter.
     */
    public MessageFilter getSupportedMessages()
    {
        return mSupportedMessages;
    }

    /**
     * Sets the receiver's filter of allowed requests to the given one.
     * If this method is not called during initialization, the filter
     * defaults to a {@link BasicFilterNoop}.
     *
     * @param allowedOrders The filters.
     */
    public void setAllowedRequests(List<BasicFilter> allowedRequests)
    {
        mAllowedRequests=allowedRequests;
    }

    /**
     * Returns the receiver's filter of allowed requests.
     *
     * @return The filters.
     */
    public List<BasicFilter> getAllowedRequests()
    {
        return mAllowedRequests;
    }
    
    // InitializingBean.
    @Override
    public void afterPropertiesSet()
        throws I18NException
    {
        if (getBrokers()==null) {
            throw new I18NException(Messages.NO_BROKERS);
        }
    }
}
