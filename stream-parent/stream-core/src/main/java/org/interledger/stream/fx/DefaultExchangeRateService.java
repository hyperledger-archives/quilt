package org.interledger.stream.fx;

import com.google.common.annotations.VisibleForTesting;
import java.math.BigDecimal;
import java.util.Objects;
import javax.money.NumberValue;
import javax.money.convert.ExchangeRateProvider;
import org.interledger.fx.Denomination;

// TODO: JavaDoc
// TODO: Move to fx-core, plus tests
public class DefaultExchangeRateService implements ExchangeRateService {

  private final ExchangeRateProvider exchangeRateProvider;

  public DefaultExchangeRateService(ExchangeRateProvider exchangeRateProvider) {
    this.exchangeRateProvider = exchangeRateProvider;
  }

  @Override
  public BigDecimal getScaledExchangeRate(
    final Denomination senderDenomination, final Denomination receiverDenomination, final BigDecimal slippagePercent
  ) {
    Objects.requireNonNull(senderDenomination);
    Objects.requireNonNull(receiverDenomination);
    Objects.requireNonNull(slippagePercent);

    final BigDecimal exchangeRate = getExchangeRate(senderDenomination.assetCode(), receiverDenomination.assetCode());
    BigDecimal rateWithSlippage = exchangeRate.multiply(BigDecimal.ONE.subtract(slippagePercent));

    // Scale rate based on source scale
    rateWithSlippage = rateWithSlippage.divide(BigDecimal.TEN.pow(senderDenomination.assetScale()));

    // Scale rate based on destination scale
    rateWithSlippage = rateWithSlippage.multiply(BigDecimal.TEN.pow(receiverDenomination.assetScale()));

    return rateWithSlippage;
  }

  @VisibleForTesting
  BigDecimal getExchangeRate(
    final String sourceAsssetCode,
    final String destinationoAssetCode
  ) {
    NumberValue rate = this.exchangeRateProvider.getExchangeRate(sourceAsssetCode, destinationoAssetCode)
      .getFactor();

    return rate.numberValueExact(BigDecimal.class);
  }
}
