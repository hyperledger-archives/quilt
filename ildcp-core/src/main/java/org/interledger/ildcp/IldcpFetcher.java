package org.interledger.ildcp;

import org.interledger.core.InterledgerProtocolException;

import java.util.concurrent.CompletableFuture;

/**
 * Defines how to fetch IL-DCP data from a remote connector.
 */
public interface IldcpFetcher {

  /**
   * Send an IL-DCP request to a remote connector, and expect a response containing IL-DCP
   * information to configure this caller.
   *
   * @param ildcpRequest An instance of {@link IldcpRequest} that can be used to make a request to a
   *                     parent Connector.
   *
   * @return A {@link CompletableFuture} that resolves to the IL-DCP response.
   *
   * @throws InterledgerProtocolException If the IL-DCP server could not complete the request.
   */
  IldcpResponse fetch(IldcpRequest ildcpRequest) throws InterledgerProtocolException;

}