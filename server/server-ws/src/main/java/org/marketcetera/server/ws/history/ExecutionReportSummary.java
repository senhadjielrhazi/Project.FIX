package org.marketcetera.server.ws.history;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

import javax.persistence.*;

import org.hibernate.annotations.Type;
import org.marketcetera.core.position.PositionKey;
import org.marketcetera.core.position.PositionKeyFactory;
import org.marketcetera.persist.EntityBase;
import org.marketcetera.persist.PersistContext;
import org.marketcetera.persist.PersistenceException;
import org.marketcetera.persist.Transaction;
import org.marketcetera.symbol.InstrumentSymbolResolver;
import org.marketcetera.trade.*;
import org.marketcetera.util.log.SLF4JLoggerProxy;
import org.marketcetera.util.misc.ClassVersion;
import org.marketcetera.ws.server.security.SimpleUser;

/**
 * Maintains a summary of fields of an ExecutionReport
 * to aid Position calculations. The lifecycle of this object
 * is controlled by {@link PersistentReport}
 */
@ClassVersion("$Id: ExecutionReportSummary.java 16670 2013-08-28 19:49:06Z colin $")
@Entity
@Table(name="execreports")
@NamedQueries({
    @NamedQuery(name="rootIDForOrderID",query="select e.rootID from ExecutionReportSummary e where e.orderID = :orderID"),
    @NamedQuery(name="setIsOpen",query="update ExecutionReportSummary e set e.isOpen = false where e.rootID = :rootID and e.id != :Id") })
@SqlResultSetMappings({
    @SqlResultSetMapping(name = "positionForSymbol",
            columns = {@ColumnResult(name = "position")}),
    @SqlResultSetMapping(name = "positionsForType",
            columns = {
            	@ColumnResult(name = "brokerID"),
                @ColumnResult(name = "fullSymbol"),
                @ColumnResult(name = "account"),
                @ColumnResult(name = "actor"),
                @ColumnResult(name = "position")
                    }),
    @SqlResultSetMapping(name = "allPositions",
           columns = {
        		@ColumnResult(name = "brokerID"),   
        		@ColumnResult(name = "securityType"),
                @ColumnResult(name = "fullSymbol"),
                @ColumnResult(name = "account"),
                @ColumnResult(name = "actor"),
                @ColumnResult(name = "position")
                   })
        })
// CD 26-Apr-2012 ORS-84
// The position queries should ignore PENDING ERs. This is done by excluding Server with particular order status values.
// Hibernate maps enums to 0-based index values, so 7, 11, and 15 map to values in the OrderStatus enum, the PENDING values.
@NamedNativeQueries({
    @NamedNativeQuery(name = "positionForSymbol",query = "select " +
            "sum(case when e.side = :sideBuy then e.cumQuantity else -e.cumQuantity end) as position " +
            "from execreports e " +
            "where e.fullSymbol = :fullSymbol " +
            "and (e.securityType is null " +
            "or e.securityType = :securityType) " +
            "and e.sendingTime <= :sendingTime " +
            "and (:allActors or e.actor_id = :actorID) " +
            "and e.id = " +
            "(select max(s.id) from execreports s where s.rootID = e.rootID and s.orderStatus not in (7,11,15))",
            resultSetMapping = "positionForSymbol"),
    @NamedNativeQuery(name = "positionsForType",query = "select " +
            "e.brokerID as brokerID, e.fullSymbol as fullSymbol, e.account as account, r.actor_id as actor, sum(case when e.side = :sideBuy then e.cumQuantity else -e.cumQuantity end) as position " +
            "from execreports e " +
            "join reports r on (e.report_id=r.id) " +
            "where e.sendingTime <= :sendingTime " +
            "and (e.securityType is null " +
            "or e.securityType = :securityType) " +
            "and (:allActors or e.actor_id = :actorID) " +
            "and e.id = " +
            "(select max(s.id) from execreports s where s.rootID = e.rootID and s.orderStatus not in (7,11,15)) " +
            "group by brokerID, fullSymbol, account, actor having position <> 0",
            resultSetMapping = "positionsForType"),
    @NamedNativeQuery(name = "allPositions",query = "select " +
            "e.brokerID as brokerID, e.securityType as securityType, e.fullSymbol as fullSymbol, e.account as account, r.actor_id as actor, sum(case when e.side = :sideBuy then e.cumQuantity else -e.cumQuantity end) as position " +
            "from execreports e " +
            "join reports r on (e.report_id=r.id) " +
            "where e.sendingTime <= :sendingTime " +
            "and (:allActors or e.actor_id = :actorID) " +
            "and e.id = " +
            "(select max(s.id) from execreports s where s.rootID = e.rootID and s.orderStatus not in (7,11,15)) " +
            "group by brokerID, securityType, fullSymbol, account, actor having position <> 0",
             resultSetMapping = "allPositions"), 
    @NamedNativeQuery(name="openOrders",query="select * from execreports e where e.isOpen=true and (:allActors=true or e.actor_id=:actorID)",resultClass=ExecutionReportSummary.class),
    @NamedNativeQuery(name="deleteReportsFor",query="delete from execreports where report_id=:id",resultClass=ExecutionReportSummary.class)
        })
class ExecutionReportSummary extends EntityBase {
    /**
     * Returns all open orders visible to the given user.
     *
     * @param inUser a <code>SimplUser</code> value
     * 
     * @return a <code>List&lt;ExecutionReportSummary&gt;</code> value
     * @throws PersistenceException if an error occurs retrieving the orders
     */
    static List<ExecutionReportSummary> getOpenOrders(final SimpleUser inUser)
            throws PersistenceException
    {
        return executeRemote(new Transaction<List<ExecutionReportSummary>>() {
            @Override
            @SuppressWarnings("unchecked")
            public List<ExecutionReportSummary> execute(EntityManager inEntityManager,
                                                        PersistContext inContext)
                    throws PersistenceException
            {
                Query query = inEntityManager.createNamedQuery("openOrders");  //$NON-NLS-1$
                query.setParameter("actorID",inUser.getUserID().getValue());  //$NON-NLS-1$
                query.setParameter("allActors",inUser.isSuperuser());  //$NON-NLS-1$
                return query.getResultList();
            }
            private static final long serialVersionUID = 1L;
        },null);
    }
    
    /**
     * Gets the current aggregate position for the instrument based on
     * execution reports received on or before the supplied time, and which
     * are visible to the given user.
     *
     * <p>
     * Buy trades result in positive positions. All other kinds of trades
     * result in negative positions.
     *
     * @param inUser the user making the query. Cannot be null.
     * @param inDate the time. execution reports with sending time values less
     * than or equal to this time are included in this calculation.
     * @param inInstrument the instrument for which this position needs to be computed
     *
     * @return the aggregate position for the equity.
     * @throws PersistenceException if there were errors retrieving the
     * position.
     */
    static BigDecimal getPositionAsOf
        (final SimpleUser inUser,
         final Date inDate,
         final Instrument inInstrument)
        throws PersistenceException
    {
        BigDecimal position = executeRemote(new Transaction<BigDecimal>() {
            private static final long serialVersionUID = 1L;

            @Override
            public BigDecimal execute(EntityManager em, PersistContext context) {
                Query query = em.createNamedQuery(
                        "positionForSymbol");  //$NON-NLS-1$

                query.setParameter("actorID",inUser.getUserID().getValue());  //$NON-NLS-1$
                query.setParameter("allActors",inUser.isSuperuser());  //$NON-NLS-1$
                query.setParameter("sideBuy", Side.Buy.ordinal());  //$NON-NLS-1$
                query.setParameter("fullSymbol", inInstrument.getFullSymbol());  //$NON-NLS-1$
                query.setParameter("securityType", inInstrument.getSecurityType().getValue());  //$NON-NLS-1$
                query.setParameter("sendingTime", inDate,  //$NON-NLS-1$
                        TemporalType.TIMESTAMP);
                return (BigDecimal) query.getSingleResult();  //$NON-NLS-1$
            }
        }, null);
        return position == null? BigDecimal.ZERO: position;
    }
    
    /**
     * Returns the aggregate position of each (instrument,account,actor)
     * tuple based on all reports received for each tuple with given security
     * type on or before the supplied date, and which are visible to the given user.
     *
     * <p> Buy trades result in positive positions. All other kinds of
     * trades result in negative positions.
     *
     * @param inUser the user making the query. Cannot be null.
     * @param inDate the date to compare with all the reports. Only
     * the reports that were received on or prior to this date will be
     * used in this calculation.  Cannot be null.
     * @param inSecurityType The security type.
     * 
     * @return the position map.
     * @throws PersistenceException if there were errors retrieving the
     * position map.
     */
    static Map<PositionKey, BigDecimal> getPositionsAsOf
        (final SimpleUser inUser,
         final Date inDate,
         SecurityType inSecurityType)
        throws PersistenceException
    {
        return executeRemote(new Transaction<Map<PositionKey, BigDecimal>>() {
            private static final long serialVersionUID = 1L;

            @Override
            public Map<PositionKey, BigDecimal> execute(EntityManager em,
                                                    PersistContext context) {
                Query query = em.createNamedQuery(
                        "positionsForType");  //$NON-NLS-1$
                query.setParameter("actorID",inUser.getUserID().getValue());  //$NON-NLS-1$
                query.setParameter("allActors",inUser.isSuperuser());  //$NON-NLS-1$
                query.setParameter("sideBuy", Side.Buy.ordinal());  //$NON-NLS-1$
                query.setParameter("securityType", inSecurityType.getValue());  //$NON-NLS-1$
                query.setParameter("sendingTime", inDate,  //$NON-NLS-1$
                        TemporalType.TIMESTAMP);
                HashMap<PositionKey, BigDecimal> map = new HashMap<>();
                List<?> list = query.getResultList();
                Object[] columns;
                for(Object o: list) {
                    columns = (Object[]) o;
                    //4 columns
                    if(columns.length > 1) {
                    	//first one is the brokerID
                        //second one is the fullSymbol
                        //third one is the account
                        //fourth one is the actor ID
                        //fifth one is the position
                    	BrokerID brokerID = new BrokerID((String)columns[0]);
                    	Instrument instrument = InstrumentSymbolResolver.resolveSymbol(inSecurityType, (String)columns[1]);                  	
                    	map.put(PositionKeyFactory.createKey
                                (instrument,brokerID,
                                 (String)columns[2],
                                 ((columns[3]==null)?null:
                                  ((BigInteger)columns[3]).toString())),
                                 (BigDecimal)columns[4]);
                    }
                }
                return map;
            }
        }, null);
    }

    /**
     * Returns the aggregate position of each instrument (instrument,account,actor)
     * tuple based on all reports received for each instrument on or before
     * the supplied date, and which are visible to the given user.
     *
     * <p> Buy trades result in positive positions. All other kinds of
     * trades result in negative positions.
     *
     * @param inUser the user making the query. Cannot be null.
     * @param inDate the date to compare with all the reports. Only
     * the reports that were received on or prior to this date will be
     * used in this calculation.  Cannot be null.
     *
     * @return the position map.
     * @throws PersistenceException if there were errors retrieving the
     * position map.
     */
    static Map<PositionKey, BigDecimal> getAllPositionsAsOf
        (final SimpleUser inUser,
         final Date inDate)
        throws PersistenceException {
        return executeRemote(new Transaction<Map<PositionKey, BigDecimal>>() {
            private static final long serialVersionUID = 1L;

            @Override
            public Map<PositionKey, BigDecimal> execute(EntityManager em,
                                                    PersistContext context) {
                Query query = em.createNamedQuery(
                        "allPositions");  //$NON-NLS-1$
                query.setParameter("actorID",inUser.getUserID().getValue());  //$NON-NLS-1$
                query.setParameter("allActors",inUser.isSuperuser());  //$NON-NLS-1$
                query.setParameter("sideBuy", Side.Buy.ordinal());  //$NON-NLS-1$
                query.setParameter("sendingTime", inDate,  //$NON-NLS-1$
                        TemporalType.TIMESTAMP);
                HashMap<PositionKey, BigDecimal> map = new HashMap<>();
                List<?> list = query.getResultList();
                Object[] columns;
                for(Object o: list) {
                    columns = (Object[]) o;
                    //5 columns
                    if(columns.length > 1) {
                    	//first one is the brokerID
                        //second one is the securityType
                        //third one is the fullSymbol
                        //fourth one is the account
                        //fifth one is the actor ID
                    	//sixth one is the position
                    	BrokerID brokerID = new BrokerID((String)columns[0]);
                    	Instrument instrument = InstrumentSymbolResolver.resolveSymbol((String)columns[1], (String)columns[2]); 
                    	
                        map.put(PositionKeyFactory.createKey
                                (instrument,brokerID,
                                 (String)columns[3],
                                 ((columns[4]==null)?null:
                                  ((BigInteger)columns[4]).toString())),
                                 (BigDecimal)columns[5]);
                    }
                }
                return map;
            }
        }, null);
    }
    
    /**
     * Gets the root order ID for the given order ID.
     *
     * @param inOrderID an <code>OrderID</code> value
     * 
     * @return an <code>OrderID</code> value or <code>null</code>
     * @throws PersistenceException if there were errors retrieving the
     * root ID.
     */
	public static OrderID getRootOrderID(final OrderID inOrderID) 
			 throws PersistenceException {
		 return executeRemote(new Transaction<OrderID>() {
	            @Override
	            public OrderID execute(EntityManager em, PersistContext inContext)
	                    throws PersistenceException
	            {
	            	// CD 17-Mar-2011 ORS-79
	                // we need to find the correct root ID of the incoming ER. for cancels and cancel/replaces,
	                //  this is easy - we can look up the root ID from the origOrderID. for a partial fill or fill
	                //  of an original order, this is also easy - the rootID is just the orderID. the difficult case
	                //  is a partial fill or fill of a replaced order. the origOrderID won't be present (not required)
	                //  but there still exists an order chain to be respected or position reporting will be broken.
	                //  therefore, the algorithm should be:
	                // if the original orderID is present, use the root from that order
	                // if it's not present, look for the rootID of an existing record with the same orderID
	                Query query = em.createNamedQuery("rootIDForOrderID");  //$NON-NLS-1$
	                SLF4JLoggerProxy.debug(ExecutionReportSummary.class,
	                                       "Searching for rootID for {}",  //$NON-NLS-1$
	                                       inOrderID);
	                SLF4JLoggerProxy.debug(ExecutionReportSummary.class,
	                                           "Using origOrderID {} for query",  //$NON-NLS-1$
	                                           inOrderID);
	                query.setParameter("orderID",  //$NON-NLS-1$
	                    		inOrderID);

	                List<?> list = query.getResultList();
	                if(list.isEmpty()) {
	                    SLF4JLoggerProxy.debug(ExecutionReportSummary.class,
	                                           "No other orders match this orderID - this must be the first in the order chain");  //$NON-NLS-1$
	                    // this is the first order in this chain
	                    return inOrderID;
	                } else {
	                    OrderID rootID = (OrderID)list.get(0);
	                    SLF4JLoggerProxy.debug(ExecutionReportSummary.class,
	                                           "Using {} for rootID",  //$NON-NLS-1$
	                                           rootID);
	                    return rootID;
	                }
	            }
	            private static final long serialVersionUID = 1L;
	        },null);
	}
	
    /**
     * Deletes any <code>ExecutionReportSummary</code> objects related to the given <code>PersistentReport</code>.
     *
     * @param inReport a <code>PersistentReport</code> value
     * @throws PersistenceException if an error occurs deleting the reports
     */
    static void deleteReportsFor(final PersistentReport inReport)
            throws PersistenceException
    {
        executeRemote(new Transaction<Integer>() {
            @Override
            public Integer execute(EntityManager inEntityManager,
                                   PersistContext inContext)
            {
                Query query = inEntityManager.createNamedQuery("deleteReportsFor");  //$NON-NLS-1$
                query.setParameter("id",inReport.getId());  //$NON-NLS-1$
                return query.executeUpdate();
            }
            private static final long serialVersionUID = 1L;
        },null);
    }
    /**
     * 
     *
     *
     * @param inReports
     * @throws PersistenceException
     */
    static void deleteReportsIn(final List<PersistentReport> inReports)
            throws PersistenceException
    {
        executeRemote(new Transaction<Integer>() {
            @Override
            public Integer execute(EntityManager inEntityManager,
                                   PersistContext inContext)
            {
                List<Long> ids = new ArrayList<Long>();
                if(inReports != null) {
                    for(PersistentReport report : inReports) {
                        ids.add(report.getId());
                    }
                }
                return inEntityManager.createNativeQuery("DELETE FROM execreports WHERE report_id IN (:ids)").setParameter("ids",ids).executeUpdate();
            }
            private static final long serialVersionUID = 1L;
        },null);
    }
    /**
     * Creates an instance.
     *
     * @param inReport The original execution report message.
     * @param inSavedReport the saved persistent report.
     */
    ExecutionReportSummary(ExecutionReport inReport,
                           PersistentReport inSavedReport) {
        setReport(inSavedReport);
        mOrderID = inReport.getOrderID();
        mOrigOrderID = inReport.getOriginalOrderID();
        Instrument instrument = inReport.getInstrument();
        if (instrument != null) {
            mSecurityType = instrument.getSecurityType().getValue();
            mFullSymbol = instrument.getFullSymbol();
        }
        mAccount = inReport.getAccount();
        mSide = inReport.getSide();
        mCumQuantity = inReport.getCumulativeQuantity();
        mAvgPrice = inReport.getAveragePrice();
        mLastQuantity = inReport.getLastQuantity();
        mLastPrice = inReport.getLastPrice();
        mOrderStatus = inReport.getOrderStatus();
        mSendingTime = inReport.getSendingTime();
        mBrokerID = inSavedReport.getBrokerID();
        mActor = inSavedReport.getActor();
        mIsOpen = inReport.isCancelable();
    }

    /**
     * Saves this instance within an existing transaction.
     *
     * @param inManager the entity manager instance
     * @param inContext the persistence context
     *
     * @throws PersistenceException if there were errors.
     */
    void localSave(EntityManager inManager,
                   PersistContext inContext)
            throws PersistenceException {
        super.saveLocal(inManager, inContext);
    }
	
    /* (non-Javadoc)
     * @see org.marketcetera.persist.EntityBase#preSaveLocal(javax.persistence.EntityManager, org.marketcetera.persist.PersistContext)
     */
    @Override
    protected void preSaveLocal(EntityManager em, PersistContext context)
            throws PersistenceException {
        super.preSaveLocal(em, context);
        // CD 17-Mar-2011 ORS-79
        // we need to find the correct root ID of the incoming ER. for cancels and cancel/replaces,
        //  this is easy - we can look up the root ID from the origOrderID. for a partial fill or fill
        //  of an original order, this is also easy - the rootID is just the orderID. the difficult case
        //  is a partial fill or fill of a replaced order. the origOrderID won't be present (not required)
        //  but there still exists an order chain to be respected or position reporting will be broken.
        //  therefore, the algorithm should be:
        // if the original orderID is present, use the root from that order
        // if it's not present, look for the rootID of an existing record with the same orderID
        Query query = em.createNamedQuery("rootIDForOrderID");  //$NON-NLS-1$
        SLF4JLoggerProxy.debug(ExecutionReportSummary.class,
                               "Searching for rootID for {}",  //$NON-NLS-1$
                               getOrderID());
        if(getOrigOrderID() == null) {
            SLF4JLoggerProxy.debug(ExecutionReportSummary.class,
                                   "No origOrderID present, using orderID for query");  //$NON-NLS-1$
            query.setParameter("orderID",  //$NON-NLS-1$
                               getOrderID());
        } else {
            SLF4JLoggerProxy.debug(ExecutionReportSummary.class,
                                   "Using origOrderID {} for query",  //$NON-NLS-1$
                                   getOrigOrderID());
            query.setParameter("orderID",  //$NON-NLS-1$
                               getOrigOrderID());
        }
        List<?> list = query.getResultList();
        if(list.isEmpty()) {
            SLF4JLoggerProxy.debug(ExecutionReportSummary.class,
                                   "No other orders match this orderID - this must be the first in the order chain");  //$NON-NLS-1$
            // this is the first order in this chain
            setRootID(getOrderID());
        } else {
            OrderID rootID = (OrderID)list.get(0);
            SLF4JLoggerProxy.debug(ExecutionReportSummary.class,
                                   "Using {} for rootID",  //$NON-NLS-1$
                                   rootID);
            setRootID(rootID);
        }
    }
    
    /* (non-Javadoc)
     * @see org.marketcetera.persist.EntityBase#postSaveLocal(javax.persistence.EntityManager, org.marketcetera.persist.EntityBase, org.marketcetera.persist.PersistContext)
     */
    @Override
    protected void postSaveLocal(EntityManager inEntityManager,
                                 EntityBase inMerged,
                                 PersistContext inContext)
            throws PersistenceException
    {
        super.postSaveLocal(inEntityManager,
                            inMerged,
                            inContext);
        // CD 27-Jul-2013 MATP-350
        // mark all other orders of this family as closed
        Query query = inEntityManager.createNamedQuery("setIsOpen"); //$NON-NLS-1$
        ExecutionReportSummary summaryReport = (ExecutionReportSummary)inMerged;
        query.setParameter("Id",summaryReport.getId()).setParameter("rootID",summaryReport.getRootID()).executeUpdate();
    }
    
    @OneToOne(optional = false)
    PersistentReport getReport() {
        return mReport;
    }
    
    private void setReport(PersistentReport inReport) {
        mReport = inReport;
    }

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name="value",
                    column = @Column(name = "rootID", nullable = false))})
    @Column(nullable = false)
    OrderID getRootID() {
        return mRootID;
    }

    private void setRootID(OrderID inRootID) {
        mRootID = inRootID;
    }

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name="value",
                    column = @Column(name = "orderID", nullable = false))})
    OrderID getOrderID() {
        return mOrderID;
    }

    @SuppressWarnings("unused")
    private void setOrderID(OrderID inOrderID) {
        mOrderID = inOrderID;
    }

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name="value",
                    column = @Column(name = "brokerID", nullable = false))})
    BrokerID getBrokerID() {
        return mBrokerID;
    }

    @SuppressWarnings("unused")
    private void setBrokerID(BrokerID inBrokerID) {
        mBrokerID= inBrokerID;
    }
    
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name="value",
                    column = @Column(name = "origOrderID"))})
    OrderID getOrigOrderID() {
        return mOrigOrderID;
    }

    @SuppressWarnings("unused")
    private void setOrigOrderID(OrderID inOrigOrderID) {
        mOrigOrderID = inOrigOrderID;
    }
    
    @Column(nullable = false)
    String getSecurityType() {
        return mSecurityType;
    }

    @SuppressWarnings("unused")
    private void setSecurityType(String inSecurityType) {
        mSecurityType = inSecurityType;
    }
    
    @Column(nullable = false)
    String getFullSymbol() {
        return mFullSymbol;
    }

    @SuppressWarnings("unused")
    private void setFullSymbol(String inFullSymbol) {
    	mFullSymbol = inFullSymbol;
    }

    String getAccount() {
        return mAccount;
    }

    @SuppressWarnings("unused")
    private void setAccount(String inAccount) {
        mAccount = inAccount;
    }

    @Column(nullable = false)
    Side getSide() {
        return mSide;
    }

    @SuppressWarnings("unused")
    private void setSide(Side inSide) {
        mSide = inSide;
    }

    @Column(precision = DECIMAL_PRECISION, scale = DECIMAL_SCALE, nullable = false)
    BigDecimal getCumQuantity() {
        return mCumQuantity;
    }

    @SuppressWarnings("unused")
    private void setCumQuantity(BigDecimal inCumQuantity) {
        mCumQuantity = inCumQuantity;
    }

    @Column(precision = DECIMAL_PRECISION, scale = DECIMAL_SCALE, nullable = false)
    BigDecimal getAvgPrice() {
        return mAvgPrice;
    }

    @SuppressWarnings("unused")
    private void setAvgPrice(BigDecimal inAvgPrice) {
        mAvgPrice = inAvgPrice;
    }

    @Column(precision = DECIMAL_PRECISION, scale = DECIMAL_SCALE)
    BigDecimal getLastQuantity() {
        return mLastQuantity;
    }

    @SuppressWarnings("unused")
    private void setLastQuantity(BigDecimal inLastQuantity) {
        mLastQuantity = inLastQuantity;
    }

    @Column(precision = DECIMAL_PRECISION, scale = DECIMAL_SCALE)
    BigDecimal getLastPrice() {
        return mLastPrice;
    }

    @SuppressWarnings("unused")
    private void setLastPrice(BigDecimal inLastPrice) {
        mLastPrice = inLastPrice;
    }

    @Column(nullable = false)
    OrderStatus getOrderStatus() {
        return mOrderStatus;
    }

    @SuppressWarnings("unused")
    private void setOrderStatus(OrderStatus inOrderStatus) {
        mOrderStatus = inOrderStatus;
    }

    @Column(nullable = false)
    Date getSendingTime() {
        return mSendingTime;
    }

    @SuppressWarnings("unused")
    private void setSendingTime(Date inSendingTime) {
        mSendingTime = inSendingTime;
    }

    @ManyToOne
    public SimpleUser getActor() {
        return mActor;
    }

    @SuppressWarnings("unused")
    private void setActor(SimpleUser inActor) {
    	mActor = inActor;
    }
    
    @Transient
    UserID getActorID() {
        if (getActor()==null) {
            return null;
        }
        return getActor().getUserID();
    }

    /**
     * Gets the is open value.
     *
     * @return a <code>boolean</code> value
     */
    @Column
    @Type(type = "org.hibernate.type.NumericBooleanType")//OR Fix
    public boolean getIsOpen()
    {
        return mIsOpen;
    }
    
    /**
     * Sets the is open value.
     *
     * @param a <code>boolean</code> value
     */
    public void setIsOpen(boolean inIsOpen)
    {
        mIsOpen = inIsOpen;
    }

    /**
     * Defined to get JPA to work.
     */
    ExecutionReportSummary() 
    {
    }

    private OrderID mRootID;
    private OrderID mOrderID;
    private OrderID mOrigOrderID;
    private BrokerID mBrokerID;
    private String mSecurityType;
    private String mFullSymbol;
    private String mAccount;
    private Side mSide;
    private BigDecimal mCumQuantity;
    private BigDecimal mAvgPrice;
    private BigDecimal mLastQuantity;
    private BigDecimal mLastPrice;
    private OrderStatus mOrderStatus;
    private Date mSendingTime;
    private SimpleUser mActor; 
    private PersistentReport mReport;
    private boolean mIsOpen;

    /**
     * The scale used for storing all decimal values.
     */
    static final int DECIMAL_SCALE = 7;
    /**
     * The precision used for storing all decimal values.
     */
    static final int DECIMAL_PRECISION = 17;
    private static final long serialVersionUID = -6939295144839290006L;
}
