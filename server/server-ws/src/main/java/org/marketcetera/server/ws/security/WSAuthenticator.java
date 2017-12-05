package org.marketcetera.server.ws.security;

import org.apache.commons.lang.ObjectUtils;
import org.marketcetera.core.ApplicationVersion;
import org.marketcetera.core.Util;
import org.marketcetera.persist.NoResultException;
import org.marketcetera.persist.PersistenceException;
import org.marketcetera.util.log.I18NBoundMessage2P;
import org.marketcetera.util.log.I18NBoundMessage3P;
import org.marketcetera.util.misc.ClassVersion;
import org.marketcetera.util.ws.stateful.Authenticator;
import org.marketcetera.util.ws.stateless.StatelessClientContext;
import org.marketcetera.ws.client.ClientVersion;
import org.marketcetera.ws.client.IncompatibilityException;
import org.marketcetera.ws.server.security.SimpleUser;
import org.marketcetera.ws.server.security.SingleSimpleUserQuery;

/**
 * A session authenticator that uses the database for authentication. 
 */
@ClassVersion("$Id: WSAuthenticator.java 16154 2012-07-14 16:34:05Z colin $")
public class WSAuthenticator
    implements Authenticator
{
    // CLASS METHODS.
    /**
     * Checks for compatibility between the given client and server
     * versions.
     *
     * @param clientVersion The client version.
     * @param serverVersion The server version.
     *
     * @return True if the two versions are compatible.
     */

    static boolean compatibleVersions
        (String clientVersion,
         String serverVersion)
    {
        // If the server's version is unknown, any client is allowed.
        return (ApplicationVersion.DEFAULT_VERSION.equals(serverVersion) ||
                ObjectUtils.equals(clientVersion,serverVersion));
    }

    /**
     * Checks if a client with the supplied name is compatible with
     * this server.
     *
     * @param clientName The client name.
     *
     * @return True if the client is compatible with this server.
     */
    static boolean compatibleApp
        (String clientName)
    {
        return ClientVersion.APP_ID_NAME.equals(clientName);
    }

    // Authenticator.
    @Override
    public boolean shouldAllow
        (StatelessClientContext context,
         String user,
         char[] password)
        throws IncompatibilityException
    {
        String serverVersion=ApplicationVersion.getVersion().getVersionInfo();
        String clientName=Util.getName(context.getAppId());
        String clientVersion=Util.getVersion(context.getAppId());
        if (!compatibleApp(clientName)) {
            throw new IncompatibilityException
                (new I18NBoundMessage2P(Messages.APP_MISMATCH,
                                        clientName,user),
                 serverVersion);
        }
        if (!compatibleVersions(clientVersion,serverVersion)) {
            throw new IncompatibilityException
                (new I18NBoundMessage3P(Messages.VERSION_MISMATCH,
                                        clientVersion,serverVersion,user),
                 serverVersion);
        }
        try {
            SimpleUser u=new SingleSimpleUserQuery(user).fetch();
            if (!u.isActive()) {
                Messages.BAD_CREDENTIALS.warn(this,user);
                return false;
            }
            u.validatePassword(password);
        } catch (NoResultException ex) {
            Messages.BAD_CREDENTIALS.warn(this,ex,user);
            return false;
        } catch (PersistenceException ex) {
            Messages.BAD_CREDENTIALS.warn(this,ex,user);
            return false;
        }
        return true;
    }
}
