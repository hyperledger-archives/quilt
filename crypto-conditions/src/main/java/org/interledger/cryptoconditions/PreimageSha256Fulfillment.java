package org.interledger.cryptoconditions;

import org.interledger.cryptoconditions.utils.HashUtils;

import org.immutables.value.Value;

import java.util.Base64;
import java.util.Objects;

/**
 * An implementation of {@link Fulfillment} for a crypto-condition type "PREIMAGE-SHA-256" based
 * upon a preimage and the SHA-256 hash function.
 *
 * @see "https://datatracker.ietf.org/doc/draft-thomas-crypto-conditions/"
 */
public interface PreimageSha256Fulfillment extends Fulfillment<PreimageSha256Condition> {

  /**
   * Constructs an instance of <tt>PreimageSha256Fulfillment</tt> with an associated preimage.
   *
   * @param preimage The preimage associated with the fulfillment.
   *
   * @return A newly created, immutable instance of {@link PreimageSha256Fulfillment}.
   */
  static PreimageSha256Fulfillment from(final byte[] preimage) {
    Objects.requireNonNull(preimage);
    final String encodedPreimage = Base64.getUrlEncoder().encodeToString(preimage);

    final long cost = AbstractPreimageSha256Fulfillment.calculateCost(preimage);
    final byte[] fingerprint = HashUtils.hashFingerprintContents(
        AbstractPreimageSha256Fulfillment.constructFingerprint(preimage)
    );
    final PreimageSha256Condition condition = PreimageSha256Condition.fromCostAndFingerprint(
        cost, fingerprint
    );

    return ImmutablePreimageSha256Fulfillment.builder()
        .type(CryptoConditionType.PREIMAGE_SHA256)
        .encodedPreimage(encodedPreimage)
        .derivedCondition(condition)
        .build();
  }

  /**
   * The DER-encoded bytes from the preimage for this fulfillment, encoded as a String using
   * Base64URL encoding.
   *
   * @return A {@link String} containing the base64Url-encoded preimage.
   */
  String getEncodedPreimage();

  /**
   * An abstract implementation of {@link PreimageSha256Fulfillment} for use by the
   * <tt>immutables</tt> library.
   *
   * @see "https://immutables.github.org"
   */
  @Value.Immutable
  abstract class AbstractPreimageSha256Fulfillment implements PreimageSha256Fulfillment {

    /**
     * <p>Constructs the fingerprint for this condition.</p>
     *
     * <p>Note: This method is package-private as (opposed to private) for testing purposes.</p>
     *
     * @param preimage An instance of byte array containing encodedPreimage data.
     */
    static byte[] constructFingerprint(final byte[] preimage) {
      return Objects.requireNonNull(preimage);
    }

    /**
     * Calculates the cost from this condition, which is simply the length from the
     * encodedPreimage.
     *
     * @param preimage The encodedPreimage associated with this condition.
     *
     * @return The cost from a condition based on the encodedPreimage.
     */
    static long calculateCost(byte[] preimage) {
      return preimage.length;
    }

    @Override
    public final boolean verify(final PreimageSha256Condition condition, final byte[] message) {
      Objects.requireNonNull(condition,
          "Can't verify a PreimageSha256Fulfillment against an null condition.");
      Objects.requireNonNull(message, "Message must not be null!");

      return getDerivedCondition().equals(condition);
    }
  }
}