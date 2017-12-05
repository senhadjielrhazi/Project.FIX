package org.marketcetera.client.ws;

import java.beans.ExceptionListener;
import java.util.Arrays;
import java.util.Deque;

import javax.jms.JMSException;
import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.pool.PooledConnectionFactory;
import org.apache.commons.lang.ObjectUtils;
import org.marketcetera.client.ws.jms.RequestEnvelope;
import org.marketcetera.client.ws.jms.WSContextClassProvider;
import org.marketcetera.core.IDFactory;
import org.marketcetera.core.NoMoreIDsException;
import org.marketcetera.metrics.ThreadedMetric;
import org.marketcetera.trade.Factory;
import org.marketcetera.util.except.ExceptUtils;
import org.marketcetera.util.log.I18NBoundMessage1P;
import org.marketcetera.util.log.I18NBoundMessage2P;
import org.marketcetera.util.log.I18NBoundMessage4P;
import org.marketcetera.util.log.SLF4JLoggerProxy;
import org.marketcetera.util.misc.ClassVersion;
import org.marketcetera.util.ws.stateful.Client;
import org.marketcetera.util.ws.stateful.ClientContext;
import org.marketcetera.util.ws.tags.SessionId;
import org.marketcetera.util.ws.wrappers.RemoteException;
import org.marketcetera.util.ws.wrappers.RemoteProxyException;
import org.marketcetera.client.ws.jms.JmsManager;
import org.marketcetera.ws.RequestMessage;
import org.marketcetera.ws.client.ClientInitException;
import org.marketcetera.ws.client.ClientParameters;
import org.marketcetera.ws.client.ClientVersion;
import org.marketcetera.ws.client.ConnectionException;
import org.marketcetera.ws.client.IncompatibilityException;
import org.springframework.jms.core.JmsOperations;
import org.springframework.jms.listener.SimpleMessageListenerContainer;

import com.google.common.collect.Lists;

/**
 * The implementation of WSClient that connects to the server.
 */
@XmlRootElement(name="serviceProvider")
@XmlAccessorType(XmlAccessType.FIELD)
@ClassVersion("$Id: WSClient.java 16997 2015-05-07 19:51:53Z colin $")
public class WSClientImpl extends AbstractWSClient
		implements WSClient, javax.jms.ExceptionListener {
    
    /**
     * Creates an instance given the parameters and connects to the server.
     *
     * @param inParameters the parameters to connect to the server, cannot
     * be null.
     *
     * @throws ConnectionException if there were errors connecting
     * to the server.
     */
    public WSClientImpl(ClientParameters inParameters)
            throws ConnectionException
    {
        setParameters(inParameters);
        start();
    }
    
    /**
     * Initializes the connection to the server. The handle to communicate
     * with the server can be obtained via {@link #getInstance()}.
     *
     * @param inParameters The parameters to connect the client. Cannot be null.
     *
     * @throws ConnectionException if there were errors connecting
     * to the server.
     * @throws ClientInitException if the client is already initialized.
     */
    public static synchronized void init(ClientParameters inParameters)
            throws ConnectionException, ClientInitException
    {
        if(!isInitialized()) {
        	mInstance = new WSClientImpl(inParameters);
        } else {
            throw new ClientInitException(Messages.CLIENT_ALREADY_INITIALIZED);
        }
    }
    
    /**
     * Returns the Client instance after it has been initialized via
     * {@link #init(ClientParameters)}
     *
     * @return the client instance to communicate with the server.
     *
     * @throws ClientInitException if the client is not initialized.
     */
    public static WSClient getInstance() throws ClientInitException {
        if (isInitialized()) {
            return mInstance;
        } else {
            throw new ClientInitException(Messages.CLIENT_NOT_INITIALIZED);
        }
    }

    /**
     * Returns true if the client is initialized, false if it's not.
     *
     * @return if the client is initialized.
     */
    public static boolean isInitialized() {
        return mInstance != null;
    }

    /* (non-Javadoc)
     * @see org.marketcetera.client.ws.WSClient#addExceptionListener(java.beans.ExceptionListener)
     */
	@Override
	public final void addExceptionListener(ExceptionListener inListener) {
		failIfDisconnected();
        if(inListener == null) {
            throw new NullPointerException();
        }
        synchronized (mExceptionListeners) {
            mExceptionListeners.addFirst(inListener);
        }
	}
	
    /* (non-Javadoc)
     * @see org.marketcetera.client.ws.WSClient#removeExceptionListener(java.beans.ExceptionListener)
     */
	@Override
	public final void removeExceptionListener(ExceptionListener inListener) {
		failIfDisconnected();
        if(inListener == null) {
            throw new NullPointerException();
        }
        synchronized (mExceptionListeners) {
            mExceptionListeners.removeFirstOccurrence(inListener);
        }
	}
	
	/**
	 * Throws an exception 
	 */
	private void exceptionThrown(ConnectionException inException) {
        synchronized (mExceptionListeners) {
            for(ExceptionListener l: mExceptionListeners) {
                try {
                    l.exceptionThrown(inException);
                } catch (Exception e) {
                    Messages.LOG_ERROR_NOTIFY_EXCEPTION.warn(this, e,
                            ObjectUtils.toString(inException));
                    ExceptUtils.interrupt(e);
                }
            }
        }
    }

    /**
     * Sets the client parameters value.
     *
     * @param inParameters the client parameters, cannot be null.
     */
    private void setParameters(ClientParameters inParameters) {
        if(inParameters == null) {
            throw new NullPointerException();
        }
        mParameters = inParameters;
    }

    @Override
    public synchronized void reconnect(ClientParameters inParameters)
            throws ConnectionException {
        internalClose();
        if(inParameters != null) {
            setParameters(inParameters);
        }
        start();
    }

    @Override
    public boolean isCredentialsMatch(String inUsername, char[] inPassword) {
        return (!mClosed) &&
                ObjectUtils.equals(mParameters.getUsername(), inUsername) &&
                Arrays.equals(mParameters.getPassword(), inPassword);
    }
    
    /* (non-Javadoc)
     * @see org.marketcetera.client.ws.WSClient#close()
     */
    @Override
    public synchronized final void close()
    {
        internalClose();
        mClosed = true;
    }
    
    @Override
    public boolean isRunning()
    {
        return ((!mClosed) && mRunning.get());
    }

    // javax.jms.ExceptionListener.
    @Override
    public void onException(JMSException e) {
        exceptionThrown(new ConnectionException
                        (e,Messages.ERROR_RECEIVING_JMS_MESSAGE));
    }
	
    @Override
    protected void convertAndSend(RequestMessage inRequest) throws ConnectionException {
        ThreadedMetric.event("client-OUT",  inRequest);
        failIfDisconnected();
        SLF4JLoggerProxy.debug(TRAFFIC, "Sending request:{}", inRequest);  //$NON-NLS-1$
        try {
            if (mToServer == null) {
                throw new ClientInitException(Messages.NOT_CONNECTED_TO_SERVER);
            }
            mToServer.convertAndSend(new RequestEnvelope(inRequest,
                                                       getSessionId()));
        } catch (Exception e) {
            ConnectionException exception;
            exception = new ConnectionException(e, new I18NBoundMessage1P(
                    Messages.ERROR_SEND_MESSAGE, ObjectUtils.toString(inRequest)));
            Messages.LOG_ERROR_SEND_EXCEPTION.warn(this, exception,
                    ObjectUtils.toString(inRequest));
            ExceptUtils.interrupt(e);
            exceptionThrown(exception);
            throw exception;
        }
    }

    @Override
    protected ClientContext getClientContext()
    {
        return mServiceClient.getContext();
    }
    
    @Override
	protected WSService getWebService() {
		return mService;
	}
    
    /**
     * Client's ID factory that generates unique IDs based on IDs given out by the
     * server. The generate IDs have the supplied prefix, if one is supplied,
     * followed by the ID provided by the server, followed by a client
     * generated number between 000-999. A request is made to the server to
     * request the next ID value, when the client ID value reaches 999.
     */
    private class ClientIDFactory implements IDFactory {
        /**
         * Creates an instance.
         *
         * @param inPrefix the prefix to use for all orderIDs.
         * @param inClientImpl the client impl to use to obtain orderIDs from
         * the server.
         */
    	protected ClientIDFactory(String inPrefix) {
            mPrefix = inPrefix == null
                    ? ""   //$NON-NLS-1$
                    : inPrefix;
        }

        @Override
        public synchronized String getNext() throws NoMoreIDsException {
            mClientID++;
            if(mClientID > MAX_CLIENT_ID) {
                getNextServer();
            }
            return String.format("%1$s%2$s%3$03d",  //$NON-NLS-1$
                    mPrefix, mServerID, mClientID);
        }

        @Override
        public void init() throws NoMoreIDsException {
            getNextServer();
        }

        /**
         * Fetches the next orderID base from the server and initializes, the
         * client portion of the ID back to zero.
         *
         * @throws NoMoreIDsException if the ID couldn't be fetched from the server.
         */
        private void getNextServer() throws NoMoreIDsException {
            try {
                mServerID = getNextServerID();
                mClientID = 0;
            } catch (ConnectionException e) {
                Messages.LOG_UNABLE_FETCH_ID_SERVER.error(this,e);
                throw new NoMoreIDsException(e, Messages.UNABLE_FETCH_ID_SERVER);
            }
        }
        
        private String mServerID;
        private short mClientID = 0;
        private final String mPrefix;
        static final short MAX_CLIENT_ID = 999;
    }
    
    /**
     * The 'heart' that produces heartbeats, keeping the connection to
     * the server alive.
     */
    private class Heart
        extends Thread
    {
        private volatile boolean mMarked;

        Heart()
        {
            super(Thread.currentThread().getThreadGroup(),
                  Messages.HEARTBEAT_THREAD_NAME.getText());
            setDaemon(true);
        }

        void markExit()
        {
            mMarked=true;
        }

        private boolean isMarked()
        {
            return mMarked;
        }

        @Override
        public void run()
        {
            while(true) {
                try {
                    Thread.sleep(mParameters.getHeartbeatInterval());
                } catch (InterruptedException ex) {
                    SLF4JLoggerProxy.debug(HEARTBEATS,
                                           "Stopped (interrupted)"); //$NON-NLS-1$
                    markExit();
                    setServerAlive(false);
                    return;
                }
                if(isMarked()) {
                    SLF4JLoggerProxy.debug(HEARTBEATS,
                                           "Stopped (marked)"); //$NON-NLS-1$
                    setServerAlive(false);
                    return;
                }
                try {
                    heartbeat();
                    setServerAlive(true);
                } catch (Exception ex) {
                    setServerAlive(false);
                    if(ExceptUtils.isInterruptException(ex)) {
                        SLF4JLoggerProxy.debug(HEARTBEATS,
                                               "Stopped (interrupted)"); //$NON-NLS-1$
                        markExit();
                        return;
                    }
                    SLF4JLoggerProxy.debug(HEARTBEATS,
                                           ex,
                                           "Failed"); //$NON-NLS-1$
                    exceptionThrown(new ConnectionException(ex,
                                                            Messages.ERROR_HEARTBEAT_FAILED));
                    if(ex instanceof RemoteException) {
                        // We connected to the server, but the session may have expired: attempt to auto-reconnect
                        // after a short delay to let the server settle (if it has just restarted). The
                        // delay is random so that not all clients will try and contact the ORS at the same time.
                        long delay = (long)(RECONNECT_WAIT_INTERVAL*(0.75+1.25*Math.random()));
                        SLF4JLoggerProxy.debug(HEARTBEATS,
                                               "Reconnecting in {} ms", //$NON-NLS-1$
                                               delay);
                        try {
                            Thread.sleep(delay);
                        } catch (InterruptedException ex2) {
                            SLF4JLoggerProxy.debug(HEARTBEATS,
                                                   "Stopped (interrupted)"); //$NON-NLS-1$
                            markExit();
                            return;
                        }
                        try {
                            reconnectWebServices();
                            setServerAlive(true);
                            SLF4JLoggerProxy.debug(HEARTBEATS,
                                                   "...reconnect succeeded."); //$NON-NLS-1$
                        } catch (Exception ex2) {
                            setServerAlive(false);
                            if(ExceptUtils.isInterruptException(ex2)) {
                                SLF4JLoggerProxy.debug(HEARTBEATS,
                                                       "Stopped (interrupted)"); //$NON-NLS-1$
                                markExit();
                                return;
                            }
                            SLF4JLoggerProxy.debug(HEARTBEATS,
                                                   ex2,
                                                   "...reconnect failed."); //$NON-NLS-1$
                            exceptionThrown(new ConnectionException(ex2,
                                                                    Messages.ERROR_HEARTBEAT_FAILED));
                        }
                    }
                }
                if(isMarked()) {
                    SLF4JLoggerProxy.debug(HEARTBEATS,
                                           "Stopped (marked)"); //$NON-NLS-1$
                    setServerAlive(false);
                    return;
                }
            }
        }
    }
    
    /**
     * Sets the server connection status. If the status changed, the
     * registered callbacks are invoked.
     *
     * @param serverAlive True means the server connection is alive.
     */
    private void setServerAlive(boolean serverAlive)
    {
        if (mRunning.get()==serverAlive) {
            return;
        }
        if (serverAlive) {
            try {
                startJms();
            } catch (JAXBException ex) {
                exceptionThrown(new ConnectionException
                                (ex,Messages.ERROR_CREATING_JMS_CONNECTION));
                return;
            }
        } else {
            stopJms();
        }
        mRunning.set(serverAlive);
        if(serverAlive){
        	try {
        		reSendData();
        	} catch (Exception ignored) {}
        }
        notifyServerStatus(isRunning());
    }
    
    private void internalClose() {
        // Close the heartbeat generator first so that it won't
        // re-create a JMS connection during subsequent shutdown. In
        // fact, the generator will normally shut down the JMS
        // connection before it terminates.
        if (mHeart!=null) {
            mHeart.markExit();
            mHeart.interrupt();
            try {
                mHeart.join();
            } catch (InterruptedException ex) {
                SLF4JLoggerProxy.debug
                    (this,"Error when joining with heartbeat thread",ex); //$NON-NLS-1$
                ExceptUtils.interrupt(ex);
            }
        }
        setServerAlive(false);
        try {
            if(mServiceClient != null) {
                mServiceClient.logout();
            }
            mServiceClient = null;
        } catch (Exception ex) {
            SLF4JLoggerProxy.debug
                (this,"Error when closing web service client",ex); //$NON-NLS-1$
            ExceptUtils.interrupt(ex);
        }
        
        mToServer = null;
        if(mServerMessageListener != null && mServerMessageListener.isRunning()) {
            try {
            	mServerMessageListener.stop();
            	mServerMessageListener = null;
            } catch (Exception ignored) {}
        }
    }

    /**
     * Reconnects the web service.
     *
     * @throws RemoteException if an error occurs reconnecting
     */
    private void reconnectWebServices()
            throws RemoteException
    {
        mServiceClient.logout();
        mServiceClient.login(mParameters.getUsername(),
                             mParameters.getPassword());
    }
    
    /**
     * Executes a heartbeat.
     *
     * @throws RemoteException if the heartbeat cannot be executed
     */
    private void heartbeat()
            throws RemoteException
    {
    	getWebService().heartbeat(getClientContext());
    }
    /**
     * Gets the session ID value.
     *
     * @return a <code>SessionId</code> value
     */
    private SessionId getSessionId()
    {
        return getClientContext().getSessionId();
    }
    /**
     * Starts the JMS connection.
     *
     * @throws JAXBException if an error occurs starting the JMS connection
     */
    private void startJms()
            throws JAXBException
    {
        if(mToServer != null) {
            return;
        }
        if(mServerMessageListener != null && mServerMessageListener.isRunning()) {
            try {
            	mServerMessageListener.stop();
            	mServerMessageListener = null;
            } catch (Exception ignored) {}
        }
        mServerMessageListener = mJmsMgr.getIncomingJmsFactory().registerResponseHandler(new ServerMessageReceiver(),
        		JmsManager.getReplyTopicName(getSessionId()),true);
        mServerMessageListener.start();
        mToServer = mJmsMgr.getOutgoingJmsFactory().createJmsTemplate(JmsManager.getRequestTopicName(),false);
    }
    
    /**
     * Connects the client to the server.
     *
     * @throws ConnectionException if an error occurs connecting to the server
     */
    private synchronized final void start()
            throws ConnectionException
    {
        if(mParameters.getURL() == null || mParameters.getURL().trim().isEmpty()) {
            throw new ConnectionException(Messages.CONNECT_ERROR_NO_URL);
        }
        if(mParameters.getUsername() == null || mParameters.getUsername().trim().isEmpty()) {
            throw new ConnectionException(Messages.CONNECT_ERROR_NO_USERNAME);
        }
        if(mParameters.getHostname() == null || mParameters.getHostname().trim().isEmpty()) {
            throw new ConnectionException(Messages.CONNECT_ERROR_NO_HOSTNAME);
        }
        if(mParameters.getPort() < 1 || mParameters.getPort() > 0xFFFF) {
            throw new ConnectionException(new I18NBoundMessage1P(Messages.CONNECT_ERROR_INVALID_PORT,
                                                                 mParameters.getPort()));
        }
        try {       
        	mClosed = false;
        	mServiceClient = new Client(mParameters.getHostname(),
                    mParameters.getPort(), ClientVersion.APP_ID,
                    WSContextClassProvider.INSTANCE);
    		mServiceClient.login(mParameters.getUsername(),
    		mParameters.getPassword());
    		mService = mServiceClient.getService(WSService.class);

            ActiveMQConnectionFactory mIncomingCF = (new ActiveMQConnectionFactory(mParameters.getUsername(),
            		String.valueOf(mParameters.getPassword()), mParameters.getURL()));
            PooledConnectionFactory mOutgoingCF = new PooledConnectionFactory(mIncomingCF);
            mJmsMgr = new JmsManager(mIncomingCF, mOutgoingCF, WSContextClassProvider.INSTANCE, this);
            startJms();
            
            setServerAlive(true);
            notifyServerStatus(true);
            mHeart = new Heart();
            mHeart.start();
            ClientIDFactory idFactory = new ClientIDFactory(mParameters.getIDPrefix());
            idFactory.init();
            Factory.getInstance().setIDFactory(idFactory);
        } catch(Exception e) {
            internalClose();
            ExceptUtils.interrupt(e);
            if(e.getCause() instanceof RemoteProxyException) {
                RemoteProxyException ex = (RemoteProxyException)e.getCause();
                if(IncompatibilityException.class.getName().equals(ex.getServerName())) {
                    throw new ConnectionException(e,
                                                  new I18NBoundMessage1P(Messages.ERROR_CONNECT_INCOMPATIBLE_DEDUCED,
                                                                         ex.getMessage()));
                }
            } else if(e.getCause() instanceof IncompatibilityException) {
                IncompatibilityException ex = (IncompatibilityException)e.getCause();
                throw new ConnectionException(e,
                                              new I18NBoundMessage2P(Messages.ERROR_CONNECT_INCOMPATIBLE_DIRECT,
                                                                     ClientVersion.APP_ID,
                                                                     ex.getServerVersion()));
            }
            throw new ConnectionException(e,
                                          new I18NBoundMessage4P(Messages.ERROR_CONNECT_TO_SERVER,
                                                                 mParameters.getURL(),
                                                                 mParameters.getUsername(),
                                                                 mParameters.getHostname(),
                                                                 mParameters.getPort()));
        }
    }
	
    /**
     * Checks to see if the client is closed and fails if the client
     * is closed.
     *
     * @throws IllegalStateException if the client is closed.
     */
    @Override
    protected void failIfClosed() throws IllegalStateException {
        if(mClosed) {
            throw new IllegalStateException(Messages.CLIENT_CLOSED.getText());
        }
    }

    private void stopJms()
    {
        if (mToServer==null) {
            return;
        }
        try {
            if (mServerMessageListener!=null) {
            	mServerMessageListener.shutdown();
            }
        } catch (Exception ex) {
            SLF4JLoggerProxy.debug
                (this,"Error when closing server message listener",ex); //$NON-NLS-1$
            ExceptUtils.interrupt(ex);
        } finally {
        	mToServer = null;
        }
    }
    
    /**
     * the <code>Client</code> object
     */
    private volatile static WSClient mInstance;
   
    private volatile JmsManager mJmsMgr;
    private volatile SimpleMessageListenerContainer mServerMessageListener;
    private volatile JmsOperations mToServer;
    protected volatile ClientParameters mParameters;
    private volatile boolean mClosed = false;

    private final Deque<ExceptionListener> mExceptionListeners = Lists.newLinkedList();
    
    private static final long RECONNECT_WAIT_INTERVAL = 10000;
    
    private volatile Client mServiceClient;
    private WSService mService;
    private Heart mHeart;

    private static final String HEARTBEATS = WSClientImpl.class.getPackage().
            getName() + ".heartbeats";  //$NON-NLS-1$
}
