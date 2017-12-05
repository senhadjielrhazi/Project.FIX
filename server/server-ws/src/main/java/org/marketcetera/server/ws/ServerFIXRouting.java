package org.marketcetera.server.ws;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.util.Date;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.log4j.PropertyConfigurator;
import org.marketcetera.server.ws.brokers.Broker;
import org.marketcetera.server.ws.brokers.Brokers;
import org.marketcetera.client.ws.WSService;
import org.marketcetera.core.ApplicationBase;
import org.marketcetera.core.ApplicationContainer;
import org.marketcetera.core.ApplicationVersion;
import org.marketcetera.info.SystemInfo;
import org.marketcetera.info.SystemInfoImpl;
import org.marketcetera.server.ws.config.JobSession;
import org.marketcetera.server.ws.config.SpringConfig;
import org.marketcetera.server.ws.history.HistoryService;
import org.marketcetera.quickfix.CurrentFIXDataDictionary;
import org.marketcetera.quickfix.FIXDataDictionary;
import org.marketcetera.quickfix.FIXVersion;
import org.marketcetera.util.auth.StandardAuthentication;
import org.marketcetera.util.except.I18NException;
import org.marketcetera.util.log.I18NBoundMessage;
import org.marketcetera.util.log.SLF4JLoggerProxy;
import org.marketcetera.util.misc.ClassVersion;
import org.marketcetera.util.quickfix.SpringSessionSettings;
import org.marketcetera.util.spring.SpringUtils;
import org.marketcetera.util.ws.stateful.Server;
import org.marketcetera.util.ws.stateful.SessionHolder;
import org.marketcetera.util.ws.stateful.SessionManager;
import org.marketcetera.util.ws.stateless.StatelessClientContext;
import org.marketcetera.util.ws.tags.SessionId;
import org.marketcetera.client.ws.jms.JmsManager;
import org.marketcetera.server.ws.security.ClientSession;
import org.marketcetera.server.ws.security.ClientSessionFactory;
import org.marketcetera.server.ws.security.DBAdmin;
import org.marketcetera.ws.server.BasicJob;
import org.marketcetera.ws.server.security.SimpleUser;
import org.marketcetera.server.ws.security.WSAuthenticator;
import org.marketcetera.server.ws.LocalIDFactory;
import org.quickfixj.jmx.JmxExporter;
import org.springframework.context.support.FileSystemXmlApplicationContext;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.jms.listener.SimpleMessageListenerContainer;

import quickfix.DefaultMessageFactory;
import quickfix.Message;
import quickfix.SessionNotFound;
import quickfix.ThreadedSocketInitiator;
import quickfix.field.MsgType;
import quickfix.field.SenderCompID;
import quickfix.field.SendingTime;
import quickfix.field.TargetCompID;

/**
 * The main application. See {@link SpringConfig} for configuration
 * information.
 */
@ClassVersion("$Id: ServerFIXRouting.java 16664 2013-08-23 23:06:00Z colin $")
public class ServerFIXRouting
    extends ApplicationBase
{
    // CLASS DATA.
    private static final Class<?> LOGGER_CATEGORY = ServerFIXRouting.class;
    private static final String APP_CONTEXT_CFG_BASE = "file:" + CONF_DIR + "properties.xml"; //$NON-NLS-1$ //$NON-NLS-2$
    private static final String JMX_NAME = "org.marketcetera.server.ws.security:type=DBAdmin"; //$NON-NLS-1$

    // INSTANCE DATA.
    private ApplicationContainer mApplication;
    private SimpleMessageListenerContainer mListener;
    private ThreadedSocketInitiator mInitiator;
    
    private final StandardAuthentication mAuth;
    private final Brokers mBrokers;
    private final QuickFIXApplication mQFApp;
    private final ServerPersister mServerPersister;
    private final ServerManager mServerManager;
    private final JobSession mSession;
    
    /**
     * singleton instance reference
     */
    private static ServerFIXRouting mIinstance;
    
    /**
     * Provides server-side WS services
     */
    private Server<ClientSession> mServer;

    // CONSTRUCTORS.
    /**
     * Creates a new application given the command-line arguments. The
     * application spawns child threads, but the constructor does not
     * block while waiting for those threads to terminate; instead, it
     * returns as soon as construction is complete.
     *
     * @param args The command-line arguments.
     *
     * @throws Exception Thrown if construction fails.
     */
    public ServerFIXRouting(String[] args)
        throws Exception
    {
        // Obtain authorization credentials.
    	mIinstance = this;
        mAuth=new StandardAuthentication(APP_CONTEXT_CFG_BASE,args);
        if (!getAuth().setValues()) {
            printUsage(Messages.APP_MISSING_CREDENTIALS);
        }
        args=getAuth().getOtherArgs();
        if (args.length!=0) {
            printUsage(Messages.APP_NO_ARGS_ALLOWED);
        }
        StaticApplicationContext parentContext=
            new StaticApplicationContext
            (new FileSystemXmlApplicationContext(APP_CONTEXT_CFG_BASE));
        SpringUtils.addStringBean
            (parentContext,USERNAME_BEAN_NAME,
             getAuth().getUser());
        SpringUtils.addStringBean
            (parentContext,PASSWORD_BEAN_NAME,
             getAuth().getPasswordAsString());
        parentContext.refresh();

        //Run the application.
        mApplication = new ApplicationContainer(parentContext, "wsserver.xml");
        mApplication.start();
        mApplication.getContext().start();
        
        // Create system information.
        SystemInfoImpl systemInfo=new SystemInfoImpl();

        // Create resource managers.
        SpringConfig cfg=SpringConfig.getSingleton();
        if (cfg==null) {
            throw new I18NException(Messages.APP_NO_CONFIGURATION);
        }
        cfg.getIDFactory().init();
        JmsManager jmsMgr=new JmsManager
            (cfg.getIncomingConnectionFactory(),
             cfg.getOutgoingConnectionFactory(), 
             cfg.getContextClassProvider());
        HistoryService historyService=cfg.getHistoryService();
        systemInfo.setValue
            (SystemInfo.HISTORY_SERVICES,historyService);
        mBrokers=cfg.getBrokers();
        mServerPersister=new ServerPersisterImpl(historyService);
        historyService.init(cfg.getIDFactory(),mServerPersister,cfg.getDataSource());

        // Set dictionary for all QuickFIX/J messages we generate.
        CurrentFIXDataDictionary.setCurrentFIXDataDictionary
            (FIXDataDictionary.initializeDataDictionary
             (FIXVersion.FIX_SYSTEM.getDataDictionaryURL()));

        // Setup the server manager
        ClientSessionFactory sessionFactorty=new ClientSessionFactory(systemInfo,jmsMgr);
        mServerManager=new ServerManager(sessionFactorty, cfg.getServerSessionLife()==
                (SessionManager.INFINITE_SESSION_LIFESPAN)?
               SessionManager.INFINITE_SESSION_LIFESPAN:
               (cfg.getServerSessionLife()*1000)); 
        sessionFactorty.addSessionListener(mServerManager); 
        sessionFactorty.addSessionListener(mServerPersister);
        
        // Initiate web services.
        mServer=new Server<ClientSession>
        (cfg.getServerHost(),cfg.getServerPort(),
         new WSAuthenticator(),mServerManager);
        mServer.publish(new WSServiceImpl(mServerManager,
                                   getBrokers(),
                                   cfg.getIDFactory(),
                                   historyService),
        						   WSService.class);
        
        // Initiate JMS.
        LocalIDFactory localIdFactory=new LocalIDFactory(cfg.getIDFactory());
        localIdFactory.init();
        RequestHandler handler=new RequestHandler
            (getBrokers(),cfg.getAllowedRequests()
            		,mServerPersister,mServerManager,localIdFactory);
        mListener=jmsMgr.getIncomingJmsFactory().registerRequestHandler
            (handler,JmsManager.getRequestTopicName(),false);
        
        // Initiate the system session
        SessionId sessionId=SessionId.generate();
        SimpleUser jobsUser=cfg.getJobsUser();
        StatelessClientContext context=new StatelessClientContext();
        SessionHolder<ClientSession>holder=new SessionHolder<>(jobsUser.getName(), context);
        mSession=new JobSession(holder,systemInfo,sessionId,jobsUser);
        mServerManager.put(sessionId, holder, mSession);
        mServerManager.addSession(mSession);
        
        //Hook the job data updates
        for(BasicJob job:cfg.getBasicJobs()){
        	mSession.addDataReceiver(job);
        	job.run();
        }
        
        mQFApp=new QuickFIXApplication(systemInfo,getBrokers(),cfg.getSupportedMessages(),
        		mServerPersister,
        		mServerManager);
        
        // Initiate Initiator connections.
        SpringSessionSettings initiatorSettings=getBrokers().getSettings();
        mInitiator = new ThreadedSocketInitiator(mQFApp,
        		initiatorSettings.getQMessageStoreFactory(),
        		initiatorSettings.getQSettings(),
        		initiatorSettings.getQLogFactory(),
        		new DefaultMessageFactory());
        mInitiator.start();
        
        // Initiate JMX (for QuickFIX/J and application MBeans).
        MBeanServer mbeanServer=ManagementFactory.getPlatformMBeanServer();
        (new JmxExporter(mbeanServer)).register(mInitiator);
        mbeanServer.registerMBean(new DBAdmin(getBrokers(),
                                               localIdFactory,
                                               mServerManager),
                                  new ObjectName(JMX_NAME));
    }

    // INSTANCE METHODS.
    /**
     * Gets the <code>ServerReceiver</code> value.
     *
     * @return an <code>ServerReceiver</code> value
     */
    public ServerReceiver getServerReceiver()
    {
        return mQFApp;
    }
    
    /**
     * Prints the given message alongside usage information on the
     * standard error stream, and throws an exception.
     *
     * @param message The message.
     *
     * @throws IllegalStateException Always thrown.
     */
    private void printUsage
        (I18NBoundMessage message)
        throws I18NException
    {
        System.err.println(message.getText());
        System.err.println(Messages.APP_USAGE.getText
                           (ServerFIXRouting.class.getName()));
        System.err.println(Messages.APP_AUTH_OPTIONS.getText());
        System.err.println();
        getAuth().printUsage(System.err);
        throw new I18NException(message);
    }

    /**
     * Stops worker threads of the receiver.
     *
     * @return The context.
     */
    synchronized void stop()
    {
        Brokers brokers = getBrokers();
        for(Broker broker : brokers.getBrokers()) {
            if(broker.getFixLogoutRequired()) {
                SLF4JLoggerProxy.debug(ServerFIXRouting.class,
                                       "Broker {} requires FIX logout", //$NON-NLS-1$
                                       broker.getBrokerID());
                Message logout = broker.getFIXMessageFactory().createMessage(MsgType.LOGOUT);
                // set mandatory fields
                logout.getHeader().setField(new SenderCompID(broker.getDescriptor().getDictionary().get("SenderCompID")));
                logout.getHeader().setField(new TargetCompID(broker.getDescriptor().getDictionary().get("TargetCompID")));
                logout.getHeader().setField(new SendingTime(new Date()));
                logout.toString();
                try {
                    SLF4JLoggerProxy.debug(ServerFIXRouting.class,
                                           "Sending logout message {} to broker {}", //$NON-NLS-1$
                                           logout,
                                           broker.getBrokerID());
                    broker.sendToTarget(logout);
                } catch (SessionNotFound e) {
                    SLF4JLoggerProxy.warn(ServerFIXRouting.class,
                                          e,
                                          "Unable to logout from {}",
                                          broker.getBrokerID());
                }
            } else {
                SLF4JLoggerProxy.debug(ServerFIXRouting.class,
                                       "Broker {} does not require FIX logout", //$NON-NLS-1$
                                       broker.getBrokerID());
            }
        }
        
        if (mInitiator!=null) {
            mInitiator.stop();
            mInitiator=null;
        }
        if (mListener!=null) {
            mListener.shutdown();
            mListener=null;
        }
        if (mApplication!=null) {
        	mApplication.stop();
        	mApplication=null;
        }
        if (mServer!=null) {
        	mServer.stop();
        	mServer=null;
        }
    }

    /**
     * Returns the receiver's authentication system.
     *
     * @return The authentication system.
     */
    StandardAuthentication getAuth()
    {
        return mAuth;
    }

    /**
     * Gets the <code>Brokers</code> value.
     *
     * @return an <code>Brokers</code> value
     */
    Brokers getBrokers()
    {
        return mBrokers;
    }
    
    // CLASS METHODS.
    /**
     * Gets the <code>ServerFIXRouting</code> value.
     *
     * @return an <code>ServerFIXRouting</code> value
     */
    public static ServerFIXRouting getInstance()
    {
        return mIinstance;
    }
    
    /**
     * Gets the <code>ServerPersister</code> value.
     *
     * @return an <code>ServerPersister</code> value
     */
    public ServerPersister getServerPersister()
    {
        return mServerPersister;
    }
    
    /**
     * Gets the <code>ServerManager</code> value.
     *
     * @return an <code>ServerManager</code> value
     */
    public ServerManager getServerManager()
    {
        return mServerManager;
    }
	
    /**
	 * Gets the job session 
	 * 
	 * @return session the job session
	 */
	public JobSession getJobSession()
	{
		return mSession;
	}
    
    /**
     * Main program.
     *
     * @param args The command-line arguments.
     */
    public static void main
        (String[] args)
    {
        // Configure logger.
        PropertyConfigurator.configureAndWatch
            (ApplicationBase.CONF_DIR+"log4j"+ //$NON-NLS-1$
             File.separator+"server.properties", //$NON-NLS-1$
             LOGGER_WATCH_DELAY);

        // Log application start.
        Messages.APP_COPYRIGHT.info(LOGGER_CATEGORY);
        Messages.APP_VERSION_BUILD.info(LOGGER_CATEGORY,
                ApplicationVersion.getVersion(),
                ApplicationVersion.getBuildNumber());
        Messages.APP_START.info(LOGGER_CATEGORY);

        // Start FIX Service.
        final ServerFIXRouting server;
        try {
        	server=new ServerFIXRouting(args);
        } catch (Throwable t) {
            try {
                Messages.APP_STOP_ERROR.error(LOGGER_CATEGORY,t);
            } catch (Throwable t2) {
                System.err.println("Reporting failed"); //$NON-NLS-1$
                System.err.println("Reporting failure"); //$NON-NLS-1$
                t2.printStackTrace();
                System.err.println("Original failure"); //$NON-NLS-1$
                t.printStackTrace();
            }
            return;
        }
        Messages.APP_STARTED.info(LOGGER_CATEGORY);

        // Hook to log shutdown.
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
            	server.stop();
                Messages.APP_STOP.info(LOGGER_CATEGORY);
            }
        });

        // Execute application.
        try {
        	server.startWaitingForever();
        } catch (Throwable t) {
            try {
                Messages.APP_STOP_ERROR.error(LOGGER_CATEGORY,t);
            } catch (Throwable t2) {
                System.err.println("Reporting failed"); //$NON-NLS-1$
                System.err.println("Reporting failure"); //$NON-NLS-1$
                t2.printStackTrace();
                System.err.println("Original failure"); //$NON-NLS-1$
                t.printStackTrace();
            }
            return;
        }
        Messages.APP_STOP_SUCCESS.info(LOGGER_CATEGORY);
    }
}
