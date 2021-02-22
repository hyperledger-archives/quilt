package org.interledger.stream.pay.probing.model;

import com.google.common.primitives.UnsignedLong;
import org.immutables.value.Value.Immutable;

/**
 * Holds a low-end and high-end estimate for the number of units that apply to a particular exchange rate. Depending on
 * the context, the measured units may be in sender or receiver units.
 */
@Immutable
public interface DeliveredExchangeRateBound {

  static ImmutableDeliveredExchangeRateBound.Builder builder() {
    return ImmutableDeliveredExchangeRateBound.builder();
  }

  /**
   * The low-end estimate (i.e., minimum) number of destination units that a particular STREAM packet is expected to
   * deliver (typed as an {@link UnsignedLong} because this value does not represent a total payment amount, but instead
   * only a packetized component amount as deliverable in a single packet).
   *
   * @return An {@link UnsignedLong}.
   */
  UnsignedLong lowEndEstimate();

  /**
   * The high-end estimate (i.e., maximum) number of destination units that a particular STREAM packet is expected to
   * deliver (typed as an {@link UnsignedLong} because this value does not represent a total payment amount, but instead
   * only a packetized component amount as deliverable in a single packet).
   *
   * @return An {@link UnsignedLong}.
   */
  UnsignedLong highEndEstimate();

}
