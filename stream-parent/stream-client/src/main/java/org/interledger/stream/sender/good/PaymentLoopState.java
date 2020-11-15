package org.interledger.stream.sender.good;

/**
 * The states that the payment can be in while processing through the payment run-loop.
 */
public enum PaymentLoopState {
  /**
   * The payment has encountered too many "reject" packets, and should be terminated (i.e., wait for any remaining
   * packets to finish), or, a single unrecoverable error has been encountered. In this state, no new packets should be
   * sent, and any in-flight packets should be waited for.
   */
  FAIL_FAST,
  /**
   * The amount of "in-flight" payments waiting to either succeed or fail has reached the link's maximum amount
   * in-flight setting. Therefore, no packets should be scheduled until some responses come in.
   */
  MAX_IN_FLIGHT,
  /**
   * The payment has been processing for too long, and should be gracefully terminated (i.e., wait for any remaining
   * packets to finish).
   */
  PAYMENT_TIMED_EXCEEDED,
  /**
   * Wait for all pending requests to complete before closing the connection
   */
  CONNECTION_CLOSABLE,
  /**
   * There is more to send on this payment, so try sending more packets.
   */
  MORE_TO_SEND,
  /**
   * There is no more to send (amountLeftToSend is 0) but the deliveredAmount is not yet large enough to complete the
   * payment.
   */
  WAIT_AND_SEE,
}
