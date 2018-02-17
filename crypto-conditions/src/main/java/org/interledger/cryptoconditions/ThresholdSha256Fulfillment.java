package org.interledger.cryptoconditions;

import org.immutables.value.Value;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * An implementation of {@link Fulfillment} for a crypto-condition fulfillment of type
 * "THRESHOLD-SHA-256" based upon a number of sub-conditions and sub-fulfillments.
 *
 * @see "https://datatracker.ietf.org/doc/draft-thomas-crypto-conditions/"
 */
public interface ThresholdSha256Fulfillment extends Fulfillment<ThresholdSha256Condition> {

  /**
   * <p>Constructs an instance of {@link ThresholdSha256Fulfillment}.</p>
   *
   * <p>Per the spec, this method will imply the threshold based upon the number of sub-fulfillments
   * present in {@code subfulfillments}. This logic can be confusing, especially when trying to
   * construct threshold fulfillments that are only partially, yet validly, fulfilled.</p>
   *
   * <p>For example, if there exists a 1-of-2 {@link ThresholdSha256Condition} (i.e., Threshold =
   * 1), then it might be tempting to assume that there exists several threshold fulfillments that
   * could be verified by such a condition. The most obvious candidate is a 2-of-2 threshold
   * fulfillment, which could be constructed using this method by supplying zero sub-conditions and
   * two sub-fulfillments. However, in reality, such a sub-fulfillment (the 2-of-2) would not verify
   * with a 1-of-2 threshold condition because a threshold fulfillment created with two
   * sub-fulfillments <b>always</b> has a threshold of two.</p>
   *
   * <p>Because usage of this method always involves an inferred threshold, which might be
   * unexpected, it is recommended that callers utilize helper methods from {@link ThresholdFactory}
   * instead.</p>
   *
   * <p>Concurrency Note: This method will create a shallow-copy of both {@code subconditions} and
   * {@code subfulfillments} before performing any operations, in order to guard against external
   * list mutations. During the brief period of time that this method is shallow-copying, callers
   * should not consider this method to be thread-safe. This is because another thread could mutate
   * the lists (e.g., by adding or removing a sub-condition), which may cause unpredictable
   * behavior. In addition, this method assumes the sub-condition and sub-fulfillment
   * implementations are immutable, making shallow-copy operations sufficient to protect against
   * external list mutation. However, if your environment uses mutable implementations of either
   * {@link Condition} or {@link Fulfillment}, such shallow-copying may not be sufficient.</p>
   *
   * @param subconditions   An ordered {@link List} of unfulfilled sub-conditions that correspond to
   *                        the threshold condition being fulfilled. For example, if a given
   *                        Threshold condition has 2 preimage sub-conditions, but a threshold of 1,
   *                        then a valid fulfillment would have one of those preimage conditions in
   *                        this List, and a fulfillment for the other preimage condition in the
   *                        {@code fulfillments} list. Note that this list must be combined with the
   *                        list of conditions derived from the subfulfillments, and the combined
   *                        list, sorted, is used as the value when deriving the fingerprint of this
   *                        threshold fulfillment.
   * @param subfulfillments An ordered {@link List} of sub-fulfillments.  The number of elements in
   *                        this list is equal to the threshold of this fulfillment (i.e., per the
   *                        crypto-condtions specification, implementations must use the length of
   *                        this list as the threshold value when deriving the fingerprint of this
   *                        crypto-condition).
   *
   * @return A newly created, immutable instance of {@link ThresholdSha256Fulfillment}.
   */
  static ThresholdSha256Fulfillment from(
      final List<Condition> subconditions, final List<Fulfillment> subfulfillments
  ) {
    Objects.requireNonNull(subconditions, "subconditions must not be null!");
    Objects.requireNonNull(subfulfillments, "subfulfillments must not be null!");

    // Create new, immutable lists so that callers accessing these from the newly constructed
    // fulfillment will not be able to modify them. Also shallow-copy the inputs so that suppliers
    // of the sub-condition and sub-fulfillment lists can't modify data stored in this class from
    // the outside.
    final List<Condition> immutableSubconditions = Collections.unmodifiableList(
        subconditions.stream().collect(Collectors.toList())
    );
    final List<Fulfillment> immutableFulfillments = Collections.unmodifiableList(
        subfulfillments.stream().collect(Collectors.toList())
    );

    // Preemptively derive the condition from this fulfillment for immutability.
    final ThresholdSha256Condition condition = AbstractThresholdSha256Fulfillment
        .constructCondition(immutableSubconditions, immutableFulfillments);

    return ImmutableThresholdSha256Fulfillment.builder()
        .type(CryptoConditionType.THRESHOLD_SHA256)
        .subconditions(immutableSubconditions)
        .subfulfillments(immutableFulfillments)
        .derivedCondition(condition)
        .build();
  }

  /**
   * Accessor for the subconditions of this fulfillment.
   *
   * @return An unordered {@link List} of zero or more sub-conditions.
   */
  List<Condition> getSubconditions();

  /**
   * Accessor for the subfulfillments of this fulfillment.
   *
   * @return An unordered {@link List} of zero or more sub-fulfillments.
   */
  List<Fulfillment> getSubfulfillments();

  /**
   * An abstract implementation of {@link ThresholdSha256Fulfillment} to provide default
   * implementations to the generated immutable implementation.
   */
  @Value.Immutable
  abstract class AbstractThresholdSha256Fulfillment implements ThresholdSha256Fulfillment {

    /**
     * Preemptively construct the derived condition for this Threshold fulfillment.
     *
     * @param subconditions   An ordered {@link List} of unfulfilled sub-conditions as supplied by
     *                        {@link ThresholdSha256Fulfillment#from(List, List)}.
     * @param subfulfillments An ordered {@link List} of sub-fulfillments as supplied by {@link
     *                        ThresholdSha256Fulfillment#from(List, List)}.
     *
     * @return The {@link ThresholdSha256Condition} that corresponds to this fulfillment.
     *
     * @see ""
     */
    static ThresholdSha256Condition constructCondition(
        final List<Condition> subconditions, final List<Fulfillment> subfulfillments
    ) {
      Objects.requireNonNull(subconditions);
      Objects.requireNonNull(subfulfillments);

      final List<Condition> allConditions = new ArrayList<>();

      // Add all subconditions...
      allConditions.addAll(subconditions);

      // Add all derived subconditions...
      allConditions.addAll(
          subfulfillments.stream()
              .map(Fulfillment::getDerivedCondition)
              .collect(Collectors.toList())
      );

      // Per the crypto-condtions specification, implementations must use the length of the
      // fulfillments list as the threshold value when deriving the fingerprint of this
      // crypto-condition.
      return ThresholdSha256Condition.from(
          subfulfillments.size(),
          allConditions.stream().collect(Collectors.toList())
      );
    }

    /**
     * <p>Verify the {@link ThresholdSha256Condition}.</p>
     *
     * <p>A THRESHOLD-SHA-256 fulfillment is valid iff:</p>
     *
     * <ol><li>All (F).subfulfillments are valid.</li> <li>The derived condition (D) (found in
     * {@link #getDerivedCondition()}) is equal to the given condition (C) (found in {@code
     * condition}).</li></ol>
     *
     * <p>For more general details about Fulfillment validation, see the Javadoc in {@link
     * Fulfillment#verify(Condition, byte[])}.</p>
     *
     * @param condition A {@link Condition} that this fulfillment should verify.
     * @param message   A byte array that is part of verifying the supplied condition.
     *
     * @return {@code true} if the condition validates this fulfillment; {@code false} otherwise.
     */
    @Override
    public boolean verify(final ThresholdSha256Condition condition, final byte[] message) {
      Objects.requireNonNull(condition,
          "Can't verify a ThresholdSha256Fulfillment against an null condition.");
      Objects.requireNonNull(message, "Message must not be null!");

      if (!getDerivedCondition().equals(condition)) {
        return false;
      }

      final List<Fulfillment> subfulfillments = this.getSubfulfillments();
      for (int i = 0; i < subfulfillments.size(); i++) {

        final Fulfillment subfulfillment = subfulfillments.get(i);
        final Condition subcondition = subfulfillment.getDerivedCondition();
        if (!subfulfillment.verify(subcondition, message)) {
          return false;
        }
      }

      return true;
    }
  }
}
