package org.marketcetera.server.ws.config;

import java.util.List;

import javax.jms.ConnectionFactory;

import org.marketcetera.server.ws.brokers.Brokers;
import org.marketcetera.core.IDFactory;
import org.marketcetera.persist.PersistenceException;
import org.marketcetera.ws.server.BasicJob;
import org.marketcetera.ws.server.filters.BasicFilter;
import org.marketcetera.ws.server.filters.BasicFilterNoop;
import org.marketcetera.ws.server.filters.MessageFilter;
import org.marketcetera.ws.server.filters.MessageFilterNoop;
import org.marketcetera.server.ws.history.HistoryService;
import org.marketcetera.ws.server.security.SimpleUser;
import org.marketcetera.ws.server.security.SingleSimpleUserQuery;
import org.marketcetera.util.except.I18NException;
import org.marketcetera.util.except.I18NRuntimeException;
import org.marketcetera.util.log.I18NBoundMessage1P;
import org.marketcetera.util.misc.ClassVersion;
import org.marketcetera.util.ws.ContextClassProvider;
import org.marketcetera.util.ws.stateful.SessionManager;
import org.marketcetera.util.ws.stateless.Node;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import com.mchange.v2.c3p0.ComboPooledDataSource;

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
    private String mServerHost;
    private int mServerPort;
    private long mServerSessionLife;
    private ConnectionFactory mIncomingCF;
    private ConnectionFactory mOutgoingCF;
    private ContextClassProvider mContextClassProvider;
    private IDFactory mIDFactory;
    private HistoryService mHistoryService;
    private SimpleUser mJobsUser;
    private List<BasicJob> mBasicJobs;
    private ComboPooledDataSource mDataSource;
    
    // CONSTRUCTORS.
    /**
     * Creates a new application configuration, which becomes the
     * global singleton.
     */
    public SpringConfig()
    {
        mSupportedMessages=new MessageFilterNoop();
        mServerHost=Node.DEFAULT_HOST;
        mServerPort=Node.DEFAULT_WS_PORT;
        mServerSessionLife=SessionManager.INFINITE_SESSION_LIFESPAN;
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
         List<BasicFilter> allowedRequests,
         String serverHost,
         int serverPort,
         long serverSessionLife,
         ConnectionFactory incomingCF,
         ConnectionFactory outgoingCF,
         ContextClassProvider contextClassProvider,
         IDFactory idFactory,
         HistoryService historyService,
         String jobsUser,
         List<BasicJob> basicJobs,
         ComboPooledDataSource dataSource)
        throws I18NException
    {
        setBrokers(brokers);
        setSupportedMessages(supportedMessages);
        setAllowedRequests(allowedRequests);
        setServerHost(serverHost);
        setServerPort(serverPort);
        setServerSessionLife(serverSessionLife);
        setIncomingConnectionFactory(incomingCF);
        setOutgoingConnectionFactory(outgoingCF);
        setContextClassProvider(contextClassProvider);
        setIDFactory(idFactory);
        setHistoryService(historyService);
        setJobsUser(jobsUser);
        setBasicJobs(basicJobs);

        setDataSource(dataSource);
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
    
    /**
     * Sets the receiver's host name for web services to the given
     * value. If this method is not called during initialization, the
     * host name defaults to {@link Node#DEFAULT_HOST}.
     *
     * @param serverHost The host name.
     */
    public void setServerHost
        (String serverHost)
    {
        mServerHost=serverHost;
    }

    /**
     * Returns the receiver's host name for web services.
     *
     * @return The host name.
     */
    public String getServerHost()
    {
        return mServerHost;
    }

    /**
     * Sets the receiver's port for web services to the given
     * value. If this method is not called during initialization, the
     * port defaults to {@link Node#DEFAULT_WS_PORT}.
     *
     * @param serverPort The port.
     */
    public void setServerPort
        (int serverPort)
    {
        mServerPort=serverPort;
    }

    /**
     * Returns the receiver's port for web services.
     *
     * @return The port.
     */
    public int getServerPort()
    {
        return mServerPort;
    }

    /**
     * Sets the receiver's web services session lifetime to the given
     * value. If this method is not called during initialization, the
     * lifetime defaults to {@link
     * SessionManager#INFINITE_SESSION_LIFESPAN}.
     *
     * @param serverSessionLife The lifetime, in seconds.
     */
    public void setServerSessionLife
        (long serverSessionLife)
    {
        mServerSessionLife=serverSessionLife;
    }

    /**
     * Returns the receiver's web services session lifetime.
     *
     * @return The lifetime, in seconds.
     */
    public long getServerSessionLife()
    {
        return mServerSessionLife;
    }

    /**
     * Sets the receiver's connection factory for incoming connections
     * to the given one. A non-null value should be set during the
     * receiver's initialization.
     *
     * @param incomingCF The factory.
     */
    public void setIncomingConnectionFactory
        (ConnectionFactory incomingCF)
    {
        mIncomingCF=incomingCF;
    }

    /**
     * Returns the receiver's connection factory for incoming
     * connections.
     *
     * @return The factory.
     */
    public ConnectionFactory getIncomingConnectionFactory()
    {
        return mIncomingCF;
    }

    /**
     * Sets the receiver's connection factory for outgoing connections
     * to the given one. A non-null value should be set during the
     * receiver's initialization.
     *
     * @param outgoingCF The factory.
     */
    public void setOutgoingConnectionFactory
        (ConnectionFactory outgoingCF)
    {
        mOutgoingCF=outgoingCF;
    }
    
    /**
     * Returns the receiver's connection factory for outgoing
     * connections.
     *
     * @return The factory.
     */
    public ConnectionFactory getOutgoingConnectionFactory()
    {
        return mOutgoingCF;
    }
    
    /**
     * Sets the context class provider.
     *
     * @param contextClassProvider The context class provider.
     */
    public void setContextClassProvider
        (ContextClassProvider contextClassProvider)
    {
        mContextClassProvider=contextClassProvider;
    }

    /**
     * Gets the context class provider.
     *
     * @return The context context class provider.
     */
    public ContextClassProvider getContextClassProvider()
    {
        return mContextClassProvider;
    }

    /**
     * Sets the receiver's ID generation factory to the given one. A
     * non-null value should be set during the receiver's
     * initialization.
     *
     * @param idFactory The factory.
     */
    public void setIDFactory
        (IDFactory idFactory)
    {
        mIDFactory=idFactory;
    }

    /**
     * Returns the receiver's ID generation factory.
     *
     * @return The factory.
     */
    public IDFactory getIDFactory()
    {
        return mIDFactory;
    }

    /**
     * Sets the receiver's history service provider to the
     * given one. A non-null value should be set during the receiver's
     * initialization.
     *
     * @param historyService The provider.
     */
    public void setHistoryService
        (HistoryService historyService)
    {
        mHistoryService=historyService;
    }

    /**
     * Returns the receiver's history service provider.
     *
     * @return The provider.
     */
    public HistoryService getHistoryService()
    {
        return mHistoryService;
    }
    
    /**
     * Sets the jobs's user.
     *
     * @param jobsUser The user.
     */
    public void setJobsUser
        (String jobsUser)
    {
        try {
        	mJobsUser=(new SingleSimpleUserQuery(jobsUser)).fetch();
        } catch (PersistenceException ex) {
            throw new I18NRuntimeException
                (ex,new I18NBoundMessage1P
                 (Messages.CANNOT_RETRIEVE_JOBS_USER,jobsUser));
        }
    }

    /**
     * Returns the jobs's user.
     *
     * @return The user.
     */
    public SimpleUser getJobsUser()
    {
        return mJobsUser;
    }

    /**
     * Sets the list of user jobs.
     *
     * @param jobsUser The user.
     */
    public void setBasicJobs
        (List<BasicJob> basicJobs)
    {
    	mBasicJobs=basicJobs;
    }

    /**
     * Returns the list of user jobs.
     *
     * @return The list of user jobs.
     */
    public List<BasicJob> getBasicJobs()
    {
        return mBasicJobs;
    }

    /**
     * Sets the list of user jobs.
     *
     * @param jobsUser The data source.
     */
    public void setDataSource
        (ComboPooledDataSource dataSource)
    {
    	mDataSource=dataSource;
    }

    /**
     * Returns the pool data source.
     *
     * @return The data source.
     */
    public ComboPooledDataSource getDataSource()
    {
        return mDataSource;
    }
    
    // InitializingBean.
    @Override
    public void afterPropertiesSet()
        throws I18NException
    {
        if (getBrokers()==null) {
            throw new I18NException(Messages.NO_BROKERS);
        }
        if (getIncomingConnectionFactory()==null) {
            throw new I18NException(Messages.NO_INCOMING_CONNECTION_FACTORY);
        }
        if (getOutgoingConnectionFactory()==null) {
            throw new I18NException(Messages.NO_OUTGOING_CONNECTION_FACTORY);
        }
        if (getContextClassProvider()==null) {
            throw new I18NException(Messages.NO_CONTEXT_CLASS_PROVIDER);
        }
        if (getIDFactory()==null) {
            throw new I18NException(Messages.NO_ID_FACTORY);
        }
        if (getHistoryService()==null) {
            throw new I18NException(Messages.NO_REPORT_HISTORY_SERVICE);
        }
        if (getJobsUser()==null) {
            throw new I18NException(Messages.NO_JOBS_USER);
        }
        if (getBasicJobs()==null) {
            throw new I18NException(Messages.NO_BASIC_JOBS);
        }
        if (getDataSource()==null) {
            throw new I18NException(Messages.NO_DATA_SOURCE);
        }
    }
}
