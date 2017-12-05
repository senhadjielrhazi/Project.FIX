package prd.db.index;

import java.math.BigDecimal;

import org.marketcetera.event.info.EquityInfo;
import org.marketcetera.event.info.InstrumentInfo;
import org.marketcetera.strategy.java.Strategy;
import org.marketcetera.trade.BrokerID;
import org.marketcetera.trade.Equity;
import org.marketcetera.trade.Instrument;

/**
 * FE info db loading strategy.
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
        BigDecimal inMinSize = BigDecimal.valueOf(1.0);
        BigDecimal inTradeFee = BigDecimal.valueOf(150./1000000.);
        
        Instrument inInstrument;
        InstrumentInfo inInfo;
      
        /****************************************************IDX*************************************************/
        inInstrument = new Equity("US30");
        inInfo = new EquityInfo(
        		BigDecimal.valueOf(0.05), 
        		inMinSize, 
        		BigDecimal.valueOf(1.0), 
        		inTradeFee);
        setInstrumentInfo(inBrokerID, inInstrument, inInfo);//
        
        inInstrument = new Equity("UK100");
        inInfo = new EquityInfo(
        		BigDecimal.valueOf(0.2), 
        		inMinSize, 
        		BigDecimal.valueOf(0.1), 
        		inTradeFee);
        setInstrumentInfo(inBrokerID, inInstrument, inInfo);//
        
        inInstrument = new Equity("US500");
        inInfo = new EquityInfo(
        		BigDecimal.valueOf(0.5), 
        		inMinSize, 
        		BigDecimal.valueOf(0.1), 
        		inTradeFee);
        setInstrumentInfo(inBrokerID, inInstrument, inInfo);//
        
        inInstrument = new Equity("US100");
        inInfo = new EquityInfo(
        		BigDecimal.valueOf(0.2), 
        		inMinSize, 
        		BigDecimal.valueOf(0.01), 
        		inTradeFee);
        setInstrumentInfo(inBrokerID, inInstrument, inInfo);//
        
        inInstrument = new Equity("GER30");
        inInfo = new EquityInfo(
        		BigDecimal.valueOf(0.25), 
        		inMinSize, 
        		BigDecimal.valueOf(0.1), 
        		inTradeFee);
        setInstrumentInfo(inBrokerID, inInstrument, inInfo);//
        
        inInstrument = new Equity("FRA40");
        inInfo = new EquityInfo(
        		BigDecimal.valueOf(0.1), 
        		inMinSize, 
        		BigDecimal.valueOf(0.1), 
        		inTradeFee);
        setInstrumentInfo(inBrokerID, inInstrument, inInfo);//
        
        inInstrument = new Equity("EU50");
        inInfo = new EquityInfo(
        		BigDecimal.valueOf(0.1), 
        		inMinSize, 
        		BigDecimal.valueOf(0.1), 
        		inTradeFee);
        setInstrumentInfo(inBrokerID, inInstrument, inInfo);//
        
        inInstrument = new Equity("JAP225");
        inInfo = new EquityInfo(
        		BigDecimal.valueOf(10.), 
        		inMinSize, 
        		BigDecimal.valueOf(1.0), 
        		inTradeFee);
        setInstrumentInfo(inBrokerID, inInstrument, inInfo);//
        
        inInstrument = new Equity("AUS200");
        inInfo = new EquityInfo(
        		BigDecimal.valueOf(0.33), 
        		inMinSize, 
        		BigDecimal.valueOf(1.0), 
        		inTradeFee);
        setInstrumentInfo(inBrokerID, inInstrument, inInfo);//
        
        inInstrument = new Equity("SPA35");
        inInfo = new EquityInfo(
        		BigDecimal.valueOf(0.1), 
        		inMinSize, 
        		BigDecimal.valueOf(1.0), 
        		inTradeFee);
        setInstrumentInfo(inBrokerID, inInstrument, inInfo);//
        
        /*inInstrument = new Equity("CHNIND");
        inInfo = new EquityInfo(
        		inContractSize, 
        		inMinSize, 
        		BigDecimal.valueOf(1.0), 
        		inTradeFee);
        setInstrumentInfo(inBrokerID, inInstrument, inInfo);
        
        inInstrument = new Equity("HKIND");
        inInfo = new EquityInfo(
        		inContractSize, 
        		inMinSize, 
        		BigDecimal.valueOf(1.0), 
        		inTradeFee);
        setInstrumentInfo(inBrokerID, inInstrument, inInfo);*/
        
        /****************************************************CFD*************************************************/
        inInstrument = new Equity("BRENT");
        inInfo = new EquityInfo(
        		BigDecimal.valueOf(10.), 
        		inMinSize, 
        		BigDecimal.valueOf(0.01), 
        		inTradeFee);
        setInstrumentInfo(inBrokerID, inInstrument, inInfo);
        
        inInstrument = new Equity("WTI");
        inInfo = new EquityInfo(
        		BigDecimal.valueOf(10.), 
        		inMinSize, 
        		BigDecimal.valueOf(0.01), 
        		inTradeFee);
        setInstrumentInfo(inBrokerID, inInstrument, inInfo);
        
        warn("Loading Done FE...");
    }
}
