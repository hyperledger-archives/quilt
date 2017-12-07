package org.interledger.ipr;

import org.interledger.annotations.Immutable;
import org.interledger.cryptoconditions.Condition;
import org.interledger.ilp.InterledgerPayment;

import org.immutables.value.internal.$processor$.meta.$ValueMirrors;

/**
 * An Interledger Payment Request as defined in ILP RFC 11.
 *
 * @see "https://github.com/interledger/rfcs/blob/master/0011-interledger-payment-request/0011
 * -interledger-payment-request.md"
 */
public interface InterledgerPaymentRequest {

  /**
   * Get the default builder.
   *
   * @return a {@link InterledgerPaymentRequestBuilder} instance.
   */
  static InterledgerPaymentRequestBuilder builder() {
    return new InterledgerPaymentRequestBuilder();
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

  @Immutable
  abstract class AbstractInterledgerPaymentRequest implements InterledgerPaymentRequest {

  }

}

