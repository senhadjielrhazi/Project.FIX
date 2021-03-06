package org.marketcetera.server.sa.security;

import org.apache.commons.lang.ObjectUtils;
import org.marketcetera.client.ws.WSClientImpl;
import org.marketcetera.core.ApplicationVersion;
import org.marketcetera.core.Util;
import org.marketcetera.core.VersionInfo;
import org.marketcetera.server.sa.Messages;
import org.marketcetera.util.except.I18NException;
import org.marketcetera.util.log.I18NBoundMessage3P;
import org.marketcetera.util.misc.ClassVersion;
import org.marketcetera.util.ws.stateful.Authenticator;
import org.marketcetera.util.ws.stateless.StatelessClientContext;

/**
 * Provides authentication services.
 */
@ClassVersion("$Id: SAAuthenticator.java 16901 2014-05-11 16:14:11Z colin $")
public class SAAuthenticator
        implements Authenticator
{
    /* (non-Javadoc)
     * @see org.marketcetera.util.ws.stateful.Authenticator#shouldAllow(org.marketcetera.util.ws.stateless.StatelessClientContext, java.lang.String, char[])
     */
    @Override
    public boolean shouldAllow(StatelessClientContext inContext,
                               String inUser,
                               char[] inPassword)
            throws I18NException
    {
        // Verify client version
        VersionInfo serverVersion = ApplicationVersion.getVersion();
        VersionInfo clientVersion = VersionInfo.DEFAULT_VERSION;
        String version = Util.getVersion(inContext.getAppId());
        if(version != null) {
            clientVersion =  new VersionInfo(Util.getVersion(inContext.getAppId()));
        }
        if(!compatibleVersions(clientVersion,
                               serverVersion)) {
            throw new I18NException(new I18NBoundMessage3P(Messages.VERSION_MISMATCH,
                                                           clientVersion,
                                                           serverVersion,
                                                           inUser));
        }
        // Use client to carry out authentication.
        return WSClientImpl.getInstance().isCredentialsMatch(inUser,
                                                              inPassword);
    }
    /**
     * Checks for compatibility between the given client and server
     * versions.
     *
     * @param clientVersion The client version.
     * @param serverVersion The server version.
     * @return True if the two versions are compatible.
     */
    private static boolean compatibleVersions(VersionInfo clientVersion,
                                              VersionInfo serverVersion)
    {
        // If the server's version is unknown, any client is allowed.
        return (VersionInfo.DEFAULT_VERSION.equals(serverVersion) || VersionInfo.DEFAULT_VERSION.equals(clientVersion) || ObjectUtils.equals(clientVersion, serverVersion));
    }
}
