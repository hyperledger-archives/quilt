package org.interledger.stream.good;

import com.google.common.primitives.UnsignedLong;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;
import org.immutables.value.Value.Default;
import org.interledger.core.Immutable;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.SharedSecret;
import org.interledger.fx.Denomination;

@Immutable
public interface SendMoneyRequest {

  static SendMoneyRequestBuilder builder() {
    return new SendMoneyRequestBuilder();
  }

  /**
   * A unique identifier for this Send Money request.
   *
   * @return A {@link UUID} that uniquely identifies this request.
   */
  @Default
  default UUID requestId() {
    return UUID.randomUUID();
  }

  /**
   * The shared secret, shared between only the sender and receiver, required by IL-RFC-29 to encrypt stream frames so
   * that only the sender and receiver can decrypt them.
   *
   * @return A {@link SharedSecret} known only to the sender and receiver, negotiated using some higher-level protocol
   * (e.g., SPSP or something else).
   */
  SharedSecret sharedSecret();

  /**
   * The optionally-supplied source address of this request. Senders are not required to send a source ILP address,
   * especially to support scenarios where the sender is not routable (e.g., client making sendMoney requests using an
   * ILP-over-HTTP link that has no incoming URL, such as from an Android device).
   *
   * @return The optionally-present {@link InterledgerAddress} of the source of this payment.
   */
  Optional<InterledgerAddress> senderAddress();

  /**
   * The {@link Denomination} of the sender of this payment.
   *
   * @return A {@link Denomination}.
   */
  Denomination senderDenomination();

  /**
   * The amount of units to send, in sender's units.
   *
   * @return An {@link UnsignedLong} containing the amount of this payment.
   */
  UnsignedLong senderAmount();

  /**
   * The destination address of this request.
   *
   * @return The {@link InterledgerAddress} of the receiver of this payment.
   */
  InterledgerAddress destinationAddress();

  /**
   * The {@link Denomination} of the receiver.
   *
   * @return A {@link UUID} that uniquely identifies this request.
   */
  Optional<Denomination> receiverDenomination();

  /**
   * The maximum acceptable slippage percentage (below the calculated exchange rate) that should be tolerated by this
   * payment. Slippage refers to the difference between the expected price of a trade and the price at which the trade
   * is executed, and can occur above or below an expected price. This implementation only enforces slippage that goes
   * against the sender. For example, if the caller would tolerate 10% slippage, then this value should be `0.1`.
   */
  // TODO: Consider making this just Zero by default. I thought that having this empty might mean "don't consider rates"
  //  and just pay up to the source amount, ignoring rates. In this world, the min amount would be 0 units. However,
  //  an alternate model would be where someone sets slippage to something they can tolerate, or else the slippage is 0,
  // which means don't tolerate rate slippage. By default this should probably be 0.015 (1.5%).
  Optional<BigDecimal> maxSlippagePercent();

  /**
   * <p>A {@link Duration} to wait before no longer scheduling any more requests to send stream packets for this
   * payment.</p>
   *
   * <p>This is an important distinction as compared to a traditional `hard timeout` where processing might end
   * immediately upon timeout. Instead, this timeout should be considered to be a `soft timeout`, because the sender
   * will wait for any in-flight packetized payments to be sent before exiting out of it run-loop.</p>
   *
   * @return A {@link Duration} of time to wait before terminating this payment.
   */
  Optional<Duration> paymentTimeout();

  /**
   * The default {@link Duration} to wait for any particular packet to fulfill or reject.
   *
   * @return A {@link Duration} of time to wait before rejecting a packet if no authoritative response was returned from
   * a peer.
   */
  default Duration perPacketTimeout() {
    return Duration.of(30, ChronoUnit.SECONDS);
  }
}
