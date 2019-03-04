package org.interledger.ildcp;

import org.interledger.core.InterledgerProtocolException;

/**
 * Defines how to server IL-DCP data from to remote child peers.
 */
public interface IldcpServer {

  /**
   * Request IL-DCP configuration information from a server.
   *
   * @param ildcpRequest A {@link IldcpRequest} with all information necessary for the server to construct a response.
   *
   * @return A {@link IldcpResponse} containing all information to allow an ILP node to act as a child of the server.
   */
  IldcpResponse serve(IldcpRequest ildcpRequest) throws InterledgerProtocolException;
}
