package org.interledger.stream.pay.probing.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.math.BigInteger;
import org.immutables.value.Value;
import org.immutables.value.Value.Immutable;

/**
 * An estimate of the outcome of a stream payment.
 */
@Immutable
@JsonSerialize(as = ImmutableEstimatedPaymentOutcome.class)
@JsonDeserialize(as = ImmutableEstimatedPaymentOutcome.class)
public interface EstimatedPaymentOutcome {

  static ImmutableEstimatedPaymentOutcome.Builder builder() {
    return ImmutableEstimatedPaymentOutcome.builder();
  }

  /**
   * Estimated total number of packets that will be required to make a payment.
   *
   * @return A {@link BigInteger}.
   */
  BigInteger estimatedNumberOfPackets();

  /**
   * The estimated maximum amount that will be sent as part of this payment, denominated in source units with scale
   * equal to 0 (i.e., for dollars, a value of 1 would be $1 dollar).
   *
   * @return A {@link BigInteger}.
   */
  BigInteger maxSendAmountInWholeSourceUnits();

  /**
   * The estimated minimum amount that will be delivered as part of this payment, denominated in destination units with
   * scale equal to 0 (i.e., for dollars, a value of 1 would be $1 dollar).
   *
   * @return A {@link BigInteger}.
   */
  BigInteger minDeliveryAmountInWholeDestinationUnits();

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
