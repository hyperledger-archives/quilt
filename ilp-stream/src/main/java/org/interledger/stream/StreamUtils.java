package org.interledger.stream;

import org.interledger.core.InterledgerCondition;
import org.interledger.core.InterledgerFulfillment;
import org.interledger.stream.crypto.Random;

import java.nio.charset.Charset;
import java.security.SignatureException;
import java.util.Objects;

import javax.crypto.Mac;
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
  private static final byte[] ILP_STREAM_FULFILLMENT = "ilp_stream_fulfillment".getBytes(Charset.forName("UTF-8"));

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
   * @return
   */
  public static final InterledgerFulfillment generatedFulfillableFulfillment(final byte[] sharedSecret,
      final byte[] data)
      throws SignatureException {
    Objects.requireNonNull(sharedSecret);

    // hmac_key = hmac_sha256(shared_secret, "ilp_stream_fulfillment");
    try {
      final Mac mac = Mac.getInstance(HMAC_SHA256_ALG_NAME);
      final SecretKey secretKey = new SecretKeySpec(sharedSecret, HMAC_SHA256_ALG_NAME);
      mac.init(secretKey);
      // Per the Javadoc, this `mac` is reset and available to generate another MAC from the same key, if desired, via
      // new calls to update and doFinal. (In order to reuse this Mac object with a different key, it must be
      // reinitialized via a call to init(Key) or init(Key, AlgorithmParameterSpec).
      final byte[] hmacKey = mac.doFinal(ILP_STREAM_FULFILLMENT);

      // fulfillment = hmac_sha256(hmac_key, data);
      final SecretKey hmacSecretKey = new SecretKeySpec(hmacKey, HMAC_SHA256_ALG_NAME);
      mac.init(hmacSecretKey);
      final byte[] fulfillment = mac.doFinal(data);

      return InterledgerFulfillment.of(fulfillment);
    } catch (Exception e) {
      throw new SignatureException("unable to sign", e);
    }
  }

}
