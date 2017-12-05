package org.marketcetera.server.ws.history;

import java.util.List;
import java.util.Set;

import javax.persistence.*;

import org.apache.commons.lang.Validate;
import org.marketcetera.event.info.InstrumentInfo;
import org.marketcetera.event.info.InstrumentInfoResolver;
import org.marketcetera.persist.EntityBase;
import org.marketcetera.persist.PersistContext;
import org.marketcetera.persist.PersistenceException;
import org.marketcetera.persist.Transaction;
import org.marketcetera.symbol.InstrumentSymbolResolver;
import org.marketcetera.trade.*;
import org.marketcetera.util.misc.ClassVersion;
import org.marketcetera.util.ws.wrappers.RemoteException;

import com.google.common.collect.Sets;

/* $License$ */
/**
 * A persistent instrument. The instruments can be retrieved filtered / sorted by the brokerID
 * security type or instrument full symbol.
 */
@ClassVersion("$Id: SimpleInstrument.java $")
@Entity
@Table(name = "simpleinfo")
@SqlResultSetMappings({
    @SqlResultSetMapping(name = "resultInfo",
            columns = {@ColumnResult(name = "fullInfo")}),
    @SqlResultSetMapping(name = "resultInstruments",
		    columns = {
		    	@ColumnResult(name = "securityType"),
		        @ColumnResult(name = "fullSymbol")
		            })
})
@NamedNativeQueries({
    @NamedNativeQuery(name = "getInfo",query = "select e.fullInfo as fullInfo from simpleinfo e "+
    		"where e.fullSymbol = :fullSymbol and e.securityType = :securityType and e.brokerID = :brokerID",
            resultSetMapping = "resultInfo"),
	@NamedNativeQuery(name="setInfo",query="select * from simpleinfo e where "+
			"e.fullSymbol = :fullSymbol and e.securityType = :securityType and e.brokerID = :brokerID",
			resultClass=SimpleInstrument.class),
    @NamedNativeQuery(name = "deleteInfo",query = "delete from simpleinfo where " +
            "fullSymbol = :fullSymbol and securityType = :securityType and brokerID = :brokerID",
            resultClass=SimpleInstrument.class),
    @NamedNativeQuery(name = "getInstruments",query = "select e.securityType as securityType, e.fullSymbol as fullSymbol "+
            "from simpleinfo e where e.brokerID = :brokerID",
            resultSetMapping = "resultInstruments")
        })
public class SimpleInstrument
        extends EntityBase
{		
    /**
     * Retrieves the given instrument info.
     *
     * @param brokerID a <code>BrokerID</code> value
	 * @param instrument a <code>Instrument</code> value
     * @throws PersistenceException if an error occurs retreiving the info
     */
	public static InstrumentInfo getInfo(final BrokerID brokerID, final Instrument instrument)
            throws PersistenceException
    {
        return executeRemote(new Transaction<InstrumentInfo>() {
            @Override
            public InstrumentInfo execute(EntityManager em,
                                            PersistContext context)
                    throws PersistenceException
            {
                Query query = em.createNamedQuery("getInfo"); //$NON-NLS-1$
                query.setParameter("fullSymbol", instrument.getFullSymbol()); //$NON-NLS-1$
                query.setParameter("securityType", instrument.getSecurityType().getValue()); //$NON-NLS-1$
                query.setParameter("brokerID", brokerID.getValue()); //$NON-NLS-1$
                
                String inFullInfo = (String)query.getSingleResult();
                if(inFullInfo == null){
                	return null;
                }
                
                SecurityType inSecurityType = instrument.getSecurityType();
                InstrumentInfo instrumentInfo = InstrumentInfoResolver.resolveInfo(inSecurityType, inFullInfo);
                
                return instrumentInfo;
            }
            private static final long serialVersionUID = 1L;
        },null);
    }
    
    /**
     * Saves the supplied informations to the database.
     *
     * @param brokerID a <code>BrokerID</code> value
	 * @param instrument a <code>Instrument</code> value
     * @param info a <code>InstrumentInfo</code> value
     * @throws RemoteException if the operation cannot b
     *
     * @throws PersistenceException if there were errors saving the
     * info to the database.
     */
	static void setInfo(BrokerID brokerID, Instrument instrument, InstrumentInfo info) 
			throws PersistenceException 
	{
		executeRemote(new Transaction<SimpleInstrument>() {
            @Override
            public SimpleInstrument execute(EntityManager inEntityManager,
                                                        PersistContext inContext)
                    throws PersistenceException
            {
                Query query = inEntityManager.createNamedQuery("setInfo");  //$NON-NLS-1$
                query.setParameter("fullSymbol", instrument.getFullSymbol()); //$NON-NLS-1$
                query.setParameter("securityType", instrument.getSecurityType().getValue()); //$NON-NLS-1$
                query.setParameter("brokerID", brokerID.getValue()); //$NON-NLS-1$
                
                SimpleInstrument sInstrument = null;
                try{
                	sInstrument =(SimpleInstrument)query.getSingleResult();
                	sInstrument.setFullInfo(info.getFullInfo());
                	sInstrument.saveRemote(inContext);
                }catch(NoResultException ignore){
        			sInstrument = new SimpleInstrument(brokerID, instrument, info);
        	        sInstrument.saveRemote(inContext);
                }
                
                return sInstrument;
            }
            private static final long serialVersionUID = 1L;
        },null);
    }
	
    /**
     * Deletes the given simple instrument.
     *
     * @param brokerID a <code>BrokerID</code> value
	 * @param instrument a <code>Instrument</code> value
     * @throws PersistenceException if an error occurs deleting the info
     */
    static void deleteInfo(final BrokerID brokerID, final Instrument instrument)
            throws PersistenceException
    {
        executeRemote(new Transaction<Integer>() {
            @Override
            public Integer execute(EntityManager em,
                                            PersistContext context)
                    throws PersistenceException
            {
                Query query = em.createNamedQuery("deleteInfo"); //$NON-NLS-1$
                query.setParameter("fullSymbol", instrument.getFullSymbol()); //$NON-NLS-1$
                query.setParameter("securityType", instrument.getSecurityType().getValue()); //$NON-NLS-1$
                query.setParameter("brokerID", brokerID.getValue()); //$NON-NLS-1$
                
                return query.executeUpdate();
            }
            private static final long serialVersionUID = 1L;
        },null);
    }

    /**
     * Retrieves the broker's instruments.
     *
     * @param brokerID a <code>BrokerID</code> value
     * @throws PersistenceException if an error occurs retrieving the instruments
     */
    public static Set<Instrument> getInstruments(final BrokerID brokerID)
            throws PersistenceException
    {
        return executeRemote(new Transaction<Set<Instrument>>() {
            @Override
            public Set<Instrument> execute(EntityManager em,
                                            PersistContext context)
                    throws PersistenceException
            {
                Query query = em.createNamedQuery("getInstruments"); //$NON-NLS-1$
                query.setParameter("brokerID", brokerID.getValue()); //$NON-NLS-1$
                
                Set<Instrument> set = Sets.newHashSet();
                List<?>list = query.getResultList();
                if(!list.isEmpty()) {
	                Object[] columns;
	                for(Object o: list) {
	                    columns = (Object[]) o;
	                    //4 columns
	                    if(columns.length > 1) {
	                        //first one is the securityType
	                        //second one is the fullSymbol
	                    	Instrument instrument = InstrumentSymbolResolver.resolveSymbol((String)columns[0], (String)columns[1]);                  	
	                    	set.add(instrument);
	                    }
	                }
                }
                return set;
            }
            private static final long serialVersionUID = 1L;
        },null);
    }
    
    /**
     * Creates an instance, given a simple instrument.
     *
     * @param brokerID a <code>BrokerID</code> value
	 * @param instrument a <code>Instrument</code> value
     * @param info a <code>InstrumentInfo</code> value
     *
     * @throws PersistenceException if there were errors creating the
     * instance.
     */
    SimpleInstrument(BrokerID brokerID, Instrument instrument, InstrumentInfo info)
        throws PersistenceException
    {
    	Validate.noNullElements(new Object[]{brokerID, instrument, info});
    	setSecurityType(instrument.getSecurityType().getValue());
    	setFullSymbol(instrument.getFullSymbol());
        setBrokerID(brokerID);
        setFullInfo(info.getFullInfo());
    }
    
    @Column(nullable = false)
    String getSecurityType() {
        return mSecurityType;
    }

    private void setSecurityType(String inSecurityType) {
        mSecurityType = inSecurityType;
    }
    
    @Column(nullable = false)
    String getFullSymbol() {
        return mFullSymbol;
    }
    
    private void setFullSymbol(String inFullSymbol) {
    	mFullSymbol = inFullSymbol;
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
    
    @Lob
    @Column(nullable = false)
    private String getFullInfo() {
        return mFullInfo;
    }
    
    private void setFullInfo(String inFullInfo) {
    	mFullInfo = inFullInfo;
    }

    /**
     * Declared to get JPA to work.
     */
    SimpleInstrument() {
    }

    /**
     * The entity name as is used in various JPQL Queries
     */
    static final String ENTITY_NAME = SimpleInstrument.class.getSimpleName();

    private String mSecurityType;
    private String mFullSymbol;
    private BrokerID mBrokerID;
    private String mFullInfo;

    private static final long serialVersionUID = 1;
}
