package org.interledger.stream.client;

import static okhttp3.CookieJar.NO_COOKIES;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;

import okhttp3.*;
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
import org.interledger.stream.ConnectionManager;
import org.interledger.stream.SendMoneyResult;
import org.interledger.stream.crypto.JavaxStreamEncryptionService;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.collect.ImmutableMap;
import com.google.common.primitives.UnsignedLong;
import okhttp3.logging.HttpLoggingInterceptor;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.zalando.problem.ProblemModule;

import java.io.IOException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Integration tests for {@link StreamClient}.
 */
public class StreamClientTest {

  public static final String ILP_ADDRESS = "test.xpring-dev.rs1";
  private static final InterledgerAddress SENDER_ADDRESS
      = InterledgerAddress.of(ILP_ADDRESS + ".java_stream_client");

  public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

  public static final String AUTH_TOKEN = "password";

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private Link link;
  private byte[] sharedSecret;
  private InterledgerAddress destinationAddress;
  private String interledgerNodeBaseURI;

  @Rule
  public GenericContainer interledgerNode = new GenericContainer<>("nhartner:interledger-rs") // FIXME use official interledger-rs image
      .withExposedPorts(7770)
      .withCommand("--admin_auth_token " + AUTH_TOKEN + " " +
          "--ilp_address " + ILP_ADDRESS + " " +
          "--secret_seed 9dce76b1a20ec8d3db05ad579f3293402743767692f935a0bf06b30d2728439d " +
          "--http_bind_address 0.0.0.0:7770");

  private static ObjectMapper objectMapperForTesting() {
    final ObjectMapper objectMapper = new ObjectMapper()
        .registerModule(new Jdk8Module())
        .registerModule(new JavaTimeModule())
        .registerModule(new GuavaModule())
        .registerModule(new ProblemModule()
        );

    objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    objectMapper.configure(JsonGenerator.Feature.WRITE_NUMBERS_AS_STRINGS, true);
    objectMapper.configure(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN, true);

    return objectMapper;
  }

  @Before
  public void setUp() throws IOException {
    ConnectionPool connectionPool = new ConnectionPool(10, 5, TimeUnit.MINUTES);
    HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
    logging.level(HttpLoggingInterceptor.Level.BASIC);
    ConnectionSpec spec = new ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS).build();
    OkHttpClient.Builder builder = new OkHttpClient.Builder()
      .connectionSpecs(Arrays.asList(spec, ConnectionSpec.CLEARTEXT))
      .cookieJar(NO_COOKIES)
      .connectTimeout(5000, TimeUnit.MILLISECONDS)
      .addInterceptor(logging)
      .readTimeout(30, TimeUnit.SECONDS)
      .writeTimeout(30, TimeUnit.SECONDS);
    OkHttpClient httpClient = builder.connectionPool(connectionPool).build();
    interledgerNodeBaseURI = "http://localhost:" + interledgerNode.getFirstMappedPort();

    final IlpOverHttpLinkSettings linkSettings = IlpOverHttpLinkSettings.builder()
        .incomingHttpLinkSettings(IncomingLinkSettings.builder()
            .authType(AuthType.SIMPLE)
            .encryptedTokenSharedSecret(AUTH_TOKEN)
            .build())
        .outgoingHttpLinkSettings(OutgoingLinkSettings.builder()
            .authType(AuthType.SIMPLE)
            .tokenSubject("java_stream_client")
            .url(HttpUrl.parse(interledgerNodeBaseURI + "/ilp"))
            .encryptedTokenSharedSecret(AUTH_TOKEN)
            .build())
        .build();

    this.link = new IlpOverHttpLink(
        () -> Optional.of(SENDER_ADDRESS),
        linkSettings,
        httpClient,
        objectMapperForTesting(),
        InterledgerCodecContextFactory.oer(),
        new SimpleBearerTokenSupplier("java_stream_client:password")
    );
    link.setLinkId(LinkId.of("ilpHttpLink"));

    this.sharedSecret = Base64.getDecoder().decode("R5FMgJ1fOSg3SztrMwKAS9KaGJuVYAUeLstWt8ZP6mk=");
    this.destinationAddress = InterledgerAddress
        .of(ILP_ADDRESS + ".java_stream_receiver.Khml7p2S2JrKWsOSJBTlQDWK5Wz7xiHHvKA8hqS-zHU"); // TODO: Get from SPSP

    createAccount(httpClient, "java_stream_client");
    createAccount(httpClient, "java_stream_receiver");
  }

  @Test
  public void sendMoney() {
    final UnsignedLong paymentAmount = UnsignedLong.valueOf(1000);

    StreamClient streamClient = new StreamClient(
        new JavaxStreamEncryptionService(),
        link,
        new ConnectionManager()
    );

    final SendMoneyResult sendMoneyResult = streamClient
        .sendMoney(sharedSecret, SENDER_ADDRESS, destinationAddress, paymentAmount).join();

    assertThat(sendMoneyResult.amountDelivered(), is(paymentAmount));
    assertThat(sendMoneyResult.originalAmount(), is(paymentAmount));
    assertThat(sendMoneyResult.numFulfilledPackets(), is(2));
    assertThat(sendMoneyResult.numRejectPackets(), is(0));

    logger.info("Payment Sent: {}", sendMoneyResult);
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

  private void createAccount(OkHttpClient httpClient, String accountName) throws IOException {
    ImmutableMap<Object, Object> newAccountPayload = ImmutableMap.builder()
      .put("ilp_address", ILP_ADDRESS + "." + accountName)
      .put("username", accountName)
      .put("asset_code", "XRP")
      .put("asset_scale", 6)
      .put("http_endpoint", "https://peer-ilp-over-http-endpoint.example/ilp")
      .put("http_incoming_token", AUTH_TOKEN)
      .put("http_outgoing_)token", AUTH_TOKEN)
      .put("is_admin", false)
      .put("min_balance", -1000000000L)
      .put("receive_routes", false)
      .put("send_routes", false)
      .put("round_trip_times", 500)
      .put("routing_relation", "Peer")
      .build();

    Request request = new Request.Builder()
      .url(HttpUrl.parse(interledgerNodeBaseURI + "/accounts"))
      .post(RequestBody.create(new ObjectMapper().writeValueAsString(newAccountPayload), JSON))
      .headers(Headers.of(ImmutableMap.of("Authorization", "Bearer " + AUTH_TOKEN)))
      .build();
    Response response = httpClient.newCall(request).execute();
    // FIXME switch to assertj once hamcrest->assertj refactoring merged
    assertEquals(200, response.code());
  }
}
