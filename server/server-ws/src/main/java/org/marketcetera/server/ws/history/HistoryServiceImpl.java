package org.marketcetera.server.ws.history;


import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.marketcetera.core.IDFactory;
import org.marketcetera.core.NoMoreIDsException;
import org.marketcetera.core.position.PositionKey;
import org.marketcetera.core.time.HasTime;
import org.marketcetera.core.time.Period;
import org.marketcetera.core.time.TimeFactory;
import org.marketcetera.core.time.TimeFactoryImpl;
import org.marketcetera.event.Quote;
import org.marketcetera.event.QuoteEvent;
import org.marketcetera.event.info.InstrumentInfo;
import org.marketcetera.persist.PersistenceException;
import org.marketcetera.trade.*;
import org.marketcetera.util.log.I18NBoundMessage1P;
import org.marketcetera.util.misc.ClassVersion;
import org.marketcetera.ws.server.security.SimpleUser;
import org.marketcetera.server.ws.LongIDFactory;

import com.google.common.collect.Lists;
import com.mchange.v2.c3p0.ComboPooledDataSource;

/**
 * Provides basic service to save and query reports and events.
 */
@ClassVersion("$Id: HistoryServiceImpl.java 16671 2013-08-28 20:09:40Z colin $")
public class HistoryServiceImpl
        implements HistoryService
{
    // INSTANCE DATA.
    private LongIDFactory mReportIDFactory;
    private ReportSavedListener mReportSavedListener;
    private Connection mConnection ;
    private DateTime mPurgeDate;
    
    /**
     * pattern used to identify a history threshold value expressed in seconds
     */
    private static final Pattern SECOND_INTERVAL = Pattern.compile("\\d{1,}[s|S]{1}");
    /**
     * pattern used to identify a history threshold value expressed in minutes
     */
    private static final Pattern MINUTE_INTERVAL = Pattern.compile("\\d{1,}[m|M]{1}");
    /**
     * pattern used to identify a history threshold value expressed in hours
     */
    private static final Pattern HOUR_INTERVAL = Pattern.compile("\\d{1,}[h|H]{1}");
    /**
     * pattern used to identify a history threshold value expressed in days
     */
    private static final Pattern DAY_INTERVAL = Pattern.compile("\\d{1,}[d|D]{1}");
    /**
     * pattern used to identify a history threshold value expressed in weeks
     */
    private static final Pattern WEEK_INTERVAL = Pattern.compile("\\d{1,}[w|W]{1}");
    /**
     * creates time values
     */
    private TimeFactory timeFactory = new TimeFactoryImpl();
    /**
     * historical data base
     */
	private static String DATA_BASE_ID = "hqdb";
	
    // CONSTRUCTORS.
    /**
     * Creates a new basic history service provider.
     */
    public HistoryServiceImpl() {}

    // HistoryService.
    @Override
    public void init
    	(IDFactory idFactory, 
    	 ReportSavedListener reportSavedListener,
    	 ComboPooledDataSource dataSource)
    			throws PersistenceException
    {
        mReportIDFactory=new LongIDFactory(idFactory);
        mReportSavedListener=reportSavedListener;
        
        try {
			mConnection=dataSource.getConnection();
		} catch (SQLException e) {
			throw new PersistenceException(new I18NBoundMessage1P(Messages.RHS_ERROR_CONNECTION, e));
		}
        
        // purge report history, if necessary
        if(mPurgeDate != null) {
            Messages.RHS_PURGING_RECORDS.info(this,mPurgeDate);
            int count = PersistentReport.deleteBefore(mPurgeDate.toDate());
            Messages.RHS_RECORDS_PURGED.info(this,count);
        }
    }
    
    /* (non-Javadoc)
     * @see org.marketcetera.server.ws.history.HistoryService#getReportsSince(org.marketcetera.ws.server.security.SimpleUser, java.util.Date)
     */
    @Override
    public ReportBaseImpl[] getReportsSince
    	(SimpleUser inUser, Date inDate)
    			throws PersistenceException {
        MultiPersistentReportQuery query = MultiPersistentReportQuery.all();
        query.setSendingTimeAfterFilter(inDate);
        if (!inUser.isSuperuser()) {
            query.setActorFilter(inUser);
        }
        query.setEntityOrder(MultiPersistentReportQuery.BY_ID);

        List<PersistentReport> reportList = query.fetch();
        ReportBaseImpl [] reports = new ReportBaseImpl[reportList.size()];
        int i = 0;
        for(PersistentReport report: reportList) {
            reports[i++] = (ReportBaseImpl) report.toReport();
        }
        return reports;
    }

    /* (non-Javadoc)
     * @see org.marketcetera.server.ws.history.HistoryService#getOpenOrders(org.marketcetera.ws.server.security.SimpleUser)
     */
    @Override
    public List<ReportBaseImpl> getOpenOrders
    	(SimpleUser inUser)
    			throws PersistenceException
    {
        List<ReportBaseImpl> reports = new ArrayList<>();
        List<ExecutionReportSummary> rawReports = ExecutionReportSummary.getOpenOrders(inUser);
        try {
            for(ExecutionReportSummary summary : rawReports) {
                reports.add((ReportBaseImpl)summary.getReport().toReport());
            }
        } catch (PersistenceException e) {
            throw new PersistenceException(e);
        }
        return reports;
    }
    
    /* (non-Javadoc)
     * @see org.marketcetera.server.ws.history.HistoryService#getPositionAsOf(org.marketcetera.ws.server.security.SimpleUser, java.util.Date, org.marketcetera.trade.Instrument)
     */
    @Override
    public BigDecimal getPositionAsOf
        (SimpleUser inUser, Date inDate, Instrument inInstrument)
        		throws PersistenceException
    {
        return ExecutionReportSummary.getPositionAsOf(inUser,inDate,inInstrument);
    }

    /* (non-Javadoc)
     * @see org.marketcetera.server.ws.history.HistoryService#getPositionsAsOf(org.marketcetera.ws.server.security.SimpleUser, java.util.Date, org.marketcetera.trade.SecurityType)
     */
    @Override
    public Map<PositionKey, BigDecimal> getPositionsAsOf
        (SimpleUser inUser, Date inDate, SecurityType inSecurityType)
        		throws PersistenceException
    {
        return ExecutionReportSummary.getPositionsAsOf(inUser,inDate,inSecurityType);
    }
    
    /* (non-Javadoc)
     * @see org.marketcetera.server.ws.history.HistoryService#getAllPositionsAsOf(org.marketcetera.ws.server.security.SimpleUser, java.util.Date)
     */
    @Override
    public Map<PositionKey, BigDecimal> getAllPositionsAsOf
    	(SimpleUser inUser, Date inDate)
    			throws PersistenceException
    {
        return ExecutionReportSummary.getAllPositionsAsOf(inUser,inDate);
    }
    
    /* (non-Javadoc)
     * @see org.marketcetera.server.ws.history.HistoryService#saveReport(org.marketcetera.trade.ReportBase)
     */
    @Override
    public void saveReport
    	(ReportBase report)
    			throws PersistenceException
    {
        boolean success=false;
        try {
            assignID(report);
            PersistentReport.save(report);
            success=true;
            Messages.RHS_PERSISTED_DB.info(this,report);
        } finally {
            invokeListener(report,success);
        }
    }
    
    /* (non-Javadoc)
     * @see org.marketcetera.server.ws.history.HistoryService#deleteReport(org.marketcetera.trade.ReportBase)
     */
    @Override
    public void deleteReport
    	(ReportBase inReport)
    			throws PersistenceException
    {
        PersistentReport.delete(inReport);
    }
    
    /* (non-Javadoc)
     * @see org.marketcetera.server.ws.history.HistoryService#getInstruments(org.marketcetera.trade.Instrument)
     */
    @Override
    public Set<Instrument> getInstruments
        (BrokerID brokerID)
        		throws PersistenceException
    {
    	return SimpleInstrument.getInstruments(brokerID);
    }
    
    /* (non-Javadoc)
     * @see org.marketcetera.server.ws.history.HistoryService#getInstrumentInfo(org.marketcetera.trade.BrokerID, org.marketcetera.trade.Instrument)
     */
    @Override
    public InstrumentInfo getInstrumentInfo
        (BrokerID brokerID, Instrument instrument)
        		throws PersistenceException
    {
    	return SimpleInstrument.getInfo(brokerID, instrument);
    }
    
    /* (non-Javadoc)
     * @see org.marketcetera.server.ws.history.HistoryService#setInstrumentInfo(org.marketcetera.trade.BrokerID, org.marketcetera.trade.Instrument, org.marketcetera.event.info.InstrumentInfo)
     */
    @Override
    public void setInstrumentInfo
        (BrokerID brokerID, Instrument instrument, InstrumentInfo info)
        		throws PersistenceException
    {
        SimpleInstrument.setInfo(brokerID, instrument, info);
        Messages.RHS_PERSISTED_DB.info(this,info);
    }
    
    /* (non-Javadoc)
     * @see org.marketcetera.server.ws.history.HistoryService#deleteInstrumentInfo(org.marketcetera.trade.BrokerID, org.marketcetera.trade.Instrument)
     */
    @Override
    public void deleteInstrumentInfo
    	(BrokerID brokerID, Instrument instrument)
    			throws PersistenceException
    {
    	SimpleInstrument.deleteInfo(brokerID, instrument);
    }
    
    /* (non-Javadoc)
     * @see org.marketcetera.server.ws.history.HistoryService#getActorID(org.marketcetera.trade.OrderID)
     */
    @Override
    public UserID getActorID
        (OrderID orderID)
        		throws PersistenceException
    {
        return PersistentReport.getActorID(orderID);
    }
    
    /* (non-Javadoc)
     * @see org.marketcetera.server.ws.history.HistoryService#getRootOrderID(org.marketcetera.util.ws.stateful.ClientContext, org.marketcetera.trade.OrderID)
     */
	@Override
	public OrderID getRootOrderID
		(OrderID inOrderID) 
				throws PersistenceException 
	{
		 return ExecutionReportSummary.getRootOrderID(inOrderID);
	}
    
	/**
	 * Returns the name of the table for a given instrument and period.
	 * 
	 * @param instrument The instrument
	 * @param period The period
	 * 
	 * @return the table name
	 */
	private String getTableName(Instrument instrument, Period period){
		return String.format("%s_%s", instrument.getSymbol().replace("/", ""), period.name());
	}
	
    /* (non-Javadoc)
     * @see org.marketcetera.server.ws.history.HistoryService#getQuoteHistory(org.marketcetera.trade.Instrument, org.marketcetera.core.time.Period, java.util.Date, java.util.Date)
     */
    @Override
    public List<Quote> getQuoteHistory(Instrument instrument, Period period, 
    		Date fromDate, Date toDate)
            throws PersistenceException
    {
        List<Quote> quotes = Lists.newArrayList();
       
		String idDT = getTableName(instrument, period);
        try {
            Statement statement = getConnection().createStatement();
            
            // use DB ..
            statement.executeUpdate("USE " + DATA_BASE_ID); 
            
            // retrieve results
     		ResultSet rs = statement.executeQuery("SELECT * FROM "+ idDT);
    		while (rs.next()) {
    			BigDecimal inOpen = new BigDecimal(rs.getString("open"));
    			BigDecimal inHigh = new BigDecimal(rs.getString("high"));
				BigDecimal inLow = new BigDecimal(rs.getString("low"));
				BigDecimal inClose = new BigDecimal(rs.getString("close"));
				BigDecimal inVolume = new BigDecimal(rs.getString("volume"));
				BigDecimal inSpread = new BigDecimal(rs.getString("spread"));
				long inTime = HasTime.parse(rs.getString("time"));
				
				if(inTime >= fromDate.getTime() 
						&& inTime <= toDate.getTime()){
					quotes.add(new Quote(inTime, inOpen, inHigh, inLow, inClose, inVolume, inSpread));
				}
			}
    		rs.close();
    		
    		// clean up
            statement.close();
        } catch (SQLException e) {
        	Messages.LOADING_DATA_ERROR.error(this, instrument, period, fromDate, toDate, e);
        }
        
        return quotes;
    }
    
    /*
     * (non-Javadoc)
     * @see org.marketcetera.server.ws.history.HistoryService#saveQuote(org.marketcetera.event.QuoteEvent)
     */
	@Override
	public void saveQuote
		(QuoteEvent quoteEvent) 
				throws PersistenceException 
	{
		String idDT = getTableName(quoteEvent.getInstrument(), quoteEvent.getPeriod());
        try {
            Statement statement = getConnection().createStatement();
            
            // create the DB .. 
            statement.executeUpdate("CREATE DATABASE IF NOT EXISTS " + DATA_BASE_ID);

            // use DB ..
            statement.executeUpdate("USE " + DATA_BASE_ID); 

            // create table ...
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS " + idDT
            					 +"(open NUMERIC(17,7) NOT NULL,"
                                 +"high NUMERIC(17,7) NOT NULL,"
                                 +"low NUMERIC(17,7) NOT NULL,"
                                 +"close NUMERIC(17,7) NOT NULL,"
                                 +"volume NUMERIC(17,7) NOT NULL,"
                                 +"time VARCHAR(255) NOT NULL,"
                                 +"spread NUMERIC(17,7) NOT NULL,"
                                 +"UNIQUE (time))");

            // insert the quote
            Quote quote = quoteEvent.getQuote();
            statement.executeUpdate("INSERT INTO "+ idDT +
            		"(open,high,low,close,volume,time,spread) VALUES('"
            		+quote.getOpen() + "','"
            		+quote.getHigh() + "','"
            		+quote.getLow() + "','"
            		+quote.getClose() + "','"
            		+quote.getVolume() + "','"
            		+HasTime.format(quote.getTime()) + "','"
            		+quote.getSpread() + "')" +
            		"ON DUPLICATE KEY UPDATE open=VALUES(open), high=VALUES(high), low=VALUES(low), close=VALUES(close)" +
            		", volume=VALUES(volume), time=VALUES(time), spread=VALUES(spread);");
            
            // clean up
            statement.close();
            Messages.RHS_PERSISTED_DB.info(this, quoteEvent);
        } catch (SQLException e) {
        	Messages.RHS_PERSIST_ERROR.error(this, quoteEvent, e);
        }
	}

	/*
	 * (non-Javadoc)
	 * @see org.marketcetera.server.ws.history.HistoryService#deleteQuote(org.marketcetera.event.QuoteEvent)
	 */
	@Override
	public void deleteQuote
		(QuoteEvent quoteEvent) 
				throws PersistenceException 
	{
		String idDT = getTableName(quoteEvent.getInstrument(), quoteEvent.getPeriod());
        try {
            Statement statement = getConnection().createStatement();
            
            // use DB ..
            statement.executeUpdate("USE " + DATA_BASE_ID); 
            
            // delete the quote
            String cltime = quoteEvent.formatedTime();
            statement.execute("DELETE FROM `" + idDT + "` WHERE `time` ='"+cltime+"' LIMIT 1");
            
            // clean up
            statement.close();
        } catch (SQLException e) {
        	Messages.RHS_PERSIST_ERROR.error(this, quoteEvent, e);
        }   
	}
	
    /**
     * Get the purgeDate value.
     *
     * @return a <code>String</code> value
     */
    public String getPurgeDate()
    {
        return String.valueOf(mPurgeDate);
    }
    
    /**
     * Sets the purgeDate value.
     * 
     * <p>Purge date describes a point in time UTC before which all report history
     * should be truncated. May be described as an actual point in time:
     * <ul>
     *   <li>YYYYMMDD-HH:MM:SS</li>
     *   <li>HH:MM:SS</li>
     *   <li>HH:MM</li>
     * </ul>
     * May also be described as a relative point in time:
     * <ul>
     *   <li>4w</li>
     *   <li>30d</li>
     *   <li>3h</li>
     *   <li>120m</li>
     *   <li>10s</li>
     * </ul>
     *
     * @param inPurgeDate a <code>String</code> value
     */
    public void setPurgeDate(String inPurgeDate)
    {
        mPurgeDate = translateHistoryValue(inPurgeDate);
    }
    
    // INSTANCE METHODS.
    /**
     * Returns the receiver's report ID factory.
     *
     * @return The factory.
     */
    protected LongIDFactory getReportIDFactory()
    {
        return mReportIDFactory;
    }

    /**
     * Returns the receiver's listener which should be notified after
     * a report has been saved.
     *
     * @return The listener. It may be null.
     */
    protected ReportSavedListener getReportSavedListener()
    {
        return mReportSavedListener;
    }
    
    /**
     * Returns the receiver's pooled Connection.
     *
     * @return The connection.
     */
    protected Connection getConnection()
    {
        return mConnection;
    }
    
    /**
     * Sets the ID of the given report.
     *
     * @param report The report.
     *
     * @throws PersistenceException Thrown if there were errors
     * assigning the ID.
     */
    protected void assignID(ReportBase report)
        throws PersistenceException
    {
        try {
            ReportBaseImpl.assignReportID
                ((ReportBaseImpl)report,
                 new ReportID(getReportIDFactory().getNext()));
        } catch (NoMoreIDsException ex) {
            throw new PersistenceException(ex,Messages.RHS_NO_MORE_IDS);
        }
    }

    /**
     * Invokes the listener which should be notified after the given
     * report has been saved. The given flag indicates whether saving
     * completed successfully or not.
     *
     * @param report The report.
     * @param status True if saving completed successfully.
     */
    protected void invokeListener
        (ReportBase report,
         boolean status)
    {
        if (getReportSavedListener()!=null) {
            getReportSavedListener().reportSaved(report,status);
        }
    }
    /**
     * Translates the given literal value to a <code>DateTime</code> value.
     *
     * @param inHistoryValue a <code>String</code> value
     * @return a <code>DateTime</code> value
     */
    private DateTime translateHistoryValue(String inHistoryValue)
    {
        inHistoryValue = StringUtils.trimToNull(inHistoryValue);
        if(inHistoryValue == null) {
            return new DateTime(0);
        }
        if(SECOND_INTERVAL.matcher(inHistoryValue).matches()) {
            int seconds = Integer.parseInt(inHistoryValue.substring(0,inHistoryValue.length()-1));
            return new DateTime().minusSeconds(seconds);
        }
        if(MINUTE_INTERVAL.matcher(inHistoryValue).matches()) {
            int minutes = Integer.parseInt(inHistoryValue.substring(0,inHistoryValue.length()-1));
            return new DateTime().minusMinutes(minutes);
        }
        if(HOUR_INTERVAL.matcher(inHistoryValue).matches()) {
            int hours = Integer.parseInt(inHistoryValue.substring(0,inHistoryValue.length()-1));
            return new DateTime().minusHours(hours);
        }
        if(DAY_INTERVAL.matcher(inHistoryValue).matches()) {
            int days = Integer.parseInt(inHistoryValue.substring(0,inHistoryValue.length()-1));
            return new DateTime().minusDays(days);
        }
        if(WEEK_INTERVAL.matcher(inHistoryValue).matches()) {
            int weeks = Integer.parseInt(inHistoryValue.substring(0,inHistoryValue.length()-1));
            return new DateTime().minusWeeks(weeks);
        }
        return timeFactory.create(inHistoryValue);
    }
}
