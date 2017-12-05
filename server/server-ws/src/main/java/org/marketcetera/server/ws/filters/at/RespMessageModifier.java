package org.marketcetera.server.ws.filters.at;

import org.marketcetera.core.CoreException;
import org.marketcetera.quickfix.FIXMessageUtil;
import org.marketcetera.ws.server.filters.MessageModifier;
import org.marketcetera.quickfix.messagefactory.FIXMessageAugmentor;
import org.marketcetera.util.misc.ClassVersion;

import quickfix.FieldNotFound;
import quickfix.Message;
import quickfix.field.SecurityType;
import quickfix.field.Symbol;

/**
 * Takes in a collection of message/header/trailer fields to always modify on a
 * passed-in message
 */
@ClassVersion("$Id: RespMessageModifier.java $")
public class RespMessageModifier implements MessageModifier {

	public RespMessageModifier() {
	}

	@Override
	public boolean modifyMessage(Message message, FIXMessageAugmentor augmentor)
			throws CoreException {
		boolean modified = false;

		 if (FIXMessageUtil.isExecutionReport(message)) {
			try {
				String symbol = message.getString(Symbol.FIELD);				
				if(symbol.contains("/")){
					message.setField(new SecurityType(SecurityType.FOREIGN_EXCHANGE_CONTRACT));
				}else{
					message.setField(new SecurityType(SecurityType.COMMON_STOCK));
				}
				
				modified = true;
			} catch (FieldNotFound e) {
				modified = false;
			}
		}
	
		return modified;
	}
}
