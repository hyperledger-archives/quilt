package org.interledger.core;

import org.interledger.core.ImmutableAmountTooLargeErrorData.Builder;

import com.google.common.primitives.UnsignedLong;
import org.immutables.value.Value.Immutable;

/**
 * An error returned by a Connector when a packet amount is too large.  Each of the following should be included as the
 * `data` field in an ILP Reject packet.
 */
@Immutable
public interface AmountTooLargeErrorData {

  static Builder builder() {
    return ImmutableAmountTooLargeErrorData.builder();
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
