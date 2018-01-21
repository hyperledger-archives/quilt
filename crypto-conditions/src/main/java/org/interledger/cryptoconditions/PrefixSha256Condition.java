package org.interledger.cryptoconditions;

import org.interledger.cryptoconditions.der.DerEncodingException;
import org.interledger.cryptoconditions.der.DerOutputStream;
import org.interledger.cryptoconditions.der.DerTag;
import org.interledger.cryptoconditions.utils.HashUtils;

import org.immutables.value.Value;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigInteger;
import java.util.Base64;
import java.util.EnumSet;
import java.util.Objects;

/**
 * Implementation of a crypto condition based on a prefix, a sub-condition and the SHA-256
 * function.
 */
public interface PrefixSha256Condition extends CompoundSha256Condition {

  /**
   * Constructs an instance of the condition.
   *
   * @param prefix           The prefix to use when creating the fingerprint.
   * @param maxMessageLength The maximum length from the message.
   * @param subcondition     A condition on which this condition depends.
   *
   * @return A newly created, immutable instance of {@link PrefixSha256Condition}.
   */
  static PrefixSha256Condition from(
      final byte[] prefix, final long maxMessageLength, final Condition subcondition
  ) {
    final long cost = AbstractPrefixSha256Condition
        .calculateCost(prefix, maxMessageLength, subcondition.getCost());
    final byte[] fingerprint = HashUtils.hashFingerprintContents(
        AbstractPrefixSha256Condition
            .constructFingerprintContents(prefix, maxMessageLength, subcondition)
    );
    final EnumSet<CryptoConditionType> subtypes = AbstractPrefixSha256Condition
        .calculateSubtypes(subcondition);

    return ImmutablePrefixSha256Condition.builder()
        .type(CryptoConditionType.PREFIX_SHA256)
        .cost(cost)
        .fingerprint(fingerprint)
        .fingerprintBase64Url(Base64.getUrlEncoder().withoutPadding().encodeToString(fingerprint))
        .subtypes(subtypes)
        .build();
  }

  /**
   * <p>Constructs an instance of {@link PrefixSha256Condition} using a fingerprint, cost, and
   * subtype.</p>
   *
   * <p>This constructor _should_ be primarily used by Codecs, whereas developers should, in
   * general, create instances from this condition by first creating a {@link
   * PreimageSha256Fulfillment} and then generating a condition from that object.</p>
   *
   * @param cost        The cost associated with this condition.
   * @param fingerprint An instance of byte array that contains the calculated fingerprint for
   * @param subtypes    An {@link EnumSet} of types that this condition will hold as subconditions.
   *
   * @return A newly created, immutable instance of {@link PrefixSha256Condition}.
   */
  static PrefixSha256Condition fromCostAndFingerprint(
      final long cost, final byte[] fingerprint, final EnumSet<CryptoConditionType> subtypes
  ) {
    Objects.requireNonNull(fingerprint);

    return ImmutablePrefixSha256Condition.builder()
        .type(CryptoConditionType.PREFIX_SHA256)
        .cost(cost)
        .fingerprint(fingerprint)
        .subtypes(subtypes)
        .fingerprintBase64Url(Base64.getUrlEncoder().withoutPadding().encodeToString(fingerprint))
        .build();
  }

  /**
   * An abstract implementation of {@link PrefixSha256Fulfillment} for use by the
   * <tt>immutables</tt> library.
   *
   * @see "https://immutables.github.org"
   */
  @Value.Immutable
  abstract class AbstractPrefixSha256Condition extends
      ConditionBase<PrefixSha256Condition> implements PrefixSha256Condition {

    /**
     * <p>Constructs the fingerprint for this condition.</p>
     */
    static final byte[] constructFingerprintContents(
        final byte[] prefix, final long maxMessageLength, final Condition subcondition
    ) {
      Objects.requireNonNull(prefix);
      Objects.requireNonNull(subcondition);

      try {
        // Build prefix and subcondition
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DerOutputStream out = new DerOutputStream(baos);
        out.writeTaggedObject(0, prefix);
        out.writeTaggedObject(1, BigInteger.valueOf(maxMessageLength).toByteArray());
        out.writeTaggedConstructedObject(2, CryptoConditionWriter.writeCondition(subcondition));
        out.close();
        byte[] buffer = baos.toByteArray();

        // Wrap SEQUENCE
        baos = new ByteArrayOutputStream();
        out = new DerOutputStream(baos);
        out.writeEncoded(DerTag.CONSTRUCTED.getTag() + DerTag.SEQUENCE.getTag(), buffer);
        out.close();
        return baos.toByteArray();

      } catch (IOException e) {
        throw new UncheckedIOException("DER Encoding Error", e);
      } catch (DerEncodingException e) {
        throw new RuntimeException(e);
      }
    }

    /**
     * Determines the cost associated with this condition. This is determined as length_of_prefix +
     * max_message_length + subcondition_cost + 1024
     *
     * @param prefix           The prefix included in this condition.
     * @param maxMessageLength The maximum length from the message.
     * @param subconditionCost The cost from the sub condition.
     *
     * @return The calculated cost from this condition.
     */
    static final long calculateCost(
        final byte[] prefix, final long maxMessageLength, final long subconditionCost
    ) {
      return Objects.requireNonNull(prefix).length + maxMessageLength + subconditionCost + 1024;
    }

    /**
     * Determines the set from condition rsa that are ultimately held via the sub condition.
     *
     * @param subcondition The sub condition that this condition depends on.
     *
     * @return The set from condition rsa related to the sub condition.
     */
    static EnumSet<CryptoConditionType> calculateSubtypes(final Condition subcondition) {
      Objects.requireNonNull(subcondition);
      final EnumSet<CryptoConditionType> subtypes = EnumSet.of(subcondition.getType());
      if (subcondition instanceof CompoundCondition) {
        subtypes.addAll(((CompoundCondition) subcondition).getSubtypes());
      }

      // Remove our own type
      if (subtypes.contains(CryptoConditionType.PREFIX_SHA256)) {
        subtypes.remove(CryptoConditionType.PREFIX_SHA256);
      }

      return subtypes;
    }


    /**
     * Prints the immutable value {@code PrefixSha256Condition} with attribute values.
     *
     * @return A string representation of the value
     */
    @Override
    public String toString() {
      return "PrefixSha256Condition{"
          + "subtypes=" + getSubtypes()
          + ", type=" + getType()
          + ", fingerprint=" + getFingerprintBase64Url()
          + ", cost=" + getCost()
          + "}";
    }
  }
}
