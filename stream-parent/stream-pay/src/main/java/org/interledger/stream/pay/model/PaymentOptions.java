package org.interledger.stream.pay.model;

import org.interledger.fx.Denomination;
import org.interledger.fx.Slippage;
import org.interledger.spsp.PaymentPointer;
import org.interledger.stream.model.AccountDetails;

import org.immutables.value.Value.Default;
import org.immutables.value.Value.Immutable;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Optional;

@Immutable
public interface PaymentOptions {

  static ImmutablePaymentOptions.Builder builder() {
    return ImmutablePaymentOptions.builder();
  }

  /**
   * The Account details of the sender. Note that in certain use-cases this can be populated via IL-DCP or with a
   * private network value if the details are not meaningful.
   *
   * @return An {@link AccountDetails}.
   */
  AccountDetails senderAccountDetails();

  /**
   * Fixed amount to send to the recipient, in normalized source-units with arbitrary precision (E.g., if the sender
   * wants to send three dollars, then this value would be 3.0, even though the source-account sending the payment may
   * have a different precision).
   *
   * @return
   */
  // TODO [NewFeature]: Use a Money here so that the currency is reflected in the amount? If the desired currency
  //  doesn't match the account, then we can throw an exception because the sender should know how the source currency
  //  code ahead of time.
  BigDecimal amountToSend();

  /**
   * The Payment Pointer for the destination account.
   *
   * @return A {@link PaymentPointer}.
   */
  PaymentPointer destinationPaymentPointer();

  /**
   * The {@link Denomination} that we expect the receiver to have and use as part of this payment.
   *
   * @return An optionally-present {@link Denomination}.
   */
  Optional<Denomination> expectedDestinationDenomination();

  /**
   * Indicates if the initial foreign exchange rate probe should be skipped in order to save time. Setting this to
   * {@code true} can be useful in scenarios where rate-probing isn't useful, such as when the source and destination
   * denominations are the same or otherwise. Note that settting this to true does not skip the rate probe entirely if
   * the destination denomination is not set via {@link #expectedDestinationDenomination()} (in this scenario, a single
   * zero-value probe packet will be sent in order to trigger the CAD response from the Stream receiver).
   *
   * @return {@code true} if the initial rate probe should be skipped; {@code false} otherwise.
   */
  @Default
  default boolean probePathUsingExternalRates() {
    return false;
  }

  @Default
  default Slippage slippage() {
    return Slippage.ONE_PERCENT;
  }

  /**
   * The total amount of time to wait before failing this payment.
   *
   * @return A {@link Duration}.
   */
  @Default
  default Duration paymentTimeout() {
    return Duration.ofSeconds(30);
  }

}
