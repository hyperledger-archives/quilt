package org.interledger.stream.pay.probing.model;

import com.google.common.base.Preconditions;
import java.math.BigDecimal;
import java.math.BigInteger;
import org.immutables.value.Value;
import org.immutables.value.Value.Immutable;
import org.interledger.core.fluent.FluentCompareTo;

/**
 * Amount and exchange rate conditions that must be met for the payment to complete.
 */
@Immutable
public interface PaymentTargetConditions {

  static ImmutablePaymentTargetConditions.Builder builder() {
    return ImmutablePaymentTargetConditions.builder();
  }

  PaymentType paymentType();

  /**
   * The maximum total amount that can be sent for this entire payment, in scaled source account units.
   *
   * @return A {@link BigInteger}.
   */
  BigInteger maxSourceAmount();

  /**
   * The minimum total amount that must be sent for this entire payment, in scaled destination account units.
   *
   * @return A {@link BigInteger}.
   */
  BigInteger minDeliveryAmount();

  BigDecimal minExchangeRate();

  @Value.Check
  default void check() {
    Preconditions.checkState(
      FluentCompareTo.is(this.maxSourceAmount()).greaterThan(BigInteger.ZERO),
      "maxSourceAmount must be greater-than 0"
    );
    Preconditions.checkState(
      FluentCompareTo.is(this.minDeliveryAmount()).greaterThanEqualTo(BigInteger.ZERO),
      "minDeliveryAmount must be greater-than or equal-to 0"
    );
  }

  enum PaymentType {
    /**
     * The amount that should be sent for this payment is fixed in the sender's units.
     */
    FIXED_SEND,
    /**
     * The amount that should be sent for this payment is fixed in the receiver's units.
     */
    FIXED_DELIVERY
  }

}
