package org.interledger.fx;

import static org.javamoney.moneta.spi.AbstractCurrencyConversion.KEY_SCALE;

import org.interledger.core.fluent.FluentCompareTo;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.javamoney.moneta.convert.ExchangeRateBuilder;
import org.javamoney.moneta.spi.AbstractRateProvider;
import org.javamoney.moneta.spi.DefaultNumberValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Objects;
import java.util.function.Function;

import javax.money.MonetaryException;
import javax.money.convert.ConversionContext;
import javax.money.convert.ConversionQuery;
import javax.money.convert.ExchangeRate;
import javax.money.convert.ExchangeRateProvider;
import javax.money.convert.ProviderContext;
import javax.money.convert.ProviderContextBuilder;
import javax.money.convert.RateType;

/**
 * A faux {@link ExchangeRateProvider} that loads faux FX data from memory for all identity rates, and for USD to XRP
 * rates.
 */
public class FauxRateProvider extends AbstractRateProvider {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private static final ProviderContext CONTEXT = ProviderContextBuilder.of("FAUX", RateType.ANY)
    .set("providerDescription", "Faux API Rate (USD, XRP only)")
    .build();

  private final Cache<ConversionQuery, ExchangeRate> exchangeRateCache;
  private final BigDecimal fauxFxRate;
  private final BigDecimal inverseFauxFxRate;

  /**
   * Required-args Constructor.
   *
   * @param fauxFxRate An {@link BigDecimal} that sets the FX rate for all calls.
   */
  public FauxRateProvider(final BigDecimal fauxFxRate) {
    this(fauxFxRate, Caffeine.newBuilder().build());
  }

  /**
   * Required-args Constructor.
   *
   * @param exchangeRateCache An {@link Cache} for exchange rates.
   */
  public FauxRateProvider(
    final BigDecimal fauxFxRate, final Cache<ConversionQuery, ExchangeRate> exchangeRateCache
  ) {
    super(CONTEXT);

    this.fauxFxRate = Objects.requireNonNull(fauxFxRate);
    this.inverseFauxFxRate = FluentCompareTo.is(fauxFxRate).lessThanOrEqualTo(BigDecimal.ZERO) ?
      BigDecimal.ONE :
      BigDecimal.ONE.divide(fauxFxRate, MathContext.DECIMAL128);

    this.exchangeRateCache = Objects.requireNonNull(exchangeRateCache);

    logger.info("USD to XRP Faux Rate: {}", this.fauxFxRate);
    logger.info("XRP-to-USD Faux Rate: {}", this.inverseFauxFxRate);
  }

  // Access a {@link ExchangeRate} using the given currencies.
  @Override
  public ExchangeRate getExchangeRate(ConversionQuery conversionQuery) {
    Objects.requireNonNull(conversionQuery);
    try {
      // NOTE: Interface contract says "never-null" but all implementations return null. :(
      return this.exchangeRateCache.get(conversionQuery, fxLoader());
    } catch (Exception e) {
      throw new MonetaryException("Failed to load currency conversion data", e);
    }
  }

  private ExchangeRateBuilder exchangeRateBuilder(ConversionQuery query) {
    ExchangeRateBuilder builder = new ExchangeRateBuilder(getExchangeContext("cc.digit.fraction"));
    builder.setBase(query.getBaseCurrency());
    builder.setTerm(query.getCurrency());
    return builder;
  }

  @Override
  protected ConversionContext getExchangeContext(String key) {
    int scale = getScale(key);
    if (scale < 0) {
      return ConversionContext.of(CONTEXT.getProviderName(), RateType.DEFERRED);
    } else {
      return ConversionContext.of(CONTEXT.getProviderName(), RateType.DEFERRED).toBuilder()
        .set(KEY_SCALE, scale)
        .build();
    }
  }

  /**
   * A {@link Function} that can be used by the FX Cache as a "mapping function" in order to load data into the cache in
   * the event of a cache-miss.
   *
   * @return A {@link Function} that accepts a {@link ConversionQuery} and returns an {@link ExchangeRate}.
   */
  private Function<ConversionQuery, ExchangeRate> fxLoader() {
    return (conversionQuery) -> {
      // Computes or retrieves the value corresponding to {@code conversionQuery}.
      Objects.requireNonNull(conversionQuery);

      final ExchangeRateBuilder builder = exchangeRateBuilder(conversionQuery);

      final String baseCurrencyCode = conversionQuery.getBaseCurrency().getCurrencyCode()
        .toUpperCase();
      final String terminatingCurrencyCode = conversionQuery.getCurrency().getCurrencyCode()
        .toUpperCase();

      if (baseCurrencyCode.equals(terminatingCurrencyCode)) {
        builder.setFactor(DefaultNumberValue.ONE);
      } else {
        // In JavaMoney, the Base currency is the currency being dealt with, and the terminating currency is
        // the currency that the base is converted into. E.g., `XRP, in USD, is $0.3133`, then XRP would be the
        // base currency, and USD would be the terminating currency. In CryptoCompare, the `fsym` and `tsym`
        // map this relationship. We ask the API, convert `XRP` (fsym) into `USD` (tsym). We get a response
        // containing a map of values keyed by each `tsym`. So, we can map the `tsym` to the terminating currency.
        if (baseCurrencyCode.equalsIgnoreCase("XRP") && terminatingCurrencyCode.equalsIgnoreCase("USD")) {
          builder.setFactor(new DefaultNumberValue(fauxFxRate));
        } else if (baseCurrencyCode.equalsIgnoreCase("USD") && terminatingCurrencyCode.equalsIgnoreCase("XRP")) {
          builder.setFactor(new DefaultNumberValue(inverseFauxFxRate));
        } else {
          throw new ExchangeRateException("This provider only supports XRP:USD or Identity rates.");
        }
      }

      // If everything works, return a constructed ExchangeRateBuilder.
      return builder.build();

    };
  }
}
