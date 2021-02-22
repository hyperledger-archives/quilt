package org.interledger.examples;

import static okhttp3.CookieJar.NO_COOKIES;

import org.interledger.codecs.ilp.InterledgerCodecContextFactory;
import org.interledger.codecs.stream.StreamCodecContextFactory;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.fluent.Percentage;
import org.interledger.fx.DefaultOracleExchangeRateService;
import org.interledger.fx.FauxRateProvider;
import org.interledger.fx.Slippage;
import org.interledger.link.Link;
import org.interledger.link.LinkId;
import org.interledger.link.http.IlpOverHttpLink;
import org.interledger.link.http.auth.SimpleBearerTokenSupplier;
import org.interledger.quilt.jackson.InterledgerModule;
import org.interledger.quilt.jackson.conditions.Encoding;
import org.interledger.spsp.PaymentPointer;
import org.interledger.stream.crypto.AesGcmStreamSharedSecretCrypto;
import org.interledger.stream.crypto.StreamPacketEncryptionService;
import org.interledger.stream.model.AccountDetails;
import org.interledger.stream.pay.StreamPayer;
import org.interledger.stream.pay.model.PaymentOptions;
import org.interledger.stream.pay.model.PaymentReceipt;
import org.interledger.stream.pay.model.Quote;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import okhttp3.ConnectionPool;
import okhttp3.ConnectionSpec;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zalando.problem.ProblemModule;
import org.zalando.problem.violations.ConstraintViolationProblemModule;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * Example how to use Quilt to send a STREAM payment using {@link StreamPayer}. See this module's README for more
 * details.
 */
public class SendMoneyWithIlpPayExample {

  private static final Logger LOGGER = LoggerFactory.getLogger(SendMoneyWithIlpPayExample.class);

  // NOTE - replace this with the username for your sender account
  private static final String SENDER_ACCOUNT_USERNAME = "demo_user";
  // NOTE - replace this with the passkey for your sender account
  private static final String SENDER_AUTH_TOKEN = "OWIyMzUwMzgtNWUzZi00MDU1LWJlMmUtZjk4NjdmMTJlOWYz";
  // NOTE - replace this with the payment pointer for your receiver account
  private static final PaymentPointer RECEIVER_PAYMENT_POINTER = PaymentPointer.of("$ripplex.money/demo_receiver");

  private static final HttpUrl TESTNET_URI = HttpUrl.parse(
    "https://rxprod.wc.wallet.ripplex.io/accounts/" + SENDER_ACCOUNT_USERNAME + "/ilp"
  );

  private static final InterledgerAddress OPERATOR_ADDRESS =
    InterledgerAddress.of("private.org.interledger.examples.send-money-with-ilp-pay-example")
      .with(SENDER_ACCOUNT_USERNAME);

  /**
   * Main method.
   *
   * @param args A String array.
   */
  public static void main(String[] args) {
    final StreamPacketEncryptionService streamPacketEncryptionService = new StreamPacketEncryptionService(
      StreamCodecContextFactory.oer(), new AesGcmStreamSharedSecretCrypto()
    );

    // Use ILP over HTTP for our underlying link
    final Link<?> link = newIlpOverHttpLink();
    link.setLinkId(LinkId.of("ILP Drip Source Account"));

    final StreamPayer streamPayer = new StreamPayer.Default(
      streamPacketEncryptionService,
      link,
      new DefaultOracleExchangeRateService(
//          new CryptoCompareRateProvider(
//            () -> cryptoCompareApiKey,
//            createObjectMapperForProblemsJson(),
//            newHttpClient(),
//            Caffeine.newBuilder()
//              .expireAfterWrite(1, TimeUnit.HOURS)
//              .build()
//          )
        new FauxRateProvider(new BigDecimal("0.25"))
      )
    );

    final PaymentOptions paymentOptions = PaymentOptions.builder()
      .senderAccountDetails(AccountDetails.builder()
        .denomination(org.interledger.fx.Denominations.XRP_MILLI_DROPS)
        .interledgerAddress(OPERATOR_ADDRESS)
        .build())
      .amountToSend(BigDecimal.valueOf(1.50))
      .destinationPaymentPointer(RECEIVER_PAYMENT_POINTER)
      .paymentTimeout(Duration.ofSeconds(30))
      .slippage(Slippage.of(Percentage.of(new BigDecimal("0.10")))) // <-- Allow up to 10% slippage.
      .build();

    LOGGER.info("paymentOptions: {}\n\n", paymentOptions);

    ///////////////
    // Get a Quote
    final Quote quote = streamPayer.getQuote(paymentOptions)
      // Call .handle for logging...
      .handle((q, throwable) -> {
        if (throwable != null) {
          LOGGER.error(throwable.getMessage(), throwable);
          return null;
        }
        LOGGER.info("Quote: {}", q);
        return q;
      })
      .join();

    ///////////////
    // Send payment using ILP Pay
    final PaymentReceipt paymentReceipt = streamPayer.pay(quote)
      .handle((pr, throwable) -> {
        if (throwable != null) {
          LOGGER.error(throwable.getMessage(), throwable);
          return null;
        } else {
          return pr;
        }
      }).join();

    LOGGER.info("Payment Receipt: {}", paymentReceipt);
    if (paymentReceipt.successfulPayment()) {
      LOGGER.info("\n\nPAYMENT SUCCESS: Sent {} and Delivered {}",
        String.format("%,d XRP DROPS", paymentReceipt.amountSentInSendersUnits()),
        String.format("%,d XRP DROPS", paymentReceipt.amountDeliveredInDestinationUnits())
      );
    } else {
      LOGGER.info("\n\nPAYMENT FAILED: Sent {} and Delivered {}",
        String.format("%,d XRP DROPS", paymentReceipt.amountSentInSendersUnits()),
        String.format("%,d XRP DROPS", paymentReceipt.amountDeliveredInDestinationUnits())
      );
    }

    // Exit without waiting for HTTP clients to shutdown.
    System.exit(-1);
  }

  //////////////////
  // Private Helpers
  //////////////////

  private static Link<?> newIlpOverHttpLink() {
    return new IlpOverHttpLink(
      () -> OPERATOR_ADDRESS,
      TESTNET_URI,
      newHttpClient(),
      createObjectMapperForProblemsJson(),
      InterledgerCodecContextFactory.oer(),
      new SimpleBearerTokenSupplier(SENDER_AUTH_TOKEN)
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
