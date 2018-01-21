package org.interledger.cryptoconditions;

import org.immutables.value.Value;

import java.util.Base64;
import java.util.Objects;

/**
 * <p>An extension of {@link Sha256Condition} that implements a pre-image condition.</p>
 *
 * <p>This condition type is also known as a "hash-lock".  By creating a hash of a
 * difficult-to-guess, 256-bit, random (or pseudo-random) integer, it is possible to create a
 * condition which the creator can trivially fulfill by publishing the random value used to create
 * the condition (the original random value is also referred to as a "p-image"). For anyone without
 * the fulfillment or pre-image, a hash-lock condition is cryptographically hard to fulfill because
 * one would have to brute-force into a pre-image using just the hash found in a given pre-image
 * condition.</p>
 */
public interface PreimageSha256Condition extends Sha256Condition {

  /**
   * <p>Constructs an instance of {@link PreimageSha256Condition} using a fingerprint and a
   * cost.</p>
   *
   * <p>This constructor _should_ be primarily used by Codecs, whereas developers should, in
   * general, create an actual fulfillment via {@link PreimageSha256Fulfillment
   * #PreimageSha256Fulfillment(byte[])} and then generate a condition from that object.</p>
   *
   * @param cost        The cost associated with this condition.
   * @param fingerprint An instance of byte array that contains the calculated fingerprint of this
   *                    condition.
   *
   * @return A newly created, immutable instance of {@link PreimageSha256Condition}.
   */
  static PreimageSha256Condition fromCostAndFingerprint(final long cost, final byte[] fingerprint) {
    Objects.requireNonNull(fingerprint);
    return ImmutablePreimageSha256Condition.builder()
        .type(CryptoConditionType.PREIMAGE_SHA256)
        .cost(cost)
        .fingerprint(fingerprint)
        .fingerprintBase64Url(Base64.getUrlEncoder().withoutPadding().encodeToString(fingerprint))
        .build();
  }

  /**
   * An abstract implementation of {@link PreimageSha256Condition} for use by the
   * <tt>immutables</tt> library.
   *
   * @see "https://immutables.github.org"
   */
  @Value.Immutable
  abstract class AbstractPreimageSha256Condition extends
      ConditionBase<PreimageSha256Condition> implements PreimageSha256Condition {

    @Override
    public String toString() {
      return "PreimageSha256Condition{"
          + "type=" + getType() + ", "
          + "fingerprint=" + getFingerprintBase64Url() + ", "
          + "cost=" + getCost()
          + "}";
    }
  }
}