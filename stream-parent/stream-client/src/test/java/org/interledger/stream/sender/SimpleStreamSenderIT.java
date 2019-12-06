package org.interledger.stream.sender;

import static okhttp3.CookieJar.NO_COOKIES;
import static org.assertj.core.api.Assertions.assertThat;

import org.interledger.codecs.ilp.InterledgerCodecContextFactory;
import org.interledger.core.InterledgerAddress;
import org.interledger.link.Link;
import org.interledger.link.LinkId;
import org.interledger.link.http.IlpOverHttpLink;
import org.interledger.link.http.IlpOverHttpLinkSettings;
import org.interledger.link.http.IlpOverHttpLinkSettings.AuthType;
import org.interledger.link.http.IncomingLinkSettings;
import org.interledger.link.http.OutgoingLinkSettings;
import org.interledger.link.http.auth.SimpleBearerTokenSupplier;
import org.interledger.spsp.PaymentPointer;
import org.interledger.spsp.StreamConnectionDetails;
import org.interledger.spsp.client.SimpleSpspClient;
import org.interledger.spsp.client.SpspClientDefaults;
import org.interledger.spsp.client.rust.ImmutableRustNodeAccount;
import org.interledger.spsp.client.rust.InterledgerRustNodeClient;
import org.interledger.spsp.client.rust.RustNodeAccount;
import org.interledger.stream.Denomination;
import org.interledger.stream.Denominations;
import org.interledger.stream.SendMoneyRequest;
import org.interledger.stream.SendMoneyResult;
import org.interledger.stream.calculators.ExchangeRateCalculator;
import org.interledger.stream.calculators.NoExchangeRateException;
import org.interledger.stream.calculators.NoOpExchangeRateCalculator;
import org.interledger.stream.crypto.JavaxStreamEncryptionService;

import com.google.common.primitives.UnsignedLong;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import okhttp3.ConnectionPool;
import okhttp3.ConnectionSpec;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import org.assertj.core.data.Offset;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Integration tests for {@link SimpleStreamSender} that connects to a running ILP Connector using the information
 * supplied in this link, and initiates a STREAM payment.
 */
public class SimpleStreamSenderIT {

  private static final String AUTH_TOKEN = "password";
  private static final InterledgerAddress HOST_ADDRESS = InterledgerAddress.of("test.xpring-dev.rs1");
  private static final String SENDER_ACCOUNT_USERNAME = "java_stream_client";
  private static final InterledgerAddress SENDER_ADDRESS = HOST_ADDRESS.with(SENDER_ACCOUNT_USERNAME);
  private static final String LINK_ID = "simpleStreamSenderIT-to-Rust-IlpOverHttpLink";

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

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
      //.withLogConsumer(new org.testcontainers.containers.output.Slf4jLogConsumer (logger)) // uncomment to see logs
      .withEnv("ILP_REDIS_URL", "redis://redis:6379")
      .withCommand(""
          + "--admin_auth_token " + AUTH_TOKEN + " "
          + "--ilp_address " + HOST_ADDRESS.getValue() + " "
          + "--secret_seed 9dce76b1a20ec8d3db05ad579f3293402743767692f935a0bf06b30d2728439d "
          + "--http_bind_address 0.0.0.0:7770"
      );
  private Link link;
  private InterledgerRustNodeClient nodeClient;
  private SimpleSpspClient spspClient;

  @Before
  public void setUp() throws IOException {
    OkHttpClient httpClient = this.constructOkHttpClient();

    final HttpUrl interledgerNodeBaseUrl = this.getInterledgerBaseUri();
    this.nodeClient = new InterledgerRustNodeClient(
        httpClient,
        AUTH_TOKEN,
        interledgerNodeBaseUrl
    );

    this.spspClient = new SimpleSpspClient(
        httpClient,
        (pointer) -> {
          String pointerPath = pointer.path();
          if (pointerPath.startsWith("/")) {
            pointerPath = pointerPath.substring(1);
          }
          return interledgerNodeBaseUrl.newBuilder().addPathSegments(pointerPath).build();
        },
        SpspClientDefaults.MAPPER
    );

    final IlpOverHttpLinkSettings linkSettings = IlpOverHttpLinkSettings.builder()
        .incomingHttpLinkSettings(IncomingLinkSettings.builder()
            .authType(AuthType.SIMPLE)
            .encryptedTokenSharedSecret(AUTH_TOKEN)
            .build())
        .outgoingHttpLinkSettings(OutgoingLinkSettings.builder()
            .authType(AuthType.SIMPLE)
            .tokenSubject(SENDER_ACCOUNT_USERNAME)
            .url(this.constructIlpOverHttpUrl(SENDER_ACCOUNT_USERNAME))
            .encryptedTokenSharedSecret(AUTH_TOKEN)
            .build())
        .build();

    this.link = new IlpOverHttpLink(
        () -> SENDER_ADDRESS,
        linkSettings,
        httpClient,
        SpspClientDefaults.MAPPER,
        InterledgerCodecContextFactory.oer(),
        () -> AUTH_TOKEN
    );
    link.setLinkId(LinkId.of(LINK_ID));

    RustNodeAccount sender = accountBuilder()
        .username(SENDER_ACCOUNT_USERNAME)
        .ilpAddress(SENDER_ADDRESS)
        .build();

    nodeClient.createAccount(sender);
  }

  /**
   * One call to {@link SimpleStreamSender#sendMoney(SendMoneyRequest)} that involves a single packet for the entire
   * payment.
   */
  @Test
  public void sendMoneySinglePacket() {
    final UnsignedLong paymentAmount = UnsignedLong.valueOf(1000);

    StreamSender streamSender = new SimpleStreamSender(
        new JavaxStreamEncryptionService(), link
    );

    final StreamConnectionDetails connectionDetails = getStreamConnectionDetails(1000000);

    SendMoneyRequest request = SendMoneyRequest.builder()
        .sourceAddress(SENDER_ADDRESS)
        .amount(paymentAmount)
        .denomination(Denominations.XRP)
        .destinationAddress(connectionDetails.destinationAddress())
        .paymentTracker(new FixedSenderAmountPaymentTracker(paymentAmount, new NoOpExchangeRateCalculator()))
        .sharedSecret(connectionDetails.sharedSecret())
        .build();

    final SendMoneyResult sendMoneyResult = streamSender.sendMoney(request).join();

    assertThat(sendMoneyResult.amountDelivered()).isEqualTo(paymentAmount);
    assertThat(sendMoneyResult.originalAmount()).isEqualTo(paymentAmount);
    assertThat(sendMoneyResult.numFulfilledPackets()).isEqualTo(1);
    assertThat(sendMoneyResult.numRejectPackets()).isEqualTo(0);

    logger.info("Payment Sent: {}", sendMoneyResult);
  }

  /**
   * One call to {@link SimpleStreamSender#sendMoney(SendMoneyRequest)} that involves a single packet for the entire
   * payment.
   */
  @Test
  public void sendSmallPayment() {
    final UnsignedLong paymentAmount = UnsignedLong.valueOf(100);

    StreamSender streamSender = new SimpleStreamSender(
        new JavaxStreamEncryptionService(), link
    );

    final StreamConnectionDetails connectionDetails = getStreamConnectionDetails("sendSmallPayment");

    final SendMoneyResult sendMoneyResult = streamSender.sendMoney(
        SendMoneyRequest.builder()
            .sourceAddress(SENDER_ADDRESS)
            .amount(paymentAmount)
            .denomination(Denominations.XRP)
            .destinationAddress(connectionDetails.destinationAddress())
            .paymentTracker(new FixedSenderAmountPaymentTracker(paymentAmount, new NoOpExchangeRateCalculator()))
            .sharedSecret(connectionDetails.sharedSecret())
            .build()
    ).join();

    assertThat(sendMoneyResult.amountDelivered()).isEqualTo(paymentAmount);
    assertThat(sendMoneyResult.originalAmount()).isEqualTo(paymentAmount);
    assertThat(sendMoneyResult.numFulfilledPackets()).isEqualTo(1);
    assertThat(sendMoneyResult.numRejectPackets()).isEqualTo(0);

    logger.info("Payment Sent: {}", sendMoneyResult);
  }

  /**
   * In general, calling sendMoney using the same Connection (i.e., SharedSecret) in parallel should not be done.
   * However, the implementation is smart enough to queue up parallel requests and only allow one to run at a time.
   * However, sometimes waiting tasks will timeout, in which case a particular `sendMoney` may throw an exception. This
   * test does not expect any exceptions.
   */
  @Test
  public void sendMoneyOnSameConnectionInParallel() {
    final int numExecutions = 10;
    final UnsignedLong paymentAmount = UnsignedLong.valueOf(100000L);

    StreamSender streamSender = new SimpleStreamSender(
        new JavaxStreamEncryptionService(), link
    );

    final StreamConnectionDetails connectionDetails = getStreamConnectionDetails(1000000);

    List<CompletableFuture<SendMoneyResult>> results = new ArrayList<>();

    for (int i = 0; i < numExecutions; i++) {
      final CompletableFuture<SendMoneyResult> job = streamSender.sendMoney(
          SendMoneyRequest.builder()
              .sourceAddress(SENDER_ADDRESS)
              .amount(paymentAmount)
              .denomination(Denominations.XRP)
              .destinationAddress(connectionDetails.destinationAddress())
              .sharedSecret(connectionDetails.sharedSecret())
              .paymentTracker(new FixedSenderAmountPaymentTracker(paymentAmount, new NoOpExchangeRateCalculator()))
              .build()
      );
      results.add(job);
    }

    // Wait for all to complete...
    CompletableFuture[] completableFutures = results.toArray(new CompletableFuture[0]);
    CompletableFuture.allOf(completableFutures)
        .handle(($, error) -> {
          if (error != null) {
            logger.error(error.getMessage(), error);
          }
          // To placate completable future.
          return null;
        }).join();

    // Run a bunch of `sendMoney` calls on the same Connection, in parallel.
    List<SendMoneyResult> sendMoneyResults = results.stream()
        .map($ -> {
          try {
            return $.get();
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
        })
        .collect(Collectors.toList());

    // Sum-up the total results of all Calls.
    long totalAmountDelivered = sendMoneyResults.stream()
        .map(SendMoneyResult::amountDelivered)
        .mapToLong(UnsignedLong::longValue)
        .sum();
    assertThat(totalAmountDelivered).isEqualTo(numExecutions * paymentAmount.longValue());
    long totalPackets = sendMoneyResults.stream()
        .map(SendMoneyResult::totalPackets)
        .mapToLong(Integer::longValue)
        .sum();
    assertThat(totalPackets).isCloseTo(numExecutions * 7, Offset.offset(2L));
  }

  /**
   * One call to {@link SimpleStreamSender#sendMoney(SendMoneyRequest)}} that involves multiple packets in parallel.
   */
  @Test
  public void sendMoneyMultiPacket() {
    final UnsignedLong paymentAmount = UnsignedLong.valueOf(100000);

    StreamSender streamSender = new SimpleStreamSender(
        new JavaxStreamEncryptionService(), link
    );

    final StreamConnectionDetails connectionDetails = getStreamConnectionDetails(1000001);

    final SendMoneyResult sendMoneyResult = streamSender.sendMoney(
        SendMoneyRequest.builder()
            .sourceAddress(SENDER_ADDRESS)
            .amount(paymentAmount)
            .denomination(Denominations.XRP)
            .destinationAddress(connectionDetails.destinationAddress())
            .sharedSecret(connectionDetails.sharedSecret())
            .paymentTracker(new FixedSenderAmountPaymentTracker(paymentAmount, new NoOpExchangeRateCalculator()))
            .build()
    ).join();

    assertThat(sendMoneyResult.amountDelivered()).isEqualTo(paymentAmount);
    assertThat(sendMoneyResult.originalAmount()).isEqualTo(paymentAmount);
    assertThat(sendMoneyResult.numFulfilledPackets()).isCloseTo(8, Offset.offset(1));
    assertThat(sendMoneyResult.numRejectPackets()).isEqualTo(0);

    logger.info("Payment Sent: {}", sendMoneyResult);
  }

  /**
   * Multiple calls to {@link SimpleStreamSender#sendMoney(SendMoneyRequest)}} that involves multiple packets in
   * parallel, but using different accounts for each Stream, and thus a different Connection.
   */
  @Test
  public void sendMoneyMultiThreadedToSeparateAccounts() throws InterruptedException {
    final UnsignedLong paymentAmount = UnsignedLong.valueOf(1000000);

    int parallelism = 20;
    int sendCount = 100;
    ExecutorService executorService = Executors.newFixedThreadPool(parallelism);

    ThreadPoolExecutor streamExecutor = new ThreadPoolExecutor(0, 200,
        60L, TimeUnit.SECONDS,
        new SynchronousQueue<>(), new ThreadFactoryBuilder()
        .setDaemon(true)
        .setNameFormat("simple-stream-sender-%d")
        .build());

    List<Future<SendMoneyResult>> results = new ArrayList<>();
    for (int i = 0; i < sendCount; i++) {
      final int id = i;
      results.add(executorService.submit(() -> sendMoney(paymentAmount, id, streamExecutor)));
    }

    executorService.shutdown();
    executorService.awaitTermination(10000, TimeUnit.MILLISECONDS);
    results.forEach($ -> {
      try {
        $.get();
      } catch (Throwable e) {
        e.printStackTrace();
        throw new RuntimeException(e);
      }
    });

    BigDecimal totalSent = results.stream().map($ -> {
      try {
        return new BigDecimal($.get().amountDelivered().longValue());
      } catch (Exception e) {
        e.printStackTrace();
        throw new RuntimeException(e);
      }
    }).reduce(new BigDecimal(0), BigDecimal::add);

    assertThat(totalSent).isEqualTo(new BigDecimal(paymentAmount.longValue()).multiply(new BigDecimal(sendCount)));
  }

  @Test
  public void sendMoneyHonorsTimeout() {
    final UnsignedLong paymentAmount = UnsignedLong.valueOf(10000000);

    StreamSender streamSender = new SimpleStreamSender(
        new JavaxStreamEncryptionService(), link
    );

    String username = "sendMoneyHonorsTimeout";
    InterledgerAddress address = HOST_ADDRESS.with(username);
    RustNodeAccount rustNodeAccount = accountBuilder()
        .username(username)
        .ilpAddress(address)
        .maxPacketAmount(BigInteger.valueOf(100))
        .amountPerMinuteLimit(BigInteger.valueOf(1))
        .packetsPerMinuteLimit(BigInteger.valueOf(1))
        .build();

    final StreamConnectionDetails connectionDetails = getStreamConnectionDetails(rustNodeAccount);

    final SendMoneyResult sendMoneyResult = streamSender
        .sendMoney(
            SendMoneyRequest.builder()
                .sourceAddress(SENDER_ADDRESS)
                .amount(paymentAmount)
                .denomination(Denominations.XRP)
                .destinationAddress(connectionDetails.destinationAddress())
                .sharedSecret(connectionDetails.sharedSecret())
                .paymentTracker(new FixedSenderAmountPaymentTracker(paymentAmount, new NoOpExchangeRateCalculator()))
                .timeout(Duration.ofMillis(100))
                .build()
        ).join();

    assertThat(sendMoneyResult.successfulPayment()).isFalse();

    logger.info("Payment Sent: {}", sendMoneyResult);
  }

  @Test(expected = NoExchangeRateException.class)
  public void sendFailsIfNoExchangeRate() throws Throwable {
    final UnsignedLong paymentAmount = UnsignedLong.valueOf(1000);

    StreamSender streamSender = new SimpleStreamSender(
        new JavaxStreamEncryptionService(), link
    );

    String username = "sendFailsIfNoExchangeRate";
    InterledgerAddress address = HOST_ADDRESS.with(username);
    RustNodeAccount rustNodeAccount = accountBuilder()
        .username(username)
        .ilpAddress(address)
        .maxPacketAmount(BigInteger.valueOf(100))
        .amountPerMinuteLimit(BigInteger.valueOf(1))
        .packetsPerMinuteLimit(BigInteger.valueOf(1))
        .build();

    final StreamConnectionDetails connectionDetails = getStreamConnectionDetails(rustNodeAccount);

    ExchangeRateCalculator noExchangeRateExceptionCalculator = new CrankyExchangeRateCalculator();

    SendMoneyRequest request = SendMoneyRequest.builder()
        .sourceAddress(SENDER_ADDRESS)
        .amount(paymentAmount)
        .denomination(Denominations.XRP)
        .destinationAddress(connectionDetails.destinationAddress())
        .paymentTracker(new FixedSenderAmountPaymentTracker(paymentAmount, noExchangeRateExceptionCalculator))
        .sharedSecret(connectionDetails.sharedSecret())
        .timeout(Duration.ofMillis(100))
        .build();

    try {
      streamSender.sendMoney(request).join();
    } catch (CompletionException e) {
      throw e.getCause();
    }
  }

  @Test
  public void sendAcceptableDeliveredAmount() {
    final UnsignedLong paymentAmount = UnsignedLong.valueOf(100);

    StreamSender streamSender = new SimpleStreamSender(link);

    String username = "deliveredAmountRejected";
    InterledgerAddress address = HOST_ADDRESS.with(username);
    RustNodeAccount rustNodeAccount = accountBuilder()
        .username(username)
        .ilpAddress(address)
        .build();

    final StreamConnectionDetails connectionDetails = getStreamConnectionDetails(rustNodeAccount);

    SendMoneyRequest request = SendMoneyRequest.builder()
        .sourceAddress(SENDER_ADDRESS)
        .amount(paymentAmount)
        .denomination(Denominations.XRP)
        .destinationAddress(connectionDetails.destinationAddress())
        .paymentTracker(new FixedSenderAmountPaymentTracker(paymentAmount, new NoOpExchangeRateCalculator()))
        .sharedSecret(connectionDetails.sharedSecret())
        .timeout(Duration.ofMillis(1000))
        .build();

    SendMoneyResult result = streamSender.sendMoney(request).join();
    assertThat(result.amountDelivered()).isEqualTo(paymentAmount);
    assertThat(result.successfulPayment()).isTrue();
  }

  @Test
  public void sendFailsIfDeliveredAmountRejected() {
    final UnsignedLong paymentAmount = UnsignedLong.valueOf(100);

    StreamSender streamSender = new SimpleStreamSender(link);

    String username = "deliveredAmountRejected";
    InterledgerAddress address = HOST_ADDRESS.with(username);
    RustNodeAccount rustNodeAccount = accountBuilder()
        .username(username)
        .ilpAddress(address)
        .build();

    final StreamConnectionDetails connectionDetails = getStreamConnectionDetails(rustNodeAccount);

    SendMoneyRequest request = SendMoneyRequest.builder()
        .sourceAddress(SENDER_ADDRESS)
        .amount(paymentAmount)
        .denomination(Denominations.XRP)
        .destinationAddress(connectionDetails.destinationAddress())
        .paymentTracker(new FixedSenderAmountPaymentTracker(paymentAmount, new GreedyExchangeRateCalculator()))
        .sharedSecret(connectionDetails.sharedSecret())
        .timeout(Duration.ofMillis(1000))
        .build();

    SendMoneyResult result = streamSender.sendMoney(request).join();
    assertThat(result.amountDelivered()).isEqualTo(UnsignedLong.ZERO);
  }

  @Test
  public void sendMoneyWithWrongLinkPassword() throws IOException {
    final String connectorAccountUsername = UUID.randomUUID().toString().replace("-", "");

    final OkHttpClient httpClient = this.constructOkHttpClient();
    final IlpOverHttpLinkSettings linkSettings = IlpOverHttpLinkSettings.builder()
        .incomingHttpLinkSettings(IncomingLinkSettings.builder()
            .authType(AuthType.SIMPLE)
            .encryptedTokenSharedSecret(AUTH_TOKEN)
            .build())
        .outgoingHttpLinkSettings(OutgoingLinkSettings.builder()
            .authType(AuthType.SIMPLE)
            .tokenSubject(connectorAccountUsername)
            .url(this.constructIlpOverHttpUrl(SENDER_ACCOUNT_USERNAME))
            .encryptedTokenSharedSecret("wrong-password")
            .build())
        .build();

    this.link = new IlpOverHttpLink(
        () -> SENDER_ADDRESS,
        linkSettings,
        httpClient,
        SpspClientDefaults.MAPPER,
        InterledgerCodecContextFactory.oer(),
        new SimpleBearerTokenSupplier(SENDER_ACCOUNT_USERNAME + ":" + "wrong-password")
    );
    link.setLinkId(LinkId.of(LINK_ID));

    nodeClient.createAccount(accountBuilder()
        .username(connectorAccountUsername)
        .ilpAddress(HOST_ADDRESS.with(connectorAccountUsername))
        .build());

    final UnsignedLong paymentAmount = UnsignedLong.valueOf(1000);
    final StreamSender streamSender = new SimpleStreamSender(
        new JavaxStreamEncryptionService(), link
    );
    final StreamConnectionDetails connectionDetails = getStreamConnectionDetails(1000000);

    streamSender.sendMoney(
        SendMoneyRequest.builder()
            .sourceAddress(SENDER_ADDRESS)
            .destinationAddress(connectionDetails.destinationAddress())
            .amount(paymentAmount)
            .denomination(Denominations.XRP)
            .sharedSecret(connectionDetails.sharedSecret())
            .paymentTracker(new FixedSenderAmountPaymentTracker(paymentAmount, new NoOpExchangeRateCalculator()))
            .timeout(Duration.ofMillis(100L))
            .build()
    ).whenComplete(($, error) -> assertThat(error).isNotNull());
  }

  private StreamConnectionDetails getStreamConnectionDetails(int id) {
    return getStreamConnectionDetails("accountTest" + id);
  }

  private StreamConnectionDetails getStreamConnectionDetails(String username) {
    InterledgerAddress address = HOST_ADDRESS.with(username);
    return getStreamConnectionDetails(accountBuilder()
        .username(username)
        .ilpAddress(address)
        .build());
  }

  private StreamConnectionDetails getStreamConnectionDetails(RustNodeAccount rustNodeAccount) {
    try {
      nodeClient.createAccount(rustNodeAccount);
    } catch (Exception e) {
      throw new RuntimeException(e.getMessage(), e);
    }
    PaymentPointer pointer =
        PaymentPointer.of("$" + HOST_ADDRESS.getValue() + "/accounts/" + rustNodeAccount.username() + "/spsp");
    return spspClient.getStreamConnectionDetails(pointer);
  }

  private SendMoneyResult sendMoney(UnsignedLong paymentAmount, int taskId, ThreadPoolExecutor executor) {
    final StreamConnectionDetails connectionDetails = getStreamConnectionDetails(taskId);

    StreamSender streamSender = new SimpleStreamSender(
        new JavaxStreamEncryptionService(), link, executor
    );

    final SendMoneyResult sendMoneyResult = streamSender.sendMoney(
        SendMoneyRequest.builder()
            .sourceAddress(SENDER_ADDRESS)
            .destinationAddress(connectionDetails.destinationAddress())
            .amount(paymentAmount)
            .denomination(Denominations.XRP)
            .sharedSecret(connectionDetails.sharedSecret())
            .paymentTracker(new FixedSenderAmountPaymentTracker(paymentAmount, new NoOpExchangeRateCalculator()))
            .build()
    ).join();

    assertThat(sendMoneyResult.amountDelivered()).isEqualTo(paymentAmount);
    assertThat(sendMoneyResult.originalAmount()).isEqualTo(paymentAmount);
    assertThat(sendMoneyResult.numRejectPackets()).isEqualTo(0);

    logger.info("Payment Sent: {}", sendMoneyResult);
    return sendMoneyResult;
  }

  private ImmutableRustNodeAccount.Builder accountBuilder() {
    return RustNodeAccount.builder()
        .httpIncomingToken(AUTH_TOKEN)
        .httpOutgoingToken(AUTH_TOKEN)
        .assetCode("XRP")
        .assetScale(6)
        .minBalance(new BigInteger("-10000000000"))
        .roundTripTime(new BigInteger("500"))
        .routingRelation(RustNodeAccount.RoutingRelation.PEER);
  }

  /**
   * Helper method to return the base URL for the Rust Connector.
   *
   * @return An {@link HttpUrl} to communicate with.
   */
  private HttpUrl getInterledgerBaseUri() {
    return new HttpUrl.Builder()
        .scheme("http")
        .host(interledgerNode.getContainerIpAddress())
        .port(interledgerNode.getFirstMappedPort())
        .build();
  }

  /**
   * Helper method to construct the ILP-over-HTTP endpoint method for a given account in the Rust Connector.
   *
   * @param accountId The account identifier to send ILP packets to.
   *
   * @return An {@link HttpUrl} to use with ILP-over-HTTP using {@code accountId}.
   *
   * @see "https://github.com/interledger-rs/interledger-rs/pull/553"
   */
  private HttpUrl constructIlpOverHttpUrl(final String accountId) {
    return getInterledgerBaseUri().newBuilder()
        .addPathSegment("accounts")
        .addPathSegment(accountId)
        .addPathSegment("ilp")
        .build();
  }

  private OkHttpClient constructOkHttpClient() {
    ConnectionPool connectionPool = new ConnectionPool(10, 5, TimeUnit.MINUTES);
    ConnectionSpec spec = new ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS).build();
    HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
    logging.level(HttpLoggingInterceptor.Level.BASIC);
    OkHttpClient.Builder builder = new OkHttpClient.Builder()
        .connectionSpecs(Arrays.asList(spec, ConnectionSpec.CLEARTEXT))
        .cookieJar(NO_COOKIES)
        .connectTimeout(5000, TimeUnit.MILLISECONDS)
        .addInterceptor(logging)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS);
    return builder.connectionPool(connectionPool).build();
  }

  static class CrankyExchangeRateCalculator implements ExchangeRateCalculator {

    @Override
    public UnsignedLong calculateAmountToSend(UnsignedLong amountToReceive, Denomination sendDenomination,
        Denomination receiveDenomination) {
      throw new NoExchangeRateException("no exchanges allowed");
    }

    @Override
    public UnsignedLong calculateMinAmountToAccept(UnsignedLong sendAmount, Denomination sendDenomination,
        Optional<Denomination> expectedReceivedDenomination) throws NoExchangeRateException {
      throw new NoExchangeRateException("no exchanges allowed");
    }
  }

  static class GreedyExchangeRateCalculator implements ExchangeRateCalculator {

    @Override
    public UnsignedLong calculateAmountToSend(UnsignedLong amountToReceive, Denomination sendDenomination,
        Denomination receiveDenomination) {
      return amountToReceive.plus(UnsignedLong.ONE);
    }

    @Override
    public UnsignedLong calculateMinAmountToAccept(UnsignedLong sendAmount, Denomination sendDenomination,
        Optional<Denomination> expectedReceivedDenomination) throws NoExchangeRateException {
      return sendAmount.plus(UnsignedLong.ONE);
    }
  }

}
