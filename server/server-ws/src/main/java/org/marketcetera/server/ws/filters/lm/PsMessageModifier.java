package org.marketcetera.server.ws.filters.lm;

import java.util.List;

import org.marketcetera.core.CoreException;
import org.marketcetera.quickfix.FIXMessageUtil;
import org.marketcetera.ws.server.filters.MessageModifier;

import com.google.common.collect.Lists;

import org.marketcetera.quickfix.messagefactory.FIXMessageAugmentor;
import org.marketcetera.server.ws.filters.SymbolConverter;
import org.marketcetera.trade.FIXUtil;
import org.marketcetera.util.misc.ClassVersion;
import quickfix.FieldNotFound;
import quickfix.Group;
import quickfix.Message;
import quickfix.field.Currency;
import quickfix.field.DiscretionOffsetValue;
import quickfix.field.HandlInst;
import quickfix.field.MDEntryType;
import quickfix.field.NoMDEntryTypes;
import quickfix.field.NoRelatedSym;
import quickfix.field.OrderID;
import quickfix.field.Product;
import quickfix.field.SecondaryExecID;
import quickfix.field.SecurityID;
import quickfix.field.SecurityIDSource;
import quickfix.field.SecurityType;
import quickfix.field.SubscriptionRequestType;
import quickfix.field.Symbol;

/**
 * Takes in a collection of message/header/trailer fields to always modify on a
 * passed-in message
 */
@ClassVersion("$Id: PsMessageModifier.java $")
public class PsMessageModifier implements MessageModifier {
	
	private SymbolConverter symbolConverter;

	public PsMessageModifier() {
	}

	@Override
	public boolean modifyMessage(Message message, FIXMessageAugmentor augmentor)
			throws CoreException {
		boolean modified = false;

		if (FIXMessageUtil.isMarketDataRequest(message)) {
			try {
				SubscriptionRequestType type = FIXUtil.getSubscriptionType(message);
				// Cancel market data
				if (type.getValue() == SubscriptionRequestType.DISABLE_PREVIOUS_SNAPSHOT_PLUS_UPDATE_REQUEST) {
					
					List<Group> mdEntryGroup = Lists.newArrayList(message.getGroups(NoMDEntryTypes.FIELD));
					message.removeGroup(NoMDEntryTypes.FIELD);
					for (Group group:mdEntryGroup) {
						group.setField(new MDEntryType(MDEntryType.BID));
						message.addGroup(group);
						
			 	       	group.setField(new MDEntryType(MDEntryType.OFFER));
			 	       	message.addGroup(group);
					}
					
					List<Group> symbolGroup = message.getGroups(NoRelatedSym.FIELD);
					for (Group group:symbolGroup) {
						group.setString(SecurityIDSource.FIELD, "8");
						group.setString(SecurityID.FIELD, "4001");// Doesn't matter as it 
																  // is a cancel request
						group.removeField(Symbol.FIELD);
					}	
					
					modified = true;
				} else {
					// Request market data
					List<Group> mdEntryGroup = Lists.newArrayList(message.getGroups(NoMDEntryTypes.FIELD));
					message.removeGroup(NoMDEntryTypes.FIELD);
					for (Group group:mdEntryGroup) {
						if(group.isSetField(MDEntryType.FIELD)){
							char entryType = group.getChar(MDEntryType.FIELD);
							if(entryType != MDEntryType.TRADE){
								message.addGroup(group);
							}
						}					
					}
					
					List<Group> symbolGroup = message.getGroups(NoRelatedSym.FIELD);
					for (Group group:symbolGroup) {
						String symbol = group.getString(Symbol.FIELD);
						if (symbol != null) {
							group.setString(SecurityIDSource.FIELD, "8");
							group.setString(SecurityID.FIELD, symbolConverter.getId(symbol));
							group.removeField(Symbol.FIELD);
						}
					}
					
					modified = true;
				}

			} catch (FieldNotFound e) {
				modified = false;
			}
		} else if (FIXMessageUtil.isOrderSingle(message)
				|| FIXMessageUtil.isCancelReplaceRequest(message)
				|| FIXMessageUtil.isCancelRequest(message)) {
			try {
				String symbol = message.getString(Symbol.FIELD);

				message.setString(SecurityIDSource.FIELD, "8");
				message.setString(SecurityID.FIELD, symbolConverter.getId(symbol));
				message.removeField(Symbol.FIELD);
				message.removeField(Currency.FIELD);
				message.removeField(HandlInst.FIELD);
				message.removeField(SecurityType.FIELD);
				message.removeField(DiscretionOffsetValue.FIELD);
				message.removeField(Product.FIELD);
				message.removeField(SecondaryExecID.FIELD);
				if (FIXMessageUtil.isCancelReplaceRequest(message)) {
					message.removeField(OrderID.FIELD);
				}
				modified = true;

			} catch (FieldNotFound e) {
				modified = false;
			}
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