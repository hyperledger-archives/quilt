package org.interledger.stream;

import com.google.common.primitives.UnsignedLong;
import org.interledger.core.Immutable;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.SharedSecret;
import org.interledger.stream.calculators.ExchangeRateCalculator;

import java.time.Duration;
import java.util.Optional;

@Immutable
public interface SendMoneyRequest {

  static SendMoneyRequestBuilder builder() { return new SendMoneyRequestBuilder(); }

  /**
   *
   * @return A {@link SharedSecret} known only to the sender and receiver, negotiated using
   *         some higher-level protocol (e.g., SPSP or something else).
   */
  SharedSecret sharedSecret();

  /**
   *
   * @return The {@link InterledgerAddress} of the source of this payment.
   */
  InterledgerAddress sourceAddress();

  /**
   *
   * @return The {@link InterledgerAddress} of the receiver of this payment.
   */
  InterledgerAddress destinationAddress();

  /**
   *
   * @return An {@link UnsignedLong} containing the amount of this payment.
   */
  UnsignedLong amount();

  /**
   *
   * @return
   */
  Denomination denomination();

  /**
   *
   * @return A {@link Duration} to wait before no longer scheduling any off more requests
   *         to send stream packets for this payment. This is an important distinction as
   *         compared to a traditional `hard timeout` where processing might end
   *         immediately upon timeout. Instead, this timeout should be considered to be a
   *         `soft timeout`, because the sender will wait for any in-flight packetized
   *         payments to be sent before exiting out of it run-loop.
   */
  Optional<Duration> timeout();

  /**
   *
   * @return a calculator to figure out how much money should arrive on the other side of a given payment.
   */
  ExchangeRateCalculator exchangeRateCalculator();

}
