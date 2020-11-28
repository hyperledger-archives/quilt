package org.interledger.stream.pay;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.interledger.codecs.ilp.InterledgerCodecContextFactory;
import org.interledger.codecs.stream.StreamCodecContextFactory;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.fluent.Percentage;
import org.interledger.core.fluent.Ratio;
import org.interledger.fx.Denomination;
import org.interledger.fx.Slippage;
import org.interledger.link.Link;
import org.interledger.link.LinkId;
import org.interledger.link.http.IlpOverHttpLink;
import org.interledger.link.http.auth.SimpleBearerTokenSupplier;
import org.interledger.spsp.PaymentPointer;
import org.interledger.spsp.client.SimpleSpspClient;
import org.interledger.stream.crypto.AesGcmStreamEncryptionService;
import org.interledger.stream.crypto.StreamEncryptionUtils;
import org.interledger.stream.model.AccountDetails;
import org.interledger.stream.pay.model.PaymentOptions;
import org.interledger.stream.pay.model.Quote;
import org.interledger.stream.pay.model.Receipt;
import org.interledger.stream.pay.trackers.MaxPacketAmountTracker.MaxPacketState;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Integration test for {@link StreamPayer}.
 */
public class StreamPayerIT extends AbstractIT {

  // TODO: Stop using rafiki and instead use interledger4j with testcontainers.

  private final Logger logger = LoggerFactory.getLogger(this.getClass());
  private StreamPayer streamPayer;
  private Link ilpLink;
  private Link ildcpLink;
  private StreamEncryptionUtils streamEncryptionUtils;

  @Before
  public void setUp() {
    this.ilpLink = this.newIlpOverHttpLinkForDemoUser();
    this.ildcpLink = this.newIlpOverHttpLinkForILDCP();

    this.streamEncryptionUtils = new StreamEncryptionUtils(
      StreamCodecContextFactory.oer(), new AesGcmStreamEncryptionService()
    );
  }

  /**
   * Tests a divisible number of XRP.
   */
  @Test
  public void testGetQuoteFor1000XRP() throws ExecutionException, InterruptedException, TimeoutException {
    final AccountDetails senderAccountDetails = newSenderAccountDetailsViaILDCP(ildcpLink);
    this.streamPayer = new StreamPayer.Default(
      streamEncryptionUtils, ilpLink, mockExchangeRateProvider(), new SimpleSpspClient()
    );

    final BigDecimal amountToSendInXrp = new BigDecimal("1000");
    final PaymentOptions paymentOptions = PaymentOptions.builder()
      .senderAccountDetails(senderAccountDetails)
      .amountToSend(amountToSendInXrp)
      .destinationPaymentPointer(PaymentPointer.of(RAFIKI_PAYMENT_POINTER))
      .slippage(Slippage.of(Percentage.of(new BigDecimal("0.03")))) // <-- Allow up to 3% slippage.
      .build();
    final Quote quote = streamPayer.getQuote(paymentOptions).get(15, TimeUnit.SECONDS);

    // The current price of XRP in USD is ~$0.24. Thus, for this test we only really care if the price is accurate to
    // within a 100% margin of error.

    assertThat(quote).isNotNull();
    assertThat(quote.estimatedDuration()).isBetween(Duration.ofMillis(5), Duration.ofMillis(100));
    // Accounts
    assertThat(quote.sourceAccount()).isEqualTo(paymentOptions.senderAccountDetails());
    assertThat(quote.destinationAccount().interledgerAddress().getValue()).startsWith("test.rafikius1.mini.");
    assertThat(quote.destinationAccount().denomination()).isPresent();
    assertThat(quote.destinationAccount().denomination().get())
      .isEqualTo(Denomination.builder().assetCode("USD").assetScale((short) 6).build());

    // In Source Units.
    assertThat(quote.estimatedPaymentOutcome().maxSendAmountInWholeSourceUnits()).isEqualTo(BigInteger.valueOf(1000L));
    assertThat(quote.estimatedPaymentOutcome().estimatedNumberOfPackets()).isEqualTo(2);
    assertThat(quote.estimatedPaymentOutcome().minDeliveryAmountInWholeDestinationUnits())
      .isBetween(BigInteger.valueOf(100L), BigInteger.valueOf(300L)); // <- Will be 264 when FX is $0.264 / XRP

    // Min ExchangeRate
    assertThat(quote.minExchangeRate()).isEqualTo(Ratio.from(BigInteger.valueOf(235665962L),
      BigInteger.valueOf(1000000000L)));

    // estimated ExchangeRate
    assertThat(quote.estimatedExchangeRate().lowerBoundRate())
      .isEqualTo(Ratio.from(BigInteger.valueOf(2429546), BigInteger.valueOf(10000000L)));
    assertThat(quote.estimatedExchangeRate().upperBoundRate())
      .isEqualTo(Ratio.from(BigInteger.valueOf(24295461), BigInteger.valueOf(100000000L)));

    assertThat(quote.estimatedExchangeRate().sourceDenomination()).isPresent();
    assertThat(quote.estimatedExchangeRate().sourceDenomination().get().assetCode()).isEqualTo("XRP");
    assertThat(quote.estimatedExchangeRate().sourceDenomination().get().assetScale()).isEqualTo((short) 9);
    assertThat(quote.estimatedExchangeRate().destinationDenomination()).isPresent();
    assertThat(quote.estimatedExchangeRate().destinationDenomination().get().assetCode()).isEqualTo("USD");
    assertThat(quote.estimatedExchangeRate().destinationDenomination().get().assetScale()).isEqualTo((short) 6);
    assertThat(quote.estimatedExchangeRate().maxPacketAmount().maxPacketState()).isEqualTo(MaxPacketState.ImpreciseMax);
    assertThat(quote.estimatedExchangeRate().maxPacketAmount().value()).isPresent();
    assertThat(quote.estimatedExchangeRate().maxPacketAmount().value().get().longValue()).isEqualTo(999999999999L);
    assertThat(quote.paymentOptions()).isEqualTo(paymentOptions);
  }

  /**
   * Tests an unusual amount of XRP.
   */
  @Test
  public void testGetQuoteFor123XRP() throws ExecutionException, InterruptedException, TimeoutException {
    final AccountDetails senderAccountDetails = newSenderAccountDetailsViaILDCP(ildcpLink);

    this.streamPayer = new StreamPayer.Default(
      streamEncryptionUtils, ilpLink, mockExchangeRateProvider(), new SimpleSpspClient()
    );

    final BigDecimal amountToSendInXrp = new BigDecimal("123");
    final PaymentOptions paymentOptions = PaymentOptions.builder()
      .senderAccountDetails(senderAccountDetails)
      .amountToSend(amountToSendInXrp)
      .destinationPaymentPointer(PaymentPointer.of(RAFIKI_PAYMENT_POINTER))
      .slippage(Slippage.of(Percentage.of(new BigDecimal("0.02")))) // <-- Allow up to 2% slippage.
      .build();
    final Quote quote = streamPayer.getQuote(paymentOptions).get(15, TimeUnit.SECONDS);

    ///////////////////
    // Quote Assertions
    ///////////////////
    // The current price of XRP is pegged at $0.2429546 USD in rafiki.
    assertThat(quote).isNotNull();
    assertThat(quote.estimatedDuration()).isBetween(Duration.ofMillis(5), Duration.ofMillis(100));
    assertThat(quote.sourceAccount()).isEqualTo(paymentOptions.senderAccountDetails());
    assertThat(quote.destinationAccount().interledgerAddress().getValue()).startsWith("test.rafikius1.mini.");
    assertThat(quote.destinationAccount().denomination()).isPresent();
    assertThat(quote.destinationAccount().denomination().get())
      .isEqualTo(Denomination.builder().assetCode("USD").assetScale((short) 6).build());
    // In Source Units.
    assertThat(quote.estimatedPaymentOutcome().maxSendAmountInWholeSourceUnits()).isEqualTo(BigInteger.valueOf(123L));
    assertThat(quote.estimatedPaymentOutcome().estimatedNumberOfPackets()).isEqualTo(1);
    assertThat(quote.estimatedPaymentOutcome().minDeliveryAmountInWholeDestinationUnits())
      .isBetween(BigInteger.valueOf(10L), BigInteger.valueOf(100L));
    // Min ExchangeRate
    assertThat(quote.minExchangeRate()).isEqualTo(Ratio.from(BigInteger.valueOf(238095508L),
      BigInteger.valueOf(1000000000L)));

    ///////////////////
    // EstimatedExchangeRate Assertions
    ///////////////////
    assertThat(quote.estimatedExchangeRate().lowerBoundRate())
      .isEqualTo(Ratio.from(BigInteger.valueOf(2429546), BigInteger.valueOf(10000000L)));
    assertThat(quote.estimatedExchangeRate().upperBoundRate())
      .isEqualTo(Ratio.from(BigInteger.valueOf(24295461), BigInteger.valueOf(100000000L)));
    assertThat(quote.estimatedExchangeRate().sourceDenomination()).isPresent();
    assertThat(quote.estimatedExchangeRate().sourceDenomination().get().assetCode()).isEqualTo("XRP");
    assertThat(quote.estimatedExchangeRate().sourceDenomination().get().assetScale()).isEqualTo((short) 9);
    assertThat(quote.estimatedExchangeRate().destinationDenomination()).isPresent();
    assertThat(quote.estimatedExchangeRate().destinationDenomination().get().assetCode()).isEqualTo("USD");
    assertThat(quote.estimatedExchangeRate().destinationDenomination().get().assetScale()).isEqualTo((short) 6);
    assertThat(quote.estimatedExchangeRate().maxPacketAmount().maxPacketState()).isEqualTo(MaxPacketState.ImpreciseMax);
    assertThat(quote.estimatedExchangeRate().maxPacketAmount().value()).isPresent();
    assertThat(quote.estimatedExchangeRate().maxPacketAmount().value().get().longValue()).isEqualTo(999999999999L);
    assertThat(quote.estimatedExchangeRate().verifiedPathCapacity().longValue()).isEqualTo(100000000000L);
    assertThat(quote.paymentOptions()).isEqualTo(paymentOptions);
  }

  /**
   * Tests an unusual amount of XRP.
   */
  @Test
  public void testGetQuoteForVerySmallAmountOfXRP() throws ExecutionException, InterruptedException, TimeoutException {
    final AccountDetails senderAccountDetails = newSenderAccountDetailsViaILDCP(ildcpLink);
    this.streamPayer = new StreamPayer.Default(
      streamEncryptionUtils, ilpLink, mockExchangeRateProvider(), new SimpleSpspClient()
    );

    final BigDecimal amountToSendInXrp = new BigDecimal("0.01");
    final PaymentOptions paymentOptions = PaymentOptions.builder()
      .senderAccountDetails(senderAccountDetails)
      .amountToSend(amountToSendInXrp)
      .destinationPaymentPointer(PaymentPointer.of(RAFIKI_PAYMENT_POINTER))
      // Allow default of 1% slippage.
      .build();
    final Quote quote = streamPayer.getQuote(paymentOptions).get(15, TimeUnit.SECONDS);

    ///////////////////
    // Quote Assertions
    ///////////////////
    // The current price of XRP is pegged at $0.2429546 USD in rafiki.
    assertThat(quote).isNotNull();
    assertThat(quote.estimatedDuration()).isBetween(Duration.ofMillis(5), Duration.ofMillis(100));
    assertThat(quote.sourceAccount()).isEqualTo(paymentOptions.senderAccountDetails());
    assertThat(quote.destinationAccount().interledgerAddress().getValue()).startsWith("test.rafikius1.mini.");
    assertThat(quote.destinationAccount().denomination()).isPresent();
    assertThat(quote.destinationAccount().denomination().get())
      .isEqualTo(Denomination.builder().assetCode("USD").assetScale((short) 6).build());
    // In Source Units.
    assertThat(quote.estimatedPaymentOutcome().maxSendAmountInWholeSourceUnits()).isEqualTo(BigInteger.ZERO);
    assertThat(quote.estimatedPaymentOutcome().estimatedNumberOfPackets()).isEqualTo(1);
    assertThat(quote.estimatedPaymentOutcome().minDeliveryAmountInWholeDestinationUnits()).isEqualTo(BigInteger.ZERO);
    // Min ExchangeRate
    assertThat(quote.minExchangeRate()).isEqualTo(Ratio.from(BigInteger.valueOf(240525054L),
      BigInteger.valueOf(1000000000L)));

    ///////////////////
    // EstimatedExchangeRate Assertions
    ///////////////////
    assertThat(quote.estimatedExchangeRate().lowerBoundRate())
      .isEqualTo(Ratio.from(BigInteger.valueOf(2429546), BigInteger.valueOf(10000000L)));
    assertThat(quote.estimatedExchangeRate().upperBoundRate())
      .isEqualTo(Ratio.from(BigInteger.valueOf(24295461), BigInteger.valueOf(100000000L)));
    assertThat(quote.estimatedExchangeRate().sourceDenomination()).isPresent();
    assertThat(quote.estimatedExchangeRate().sourceDenomination().get().assetCode()).isEqualTo("XRP");
    assertThat(quote.estimatedExchangeRate().sourceDenomination().get().assetScale()).isEqualTo((short) 9);
    assertThat(quote.estimatedExchangeRate().destinationDenomination()).isPresent();
    assertThat(quote.estimatedExchangeRate().destinationDenomination().get().assetCode()).isEqualTo("USD");
    assertThat(quote.estimatedExchangeRate().destinationDenomination().get().assetScale()).isEqualTo((short) 6);
    assertThat(quote.estimatedExchangeRate().maxPacketAmount().maxPacketState()).isEqualTo(MaxPacketState.ImpreciseMax);
    assertThat(quote.estimatedExchangeRate().maxPacketAmount().value()).isPresent();
    assertThat(quote.estimatedExchangeRate().maxPacketAmount().value().get().longValue()).isEqualTo(999999999999L);
    assertThat(quote.estimatedExchangeRate().verifiedPathCapacity().longValue()).isEqualTo(100000000000L);
    assertThat(quote.paymentOptions()).isEqualTo(paymentOptions);
  }

  @Test
  public void testPay1XRP() throws ExecutionException, InterruptedException, TimeoutException {
    final AccountDetails senderAccountDetails = newSenderAccountDetailsViaILDCP(ildcpLink);

    this.streamPayer = new StreamPayer.Default(
      streamEncryptionUtils, ilpLink, mockExchangeRateProvider(), new SimpleSpspClient()
    );

    final BigDecimal amountToSendInXrp = new BigDecimal("4"); // <-- Send 4 XRP
    final PaymentOptions paymentOptions = PaymentOptions.builder()
      .senderAccountDetails(senderAccountDetails)
      .amountToSend(amountToSendInXrp)
      .destinationPaymentPointer(PaymentPointer.of(RAFIKI_PAYMENT_POINTER))
      // V-- Allow up to 1% slippage.
      .slippage(Slippage.of(Percentage.of(new BigDecimal("0.01"))))
      .build();

    streamPayer.getQuote(paymentOptions)
      .handle((quote, throwable) -> {
        if (throwable != null) {
          fail("No valid quote returned from receiver: " + throwable.getMessage(), throwable);
          return null;
        } else if (quote != null) {
          logger.info("Quote={}", quote);

          ///////////////////
          // Quote Assertions
          ///////////////////
          // The current price of XRP is pegged at $0.2429546 USD in rafiki.
          assertThat(quote).isNotNull();
          assertThat(quote.estimatedDuration()).isBetween(Duration.ofMillis(5), Duration.ofMillis(100));
          assertThat(quote.sourceAccount()).isEqualTo(paymentOptions.senderAccountDetails());
          assertThat(quote.destinationAccount().interledgerAddress().getValue()).startsWith("test.rafikius1.mini.");
          assertThat(quote.destinationAccount().denomination()).isPresent();
          assertThat(quote.destinationAccount().denomination().get())
            .isEqualTo(Denomination.builder().assetCode("USD").assetScale((short) 6).build());
          // In Source Units.
          assertThat(quote.estimatedPaymentOutcome().maxSendAmountInWholeSourceUnits())
            .isEqualTo(BigInteger.valueOf(4L));
          assertThat(quote.estimatedPaymentOutcome().estimatedNumberOfPackets()).isEqualTo(1);
          assertThat(quote.estimatedPaymentOutcome().minDeliveryAmountInWholeDestinationUnits())
            .isEqualTo(BigInteger.ONE);
          // Min ExchangeRate, due to slippage
          assertThat(quote.minExchangeRate()).isEqualTo(Ratio.from(BigInteger.valueOf(240525054),
            BigInteger.valueOf(1000000000L)));

          ///////////////////
          // EstimatedExchangeRate Assertions
          ///////////////////
          assertThat(quote.estimatedExchangeRate().lowerBoundRate())
            .isEqualTo(Ratio.from(BigInteger.valueOf(2429546), BigInteger.valueOf(10000000L)));
          assertThat(quote.estimatedExchangeRate().upperBoundRate())
            .isEqualTo(Ratio.from(BigInteger.valueOf(24295461), BigInteger.valueOf(100000000L)));
          assertThat(quote.estimatedExchangeRate().sourceDenomination()).isPresent();
          assertThat(quote.estimatedExchangeRate().sourceDenomination().get().assetCode()).isEqualTo("XRP");
          assertThat(quote.estimatedExchangeRate().sourceDenomination().get().assetScale()).isEqualTo((short) 9);
          assertThat(quote.estimatedExchangeRate().destinationDenomination()).isPresent();
          assertThat(quote.estimatedExchangeRate().destinationDenomination().get().assetCode()).isEqualTo("USD");
          assertThat(quote.estimatedExchangeRate().destinationDenomination().get().assetScale()).isEqualTo((short) 6);
          assertThat(quote.estimatedExchangeRate().maxPacketAmount().maxPacketState())
            .isEqualTo(MaxPacketState.ImpreciseMax);
          assertThat(quote.estimatedExchangeRate().maxPacketAmount().value()).isPresent();
          assertThat(quote.estimatedExchangeRate().maxPacketAmount().value().get().longValue())
            .isEqualTo(999999999999L);
          assertThat(quote.estimatedExchangeRate().verifiedPathCapacity().longValue()).isEqualTo(100000000000L);
          assertThat(quote.paymentOptions()).isEqualTo(paymentOptions);

          final Receipt receipt = streamPayer.pay(quote).join();
          logger.info("Receipt={}", receipt);
          assertThat(receipt.paymentError()).isEmpty();
          assertThat(receipt.originalQuote()).isEqualTo(quote);
          assertThat(receipt.amountSentInSendersUnits()).isEqualTo(
            amountToSendInXrp.movePointRight(senderAccountDetails.denomination().get().assetScale()).toBigIntegerExact()
          );
          assertThat(receipt.amountDeliveredInDestinationUnits()).isLessThan(
            amountToSendInXrp.movePointRight(senderAccountDetails.denomination().get().assetScale()).toBigIntegerExact()
          );
          // TODO: Once Rust connector is being used instead, assert the actual amount based on the pegged FX rate.
          return quote;
        } else {
          fail("Neither quote nor throwable was return from streamPayer.getQuote(paymentOptions)");
          return null;
        }
      }).get(15, TimeUnit.SECONDS);  // <-- Don't wait too long for this to timeout.
  }

  /**
   * 1 drop is too small to make its way to Rafiki, which has a precision of 6. Therefore, the only way to get 1 unit to
   * rafiki from an account with scale 9 is to send at least 1000 drops, or .001 XRP.
   */
  @Test
  public void testPay1Drop() throws ExecutionException, InterruptedException, TimeoutException {
    final AccountDetails senderAccountDetails = newSenderAccountDetailsViaILDCP(ildcpLink);

    this.streamPayer = new StreamPayer.Default(
      streamEncryptionUtils, ilpLink, mockExchangeRateProvider(), new SimpleSpspClient()
    );

    final BigDecimal amountToSendInXrp = new BigDecimal("0.000001"); // <-- Send 1 XRP Drop
    final PaymentOptions paymentOptions = PaymentOptions.builder()
      .senderAccountDetails(senderAccountDetails)
      .amountToSend(amountToSendInXrp)
      .destinationPaymentPointer(PaymentPointer.of(RAFIKI_PAYMENT_POINTER))
      .build();

    streamPayer.getQuote(paymentOptions)
      .handle((quote, throwable) -> {
        if (throwable != null) {
          fail("No valid quote returned from receiver: " + throwable.getMessage(), throwable);
          return null;
        } else if (quote != null) {
          logger.info("Quote={}", quote);

          ///////////////////
          // Quote Assertions
          ///////////////////
          // The current price of XRP is pegged at $0.2429546 USD in rafiki.
          assertThat(quote).isNotNull();
          assertThat(quote.estimatedDuration()).isBetween(Duration.ofMillis(5), Duration.ofMillis(100));
          assertThat(quote.sourceAccount()).isEqualTo(paymentOptions.senderAccountDetails());
          assertThat(quote.destinationAccount().interledgerAddress().getValue()).startsWith("test.rafikius1.mini.");
          assertThat(quote.destinationAccount().denomination()).isPresent();
          assertThat(quote.destinationAccount().denomination().get())
            .isEqualTo(Denomination.builder().assetCode("USD").assetScale((short) 6).build());
          // In Source Units.
          assertThat(quote.estimatedPaymentOutcome().maxSendAmountInWholeSourceUnits()).isEqualTo(BigInteger.ZERO);
          assertThat(quote.estimatedPaymentOutcome().estimatedNumberOfPackets()).isEqualTo(1);
          assertThat(quote.estimatedPaymentOutcome().minDeliveryAmountInWholeDestinationUnits())
            .isEqualTo(BigInteger.ZERO);
          // Min ExchangeRate, due to slippage
          assertThat(quote.minExchangeRate()).isEqualTo(Ratio.from(BigInteger.valueOf(240525054),
            BigInteger.valueOf(1000000000L)));

          ///////////////////
          // EstimatedExchangeRate Assertions
          ///////////////////
          assertThat(quote.estimatedExchangeRate().lowerBoundRate())
            .isEqualTo(Ratio.from(BigInteger.valueOf(2429546), BigInteger.valueOf(10000000L)));
          assertThat(quote.estimatedExchangeRate().upperBoundRate())
            .isEqualTo(Ratio.from(BigInteger.valueOf(24295461), BigInteger.valueOf(100000000L)));
          assertThat(quote.estimatedExchangeRate().sourceDenomination()).isPresent();
          assertThat(quote.estimatedExchangeRate().sourceDenomination().get().assetCode()).isEqualTo("XRP");
          assertThat(quote.estimatedExchangeRate().sourceDenomination().get().assetScale()).isEqualTo((short) 9);
          assertThat(quote.estimatedExchangeRate().destinationDenomination()).isPresent();
          assertThat(quote.estimatedExchangeRate().destinationDenomination().get().assetCode()).isEqualTo("USD");
          assertThat(quote.estimatedExchangeRate().destinationDenomination().get().assetScale()).isEqualTo((short) 6);
          assertThat(quote.estimatedExchangeRate().maxPacketAmount().maxPacketState())
            .isEqualTo(MaxPacketState.ImpreciseMax);
          assertThat(quote.estimatedExchangeRate().maxPacketAmount().value()).isPresent();
          assertThat(quote.estimatedExchangeRate().maxPacketAmount().value().get().longValue())
            .isEqualTo(999999999999L);
          assertThat(quote.estimatedExchangeRate().verifiedPathCapacity().longValue()).isEqualTo(100000000000L);
          assertThat(quote.paymentOptions()).isEqualTo(paymentOptions);

          final Receipt receipt = streamPayer.pay(quote).join();
          logger.info("Receipt={}", receipt);
          assertThat(receipt.paymentError()).isEmpty();
          assertThat(receipt.originalQuote()).isEqualTo(quote);
          assertThat(receipt.amountSentInSendersUnits()).isEqualTo(
            amountToSendInXrp.movePointRight(senderAccountDetails.denomination().get().assetScale()).toBigIntegerExact()
          );
          assertThat(receipt.amountDeliveredInDestinationUnits()).isLessThan(
            amountToSendInXrp.movePointRight(senderAccountDetails.denomination().get().assetScale()).toBigIntegerExact()
          );
          // TODO: Once Rust connector is being used instead, assert the actual amount based on the pegged FX rate.
          return quote;
        } else {
          fail("Neither quote nor throwable was return from streamPayer.getQuote(paymentOptions)");
          return null;
        }
      }).get(15, TimeUnit.SECONDS);  // <-- Don't wait too long for this to timeout.
  }

  //////////////////
  // Private Helpers
  //////////////////

  /**
   * Helper method to construct a new {@link Link} for transmitting ILP packets.
   *
   * @return A {@link Link}.
   */
  private Link newIlpOverHttpLinkForILDCP() {
    final Link link = new IlpOverHttpLink(
      this::getSenderAddressForIlDcp,
      getTestnetUrl(ACCOUNT_USERNAME_DEMO_USER),
      newHttpClient(),
      newObjectMapperForProblemsJson(),
      InterledgerCodecContextFactory.oer(),
      new SimpleBearerTokenSupplier(PASS_KEY_DEMO_USER)
    );

    link.setLinkId(LinkId.of(getClass().getSimpleName() + "-ildcp"));
    return link;
  }

  private InterledgerAddress getSenderAddressForIlDcp() {
    return InterledgerAddress.of(getSenderAddressPrefix().with("ilcdp".toLowerCase()).getValue());
  }
}