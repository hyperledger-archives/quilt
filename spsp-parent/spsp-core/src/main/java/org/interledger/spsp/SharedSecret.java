package org.interledger.spsp;

import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.base.Preconditions;
import org.immutables.value.Value;

import java.util.Base64;

/**
 * Wrapper for a SPSP shared secret (32-byte, base64 encoded).
 *
 * @see "https://github.com/interledger/rfcs/blob/master/0009-simple-payment-setup-protocol/0009-simple-payment-setup-protocol.md"
 */
@JsonSerialize(as = ImmutableSharedSecret.class)
@JsonDeserialize(as = ImmutableSharedSecret.class)
@Value.Immutable
public interface SharedSecret {

  static SharedSecret of(String base64EncodedKey) {
    return ImmutableSharedSecret.of(base64EncodedKey);
  }

  static SharedSecret of(byte[] key) {
    return ImmutableSharedSecret.of(Base64.getEncoder().withoutPadding().encodeToString(key));
  }

  /**
   * Base64 encoded key for sending over the wire.
   *
   * @return A {@link String} containing {@link #key()} in a Base64 encoding.
   */
  @Value.Parameter
  @JsonValue
  String value();

  /**
   * 32-byte key for encryption/decryption within Java.
   *
   * @return key bytes
   */
  @Value.Derived
  @Value.Auxiliary
  default byte[] key() {
    // both base64 and base64 url encoded keys are allowed by the rfc
    try {
      return Base64.getDecoder().decode(value());
    } catch (IllegalArgumentException e) {
      try {
        return Base64.getUrlDecoder().decode(value());
      } catch (IllegalArgumentException ex) {
        throw new IllegalArgumentException("SharedSecret must be base64 encoded");
      }
    }
  }

  @Value.Check
  default SharedSecret validate() {
    Preconditions.checkState(key().length == 32, "SharedSecret must be 32 bytes");
    return this;
  }

}
