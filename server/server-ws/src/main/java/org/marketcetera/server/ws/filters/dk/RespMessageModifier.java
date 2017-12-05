package org.marketcetera.server.ws.filters.dk;

import org.marketcetera.core.CoreException;
import org.marketcetera.quickfix.FIXMessageUtil;
import org.marketcetera.ws.server.filters.MessageModifier;
import org.marketcetera.quickfix.messagefactory.FIXMessageAugmentor;
import org.marketcetera.server.ws.filters.SymbolConverter;
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
	
	private SymbolConverter symbolConverter;

	public RespMessageModifier() {
	}

	@Override
	public boolean modifyMessage(Message message, FIXMessageAugmentor augmentor)
			throws CoreException {
		boolean modified = false;

		if (FIXMessageUtil.isMarketDataSnapshotFullRefresh(message)
				|| FIXMessageUtil.isMarketDataIncrementalRefresh(message)) {
			try {
				String symbol = symbolConverter.getSymbol(message.getString(Symbol.FIELD));
				message.setString(Symbol.FIELD,symbol);
				
				modified = true;
			} catch (FieldNotFound e) {}
		} else if (FIXMessageUtil.isExecutionReport(message)) {
			try {
				String symbol = symbolConverter.getSymbol(message.getString(Symbol.FIELD));
				message.setString(Symbol.FIELD,symbol);
				
				if(symbol.contains("/")){
					message.setField(new SecurityType(SecurityType.FOREIGN_EXCHANGE_CONTRACT));
				}else{
					message.setField(new SecurityType(SecurityType.COMMON_STOCK));
				}
				modified = true;
			} catch (FieldNotFound e) {}
		}

		return modified;
	}

	public SymbolConverter getSymbolConverter() {
		return symbolConverter;
	}

	public void setSymbolConverter(SymbolConverter inSymbolConverter) {
		this.symbolConverter = inSymbolConverter;
	}
}
