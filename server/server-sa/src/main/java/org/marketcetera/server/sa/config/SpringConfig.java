package org.marketcetera.server.sa.config;

import org.marketcetera.core.publisher.PublisherEngine;
import org.marketcetera.module.ModuleManager;
import org.marketcetera.server.sa.security.ClientSession;
import org.marketcetera.util.except.I18NException;
import org.marketcetera.util.ws.stateful.SessionManager;
import org.marketcetera.util.ws.stateless.Node;
import org.springframework.beans.factory.InitializingBean;

public class SpringConfig implements InitializingBean {
    
	// CLASS DATA.
    private static SpringConfig sSingleton;

    // INSTANCE DATA.
    private ModuleManager moduleManager;
    private ClassLoader classLoader;
    private volatile PublisherEngine dataPublisher;
    private String mServerHost;
    private int mServerPort;
    private SessionManager<ClientSession> mSessionManager;
    
    // CONSTRUCTORS.
    /**
     * Creates a new application configuration, which becomes the
     * global singleton.
     */
    public SpringConfig()
    {
        mServerHost=Node.DEFAULT_HOST;
        mServerPort=Node.DEFAULT_SA_PORT;
        setSingleton(this);
    }
    
    /**
     * Creates a new application configuration with the given
     * properties. The new configuration becomes the global singleton.
     *
     * @param moduleManager The module manager.
     * @param classLoader The class loader.
     * @param dataPublisher The data publisher.
     * @param serverHost The host name for web services.
     * @param serverPort The port for web services.
     * @param sessionManager The session manager
     */
    public SpringConfig
        (ModuleManager moduleManager,
         ClassLoader classLoader,
         PublisherEngine dataPublisher,
         String serverHost,
         int serverPort,
         SessionManager<ClientSession> sessionManager)
        throws I18NException
    {
        setModuleManager(moduleManager);
        setClassLoader(classLoader);
        setDataPublisher(dataPublisher);
        setServerHost(serverHost);
        setServerPort(serverPort);
        setSessionManager(sessionManager);
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
    
    /**
     * Returns module manager instance.
     * 
     * @return the ModuleManager
     */
	public ModuleManager getModuleManager() {
		return moduleManager;
	}
    
	/**
	 * Sets the module manager.
	 * 
	 * @param moduleManager the ModuleManager
	 */
	public void setModuleManager(ModuleManager moduleManager) {
		this.moduleManager = moduleManager;
	}
	
    /**
     * Get the loader value.
     *
     * @return a <code>ClassLoader</code> value
     */
	public ClassLoader getClassLoader() {
		return classLoader;
	}
	
    /**
     * Sets the loader value.
     *
     * @param inClassLoader a <code>ClassLoader</code> value
     */
	public void setClassLoader(ClassLoader inClassLoader) {
		this.classLoader = inClassLoader;
	}
	
    /**
     * Get the dataPublisher value.
     *
     * @return a <code>PublisherEngine</code> value
     */
	public PublisherEngine getDataPublisher() {
		return dataPublisher;
	}
    
    /**
     * Sets the dataPublisher value.
     *
     * @param inDataPublisher a <code>PublisherEngine</code> value
     */
	public void setDataPublisher(PublisherEngine inDataPublisher) {
		this.dataPublisher = inDataPublisher;
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
     * Sets the session manager.
     *
     * @param sessionManager The SessionManager.
     */
    public void setSessionManager
        (SessionManager<ClientSession> sessionManager)
    {
        mSessionManager=sessionManager;
    }

    /**
     * Returns the session manager.
     *
     * @return The SessionManager.
     */
    public SessionManager<ClientSession> getSessionManager()
    {
        return mSessionManager;
    }
    
	@Override
	public void afterPropertiesSet() 
			throws I18NException {
		if (getModuleManager()==null) {
			throw new I18NException(Messages.NO_MODULE_MANAGER);
		}
		if (getClassLoader()==null) {
			throw new I18NException(Messages.NO_CLASS_LOADER);
		}
		if (getDataPublisher()==null) {
			throw new I18NException(Messages.NO_DATA_PUBLISHER);
		}
        if (getSessionManager()==null) {
            throw new I18NException(Messages.NO_SESSION_MANAGER);
        }
	}
}
