package org.marketcetera.server.sa;

import org.marketcetera.util.misc.ClassVersion;
import org.marketcetera.module.PropertiesConfigurationProvider;
import org.marketcetera.module.ModuleURN;
import org.marketcetera.client.ws.module.WSClientModuleFactory;
import org.marketcetera.module.ModuleException;
import org.marketcetera.modules.remote.receiver.ReceiverFactory;

import java.util.Map;
import java.util.HashMap;

/**
 * The custom strategy agent configuration provider.
 * <p>
 * The provider configures the receiver module's properties from the
 * strategy agent configuration file instead of the remote receiver
 * properties file. 
 */
@ClassVersion("$Id: SAConfigurationProvider.java 16154 2012-07-14 16:34:05Z colin $")
public class SAConfigurationProvider extends PropertiesConfigurationProvider {
    /**
     * Creates a new instance.
     *
     * @param inLoader the classloader to use to fetch the module
     * properties files.
     */
    public SAConfigurationProvider(ClassLoader inLoader) {
        super(inLoader);
    }

    @Override
    public String getDefaultFor(ModuleURN inURN, String inAttribute) throws ModuleException {
        String propertyValue = null;
        synchronized (this) {
            if(ReceiverFactory.INSTANCE_URN.equals(inURN)) {
                if(mReceiverProperties != null) {
                    propertyValue = mReceiverProperties.get(inAttribute);
                }
            }
            if(WSClientModuleFactory.INSTANCE_URN.parent().equals(inURN)) {
                if(mClientProperties != null) {
                    propertyValue = mClientProperties.get(inAttribute);
                }
            }
        }
        //fallback on the properties file, in case we fail to add new
        //client/receiver module properties to the spring configuration.
        return propertyValue != null
                ? propertyValue
                : super.getDefaultFor(inURN, inAttribute);
    }

    /**
     * The properties for the receiver module. Each property key
     * corresponds to the property exposed by
     * {@link org.marketcetera.modules.remote.receiver.ReceiverModuleMXBean}. 
     *
     * @return the properties for the receiver module.
     */
    public synchronized Map<String, String> getReceiverProperties() {
        return mReceiverProperties == null
                ? null
                : new HashMap<String,String>(mReceiverProperties);
    }

    /**
     * Sets the properties for the receiver module.
     *
     * @param inReceiverProperties the properties for the reeiver module.
     */
    public synchronized void setReceiverProperties(Map<String, String> inReceiverProperties) {
        mReceiverProperties = inReceiverProperties == null
                ? null
                : new HashMap<String,String>(inReceiverProperties);
    }

    /**
     * The properties for the client module. Each property key corresponds
     * to the property exposed by
     * {@link org.marketcetera.ws.client.ClientModuleFactoryMXBean}.
     *
     * @return the properties for the client module.
     */
    public synchronized Map<String, String> getClientProperties() {
        return mClientProperties == null
                ? null
                : new HashMap<String,String>(mClientProperties);
    }

    /**
     * Sets the properties for the client module.
     *
     * @param inClientProperties the properties for the client module.
     */
    public synchronized void setClientProperties(Map<String, String> inClientProperties) {
        mClientProperties = inClientProperties == null
                ? null
                : new HashMap<String,String>(inClientProperties);
    }

    private Map<String,String> mReceiverProperties;
    private Map<String,String> mClientProperties;
}
