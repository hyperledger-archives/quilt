package org.interledger.stream.pay;

import static okhttp3.CookieJar.NO_COOKIES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.interledger.codecs.ilp.InterledgerCodecContextFactory;
import org.interledger.codecs.stream.StreamCodecContextFactory;
import org.interledger.core.InterledgerAddress;
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
import org.interledger.spsp.PaymentPointer;
import org.interledger.spsp.StreamConnectionDetails;
import org.interledger.spsp.client.SimpleSpspClient;
import org.interledger.spsp.client.SpspClientDefaults;
import org.interledger.spsp.client.rust.InterledgerRustNodeClient;
import org.interledger.spsp.client.rust.RustNodeAccount;
import org.interledger.spsp.client.rust.UsdToXrpRatesRequest;
import org.interledger.stream.crypto.AesGcmStreamEncryptionService;
import org.interledger.stream.crypto.StreamEncryptionUtils;
import org.interledger.stream.model.AccountDetails;
import org.interledger.stream.pay.exceptions.StreamPayerException;
import org.interledger.stream.pay.model.SendState;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import okhttp3.ConnectionPool;
import okhttp3.ConnectionSpec;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import org.javamoney.moneta.spi.DefaultNumberValue;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.zalando.problem.ProblemModule;
import org.zalando.problem.violations.ConstraintViolationProblemModule;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import javax.money.CurrencyUnit;
import javax.money.convert.ExchangeRate;
import javax.money.convert.ExchangeRateProvider;

/**
 * An abstract IT that hits a local docker container running the Rust ILP Connector.
 */
@SuppressWarnings( {"ALL"})
public abstract class AbstractRustIT {

  protected static final InterledgerAddress HOST_ADDRESS = InterledgerAddress.of("private.quilt-it.rs1");
  protected static final String XRP_ACCOUNT_50K = "xrp_account_50k";
  protected static final String XRP_ACCOUNT = "xrp_account";
  protected static final String USD_ACCOUNT_50K = "usd_account_50k";
  protected static final String USD_ACCOUNT = "usd_account";
  protected static final String AUTH_TOKEN = "password";

  protected static final String PAYMENT_POINTER_XRP_50K = "$example.com/" + XRP_ACCOUNT_50K;
  protected static final String PAYMENT_POINTER_XRP = "$example.com/" + XRP_ACCOUNT;
  protected static final String PAYMENT_POINTER_USD_50K = "$example.com/" + USD_ACCOUNT_50K;
  protected static final String PAYMENT_POINTER_USD = "$example.com/" + USD_ACCOUNT;

  private final ObjectMapper objectMapperForPrettyPrinting = new ObjectMapper();

  protected StreamEncryptionUtils streamEncryptionUtils;
  protected SimpleSpspClient spspClient;

  protected final Logger logger = LoggerFactory.getLogger(this.getClass());
  private final Logger connectorLogger = LoggerFactory.getLogger("RustConnector");

  // For TestContainer support.
  private final Network network = Network.newNetwork();

  // 60 seconds max per method tested
  @Rule
  public Timeout globalTimeout = Timeout.seconds(60);

  @Rule
  public GenericContainer redis = new GenericContainer("redis:5.0.6")
    .withNetwork(network)
    .withNetworkAliases("redis")
    .withEnv("REDIS_URL", "redis://redis:6379");

  @Rule
  public GenericContainer interledgerNode = new GenericContainer<>("interledgerrs/ilp-node")
    .withExposedPorts(7770)
    .withNetwork(network)
    // uncomment to see logs
    //.withLogConsumer(new org.testcontainers.containers.output.Slf4jLogConsumer(connectorLogger))
    .withCommand(""
      + "--admin_auth_token " + AUTH_TOKEN + " "
      + "--database_url redis://redis:6379 "
      + "--ilp_address " + HOST_ADDRESS.getValue() + " "
      + "--secret_seed 9dce76b1a20ec8d3db05ad579f3293402743767692f935a0bf06b30d2728439d "
      + "--http_bind_address 0.0.0.0:7770"
    );

  @Before
  public void setUp() throws IOException {
    this.streamEncryptionUtils = new StreamEncryptionUtils(
      StreamCodecContextFactory.oer(), new AesGcmStreamEncryptionService()
    );

    this.initalizedConnectorAccounts();
  }

  ////////////////////
  // Protected Helpers
  ////////////////////

  //
//  protected OkHttpClient constructOkHttpClient() {
//    ConnectionPool connectionPool = new ConnectionPool(10, 5, TimeUnit.MINUTES);
//    ConnectionSpec spec = new ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS).build();
//    OkHttpClient.Builder builder = new OkHttpClient.Builder()
//      .connectionSpecs(Arrays.asList(spec, ConnectionSpec.CLEARTEXT))
//      .cookieJar(NO_COOKIES)
//      .connectTimeout(5000, TimeUnit.MILLISECONDS)
//      .readTimeout(30, TimeUnit.SECONDS)
//      .writeTimeout(30, TimeUnit.SECONDS);
//    return builder.connectionPool(connectionPool).build();
//  }
//
//  /**
//   * Construct an {@link ObjectMapper} that can be used to serialize and deserialize ProblemsJSON where JSON numbers
//   * emit as non-String values. Because Problems+Json requires HTTP status codes to be serialized as numbers (and not
//   * Strings) per RFC-7807, this ObjectMapper should not be used for payloads that involve Problems.
//   *
//   * @return An {@link ObjectMapper}.
//   * @see "https://tools.ietf.org/html/rfc7807"
//   */
//  protected static ObjectMapper newObjectMapperForProblemsJson() {
//    return new ObjectMapper()
//      .registerModule(new Jdk8Module())
//      .registerModule(new InterledgerModule(Encoding.BASE64))
//      .registerModule(new ProblemModule())
//      .registerModule(new ConstraintViolationProblemModule())
//      .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
//      .configure(JsonGenerator.Feature.WRITE_NUMBERS_AS_STRINGS, false);
//  }
//
//  /**
//   * Helper method to construct a new {@link Link} for transmitting ILP packets.
//   *
//   * @return A {@link Link}.
//   */
//  protected Link newIlpOverHttpLinkForQuiltIT() {
//    final Link link = new IlpOverHttpLink(
//      this::getSenderAddress,
//      getTestnetUrl(ACCOUNT_USERNAME_QUILT_IT),
//      constructOkHttpClient(),
//      newObjectMapperForProblemsJson(),
//      InterledgerCodecContextFactory.oer(),
//      new SimpleBearerTokenSupplier(PASS_KEY_QUILT_IT)
//    );
//
//    link.setLinkId(LinkId.of(getClass().getSimpleName() + "-ilpv4"));
//    return link;
//  }
//
//  /**
//   * Helper method to construct a new {@link Link} for transmitting ILP packets.
//   *
//   * @return A {@link Link}.
//   */
//  @Deprecated
//  protected Link newIlpOverHttpLinkForDemoUser() {
//    final Link link = new IlpOverHttpLink(
//      this::getSenderAddress,
//      getTestnetUrl(ACCOUNT_USERNAME_DEMO_USER),
//      constructOkHttpClient(),
//      newObjectMapperForProblemsJson(),
//      InterledgerCodecContextFactory.oer(),
//      new SimpleBearerTokenSupplier(PASS_KEY_DEMO_USER)
//    );
//
//    link.setLinkId(LinkId.of(getClass().getSimpleName() + "-ilpv4"));
//    return link;
//  }
//
//  protected HttpUrl getTestnetUrl(final String senderAccountUsername) {
//    Objects.requireNonNull(senderAccountUsername);
//    return HttpUrl.parse("https://rxprod.wc.wallet.ripplex.io/accounts/" + senderAccountUsername + "/ilp");
//  }
//
//  protected InterledgerAddressPrefix getSenderAddressPrefix() {
//    return SENDER_ADDRESS_PREFIX;
//  }
//
  protected <T> String pretty(final T jsonObject) {
    try {
      return objectMapperForPrettyPrinting.writerWithDefaultPrettyPrinter().writeValueAsString(jsonObject);
    } catch (JsonProcessingException e) {
      e.printStackTrace();
      return jsonObject.toString();
    }
  }
//
//  protected InterledgerAddress getSenderAddress() {
//    return InterledgerAddress.of(
//      this.getSenderAddressPrefix()
//        .with(this.getClass().getSimpleName().toLowerCase())
//        .getValue()
//    );
//  }
//
//  protected ExchangeRateProvider mockExchangeRateProvider() {
//    final ExchangeRateProvider exchangeRateProvider = mock(ExchangeRateProvider.class);
//
//    ExchangeRate xrpUsdRate = mock(ExchangeRate.class);
//    when(xrpUsdRate.getFactor()).thenReturn(DefaultNumberValue.of(new BigDecimal("0.2429546")));
//    when(exchangeRateProvider.getExchangeRate("XRP", "USD")).thenReturn(xrpUsdRate);
//
//    CurrencyUnit baseCurrencyUnit = mock(CurrencyUnit.class);
//    when(xrpUsdRate.getBaseCurrency()).thenReturn(baseCurrencyUnit);
//
//    CurrencyUnit currencyUnit = mock(CurrencyUnit.class);
//    when(xrpUsdRate.getCurrency()).thenReturn(currencyUnit);
//
//    return exchangeRateProvider;
//  }


  /**
   * Called to initialize 3 accounts on the Connector:
   *
   * <ol>
   *   <li>xrp_account_50k: An XRP (scale=9) account with a max-packet limit of 50000.</li>
   *   <li>xrp_account: An XRP (scale=9) account with no max-packet limit.</li>
   *   <li>usd_account_50k: A USD (scale=6) account with a max-packet limit of 50000.</li>
   *   <li>usd_account: A USd (scale=6) account with no max-packet limit.</li>
   * </ol>
   */
  protected void initalizedConnectorAccounts() throws IOException {
    final OkHttpClient httpClient = this.constructOkHttpClient();
    final HttpUrl interledgerNodeBaseUrl = this.getInterledgerBaseUri();
    final InterledgerRustNodeClient nodeClient = new InterledgerRustNodeClient(
      httpClient,
      AUTH_TOKEN,
      interledgerNodeBaseUrl
    );

    // Ignores the actual domain of any PaymentPointer, but respects the path.
    this.spspClient = new SimpleSpspClient(
      httpClient,
      (pointer) -> {
        String pointerPath = pointer.path();
        if (pointerPath.startsWith("/")) {
          pointerPath = pointerPath.substring(1);
        }
        return interledgerNodeBaseUrl.newBuilder()
          .addPathSegment("accounts")
          .addPathSegment(pointerPath)
          .addPathSegment("spsp")
          .build();
      },
      SpspClientDefaults.MAPPER
    );

    ///////////////////////////
    // Create `xrp_account_50k`
    ///////////////////////////
    RustNodeAccount sender = RustNodeAccount.builder()
      .username(XRP_ACCOUNT_50K)
      .ilpAddress(constructIlpAddressOnConnector(XRP_ACCOUNT_50K))
      .httpIncomingToken(AUTH_TOKEN)
      .httpOutgoingToken(AUTH_TOKEN)
      .assetCode("XRP")
      .assetScale(9)
      .minBalance(BigInteger.valueOf(Long.MAX_VALUE).subtract(BigInteger.ONE).multiply(BigInteger.valueOf(-1L)))
      .roundTripTime(new BigInteger("500"))
      .maxPacketAmount(BigInteger.valueOf(50000))
      .routingRelation(RustNodeAccount.RoutingRelation.CHILD)
      .build();
    RustNodeAccount result = nodeClient.createAccount(sender);
    assertThat(result.maxPacketAmount().isPresent()).isTrue();
    assertThat(result.maxPacketAmount().get()).isEqualTo(BigInteger.valueOf(50000));

    /////////////////////////
    // Create `xrp_account`
    /////////////////////////
    sender = RustNodeAccount.builder()
      .username(XRP_ACCOUNT)
      .ilpAddress(constructIlpAddressOnConnector(XRP_ACCOUNT))
      .httpIncomingToken(AUTH_TOKEN)
      .httpOutgoingToken(AUTH_TOKEN)
      .assetCode("XRP")
      .assetScale(9)
      .minBalance(BigInteger.valueOf(Long.MAX_VALUE).subtract(BigInteger.ONE).multiply(BigInteger.valueOf(-1L)))
      .roundTripTime(new BigInteger("500"))
      .routingRelation(RustNodeAccount.RoutingRelation.CHILD)
      .build();
    nodeClient.createAccount(sender);

    /////////////////////////
    // Create `usd_account_50k`
    /////////////////////////
    sender = RustNodeAccount.builder()
      .username(USD_ACCOUNT_50K)
      .ilpAddress(constructIlpAddressOnConnector(USD_ACCOUNT_50K))
      .httpIncomingToken(AUTH_TOKEN)
      .httpOutgoingToken(AUTH_TOKEN)
      .assetCode("USD")
      .assetScale(6)
      .minBalance(BigInteger.valueOf(Long.MAX_VALUE).subtract(BigInteger.ONE).multiply(BigInteger.valueOf(-1L)))
      .roundTripTime(new BigInteger("500"))
      .maxPacketAmount(BigInteger.valueOf(50000))
      .routingRelation(RustNodeAccount.RoutingRelation.CHILD)
      .build();
    result = nodeClient.createAccount(sender);
    assertThat(result.maxPacketAmount().isPresent()).isTrue();
    assertThat(result.maxPacketAmount().get()).isEqualTo(BigInteger.valueOf(50000));

    /////////////////////////
    // Create `usd_account`
    /////////////////////////
    sender = RustNodeAccount.builder()
      .username(USD_ACCOUNT)
      .ilpAddress(constructIlpAddressOnConnector(USD_ACCOUNT))
      .httpIncomingToken(AUTH_TOKEN)
      .httpOutgoingToken(AUTH_TOKEN)
      .assetCode("USD")
      .assetScale(6)
      .maxPacketAmount(BigInteger.valueOf(Long.MAX_VALUE)) // <-- Max supported by Rust Connector.
      .minBalance(BigInteger.valueOf(Long.MAX_VALUE).subtract(BigInteger.ONE).multiply(BigInteger.valueOf(-1L)))
      .roundTripTime(new BigInteger("500"))
      .routingRelation(RustNodeAccount.RoutingRelation.CHILD)
      .build();
    nodeClient.createAccount(sender);

    //////////////////////
    // Set USD to XRP Rate
    //////////////////////

    nodeClient.setUsdToXrpRate(UsdToXrpRatesRequest.builder()
      .usd(1.0)
      .xrp(0.24295460)
      .build());
  }

  /**
   * Helper method to construct a new {@link Link} for transmitting ILP packets.
   *
   * @return A {@link Link}.
   */
  protected Link constructIlpOverHttpLink(final String accountName) {
    Objects.requireNonNull(accountName);
    final Link link = new IlpOverHttpLink(
      () -> this.constructIlpAddressOnConnector(accountName), // <-- Sender Address
      this.constructIlpOverHttpUrl(accountName), // <-- ILPv4 URL
      this.constructOkHttpClient(),
      this.newObjectMapperForProblemsJson(),
      InterledgerCodecContextFactory.oer(),
      new SimpleBearerTokenSupplier(AUTH_TOKEN)
    );

    link.setLinkId(LinkId.of(getClass().getSimpleName() + "-ilpv4"));
    return link;
  }


  /**
   * Helper method to return the base URL for the Rust Connector.
   *
   * @return An {@link HttpUrl} to communicate with.
   */
  protected HttpUrl getInterledgerBaseUri() {
    return new HttpUrl.Builder()
      .scheme("http")
      .host(interledgerNode.getContainerIpAddress())
      .port(interledgerNode.getFirstMappedPort())
      .build();
  }

  /**
   * Given an {@code accountName}, construct a URL of the form:
   *
   * <pre>https://{server}/accounts/{accountName}/ilp</pre>.
   *
   * @param accountName The ILP account identifier.
   *
   * @return An {@link HttpUrl} that can be used to commuicate with the ILP Connector for the supplied {@code
   *   accountName}.
   */
  protected HttpUrl constructIlpOverHttpUrl(final String accountName) {
    return getInterledgerBaseUri().newBuilder()
      .addPathSegment("accounts")
      .addPathSegment(accountName)
      .addPathSegment("ilp")
      .build();
  }

  /**
   * Given an {@code accountName}, construct an {@link InterledgerAddress} of the form:
   *
   * <pre>{HOST_ADDRESS}.{accountName}</pre>.
   *
   * @param accountName The ILP account identifier.
   *
   * @return An {@link InterledgerAddress}.
   */
  protected InterledgerAddress constructIlpAddressOnConnector(final String accountName) {
    return HOST_ADDRESS.with(accountName);
  }

  /**
   * Helper method to obtain sender-account details via IL-DCP.
   *
   * @param ildcpLink A {@link Link} that can be used for IL-DCP. This Link will not have a sender address because
   *                  that's what IL-DCP is for.
   *
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

  protected OkHttpClient constructOkHttpClient() {
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

  protected ExchangeRateProvider mockExchangeRateProvider() {
    final ExchangeRateProvider exchangeRateProvider = mock(ExchangeRateProvider.class);

    {
      ExchangeRate xrpUsdRate = mock(ExchangeRate.class);
      when(xrpUsdRate.getFactor()).thenReturn(DefaultNumberValue.of(new BigDecimal("0.2429546")));
      when(exchangeRateProvider.getExchangeRate("XRP", "USD")).thenReturn(xrpUsdRate);
      CurrencyUnit baseCurrencyUnit = mock(CurrencyUnit.class);
      when(xrpUsdRate.getBaseCurrency()).thenReturn(baseCurrencyUnit);
      when(xrpUsdRate.getCurrency()).thenReturn(baseCurrencyUnit);
    }
    {
      ExchangeRate identityRate = mock(ExchangeRate.class);
      when(identityRate.getFactor()).thenReturn(DefaultNumberValue.of(BigDecimal.ONE));
      when(exchangeRateProvider.getExchangeRate("XRP", "XRP")).thenReturn(identityRate);
      when(exchangeRateProvider.getExchangeRate("USD", "USD")).thenReturn(identityRate);
      CurrencyUnit baseCurrencyUnit = mock(CurrencyUnit.class);
      when(identityRate.getBaseCurrency()).thenReturn(baseCurrencyUnit);
      when(identityRate.getCurrency()).thenReturn(baseCurrencyUnit);
    }

    return exchangeRateProvider;
  }

  /**
   * Construct an {@link ObjectMapper} that can be used to serialize and deserialize ProblemsJSON where JSON numbers
   * emit as non-String values. Because Problems+Json requires HTTP status codes to be serialized as numbers (and not
   * Strings) per RFC-7807, this ObjectMapper should not be used for payloads that involve Problems.
   *
   * @return An {@link ObjectMapper}.
   *
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
   * Helper to get a new {@link StreamConnection} for the supplied {@link PaymentPointer}.
   *
   * @return A newly constructed and obtained {@link StreamConnection}.
   */
  protected StreamConnection getNewStreamConnection(
    final AccountDetails sourceAccountDetails, final PaymentPointer paymentPointer
  ) {
    Objects.requireNonNull(sourceAccountDetails);
    Objects.requireNonNull(paymentPointer);

    // Fetch shared secret and destination address using SPSP client
    StreamConnectionDetails streamConnectionDetails = this.spspClient.getStreamConnectionDetails(paymentPointer);

    return new StreamConnection(
      sourceAccountDetails,
      streamConnectionDetails.destinationAddress(),
      streamConnectionDetails.sharedSecret()
    );
  }
}
