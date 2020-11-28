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
import org.interledger.link.LinkId;
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

  // This user has a maxPacketAmount of 50000
  protected static final String RIPPLEX_PACKET_LIMITED_PAYMENT_POINTER = "$ripplex.money/quiltit";
  // These users have no maxPacketAmount
  protected static final String RIPPLEX_UNLIMITED_PAYMENT_POINTER = "$ripplex.money/demo_user";
  protected static final String RIPPLEX_UNLIMITED_PAYMENT_POINTER2 = "$ripplex.money/demo_user2";

  private static final InterledgerAddressPrefix SENDER_ADDRESS_PREFIX
    = InterledgerAddressPrefix.of("private.quilt.it");

  protected static final String ACCOUNT_USERNAME_QUILT_IT = "quiltit";
  protected static final String PASS_KEY_QUILT_IT = "ODNjMTU1MTItMGFkMC00NzNmLTg0NDAtZThlZWJiODk2NGVl";

  protected static final String ACCOUNT_USERNAME_DEMO_USER = "demo_user";
  protected static final String PASS_KEY_DEMO_USER = "ZjRjYTdmMzctMzE2NC00YmQwLWJlMTUtZTNlMjQ1Nzc4ZTlm";

  protected static final String ACCOUNT_USERNAME_DEMO_USER_2 = "demo_user2";

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
  protected Link newIlpOverHttpLinkForQuiltIT() {
    final Link link = new IlpOverHttpLink(
      this::getSenderAddress,
      getTestnetUrl(ACCOUNT_USERNAME_QUILT_IT),
      newHttpClient(),
      newObjectMapperForProblemsJson(),
      InterledgerCodecContextFactory.oer(),
      new SimpleBearerTokenSupplier(PASS_KEY_QUILT_IT)
    );

    link.setLinkId(LinkId.of(getClass().getSimpleName() + "-ilpv4"));
    return link;
  }

  /**
   * Helper method to construct a new {@link Link} for transmitting ILP packets.
   *
   * @return A {@link Link}.
   */
  protected Link newIlpOverHttpLinkForDemoUser() {
    final Link link = new IlpOverHttpLink(
      this::getSenderAddress,
      getTestnetUrl(ACCOUNT_USERNAME_DEMO_USER),
      newHttpClient(),
      newObjectMapperForProblemsJson(),
      InterledgerCodecContextFactory.oer(),
      new SimpleBearerTokenSupplier(PASS_KEY_DEMO_USER)
    );

    link.setLinkId(LinkId.of(getClass().getSimpleName() + "-ilpv4"));
    return link;
  }

  protected HttpUrl getTestnetUrl(final String senderAccountUsername) {
    Objects.requireNonNull(senderAccountUsername);
    return HttpUrl.parse("https://rxprod.wc.wallet.ripplex.io/accounts/" + senderAccountUsername + "/ilp");
  }

  protected InterledgerAddressPrefix getSenderAddressPrefix() {
    return SENDER_ADDRESS_PREFIX;
  }

  /**
   * Helper method to obtain sender-account details via IL-DCP.
   *
   * @param ildcpLink A {@link Link} that can be used for IL-DCP. This Link will not have a sender address because
   *                  that's what IL-DCP is for.
   * @return
   */
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

  protected InterledgerAddress getSenderAddress() {
    return InterledgerAddress.of(
      this.getSenderAddressPrefix()
        .with(this.getClass().getSimpleName().toLowerCase())
        .getValue()
    );
  }

  protected ExchangeRateProvider mockExchangeRateProvider() {
    final ExchangeRateProvider exchangeRateProvider = mock(ExchangeRateProvider.class);

    ExchangeRate xrpUsdRate = mock(ExchangeRate.class);
    when(xrpUsdRate.getFactor()).thenReturn(DefaultNumberValue.of(new BigDecimal("0.2429546")));
    when(exchangeRateProvider.getExchangeRate("XRP", "USD")).thenReturn(xrpUsdRate);

    CurrencyUnit baseCurrencyUnit = mock(CurrencyUnit.class);
    when(xrpUsdRate.getBaseCurrency()).thenReturn(baseCurrencyUnit);

    CurrencyUnit currencyUnit = mock(CurrencyUnit.class);
    when(xrpUsdRate.getCurrency()).thenReturn(currencyUnit);

    return exchangeRateProvider;
  }
}
