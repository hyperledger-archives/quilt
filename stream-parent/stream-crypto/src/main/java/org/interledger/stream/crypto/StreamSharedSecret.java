package org.interledger.stream.crypto;

import org.immutables.value.Value;

import java.util.Base64;

/**
 * <p>Wrapper for a Shared Secret (32-byte, base64 encoded).</p>
 *
 * <p>Note that this immutable has no concept of JSON or other coding/encoding because it is only required by Stream as
 * a Java concept.</p>
 */
@Value.Immutable
public interface StreamSharedSecret {

  static StreamSharedSecret of(String base64EncodedKey) {
    return ImmutableStreamSharedSecret.of(base64EncodedKey);
  }

  static StreamSharedSecret of(byte[] key) {
    return ImmutableStreamSharedSecret.of(Base64.getEncoder().withoutPadding().encodeToString(key));
  }

  /**
   * Base64 encoded key for sending over the wire.
   *
   * @return A {@link String} containing {@link #key()} in a Base64 encoding.
   */
  @Value.Parameter
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

  /**
   * Precondition check method.
   *
   * @return A {@link StreamSharedSecret}.
   */
  @Value.Check
  default StreamSharedSecret validate() {
    if (key().length != 32) {
      throw new IllegalStateException("SharedSecret must be 32 bytes");
    }
    return this;
  }

}
