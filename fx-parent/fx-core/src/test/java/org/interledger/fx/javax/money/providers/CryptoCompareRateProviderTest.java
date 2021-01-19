package org.interledger.fx.javax.money.providers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.collect.Maps;
import java.io.IOException;
import java.util.Map;
import javax.money.MonetaryException;
import javax.money.convert.ConversionQuery;
import javax.money.convert.ConversionQueryBuilder;
import javax.money.convert.ExchangeRate;
import javax.money.convert.RateType;
import okhttp3.OkHttpClient;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Unit tests for {@link CryptoCompareRateProvider}.
 */
public class CryptoCompareRateProviderTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Mock
  private OkHttpClient okHttpClientMock;

  @Mock
  private ObjectMapper objectMapperMock;

  @Mock
  private Cache<ConversionQuery, ExchangeRate> cacheMock;

  private Map<String, String> ratesResponseMap;

  private CryptoCompareRateProvider provider;

  @Before
  public void setUp() throws IOException {
    MockitoAnnotations.openMocks(this);

    // Most tests use a real cache unless a test decides to explicitly mock the cache for a specific purposes to
    // isolate the loading function out of the test.
    this.provider = new CryptoCompareRateProvider(
      () -> "apiKey", objectMapperMock, okHttpClientMock, Caffeine.newBuilder().build()
    );
    this.ratesResponseMap = Maps.newHashMap();

//    when(objectMapperMock.readValue(Mockito.<InputStream>any(), Mockito.<TypeReference>any()))
//      .thenReturn(ratesResponseMap);

    //when(okHttpClientMock.newCall(any())).thenReturn()
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
  public void getExchangeRateWithNoRateFoundInCryptoCompare() {
    expectedException.expect(MonetaryException.class);
    expectedException.expectMessage("Failed to load currency conversion data");

    provider.getExchangeRate(
      ConversionQueryBuilder.of().setBaseCurrency("XRP").setTermCurrency("USD").setRateTypes(RateType.DEFERRED)
        .build()
    );
  }

  @Test
  public void getExchangeRateWhenRateExistsInCache() {
    ExchangeRate exchangeRateMock = mock(ExchangeRate.class);
    when(cacheMock.get(any(), any())).thenReturn(exchangeRateMock);
    this.provider = new CryptoCompareRateProvider(() -> "apiKey", objectMapperMock, okHttpClientMock, cacheMock);

    final ExchangeRate actual = provider.getExchangeRate(
      ConversionQueryBuilder.of().setBaseCurrency("XRP").setTermCurrency("USD").setRateTypes(RateType.DEFERRED)
        .build()
    );

    assertThat(actual).isEqualTo(exchangeRateMock);
  }
}
