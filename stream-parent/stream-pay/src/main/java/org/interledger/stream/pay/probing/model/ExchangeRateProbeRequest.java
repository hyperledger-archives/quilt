package org.interledger.stream.pay.probing.model;

import org.interledger.core.InterledgerAddress;
import org.interledger.stream.crypto.SharedSecret;
import org.interledger.stream.pay.model.ImmutableQuoteRequest;
import org.interledger.stream.pay.model.ImmutableQuoteRequest.Builder;

import org.immutables.value.Value.Immutable;

// TODO: Use StreamConnection instead.
@Deprecated
@Immutable
public interface ExchangeRateProbeRequest {

  static Builder builder() {
    return ImmutableQuoteRequest.builder();
  }

  /**
   * The optionally-supplied source address of this request. Senders are not required to send a source ILP address,
   * especially to support scenarios where the sender is not routable (e.g., client making sendMoney requests using an
   * ILP-over-HTTP link that has no incoming URL, such as from an Android device).
   *
   * @return The optionally-present {@link InterledgerAddress} of the source of this payment.
   */
  // Optional<InterledgerAddress> sourceAddress();

  /**
   * The shared secret, shared between only the sender and receiver, required by IL-RFC-29 to encrypt stream frames so
   * that only the sender and receiver can decrypt them.
   *
   * @return A {@link SharedSecret} known only to the sender and receiver, negotiated using some higher-level protocol
   *     (e.g., SPSP or something else).
   */
  //SharedSecret sharedSecret();

  /**
   * The destination address of this request.
   *
   * @return The {@link InterledgerAddress} of the receiver of this payment.
   */
  // InterledgerAddress destinationAddress();

}
