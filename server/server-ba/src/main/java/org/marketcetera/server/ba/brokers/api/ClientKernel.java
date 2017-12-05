package org.marketcetera.server.ba.brokers.api;

import org.springframework.beans.factory.InitializingBean;

import quickfix.Message;
import quickfix.SessionID;

public interface ClientKernel extends InitializingBean
{
	/**
	 * Receive a message to the broker's session.
	 *  
     * @param msg The message.
     * @param sessionID the receiver of the response.
	 */
	public void receiveMessage(Message msg, SessionID sessionID);
	
	/**
	 * Indicates if the Client is running
	 * 
	 * @return a <code>Boolean</code> value.
	 */
	public boolean isRunning();
}
