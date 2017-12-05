package org.marketcetera.server.ws.filters;

import java.util.Map;
import org.springframework.beans.factory.InitializingBean;

import com.google.common.collect.Maps;

/**
 * Conversion of Market symbols to LM symbols.
 */
public class SymbolConverter implements InitializingBean  {
	/**
	 * The map of symbols (Market to LM)
	 */
	private final Map<String, String> symbols = Maps.newHashMap();

	/**
	 * Gets the map of symbols
	 */
	public Map<String, String> getSymbols() {
		return symbols;
	}

	/**
	 * Sets the map of symbols
	 */
	public void setSymbols(Map<String, String> symbols) {
		this.symbols.clear();
		this.symbols.putAll(symbols);
	}
	
	/**
	 * Verify that the symbol is convertible
	 */
	public boolean containSymbol(String inSymbol) {
		return symbols.containsKey(inSymbol);
	}
	
	/**
	 * Verify that the id is convertible
	 */
	public boolean containId(String inId) {
		return symbols.containsValue(inId);
	}
	
	/**
	 * Gets that the id from supplied symbol 
	 */
	public String getId(String inSymbol) {
		return symbols.getOrDefault(inSymbol, inSymbol);
	}
	
	/**
	 * Gets that the symbol from supplied id
	 */
	public String getSymbol(String inId) {	
		for (Map.Entry<String, String> entry : symbols
				.entrySet()) {
			if (entry.getValue().equals(inId)) {
				return entry.getKey();
			}
			
		}
		return inId;
	}
	
    /**
     * Creates a new envelope. This empty constructor is intended for
     * use by JAXB.
     */
	private SymbolConverter() {
	}

	@Override
	public void afterPropertiesSet() throws Exception {
	}
}