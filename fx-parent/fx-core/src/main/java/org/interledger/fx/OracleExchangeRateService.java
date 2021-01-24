package org.interledger.fx;

import static org.interledger.core.fluent.FluentCompareTo.is;

import org.interledger.core.fluent.FluentBigInteger;
import org.interledger.core.fluent.Ratio;

import com.google.common.base.Preconditions;
import com.google.common.primitives.UnsignedLong;

import java.math.BigDecimal;
import java.math.BigInteger;

import javax.money.convert.ExchangeRate;

/**
 * A service for providing Exchange Rates from an external oracle, for use in comparing actual payment path rates to a
 * known/trusted external source.
 */
public interface OracleExchangeRateService {

  /**
   * Obtain a scaled exchange rate from a source account denomination to a destination account denomination. For
   * example, if a source account has an assetScale of 2, and a destination account has an asset scale of 0, then the
   * scaled exchange rate would be 200.
   *
   * @param sourceDenomination      A {@link Denomination} for the source account sending a payment.
   * @param destinationDenomination The {@link Denomination} for the destination account receiving a payment.
   * @param slippage                A {@link Slippage} containing the maximum acceptable slippage percentage below which
   *                                a calculated minimum exchange rate will be tolerated.
   *
   * @return A {@link ScaledExchangeRate} representing a scaled FX rate.
   */
  ScaledExchangeRate getScaledExchangeRate(
    Denomination sourceDenomination, Denomination destinationDenomination, Slippage slippage
  );

  /**
   * Obtain the foreign exchange-rate from a source denomination to a destination denomination.
   *
   * @param sourceDenomination      The {@link Denomination} for the source account.
   * @param destinationDenomination The {@link Denomination} for the destination account.
   *
   * @return An {@link ExchangeRate}.
   */
  ExchangeRate getExchangeRate(Denomination sourceDenomination, Denomination destinationDenomination);

  /**
   * Convert the provided {@code sourceAmount} into a destination amount using the supplied {@code scaledExchangeRate}.
   *
   * @param sourceAmount       A {@link BigDecimal} representing the source amount (e.g., 1.0 USD).
   * @param scaledExchangeRate A {@link ScaledExchangeRate} representing the scaled exchange rate (e.g., 4,000,000 or
   *                           0.00025)
   *
   * @return A {@link BigDecimal} containing a new amount that represents the {@code sourceAmount} converted into a
   *   destination amount using the supplied scaled rate.
   */
  default UnsignedLong convert(UnsignedLong sourceAmount, ScaledExchangeRate scaledExchangeRate) {
    Preconditions.checkArgument(
      is(sourceAmount).greaterThanEqualTo(UnsignedLong.ZERO), "sourceAmount must not be negative"
    );
    Preconditions.checkArgument(
      is(scaledExchangeRate.value()).greaterThanEqualTo(Ratio.ZERO), "scaledExchangeRate must not be negative"
    );

    // Apply exchange rate
    final BigInteger destinationAmount = scaledExchangeRate.value().multiplyCeil(sourceAmount.bigIntegerValue());
    return FluentBigInteger.of(destinationAmount).orMaxUnsignedLong();
  }
}