package org.interledger.stream.pay.model;

import org.interledger.stream.pay.filters.StreamPacketFilter;

/**
 * A state as signaled by a given {@link StreamPacketFilter}.
 */
public enum SendState {

  /**
   * Ready to send money and apply the next ILP Prepare
   */
  Ready,
  /**
   * Temporarily pause sending money until any request finishes or some time elapses
   */
  Wait,
  /**
   * Stop the payment
   */
  End,

  ////////////////////////////////////////////
  // Errors likely caused by the library user
  ////////////////////////////////////////////

  /**
   * Payment pointer is formatted incorrectly
   */
  InvalidPaymentPointer,
  /**
   * STREAM credentials (shared secret and destination address) were not provided or invalid
   */
  InvalidCredentials,
  /**
   * Plugin failed to connect or is disconnected from the Interleder network
   */
  Disconnected,
  /**
   * Slippage percentage is not between 0 and 1 (inclusive)
   */
  InvalidSlippage,
  /**
   * Sender and receiver use incompatible Interledger network prefixes
   */
  IncompatibleInterledgerNetworks,
  /**
   * Failed to fetch IL-DCP details for the source account: unknown sending asset or ILP address
   */
  UnknownSourceAsset,
  /**
   * No fixed source amount or fixed destination amount was provided
   */
  UnknownPaymentTarget,
  /**
   * Fixed source amount is invalid or too precise for the source account
   */
  InvalidSourceAmount,
  /**
   * Fixed delivery amount is invalid or too precise for the destination account
   */
  InvalidDestinationAmount,
  /**
   * Minimum exchange rate is 0 after subtracting slippage, and cannot enforce a fixed-delivery payment
   */
  UnenforceableDelivery,

  ////////////////////////////////////////////
  // Errors likely caused by the receiver, connectors, or other externalities
  ////////////////////////////////////////////

  /**
   * Failed to query an account or invoice from an Open Payments or SPSP server
   */
  QueryFailed,
  /**
   * Invoice is complete: amount paid into the invoice already meets or exceeds the invoice amount
   */
  InvoiceAlreadyPaid,
  /**
   * Failed to fetch the external exchange rate and unable to enforce a minimum exchange rate
   */
  ExternalRateUnavailable,
  /**
   * Probed exchange rate is too low: less than the minimum pulled from external rate APIs
   */
  InsufficientExchangeRate,
  /**
   * Destination asset details are unknown or the receiver never provided them
   */
  UnknownDestinationAsset,
  /**
   * Receiver sent conflicting destination asset details
   */
  DestinationAssetConflict,
  /**
   * Receiver's advertised limit is incompatible with the amount we want to send or deliver to them
   */
  IncompatibleReceiveMax,
  /**
   * The recipient closed the connection or stream, terminating the payment
   */
  ClosedByRecipient,
  /**
   * Receiver violated the STREAM protocol that prevented accounting for delivered amounts
   */
  ReceiverProtocolViolation,
  /**
   * Rate probe failed to establish the realized exchange rate
   */
  RateProbeFailed,
  /**
   * Failed to fulfill a packet before payment timed out
   */
  IdleTimeout,
  /**
   * Encountered an ILP Reject that cannot be retried, or the payment is not possible over this path
   */
  ConnectorError,
  /**
   * Sent too many packets with this encryption key and must close the connection
   */
  ExceededMaxSequence,
  /**
   * Rate enforcement is not possible due to rounding: max packet amount may be too low, minimum exchange rate may
   * require more slippage, or exchange rate may be insufficient
   */
  ExchangeRateRoundingError;

  /**
   * Inidicates if this SendState is a payment error or not.
   *
   * @return {@code true} if the SendState is {@link #Ready}, {@link #Wait}, or {@link #End}; Otherwise {@code false}.
   */
  public boolean isPaymentError() {
    return !(this == SendState.Ready || this == SendState.Wait || this == SendState.End);
  }

}
