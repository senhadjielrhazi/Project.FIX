package org.marketcetera.client.sa;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.marketcetera.client.sa.jms.SAContextClassProvider;
import org.marketcetera.client.sa.jms.StrategyParameters;
import org.marketcetera.ws.client.ConnectionException;
import org.marketcetera.module.ModuleInfo;
import org.marketcetera.module.ModuleURN;
import org.marketcetera.util.log.I18NBoundMessage3P;
import org.marketcetera.util.misc.ClassVersion;
import org.marketcetera.util.ws.stateful.Client;
import org.marketcetera.util.ws.stateful.ClientContext;
import org.marketcetera.util.ws.wrappers.MapWrapper;
import org.marketcetera.ws.client.ClientParameters;
import org.marketcetera.ws.client.ClientVersion;


/**
 * The client implementation that implements the details of communicating
 * with the remote strategy agent.
 */
@ClassVersion("$Id: SAClientImpl.java 16879 2014-04-15 21:40:25Z colin $")
class SAClientImpl
        extends AbstractSAClient
{
    // INSTANCE DATA.
    /**
     * connection to web services service
     */
    private Client mServiceClient;
    /**
     * provides services
     */
    private SAService mSAService;
    
    // CONSTRUCTORS.
    /**
     * Creates an instance. Once created, the client is connected to the
     * remote strategy agent.
     *
     * @param inParameters the connection details. Cannot be null.
     * @throws ConnectionException if there were errors connecting to the remote strategy agent. 
     */
    SAClientImpl(ClientParameters inParameters)
            throws ConnectionException
    {
        super(inParameters);
    }
    
    // INSTANCE METHODS. 
    /* (non-Javadoc)
     * @see org.marketcetera.client.sa.SAClient#getProviders()
     */
    @Override
    public List<ModuleURN> getProviders()
            throws ConnectionException
    {
        failIfDisconnected();
        try {
            List<ModuleURN> list = mSAService.getProviders(getServiceContext());
            //translate nulls to empty lists for more usable API.
            return list != null? list: new ArrayList<ModuleURN>();
        } catch (Exception e) {
            throw wrapRemoteFailure(e);
        }
    }
    /* (non-Javadoc)
     * @see org.marketcetera.client.sa.SAClient#getInstances(org.marketcetera.module.ModuleURN)
     */
    @Override
    public List<ModuleURN> getInstances(ModuleURN inProviderURN)
            throws ConnectionException
    {
        failIfDisconnected();
        try {
            List<ModuleURN> list = mSAService.getInstances(getServiceContext(),
                                                           inProviderURN);
            //translate nulls to empty lists for more usable API.
            return list != null? list: new ArrayList<ModuleURN>();
        } catch (Exception e) {
            throw wrapRemoteFailure(e);
        }
    }
    /* (non-Javadoc)
     * @see org.marketcetera.client.sa.SAClient#getModuleInfo(org.marketcetera.module.ModuleURN)
     */
    @Override
    public ModuleInfo getModuleInfo(ModuleURN inURN)
            throws ConnectionException
    {
        failIfDisconnected();
        try {
            return mSAService.getModuleInfo(getServiceContext(),
                                            inURN);
        } catch (Exception e) {
            throw wrapRemoteFailure(e);
        }
    }
    /* (non-Javadoc)
     * @see org.marketcetera.client.sa.SAClient#start(org.marketcetera.module.ModuleURN)
     */
    @Override
    public void start(ModuleURN inURN)
            throws ConnectionException
    {
        failIfDisconnected();
        try {
            mSAService.start(getServiceContext(),
                             inURN);
        } catch (Exception e) {
            throw wrapRemoteFailure(e);
        }
    }
    /* (non-Javadoc)
     * @see org.marketcetera.client.sa.SAClient#stop(org.marketcetera.module.ModuleURN)
     */
    @Override
    public void stop(ModuleURN inURN)
            throws ConnectionException
    {
        failIfDisconnected();
        try {
            mSAService.stop(getServiceContext(),
                            inURN);
        } catch (Exception e) {
            throw wrapRemoteFailure(e);
        }
    }
    
    /* (non-Javadoc)
     * @see org.marketcetera.client.sa.SAClient#delete(org.marketcetera.module.ModuleURN)
     */
    @Override
    public void delete(ModuleURN inURN)
            throws ConnectionException
    {
        failIfDisconnected();
        try {
            mSAService.delete(getServiceContext(),
                              inURN);
        } catch (Exception e) {
            throw wrapRemoteFailure(e);
        }
    }
    
    /* (non-Javadoc)
     * @see org.marketcetera.client.sa.SAClient#getProperties(org.marketcetera.module.ModuleURN)
     */
    @Override
    public Map<String,Object> getProperties(ModuleURN inURN)
            throws ConnectionException
    {
        failIfDisconnected();
        try {
            MapWrapper<String,Object> value = mSAService.getProperties(getServiceContext(),
                                                                       inURN);
            return value == null? null: value.getMap();
        } catch (Exception e) {
            throw wrapRemoteFailure(e);
        }
    }
    
    /* (non-Javadoc)
     * @see org.marketcetera.client.sa.SAClient#setProperties(org.marketcetera.module.ModuleURN, java.util.Map)
     */
    @Override
    public Map<String,Object> setProperties(ModuleURN inURN,
                                            Map<String,Object> inProperties)
            throws ConnectionException
    {
        failIfDisconnected();
        try {
            MapWrapper<String,Object> map = mSAService.setProperties(getServiceContext(),
                                                                     inURN,
                                                                     new MapWrapper<String,Object>(inProperties));
            return map == null ? null : map.getMap();
        } catch (Exception e) {
            throw wrapRemoteFailure(e);
        }
    }
    
    /* (non-Javadoc)
     * @see org.marketcetera.client.sa.SAClient#createStrategy(org.marketcetera.client.sa.StrategyParameters)
     */
    @Override
    public ModuleURN createStrategy(StrategyParameters inParameters)
            throws ConnectionException
    {
        failIfDisconnected();
        try {
            return mSAService.createStrategy(getServiceContext(),
                                             inParameters);
        } catch (Exception e) {
            throw wrapRemoteFailure(e);
        }
    }
    
    /* (non-Javadoc)
     * @see org.marketcetera.client.sa.SAClient#sendData(java.lang.Object)
     */
    @Override
    public void sendData(Object inData)
            throws ConnectionException
    {
        failIfDisconnected();
        try {
            mSAService.sendData(getServiceContext(),
                                inData);
        } catch (Exception e) {
            throw wrapRemoteFailure(e);
        }
    }

    /* (non-Javadoc)
     * @see org.marketcetera.client.sa.AbstractSAClient#doStop()
     */
    @Override
    protected void doStop()
    {
        try {
            if(mServiceClient != null) {
                mServiceClient.logout();
            }
        } catch (Exception ignored) {
        } finally {
            mServiceClient = null;
        }
    }
    /* (non-Javadoc)
     * @see org.marketcetera.client.sa.AbstractSAClient#doStart()
     */
    @Override
    protected void doStart()
    {
        try {
            mServiceClient = new Client(parameters.getHostname(),
                                        parameters.getPort(),
                                        ClientVersion.APP_ID,
                                        SAContextClassProvider.INSTANCE);
            mServiceClient.login(parameters.getUsername(),
                                 parameters.getPassword());
            mSAService = mServiceClient.getService(SAService.class);
        } catch (Exception e) {
            throw new ConnectionException(e,
                                          new I18NBoundMessage3P(Messages.ERROR_WS_CONNECT,
                                                                 parameters.getHostname(),
                                                                 String.valueOf(parameters.getPort()),
                                                                 parameters.getUsername()));
        }
    }
    
    /**
     * Gets the client context to use when making WS calls.
     *
     * @return the client context.
     */
    private ClientContext getServiceContext()
    {
        return mServiceClient.getContext();
    }
}
