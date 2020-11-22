package org.interledger.fx.javax.money.providers;

import static org.javamoney.moneta.spi.AbstractCurrencyConversion.KEY_SCALE;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.google.common.net.HttpHeaders;
import com.google.common.net.MediaType;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.money.MonetaryException;
import javax.money.convert.ConversionContext;
import javax.money.convert.ConversionQuery;
import javax.money.convert.ExchangeRate;
import javax.money.convert.ExchangeRateProvider;
import javax.money.convert.ProviderContext;
import javax.money.convert.ProviderContextBuilder;
import javax.money.convert.RateType;
import okhttp3.HttpUrl.Builder;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.interledger.fx.ExchangeRateException;
import org.javamoney.moneta.convert.ExchangeRateBuilder;
import org.javamoney.moneta.spi.AbstractRateProvider;
import org.javamoney.moneta.spi.DefaultNumberValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link ExchangeRateProvider} that loads FX data from CryptoCompare. This provider loads all available rates,
 * purging from its cache any older rates with each re-load.
 *
 * @see "https://min-api.cryptocompare.com/documentation"
 * @see "https://github.com/JavaMoney/javamoney-lib/blob/master/exchange/exchange-rate-frb/src/main/java/org/javamoney/
 * moneta/convert/frb/USFederalReserveRateProvider.java"
 */
public class CryptoCompareRateProvider extends AbstractRateProvider {

  private static final Logger logger = LoggerFactory.getLogger(CryptoCompareRateProvider.class.getName());

  private static final TypeReference<Map<String, String>> MAP_TYPE_REFERENCE
    = new TypeReference<Map<String, String>>() {
  };

  /**
   * The HTTP {@code Authorization} header field name.
   *
   * @see <a href="https://tools.ietf.org/html/rfc7235#section-4.2">Section 4.2 of RFC 7235</a>
   */
  private static final String AUTHORIZATION = "Authorization";

  private static final ProviderContext CONTEXT = ProviderContextBuilder.of("CC", RateType.DEFERRED)
    .set("providerDescription", "CryptoCompare API Rate (https://min-api.cryptocompare.com)")
    .build();

  private final Supplier<String> apiKeySupplier;
  private final ObjectMapper objectMapper;
  private final OkHttpClient okHttpClient;
  private final Cache<ConversionQuery, ExchangeRate> exchangeRateCache;

  public CryptoCompareRateProvider(
    final Supplier<String> apiKeySupplier,
    final ObjectMapper objectMapper,
    final OkHttpClient okHttpClient,
    final Cache<ConversionQuery, ExchangeRate> exchangeRateCache
  ) {
    super(CONTEXT);
    this.apiKeySupplier = Objects.requireNonNull(apiKeySupplier);
    this.objectMapper = Objects.requireNonNull(objectMapper);
    this.okHttpClient = Objects.requireNonNull(okHttpClient);
    this.exchangeRateCache = Objects.requireNonNull(exchangeRateCache);
  }

  // The spread is subtracted from the rate when going in either direction,
  // so that the DestinationAmount always ends up being slightly less than
  // the (equivalent) SourceAmount -- regardless of which of the 2 is fixed:
  //
  //   SourceAmount * Rate * (1 - Spread) = DestinationAmount
  //
  //    const rate = new BigNumber(destinationRate).shiftedBy(destinationInfo.assetScale)
  //      .div(new BigNumber(sourceRate).shiftedBy(sourceInfo.assetScale))
  //    .times(new BigNumber(1).minus(this.spread))
  //    .toPrecision(15)
  // '0.005'` Set the connector's spread to 0.5%. This is an example for how to pass configuration to the connector.

  // Access a {@link ExchangeRate} using the given currencies.
  @Override
  public ExchangeRate getExchangeRate(ConversionQuery conversionQuery) {
    Objects.requireNonNull(conversionQuery);
    try {
      // TODO: Interface contract says "never-null" but all implementations return null. :(
      return this.exchangeRateCache.get(conversionQuery, fxLoader());
    } catch (Exception e) {
      throw new MonetaryException("Failed to load currency conversion data", e);
    }
  }

  private ExchangeRateBuilder exchangeRateBuilder(ConversionQuery query) {
    // TODO: This maps to scale, but shouldn't be hard-coded to "6" in javamoney.properties.
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

      // WARNING: CryptoCompare will fail if the currency codes aren't upper-cased!
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

        final Request okHttpRequest = new Request.Builder()
          // TODO: Keep?
          .header(HttpHeaders.ACCEPT, MediaType.JSON_UTF_8.toString())
          // Authorization: Apikey {your_api_key}=
          .header(AUTHORIZATION, String.format("Apikey %s", apiKeySupplier.get()))
          .url(new Builder()
            .scheme("https")
            .host("min-api.cryptocompare.com")
            .addPathSegments("data/price")
            .addQueryParameter("fsym", baseCurrencyCode)
            .addQueryParameter("tsyms", terminatingCurrencyCode)
            .addQueryParameter("extraParams", "{quilt}[org.interledger.fx]")
            .build())
          .get()
          .build();

        try (Response okHttpResponse = okHttpClient.newCall(okHttpRequest).execute()) {
          if (!okHttpResponse.isSuccessful()) {
            final String errorMessage = String.format(
              "Unable to get FX rates from CryptoCompare. okHttpRequest=%s okHttpResponse=%s",
              okHttpRequest, okHttpResponse
            );
            throw new ExchangeRateException(errorMessage);
          }

          // Marshal the okHttpResponse to the correct object.
          final Map<String, String> ratesResponse = objectMapper.readValue(
            okHttpResponse.body().byteStream(), MAP_TYPE_REFERENCE
          );

          if (logger.isTraceEnabled()) {
            logger.trace("CryptoCompare rates retrieved successfully. okHttpRequest={} okHttpResponse={} rates={}",
              okHttpRequest,
              okHttpResponse,
              ratesResponse
            );
          }

          // Populate the builder if the response is present.
          Optional.ofNullable(ratesResponse.get(terminatingCurrencyCode))
            .map(value -> builder.setFactor(new DefaultNumberValue(new BigDecimal(value))))
            .orElseThrow(
              () -> new ExchangeRateException(String.format("No Rate found for ConversionQuery: %s", conversionQuery))
            );
        } catch (JsonParseException | JsonMappingException e) {
          logger.error(e.getMessage(), e);
        } catch (ExchangeRateException e) {
          throw e;
        } catch (Exception e) {
          throw new ExchangeRateException(e.getMessage(), e);
        }
      }

      // If everything works, return a constructed ExchangeRateBuilder.
      return builder.build();
    };
  }
}
