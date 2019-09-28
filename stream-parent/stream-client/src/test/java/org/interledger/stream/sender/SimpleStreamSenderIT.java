package org.interledger.stream.sender;

import static okhttp3.CookieJar.NO_COOKIES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

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
import org.interledger.quilt.jackson.InterledgerModule;
import org.interledger.quilt.jackson.conditions.Encoding;
import org.interledger.spsp.PaymentPointer;
import org.interledger.spsp.StreamConnectionDetails;
import org.interledger.spsp.client.rust.Account;
import org.interledger.spsp.client.rust.ImmutableAccount;
import org.interledger.spsp.client.rust.InterledgerRustNodeClient;
import org.interledger.stream.SendMoneyResult;
import org.interledger.stream.crypto.JavaxStreamEncryptionService;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.json.JsonWriteFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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
import org.zalando.problem.ProblemModule;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Integration tests for {@link org.interledger.stream.sender.SimpleStreamSender} that connects to a running ILP
 * Connector using the information supplied in this link, and initiates a STREAM payment.
 */
public class SimpleStreamSenderIT {

  private static final String AUTH_TOKEN = "password";
  private static final InterledgerAddress HOST_ADDRESS = InterledgerAddress.of("test.xpring-dev.rs1");
  private static final String SENDER_ACCOUNT_USERNAME = "java_stream_client";
  private static final InterledgerAddress SENDER_ADDRESS = HOST_ADDRESS.with(SENDER_ACCOUNT_USERNAME);

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  // 60 seconds max per method tested
  @Rule
  public Timeout globalTimeout = Timeout.seconds(60);

  @Rule
  public GenericContainer interledgerNode = new GenericContainer<>("nhartner/interledgerrs-standalone")
      .withExposedPorts(7770)
      .withCommand("--admin_auth_token " + AUTH_TOKEN + " " +
          "--ilp_address " + HOST_ADDRESS.getValue() + " " +
          "--secret_seed 9dce76b1a20ec8d3db05ad579f3293402743767692f935a0bf06b30d2728439d " +
          "--http_bind_address 0.0.0.0:7770");
  private Link link;
  private InterledgerRustNodeClient nodeClient;

  private static ObjectMapper objectMapperForTesting() {
    final ObjectMapper objectMapper = JsonMapper.builder()
        .enable(JsonWriteFeature.WRITE_NUMBERS_AS_STRINGS)
        .build()
        .registerModule(new Jdk8Module())
        .registerModule(new JavaTimeModule())
        .registerModule(new GuavaModule())
        .registerModule(new ProblemModule())
        .registerModule(new InterledgerModule(Encoding.BASE64)
        );

    objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    objectMapper.configure(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN, true);

    return objectMapper;
  }

  @Before
  public void setUp() throws IOException {
    String interledgerNodeBaseURI =
        "http://" + interledgerNode.getContainerIpAddress() + ":" + interledgerNode.getFirstMappedPort();
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
    OkHttpClient httpClient = builder.connectionPool(connectionPool).build();

    this.nodeClient = new InterledgerRustNodeClient(httpClient,
        AUTH_TOKEN,
        interledgerNodeBaseURI,
        (pointer) -> interledgerNodeBaseURI + "/spsp" + pointer.path());

    final IlpOverHttpLinkSettings linkSettings = IlpOverHttpLinkSettings.builder()
        .incomingHttpLinkSettings(IncomingLinkSettings.builder()
            .authType(AuthType.SIMPLE)
            .encryptedTokenSharedSecret(AUTH_TOKEN)
            .build())
        .outgoingHttpLinkSettings(OutgoingLinkSettings.builder()
            .authType(AuthType.SIMPLE)
            .tokenSubject(SENDER_ACCOUNT_USERNAME)
            .url(HttpUrl.parse(interledgerNodeBaseURI + "/ilp"))
            .encryptedTokenSharedSecret("password")
            .build())
        .build();

    this.link = new IlpOverHttpLink(
        () -> SENDER_ADDRESS,
        linkSettings,
        httpClient,
        objectMapperForTesting(),
        InterledgerCodecContextFactory.oer(),
        new SimpleBearerTokenSupplier(SENDER_ACCOUNT_USERNAME + ":" + AUTH_TOKEN)
    );
    link.setLinkId(LinkId.of("simpleStreamSenderIT-to-Rust-IlpOverHttpLink"));

    Account sender = accountBuilder()
        .username(SENDER_ACCOUNT_USERNAME)
        .ilpAddress(SENDER_ADDRESS)
        .build();

    nodeClient.createAccount(sender);
  }

  @Test
  public void sendMoneySinglePacket() {
    final UnsignedLong paymentAmount = UnsignedLong.valueOf(1000);

    StreamSender streamSender = new SimpleStreamSender(
        new JavaxStreamEncryptionService(), link
    );

    final StreamConnectionDetails connectionDetails = getStreamConnectionDetails(1000000);

    final SendMoneyResult sendMoneyResult = streamSender
        .sendMoney(connectionDetails.sharedSecret().key(),
            SENDER_ADDRESS,
            connectionDetails.destinationAddress(),
            paymentAmount).join();

    assertThat(sendMoneyResult.amountDelivered()).isEqualTo(paymentAmount);
    assertThat(sendMoneyResult.originalAmount()).isEqualTo(paymentAmount);
    assertThat(sendMoneyResult.numFulfilledPackets()).isEqualTo(2);
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
    final int NUM_EXECUTIONS = 10;
    final UnsignedLong paymentAmount = UnsignedLong.valueOf(100000L);

    StreamSender streamSender = new SimpleStreamSender(
        new JavaxStreamEncryptionService(), link
    );

    final StreamConnectionDetails connectionDetails = getStreamConnectionDetails(1000000);

    // Run a bunch of `sendMoney` calls on the same Connection, in parallel.
    List<SendMoneyResult> sendMoneyResults = new ArrayList<>();
    List<CompletableFuture<SendMoneyResult>> results = new ArrayList<>();
    for (int i = 0; i < NUM_EXECUTIONS; i++) {
      final CompletableFuture<SendMoneyResult> job = streamSender.sendMoney(
          connectionDetails.sharedSecret().key(),
          SENDER_ADDRESS,
          connectionDetails.destinationAddress(),
          paymentAmount
      ).whenComplete((sendMoneyResult, error) -> {
        // There should not be any errors for any sendMoney because they should queue-up and eventually run.
        assertThat(error).isNull();
        sendMoneyResults.add(sendMoneyResult);
      });
      results.add(job);
    }

    // Wait for all to complete...
    CompletableFuture.allOf(results.toArray(new CompletableFuture[0])).join();

    // Sum-up the total results of all Calls.
    long totalAmountDelivered = sendMoneyResults.stream()
        .map(smr -> smr.amountDelivered())
        .mapToLong($ -> $.longValue())
        .sum();
    assertThat(totalAmountDelivered).isEqualTo(NUM_EXECUTIONS * paymentAmount.longValue());
    long totalPackets = sendMoneyResults.stream()
        .map(smr -> smr.totalPackets())
        .mapToLong($ -> $.longValue())
        .sum();
    assertThat(totalPackets).isCloseTo(NUM_EXECUTIONS * 8, Offset.offset(2L));
  }

  @Test
  public void sendMoneyMultiPacket() {
    final UnsignedLong paymentAmount = UnsignedLong.valueOf(100000);

    StreamSender streamSender = new SimpleStreamSender(
        new JavaxStreamEncryptionService(), link
    );

    final StreamConnectionDetails connectionDetails = getStreamConnectionDetails(1000001);

    final SendMoneyResult sendMoneyResult = streamSender
        .sendMoney(connectionDetails.sharedSecret().key(),
            SENDER_ADDRESS,
            connectionDetails.destinationAddress(),
            paymentAmount).join();

    assertThat(sendMoneyResult.amountDelivered()).isEqualTo(paymentAmount);
    assertThat(sendMoneyResult.originalAmount()).isEqualTo(paymentAmount);
    assertThat(sendMoneyResult.numFulfilledPackets()).isCloseTo(8, Offset.offset(1));
    assertThat(sendMoneyResult.numRejectPackets()).isEqualTo(0);

    logger.info("Payment Sent: {}", sendMoneyResult);
  }

  @Test
  public void sendMoneyMultiThreaded() throws InterruptedException {
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
    results.stream().map($ -> {
      try {
        return $.get();
      } catch (Throwable e) {
        e.printStackTrace();
        throw new RuntimeException(e);
      }
    }).collect(Collectors.toList());

    BigDecimal totalSent = results.stream().map($ -> {
      try {
        return new BigDecimal($.get().amountDelivered().longValue());
      } catch (Exception e) {
        e.printStackTrace();
        throw new RuntimeException(e);
      }
    }).reduce(new BigDecimal(0), (left, right) -> left.add(right));

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
    Account account = accountBuilder()
        .username(username)
        .ilpAddress(address)
        .maxPacketAmount(BigInteger.valueOf(100))
        .amountPerMinuteLimit(BigInteger.valueOf(1))
        .packetsPerMinuteLimit(BigInteger.valueOf(1))
        .build();

    final StreamConnectionDetails connectionDetails = getStreamConnectionDetails(account);

    final SendMoneyResult sendMoneyResult = streamSender
        .sendMoney(connectionDetails.sharedSecret().key(),
            SENDER_ADDRESS,
            connectionDetails.destinationAddress(),
            paymentAmount,
            Duration.ofMillis(100)).join();

    assertThat(sendMoneyResult.successfulPayment()).isFalse();

    logger.info("Payment Sent: {}", sendMoneyResult);
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


  private StreamConnectionDetails getStreamConnectionDetails(Account account) {
    try {
      nodeClient.createAccount(account);
    } catch (Exception e) {
      System.out.println("Could not create account " + account.username());
      fail("Unexpected error", e);
    }
    PaymentPointer pointer = PaymentPointer.of("$" + HOST_ADDRESS.getValue() + "/" + account.username());
    return nodeClient.getStreamConnectionDetails(pointer);
  }

  private SendMoneyResult sendMoney(UnsignedLong paymentAmount, int taskId, ThreadPoolExecutor executor) {
    final StreamConnectionDetails connectionDetails = getStreamConnectionDetails(taskId);

    StreamSender streamSender = new SimpleStreamSender(
        new JavaxStreamEncryptionService(), link, executor
    );

    final SendMoneyResult sendMoneyResult = streamSender
        .sendMoney(connectionDetails.sharedSecret().key(),
            SENDER_ADDRESS,
            connectionDetails.destinationAddress(),
            paymentAmount).join();

    assertThat(sendMoneyResult.amountDelivered()).isEqualTo(paymentAmount);
    assertThat(sendMoneyResult.originalAmount()).isEqualTo(paymentAmount);
    assertThat(sendMoneyResult.numRejectPackets()).isEqualTo(0);

    logger.info("Payment Sent: {}", sendMoneyResult);
    return sendMoneyResult;
  }

  private ImmutableAccount.Builder accountBuilder() {
    return Account.builder()
        .httpIncomingToken(AUTH_TOKEN)
        .httpOutgoingToken(AUTH_TOKEN)
        .assetCode("XRP")
        .assetScale(6)
        .minBalance(new BigInteger("-10000000000"))
        .roundTripTime(new BigInteger("500"))
        .routingRelation(Account.RoutingRelation.PEER);
  }

//  @Test
//  public void sendMoneyWithWrongPassword() {

  // TODO:

//    this.link = new IlpOverHttpLink(
//        () -> Optional.of(SENDER_ADDRESS),
//        linkSettings,
//        new OkHttpClient(),
//        objectMapperForTesting(),
//        InterledgerCodecContextFactory.oer(),
//        new SimpleBearerTokenSupplier("shh")
//    );
//    link.setLinkId(LinkId.of("ilpHttpLink"));
//
//    this.sharedSecret = Base64.getDecoder().decode("UL6yyhL8rxYmjECCx27j6SMQxXLFcVosXvwlEvftna0=");
//    this.destinationAddress = InterledgerAddress
//        .of("test.xpring-dev.rs1.java_stream_client.wzE2o1X1bfLlZu-8WFU9kDrZPyheJYPivwBP8j1QLzQt"); // TODO: Get from SPSP
//
//
//    final UnsignedLong paymentAmount = UnsignedLong.valueOf(1000L);
//
//    StreamClient streamClient = new StreamClient(
//        new JavaxStreamEncryptionService(),
//        link,
//        new ConnectionManager()
//    );
//
//    final SendMoneyResult sendMoneyResult = streamClient
//        .sendMoney(sharedSecret, SENDER_ADDRESS, destinationAddress, paymentAmount).join();
//
//    assertThat(sendMoneyResult.amountDelivered(), is(paymentAmount));
//    assertThat(sendMoneyResult.originalAmount(), is(paymentAmount));
//    assertThat(sendMoneyResult.numFulfilledPackets(), is(10));
//    assertThat(sendMoneyResult.numRejectPackets(), is(0));
//
//    logger.info("Payment Sent: {}", sendMoneyResult);
  // }
}
