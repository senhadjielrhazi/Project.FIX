package org.marketcetera.client.ws.module;

import org.marketcetera.util.misc.ClassVersion;
import org.marketcetera.client.ws.EventListener;
import org.marketcetera.client.ws.Messages;
import org.marketcetera.client.ws.ReportListener;
import org.marketcetera.client.ws.WSClient;
import org.marketcetera.client.ws.WSClientImpl;
import org.marketcetera.ws.ValidationException;
import org.marketcetera.ws.client.ClientInitException;
import org.marketcetera.ws.client.ConnectionException;
import org.marketcetera.util.log.I18NBoundMessage1P;
import org.marketcetera.util.log.I18NBoundMessage2P;
import org.marketcetera.util.log.SLF4JLoggerProxy;
import org.marketcetera.module.*;
import org.marketcetera.trade.*;
import org.marketcetera.event.Event;
import org.marketcetera.marketdata.GenericDataRequest;
import org.marketcetera.marketdata.MarketDataReject;
import org.marketcetera.metrics.ThreadedMetric;
import org.apache.commons.lang.ObjectUtils;

import java.util.Map;
import java.util.Hashtable;

/**
 * The module that sends requests to WS and emits reports and events
 * received from WS.
 * <p>
 * The module only accepts data of following types
 * <ul>
 *      <li>{@link org.marketcetera.trade.OrderSingle}</li>
 *      <li>{@link org.marketcetera.trade.OrderCancel}</li>
 *      <li>{@link org.marketcetera.trade.OrderReplace}</li>
 *      <li>{@link org.marketcetera.trade.FIXOrder}</li> 
 *      <li>{@link org.marketcetera.marketdata.MarketDataRequest}</li>
 *      <li>{@link org.marketcetera.marketdata.MarketDataCancel}</li>
 * </ul>
 * <p>
 * The module will emit all the reports from the server when requested.
 * Following types of reports may be emitted by the module.
 * <ul>
 *      <li>{@link org.marketcetera.trade.ExecutionReport}</li>
 *      <li>{@link org.marketcetera.trade.OrderCancelReject}</li>
 *      <li>{@link org.marketcetera.event.Event}</li>
 *      <li>{@link org.marketcetera.marketdata.MarketDataReject}</li>
 * </ul>
 * <p>
 * Module Features
 * <table>
 * <tr><th>Capabilities</th><td>Data Emitter, Data Receiver</td></tr>
 * <tr><th>DataFlow Request Parameters</th><td>None, Should be null.</td></tr>
 * <tr><th>Stops data flows</th><td>Yes, if the client is not initialized</td></tr>
 * <tr><th>Start Operation</th><td>Does nothing</td></tr>
 * <tr><th>Stop Operation</th><td>Does nothing</td></tr>
 * <tr><th>Management Interface</th><td>{@link ClientModuleMXBean}</td></tr>
 * <tr><th>Factory</th><td>{@link WSClientModuleFactory}</td></tr>
 * </table>
 */
@ClassVersion("$Id: WSClientModule.java 16154 2012-07-14 16:34:05Z colin $") //$NON-NLS-1$
class WSClientModule extends Module implements DataReceiver,
        DataEmitter {

    @Override
    public void receiveData(DataFlowID inFlowID, Object inData)
            throws ReceiveDataException {
        ThreadedMetric.event("client-IN");  //$NON-NLS-1$
        try {
            if(inData instanceof Order) {
                getClient().send((Order)inData);
            } else if(inData instanceof GenericDataRequest) {
                getClient().send((GenericDataRequest)inData);
            } else {
                throw new UnsupportedDataTypeException(new I18NBoundMessage2P(
                        Messages.UNSUPPORTED_DATA_TYPE, inFlowID.getValue(),
                        ObjectUtils.toString(inData)));
            }
        } catch (ConnectionException e) {
            throw new ReceiveDataException(e, new I18NBoundMessage1P(
                    Messages.SEND_REQUEST_FAIL_NO_CONNECT,
                    ObjectUtils.toString(inData)));
        } catch (ValidationException e) {
            throw new ReceiveDataException(e, new I18NBoundMessage2P(
                        Messages.SEND_REQUEST_VALIDATION_FAILED, inFlowID.getValue(),
                        ObjectUtils.toString(inData)));
        } catch (ClientInitException e) {
            throw new StopDataFlowException(e, new I18NBoundMessage1P(
                    Messages.SEND_REQUEST_FAIL_NO_CONNECT,
                    ObjectUtils.toString(inData)));
        }
    }

    @Override
    public void requestData(DataRequest inRequest,
                            final DataEmitterSupport inSupport)
            throws RequestDataException {
        //No request parameters are supported.
        //All reports received are emitted.
        //Verify no request parameters are specified
        if(inRequest.getData() != null) {
            throw new IllegalRequestParameterValue(
                    Messages.REQUEST_PARAMETER_SPECIFIED);
        }
        try {
        	ListenerEmitter listener = new ListenerEmitter(inSupport);
            getClient().addReportListener(listener);
            getClient().addEventListener(listener);
            mRequestTable.put(inSupport.getRequestID(), listener);
        } catch (ClientInitException e) {
            throw new RequestDataException(e,
                    Messages.REQUEST_CLIENT_NOT_INITIALIZED);
        }
    }

    @Override
    public void cancel(DataFlowID inFlowID, RequestID inRequestID) {
    	ListenerEmitter listener = mRequestTable.remove(inRequestID);
        try {
            getClient().removeReportListener(listener);
            getClient().removeEventListener(listener);
        } catch (ClientInitException e) {
            Messages.LOG_CLIENT_NOT_INIT_CANCEL_REQUEST.error(this, e,
                    inRequestID.toString());
        }
    }
    
    /**
     * Creates an instance.
     *
     * @param inURN The instance URN
     * @param inAutoStart if the module should be auto-started.
     */
    protected WSClientModule(ModuleURN inURN, boolean inAutoStart) {
        super(inURN, inAutoStart);
    }

    @Override
    protected void preStart() throws ModuleException {
    }

    @Override
    protected void preStop() throws ModuleException {
    }

    private WSClient getClient() throws ClientInitException {
        return WSClientImpl.getInstance();
    }

    private final Map<RequestID, ListenerEmitter> mRequestTable =
            new Hashtable<>();

    /**
     * Instances of this class receive execution report from WS and
     * emit them into data flows.
     */
    private static class ListenerEmitter implements ReportListener, EventListener {
        /**
         * Creates an instance.
         *
         * @param inSupport the data emitter support instance.
         */
        public ListenerEmitter(DataEmitterSupport inSupport) {
            mSupport = inSupport;
        }

        @Override
        public void receiveExecutionReport(ExecutionReport inReport) {
            SLF4JLoggerProxy.debug(this, "Emitting Report {}",  //$NON-NLS-1$
                    inReport);
            mSupport.send(inReport);
        }

        @Override
        public void receiveCancelReject(OrderCancelReject inReport) {
            SLF4JLoggerProxy.debug(this, "Emitting Cancel Reject {}",  //$NON-NLS-1$ 
                    inReport);
            mSupport.send(inReport);
        }

		@Override
		public void receiveDataEvent(Event inEvent) {
            SLF4JLoggerProxy.debug(this, "Emitting Data Event {}",  //$NON-NLS-1$
            		inEvent);
            mSupport.send(inEvent);
		}

		@Override
		public void receiveDataReject(MarketDataReject inReject) {
            SLF4JLoggerProxy.debug(this, "Emitting Data Reject {}",  //$NON-NLS-1$
            		inReject);
		}
		
        private final DataEmitterSupport mSupport;
    }
}
