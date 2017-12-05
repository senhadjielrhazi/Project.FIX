package org.marketcetera.server.sa;

import java.io.File;
import java.io.IOException;
import java.io.LineNumberReader;
import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.management.JMX;
import javax.management.ObjectName;

import org.apache.log4j.PropertyConfigurator;
import org.joda.time.DateTime;
import org.marketcetera.client.sa.SAService;
import org.marketcetera.core.ApplicationBase;
import org.marketcetera.core.ApplicationContainer;
import org.marketcetera.core.ApplicationVersion;
import org.marketcetera.core.notifications.Notification;
import org.marketcetera.core.notifications.NotificationExecutor;
import org.marketcetera.module.DataFlowID;
import org.marketcetera.module.ModuleManager;
import org.marketcetera.module.ModuleManagerMXBean;
import org.marketcetera.module.SinkDataListener;
import org.marketcetera.server.sa.config.SpringConfig;
import org.marketcetera.server.sa.security.ClientSession;
import org.marketcetera.server.sa.security.SAAuthenticator;
import org.marketcetera.util.auth.StandardAuthentication;
import org.marketcetera.util.except.I18NException;
import org.marketcetera.util.log.I18NBoundMessage;
import org.marketcetera.util.spring.SpringUtils;
import org.marketcetera.util.unicode.UnicodeFileReader;
import org.marketcetera.util.ws.stateful.Server;
import org.marketcetera.util.ws.stateful.SessionManager;
import org.springframework.context.support.FileSystemXmlApplicationContext;
import org.springframework.context.support.StaticApplicationContext;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class StrategyAgent extends ApplicationBase {
   
	// CLASS DATA.
    private static final Class<?> LOGGER_CATEGORY = StrategyAgent.class;
    private static final String APP_CONTEXT_CFG_BASE = "file:" + CONF_DIR + "properties.xml"; //$NON-NLS-1$ //$NON-NLS-2$
    public static final String SINK_DATA = "SINK";  //$NON-NLS-1$
    
    // INSTANCE DATA.
    /**
    * The table of command names and command runners.
    */
   private static final Map<String, CommandRunner> sRunners = Maps.newHashMap();
   private List<Command> mCommands = Lists.newArrayList();
   private final AtomicBoolean running = new AtomicBoolean(false);
   static {
       //Initialize the set of available command runners
       addRunner(new CreateModule());
       addRunner(new CreateDataFlow());
       addRunner(new StartModule());
   }
   
    private ApplicationContainer mApplication;
    private final StandardAuthentication mAuth;
    private ModuleManager mModuleManager;
    private ModuleManagerMXBean mManagerBean;
    private NotificationExecutor notificationExecutor;
    
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
    public StrategyAgent(String[] args)
        throws Exception
    {
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
         mApplication = new ApplicationContainer(parentContext, "saserver.xml");
         mApplication.start();
         mApplication.getContext().start();
         
         // Create resource managers.
         SpringConfig cfg=SpringConfig.getSingleton();
         if (cfg==null) {
             throw new I18NException(Messages.APP_NO_CONFIGURATION);
         }
         
         //Sets the class loader
         Thread.currentThread().setContextClassLoader(cfg.getClassLoader());
         
         //Configure the application. If it fails, exit
         try { 
             if(args != null && args.length > 0) {
                 int parseErrors = parseCommands(args[0]);
                 if(parseErrors > 0) {
                     Messages.LOG_COMMAND_PARSE_ERRORS.error(StrategyAgent.class,
                                                             parseErrors);
                     throw new IllegalArgumentException(Messages.LOG_COMMAND_PARSE_ERRORS.getText(parseErrors));
                 }
             }
         } catch(Exception e) {
             Messages.LOG_ERROR_CONFIGURE_AGENT.error(StrategyAgent.class,
                                                      getMessage(e));
             Messages.LOG_ERROR_CONFIGURE_AGENT.debug(StrategyAgent.class,
                                                      e,
                                                      getMessage(e));
             throw new RuntimeException(e);
         }
         
         //Initialize the module manager.
         mModuleManager = cfg.getModuleManager();
         mModuleManager.init();
         //Add the logger sink listener
         mModuleManager.addSinkListener(new SinkDataListener() {
             public void receivedData(DataFlowID inFlowID, Object inData) {
                 final boolean isNullData = inData == null;
                 Messages.LOG_SINK_DATA.info(SINK_DATA,
                                             inFlowID,
                                             isNullData ? 0: 1,
                                             isNullData ? null: inData.getClass().getName(),
                                             inData);
             }
         });
         mManagerBean = JMX.newMXBeanProxy(ManagementFactory.getPlatformMBeanServer(),
                                               new ObjectName(ModuleManager.MODULE_MBEAN_NAME),
                                               ModuleManagerMXBean.class);
     
         // run the commands
         executeCommands();
         if(notificationExecutor != null) {
             notificationExecutor.notify(Notification.low("Strategy Agent Started",
                                                          "Stategy Agent Started at " + new DateTime(),
                                                          StrategyAgent.class.getSimpleName()));
         }
         running.set(true);
         
         // Setup the session manager
         SessionManager<ClientSession> sessionManager = cfg.getSessionManager();
         
         // Initiate web services.
         mServer = new Server<ClientSession>
         (cfg.getServerHost(),cfg.getServerPort(),
          new SAAuthenticator(),sessionManager);
         mServer.publish(new SAServiceImpl(sessionManager,
        		 					mModuleManager,
                                    cfg.getDataPublisher()),
         						   SAService.class);
    }
    
    /**
     * Parses the commands from the supplied commands file.
     *
     * @param inFile the file path
     *
     * @return the number of errors encountered when parsing the command file.
     * @throws IOException if there were errors parsing the file.
     */
    private int parseCommands(String inFile) throws IOException {
        int numErrors = 0;
        LineNumberReader reader = new LineNumberReader(
                new UnicodeFileReader(inFile));
        try {
            String line;
            while((line = reader.readLine()) != null) {
                if(line.startsWith("#") || line.trim().isEmpty()) {  //$NON-NLS-1$
                    //Ignore comments and empty lines.
                    continue;
                }
                int idx = line.indexOf(';');  //$NON-NLS-1$
                if(idx > 0) {
                    String key = line.substring(0, idx);
                    CommandRunner runner = sRunners.get(key);
                    if(runner == null) {
                        numErrors++;
                        Messages.INVALID_COMMAND_NAME.error(this, key,
                                reader.getLineNumber());
                        continue;
                    }
                    mCommands.add(new Command(runner, line.substring(++idx),
                            reader.getLineNumber()));
                } else {
                    numErrors++;
                    Messages.INVALID_COMMAND_SYNTAX.error(this,
                            line, reader.getLineNumber());
                }
            }
            return numErrors;
        } finally {
            reader.close();
        }
    }
    
    /**
     * Executes commands, if any were provided. If any command fails, the
     * failure is logged. Failure of any command doesn't prevent the next
     * command from executing or prevent the application from exiting.
     */
    private void executeCommands() {
        if(!mCommands.isEmpty()) {
            for(Command c: mCommands) {
                try {
                    Messages.LOG_RUNNING_COMMAND.info(this,
                            c.getRunner().getName(), c.getParameter());
                    Object result = c.getRunner().runCommand(
                            mManagerBean, c.getParameter());
                    Messages.LOG_COMMAND_RUN_RESULT.info(this,
                            c.getRunner().getName(), result);
                } catch (Exception e) {
                    Messages.LOG_ERROR_EXEC_CMD.warn(this,
                            c.getRunner().getName(),
                            c.getParameter(), c.getLineNum(),
                            getMessage(e));
                    Messages.LOG_ERROR_EXEC_CMD.debug(this, e,
                            c.getRunner().getName(),
                            c.getParameter(), c.getLineNum(),
                            getMessage(e));
                }
            }
        }
    }
    
    /**
     * Adds a command runner instance to the table of command runners.
     *
     * @param inRunner the command runner to be added.
     */
    private static void addRunner(CommandRunner inRunner) {
        sRunners.put(inRunner.getName(), inRunner);
    }
    
    /**
     * Stops worker threads of the receiver.
     *
     * @return The context.
     */
    synchronized void stop()
    {
        if (mApplication!=null) {
        	mApplication.stop();;
            mApplication=null;
        }
        if (mServer!=null) {
        	mServer.stop();
        	mServer=null;
        }
        running.set(false);
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
     * Return the exception message from the supplied Throwable.
     *
     * @param inThrowable the throwable whose message needs to be returned.
     *
     * @return the throwable message.
     */
    private static String getMessage(Throwable inThrowable) {
        if(inThrowable instanceof I18NException) {
            return ((I18NException)inThrowable).getLocalizedDetail();
        } else {
            return inThrowable.getLocalizedMessage();
        }
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
                           (StrategyAgent.class.getName()));
        System.err.println(Messages.APP_AUTH_OPTIONS.getText());
        System.err.println();
        getAuth().printUsage(System.err);
        throw new I18NException(message);
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

        // Start ORS.
        final StrategyAgent ors;
        try {
            ors=new StrategyAgent(args);
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
                ors.stop();
                Messages.APP_STOP.info(LOGGER_CATEGORY);
            }
        });

        // Execute application.
        try {
            ors.startWaitingForever();
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