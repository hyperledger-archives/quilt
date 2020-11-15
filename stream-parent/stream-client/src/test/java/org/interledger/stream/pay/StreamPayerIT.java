package org.interledger.stream.pay;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.fluent.Ratio;
import org.interledger.fx.Denomination;
import org.interledger.fx.Slippage;
import org.interledger.link.Link;
import org.interledger.link.LinkId;
import org.interledger.spsp.PaymentPointer;
import org.interledger.spsp.client.SimpleSpspClient;
import org.interledger.stream.model.AccountDetails;
import org.interledger.stream.pay.model.PaymentOptions;
import org.interledger.stream.pay.model.Quote;
import org.interledger.stream.pay.trackers.MaxPacketAmountTracker.MaxPacketState;
import org.junit.Test;

/**
 * Integration test for {@link StreamPayer}.
 */
public class StreamPayerIT extends AbstractIT {

  private StreamPayer streamPayer;

  /**
   * Tests a divisible number of XRP.
   */
  @Test
  public void testGetQuoteFor1000XRP() throws ExecutionException, InterruptedException, TimeoutException {
    /////////
    // IL-DCP
    /////////
    final Link ildcpLink = newIlpOverHttpLink(getSenderAddressForIlDcp());
    final AccountDetails senderAccountDetails = newSenderAccountDetailsViaILDCP(ildcpLink);

    final Link ilpLink = this.newIlpOverHttpLink(senderAccountDetails.interledgerAddress());
    ilpLink.setLinkId(LinkId.of("exchange-rate-probe-it"));

    this.streamPayer = new StreamPayer.Default(
      ilpLink, newExchangeRateProvider(), new SimpleSpspClient()
    );

    final BigDecimal amountToSendInXrp = new BigDecimal("1000");
    final PaymentOptions paymentOptions = PaymentOptions.builder()
      .senderAccountDetails(senderAccountDetails)
      .amountToSend(amountToSendInXrp)
      .destinationPaymentPointer(PaymentPointer.of(RAFIKI_PAYMENT_POINTER))
      .slippage(Slippage.ONE_PERCENT)
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
    assertThat(quote.estimatedPaymentOutcome().maxSourceAmountInSourceUnits()).isEqualTo(BigInteger.valueOf(1000L));
    assertThat(quote.estimatedPaymentOutcome().estimatedNumberOfPackets()).isEqualTo(2);
    assertThat(quote.estimatedPaymentOutcome().minDeliveryAmountInDestinationUnits())
      .isBetween(BigInteger.valueOf(100L), BigInteger.valueOf(300L)); // <- Will be 264 when FX is $0.264 / XRP

    // Min ExchangeRate
    assertThat(quote.minExchangeRate()).isBetween(
      // Between 0.20 and 0.40 cents (allows for FX variability)
      Ratio.from(BigInteger.valueOf(20000), BigInteger.valueOf(100000L)),
      Ratio.from(BigInteger.valueOf(40000), BigInteger.valueOf(100000L))
    );

    // estimated ExchangeRate
    assertThat(quote.estimatedExchangeRate().lowerBoundRate())
      .isBetween(
        // Between 0.20 and 0.40 cents (allows for FX variability)
        Ratio.from(BigInteger.valueOf(20000), BigInteger.valueOf(100000L)),
        Ratio.from(BigInteger.valueOf(40000), BigInteger.valueOf(100000L))
      );

    assertThat(quote.estimatedExchangeRate().upperBoundRate())
      .isBetween(
        // Between 0.20 and 0.40 cents (allows for FX variability)
        Ratio.from(BigInteger.valueOf(20000), BigInteger.valueOf(100000L)),
        Ratio.from(BigInteger.valueOf(40000), BigInteger.valueOf(100000L))
      );

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
    /////////
    // IL-DCP
    /////////
    final Link ildcpLink = newIlpOverHttpLink(getSenderAddressForIlDcp());
    final AccountDetails senderAccountDetails = newSenderAccountDetailsViaILDCP(ildcpLink);

    final Link ilpLink = this.newIlpOverHttpLink(senderAccountDetails.interledgerAddress());
    ilpLink.setLinkId(LinkId.of("exchange-rate-probe-it"));

    this.streamPayer = new StreamPayer.Default(
      ilpLink, newExchangeRateProvider(), new SimpleSpspClient()
    );

    final BigDecimal amountToSendInXrp = new BigDecimal("123");
    final PaymentOptions paymentOptions = PaymentOptions.builder()
      .senderAccountDetails(senderAccountDetails)
      .amountToSend(amountToSendInXrp)
      .destinationPaymentPointer(PaymentPointer.of(RAFIKI_PAYMENT_POINTER))
      .slippage(Slippage.ONE_PERCENT)
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
    assertThat(quote.estimatedPaymentOutcome().maxSourceAmountInSourceUnits()).isEqualTo(BigInteger.valueOf(123L));
    assertThat(quote.estimatedPaymentOutcome().estimatedNumberOfPackets()).isEqualTo(1);
    assertThat(quote.estimatedPaymentOutcome().minDeliveryAmountInDestinationUnits())
      .isBetween(BigInteger.valueOf(10L), BigInteger.valueOf(100L)); // <- Will be 32 when FX is ~$0.26 / XRP

    // Min ExchangeRate
    assertThat(quote.minExchangeRate()).isBetween(
      // Between 0.20 and 0.40 cents (allows for FX variability)
      Ratio.from(BigInteger.valueOf(20000), BigInteger.valueOf(100000L)),
      Ratio.from(BigInteger.valueOf(40000), BigInteger.valueOf(100000L))
    );

    // estimated ExchangeRate
    assertThat(quote.estimatedExchangeRate().lowerBoundRate())
      .isBetween(
        // Between 0.20 and 0.40 cents (allows for FX variability)
        Ratio.from(BigInteger.valueOf(20000), BigInteger.valueOf(100000L)),
        Ratio.from(BigInteger.valueOf(40000), BigInteger.valueOf(100000L))
      );

    assertThat(quote.estimatedExchangeRate().upperBoundRate())
      .isBetween(
        // Between 0.20 and 0.40 cents (allows for FX variability)
        Ratio.from(BigInteger.valueOf(20000), BigInteger.valueOf(100000L)),
        Ratio.from(BigInteger.valueOf(40000), BigInteger.valueOf(100000L))
      );

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
  public void testGetQuoteForVerySmallAmountOfXRP() throws ExecutionException, InterruptedException,
    TimeoutException {
    /////////
    // IL-DCP
    /////////
    final Link ildcpLink = newIlpOverHttpLink(getSenderAddressForIlDcp());
    final AccountDetails senderAccountDetails = newSenderAccountDetailsViaILDCP(ildcpLink);

    final Link ilpLink = this.newIlpOverHttpLink(senderAccountDetails.interledgerAddress());
    ilpLink.setLinkId(LinkId.of("exchange-rate-probe-it"));

    this.streamPayer = new StreamPayer.Default(
      ilpLink, newExchangeRateProvider(), new SimpleSpspClient()
    );

    final BigDecimal amountToSendInXrp = new BigDecimal("0.01");
    final PaymentOptions paymentOptions = PaymentOptions.builder()
      .senderAccountDetails(senderAccountDetails)
      .amountToSend(amountToSendInXrp)
      .destinationPaymentPointer(PaymentPointer.of(RAFIKI_PAYMENT_POINTER))
      .slippage(Slippage.ONE_PERCENT)
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
    assertThat(quote.estimatedPaymentOutcome().maxSourceAmountInSourceUnits()).isEqualTo(BigInteger.valueOf(0L));
    assertThat(quote.estimatedPaymentOutcome().estimatedNumberOfPackets()).isEqualTo(1);
    assertThat(quote.estimatedPaymentOutcome().minDeliveryAmountInDestinationUnits()).isEqualTo(0);

    // Min ExchangeRate
    assertThat(quote.minExchangeRate()).isBetween(
      // Between 0.20 and 0.40 cents (allows for FX variability)
      Ratio.from(BigInteger.valueOf(20000), BigInteger.valueOf(100000L)),
      Ratio.from(BigInteger.valueOf(40000), BigInteger.valueOf(100000L))
    );

    // estimated ExchangeRate
    assertThat(quote.estimatedExchangeRate().lowerBoundRate())
      .isBetween(
        // Between 0.20 and 0.40 cents (allows for FX variability)
        Ratio.from(BigInteger.valueOf(20000), BigInteger.valueOf(100000L)),
        Ratio.from(BigInteger.valueOf(40000), BigInteger.valueOf(100000L))
      );

    assertThat(quote.estimatedExchangeRate().upperBoundRate())
      .isBetween(
        // Between 0.20 and 0.40 cents (allows for FX variability)
        Ratio.from(BigInteger.valueOf(20000), BigInteger.valueOf(100000L)),
        Ratio.from(BigInteger.valueOf(40000), BigInteger.valueOf(100000L))
      );

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

  // TODO: IT for sending 1 XRP unit, which will be too small to deliver anything to USD side.


//  //////////////////
//  // Private Helpers
//  //////////////////

  private InterledgerAddress getSenderAddressForIlDcp() {
    return InterledgerAddress.of(getSenderAddressPrefix().with("ilcdp" .toLowerCase()).getValue());
  }
}