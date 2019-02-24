package org.interledger.btp;

import org.interledger.btp.ImmutableBtpSessionCredentials.Builder;

import org.immutables.value.Value;
import org.immutables.value.Value.Redacted;

import java.util.Optional;

/**
 * Authentication credentials for a {@link BtpSession}.
 */
@Value.Immutable
public interface BtpSessionCredentials {

  static Builder builder() {
    return ImmutableBtpSessionCredentials.builder();
  }

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
  @Redacted
  String getAuthToken();

}
