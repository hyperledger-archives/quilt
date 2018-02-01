package org.interledger.node.services.fx;

import org.interledger.annotations.Immutable;

import java.time.Instant;

@Immutable
public interface ConversionResult {

  static ConversionResultBuilder builder() {
    return new ConversionResultBuilder();
  }

  long getAmount();
  Instant getExpiry();

}
