package org.interledger.stream.pay.model;

import org.interledger.core.InterledgerAddress;
import org.interledger.core.SharedSecret;;
import org.interledger.fx.Denomination;

import com.google.common.primitives.UnsignedLong;
import org.immutables.value.Value.Check;
import org.immutables.value.Value.Default;
import org.immutables.value.Value.Immutable;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Immutable
public interface PaymentDetails {

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
   *     (e.g., SPSP or something else).
   */
  SharedSecret sharedSecret();

  /**
   * The optionally-supplied source address of this request. Senders are not required to send a source ILP address,
   * especially to support scenarios where the sender is not routable (e.g., client making sendMoney requests using an
   * ILP-over-HTTP link that has no incoming URL, such as from an Android device).
   *
   * @return The optionally-present {@link InterledgerAddress} of the source of this payment.
   */
  Optional<InterledgerAddress> sourceAddress();

  /**
   * The destination address of this request.
   *
   * @return The {@link InterledgerAddress} of the receiver of this payment.
   */
  InterledgerAddress destinationAddress();

  /**
   * The amount of units to send.
   *
   * @return An {@link UnsignedLong} containing the amount of this payment.
   */
  UnsignedLong amount();

  /**
   * The {@link Denomination} of the amount in this request.
   *
   * @return A {@link Denomination}.
   */
  Denomination denomination();

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
  Optional<Duration> timeout();

  /**
   * Precondition checks before an instance of this immutable is allowed to be constructed.
   *
   * @return this instance
   */
  @Check
  default PaymentDetails check() {
    return this;
  }
}
