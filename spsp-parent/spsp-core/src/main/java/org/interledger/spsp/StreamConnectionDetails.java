package org.interledger.spsp;

import org.interledger.core.InterledgerAddress;
import org.interledger.core.SharedSecret;
import org.interledger.spsp.ImmutableStreamConnectionDetails.Builder;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value.Immutable;

/**
 * Contains information that can be used to initiate and process a STREAM connection. This information typically
 * conforms to the SPSP protocol.
 *
 * @see "https://github.com/interledger/rfcs/blob/master/0009-simple-payment-setup-protocol/0009-simple-payment-setup-protocol.md"
 */
@Immutable
@JsonSerialize(as = ImmutableStreamConnectionDetails.class)
@JsonDeserialize(as = ImmutableStreamConnectionDetails.class)
public interface StreamConnectionDetails {

  static Builder builder() {
    return ImmutableStreamConnectionDetails.builder();
  }

  /**
   * <p>The ultimate destination of an ILPv4 packet that is part of a STREAM.</p>
   *
   * <p>This address is generated such that the `shared_secret` can be re-derived from a Prepare packet's destination
   * and the same server secret. If the address is modified in any way, the server will not be able to re-derive the
   * secret and the packet will be rejected.
   *
   * @return An {@link InterledgerAddress} that can receive and fulfill ILPv4 packets.
   */
  @JsonProperty("destination_account")
  InterledgerAddress destinationAddress();

  /**
   * The shared secret to be used by a specific HTTP client in a STREAM. Should be shared only by the server that
   * generated and a specific HTTP client, and should therefore be different for each query response. Even though
   * clients SHOULD accept base64url encoded secrets, base64 encoded secrets are recommended.
   *
   * @return A {@link String}.
   */
  @JsonProperty("shared_secret")
  SharedSecret sharedSecret();

}
