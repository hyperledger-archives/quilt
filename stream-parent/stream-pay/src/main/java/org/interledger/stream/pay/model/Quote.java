package org.interledger.stream.pay.model;

import org.interledger.core.fluent.Ratio;
import org.interledger.stream.connection.StreamConnection;
import org.interledger.stream.model.AccountDetails;
import org.interledger.stream.pay.model.ImmutableQuote.Builder;
import org.interledger.stream.pay.probing.model.EstimatedPaymentOutcome;
import org.interledger.stream.pay.probing.model.ExchangeRateProbeOutcome;
import org.interledger.stream.pay.trackers.PaymentSharedStateTracker;

import org.immutables.value.Value.Immutable;

import java.time.Duration;

/**
 * A quote for a payment (where the payment parameters are defined by a supplied {@link PaymentOptions}).
 */
@Immutable
public interface Quote {

  static Builder builder() {
    return ImmutableQuote.builder();
  }

  /**
   * The originally supplied options for this Quote.
   *
   * @return A {@link PaymentOptions}.
   */
  PaymentOptions paymentOptions();

  /**
   * Source account details.
   *
   * @return An {@link AccountDetails}.
   */
  AccountDetails sourceAccount();

  /**
   * Destination account details.
   *
   * @return An {@link AccountDetails}.
   */
  AccountDetails destinationAccount();

  /**
   * Minimum exchange rate allowed for a payment; used to enforce rates.
   *
   * @return A {@link Ratio}.
   */
  Ratio minAllowedExchangeRate();

  /**
   * The STREAM connection constructed as part of the quoting process.
   *
   * @return A {@link StreamConnection}.
   */
  StreamConnection streamConnection();

  /**
   * A container for all shared-state trackers for a particular payment.
   *
   * @return A {@link PaymentSharedStateTracker}.
   */
  PaymentSharedStateTracker paymentSharedStateTracker();

  /**
   * Probed exchange rate over the path, as a range between {@link ExchangeRateProbeOutcome#lowerBoundRate} and {@link
   * ExchangeRateProbeOutcome#upperBoundRate}.
   *
   * @return A {@link ExchangeRateProbeOutcome}.
   */
  ExchangeRateProbeOutcome estimatedExchangeRate();

  /**
   * Estimated payment duration in milliseconds, based on max packet amount, RTT, and rate of packet throttling.
   *
   * @return A {@link Duration}.
   */
  Duration estimatedDuration();

  /**
   * Estimated information about the ultimate payment outcome, as computed from the details discovered in {@link
   * ExchangeRateProbeOutcome}.
   *
   * @return A {@link EstimatedPaymentOutcome}.
   */
  EstimatedPaymentOutcome estimatedPaymentOutcome();
}
