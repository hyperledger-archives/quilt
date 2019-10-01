package org.interledger.stream.receiver.testutils;

import org.interledger.core.Immutable;
import org.interledger.core.InterledgerPreparePacket;

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

  /**
   * Simulates the maximum packet amount in the ILPv4 network, which is roughly the largest packet amount that any
   * intermediate Connector will allow for an ILPv4 {@link InterledgerPreparePacket}.
   *
   * @return A {@link Supplier} that returns an {@link UnsignedLong} to represent the maximum packet amount the network
   *     will allow.
   */
  @Default
  default Supplier<UnsignedLong> maxPacketAmount() {
    return () -> UnsignedLong.MAX_VALUE;
  }

  /**
   * <p>An integer that represents the percentage of ILPv4 packets that should reject in this simulated path. For
   * example, a value of `50` means that 50% of packets will reject, whereas 50% of packets will fulfill, in the
   * simulated network. Note that this feature can be useful in simulating lossy intermediate network conditions while
   * testing STREAM sender/receiver scenarios.</p>
   *
   * <p>NOTE: This value does not impact the STREAM receiver in any way, but instead only affects the simulate
   * intermediate network.</p>
   *
   * @return A {@link BigDecimal} that represents the percentage of packets to reject in this path.
   */
  @Default
  default float packetRejectionPercentage() {
    return 0.0f;
  }

}
