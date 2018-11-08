package org.interledger.ildcp;

import org.interledger.core.InterledgerProtocolException;

/**
 * Defines how to server IL-DCP data from to remote child peers.
 */
public interface IldcpServer {

  IldcpResponse serve(IldcpRequest ildcpRequest) throws InterledgerProtocolException;
}
