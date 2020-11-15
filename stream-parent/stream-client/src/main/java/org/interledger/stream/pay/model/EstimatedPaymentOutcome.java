package org.interledger.stream.pay.model;

import java.math.BigInteger;
import org.immutables.value.Value;
import org.immutables.value.Value.Immutable;
import org.interledger.stream.pay.model.ImmutableEstimatedPaymentOutcome.Builder;

/**
 * An estimate of the outcome of a stream payment.
 */
@Immutable
public interface EstimatedPaymentOutcome {

  static Builder builder() {
    return ImmutableEstimatedPaymentOutcome.builder();
  }

  /**
   * Estimated total number of packets that will be required to make a payment.
   *
   * @return A {@link BigInteger}.
   */
  BigInteger estimatedNumberOfPackets();

  /**
   * The estimated maximum amount that will be sent as part of this payment, in source units.
   *
   * @return A {@link BigInteger}.
   */
  BigInteger maxSourceAmountInSourceUnits();

  /**
   * The estimated minimum amount that will be delivered as part of this payment, in destination units.
   *
   * @return A {@link BigInteger}.
   */
  BigInteger minDeliveryAmountInDestinationUnits();

  @Value.Check
  default void check() {
//    Preconditions.checkState(
//      FluentCompareTo.is(estimatedNumberOfPackets()).greaterThan(BigInteger.ZERO),
//      "estimatedNumberOfPackets must be positive."
//    );

//    Preconditions.checkState(
//      FluentCompareTo.is(maxSourceAmountInSourceUnits()).greaterThan(BigInteger.ZERO),
//      "maxSourceAmount must be positive."
//    );
  }

}
