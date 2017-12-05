package org.marketcetera.client.ws.module;

import org.marketcetera.util.misc.ClassVersion;
import org.marketcetera.ws.client.ClientInitException;
import org.marketcetera.ws.client.ClientParameters;
import org.marketcetera.ws.client.ConnectionException;
import org.marketcetera.client.ws.Messages;
import org.marketcetera.client.ws.WSClientImpl;
import org.marketcetera.client.ws.module.ClientModuleFactoryMXBean;
import org.marketcetera.module.ModuleFactory;
import org.marketcetera.module.ModuleURN;
import org.marketcetera.module.Module;
import org.marketcetera.module.ModuleCreationException;

/**
 * The provider / factory that creates Client Module instance.
 * <p>
 * The factory assumes that the Client is already initialized if
 * the URL is not set. Otherwise if the URL is set and the Client is
 * not {@link WSClientImpl#isInitialized()  initialized},
 * the factory will initialize the Client before
 * creating the module instance.
 * <p>
 * The factory has the following characteristics.
 * <table>
 * <tr><th>Provider URN:</th><td><code>metc:server:ws</code></td></tr>
 * <tr><th>Cardinality:</th><td>Singleton</td></tr>
 * <tr><th>InstanceURN:</th><td><code>metc:server:ws:client</code></td></tr>
 * <tr><th>Auto-Instantiated:</th><td>No</td></tr>
 * <tr><th>Auto-Started:</th><td>Yes</td></tr>
 * <tr><th>Instantiation Arguments:</th><td>None</td></tr>
 * <tr><th>Management Interface</th><td>{@link ClientModuleFactoryMXBean}</td></tr>
 * <tr><th>Module Type</th><td>{@link WSClientModule}</td></tr>
 * </table>
 */
@ClassVersion("$Id: WSClientModuleFactory.java 16841 2014-02-20 19:59:04Z colin $") //$NON-NLS-1$
public class WSClientModuleFactory extends ModuleFactory
        implements ClientModuleFactoryMXBean {
    @Override
    public Module create(Object...inParameters) throws ModuleCreationException {
        if(getURL() != null && !WSClientImpl.isInitialized()) {
            ClientParameters parameters = new ClientParameters(getUsername(),
                    getPassword() == null
                    ? null
                    : getPassword().toCharArray(),getURL(),
                    getHostname(), getPort(), getIDPrefix());
            try {
            	WSClientImpl.init(parameters);
            } catch (ConnectionException e) {
                throw new ModuleCreationException(e,
                        Messages.CREATE_MODULE_ERROR);
            } catch (ClientInitException e) {
                //This failure cannot happen as we only execute
                //this code when client is not initialized.
                throw new ModuleCreationException(e,
                        Messages.CREATE_MODULE_ERROR);
            }
        }
        return new WSClientModule(INSTANCE_URN, true);
    }

    /**
     * Creates an instance.
     */
    public WSClientModuleFactory() {
        super(PROVIDER_URN, Messages.PROVIDER_DESCRIPTION, false, false);
    }

    @Override
    public String getURL() {
        return mURL;
    }

    @Override
    public void setURL(String inURL) {
        mURL = inURL;
    }

    @Override
    public String getUsername() {
        return mUsername;
    }

    @Override
    public void setUsername(String inUsername) {
        mUsername = inUsername;
    }

    @Override
    public void setPassword(String inPassword) {
        mPassword = inPassword;
    }

    private String getPassword() {
        return mPassword;
    }

    @Override
    public String getHostname() {
        return mHostname;
    }

    @Override
    public void setHostname(String inHostname) {
        mHostname = inHostname;
    }

    @Override
    public int getPort() {
        return mPort;
    }

    @Override
    public void setPort(int inPort) {
        mPort = inPort;
    }

    @Override
    public String getIDPrefix() {
        return mIDPrefix;
    }

    @Override
    public void setIDPrefix(String inIDPrefix) {
        mIDPrefix = inIDPrefix;
    }

    private String mURL;
    private String mUsername;
    private String mPassword;
    private String mHostname;
    private int mPort;
    private String mIDPrefix;
    static final ModuleURN PROVIDER_URN = new ModuleURN("metc:server:ws");  //$NON-NLS-1$
    public static final ModuleURN INSTANCE_URN = new ModuleURN(PROVIDER_URN, "client");  //$NON-NLS-1$
}
