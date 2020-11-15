package org.interledger.stream.pay.model;

import org.interledger.stream.pay.model.ImmutableExchangeRateBound.Builder;

import com.google.common.primitives.UnsignedLong;
import org.immutables.value.Value.Immutable;

/**
 * The details about an Interledger Account, which is one side of a trustline between two parties.
 */
@Immutable
public interface ExchangeRateBound {

  static Builder builder() {
    return ImmutableExchangeRateBound.builder();
  }

  UnsignedLong lowEndEstimate();

  UnsignedLong highEndEstimate();

}
