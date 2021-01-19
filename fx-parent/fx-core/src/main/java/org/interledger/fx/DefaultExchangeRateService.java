package org.interledger.fx;

import com.google.common.annotations.VisibleForTesting;

import java.math.BigDecimal;
import java.util.Objects;

import javax.money.convert.ExchangeRateProvider;

/**
 * The default implementation of {@link ExchangeRateService}.
 */
public class DefaultExchangeRateService implements ExchangeRateService {

  private final ExchangeRateProvider exchangeRateProvider;

  public DefaultExchangeRateService(ExchangeRateProvider exchangeRateProvider) {
    this.exchangeRateProvider = exchangeRateProvider;
  }

  @Override
  public BigDecimal getScaledExchangeRate(
    final Denomination sourceDenomination, final Denomination destinationDenomination, final BigDecimal slippagePercent
  ) {
    Objects.requireNonNull(sourceDenomination);
    Objects.requireNonNull(destinationDenomination);
    Objects.requireNonNull(slippagePercent);

    final BigDecimal exchangeRate = getExchangeRate(sourceDenomination.assetCode(),
      destinationDenomination.assetCode());
    BigDecimal rateWithSlippage = exchangeRate.multiply(BigDecimal.ONE.subtract(slippagePercent));

    // Scale rate based on source scale
    rateWithSlippage = rateWithSlippage.divide(BigDecimal.TEN.pow(sourceDenomination.assetScale()));

    // Scale rate based on destination scale
    rateWithSlippage = rateWithSlippage.multiply(BigDecimal.TEN.pow(destinationDenomination.assetScale()));

    return rateWithSlippage;
  }

  @VisibleForTesting
  BigDecimal getExchangeRate(final String sourceAsssetCode, final String destinationoAssetCode) {
    return this.exchangeRateProvider
      .getExchangeRate(sourceAsssetCode, destinationoAssetCode)
      .getFactor()
      .numberValueExact(BigDecimal.class);
  }
}
