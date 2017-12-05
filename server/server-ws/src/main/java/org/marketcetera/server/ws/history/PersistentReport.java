package org.marketcetera.server.ws.history;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.*;

import org.marketcetera.persist.EntityBase;
import org.marketcetera.persist.PersistContext;
import org.marketcetera.persist.PersistenceException;
import org.marketcetera.persist.Transaction;
import org.marketcetera.trade.*;
import org.marketcetera.util.log.I18NBoundMessage1P;
import org.marketcetera.util.log.SLF4JLoggerProxy;
import org.marketcetera.util.misc.ClassVersion;
import org.marketcetera.ws.server.security.SimpleUser;
import org.marketcetera.ws.server.security.SingleSimpleUserQuery;

import quickfix.InvalidMessage;
import quickfix.Message;

/**
 * A persistent report. The report instance is persisted to maintain
 * history. The reports can be retrieved filtered / sorted by the timestamp
 * of when they were sent.
 */
@ClassVersion("$Id: PersistentReport.java 16670 2013-08-28 19:49:06Z colin $")
@Entity
@Table(name = "reports")
@NamedQueries( { @NamedQuery(name="forOrderID",query="select e from PersistentReport e where e.orderID = :orderID"),
                 @NamedQuery(name="since",query="select e from PersistentReport e where e.sendingTime < :target") })
class PersistentReport
        extends EntityBase
{
	public enum ReportType {
	    /**
	     * Represents an execution report.
	     *
	     * @see org.marketcetera.trade.ExecutionReport
	     */
	    ExecutionReport,
	    /**
	     * Represents an order cancel reject report.
	     *
	     * @see org.marketcetera.trade.OrderCancelReject
	     */
	    CancelReject
	}
	
    /**
     * Saves the supplied report to the database.
     *
     * @param inReport The report to be saved.
     *
     * @throws PersistenceException if there were errors saving the
     * report to the database.
     */
    static void save(ReportBase inReport) throws PersistenceException {
        PersistentReport report = new PersistentReport(inReport);
        report.saveRemote(null);
    }
    
    /**
     * Deletes the given report.
     *
     * @param inReport a <code>ReportBase</code> value
     * @throws PersistenceException if an error occurs deleting the report
     */
    static void delete(final ReportBase inReport)
            throws PersistenceException
    {
        executeRemote(new Transaction<PersistentReport>() {
            @Override
            public PersistentReport execute(EntityManager em,
                                            PersistContext context)
                    throws PersistenceException
            {
                Query query = em.createNamedQuery("forOrderID"); //$NON-NLS-1$
                query.setParameter("orderID",
                                   inReport.getOrderID()); //$NON-NLS-1$
                List<?>list = query.getResultList();
                if(list.isEmpty()) {
                    return null;
                }
                PersistentReport report = (PersistentReport)list.get(0);
                ExecutionReportSummary.deleteReportsFor(report);
                report.deleteRemote(null);
                return report;
            }
            private static final long serialVersionUID = 1L;
        },null);
    }
    
    static int deleteBefore(final Date inPurgeDate)
            throws PersistenceException
    {
        return executeRemote(new Transaction<Integer>() {
            @SuppressWarnings("unchecked")
            @Override
            public Integer execute(EntityManager em,
                                   PersistContext context)
                    throws PersistenceException
            {
                Query query = em.createNamedQuery("since"); //$NON-NLS-1$
                query.setParameter("target", //$NON-NLS-1$
                                   inPurgeDate);
                List<PersistentReport> list = query.getResultList();
                if(list == null || list.isEmpty()) {
                    return 0;
                }
                // delete the Exec reports first
                ExecutionReportSummary.deleteReportsIn(list);
                List<Long> ids = new ArrayList<Long>();
                if(list != null) {
                    for(PersistentReport report : list) {
                        ids.add(report.getId());
                    }
                }
                return em.createNativeQuery("DELETE FROM reports WHERE id IN (:ids)").setParameter("ids",ids).executeUpdate();
            }
            private static final long serialVersionUID = 1L;
        },null);
    }
    
    /**
     * Returns the actor ID associated with the report with given
     * order ID.
     *
     * @param orderID The order ID.
     *
     * @return The actor ID. If no report with the given order ID
     * exists, null is returned, and no exception is thrown.
     *
     * @throws PersistenceException if there were errors accessing the
     * report.
     */
    static UserID getActorID
        (final OrderID orderID)
        throws PersistenceException 
    {
        return executeRemote(new Transaction<UserID>() {
            private static final long serialVersionUID=1L;

            @Override
            public UserID execute
                (EntityManager em,
                 PersistContext context)
            {
                Query query=em.createNamedQuery("forOrderID"); //$NON-NLS-1$
                query.setParameter("orderID",orderID); //$NON-NLS-1$
                List<?> list=query.getResultList();
                if (list.isEmpty()) {
                    return null;
                }
                PersistentReport report=(PersistentReport)(list.get(0));
                return report.getActorID();
            }
        },null);
    }

    /**
     * Creates an instance, given a report.
     *
     * @param inReport the report instance.
     *
     * @throws PersistenceException if there were errors creating the
     * instance.
     */
    PersistentReport(ReportBase inReport)
        throws PersistenceException
    {
        mReportBase = inReport;
        setBrokerID(inReport.getBrokerID());
        setSendingTime(inReport.getSendingTime());
        if(inReport instanceof HasFIXMessage) {
            setFixMessage(((HasFIXMessage) inReport).getMessage().toString());
        }
        setOriginator(inReport.getOriginator());
        setOrderID(inReport.getOrderID());
        setReportID(inReport.getReportID());
        if (inReport.getActorID()!=null) {
            setActor(new SingleSimpleUserQuery
                     (inReport.getActorID().getValue()).fetch());
        }
        if(inReport instanceof ExecutionReport) {
            mReportType = ReportType.ExecutionReport;
        } else if (inReport instanceof OrderCancelReject) {
            mReportType = ReportType.CancelReject;
        } else {
            //You added new report types but forgot to update the code
            //to persist them.
            throw new IllegalArgumentException();
        }
    }

    /**
     * Converts the report into a system report instance.
     *
     * @return the system report instance.
     *
     * @throws PersistenceException if there were errors converting
     * the message from its persistent representation to system report
     * instance.
     */
    ReportBase toReport() throws PersistenceException {
        ReportBase returnValue = null;
        String fixMsgString = null;
        try {
            fixMsgString = getFixMessage();
            Message fixMessage;
            try {
            	fixMessage = new Message(fixMsgString);
			} catch (InvalidMessage e) {
				fixMessage =  new Message(fixMsgString,false); // log the validation exception and create message without validation.
				SLF4JLoggerProxy.warn(PersistentReport.class, e);    
			}
            switch(mReportType) {
                case ExecutionReport:
                    returnValue =  Factory.getInstance().createExecutionReport(
                            fixMessage, getBrokerID(),
                            getOriginator(), getActorID());
                    break;
                case CancelReject:
                    returnValue =  Factory.getInstance().createOrderCancelReject(
                            fixMessage, getBrokerID(), getOriginator(), getActorID());
                    break;
                default:
                    //You added new report types but forgot to update the code
                    //to persist them.
                    throw new IllegalArgumentException();
            }
            ReportBaseImpl.assignReportID((ReportBaseImpl)returnValue,
                                          getReportID());
            return returnValue;
        } catch (InvalidMessage | MessageCreationException e) {
            throw new PersistenceException(e, new I18NBoundMessage1P(
                    Messages.ERROR_RECONSTITUTE_FIX_MSG, fixMsgString));
        }
    }
    
    /* (non-Javadoc)
     * @see org.marketcetera.persist.EntityBase#postSaveLocal(javax.persistence.EntityManager, org.marketcetera.persist.EntityBase, org.marketcetera.persist.PersistContext)
     */
    @Override
    protected void postSaveLocal(EntityManager em,
                                 EntityBase merged,
                                 PersistContext context)
            throws PersistenceException {
        super.postSaveLocal(em, merged, context);
        PersistentReport mergedReport = (PersistentReport) merged;
        //Save the summary if the report is an execution report.
        if(mergedReport.getReportType() == ReportType.ExecutionReport) {
            new ExecutionReportSummary(
                    (ExecutionReport) mReportBase,
                    mergedReport).localSave(em, context);
        }
    }

    private Originator getOriginator() {
        return mOriginator;
    }

    private void setOriginator(Originator inOriginator) {
        mOriginator = inOriginator;
    }

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name="value",
                    column = @Column(name = "orderID", nullable = false))})
    OrderID getOrderID() {
        return mOrderID;
    }

    private void setOrderID(OrderID inOrderID) {
        mOrderID = inOrderID;
    }

    @ManyToOne
    public SimpleUser getActor() {
        return mActor;
    }

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

    @Transient
    BrokerID getBrokerID() {
        return mBrokerID;
    }

    private void setBrokerID(BrokerID inBrokerID) {
        mBrokerID = inBrokerID;
    }
    
    @Column(name = "brokerID")
    private String getBrokerIDAsString() {
        return getBrokerID() == null
                ? null
                : getBrokerID().toString();
    }
    
    @SuppressWarnings("unused")
    private void setBrokerIDAsString(String inValue) {
        setBrokerID(inValue == null
                ? null
                : new BrokerID(inValue));
    }

    @Transient
    ReportID getReportID() {
        return mReportID;
    }

    private void setReportID(ReportID inReportID) {
        mReportID = inReportID;
    }
    
    @Column(name = "report_id", nullable = false)
    private long getReportIDAsLong() {
        return getReportID().longValue();
    }
    
    @SuppressWarnings("unused")
    private void setReportIDAsLong(long inValue) {
        setReportID(new ReportID(inValue));
    }

    @Lob
    @Column(nullable = false)
    private String getFixMessage() {
        return mFixMessage;
    }

    private void setFixMessage(String inFIXMessage) {
        mFixMessage = inFIXMessage;
    }

    @Column(nullable = false)
    private Date getSendingTime() {
        return mSendingTime;
    }

    private void setSendingTime(Date inSendingTime) {
        mSendingTime = inSendingTime;
    }

    @Column(nullable = false)
    private ReportType getReportType() {
        return mReportType;
    }

    @SuppressWarnings("unused")
    private void setReportType(ReportType inReportType) {
        mReportType = inReportType;
    }

    /**
     * Declared to get JPA to work.
     */
    PersistentReport() {
    }

    private Originator mOriginator;
    private OrderID mOrderID;
    private SimpleUser mActor; 
    private BrokerID mBrokerID;
    private ReportID mReportID;
    private String mFixMessage;
    private Date mSendingTime;
    private ReportType mReportType;
    private ReportBase mReportBase;
    
    /**
     * The attribute sending time used in JPQL queries
     */
    static final String ATTRIBUTE_SENDING_TIME = "sendingTime";  //$NON-NLS-1$
    /**
     * The attribute actor used in JPQL queries
     */
    static final String ATTRIBUTE_ACTOR = "actor";  //$NON-NLS-1$
    /**
     * The entity name as is used in various JPQL Queries
     */
    static final String ENTITY_NAME = PersistentReport.class.getSimpleName();
    private static final long serialVersionUID = 1;
}
