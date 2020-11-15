package org.interledger.fx.javax.money.providers;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.Optional;
import javax.money.MonetaryException;
import javax.money.convert.ConversionQuery;
import javax.money.convert.ConversionQueryBuilder;
import javax.money.convert.ExchangeRate;
import javax.money.convert.RateType;
import okhttp3.OkHttpClient;
import org.assertj.core.data.Percentage;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Unit tests for {@link CryptoCompareRateProvider}.
 */
public class CryptoCompareRateProviderIT {

  private static final String CC_API_KEY_ENV_NAME = "CRYPTOCOMPARE_API_KEY";
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private CryptoCompareRateProvider provider;

  @Before
  public void setUp() {
    provider = new CryptoCompareRateProvider(
      () -> Optional.ofNullable(System.getenv(CC_API_KEY_ENV_NAME))
        .map(String::trim)
        .orElse(""),
      createObjectMapperForProblemsJson(),
      new OkHttpClient(),
      fxCache(Duration.of(30, ChronoUnit.SECONDS))
    );
  }

  @Test
  public void getExchangeRateWithUnknownCurrencyInCryptoCompare() {
    expectedException.expect(MonetaryException.class);
    expectedException.expectMessage("Unknown currency code: FOO");

    provider.getExchangeRate(
      ConversionQueryBuilder.of().setBaseCurrency("FOO").setTermCurrency("USD").setRateTypes(RateType.DEFERRED)
        .build()
    );
  }

  @Test
  public void getValidExchangeRate() {
    ExchangeRate rate = provider.getExchangeRate(
      ConversionQueryBuilder.of().setBaseCurrency("XRP").setTermCurrency("USD").setRateTypes(RateType.DEFERRED).build()
    );

    // When provided with an exchange rate, currency pairs indicate how much of the quote currency (or target currency
    // in JavaMoney) is needed to buy one unit of the base currency. So, we need ~0.25 USD to buy 1 unit of XRP.
    assertThat(rate.getBaseCurrency().getCurrencyCode()).isEqualTo("XRP");
    assertThat(rate.getCurrency().getCurrencyCode()).isEqualTo("USD");
    assertThat(rate.getFactor().numberValueExact(BigDecimal.class)).isGreaterThan(BigDecimal.ZERO);

    // Try inverted
    final ExchangeRate inverseRate = provider.getExchangeRate(
      ConversionQueryBuilder.of().setBaseCurrency("USD").setTermCurrency("XRP").setRateTypes(RateType.DEFERRED).build()
    );

    assertThat(inverseRate.getBaseCurrency().getCurrencyCode()).isEqualTo("USD");
    assertThat(inverseRate.getCurrency().getCurrencyCode()).isEqualTo("XRP");
    assertThat(inverseRate.getFactor().numberValueExact(BigDecimal.class)).isGreaterThan(BigDecimal.ZERO);

    assertThat(BigDecimal.ONE.divide(rate.getFactor().numberValueExact(BigDecimal.class), RoundingMode.UP))
      .isCloseTo(inverseRate.getFactor().numberValueExact(BigDecimal.class), Percentage
        .withPercentage(95));

    // Try rate again to make sure cache is working.
    rate = provider.getExchangeRate(
      ConversionQueryBuilder.of().setBaseCurrency("XRP").setTermCurrency("USD").setRateTypes(RateType.DEFERRED).build()
    );

    assertThat(rate.getBaseCurrency().getCurrencyCode()).isEqualTo("XRP");
    assertThat(rate.getCurrency().getCurrencyCode()).isEqualTo("USD");
    assertThat(rate.getFactor().numberValueExact(BigDecimal.class)).isGreaterThan(BigDecimal.ZERO);
  }

  //////////////////
  // Private Helpers
  //////////////////

  /**
   * Construct an {@link ObjectMapper} that can be used to serialize and deserialize.
   */
  private static ObjectMapper createObjectMapperForProblemsJson() {
    return new ObjectMapper()
      //.registerModule(new Jdk8Module())
      .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
      .configure(JsonGenerator.Feature.WRITE_NUMBERS_AS_STRINGS, false);
  }

  /**
   * Cache used for fx rate lookups.
   *
   * @param fxCacheTimeout A {@link Duration} containing the timeout of a cache entry≥
   * @return A {@link Cache}.
   */
  public Cache<ConversionQuery, ExchangeRate> fxCache(final Duration fxCacheTimeout) {
    Objects.requireNonNull(fxCacheTimeout);
    return Caffeine.newBuilder()
      .expireAfterWrite(fxCacheTimeout)
      .build(); // No default loading function.
  }
}
