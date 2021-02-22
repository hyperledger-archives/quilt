package org.interledger.stream.pay.model;

import org.interledger.core.fluent.FluentCompareTo;
import org.interledger.stream.pay.exceptions.StreamPayerException;

import com.google.common.primitives.UnsignedLong;
import org.immutables.value.Value.Default;
import org.immutables.value.Value.Derived;
import org.immutables.value.Value.Immutable;

import java.math.BigInteger;
import java.util.Optional;

/**
 * The result of a payment. If the payment failed or did not complete successfully, an instance of {@link
 * StreamPayerException} will be present.
 */
@Immutable
public interface PaymentReceipt {

  static ImmutablePaymentReceipt.Builder builder() {
    return ImmutablePaymentReceipt.builder();
  }

  /**
   * The original quote for this payment, which contains everything about the payment's inputs, including quoted rates
   * discovered via rate-probing.
   */
  Quote originalQuote();

  /**
   * The actual amount, in the senders units, that was sent and delivered to the receiver (i.e., fulfilled). In the case
   * of a timeout or rejected packets, this amount may be less than the requested amount to be sent.
   */
  BigInteger amountSentInSendersUnits();

  /**
   * The actual amount, in the senders units, that is still left to send. If the payment was successful, this amount
   * will be {@link BigInteger#ZERO}. However, if there was an issue completing the payment (e.g., repeated rejections
   * or an overall payment timeout), then this value will be the amount that could not be sent with finality.
   *
   * @return An {@link BigInteger} representing the amount left to send.
   */
  BigInteger amountLeftToSendInSendersUnits();

  /**
   * The actual amount, in the receivers units with arbitrary precision, that was delivered to the receiver. Note that
   * any currency conversion and/or connector fees may cause this amount to be different than the scaled sent amount.
   *
   * @return An {@link UnsignedLong} representing the amount delivered.
   */
  @Default
  default BigInteger amountDeliveredInDestinationUnits() {
    return BigInteger.ZERO;
  }

  /**
   * Statistics about the finished payment.
   *
   * @return A {@link PaymentStatistics}.
   */
  PaymentStatistics paymentStatistics();

  /**
   * Indicates if the payment was completed successfully. A payment is considered to be successful if it has delivered
   * all of the payment (i.e., the "amount left to send" is 0).
   *
   * @return {@code true} if payment was successful; {@code false} otherwise.
   */
  @Derived
  default boolean successfulPayment() {
    return FluentCompareTo.is(amountLeftToSendInSendersUnits()).lessThanOrEqualTo(BigInteger.ZERO);
  }

  /**
   * An optionally-present error representing why a payment failed.
   *
   * @return An optionally-present {@link StreamPayerException}.
   */
  Optional<StreamPayerException> paymentError();
}
