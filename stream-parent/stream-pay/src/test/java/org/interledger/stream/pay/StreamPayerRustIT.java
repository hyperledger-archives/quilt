package org.interledger.stream.pay;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import com.google.common.primitives.UnsignedLong;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.interledger.core.fluent.Percentage;
import org.interledger.core.fluent.Ratio;
import org.interledger.fx.Denomination;
import org.interledger.fx.Slippage;
import org.interledger.link.Link;
import org.interledger.spsp.PaymentPointer;
import org.interledger.stream.model.AccountDetails;
import org.interledger.stream.pay.model.PaymentOptions;
import org.interledger.stream.pay.model.Quote;
import org.interledger.stream.pay.model.Receipt;
import org.interledger.stream.pay.trackers.MaxPacketAmountTracker.MaxPacketState;
import org.junit.Test;

/**
 * Integration test for {@link StreamPayer}.
 */
// TODO: Create an IT that hits the Java Connector via TestContainers.
public class StreamPayerRustIT extends AbstractRustIT {

  private StreamPayer streamPayer;

  /**
   * Tests a divisible number of XRP (i.e., 1000, which should translate into about $40).
   */
  @Test
  public void testGetQuoteFor1000XRP() throws ExecutionException, InterruptedException, TimeoutException {
    final Link ilpLink = this.constructIlpOverHttpLink(XRP_ACCOUNT); // <-- All ILP operations from XRP_ACCOUNT
    final AccountDetails senderAccountDetails = newSenderAccountDetailsViaILDCP(ilpLink);

    this.streamPayer = new StreamPayer.Default(streamEncryptionUtils, ilpLink, mockExchangeRateProvider(), spspClient);

    final BigDecimal amountToSendInXrp = new BigDecimal("1000");

    final PaymentOptions paymentOptions = PaymentOptions.builder()
      .senderAccountDetails(senderAccountDetails)
      .amountToSend(amountToSendInXrp)
      .destinationPaymentPointer(PaymentPointer.of(PAYMENT_POINTER_USD))
      .slippage(Slippage.of(Percentage.of(new BigDecimal("0.03")))) // <-- Allow up to 3% slippage.
      .build();

    final Quote quote = streamPayer.getQuote(paymentOptions).get(15, TimeUnit.SECONDS);

    assertThat(quote).isNotNull();
    assertThat(quote.estimatedDuration()).isBetween(Duration.ofMillis(5), Duration.ofMillis(100));
    // Accounts
    assertThat(quote.sourceAccount()).isEqualTo(paymentOptions.senderAccountDetails());
    assertThat(quote.destinationAccount().interledgerAddress().getValue()).startsWith(HOST_ADDRESS.getValue());
    assertThat(quote.destinationAccount().denomination()).isPresent();
    assertThat(quote.destinationAccount().denomination().get())
      .isEqualTo(Denomination.builder().assetCode("USD").assetScale((short) 6).build());

    // In Source Units.
    assertThat(quote.estimatedPaymentOutcome().maxSendAmountInWholeSourceUnits()).isEqualTo(BigInteger.valueOf(1000L));
    assertThat(quote.estimatedPaymentOutcome().estimatedNumberOfPackets()).isEqualTo(1);

    assertThat(quote.estimatedPaymentOutcome().minDeliveryAmountInWholeDestinationUnits())
      .isEqualTo(BigInteger.valueOf(236)); // <- Due to up-to 3% slippage.

    // Min ExchangeRate
    assertThat(quote.minExchangeRate()).isEqualTo(Ratio.from(BigInteger.valueOf(235665962L),
      BigInteger.valueOf(1000000000L)));

    // estimated ExchangeRate
    assertThat(quote.estimatedExchangeRate().lowerBoundRate())
      .isEqualTo(Ratio.from(BigInteger.valueOf(2429546), BigInteger.valueOf(10000000L)));
    assertThat(quote.estimatedExchangeRate().upperBoundRate())
      .isEqualTo(Ratio.from(BigInteger.valueOf(242954601), BigInteger.valueOf(1000000000L)));

    assertThat(quote.estimatedExchangeRate().sourceDenomination()).isPresent();
    assertThat(quote.estimatedExchangeRate().sourceDenomination().get().assetCode()).isEqualTo("XRP");
    assertThat(quote.estimatedExchangeRate().sourceDenomination().get().assetScale()).isEqualTo((short) 9);
    assertThat(quote.estimatedExchangeRate().destinationDenomination()).isPresent();
    assertThat(quote.estimatedExchangeRate().destinationDenomination().get().assetCode()).isEqualTo("USD");
    assertThat(quote.estimatedExchangeRate().destinationDenomination().get().assetScale()).isEqualTo((short) 6);
    assertThat(quote.estimatedExchangeRate().maxPacketAmount().maxPacketState()).isEqualTo(MaxPacketState.UnknownMax);
    assertThat(quote.estimatedExchangeRate().maxPacketAmount().value()).isEqualTo(UnsignedLong.MAX_VALUE);
    assertThat(quote.paymentOptions()).isEqualTo(paymentOptions);
  }

  /**
   * Tests a non-uniform amount of XRP (i.e., 123) which should translate into about 29.5 USD.
   */
  @Test
  public void testGetQuoteFor123XRP() throws ExecutionException, InterruptedException, TimeoutException {
    final Link ilpLink = this.constructIlpOverHttpLink(XRP_ACCOUNT); // <-- All ILP operations from XRP_ACCOUNT
    final AccountDetails senderAccountDetails = newSenderAccountDetailsViaILDCP(ilpLink);

    this.streamPayer = new StreamPayer.Default(streamEncryptionUtils, ilpLink, mockExchangeRateProvider(), spspClient);

    final BigDecimal amountToSendInXrp = new BigDecimal("123");

    final PaymentOptions paymentOptions = PaymentOptions.builder()
      .senderAccountDetails(senderAccountDetails)
      .amountToSend(amountToSendInXrp)
      .destinationPaymentPointer(PaymentPointer.of(PAYMENT_POINTER_USD))
      .slippage(Slippage.of(Percentage.of(new BigDecimal("0.02")))) // <-- Allow up to 2% slippage.
      .build();

    final Quote quote = streamPayer.getQuote(paymentOptions).get(15, TimeUnit.SECONDS);

    ///////////////////
    // Quote Assertions
    ///////////////////
    // The current price of XRP is pegged at $0.2429546 USD on the test Rust Connector (see initAccounts).
    assertThat(quote).isNotNull();
    assertThat(quote.estimatedDuration()).isBetween(Duration.ofMillis(5), Duration.ofMillis(100));
    assertThat(quote.sourceAccount()).isEqualTo(paymentOptions.senderAccountDetails());
    assertThat(quote.destinationAccount().interledgerAddress().getValue()).startsWith(HOST_ADDRESS.getValue());
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
      .isEqualTo(Ratio.from(BigInteger.valueOf(242954601), BigInteger.valueOf(1000000000L)));
    assertThat(quote.estimatedExchangeRate().sourceDenomination()).isPresent();
    assertThat(quote.estimatedExchangeRate().sourceDenomination().get().assetCode()).isEqualTo("XRP");
    assertThat(quote.estimatedExchangeRate().sourceDenomination().get().assetScale()).isEqualTo((short) 9);
    assertThat(quote.estimatedExchangeRate().destinationDenomination()).isPresent();
    assertThat(quote.estimatedExchangeRate().destinationDenomination().get().assetCode()).isEqualTo("USD");
    assertThat(quote.estimatedExchangeRate().destinationDenomination().get().assetScale()).isEqualTo((short) 6);
    assertThat(quote.estimatedExchangeRate().maxPacketAmount().maxPacketState()).isEqualTo(MaxPacketState.UnknownMax);
    assertThat(quote.estimatedExchangeRate().maxPacketAmount().value()).isEqualTo(UnsignedLong.MAX_VALUE);
    assertThat(quote.estimatedExchangeRate().verifiedPathCapacity().longValue()).isEqualTo(1000000000000L);
    assertThat(quote.paymentOptions()).isEqualTo(paymentOptions);
  }

  /**
   * Assert that a quote can be retrieved even if teh amount of XRP is too small to deliver anything to the receiver.
   */
  @Test
  public void testGetQuoteSmallAmountOfXRP() throws ExecutionException, InterruptedException, TimeoutException {
    final Link ilpLink = this.constructIlpOverHttpLink(XRP_ACCOUNT); // <-- All ILP operations from XRP_ACCOUNT
    final AccountDetails senderAccountDetails = newSenderAccountDetailsViaILDCP(ilpLink);

    this.streamPayer = new StreamPayer.Default(streamEncryptionUtils, ilpLink, mockExchangeRateProvider(), spspClient);

    final BigDecimal amountToSendInXrp = new BigDecimal("0.01");

    final PaymentOptions paymentOptions = PaymentOptions.builder()
      .senderAccountDetails(senderAccountDetails)
      .amountToSend(amountToSendInXrp)
      .destinationPaymentPointer(PaymentPointer.of(PAYMENT_POINTER_USD))
      .slippage(Slippage.of(Percentage.of(new BigDecimal("0.01")))) // <-- Allow up to 1% slippage.
      .build();

    final Quote quote = streamPayer.getQuote(paymentOptions).get(15, TimeUnit.SECONDS);

    ///////////////////
    // Quote Assertions
    ///////////////////
    // The current price of XRP is pegged at $0.2429546 USD on the test Rust Connector (see initAccounts).
    assertThat(quote).isNotNull();
    assertThat(quote.estimatedDuration()).isBetween(Duration.ofMillis(5), Duration.ofMillis(100));
    assertThat(quote.sourceAccount()).isEqualTo(paymentOptions.senderAccountDetails());
    assertThat(quote.destinationAccount().interledgerAddress().getValue()).startsWith(HOST_ADDRESS.getValue());
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
      .isEqualTo(Ratio.from(BigInteger.valueOf(242954601), BigInteger.valueOf(1000000000L)));
    assertThat(quote.estimatedExchangeRate().sourceDenomination()).isPresent();
    assertThat(quote.estimatedExchangeRate().sourceDenomination().get().assetCode()).isEqualTo("XRP");
    assertThat(quote.estimatedExchangeRate().sourceDenomination().get().assetScale()).isEqualTo((short) 9);
    assertThat(quote.estimatedExchangeRate().destinationDenomination()).isPresent();
    assertThat(quote.estimatedExchangeRate().destinationDenomination().get().assetCode()).isEqualTo("USD");
    assertThat(quote.estimatedExchangeRate().destinationDenomination().get().assetScale()).isEqualTo((short) 6);
    assertThat(quote.estimatedExchangeRate().maxPacketAmount().maxPacketState()).isEqualTo(MaxPacketState.UnknownMax);
    assertThat(quote.estimatedExchangeRate().maxPacketAmount().value()).isEqualTo(UnsignedLong.MAX_VALUE);
    assertThat(quote.estimatedExchangeRate().verifiedPathCapacity().longValue()).isEqualTo(1000000000000L);
    assertThat(quote.paymentOptions()).isEqualTo(paymentOptions);
  }

  @Test
  public void testPay1XRP() throws ExecutionException, InterruptedException, TimeoutException {
    final Link ilpLink = this.constructIlpOverHttpLink(XRP_ACCOUNT); // <-- All ILP operations from XRP_ACCOUNT
    final AccountDetails senderAccountDetails = newSenderAccountDetailsViaILDCP(ilpLink);

    this.streamPayer = new StreamPayer.Default(streamEncryptionUtils, ilpLink, mockExchangeRateProvider(), spspClient);

    final BigDecimal amountToSendInXrp = new BigDecimal("4"); // <-- Send 4 XRP
    final PaymentOptions paymentOptions = PaymentOptions.builder()
      .senderAccountDetails(senderAccountDetails)
      .amountToSend(amountToSendInXrp)
      .destinationPaymentPointer(PaymentPointer.of(PAYMENT_POINTER_USD))
      .slippage(Slippage.of(Percentage.of(new BigDecimal("0.01")))) // <-- Allow up to 1% slippage.
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
          // The current price of XRP is pegged at $0.2429546 USD on the test Rust Connector (see initAccounts).
          assertThat(quote).isNotNull();
          assertThat(quote.estimatedDuration()).isBetween(Duration.ofMillis(5), Duration.ofMillis(100));
          assertThat(quote.sourceAccount()).isEqualTo(paymentOptions.senderAccountDetails());
          assertThat(quote.destinationAccount().interledgerAddress().getValue()).startsWith(HOST_ADDRESS.getValue());
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
            .isEqualTo(Ratio.from(BigInteger.valueOf(242954601), BigInteger.valueOf(1000000000L)));
          assertThat(quote.estimatedExchangeRate().sourceDenomination()).isPresent();
          assertThat(quote.estimatedExchangeRate().sourceDenomination().get().assetCode()).isEqualTo("XRP");
          assertThat(quote.estimatedExchangeRate().sourceDenomination().get().assetScale()).isEqualTo((short) 9);
          assertThat(quote.estimatedExchangeRate().destinationDenomination()).isPresent();
          assertThat(quote.estimatedExchangeRate().destinationDenomination().get().assetCode()).isEqualTo("USD");
          assertThat(quote.estimatedExchangeRate().destinationDenomination().get().assetScale()).isEqualTo((short) 6);
          assertThat(quote.estimatedExchangeRate().maxPacketAmount().maxPacketState())
            .isEqualTo(MaxPacketState.UnknownMax);
          assertThat(quote.estimatedExchangeRate().maxPacketAmount().value()).isEqualTo(UnsignedLong.MAX_VALUE);
          assertThat(quote.estimatedExchangeRate().verifiedPathCapacity().longValue()).isEqualTo(1000000000000L);
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
          return quote;
        } else {
          fail("Neither quote nor throwable was return from streamPayer.getQuote(paymentOptions)");
          return null;
        }
      }).get(15, TimeUnit.SECONDS);  // <-- Don't wait too long for this to timeout.
  }

  /**
   * 1 drop is too small to make its way to the USD account, which has a precision of 6. Therefore, the only way to get
   * 1 unit to rafiki from an account with scale 9 is to send at least 1000 drops, or .001 XRP. This test validates that
   * arithmetic.
   */
  @Test
  public void testPay1Drop() throws ExecutionException, InterruptedException, TimeoutException {
    final Link ilpLink = this.constructIlpOverHttpLink(XRP_ACCOUNT); // <-- All ILP operations from XRP_ACCOUNT
    final AccountDetails senderAccountDetails = newSenderAccountDetailsViaILDCP(ilpLink);

    this.streamPayer = new StreamPayer.Default(streamEncryptionUtils, ilpLink, mockExchangeRateProvider(), spspClient);

    final BigDecimal amountToSendInXrp = new BigDecimal("0.000001"); // <-- Send 1 XRP Drop
    final PaymentOptions paymentOptions = PaymentOptions.builder()
      .senderAccountDetails(senderAccountDetails)
      .amountToSend(amountToSendInXrp)
      .destinationPaymentPointer(PaymentPointer.of(PAYMENT_POINTER_USD))
      .slippage(Slippage.of(Percentage.of(new BigDecimal("0.01")))) // <-- Allow up to 1% slippage.
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
          assertThat(quote.destinationAccount().interledgerAddress().getValue()).startsWith(HOST_ADDRESS.getValue());
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
            .isEqualTo(Ratio.from(BigInteger.valueOf(242954601), BigInteger.valueOf(1000000000L)));
          assertThat(quote.estimatedExchangeRate().sourceDenomination()).isPresent();
          assertThat(quote.estimatedExchangeRate().sourceDenomination().get().assetCode()).isEqualTo("XRP");
          assertThat(quote.estimatedExchangeRate().sourceDenomination().get().assetScale()).isEqualTo((short) 9);
          assertThat(quote.estimatedExchangeRate().destinationDenomination()).isPresent();
          assertThat(quote.estimatedExchangeRate().destinationDenomination().get().assetCode()).isEqualTo("USD");
          assertThat(quote.estimatedExchangeRate().destinationDenomination().get().assetScale()).isEqualTo((short) 6);
          assertThat(quote.estimatedExchangeRate().maxPacketAmount().maxPacketState())
            .isEqualTo(MaxPacketState.UnknownMax);
          assertThat(quote.estimatedExchangeRate().maxPacketAmount().value()).isEqualTo(UnsignedLong.MAX_VALUE);
          assertThat(quote.estimatedExchangeRate().verifiedPathCapacity().longValue()).isEqualTo(1000000000000L);
          assertThat(quote.paymentOptions()).isEqualTo(paymentOptions);

          final Receipt receipt = streamPayer.pay(quote).join();
          logger.info("Receipt={}", receipt);
          assertThat(receipt.paymentError()).isEmpty();
          assertThat(receipt.originalQuote()).isEqualTo(quote);
          assertThat(receipt.amountSentInSendersUnits()).isEqualTo(BigInteger.ZERO);
          assertThat(receipt.amountDeliveredInDestinationUnits()).isEqualTo(BigInteger.ZERO);
          return quote;
        } else {
          fail("Neither quote nor throwable was return from streamPayer.getQuote(paymentOptions)");
          return null;
        }
      }).get(15, TimeUnit.SECONDS);  // <-- Don't wait too long for this to timeout.
  }

}