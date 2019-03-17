package org.interledger.ildcp;

import org.interledger.core.InterledgerProtocolException;

/**
 * Defines how to fetch IL-DCP data from a remote connector.
 */
public interface IldcpFetcher {

  /**
   * Send an IL-DCP request to a remote connector, and expect a response containing IL-DCP information to configure this
   * caller.
   *
   * @param ildcpRequest An instance of {@link IldcpRequest} that can be used to make a request to a parent Connector.
   *
   * @return An {@link IldcpResponse} fulfillment that contains everything necessary to respond to an IL-DCP response.
   *
   * @throws InterledgerProtocolException If the IL-DCP server could not complete the request.
   */
  IldcpResponse fetch(IldcpRequest ildcpRequest) throws InterledgerProtocolException;

}