package org.interledger.cryptoconditions;

import static org.interledger.cryptoconditions.CryptoConditionType.THRESHOLD_SHA256;

import org.interledger.cryptoconditions.der.DerEncodingException;
import org.interledger.cryptoconditions.der.DerOutputStream;
import org.interledger.cryptoconditions.der.DerTag;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigInteger;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

/**
 * Implements a condition based on a number of subconditions and the SHA-256 function.
 */
public final class ThresholdSha256Condition extends CompoundSha256Condition
    implements CompoundCondition {

  /**
   * <p>Constructs an instance of the condition.</p>
   *
   * <p>Developers that wish to create a new instance of this class from an existing list of
   * subconditions should instead create a new {@link ThresholdSha256Fulfillment} and call {@link
   * ThresholdSha256Fulfillment#getCondition()}.</p>
   *
   * @param threshold     The number of subconditions that must be fulfilled.
   * @param subconditions A set of subconditions that this condition is dependent on.
   */
  public ThresholdSha256Condition(final int threshold, final List<Condition> subconditions) {
    super(
        THRESHOLD_SHA256,
        calculateCost(threshold, subconditions),
        hashFingerprintContents(
            constructFingerprintContents(threshold, subconditions)
        ),
        calculateSubtypes(subconditions)
    );
  }

  /**
   * <p>Constructs an instance of the condition.</p>
   *
   * <p>Note this constructor is package-private because it is used primarily for testing
   * purposes.</p>
   *
   * @param cost        The calculated cost of this condition.
   * @param fingerprint The calculcated fingerprint for the condition.
   * @param subtypes    A set of condition rsa for the subconditions that this one depends on.
   */
  public ThresholdSha256Condition(
      final long cost, final byte[] fingerprint, final EnumSet<CryptoConditionType> subtypes
  ) {
    super(THRESHOLD_SHA256, cost, fingerprint, subtypes);
  }

  /**
   * <p>Constructs the fingerprint for this condition.</p>
   *
   * <p>Note: This method is package-private as (opposed to private) for testing purposes.</p>
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
   * Sorts the given array of conditions into ascending lexicographic order.
   *
   * @param conditions The array of conditions to sort.
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
   * Calculates the cost of a threshold condition. The calculation is as follows:
   *
   * <pre>
   * sum(biggest(t, subcondition_costs)) + 1024 * n
   * </pre>
   *
   * @param threshold     The number of subconditions that must be met.
   * @param subconditions The list of subconditions.
   *
   * @return The calculated cost of a threshold condition.
   */
  private static final long calculateCost(
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
   * Determines the set of condition rsa that are ultimately held via the sub condition.
   *
   * @param subconditions The sub conditions that this condition depends on.
   *
   * @return The set of condition rsa related to the sub condition.
   */
  private static final EnumSet<CryptoConditionType> calculateSubtypes(
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

}
