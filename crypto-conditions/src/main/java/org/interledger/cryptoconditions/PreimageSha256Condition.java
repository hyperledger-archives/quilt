package org.interledger.cryptoconditions;

import static org.interledger.cryptoconditions.CryptoConditionType.PREIMAGE_SHA256;

import java.util.Objects;

/**
 * This type of condition is also called a "hashlock".  By creating a hash of a difficult-to-guess,
 * 256-bit, random or pseudo-random integer, it is possible to create a condition which the creator
 * can trivially fulfill by publishing the random value used to create the condition (the original
 * random value is also referred to as a "preimage"). For anyone else, a hashlock condition is
 * cryptographically hard to fulfill because one would have to find a preimage for the given
 * condition hash.
 */
public final class PreimageSha256Condition extends Sha256Condition implements SimpleCondition {

  /**
   * <p>Required-args Constructor.  Constructs an instance of {@link PreimageSha256Condition} based
   * on a supplied preimage.</p>
   *
   * <p>Developers that wish to create a new {@link PreimageSha256Condition} from the preimage
   * should instead create a new {@link PreimageSha256Fulfillment} and call {@link
   * PreimageSha256Fulfillment#getCondition()}.</p>
   *
   * @param preimage An instance of byte array containing preimage data.
   */
  PreimageSha256Condition(final byte[] preimage) {
    super(
        CryptoConditionType.PREIMAGE_SHA256,
        calculateCost(preimage),
        hashFingerprintContents(
            constructFingerprintContents(preimage)
        )
    );
  }

  /**
   * <p>Constructs an instance of {@link PreimageSha256Condition} using a fingerprint and cost.</p>
   *
   * <p>This constructor _should_ be primarily used by Codecs, whereas developers should, in
   * general, prefer the {@link #PreimageSha256Condition(byte[])} constructor, or else create an
   * actual fulfillment via {@link PreimageSha256Fulfillment#PreimageSha256Fulfillment(byte[])}  and
   * then generate a condition from that object.</p>
   *
   * @param cost        The cost associated with this condition.
   * @param fingerprint An instance of byte array that contains the calculated fingerprint for
   */
  public PreimageSha256Condition(final long cost, final byte[] fingerprint) {
    super(PREIMAGE_SHA256, cost, fingerprint);
  }

  /**
   * <p>Constructs the fingerprint for this condition.</p>
   *
   * <p>Note: This method is package-private as (opposed to private) for testing purposes.</p>
   *
   * @param preimage An instance of byte array containing preimage data.
   */
  static final byte[] constructFingerprintContents(final byte[] preimage) {
    return Objects.requireNonNull(preimage);
  }

  /**
   * Calculates the cost of this condition, which is simply the length of the preimage.
   *
   * @param preimage The preimage associated with this condition.
   *
   * @return The cost of a condition based on the preimage.
   */
  private static final long calculateCost(byte[] preimage) {
    return preimage.length;
  }

}