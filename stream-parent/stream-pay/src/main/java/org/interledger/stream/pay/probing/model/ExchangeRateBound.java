package org.interledger.stream.pay.probing.model;

import com.google.common.primitives.UnsignedLong;
import org.immutables.value.Value.Immutable;

@Immutable
public interface ExchangeRateBound {

  static ImmutableExchangeRateBound.Builder builder() {
    return ImmutableExchangeRateBound.builder();
  }

  UnsignedLong lowEndEstimate();

  UnsignedLong highEndEstimate();

}
