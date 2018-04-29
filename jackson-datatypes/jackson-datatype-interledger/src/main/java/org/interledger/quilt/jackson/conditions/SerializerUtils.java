package org.interledger.quilt.jackson.conditions;

import org.interledger.core.Condition;
import org.interledger.core.Fulfillment;

import java.util.Base64;
import java.util.Objects;

/**
 * Utility helpers used by various portions of this library.
 */
public class SerializerUtils {

  /**
   * Helper method to encode a {@link Condition} using the supplied Base64 encoder, which might be
   * Base64 or Base64Url, with or without padding.
   *
   * @param encoder   A {@link Base64.Encoder} to encode with.
   * @param condition A {@link Condition} to encode into Base64 using the supplied encoder.
   *
   * @return The base64-encoded version of {@code condition}.
   */
  public static String encodeBase64(final Base64.Encoder encoder, final Condition condition) {
    Objects.requireNonNull(encoder);
    Objects.requireNonNull(condition);

    return encoder.encodeToString(condition.getHash());
  }

  /**
   * Helper method to encode a {@link Fulfillment} using the supplied Base64 encoder, which might be
   * Base64 or Base64Url, with or without padding.
   *
   * @param encoder     A {@link Base64.Encoder} to encode with.
   * @param fulfillment A {@link Fulfillment} to encode into Base64 using the supplied encoder.
   *
   * @return The base64-encoded version of {@code fulfillment}.
   */
  public static String encodeBase64(final Base64.Encoder encoder, final Fulfillment fulfillment) {
    Objects.requireNonNull(encoder);
    Objects.requireNonNull(fulfillment);

    return encoder.encodeToString(fulfillment.getPreimage());
  }
}
