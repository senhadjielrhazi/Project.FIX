package prd.db.forex;

import java.math.BigDecimal;

import org.marketcetera.event.info.CurrencyInfo;
import org.marketcetera.event.info.InstrumentInfo;
import org.marketcetera.strategy.java.Strategy;
import org.marketcetera.trade.BrokerID;
import org.marketcetera.trade.Currency;
import org.marketcetera.trade.DeliveryType;
import org.marketcetera.trade.Instrument;

/**
 * LM info db loading strategy.
 */
public class FEInfoUpload extends Strategy {
    /**
     * Executed when the strategy is started.
     * Use this method to set up data flows
     * and other initialization tasks.
     */
    @Override
    public void onStart() {
        warn("Start Loading!");
        BrokerID inBrokerID = new BrokerID("FE");
        BigDecimal inContractSize = BigDecimal.valueOf(1.);
        BigDecimal inMinSize = BigDecimal.valueOf(1000.);
        BigDecimal inTradeFee = BigDecimal.valueOf(40./1000000.);
        DeliveryType inDeliveryType = DeliveryType.CONTRACT;
        
        Instrument inInstrument;
        InstrumentInfo inInfo;
      
        /****************************************************USD*************************************************/
        inInstrument = new Currency("EUR/USD");
        inInfo = new CurrencyInfo(
        		inContractSize, 
        		inMinSize, 
        		BigDecimal.valueOf(0.00001), 
        		inTradeFee, 
        		inDeliveryType);
        setInstrumentInfo(inBrokerID, inInstrument, inInfo);
        
        inInstrument = new Currency("GBP/USD");
        inInfo = new CurrencyInfo(
        		inContractSize, 
        		inMinSize, 
        		BigDecimal.valueOf(0.00001), 
        		inTradeFee, 
        		inDeliveryType);
        setInstrumentInfo(inBrokerID, inInstrument, inInfo);
        
        inInstrument = new Currency("AUD/USD");
        inInfo = new CurrencyInfo(
        		inContractSize, 
        		inMinSize, 
        		BigDecimal.valueOf(0.00001), 
        		inTradeFee, 
        		inDeliveryType);
        setInstrumentInfo(inBrokerID, inInstrument, inInfo);
        
        inInstrument = new Currency("NZD/USD");
        inInfo = new CurrencyInfo(
        		inContractSize, 
        		inMinSize, 
        		BigDecimal.valueOf(0.00001), 
        		inTradeFee, 
        		inDeliveryType);
        setInstrumentInfo(inBrokerID, inInstrument, inInfo);
        
        inInstrument = new Currency("USD/CHF");
        inInfo = new CurrencyInfo(
        		inContractSize, 
        		inMinSize, 
        		BigDecimal.valueOf(0.00001), 
        		inTradeFee, 
        		inDeliveryType);
        setInstrumentInfo(inBrokerID, inInstrument, inInfo);
        
        inInstrument = new Currency("USD/CAD");
        inInfo = new CurrencyInfo(
        		inContractSize, 
        		inMinSize, 
        		BigDecimal.valueOf(0.00001), 
        		inTradeFee, 
        		inDeliveryType);
        setInstrumentInfo(inBrokerID, inInstrument, inInfo);
        
        inInstrument = new Currency("USD/JPY");
        inInfo = new CurrencyInfo(
        		inContractSize, 
        		inMinSize, 
        		BigDecimal.valueOf(0.001), 
        		inTradeFee, 
        		inDeliveryType);
        setInstrumentInfo(inBrokerID, inInstrument, inInfo);
        
        inInstrument = new Currency("USD/SEK");
        inInfo = new CurrencyInfo(
        		inContractSize, 
        		inMinSize, 
        		BigDecimal.valueOf(0.00001), 
        		inTradeFee, 
        		inDeliveryType);
        setInstrumentInfo(inBrokerID, inInstrument, inInfo);
        
        
        /****************************************************EUR*************************************************/
        inInstrument = new Currency("EUR/GBP");
        inInfo = new CurrencyInfo(
        		inContractSize, 
        		inMinSize, 
        		BigDecimal.valueOf(0.00001), 
        		inTradeFee, 
        		inDeliveryType);
        setInstrumentInfo(inBrokerID, inInstrument, inInfo);
        
        inInstrument = new Currency("EUR/AUD");
        inInfo = new CurrencyInfo(
        		inContractSize, 
        		inMinSize, 
        		BigDecimal.valueOf(0.00001), 
        		inTradeFee, 
        		inDeliveryType);
        setInstrumentInfo(inBrokerID, inInstrument, inInfo);
        
        inInstrument = new Currency("EUR/NZD");
        inInfo = new CurrencyInfo(
        		inContractSize, 
        		inMinSize, 
        		BigDecimal.valueOf(0.00001), 
        		inTradeFee, 
        		inDeliveryType);
        setInstrumentInfo(inBrokerID, inInstrument, inInfo);
        
        inInstrument = new Currency("EUR/CHF");
        inInfo = new CurrencyInfo(
        		inContractSize, 
        		inMinSize, 
        		BigDecimal.valueOf(0.00001), 
        		inTradeFee, 
        		inDeliveryType);
        setInstrumentInfo(inBrokerID, inInstrument, inInfo);
        
        inInstrument = new Currency("EUR/CAD");
        inInfo = new CurrencyInfo(
        		inContractSize, 
        		inMinSize, 
        		BigDecimal.valueOf(0.00001), 
        		inTradeFee, 
        		inDeliveryType);
        setInstrumentInfo(inBrokerID, inInstrument, inInfo);
        
        inInstrument = new Currency("EUR/JPY");
        inInfo = new CurrencyInfo(
        		inContractSize, 
        		inMinSize, 
        		BigDecimal.valueOf(0.001), 
        		inTradeFee, 
        		inDeliveryType);
        setInstrumentInfo(inBrokerID, inInstrument, inInfo);
        
        inInstrument = new Currency("EUR/SEK");
        inInfo = new CurrencyInfo(
        		inContractSize, 
        		inMinSize, 
        		BigDecimal.valueOf(0.00001), 
        		inTradeFee, 
        		inDeliveryType);
        setInstrumentInfo(inBrokerID, inInstrument, inInfo);       
        
        
        /****************************************************GBP*************************************************/
        inInstrument = new Currency("GBP/AUD");
        inInfo = new CurrencyInfo(
        		inContractSize, 
        		inMinSize, 
        		BigDecimal.valueOf(0.00001), 
        		inTradeFee, 
        		inDeliveryType);
        setInstrumentInfo(inBrokerID, inInstrument, inInfo);
        
        inInstrument = new Currency("GBP/NZD");
        inInfo = new CurrencyInfo(
        		inContractSize, 
        		inMinSize, 
        		BigDecimal.valueOf(0.00001), 
        		inTradeFee, 
        		inDeliveryType);
        setInstrumentInfo(inBrokerID, inInstrument, inInfo);
        
        inInstrument = new Currency("GBP/CHF");
        inInfo = new CurrencyInfo(
        		inContractSize, 
        		inMinSize, 
        		BigDecimal.valueOf(0.00001), 
        		inTradeFee, 
        		inDeliveryType);
        setInstrumentInfo(inBrokerID, inInstrument, inInfo);
        
        inInstrument = new Currency("GBP/CAD");
        inInfo = new CurrencyInfo(
        		inContractSize, 
        		inMinSize, 
        		BigDecimal.valueOf(0.00001), 
        		inTradeFee, 
        		inDeliveryType);
        setInstrumentInfo(inBrokerID, inInstrument, inInfo);
        
        inInstrument = new Currency("GBP/JPY");
        inInfo = new CurrencyInfo(
        		inContractSize, 
        		inMinSize, 
        		BigDecimal.valueOf(0.001), 
        		inTradeFee, 
        		inDeliveryType);
        setInstrumentInfo(inBrokerID, inInstrument, inInfo);
        
        
        /****************************************************AUD*************************************************/       
        inInstrument = new Currency("AUD/NZD");
        inInfo = new CurrencyInfo(
        		inContractSize, 
        		inMinSize, 
        		BigDecimal.valueOf(0.00001), 
        		inTradeFee, 
        		inDeliveryType);
        setInstrumentInfo(inBrokerID, inInstrument, inInfo);
        
        inInstrument = new Currency("AUD/CHF");
        inInfo = new CurrencyInfo(
        		inContractSize, 
        		inMinSize, 
        		BigDecimal.valueOf(0.00001), 
        		inTradeFee, 
        		inDeliveryType);
        setInstrumentInfo(inBrokerID, inInstrument, inInfo);
        
        inInstrument = new Currency("AUD/CAD");
        inInfo = new CurrencyInfo(
        		inContractSize, 
        		inMinSize, 
        		BigDecimal.valueOf(0.00001), 
        		inTradeFee, 
        		inDeliveryType);
        setInstrumentInfo(inBrokerID, inInstrument, inInfo);
        
        inInstrument = new Currency("AUD/JPY");
        inInfo = new CurrencyInfo(
        		inContractSize, 
        		inMinSize, 
        		BigDecimal.valueOf(0.001), 
        		inTradeFee, 
        		inDeliveryType);
        setInstrumentInfo(inBrokerID, inInstrument, inInfo);
        
        
        /****************************************************NZD*************************************************/               
        inInstrument = new Currency("NZD/CAD");
        inInfo = new CurrencyInfo(
        		inContractSize, 
        		inMinSize, 
        		BigDecimal.valueOf(0.00001), 
        		inTradeFee, 
        		inDeliveryType);
        setInstrumentInfo(inBrokerID, inInstrument, inInfo);
        
        inInstrument = new Currency("NZD/JPY");
        inInfo = new CurrencyInfo(
        		inContractSize, 
        		inMinSize, 
        		BigDecimal.valueOf(0.001), 
        		inTradeFee, 
        		inDeliveryType);
        setInstrumentInfo(inBrokerID, inInstrument, inInfo);
        
        
        /****************************************************CAD*************************************************/       
        inInstrument = new Currency("CAD/CHF");
        inInfo = new CurrencyInfo(
        		inContractSize, 
        		inMinSize, 
        		BigDecimal.valueOf(0.00001), 
        		inTradeFee, 
        		inDeliveryType);
        setInstrumentInfo(inBrokerID, inInstrument, inInfo);
        
        inInstrument = new Currency("CAD/JPY");
        inInfo = new CurrencyInfo(
        		inContractSize, 
        		inMinSize, 
        		BigDecimal.valueOf(0.001), 
        		inTradeFee, 
        		inDeliveryType);
        setInstrumentInfo(inBrokerID, inInstrument, inInfo);
        
        
        /****************************************************CHF*************************************************/               
        inInstrument = new Currency("CHF/JPY");
        inInfo = new CurrencyInfo(
        		inContractSize, 
        		inMinSize, 
        		BigDecimal.valueOf(0.001), 
        		inTradeFee, 
        		inDeliveryType);
        setInstrumentInfo(inBrokerID, inInstrument, inInfo);

        
        warn("Loading Done FE...");
    }
}
