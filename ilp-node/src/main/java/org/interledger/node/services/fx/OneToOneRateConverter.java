package org.interledger.node.services.fx;

import static java.time.temporal.ChronoUnit.SECONDS;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.TemporalAmount;

public class OneToOneRateConverter implements RateConverter {

  private final TemporalAmount expiryMargin;

  public OneToOneRateConverter() {
    this.expiryMargin = Duration.of(1, SECONDS);
  }

  public OneToOneRateConverter(TemporalAmount expiryMargin) {
    this.expiryMargin = expiryMargin;
  }

  @Override
  public ConversionResult convert(long sourceAmount, Instant sourceExpiry) {
    return ConversionResult.builder()
        .amount(sourceAmount)
        .expiry(sourceExpiry.minus(expiryMargin))
        .build();
  }
}
