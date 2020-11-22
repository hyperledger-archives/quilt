package org.interledger.stream.pay;

import static okhttp3.CookieJar.NO_COOKIES;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import javax.money.CurrencyUnit;
import javax.money.convert.ExchangeRate;
import javax.money.convert.ExchangeRateProvider;
import okhttp3.ConnectionPool;
import okhttp3.ConnectionSpec;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import org.interledger.codecs.ilp.InterledgerCodecContextFactory;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerAddressPrefix;
import org.interledger.fx.Denomination;
import org.interledger.ildcp.IldcpFetcher;
import org.interledger.ildcp.IldcpRequest;
import org.interledger.ildcp.IldcpResponse;
import org.interledger.link.IldcpFetcherFactory.Default;
import org.interledger.link.Link;
import org.interledger.link.http.IlpOverHttpLink;
import org.interledger.link.http.auth.SimpleBearerTokenSupplier;
import org.interledger.quilt.jackson.InterledgerModule;
import org.interledger.quilt.jackson.conditions.Encoding;
import org.interledger.stream.model.AccountDetails;
import org.interledger.stream.pay.exceptions.StreamPayerException;
import org.interledger.stream.pay.model.SendState;
import org.javamoney.moneta.spi.DefaultNumberValue;
import org.zalando.problem.ProblemModule;
import org.zalando.problem.violations.ConstraintViolationProblemModule;

public abstract class AbstractIT {

  private final ObjectMapper objectMapperForPrettyPrinting = new ObjectMapper();
  // TODO: Re-work these tests with the Rust Connector in a Docker container from interledger4j so that we're not at
  //  the whim of rafiki.money.

  // Rafiki users have no max-packet bound.
  protected static final String RAFIKI_PAYMENT_POINTER = "$rafiki.money/p/test@example.com";

  // This user has a maxPacket bound of 50000
  protected static final String RIPPLEX_PACKET_LIMITED_PAYMENT_POINTER = "$ripplex.money/quiltit";

  private static final InterledgerAddressPrefix SENDER_ADDRESS_PREFIX
    = InterledgerAddressPrefix.of("private.quilt.it");

  private static final String SENDER_ACCOUNT_USERNAME = "quiltit";
  private static final String SENDER_PASS_KEY = "YTc1OWVkNzMtODBkMi00ZjYyLWIxMTAtMjk2ZmU0YjA5MjU5";

  ////////////////////
  // Protected Helpers
  ////////////////////

  protected OkHttpClient newHttpClient() {
    ConnectionPool connectionPool = new ConnectionPool(10, 5, TimeUnit.MINUTES);
    ConnectionSpec spec = new ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS).build();
    OkHttpClient.Builder builder = new OkHttpClient.Builder()
      .connectionSpecs(Arrays.asList(spec, ConnectionSpec.CLEARTEXT))
      .cookieJar(NO_COOKIES)
      .connectTimeout(5000, TimeUnit.MILLISECONDS)
      .readTimeout(30, TimeUnit.SECONDS)
      .writeTimeout(30, TimeUnit.SECONDS);
    return builder.connectionPool(connectionPool).build();
  }

  /**
   * Construct an {@link ObjectMapper} that can be used to serialize and deserialize ProblemsJSON where JSON numbers
   * emit as non-String values. Because Problems+Json requires HTTP status codes to be serialized as numbers (and not
   * Strings) per RFC-7807, this ObjectMapper should not be used for payloads that involve Problems.
   *
   * @return An {@link ObjectMapper}.
   * @see "https://tools.ietf.org/html/rfc7807"
   */
  protected static ObjectMapper newObjectMapperForProblemsJson() {
    return new ObjectMapper()
      .registerModule(new Jdk8Module())
      .registerModule(new InterledgerModule(Encoding.BASE64))
      .registerModule(new ProblemModule())
      .registerModule(new ConstraintViolationProblemModule())
      .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
      .configure(JsonGenerator.Feature.WRITE_NUMBERS_AS_STRINGS, false);
  }

  /**
   * Helper method to construct a new {@link Link} for transmitting ILP packets.
   *
   * @return A {@link Link}.
   */
  protected Link newIlpOverHttpLink(final InterledgerAddress senderAddress) {
    Objects.requireNonNull(senderAddress);
    return new IlpOverHttpLink(
      () -> senderAddress,
      getTestnetUrl(),
      newHttpClient(),
      newObjectMapperForProblemsJson(),
      InterledgerCodecContextFactory.oer(),
      new SimpleBearerTokenSupplier(SENDER_PASS_KEY)
    );
  }

  protected HttpUrl getTestnetUrl() {
    return HttpUrl.parse("https://rxprod.wc.wallet.ripplex.io/accounts/" + getSenderAccountUsername() + "/ilp");
  }

  protected String getSenderAccountUsername() {
    return SENDER_ACCOUNT_USERNAME;
  }

  protected String getSenderAccountToken() {
    return SENDER_PASS_KEY;
  }

  protected InterledgerAddressPrefix getSenderAddressPrefix() {
    return SENDER_ADDRESS_PREFIX;
  }

//  protected InterledgerAddress getSenderAddress() {
//    return InterledgerAddress.of(getSenderAddressPrefix().getValue()).with(SENDER_ACCOUNT_USERNAME);
//  }

  /**
   * Helper method to obtain sender-account details via IL-DCP.
   *
   * @param ildcpLink A {@link Link} that can be used for IL-DCP. This Link will not have a sender address because
   *                  that's what IL-DCP is for.
   * @return
   */
// TODO: Fix the contract for the operator address. It should probably be optional.
  protected AccountDetails newSenderAccountDetailsViaILDCP(final Link ildcpLink) {
    Objects.requireNonNull(ildcpLink);
    try {
      final IldcpFetcher ildcpFetcher = new Default().construct(ildcpLink);
      final IldcpResponse result = ildcpFetcher.fetch(IldcpRequest.builder()
        .expiresAt(Instant.now().plus(10, ChronoUnit.SECONDS))
        .build());

      return AccountDetails.builder()
        .interledgerAddress(result.clientAddress())
        .denomination(Denomination.builder()
          .assetCode(result.assetCode())
          .assetScale(result.assetScale())
          .build())
        .build();
    } catch (Exception e) {
      throw new StreamPayerException(
        "Unable to obtain sender account details via IL-DCP.", e, SendState.UnknownSourceAsset
      );
    }

  }

  protected <T> String pretty(final T jsonObject) {
    try {
      return objectMapperForPrettyPrinting.writerWithDefaultPrettyPrinter().writeValueAsString(jsonObject);
    } catch (JsonProcessingException e) {
      e.printStackTrace();
      return jsonObject.toString();
    }
  }

  protected ExchangeRateProvider mockExchangeRateProvider() {
    final ExchangeRateProvider exchangeRateProvider = mock(ExchangeRateProvider.class);

    ExchangeRate xrpUsdRate = mock(ExchangeRate.class);
    when(xrpUsdRate.getFactor()).thenReturn(DefaultNumberValue.of(new BigDecimal("0.26")));
    when(exchangeRateProvider.getExchangeRate("XRP", "USD")).thenReturn(xrpUsdRate);

    CurrencyUnit baseCurrencyUnit = mock(CurrencyUnit.class);
    when(xrpUsdRate.getBaseCurrency()).thenReturn(baseCurrencyUnit);

    CurrencyUnit currencyUnit = mock(CurrencyUnit.class);
    when(xrpUsdRate.getCurrency()).thenReturn(currencyUnit);

    return exchangeRateProvider;
  }

//  protected ExchangeRateProvider newExchangeRateProvider() {
//    final Cache<ConversionQuery, ExchangeRate> fxCache = Caffeine.newBuilder()
//      .expireAfterWrite(30, TimeUnit.MINUTES) // Cache all rates for all tests if possible.
//      .build(); // No default loading function.
//
//    return new CryptoCompareRateProvider(
//      () -> "", // No API token provided for this test.
//      newObjectMapperForProblemsJson(),
//      newHttpClient(),
//      fxCache
//    );
//  }
}
