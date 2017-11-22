package org.interledger.ipr;

import org.interledger.cryptoconditions.Condition;
import org.interledger.ilp.InterledgerPayment;

import org.immutables.value.Value;

import java.util.Objects;

/**
 * An Interledger Payment Request as defined in ILP RFC 11.
 *
 * @see "https://github.com/interledger/rfcs/blob/master/0011-interledger-payment-request/0011
 * -interledger-payment-request.md"
 */
@Value.Immutable
public interface InterledgerPaymentRequest {

  /**
   * Get the default builder.
   *
   * @return a {@link ImmutableInterledgerPaymentRequest.Builder} instance.
   */
  static ImmutableInterledgerPaymentRequest.Builder builder() {
    return ImmutableInterledgerPaymentRequest.builder();
  }

  /**
   * Get the version of this IPR (this interface represents Version 2).
   *
   * @return The version of the IPR (currently 2)
   */
  default int getVersion() {
    return 2;
  }

  /**
   * The Interledger Payment being requested.
   *
   * @return an Interledger Payment.
   */
  InterledgerPayment getInterledgerPayment();

  /**
   * The {@link Condition} to use when sending the payment.
   *
   * @return a Condition
   */
  Condition getCondition();

}

