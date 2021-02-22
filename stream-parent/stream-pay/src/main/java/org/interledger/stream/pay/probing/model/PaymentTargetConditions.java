package org.interledger.stream.pay.probing.model;

import org.interledger.core.fluent.FluentCompareTo;
import org.interledger.core.fluent.Ratio;

import com.google.common.base.Preconditions;
import org.immutables.value.Value;
import org.immutables.value.Value.Immutable;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Amount and exchange rate conditions that must be met for the payment to complete.
 */
@Immutable
public interface PaymentTargetConditions {

  static ImmutablePaymentTargetConditions.Builder builder() {
    return ImmutablePaymentTargetConditions.builder();
  }

  /**
   * The Payment Type for this class.
   *
   * @return A {@link PaymentType}.
   */
  PaymentType paymentType();

  /**
   * The maximum total amount that can be sent for this entire payment, in scaled source account units.
   *
   * @return A {@link BigInteger}.
   */
  BigInteger maxPaymentAmountInSenderUnits();

  /**
   * The minimum total amount that must be sent for this entire payment, in scaled destination account units.
   *
   * @return A {@link BigInteger}.
   */
  BigInteger minPaymentAmountInDestinationUnits();

  /**
   * The minimum acceptable exchange rate for this payment.
   *
   * @return A {@link BigDecimal}.
   */
  Ratio minExchangeRate();

  /**
   * Helper method for immutables.
   */
  @Value.Check
  default void check() {
    Preconditions.checkState(
      FluentCompareTo.is(this.maxPaymentAmountInSenderUnits()).greaterThan(BigInteger.ZERO),
      "maxSourceAmount must be greater-than 0"
    );
    Preconditions.checkState(
      FluentCompareTo.is(this.minPaymentAmountInDestinationUnits()).greaterThanEqualTo(BigInteger.ZERO),
      "minDeliveryAmount must be greater-than or equal-to 0"
    );
  }

  /**
   * The type of payment to be made.
   */
  enum PaymentType {
    /**
     * The amount that should be sent for this payment is fixed in the source account's units.
     */
    FIXED_SEND,
    /**
     * The amount that should be sent for this payment is fixed in the destination account's units.
     */
    FIXED_DELIVERY
  }

}
