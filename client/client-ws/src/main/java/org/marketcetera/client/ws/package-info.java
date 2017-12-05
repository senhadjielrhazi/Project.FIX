/**
 * The client for communicating with remote instances of a web service.
 * <p>
 * All the communication with remote service is accomplished via
 * {@link org.marketcetera.client.ws.WSClient} interface. Instances of
 * this client can be created via
 * {@link org.marketcetera.client.ws.WSClientFactory#create(org.marketcetera.ws.client.ClientParameters)}.
 * <p>
 * The client provides facilitites to data base queries onto
 * the remote service and receive the data/trade emitted by brokers.
 * See {@link org.marketcetera.client.ws.WSClient} documentation for details.
 */
@XmlSchema(namespace = "http://marketcetera.org/types/client")
package org.marketcetera.client.ws;

import javax.xml.bind.annotation.XmlSchema;