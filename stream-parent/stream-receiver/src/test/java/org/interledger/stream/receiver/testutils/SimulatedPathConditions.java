package org.interledger.stream.receiver.testutils;

import org.interledger.core.Immutable;

import com.google.common.primitives.UnsignedLong;
import org.immutables.value.Value.Default;

import java.math.BigDecimal;
import java.util.function.Supplier;

/**
 * Defines various variable for a unidirectional payment path, meaning these values only apply from the perspective of a
 * sender to a receiver.
 */
@Immutable
public interface SimulatedPathConditions {

  static SimulatedPathConditionsBuilder builder() {
    return new SimulatedPathConditionsBuilder();
  }

  /**
   * The current amount that the sender's packet amount should be multiplied by in order to simulate the receiver's
   * amount.
   *
   * @return A {@link Supplier} of type {@link BigDecimal} that represents the current FX rate. The supplier is used to
   *     allow implementations to vary this rate over time.
   */
  @Default
  default Supplier<BigDecimal> currentExchangeRateSupplier() {
    return () -> BigDecimal.ONE;
  }

  /**
   * Used by a STREAM receiver to govern the maximum amount of value that can be in-flight at any time.
   *
   * @return A {@link Supplier} that returns an {@link UnsignedLong} to represent the current maximum in-flight that the
   *     sender should see from the receiver.
   */
  @Default
  default Supplier<UnsignedLong> maxInFlight() {
    return () -> UnsignedLong.MAX_VALUE;
  }

}
