package org.interledger.quilt.jackson.cryptoconditions;

import org.interledger.cryptoconditions.Condition;
import org.interledger.cryptoconditions.CryptoConditionWriter;
import org.interledger.cryptoconditions.Fulfillment;
import org.interledger.cryptoconditions.der.DerEncodingException;

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
   * @throws RuntimeException if a {@link DerEncodingException} is encountered.
   */
  public static String encodeBase64(final Base64.Encoder encoder, final Condition condition) {
    Objects.requireNonNull(encoder);
    Objects.requireNonNull(condition);

    try {
      return encoder.encodeToString(CryptoConditionWriter.writeCondition(condition));
    } catch (DerEncodingException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Helper method to encode a {@link Fulfillment} using the supplied Base64 encoder, which might be
   * Base64 or Base64Url, with or without padding.
   *
   * @param encoder     A {@link Base64.Encoder} to encode with.
   * @param fulfillment A {@link Fulfillment} to encode into Base64 using the supplied encoder.
   *
   * @throws RuntimeException if a {@link DerEncodingException} is encountered.
   */
  public static String encodeBase64(final Base64.Encoder encoder, final Fulfillment fulfillment) {
    Objects.requireNonNull(encoder);
    Objects.requireNonNull(fulfillment);

    try {
      return encoder.encodeToString(CryptoConditionWriter.writeFulfillment(fulfillment));
    } catch (DerEncodingException e) {
      throw new RuntimeException(e);
    }
  }
}
