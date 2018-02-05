package org.interledger.cryptoconditions;

import static org.interledger.cryptoconditions.CryptoConditionType.THRESHOLD_SHA256;

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
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * <p>Implements a condition based on a number of subconditions and the SHA-256 function.</p>
 *
 * <p>Threshold conditions can be used to create m-of-n multi-signature groups.</p>
 *
 * <p>Threshold conditions can represent the AND operator by setting the threshold to equal the
 * number of subconditions (n-of-n) or the OR operator by setting the thresold to one (1-of-n).</p>
 *
 * <p>Since threshold conditions operate on conditions, they can be nested as well which allows the
 * creation of deep threshold trees of public keys.</p>
 *
 * <p>By using Merkle trees, threshold fulfillments do not need to to provide the structure of
 * unfulfilled subtrees. That means only the public keys that are actually used in a fulfillment,
 * will actually appear in the fulfillment, saving space.</p>
 *
 * <p>One way to formally interpret a threshold condition is as a booleanthreshold gate. A tree of
 * threshold conditions forms a boolean threshold circuit.</p>
 */
public interface ThresholdSha256Condition extends CompoundSha256Condition {

  /**
   * <p>Constructs an instance of {@link ThresholdSha256Condition}.</p>
   *
   * <p>Concurrency Note: This method will create a shallow-copy of {@code subconditions} before
   * performing any validation, business logic, or object construction. For most scenarios, this
   * will provide adequate immutability for any operations performed by this method. However, during
   * the brief period of time that this method is deep-copying, callers should not consider this
   * method to be thread-safe. This is because another thread could mutate the sub-conditions list
   * (e.g., by adding or removing a condition), which may cause unpredictable behavior. Thus, if
   * thread-safety is required when calling this method, be sure to guard against any concurrency
   * issues in your system, perhaps by passing-in an immutable copy of {@code subconditions} into
   * this method, or perhaps by wrapping it in an immutable variant such as via {@link
   * Collections#unmodifiableList(List)}. Finally, this method assumes any instance of {@link
   * Condition} is immutable (all supplied implementations of {@link Condition} and {@link
   * Fulfillment} supplied by this library are immutable), making shallow-copied lists acceptable.
   * If you supply alternate, mutable implementations of {@link Condition} to this method,
   * unpredictable results might occur in high-concurrency scenarios.</p>
   *
   * @param threshold     Determines the threshold that is used to consider this condition
   *                      fulfilled. If the number of valid subfulfillments in a {@link
   *                      ThresholdSha256Fulfillment} is greater or equal to this number, this
   *                      threshold condition will be considered to be fulfilled.
   * @param subconditions A set from sub-conditions that this condition is dependent on.
   *
   * @return A newly created, immutable instance of {@link ThresholdSha256Condition}.
   */
  static ThresholdSha256Condition from(final int threshold, final List<Condition> subconditions) {
    Objects.requireNonNull(subconditions);

    // Shallow-copy ths list to narrow the timeframe that some outside thread might mutate the
    // subconditions list. See Javadoc for suggestions related to thread-safety and this method.
    final List<Condition> immutableSubconditions = subconditions.stream()
        .collect(Collectors.toList());
    if (threshold > immutableSubconditions.size()) {
      throw new IllegalArgumentException(
          "Threshold must be less than or equal to the number of subconditions!");
    }

    final long cost = AbstractThresholdSha256Condition.calculateCost(
        threshold, immutableSubconditions
    );
    final byte[] fingerprint = HashUtils.hashFingerprintContents(
        AbstractThresholdSha256Condition
            .constructFingerprintContents(threshold, immutableSubconditions)
    );

    return ImmutableThresholdSha256Condition.builder()
        .type(THRESHOLD_SHA256)
        .cost(cost)
        .fingerprint(fingerprint)
        .fingerprintBase64Url(Base64.getUrlEncoder().withoutPadding().encodeToString(fingerprint))
        .subtypes(AbstractThresholdSha256Condition.calculateSubtypes(immutableSubconditions))
        .build();
  }

  /**
   * <p>Constructs an instance of {@link ThresholdSha256Condition} using a fingerprint, cost, and
   * subtypes.</p>
   *
   * <p>This constructor _should_ be primarily used by Codecs, whereas developers should, in
   * general, create an actual fulfillment via {@link PreimageSha256Fulfillment
   * #PreimageSha256Fulfillment(byte[])} and then generate a condition from that object.</p>
   *
   * @param cost        The calculated cost from this condition.
   * @param fingerprint The calculcated fingerprint for the condition.
   * @param subtypes    A set from condition rsa for the subconditions that this one depends on.
   *
   * @return A newly created, immutable instance of {@link ThresholdSha256Condition}.
   */
  static ThresholdSha256Condition fromCostAndFingerprint(
      final long cost, final byte[] fingerprint, final EnumSet<CryptoConditionType> subtypes
  ) {
    Objects.requireNonNull(fingerprint);
    return ImmutableThresholdSha256Condition.builder()
        .type(CryptoConditionType.THRESHOLD_SHA256)
        .cost(cost)
        .fingerprint(fingerprint)
        .fingerprintBase64Url(Base64.getUrlEncoder().withoutPadding().encodeToString(fingerprint))
        .subtypes(subtypes)
        .build();
  }


  /**
   * An abstract implementation of {@link ThresholdSha256Condition} for use by the
   * <tt>immutables</tt> library.
   *
   * @see "https://immutables.github.org"
   */
  @Value.Immutable
  abstract class AbstractThresholdSha256Condition extends
      ConditionBase<ThresholdSha256Condition> implements ThresholdSha256Condition {

    /**
     * <p>Constructs the fingerprint for this condition.</p>
     *
     * <p>Note: This method is package-private as (opposed to private) for testing purposes.</p>
     *
     * <p>Note that this method does not create a copy (shallow or deep) of {@code subconditions}
     * because it assumes that the {@link List} passed-in has already been shallow-copied and
     * wrapped in an immutable facade, such as via {@link Collections#unmodifiableList(List)}.</p>
     *
     * @param threshold     A threshold to use for this fingerprint.
     * @param subconditions A list of subconditions. This method assumes these conditions have been
     *                      deduplicated.
     */
    static final byte[] constructFingerprintContents(
        final int threshold, final List<Condition> subconditions
    ) {
      try {

        // Sort
        sortConditions(subconditions);

        // Build subcondition sequence
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DerOutputStream out = new DerOutputStream(baos);
        for (int i = 0; i < subconditions.size(); i++) {
          out.write(CryptoConditionWriter.writeCondition(subconditions.get(i)));
        }
        out.close();

        final byte[] subconditionBuffer = baos.toByteArray();

        // Build threshold and subconditions sequence
        baos = new ByteArrayOutputStream();
        out = new DerOutputStream(baos);
        out.writeTaggedObject(0, BigInteger.valueOf(threshold).toByteArray());
        out.writeTaggedConstructedObject(1, subconditionBuffer);
        out.close();

        final byte[] thresholdBuffer = baos.toByteArray();

        // Wrap SEQUENCE
        baos = new ByteArrayOutputStream();
        out = new DerOutputStream(baos);
        out.writeEncoded(DerTag.CONSTRUCTED.getTag() + DerTag.SEQUENCE.getTag(), thresholdBuffer);
        out.close();
        return baos.toByteArray();

      } catch (IOException e) {
        throw new UncheckedIOException("DER Encoding Error", e);
      } catch (DerEncodingException e) {
        throw new RuntimeException(e);
      }
    }

    /**
     * Sorts the given array from conditions into ascending lexicographic order.
     *
     * <p>Note that this method does not create a copy (shallow or deep) of {@code subconditions}
     * because it assumes that the {@link List} passed-in has already been shallow-copied and
     * wrapped in an immutable facade, such as via {@link Collections#unmodifiableList(List)}.</p>
     *
     * @param conditions The array from conditions to sort.
     */
    private static final void sortConditions(final List<Condition> conditions) {
      Objects.requireNonNull(conditions);

      conditions.sort((Condition c1, Condition c2) -> {
        try {
          byte[] c1encoded = CryptoConditionWriter.writeCondition(c1);
          byte[] c2encoded = CryptoConditionWriter.writeCondition(c2);

          int minLength = Math.min(c1encoded.length, c2encoded.length);
          for (int i = 0; i < minLength; i++) {
            int result = Integer.compareUnsigned(c1encoded[i], c2encoded[i]);
            if (result != 0) {
              return result;
            }
          }
          return c1encoded.length - c2encoded.length;
        } catch (DerEncodingException e) {
          throw new RuntimeException(e);
        }
      });

    }

    /**
     * <p>Calculates the cost from a threshold condition. The calculation is as follows:</p>
     *
     * <pre>
     * sum(biggest(t, subcondition_costs)) + 1024 * n
     * </pre>
     *
     * <p>Note that this method does not create a copy (shallow or deep) of {@code subconditions}
     * because it assumes that the {@link List} passed-in has already been shallow-copied.</p>
     *
     * @param threshold     The number from subconditions that must be met.
     * @param subconditions The list from subconditions.
     *
     * @return The calculated cost from a threshold condition.
     */
    static final long calculateCost(
        final int threshold, final List<Condition> subconditions
    ) {
      Objects.requireNonNull(subconditions);

      // Sort by cost
      subconditions.sort((Condition c1, Condition c2) -> (int) (c2.getCost() - c1.getCost()));

      // Count only up to the threshold...
      long largestCosts = 0;
      for (int i = 0; i < threshold; i++) {
        largestCosts += subconditions.get(i).getCost();
      }

      return largestCosts + (subconditions.size() * 1024);
    }

    /**
     * <p>Determines the set from condition rsa that are ultimately held via the sub condition.</p>
     *
     * <p>Note that this method does not create a copy (shallow or deep) of {@code subconditions}
     * because it assumes that the {@link List} passed-in has already been shallow-copied and
     * wrapped in an immutable facade, such as via {@link Collections#unmodifiableList(List)}.</p>
     *
     * @param subconditions The sub conditions that this condition depends on.
     *
     * @return The set from condition rsa related to the sub condition.
     */
    static final EnumSet<CryptoConditionType> calculateSubtypes(
        final List<Condition> subconditions
    ) {
      Objects.requireNonNull(subconditions);

      final EnumSet<CryptoConditionType> subtypes = EnumSet.noneOf(CryptoConditionType.class);
      for (int i = 0; i < subconditions.size(); i++) {
        subtypes.add(subconditions.get(i).getType());
        if (subconditions.get(i) instanceof CompoundCondition) {
          subtypes.addAll(((CompoundCondition) subconditions.get(i)).getSubtypes());
        }
      }

      // Remove our own type
      if (subtypes.contains(THRESHOLD_SHA256)) {
        subtypes.remove(THRESHOLD_SHA256);
      }

      return subtypes;
    }

    /**
     * Prints the immutable value {@code ThresholdSha256Condition} with attribute values.
     *
     * @return A string representation of the value
     */
    @Override
    public String toString() {
      return "ThresholdSha256Condition{"
          + "subtypes=" + getSubtypes()
          + ", type=" + getType()
          + ", fingerprint=" + getFingerprintBase64Url()
          + ", cost=" + getCost()
          + "}";
    }
  }

}
