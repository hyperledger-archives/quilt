package org.interledger.fx;

import org.interledger.core.fluent.Ratio;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Objects;

import javax.money.convert.ExchangeRate;
import javax.money.convert.ExchangeRateProvider;

/**
 * The default implementation of {@link OracleExchangeRateService}.
 */
public class DefaultOracleExchangeRateService implements OracleExchangeRateService {

  private final ExchangeRateProvider oracleExchangeRateProvider;

  /**
   * Required-args Constructor.
   *
   * @param oracleExchangeRateProvider A {@link ExchangeRateProvider}.
   */
  public DefaultOracleExchangeRateService(final ExchangeRateProvider oracleExchangeRateProvider) {
    this.oracleExchangeRateProvider = Objects.requireNonNull(oracleExchangeRateProvider);
  }

  @Override
  public ScaledExchangeRate getScaledExchangeRate(
    final Denomination sourceDenomination, final Denomination destinationDenomination, final Slippage slippage
  ) {
    Objects.requireNonNull(sourceDenomination);
    Objects.requireNonNull(destinationDenomination);
    Objects.requireNonNull(slippage);

    final BigDecimal exchangeRateAsBigDecimal = this.getExchangeRate(sourceDenomination, destinationDenomination)
      .getFactor()
      .numberValueExact(BigDecimal.class);

    BigDecimal rateWithSlippage = exchangeRateAsBigDecimal.multiply(BigDecimal.ONE.subtract(slippage.value().value()));

    // Scale rate based on source scale
    rateWithSlippage = rateWithSlippage.divide(
      BigDecimal.TEN.pow(sourceDenomination.assetScale()), MathContext.DECIMAL128
    );

    // Scale rate based on destination scale
    rateWithSlippage = rateWithSlippage.multiply(BigDecimal.TEN.pow(destinationDenomination.assetScale()));

    return ScaledExchangeRate.builder()
      .originalSourceScale(sourceDenomination.assetScale())
      .originalDestinationScale(destinationDenomination.assetScale())
      .value(Ratio.from(rateWithSlippage))
      .slippage(slippage)
      .build();
  }

  @Override
  public ExchangeRate getExchangeRate(final Denomination sourceAssetCode, final Denomination destinationAssetCode) {
    return this.oracleExchangeRateProvider
      .getExchangeRate(sourceAssetCode.assetCode(), destinationAssetCode.assetCode());
  }
}
