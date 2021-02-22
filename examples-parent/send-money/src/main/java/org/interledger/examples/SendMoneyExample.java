package org.interledger.examples;

import static okhttp3.CookieJar.NO_COOKIES;

import org.interledger.codecs.ilp.InterledgerCodecContextFactory;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.SharedSecret;
import org.interledger.link.Link;
import org.interledger.link.http.IlpOverHttpLink;
import org.interledger.link.http.auth.SimpleBearerTokenSupplier;
import org.interledger.quilt.jackson.InterledgerModule;
import org.interledger.quilt.jackson.conditions.Encoding;
import org.interledger.spsp.PaymentPointer;
import org.interledger.spsp.StreamConnectionDetails;
import org.interledger.spsp.client.SimpleSpspClient;
import org.interledger.spsp.client.SpspClient;
import org.interledger.stream.Denominations;
import org.interledger.stream.SendMoneyRequest;
import org.interledger.stream.SendMoneyResult;
import org.interledger.stream.sender.FixedSenderAmountPaymentTracker;
import org.interledger.stream.sender.SimpleStreamSender;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.google.common.primitives.UnsignedLong;
import okhttp3.ConnectionPool;
import okhttp3.ConnectionSpec;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import org.zalando.problem.ProblemModule;
import org.zalando.problem.violations.ConstraintViolationProblemModule;

import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Example how to use Quilt to send a STREAM payment using {@link SimpleStreamSender}. See this module's README for more
 * details.
 */
public class SendMoneyExample {

  // NOTE - replace this with the username for your sender account
  private static final String SENDER_ACCOUNT_USERNAME = "demo_user";
  // NOTE - replace this with the passkey for your sender account
  private static final String SENDER_PASS_KEY = "OWIyMzUwMzgtNWUzZi00MDU1LWJlMmUtZjk4NjdmMTJlOWYz";
  // NOTE - replace this with the payment pointer for your receiver account
  private static final String RECEIVER_PAYMENT_POINTER = "$ripplex.money/demo_receiver";

  private static final String TESTNET_URI =
    "https://rxprod.wc.wallet.ripplex.io/accounts/" + SENDER_ACCOUNT_USERNAME + "/ilp";

  private static final InterledgerAddress OPERATOR_ADDRESS =
    InterledgerAddress.of("private.org.interledger.examples.send-money-with-stream-sender-example")
      .with(SENDER_ACCOUNT_USERNAME);

  /**
   * Main method.
   *
   * @param args A String array.
   */
  public static void main(String[] args) throws ExecutionException, InterruptedException {
    SpspClient spspClient = new SimpleSpspClient();

    // Fetch shared secret and destination address using SPSP client
    StreamConnectionDetails connectionDetails = spspClient
      .getStreamConnectionDetails(PaymentPointer.of(RECEIVER_PAYMENT_POINTER));

    // Use ILP over HTTP for our underlying link
    Link link = newIlpOverHttpLink();

    // Create SimpleStreamSender for sending STREAM payments
    SimpleStreamSender simpleStreamSender = new SimpleStreamSender(link);

    // This is 1 drop when scale=9
    final long ONE_DROP_IN_SCALE_9 = 1000;

    // Send payment using STREAM
    SendMoneyResult result = simpleStreamSender.sendMoney(
      SendMoneyRequest.builder()
        // The client is not routable, so while we could send an empty ConnectionNewAddress frame, this breaks older
        // version of the Java receiver, so for now we specify an address here.
        .sourceAddress(OPERATOR_ADDRESS)
        .amount(UnsignedLong.valueOf(ONE_DROP_IN_SCALE_9))
        .denomination(Denominations.XRP_MILLI_DROPS)
        .destinationAddress(connectionDetails.destinationAddress())
        .timeout(Duration.ofMillis(15000))
        .paymentTracker(new FixedSenderAmountPaymentTracker(UnsignedLong.valueOf(ONE_DROP_IN_SCALE_9)))
        .sharedSecret(SharedSecret.of(connectionDetails.sharedSecret().value()))
        .build()
    ).get();

    System.out.println("Send money result: " + result);
  }

  private static Link newIlpOverHttpLink() {
    return new IlpOverHttpLink(
      () -> OPERATOR_ADDRESS,
      HttpUrl.parse(TESTNET_URI),
      newHttpClient(),
      createObjectMapperForProblemsJson(),
      InterledgerCodecContextFactory.oer(),
      new SimpleBearerTokenSupplier(SENDER_PASS_KEY)
    );
  }

  private static OkHttpClient newHttpClient() {
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
   *
   * @see "https://tools.ietf.org/html/rfc7807"
   */
  private static ObjectMapper createObjectMapperForProblemsJson() {
    return new ObjectMapper()
      .registerModule(new Jdk8Module())
      .registerModule(new InterledgerModule(Encoding.BASE64))
      .registerModule(new ProblemModule())
      .registerModule(new ConstraintViolationProblemModule())
      .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
      .configure(JsonGenerator.Feature.WRITE_NUMBERS_AS_STRINGS, false);
  }
}
