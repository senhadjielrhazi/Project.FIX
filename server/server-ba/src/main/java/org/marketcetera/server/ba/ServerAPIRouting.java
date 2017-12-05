package org.marketcetera.server.ba;

import java.io.File;
import java.util.Date;

import org.apache.log4j.PropertyConfigurator;
import org.marketcetera.server.ba.brokers.Broker;
import org.marketcetera.server.ba.brokers.Brokers;
import org.marketcetera.server.ba.config.SpringConfig;
import org.marketcetera.core.ApplicationBase;
import org.marketcetera.core.ApplicationContainer;
import org.marketcetera.core.ApplicationVersion;
import org.marketcetera.info.SystemInfoImpl;
import org.marketcetera.quickfix.CurrentFIXDataDictionary;
import org.marketcetera.quickfix.FIXDataDictionary;
import org.marketcetera.quickfix.FIXVersion;
import org.marketcetera.util.except.I18NException;
import org.marketcetera.util.log.SLF4JLoggerProxy;
import org.marketcetera.util.misc.ClassVersion;
import org.marketcetera.util.quickfix.SpringSessionSettings;
import org.springframework.context.support.FileSystemXmlApplicationContext;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.jms.listener.SimpleMessageListenerContainer;

import quickfix.DefaultMessageFactory;
import quickfix.Message;
import quickfix.SessionNotFound;
import quickfix.ThreadedSocketAcceptor;
import quickfix.field.MsgType;
import quickfix.field.SenderCompID;
import quickfix.field.SendingTime;
import quickfix.field.TargetCompID;

/**
 * The main application. See {@link SpringConfig} for configuration
 * information.
 */
@ClassVersion("$Id: ServerAPIRouting.java 16664 2013-08-23 23:06:00Z colin $")
public class ServerAPIRouting
    extends ApplicationBase
{
    // CLASS DATA.
    private static final Class<?> LOGGER_CATEGORY = ServerAPIRouting.class;
    private static final String APP_CONTEXT_CFG_BASE = "file:" + CONF_DIR + "properties.xml"; //$NON-NLS-1$ //$NON-NLS-2$
    
    // INSTANCE DATA.
    private ApplicationContainer mApplication;
    private SimpleMessageListenerContainer mListener;
    private ThreadedSocketAcceptor mAcceptor;
    
    private final Brokers mBrokers;
    private final QuickFIXApplication mQFApp;
    
    /**
     * singleton instance reference
     */
    private static ServerAPIRouting mIinstance;

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
    public ServerAPIRouting(String[] args)
        throws Exception
    {
        // Obtain authorization credentials.
    	mIinstance = this;

        StaticApplicationContext parentContext=
            new StaticApplicationContext
            (new FileSystemXmlApplicationContext(APP_CONTEXT_CFG_BASE));
        parentContext.refresh();

        //Run the application.
        mApplication = new ApplicationContainer(parentContext, "baserver.xml");
        mApplication.start();
        mApplication.getContext().start();
        
        // Create system information.
        SystemInfoImpl systemInfo=new SystemInfoImpl();

        // Create resource managers.
        SpringConfig cfg=SpringConfig.getSingleton();
        if (cfg==null) {
            throw new I18NException(Messages.APP_NO_CONFIGURATION);
        }
        mBrokers=cfg.getBrokers();

        // Set dictionary for all QuickFIX/J messages we generate.
        CurrentFIXDataDictionary.setCurrentFIXDataDictionary
            (FIXDataDictionary.initializeDataDictionary
             (FIXVersion.FIX_SYSTEM.getDataDictionaryURL()));

        mQFApp=new QuickFIXApplication(systemInfo,getBrokers(),cfg.getSupportedMessages());
        
        // Initiate Initiator connections.
        SpringSessionSettings acceptorSettings=getBrokers().getSettings();
        mAcceptor = new ThreadedSocketAcceptor(mQFApp,
        		acceptorSettings.getQMessageStoreFactory(),
        		acceptorSettings.getQSettings(),
        		acceptorSettings.getQLogFactory(),
        		new DefaultMessageFactory());
        mAcceptor.start();
    }

    // INSTANCE METHODS.    
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
                SLF4JLoggerProxy.debug(ServerAPIRouting.class,
                                       "Broker {} requires FIX logout", //$NON-NLS-1$
                                       broker.getBrokerID());
                Message logout = broker.getFIXMessageFactory().createMessage(MsgType.LOGOUT);
                // set mandatory fields
                logout.getHeader().setField(new SenderCompID(broker.getDescriptor().getDictionary().get("SenderCompID")));
                logout.getHeader().setField(new TargetCompID(broker.getDescriptor().getDictionary().get("TargetCompID")));
                logout.getHeader().setField(new SendingTime(new Date()));
                logout.toString();
                try {
                    SLF4JLoggerProxy.debug(ServerAPIRouting.class,
                                           "Sending logout message {} to broker {}", //$NON-NLS-1$
                                           logout,
                                           broker.getBrokerID());
                    broker.sendToTarget(logout);
                } catch (SessionNotFound e) {
                    SLF4JLoggerProxy.warn(ServerAPIRouting.class,
                                          e,
                                          "Unable to logout from {}",
                                          broker.getBrokerID());
                }
            } else {
                SLF4JLoggerProxy.debug(ServerAPIRouting.class,
                                       "Broker {} does not require FIX logout", //$NON-NLS-1$
                                       broker.getBrokerID());
            }
        }
        
        if (mAcceptor!=null) {
            mAcceptor.stop();
            mAcceptor=null;
        }
        if (mListener!=null) {
            mListener.shutdown();
            mListener=null;
        }
        if (mApplication!=null) {
        	mApplication.stop();
        	mApplication=null;
        }
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
     * Gets the <code>ServerAPIRouting</code> value.
     *
     * @return an <code>ServerAPIRouting</code> value
     */
    public static ServerAPIRouting getInstance()
    {
        return mIinstance;
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
        final ServerAPIRouting server;
        try {
        	server=new ServerAPIRouting(args);
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
