package org.interledger.examples.drip;

import org.interledger.codecs.ilp.InterledgerCodecContextFactory;
import org.interledger.codecs.stream.StreamCodecContextFactory;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.fluent.Percentage;
import org.interledger.fx.DefaultOracleExchangeRateService;
import org.interledger.fx.Denominations;
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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import okhttp3.ConnectionPool;
import okhttp3.ConnectionSpec;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zalando.problem.ProblemModule;
import org.zalando.problem.violations.ConstraintViolationProblemModule;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Example how to use Quilt to send a STREAM payment. See this module's README for more details.
 */
public class IlpDrip {

  private static final Logger LOGGER = LoggerFactory.getLogger(IlpDrip.class);

  private static final InterledgerAddress OPERATOR_ADDRESS
    = InterledgerAddress.of("private.org.interledger.ilpDrip.application~send-only");

  // Create SimpleStreamSender for sending STREAM payments
  private static StreamPayer streamPayer;

  /**
   * Main method.
   *
   * @param args An array of {@link String} containing application arguments.
   */
  @SuppressWarnings("InfiniteLoopStatement")
  public static void main(String[] args) {

    final String senderAccountUsername;
    final HttpUrl testNetUrl;
    final String senderAuthToken;
    final PaymentPointer receiver1;
    final Optional<PaymentPointer> receiver2;
    final String cryptoCompareApiKey;

    try {
      senderAccountUsername = Optional.ofNullable(args[0])
        .map(val -> {
          LOGGER.debug("Arg0 = `" + val + "`");
          return val;
        })
        .map(String::trim)
        .orElseThrow(() -> new IllegalStateException("Arg0 must contain a ILP source account id."));

      testNetUrl
        = HttpUrl.parse("https://rxprod.wc.wallet.ripplex.io/accounts/" + senderAccountUsername + "/ilp");

      senderAuthToken = Optional.ofNullable(args[1])
        .map(val -> {
          LOGGER.debug("Arg1 = `" + val + "`");
          return val;
        })
        .map(String::trim)
        .orElseThrow(() -> new IllegalStateException("Arg1 must contain a valid ILP auth token."));

      cryptoCompareApiKey = Optional.ofNullable(args[2])
        .map(val -> {
          LOGGER.debug("Arg2 = `" + val + "`");
          return val;
        })
        .map(String::trim)
        .orElseGet(() -> {
          LOGGER.warn("Supply Arg3 if a valid CryptoCompare API Key is desired.");
          return "";
        });

      receiver1 = Optional.ofNullable(args[3])
        .map(val -> {
          LOGGER.debug("Arg3 = `" + val + "`");
          return val;
        })
        .map(String::trim)
        .map(PaymentPointer::of)
        .orElseThrow(() -> new IllegalStateException("Arg2 must contain a valid Payment Pointer."));

      if (args.length > 4) {
        receiver2 = Optional.ofNullable(args[4])
          .map(String::trim)
          .map(PaymentPointer::of);
      } else {
        receiver2 = Optional.empty();
      }

    } catch (Exception e) {
      LOGGER.error(e.getMessage(), e);
      displayUsage();
      return;
    }

    try {
      // Use ILP over HTTP for our underlying link
      final Link<?> link = newIlpOverHttpLink(testNetUrl, senderAuthToken);
      link.setLinkId(LinkId.of("ILP Drip Account"));

      final StreamPacketEncryptionService streamPacketEncryptionService = new StreamPacketEncryptionService(
        StreamCodecContextFactory.oer(), new AesGcmStreamSharedSecretCrypto()
      );

      // Create StreamPayer for sending STREAM payments
      streamPayer = new StreamPayer.Default(
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

      // Program never ends until it's halted at the command-line.
      while (true) {
        LOGGER.info("PAYMENT LINK\n{}\n\n", link);

        CompletableFuture<PaymentReceipt> firstPayment = quoteAndPay(receiver1);
        CompletableFuture<PaymentReceipt> secondPayment = receiver2
          .map(IlpDrip::quoteAndPay)
          .orElseGet(() -> CompletableFuture.completedFuture(null));

        // Wait for both to complete...
        CompletableFuture.allOf(
          firstPayment, secondPayment
        ).get();
        LOGGER.info("\n" +
          "$$$$$$$$$$$$$$$$$$$$$\n" +
          "ALL PAYMENTS COMPLETE\n" +
          "$$$$$$$$$$$$$$$$$$$$$\n"
        );
      }
    } catch (Exception e) {
      LOGGER.error(e.getMessage(), e);
    }
  }

  /**
   * Show the usage message in the console to help the user use the app.
   */
  private static void displayUsage() {
    LOGGER.info(
      "USAGE: java -jar ilp-drip-1.0-SNAPSHOT-spring-boot.jar {senderAccountUsername} {senderAccountToken} " +
        "{cryptoCompareApiKey} {receiver1PaymentPointer} {receiver2PaymentPointer [optional]}"
    );
  }

  private static CompletableFuture<PaymentReceipt> quoteAndPay(final PaymentPointer receiverPaymentPointer) {

    final PaymentOptions paymentOptions = PaymentOptions.builder()
      .senderAccountDetails(AccountDetails.builder()
        .denomination(Denominations.XRP_MILLI_DROPS)
        .interledgerAddress(OPERATOR_ADDRESS)
        .build())
      .amountToSend(BigDecimal.valueOf(1.50))
      .destinationPaymentPointer(receiverPaymentPointer)
      .paymentTimeout(Duration.ofSeconds(30))
      .slippage(Slippage.of(Percentage.of(new BigDecimal("0.10")))) // <-- Allow up to 10% slippage.
      .build();

    LOGGER.info("SEND MONEY REQUEST\n{}\n\n", paymentOptions);

    return streamPayer.getQuote(paymentOptions)
      .handle((quote, throwable) -> {
        if (throwable != null) {
          throw new RuntimeException("No valid quote returned from receiver: " + throwable.getMessage(), throwable);
          //return null;
        } else if (quote != null) {
          LOGGER.info("RECEIPT={}", quote);
          final PaymentReceipt paymentReceipt = streamPayer.pay(quote).join();
          LOGGER.info("STREAM PAY RECEIPT: \n{}\n\n", paymentReceipt);
          return paymentReceipt;
        } else {
          throw new RuntimeException(
            "Neither quote nor throwable was return from streamPayer.getQuote(paymentOptions)");
          //return null;
        }
      });
  }

  private static Link<?> newIlpOverHttpLink(final HttpUrl testNetUrl, final String senderAuthToken) {
    Objects.requireNonNull(testNetUrl);
    Objects.requireNonNull(senderAuthToken);

    return new IlpOverHttpLink(
      () -> OPERATOR_ADDRESS,
      testNetUrl,
      newHttpClient(),
      createObjectMapperForProblemsJson(),
      InterledgerCodecContextFactory.oer(),
      new SimpleBearerTokenSupplier(senderAuthToken)
    );
  }

  private static OkHttpClient newHttpClient() {
    ConnectionPool connectionPool = new ConnectionPool(10, 5, TimeUnit.MINUTES);
    ConnectionSpec spec = new ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS).build();
    OkHttpClient.Builder builder = new OkHttpClient.Builder()
      .connectionSpecs(Arrays.asList(spec, ConnectionSpec.CLEARTEXT))
      .cookieJar(CookieJar.NO_COOKIES)
      .connectTimeout(5000, TimeUnit.MILLISECONDS)
      .readTimeout(35, TimeUnit.SECONDS)
      .writeTimeout(35, TimeUnit.SECONDS);
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
