package org.interledger.examples;

import static okhttp3.CookieJar.NO_COOKIES;

import org.interledger.codecs.ilp.InterledgerCodecContextFactory;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.SharedSecret;
import org.interledger.link.Link;
import org.interledger.link.http.IlpOverHttpLink;
import org.interledger.link.http.IlpOverHttpLinkSettings;
import org.interledger.link.http.IncomingLinkSettings;
import org.interledger.link.http.OutgoingLinkSettings;
import org.interledger.link.http.auth.SimpleBearerTokenSupplier;
import org.interledger.spsp.PaymentPointer;
import org.interledger.spsp.StreamConnectionDetails;
import org.interledger.spsp.client.rust.InterledgerRustNodeClient;
import org.interledger.stream.Denominations;
import org.interledger.stream.SendMoneyRequest;
import org.interledger.stream.SendMoneyResult;
import org.interledger.stream.SenderAmountMode;
import org.interledger.stream.sender.FixedSenderAmountPaymentTracker;
import org.interledger.stream.sender.SimpleStreamSender;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.primitives.UnsignedLong;
import okhttp3.ConnectionPool;
import okhttp3.ConnectionSpec;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;

import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Example how to use Quilt to send a STREAM payment. See this module's README for more details.
 */
public class SendMoneyExample {

  // NOTE - replace this with the username for your sender account
  private static final String SENDER_ACCOUNT_USERNAME = "user_ju7z53sh";
  // NOTE - replace this with the passkey for your sender account
  private static final String SENDER_PASS_KEY = "lzjtsi4ny5f09";
  // NOTE - replace this with the payment pointer for your receiver account
  private static final String RECEIVER_PAYMENT_POINTER = "$rs3.xpring.dev/accounts/user_f5zfkf4a/spsp";

  private static final String TESTNET_URI = "https://rs3.xpring.dev";

  private static final InterledgerAddress SENDER_ADDRESS =
      InterledgerAddress.of("test.xpring-dev.rs3").with(SENDER_ACCOUNT_USERNAME);

  public static void main(String[] args) throws ExecutionException, InterruptedException {
    // Create SPSP client
    InterledgerRustNodeClient spspClient =
        new InterledgerRustNodeClient(newHttpClient(), SENDER_ACCOUNT_USERNAME + ":" + SENDER_PASS_KEY, TESTNET_URI);

    // Fetch shared secret and destination address using SPSP client
    StreamConnectionDetails connectionDetails =
        spspClient.getStreamConnectionDetails(PaymentPointer.of(RECEIVER_PAYMENT_POINTER));

    // Use ILP over HTTP for our underlying link
    Link link = newIlpOverHttpLink();

    // Create SimpleStreamSender for sending STREAM payments
    SimpleStreamSender simpleStreamSender = new SimpleStreamSender(link);

    System.out.println("Starting balance for sender: " + spspClient.getBalance(SENDER_ACCOUNT_USERNAME));

    // Send payment using STREAM
    SendMoneyResult result = simpleStreamSender.sendMoney(
        SendMoneyRequest.builder()
            .sourceAddress(SENDER_ADDRESS)
            .senderAmountMode(SenderAmountMode.SENDER_AMOUNT)
            .amount(UnsignedLong.valueOf(100000))
            .denomination(Denominations.XRP)
            .destinationAddress(connectionDetails.destinationAddress())
            .timeout(Duration.ofMillis(30000))
            .paymentTracker(new FixedSenderAmountPaymentTracker(UnsignedLong.valueOf(100000)))
            .sharedSecret(SharedSecret.of(connectionDetails.sharedSecret().value()))
            .build()
    ).get();

    System.out.println("Send money result: " + result);
    System.out.println("Ending balance for sender: " + spspClient.getBalance(SENDER_ACCOUNT_USERNAME));
  }

  private static Link newIlpOverHttpLink() {
    String sharedSecret = "some random secret here";
    final IlpOverHttpLinkSettings linkSettings = IlpOverHttpLinkSettings.builder()
        .incomingHttpLinkSettings(IncomingLinkSettings.builder()
            .authType(IlpOverHttpLinkSettings.AuthType.SIMPLE)
            .encryptedTokenSharedSecret(sharedSecret)
            .build())
        .outgoingHttpLinkSettings(OutgoingLinkSettings.builder()
            .authType(IlpOverHttpLinkSettings.AuthType.SIMPLE)
            .tokenSubject(SENDER_ACCOUNT_USERNAME)
            .url(HttpUrl.parse(TESTNET_URI + "/ilp"))
            .encryptedTokenSharedSecret(sharedSecret)
            .build())
        .build();

    return new IlpOverHttpLink(
        () -> SENDER_ADDRESS,
        linkSettings,
        newHttpClient(),
        new ObjectMapper(),
        InterledgerCodecContextFactory.oer(),
        new SimpleBearerTokenSupplier(SENDER_ACCOUNT_USERNAME + ":" + SENDER_PASS_KEY)
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

}
