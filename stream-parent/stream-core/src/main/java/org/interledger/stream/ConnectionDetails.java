package org.interledger.stream;

import org.interledger.core.Immutable;
import org.interledger.core.InterledgerAddress;

import java.util.UUID;

/**
 * Contains everything necessary to track a connection as defined by the STREAM protocol as defined in IL-RFC-29.
 */
@Immutable
public interface ConnectionDetails {

  UUID conectionId();

  /**
   * <p>When a client connects to a server, the client MUST communicate its ILP address to the server using a
   * ConnectionNewAddress frame.</p>
   *
   * <p>Either endpoint MAY change its ILP address at any point during a connection by sending a ConnectionNewAddress
   * frame.</p>
   *
   * @return The {@link InterledgerAddress} of the remote party in this connection.
   */
  InterledgerAddress remoteConnectionAddress();

  // TODO: Link?
}
