package org.interledger.stream.fx;

import static org.interledger.core.fluent.FluentCompareTo.is;

import org.interledger.fx.Denomination;

import com.google.common.base.Preconditions;
import com.google.common.primitives.UnsignedLong;

import java.math.BigDecimal;
import java.math.RoundingMode;

// TODO: Move to fx-core?
public interface ExchangeRateService {

  // TODO: Fnish Javadoc

  /**
   * @param senderDenomination
   * @param receiverDenomination * @param slippagePercent  A {@link BigDecimal} representing the maximum acceptable
   *                             slippage percentage below *                         calculated minimum exchange rate to
   *                             tolerate. Default slippage is 1.5% (or 0.015).
   *
   * @return
   */
  BigDecimal getScaledExchangeRate(
      Denomination senderDenomination, Denomination receiverDenomination, BigDecimal slippagePercent
  );

  /**
   * Convert the provided {@code sourceAmount} into a destination amount using the supplied {@code scaledExchangeRate}.
   *
   * @param sourceAmount       A {@link BigDecimal} representing the source amount (e.g., 1.0 USD).
   * @param scaledExchangeRate A {@link BigDecimal} representing the scaled exchange rate (e.g., 4,000,000.0)
   *
   * @return A {@link BigDecimal} containing a new amount that reprsents the {@code sourceAmount} converted into a
   *     destination amount using the supplied scaled rate.
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
