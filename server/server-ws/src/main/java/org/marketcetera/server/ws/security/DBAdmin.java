package org.marketcetera.server.ws.security;

import org.marketcetera.util.misc.ClassVersion;
import org.marketcetera.core.IDFactory;
import org.marketcetera.core.NoMoreIDsException;
import org.marketcetera.server.ws.ServerManager;
import org.marketcetera.server.ws.brokers.Broker;
import org.marketcetera.server.ws.brokers.Brokers;
import org.marketcetera.trade.BrokerID;
import org.marketcetera.util.log.SLF4JLoggerProxy;

import quickfix.Message;
import quickfix.SessionID;
import quickfix.SessionNotFound;
import quickfix.field.*;

/**
 * Implements the {@link DBAdminMBean} interface
 */
@ClassVersion("$Id: DBAdmin.java 16154 2012-07-14 16:34:05Z colin $")
public class DBAdmin implements DBAdminMBean {
    private Brokers brokers;
    private IDFactory idFactory;
    private ServerManager serverManager;

    public DBAdmin(Brokers brokers,
                    IDFactory idFactory,
                    ServerManager serverManager)
            throws NoMoreIDsException, ClassNotFoundException {
        this.brokers = brokers;
        this.idFactory = idFactory;
        this.serverManager = serverManager;
    }

    @Override
    public void resetTradePassword(String broker, String oldPassword, String newPassword) {
        Broker b=brokers.getTradeBroker(new BrokerID(broker));
        SLF4JLoggerProxy.debug(this, "Trade session halted, resetting password"); //$NON-NLS-1$
        sendRessetPassword(b, oldPassword, newPassword);
       
    }

	@Override
    public void resetDataPassword(String broker, String oldPassword, String newPassword) {
        Broker b=brokers.getDataBroker(new BrokerID(broker));
        SLF4JLoggerProxy.debug(this, "Data session halted, resetting password"); //$NON-NLS-1$
        sendRessetPassword(b, oldPassword, newPassword);
    }
    
    private void sendRessetPassword(Broker b, String oldPassword, String newPassword) {
    	 SessionID session = b.getSessionID();
         Message msg = b.getFIXMessageFactory().createMessage(MsgType.USER_REQUEST);
         // in case of Currenex that uses FIX.4.2 right message won't be created to set the type manually
         if (!msg.getHeader().isSetField(MsgType.FIELD)) {
             msg.getHeader().setField(new MsgType(MsgType.USER_REQUEST));
         }
         msg.setField(new UserRequestID(getNextID()));
         msg.setField(new UserRequestType(UserRequestType.CHANGEPASSWORDFORUSER));
         msg.setField(new Username(session.getSenderCompID()));
         msg.setField(new Password(oldPassword));
         msg.setField(new NewPassword(newPassword));

         try {
             b.sendToTarget(msg);
         } catch (SessionNotFound sessionNotFound) {
             sessionNotFound.printStackTrace();
         }
	}
    
    @Override
    public void syncSessions()
    {
    	serverManager.sync();
    }

    private String getNextID() {
        try {
            return idFactory.getNext();
        } catch(NoMoreIDsException ex) {
            Messages.ERROR_GENERATING_EXEC_ID.error(this, ex.getMessage());
            return "ZZ-INTERNAL"; //$NON-NLS-1$
        }
    }
}
