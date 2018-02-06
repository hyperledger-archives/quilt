package org.interledger.node.services.fx;

import static java.time.temporal.ChronoUnit.SECONDS;

import org.javamoney.moneta.Money;
import org.javamoney.moneta.spi.DefaultNumberValue;

import javax.money.CurrencyUnit;
import javax.money.NumberValue;
import javax.money.convert.ConversionContext;
import javax.money.convert.ExchangeRate;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.TemporalAmount;
import java.util.Arrays;
import java.util.List;

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

  /**
   * Access the {@link ConversionContext} of {@link ExchangeRate}.
   *
   * @return the conversion context, never null.
   */
  @Override
  public ConversionContext getContext() {
    return ConversionContext.ANY_CONVERSION;
  }

  /**
   * Get the base (source) {@link CurrencyUnit}.
   *
   * @return the base {@link CurrencyUnit}.
   */
  @Override
  public CurrencyUnit getBaseCurrency() {
    return null;
  }

  /**
   * Get the term (target) {@link CurrencyUnit}.
   *
   * @return the term {@link CurrencyUnit}.
   */
  @Override
  public CurrencyUnit getCurrency() {
    return null;
  }

  /**
   * Access the rate's bid factor.
   *
   * @return the bid factor for this exchange rate, or {@code null}.
   */
  @Override
  public NumberValue getFactor() {
    return DefaultNumberValue.ONE;
  }

  /**
   * Access the chain of exchange rates.
   *
   * @return the chain of rates, in case of a derived rate, this may be
   * several instances. For a direct exchange rate, this equals to
   * <code>new ExchangeRate[]{this}</code>.
   */
  @Override
  public List<ExchangeRate> getExchangeRateChain() {
    return Arrays.asList(this);
  }
}
