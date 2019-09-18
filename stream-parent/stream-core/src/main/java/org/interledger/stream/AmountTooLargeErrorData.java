package org.interledger.stream;

import org.interledger.core.Immutable;

import com.google.common.primitives.UnsignedLong;

/**
 * An error returned by a Connector when a packet amount is too large.  Each of the following should be included as the
 * `data` field in an ILP Reject packet.
 */
@Immutable
public interface AmountTooLargeErrorData {

  static AmountTooLargeErrorDataBuilder builder() {
    return new AmountTooLargeErrorDataBuilder();
  }

  /**
   * Local amount received by the connector.
   *
   * @return A {@link UnsignedLong} with the received amount.
   */
  UnsignedLong receivedAmount();

  /**
   * Maximum amount (inclusive, denominated in same units as the receivedAmount) the connector will forward.
   *
   * @return A {@link UnsignedLong} with the maximum amount.
   */
  UnsignedLong maximumAmount();

}
