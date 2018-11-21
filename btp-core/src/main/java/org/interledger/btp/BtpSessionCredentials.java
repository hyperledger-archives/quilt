package org.interledger.btp;

import org.immutables.value.Value;

import java.util.Optional;

/**
 * Authentication credentials for a {@link BtpSession}.
 */
@Value.Immutable
public interface BtpSessionCredentials {

  /**
   * <p>The `auth_username` for a BTP client. Enables multiple accounts over a single BTP WebSocket connection.</p>
   *
   * @return
   */
  Optional<String> getAuthUsername();

  /**
   * The <tt>auth_token</tt> for a BTP client, as specified in IL-RFC-23.
   *
   * @return
   *
   * @see "https://github.com/interledger/rfcs/blob/master/0023-bilateral-transfer-protocol
   *     /0023-bilateral-transfer-protocol.md#authentication"
   */
  String getAuthToken();

}
