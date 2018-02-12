package org.interledger.node.services.fx;

import org.javamoney.moneta.spi.DefaultNumberValue;

import javax.money.CurrencyUnit;
import javax.money.NumberValue;
import javax.money.convert.ConversionContext;
import javax.money.convert.ExchangeRate;
import java.util.Arrays;
import java.util.List;

public class OneToOneExchangeRate implements ExchangeRate {

  private final CurrencyUnit baseCurrency;
  private final CurrencyUnit currency;

  public OneToOneExchangeRate(CurrencyUnit baseCurrency, CurrencyUnit currency) {
    this.baseCurrency = baseCurrency;
    this.currency = currency;
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
    return this.baseCurrency;
  }

  /**
   * Get the term (target) {@link CurrencyUnit}.
   *
   * @return the term {@link CurrencyUnit}.
   */
  @Override
  public CurrencyUnit getCurrency() {
    return this.currency;
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
   * @return the chain of rates.
   */
  @Override
  public List<ExchangeRate> getExchangeRateChain() {
    return Arrays.asList(this);
  }
}
