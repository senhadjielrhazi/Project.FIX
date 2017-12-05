
package org.marketcetera.server.ws.util;

import org.apache.log4j.PropertyConfigurator;
import org.marketcetera.core.ApplicationBase;
import org.marketcetera.util.log.SLF4JLoggerProxy;
import org.marketcetera.util.misc.ClassVersion;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.io.File;
import java.util.Arrays;

/* $License$ */
/**
 * Initializes DB for ORS, so that it can be
 *
 * @author anshul@marketcetera.com
 */
@ClassVersion("$Id: DBInit.java 16154 2012-07-14 16:34:05Z colin $")
public class DBInit
    extends ApplicationBase {
	
    public static final File TEST_ROOT = new File("src" + //$NON-NLS-1$
            File.separator + "test"); //$NON-NLS-1$
    public static final File TEST_SAMPLE_DATA = new File(TEST_ROOT, "sample_data"); //$NON-NLS-1$
    public static final File TEST_CONF = new File(TEST_SAMPLE_DATA, "conf"); //$NON-NLS-1$
    public static final File LOGGER_CONFIG = new File(TEST_CONF, "log4j.properties"); //$NON-NLS-1$
    
    /**
     * Initialize the schema and create the admin user
     * Close the spring context so that Server can startup
     *
     * @throws Exception if there was an error.
     */
    public static void initORSDB() throws Exception {
        //Close the spring context so that Server can startup
        springSetup
            (new String[] {
                "file:"+CONF_DIR+ //$NON-NLS-1$
                "dbinit.xml"}).close(); //$NON-NLS-1$*/
    }
    
    /**
     * Sets up the spring configuration for the given spring configuration
     * files
     *
     * @param configFiles the spring configuration file names
     *
     * @throws Exception if there was an error
     * @return the initialized spring context
     */
    public static AbstractApplicationContext springSetup(
            String[] configFiles) throws Exception {
        return springSetup(configFiles,null);
    }
    
    /**
     * Sets up the spring configuration for the given spring configuration
     * files
     *
     * @param configFiles the spring configuration file names
     *
     * @param parent the parent spring configuration, if one is available.
     *  Can be null.
     * @throws Exception if there was an error
     * @return the initialized spring context
     */
    public static AbstractApplicationContext springSetup(
            String[] configFiles,
            ApplicationContext parent) throws Exception {
        try {
            logSetup();
            //runs multiple tests in the same vm (which it currently does)
            ClassPathXmlApplicationContext context;
            if (parent == null) {
                context = new ClassPathXmlApplicationContext(configFiles);
            } else {
                context = new ClassPathXmlApplicationContext(
                        configFiles, parent);
            }
            context.registerShutdownHook();
            return context;
        } catch (Exception e) {
            SLF4JLoggerProxy.error(DBInit.class, "FailedSetup:", e); //$NON-NLS-1$
            throw e;
        }
    }
    
    /**
     * Sets up logging.
     */
    public static void logSetup() {
        if(!LOGGER_CONFIG.exists()) {
            SLF4JLoggerProxy.warn(DBInit.class,
                    "logger configuration file {} not found", //$NON-NLS-1$
                    LOGGER_CONFIG.getAbsolutePath());
        }
        PropertyConfigurator.configureAndWatch
            (LOGGER_CONFIG.getAbsolutePath(), 10 * 1000l); //10 seconds
    }    
    
    /**
     * Runs the main method in the class supplied as the
     * first argument after initializing the DB.
     * 
     * @param args the class name of the main class followed
     * by any arguments to that class.
     */
    public static void main(String[] args) {
        try {
            initORSDB();
            //Verify that atleast the class name is supplied
            Class.forName(args[0]).getDeclaredMethod("main",
                    String[].class).invoke(null,
                    (Object)Arrays.copyOfRange(args,1,args.length));
        } catch (Throwable t) {
            t.printStackTrace();
            SLF4JLoggerProxy.error(DBInit.class,"Error",t);
        }

    }
}
