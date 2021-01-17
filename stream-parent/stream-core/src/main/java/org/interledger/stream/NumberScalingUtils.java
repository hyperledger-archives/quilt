package org.interledger.stream;

import com.google.common.base.Preconditions;
import com.google.common.primitives.UnsignedLong;

import java.math.BigInteger;
import java.util.Objects;

/**
 * Utilities for normalizing scaled account values.
 *
 * NOTE: This class may appear unused, but is used by the Java Connector and potentially other software.
 */
public class NumberScalingUtils {

  /**
   * Translate a {@link BigInteger} with a given scale into an {@link UnsignedLong} that corresponds to the target
   * scale.
   *
   * @param sourceAmount     A {@link BigInteger} representing the amount of units, in the clearing account's scale, to
   *                         attempt to settle.
   * @param sourceScale      An int representing a source scale.
   * @param destinationScale An int representing a destination scale
   *
   * @return A new {@link BigInteger} containing an amount scaled into the {@code destinationScale} units. Note that if
   *   there are any remainder units after performing the conversion, these will not be present in this return value
   *   (they will be left in the balance tracker to be dealt with in future adjustments).
   *
   * @see "https://java-connector.ilpv4.dev/overview/terminology#scale"
   */
  public static UnsignedLong translate(
    final UnsignedLong sourceAmount, final short sourceScale, final short destinationScale
  ) throws IllegalArgumentException {
    Objects.requireNonNull(sourceAmount, "sourceAmount must not be null");
    Preconditions.checkArgument(sourceScale >= 0, "sourceScale must be positive");
    Preconditions.checkArgument(destinationScale >= 0, "destinationScale must be positive");

    BigInteger sourceAmountBI = BigInteger.valueOf(sourceAmount.longValue());

    // The difference between the two `scale` values
    final int scaleDifference = destinationScale - sourceScale;

    // TODO [New Feature]: Try shiftBy and compare speed?
    final BigInteger scaledAmount = scaleDifference > 0 ?
      // value * (10^scaleDifference), always rounds to floor via BigInteger
      sourceAmountBI.multiply(BigInteger.TEN.pow(scaleDifference)) :
      // value / (10^-scaleDifference))
      sourceAmountBI.divide((BigInteger.TEN.pow(scaleDifference * -1)));

    // Throws IllegalArgumentException if the BigInteger is too large.
    return UnsignedLong.valueOf(scaledAmount);
  }
}
