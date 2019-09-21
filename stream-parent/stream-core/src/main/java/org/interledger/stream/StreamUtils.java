package org.interledger.stream;

import org.interledger.core.InterledgerCondition;
import org.interledger.core.InterledgerFulfillment;
import org.interledger.stream.crypto.Random;

import com.google.common.hash.Hashing;
import com.google.common.primitives.UnsignedLong;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**
 * Utilities to support the STREAM protocol.
 */
public class StreamUtils {

  /**
   * The string "ilp_stream_fulfillment" is encoded as UTF-8 or ASCII (the byte representation is the same with both
   * encodings).
   */
  private static final byte[] ILP_STREAM_FULFILLMENT = "ilp_stream_fulfillment".getBytes(StandardCharsets.UTF_8);

  private static final String HMAC_SHA256_ALG_NAME = "HmacSHA256";

  /**
   * If the sender does not want the receiver to be able to fulfill the payment (as for an informational quote), they
   * can generate an unfulfillable random condition.
   *
   * @return A {@link InterledgerCondition} that is not fulfillable.
   */
  public static InterledgerCondition unfulfillableCondition() {
    return InterledgerCondition.of(Random.randBytes(32));
  }

  /**
   * <p>If the sender _does_ want the receiver to be able to fulfill the condition, the condition MUST be generated
   * from a fulfillment in the following manner: First, the shared_secret is the cryptographic seed exchanged during
   * Setup. The string "ilp_stream_fulfillment" is encoded as UTF-8 or ASCII (the byte representation is the same with
   * both encodings). Finally, the data is the encrypted STREAM packet.</p>
   *
   * @param sharedSecret The cryptographic seed exchanged during STREAM Setup.
   * @param data         The encrypted STREAM packet in ASN.1 OER bytes.
   *
   * @return An {@link InterledgerFulfillment} that can be used to prove a payment.
   */
  public static final InterledgerFulfillment generatedFulfillableFulfillment(
      final byte[] sharedSecret, final byte[] data
  ) {
    Objects.requireNonNull(sharedSecret);
    Objects.requireNonNull(data);

    // hmac_key = hmac_sha256(shared_secret, "ilp_stream_fulfillment");
    final SecretKey secretKey = new SecretKeySpec(sharedSecret, HMAC_SHA256_ALG_NAME);
    final byte[] hmacKey = Hashing.hmacSha256(secretKey).hashBytes(ILP_STREAM_FULFILLMENT).asBytes();

    // fulfillment = hmac_sha256(hmac_key, data);
    final SecretKey hmacSecretKey = new SecretKeySpec(hmacKey, HMAC_SHA256_ALG_NAME);
    final byte[] fulfillmentBytes = Hashing.hmacSha256(hmacSecretKey).hashBytes(data).asBytes();

    return InterledgerFulfillment.of(fulfillmentBytes);
  }

  /**
   * Compute the smaller of {@code value1} and {@code value2}.
   *
   * @param value1 The first value.
   * @param value2 The second value.
   *
   * @return The smaller of the two supplied values.
   */
  public static UnsignedLong min(final UnsignedLong value1, final UnsignedLong value2) {
    Objects.requireNonNull(value1);
    Objects.requireNonNull(value2);

    if (value1.compareTo(value2) < 0) {
      return value1;
    } else {
      return value2;
    }
  }

  /**
   * Compute the larger of {@code value1} and {@code value2}.
   *
   * @param value1 The first value.
   * @param value2 The second value.
   *
   * @return The smaller of the two supplied values.
   */
  public static UnsignedLong max(final UnsignedLong value1, final UnsignedLong value2) {
    Objects.requireNonNull(value1);
    Objects.requireNonNull(value2);

    if (value1.compareTo(value2) > 0) {
      return value1;
    } else {
      return value2;
    }
  }
}
