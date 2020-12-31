package org.interledger.stream.pay.model;

import org.interledger.stream.pay.exceptions.StreamPayerException;

import org.immutables.value.Value.Default;
import org.immutables.value.Value.Immutable;

import java.math.BigInteger;
import java.util.Optional;

/**
 * The result of a payment. If the payment failed or did not complete successfully, an instance of {@link
 * StreamPayerException} will be present.
 */
@Immutable
public interface Receipt {

  static ImmutableReceipt.Builder builder() {
    return ImmutableReceipt.builder();
  }

  /**
   * The original quote for this payment, which contains everything about the payment's inputs, including quoted rates
   * discovered via rate-probing.
   */
  Quote originalQuote();

  /**
   * Amount sent and fulfilled, in normalized source units with arbitrary precision
   */
  @Default
  default BigInteger amountSentInSendersUnits() {
    return BigInteger.ZERO;
  }

  /**
   * Amount delivered to recipient, in normalized destination units with arbitrary precision
   */
  @Default
  default BigInteger amountDeliveredInDestinationUnits() {
    return BigInteger.ZERO;
  }

  /**
   * An optionally-present error representing why a payment failed.
   *
   * @return An optionally-present {@link StreamPayerException}.
   */
  Optional<StreamPayerException> paymentError();
}
