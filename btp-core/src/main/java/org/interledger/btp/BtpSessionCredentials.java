package org.interledger.btp;

import org.immutables.value.Value;

import java.util.Optional;

/**
 * Authentication credentials for a {@link BtpSession}.
 */
@Value.Immutable
public interface BtpSessionCredentials {

  /**
   * The <tt>auth_token</tt> for a BTP client, as specified in IL-RFC-23.
   *
   * @return
   *
   * @see "https://github.com/interledger/rfcs/blob/master/0023-bilateral-transfer-protocol
   *     /0023-bilateral-transfer-protocol.md#authentication"
   */
  String getAuthToken();

  /**
   * <p>An optionally-present name that identifies this BTP session.</p>
   *
   * <p>TODO: Look at JS implementation and determine what this is supposed to be!</p>
   *
   * @return
   */
  Optional<String> getName();

}
