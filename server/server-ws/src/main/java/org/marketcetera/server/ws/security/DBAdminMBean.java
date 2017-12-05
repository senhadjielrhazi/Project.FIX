package org.marketcetera.server.ws.security;

import org.marketcetera.util.misc.ClassVersion;

/**
 * MBean interface for DB operations.
 * @author toli
 * @version $Id: DBAdminMBean.java 16154 2012-07-14 16:34:05Z colin $
 */

@ClassVersion("$Id: DBAdminMBean.java 16154 2012-07-14 16:34:05Z colin $")
public interface DBAdminMBean {

    /** Sends a {@link quickfix.fix44.UserRequest} message containing the password reset message */
    public void resetTradePassword(String broker, String oldPassword, String newPassword);
    
    /** Sends a {@link quickfix.fix44.UserRequest} message containing the password reset message */
    public void resetDataPassword(String broker, String oldPassword, String newPassword);
    
    /**
     * Syncs up the in-memory sessions to reflect the current user
     * definitions in the database.
     */

    public void syncSessions();
}
