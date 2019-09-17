package org.interledger.stream.sender;

import static okhttp3.CookieJar.NO_COOKIES;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

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
import org.interledger.stream.SendMoneyResult;
import org.interledger.stream.crypto.JavaxStreamEncryptionService;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.primitives.UnsignedLong;
import okhttp3.ConnectionPool;
import okhttp3.ConnectionSpec;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zalando.problem.ProblemModule;

import java.util.Arrays;
import java.util.Base64;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Integration tests for {@link SimpleStreamSender} that connects to a running ILP Connector using the information
 * supplied in this link, and initiates a STREAM payment.
 */
public class SimpleStreamSenderIT {

  private static final InterledgerAddress SENDER_ADDRESS
      = InterledgerAddress.of("test.xpring-dev.rs1.java_stream_client");

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private Link link;
  private byte[] sharedSecret;
  private InterledgerAddress destinationAddress;

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
  public void setUp() {
    ConnectionPool connectionPool = new ConnectionPool(10, 5, TimeUnit.MINUTES);
    OkHttpClient.Builder builder = new OkHttpClient.Builder();
    ConnectionSpec spec = new ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS).build();
    builder.connectionSpecs(Arrays.asList(spec, ConnectionSpec.CLEARTEXT));
    builder.cookieJar(NO_COOKIES);
    builder.connectTimeout(5000, TimeUnit.MILLISECONDS);
    builder.readTimeout(30, TimeUnit.SECONDS);
    builder.writeTimeout(30, TimeUnit.SECONDS);
    OkHttpClient httpClient = builder.connectionPool(connectionPool).build();

    final IlpOverHttpLinkSettings linkSettings = IlpOverHttpLinkSettings.builder()
        .incomingHttpLinkSettings(IncomingLinkSettings.builder()
            .authType(AuthType.SIMPLE)
            .encryptedTokenSharedSecret("password")
            .build())
        .outgoingHttpLinkSettings(OutgoingLinkSettings.builder()
            .authType(AuthType.SIMPLE)
            .tokenSubject("java_stream_client")
            .url(HttpUrl.parse("http://localhost:7770/ilp"))
            .encryptedTokenSharedSecret("password")
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
        .of("test.xpring-dev.rs1.java_stream_receiver.Khml7p2S2JrKWsOSJBTlQDWK5Wz7xiHHvKA8hqS-zHU"); // TODO: Get from SPSP
  }

  @Test
  @Ignore // TODO: Re-enable with some sort of flags so that developer builds work without a Connector running.
  public void sendMoney() {
    final UnsignedLong paymentAmount = UnsignedLong.valueOf(1000);

    StreamSender streamSender = new SimpleStreamSender(
        new JavaxStreamEncryptionService(), link
    );

    final SendMoneyResult sendMoneyResult = streamSender
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

  // TODO: Send too much
  // TODO: Send too little.
}
