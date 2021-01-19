package org.interledger.fx;

import static org.interledger.core.fluent.FluentCompareTo.is;

import com.google.common.base.Preconditions;
import com.google.common.primitives.UnsignedLong;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * A service for providing Exchange Rates from an external oracle, for use in comparing actual payment path rates to a
 * known/trusted external source.
 */
public interface ExchangeRateService {

  /**
   * Obtain a scaled exchange rate from a source account denomination to a destination account denomination. For
   * example, if a source account has an assetScale of 2, and a destination account has an asset scale of 0, then the
   * scaled excahnge rate would be 200.
   *
   * @param sourceDenomination      A {@link Denomination} for the source account sending a payment.
   * @param destinationDenomination The {@link Denomination} for the destination account receiving a payment.
   * @param slippagePercent         A {@link BigDecimal} representing the maximum acceptable slippage percentage below a
   *                                calculated minimum exchange rate to tolerate.
   *
   * @return A {@link BigDecimal} representing a scaled FX rate.
   */
  BigDecimal getScaledExchangeRate(
    Denomination sourceDenomination, Denomination destinationDenomination, BigDecimal slippagePercent
  );

  /**
   * Convert the provided {@code sourceAmount} into a destination amount using the supplied {@code scaledExchangeRate}.
   *
   * @param sourceAmount       A {@link BigDecimal} representing the source amount (e.g., 1.0 USD).
   * @param scaledExchangeRate A {@link BigDecimal} representing the scaled exchange rate (e.g., 4,000,000.0)
   *
   * @return A {@link BigDecimal} containing a new amount that reprsents the {@code sourceAmount} converted into a
   *   destination amount using the supplied scaled rate.
   */
  default UnsignedLong convert(UnsignedLong sourceAmount, BigDecimal scaledExchangeRate) {
    Preconditions.checkArgument(
      is(sourceAmount).greaterThanEqualTo(UnsignedLong.ZERO), "sourceAmount must not be negative"
    );
    Preconditions.checkArgument(
      is(scaledExchangeRate).greaterThanEqualTo(BigDecimal.ZERO), "scaledExchangeRate must not be negative"
    );

    // Apply exchange rate
    final BigDecimal destinationAmount = new BigDecimal(sourceAmount.bigIntegerValue()).multiply(scaledExchangeRate);

    // Round up for safety
    return UnsignedLong.valueOf(destinationAmount.setScale(0, RoundingMode.CEILING)
      .toBigIntegerExact()); // Assumes sourceAmount is non-negative.
  }
}