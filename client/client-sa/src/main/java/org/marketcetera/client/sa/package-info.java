/**
 * The client for communicating with remote instances of a strategy agent.
 * <p>
 * All the communication with remote strategy agents is accomplished via
 * {@link org.marketcetera.client.sa.SAClient} interface. Instances of
 * this client can be created via
 * {@link org.marketcetera.client.sa.SAClientFactory#create(org.marketcetera.ws.client.ClientParameters)}.
 * <p>
 * The client provides facilitites to deploy / manage strategies onto
 * the remote strategy agent and receive the data emitted by them.
 * See {@link org.marketcetera.client.sa.SAClient} documentation for details.
 */
@XmlSchema(namespace = "http://marketcetera.org/types/client")
package org.marketcetera.client.sa;

import javax.xml.bind.annotation.XmlSchema;