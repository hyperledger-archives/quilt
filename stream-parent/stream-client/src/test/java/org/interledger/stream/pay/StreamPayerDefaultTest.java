package org.interledger.stream.pay;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.primitives.UnsignedLong;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import javax.money.convert.ExchangeRate;
import javax.money.convert.ExchangeRateProvider;
import javax.money.convert.RateType;
import okhttp3.HttpUrl;
import org.assertj.core.util.Maps;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerErrorCode;
import org.interledger.core.SharedSecret;
import org.interledger.core.fluent.Ratio;
import org.interledger.fx.Denomination;
import org.interledger.fx.Denominations;
import org.interledger.core.fluent.Percentage;
import org.interledger.fx.ScaledExchangeRate;
import org.interledger.fx.Slippage;
import org.interledger.link.LinkId;
import org.interledger.link.LinkSettings;
import org.interledger.link.LinkType;
import org.interledger.link.LoopbackLink;
import org.interledger.link.PacketRejector;
import org.interledger.spsp.PaymentPointer;
import org.interledger.spsp.StreamConnectionDetails;
import org.interledger.spsp.client.InvalidReceiverClientException;
import org.interledger.spsp.client.SimpleSpspClient;
import org.interledger.spsp.client.SpspClient;
import org.interledger.stream.model.AccountDetails;
import org.interledger.stream.pay.StreamPayer.Default;
import org.interledger.stream.pay.exceptions.StreamPayerException;
import org.interledger.stream.pay.model.ExchangeRateProbeOutcome;
import org.interledger.stream.pay.model.PaymentOptions;
import org.interledger.stream.pay.model.SendState;
import org.interledger.stream.pay.trackers.AmountTracker;
import org.interledger.stream.pay.trackers.AssetDetailsTracker;
import org.interledger.stream.pay.trackers.ExchangeRateTracker;
import org.interledger.stream.pay.trackers.MaxPacketAmountTracker.MaxPacketAmount;
import org.interledger.stream.pay.trackers.MaxPacketAmountTracker.MaxPacketState;
import org.interledger.stream.pay.trackers.PacingTracker;
import org.interledger.stream.pay.trackers.PaymentSharedStateTracker;
import org.javamoney.moneta.CurrencyUnitBuilder;
import org.javamoney.moneta.convert.ExchangeRateBuilder;
import org.javamoney.moneta.spi.DefaultNumberValue;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.MockitoAnnotations;

/**
 * Unit tests for {@link StreamPayer.Default}.
 */
public class StreamPayerDefaultTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private static final InterledgerAddress LINK_OPERATOR_ADDRESS = InterledgerAddress.of("example.connector");

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  //////////////////////////////////
  // Quoting Flows
  //////////////////////////////////

  @Test
  public void failsIfPaymentPointerCannotResolve() throws ExecutionException, InterruptedException {
    final AccountDetails sourceAccountDetails = getSourceAccountDetails();
    final LoopbackLink simulatedLink = this.getLinkForTesting();

    StreamPayer streamPayer = new Default(
      simulatedLink, newExternalExchangeRateProvider(Ratio.ONE), new SimpleSpspClient()
    );

    final BigDecimal amountToSendInXrp = new BigDecimal("1000");
    final PaymentOptions paymentOptions = PaymentOptions.builder()
      .senderAccountDetails(sourceAccountDetails)
      .amountToSend(amountToSendInXrp)
      .destinationPaymentPointer(PaymentPointer.of("$example.com/foo"))
      .build();
    streamPayer.getQuote(paymentOptions).exceptionally(throwable -> {
      assertThat(throwable.getCause().getMessage()).isEqualTo("Unable to obtain STREAM connection details via SPSP.");
      assertThat(throwable.getCause() instanceof StreamPayerException);
      assertThat(((StreamPayerException) throwable.getCause()).getSendState()).isEqualTo(SendState.QueryFailed);
      return null;
    }).get();
  }

  @Test
  public void failsIfSpspResponseIsInvalid() throws ExecutionException, InterruptedException {
    final AccountDetails sourceAccountDetails = getSourceAccountDetails();
    final LoopbackLink simulatedLink = this.getLinkForTesting();

    StreamPayer streamPayer = new StreamPayer.Default(
      simulatedLink,
      newExternalExchangeRateProvider(Ratio.builder()
        .numerator(BigInteger.ONE)
        .denominator(BigInteger.valueOf(Integer.MAX_VALUE))
        .build()
      ),
      spspMockClientWithErrors()
    );

    final BigDecimal amountToSendInXrp = new BigDecimal("1000");
    final PaymentOptions paymentOptions = PaymentOptions.builder()
      .senderAccountDetails(sourceAccountDetails)
      .amountToSend(amountToSendInXrp)
      .destinationPaymentPointer(PaymentPointer.of("$example.com/foo"))
      .build();
    streamPayer.getQuote(paymentOptions).exceptionally(throwable -> {
      assertThat(throwable.getCause().getMessage()).isEqualTo("Unable to obtain STREAM connection details via SPSP.");
      assertThat(throwable.getCause() instanceof StreamPayerException);
      assertThat(((StreamPayerException) throwable.getCause()).getSendState()).isEqualTo(SendState.QueryFailed);
      return null;
    }).get();
  }

  @Test
  public void failsIfSlippageIsInvalid() {
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Slippage must be a percentage between 0% and 100% (inclusive)");

    final AccountDetails sourceAccountDetails = getSourceAccountDetails();
    final LoopbackLink simulatedLink = this.getLinkForTesting();

    StreamPayer streamPayer = new StreamPayer.Default(simulatedLink, newExternalExchangeRateProvider(Ratio.ONE),
      new SimpleSpspClient());

    final BigDecimal amountToSendInXrp = new BigDecimal("1000");
    PaymentOptions.builder()
      .senderAccountDetails(sourceAccountDetails)
      .amountToSend(amountToSendInXrp)
      .destinationPaymentPointer(PaymentPointer.of("$example.com/foo"))
      .slippage(Slippage.of(Percentage.of(BigDecimal.valueOf(100L))))
      .build();
  }

  @Test
  public void failsIfLinkCannotConnect() throws ExecutionException, InterruptedException {
    final AccountDetails sourceAccountDetails = getSourceAccountDetails();
    final LoopbackLink simulatedLink = this.getLinkForTesting(Optional.of(InterledgerErrorCode.T02_PEER_BUSY));

    StreamPayer streamPayer = new StreamPayer.Default(
      simulatedLink, newExternalExchangeRateProvider(Ratio.ONE),
      this.spspMockClient(InterledgerAddress.of("example.receiver"))
    );

    final BigDecimal amountToSendInXrp = new BigDecimal("1000");
    final PaymentOptions paymentOptions = PaymentOptions.builder()
      .senderAccountDetails(sourceAccountDetails)
      .amountToSend(amountToSendInXrp)
      .destinationPaymentPointer(PaymentPointer.of("$example.com/foo"))
      .build();
    streamPayer.getQuote(paymentOptions)
      .exceptionally(throwable -> {
        assertThat(throwable.getCause().getMessage()).isEqualTo("No lowerBoundRate was detected from the receiver.");
        assertThat(throwable.getCause() instanceof StreamPayerException);
        assertThat(((StreamPayerException) throwable.getCause()).getSendState()).isEqualTo(SendState.RateProbeFailed);
        return null;
      })
      .get();
  }

  @Test
  public void failsOnIncompatibleAddressSchemes() throws ExecutionException, InterruptedException {
    final AccountDetails sourceAccountDetails = getSourceAccountDetails();
    final LoopbackLink simulatedLink = this.getLinkForTesting();

    StreamPayer streamPayer = new StreamPayer.Default(
      // Incompatible address scheme (`private` vs `example`)
      simulatedLink, newExternalExchangeRateProvider(Ratio.ONE),
      this.spspMockClient(InterledgerAddress.of("private.receiver"))
    );

    final BigDecimal amountToSendInXrp = new BigDecimal("1000");
    final PaymentOptions paymentOptions = PaymentOptions.builder()
      .senderAccountDetails(sourceAccountDetails)
      .amountToSend(amountToSendInXrp)
      .destinationPaymentPointer(PaymentPointer.of("$example.com/foo"))
      .build();
    streamPayer.getQuote(paymentOptions)
      .exceptionally(throwable -> {
        assertThat(throwable.getCause().getMessage())
          .contains("Quote failed: incompatible sender/receiver address schemes.");
        assertThat(throwable.getCause() instanceof StreamPayerException);
        assertThat(((StreamPayerException) throwable.getCause()).getSendState())
          .isEqualTo(SendState.IncompatibleInterledgerNetworks);
        return null;
      })
      .get();
  }

  @Test
  public void failsOnNegativeSendAmount() throws ExecutionException, InterruptedException {
    final AccountDetails sourceAccountDetails = getSourceAccountDetails();
    final LoopbackLink simulatedLink = this.getLinkForTesting();

    StreamPayer streamPayer = new StreamPayer.Default(
      simulatedLink, newExternalExchangeRateProvider(Ratio.ONE),
      this.spspMockClient(InterledgerAddress.of("example.receiver"))
    );

    final BigDecimal amountToSendInXrp = new BigDecimal("-1000");
    final PaymentOptions paymentOptions = PaymentOptions.builder()
      .senderAccountDetails(sourceAccountDetails)
      .amountToSend(amountToSendInXrp)
      .destinationPaymentPointer(PaymentPointer.of("$example.com/foo"))
      .build();
    streamPayer.getQuote(paymentOptions)
      .exceptionally(throwable -> {
        assertThat(throwable.getCause().getMessage())
          .contains("Payment source amount must be a non-negative amount.");
        assertThat(throwable.getCause() instanceof StreamPayerException);
        assertThat(((StreamPayerException) throwable.getCause()).getSendState())
          .isEqualTo(SendState.InvalidSourceAmount);
        return null;
      })
      .get();
  }

  @Test
  public void failsOnSendAmountMorePreciseThanReceiveAmount() throws ExecutionException, InterruptedException {
    final AccountDetails sourceAccountDetails = AccountDetails.builder()
      .interledgerAddress(InterledgerAddress.of("example.unit.test.sender"))
      .denomination(Denomination.builder()
        .assetCode("USD")
        .assetScale((short) 3) // Asset scale only allows 3 units of precision
        .build())
      .build();
    final LoopbackLink simulatedLink = this.getLinkForTesting();

    StreamPayer streamPayer = new StreamPayer.Default(
      simulatedLink, newExternalExchangeRateProvider(Ratio.ONE),
      this.spspMockClient(InterledgerAddress.of("example.receiver"))
    );

    final BigDecimal amountToSendInXrp = new BigDecimal("100.0001");
    final PaymentOptions paymentOptions = PaymentOptions.builder()
      .senderAccountDetails(sourceAccountDetails)
      .amountToSend(amountToSendInXrp)
      .destinationPaymentPointer(PaymentPointer.of("$example.com/foo"))
      .build();
    streamPayer.getQuote(paymentOptions)
      .exceptionally(throwable -> {
        assertThat(throwable.getCause().getMessage()).contains("Invalid source scale");
        assertThat(throwable.getCause() instanceof StreamPayerException);
        assertThat(((StreamPayerException) throwable.getCause()).getSendState())
          .isEqualTo(SendState.InvalidSourceAmount);
        return null;
      })
      .get();
  }

  @Test
  public void failsIfAmountToDeliverIsZero() throws ExecutionException, InterruptedException {
    final AccountDetails sourceAccountDetails = this.getSourceAccountDetails();
    final AccountDetails destinationAccountDetails = this.getDestinationAccountDetails();
    final LoopbackLink simulatedLink = this.getLinkForTesting();

    final Ratio exchangeRate = Ratio.builder()
      .numerator(BigInteger.ONE)
      .denominator(BigInteger.valueOf(Integer.MAX_VALUE))
      .build();
    final PaymentSharedStateTracker paymentSharedStateTrackerMock = this.newPaymentSharedStateTrackerMock(
      sourceAccountDetails, destinationAccountDetails, exchangeRate
    );

    // By setting a very small FX rate, we can simulate a destination amount of 0.
    StreamPayer streamPayer = new StreamPayer.Default(
      simulatedLink,
      newExternalExchangeRateProvider(exchangeRate),
      this.spspMockClient(InterledgerAddress.of("example.receiver"))
    ) {
      @Override
      protected ExchangeRateProber newExchangeRateProber() {
        ExchangeRateProber exchangeRateProberMock = mock(ExchangeRateProber.class);

        when(exchangeRateProberMock.probePath(any()))
          .thenReturn(
            ExchangeRateProbeOutcome.builder()
              .sourceDenomination(Denominations.USD)
              .destinationDenomination(Denominations.EUR)
              .maxPacketAmount(MaxPacketAmount.builder()
                .maxPacketState(MaxPacketState.ImpreciseMax)
                .value(UnsignedLong.MAX_VALUE)
                .build()
              )
              .lowerBoundRate(exchangeRate)
              .upperBoundRate(exchangeRate)
              .build()
          );
        when(exchangeRateProberMock.getPaymentSharedStateTracker(any()))
          .thenReturn(Optional.of(paymentSharedStateTrackerMock));
        return exchangeRateProberMock;
      }
    };

    final BigDecimal amountToSendInXrp = new BigDecimal("0.000000001");
    final PaymentOptions paymentOptions = PaymentOptions.builder()
      .senderAccountDetails(sourceAccountDetails)
      .amountToSend(amountToSendInXrp)
      .destinationPaymentPointer(PaymentPointer.of("$example.com/foo"))
      .build();
    streamPayer.getQuote(paymentOptions)
      .handle((quote, throwable) -> {
        if (throwable != null) {
          throwable.printStackTrace();
          assertThat(throwable).isNull();
        }
        assertThat(quote.estimatedPaymentOutcome().minDeliveryAmountInDestinationUnits()).isEqualTo(BigInteger.ZERO);
        return quote;
      })
      .get();
  }

  @Test
  public void failsIfMaxPacketAmountIsZero() throws ExecutionException, InterruptedException {
    final AccountDetails sourceAccountDetails = this.getSourceAccountDetails();
    final AccountDetails destinationAccountDetails = this.getDestinationAccountDetails();
    final LoopbackLink simulatedLink = this.getLinkForTesting();

    final Ratio exchangeRate = Ratio.builder()
      .numerator(BigInteger.ONE)
      .denominator(BigInteger.valueOf(Integer.MAX_VALUE))
      .build();
    final PaymentSharedStateTracker paymentSharedStateTrackerMock = this.newPaymentSharedStateTrackerMock(
      sourceAccountDetails, destinationAccountDetails, exchangeRate
    );

    // By setting a very small FX rate, we can simulate a destination amount of 0.
    StreamPayer streamPayer = new StreamPayer.Default(
      simulatedLink,
      newExternalExchangeRateProvider(exchangeRate),
      this.spspMockClient(InterledgerAddress.of("example.receiver"))
    ) {
      @Override
      protected ExchangeRateProber newExchangeRateProber() {
        ExchangeRateProber exchangeRateProberMock = mock(ExchangeRateProber.class);

        when(exchangeRateProberMock.probePath(any()))
          .thenReturn(
            ExchangeRateProbeOutcome.builder()
              .sourceDenomination(Denominations.USD)
              .destinationDenomination(Denominations.EUR)
              .maxPacketAmount(MaxPacketAmount.builder()
                .maxPacketState(MaxPacketState.ImpreciseMax)
                .value(UnsignedLong.ZERO) // <-- Key part of this test!
                .build()
              )
              .lowerBoundRate(exchangeRate)
              .upperBoundRate(exchangeRate)
              .build()
          );
        when(exchangeRateProberMock.getPaymentSharedStateTracker(any()))
          .thenReturn(Optional.of(paymentSharedStateTrackerMock));
        return exchangeRateProberMock;
      }
    };

    final BigDecimal amountToSendInXrp = new BigDecimal("0.000000001");
    final PaymentOptions paymentOptions = PaymentOptions.builder()
      .senderAccountDetails(sourceAccountDetails)
      .amountToSend(amountToSendInXrp)
      .destinationPaymentPointer(PaymentPointer.of("$example.com/foo"))
      .build();
    streamPayer.getQuote(paymentOptions)
      .handle((quote, throwable) -> {
        assertThat(throwable).isNotNull();
        assertThat(throwable.getCause().getMessage()).contains("Rate enforcement may incur rounding errors. "
          + "maxPacketAmount=0 is below proposed minimum of 1844674407370955161");
        assertThat(throwable.getCause() instanceof StreamPayerException);
        assertThat(((StreamPayerException) throwable.getCause()).getSendState())
          .isEqualTo(SendState.ExchangeRateRoundingError);
        return null;
      }).get();
  }

  @Test
  public void failsIfNoDestinationAssetDetails() throws ExecutionException, InterruptedException {
    final AccountDetails sourceAccountDetails = this.getSourceAccountDetails();
    final LoopbackLink simulatedLink = this.getLinkForTesting(InterledgerErrorCode.F99_APPLICATION_ERROR);

    final Ratio exchangeRate = Ratio.ONE;

    final ExchangeRateTracker exchangeRateTracker = mock(ExchangeRateTracker.class);
    when(exchangeRateTracker.getLowerBoundRate()).thenReturn(exchangeRate);
    when(exchangeRateTracker.getUpperBoundRate()).thenReturn(exchangeRate);
    final AmountTracker amountTracker = new AmountTracker(exchangeRateTracker);
    final PaymentSharedStateTracker paymentSharedStateTrackerMock = mock(PaymentSharedStateTracker.class);
    when(paymentSharedStateTrackerMock.getStreamConnection()).thenReturn(newStreamConnection(sourceAccountDetails));
    when(paymentSharedStateTrackerMock.getAmountTracker()).thenReturn(amountTracker);
    when(paymentSharedStateTrackerMock.getExchangeRateTracker()).thenReturn(exchangeRateTracker);
    AssetDetailsTracker assetTrackerMock = assetDetailsTrackerMock(sourceAccountDetails, null);
    when(paymentSharedStateTrackerMock.getAssetDetailsTracker()).thenReturn(assetTrackerMock);
    when(paymentSharedStateTrackerMock.getPacingTracker()).thenReturn(new PacingTracker());

    StreamPayer streamPayer = new StreamPayer.Default(
      simulatedLink,
      newExternalExchangeRateProvider(exchangeRate),
      this.spspMockClient(InterledgerAddress.of("example.receiver"))
    ) {
      @Override
      protected ExchangeRateProber newExchangeRateProber() {
        ExchangeRateProber exchangeRateProberMock = mock(ExchangeRateProber.class);

        when(exchangeRateProberMock.probePath(any()))
          .thenReturn(
            ExchangeRateProbeOutcome.builder()
              .sourceDenomination(Denominations.USD)
              .destinationDenomination(Denominations.EUR)
              .maxPacketAmount(MaxPacketAmount.builder()
                .maxPacketState(MaxPacketState.ImpreciseMax)
                .value(UnsignedLong.MAX_VALUE)
                .build()
              )
              .lowerBoundRate(exchangeRate)
              .upperBoundRate(exchangeRate)
              .build()
          );
        when(exchangeRateProberMock.getPaymentSharedStateTracker(any()))
          .thenReturn(Optional.of(paymentSharedStateTrackerMock));
        return exchangeRateProberMock;
      }
    };

    final BigDecimal amountToSendInXrp = new BigDecimal("0.000000001");
    final PaymentOptions paymentOptions = PaymentOptions.builder()
      .senderAccountDetails(sourceAccountDetails)
      .amountToSend(amountToSendInXrp)
      .destinationPaymentPointer(PaymentPointer.of("$example.com/foo"))
      .build();
    streamPayer.getQuote(paymentOptions)
      .handle((result, throwable) -> {
        assertThat(throwable).isNotNull();
        assertThat(throwable.getCause().getMessage()).contains("Receiver never shared destination asset details.");
        assertThat(throwable.getCause() instanceof StreamPayerException);
        assertThat(((StreamPayerException) throwable.getCause()).getSendState())
          .isEqualTo(SendState.UnknownDestinationAsset);
        return null;
      }).get();
  }

  @Test
  public void testFailIfNoSourceInExternalOracle() throws ExecutionException, InterruptedException {

    final Denomination sourceDenomination = Denomination.builder().assetCode("FOO").assetScale((short) 0).build();
    final Denomination receiverDenomination = Denomination.builder().assetCode("USD").assetScale((short) 0).build();

    final AccountDetails sourceAccountDetails = AccountDetails.builder()
      .interledgerAddress(InterledgerAddress.of("example.unit.test.source"))
      .denomination(sourceDenomination)
      .build();
    final AccountDetails destinationAccountDetails = AccountDetails.builder()
      .interledgerAddress(InterledgerAddress.of("example.unit.test.receiver"))
      .denomination(receiverDenomination)
      .build();
    final LoopbackLink simulatedLink = this.getLinkForTesting();

    final Ratio exchangeRate = Ratio.ONE;
    final PaymentSharedStateTracker paymentSharedStateTrackerMock = this.newPaymentSharedStateTrackerMock(
      sourceAccountDetails, destinationAccountDetails, exchangeRate
    );

    final ExchangeRateProvider exchangeRateProviderMock = newExternalExchangeRateProvider(exchangeRate);
    doThrow(new RuntimeException("no rate found")).when(exchangeRateProviderMock).getExchangeRate("FOO", "USD");

    // By setting a very small FX rate, we can simulate a destination amount of 0.
    StreamPayer streamPayer = new StreamPayer.Default(
      simulatedLink,
      exchangeRateProviderMock,
      this.spspMockClient(InterledgerAddress.of("example.receiver"))
    ) {
      @Override
      protected ExchangeRateProber newExchangeRateProber() {
        ExchangeRateProber exchangeRateProberMock = mock(ExchangeRateProber.class);

        when(exchangeRateProberMock.probePath(any()))
          .thenReturn(
            ExchangeRateProbeOutcome.builder()
              .sourceDenomination(sourceDenomination)
              .destinationDenomination(receiverDenomination)
              .maxPacketAmount(MaxPacketAmount.builder()
                .maxPacketState(MaxPacketState.ImpreciseMax)
                .value(UnsignedLong.MAX_VALUE)
                .build()
              )
              .lowerBoundRate(exchangeRate)
              .upperBoundRate(exchangeRate)
              .build()
          );
        when(exchangeRateProberMock.getPaymentSharedStateTracker(any()))
          .thenReturn(Optional.of(paymentSharedStateTrackerMock));
        return exchangeRateProberMock;
      }
    };

    final BigDecimal amountToSendInXrp = new BigDecimal("1");
    final PaymentOptions paymentOptions = PaymentOptions.builder()
      .senderAccountDetails(sourceAccountDetails)
      .amountToSend(amountToSendInXrp)
      .destinationPaymentPointer(PaymentPointer.of("$example.com/foo"))
      .build();
    streamPayer.getQuote(paymentOptions)
      .handle((quote, throwable) -> {
        assertThat(quote).isNull();
        assertThat(throwable).isNotNull();
        assertThat(throwable.getCause().getMessage()).contains("No rate found in oracleExchangeRateProvider");
        assertThat(throwable.getCause() instanceof StreamPayerException);
        assertThat(((StreamPayerException) throwable.getCause()).getSendState())
          .isEqualTo(SendState.ExternalRateUnavailable);
        return null;
      }).get();
  }

  @Test
  public void testFailIfNoDestAssetInExternalOracle() throws ExecutionException, InterruptedException {
    final Denomination sourceDenomination = Denomination.builder().assetCode("USD").assetScale((short) 0).build();
    final Denomination receiverDenomination = Denomination.builder().assetCode("FOO").assetScale((short) 0).build();

    final AccountDetails sourceAccountDetails = AccountDetails.builder()
      .interledgerAddress(InterledgerAddress.of("example.unit.test.source"))
      .denomination(sourceDenomination)
      .build();
    final AccountDetails destinationAccountDetails = AccountDetails.builder()
      .interledgerAddress(InterledgerAddress.of("example.unit.test.receiver"))
      .denomination(receiverDenomination)
      .build();
    final LoopbackLink simulatedLink = this.getLinkForTesting();

    final Ratio exchangeRate = Ratio.ONE;
    final PaymentSharedStateTracker paymentSharedStateTrackerMock = this.newPaymentSharedStateTrackerMock(
      sourceAccountDetails, destinationAccountDetails, exchangeRate
    );

    final ExchangeRateProvider exchangeRateProviderMock = newExternalExchangeRateProvider(exchangeRate);
    doThrow(new RuntimeException("no rate found")).when(exchangeRateProviderMock).getExchangeRate("USD", "FOO");

    // By setting a very small FX rate, we can simulate a destination amount of 0.
    StreamPayer streamPayer = new StreamPayer.Default(
      simulatedLink,
      exchangeRateProviderMock,
      this.spspMockClient(InterledgerAddress.of("example.receiver"))
    ) {
      @Override
      protected ExchangeRateProber newExchangeRateProber() {
        ExchangeRateProber exchangeRateProberMock = mock(ExchangeRateProber.class);

        when(exchangeRateProberMock.probePath(any()))
          .thenReturn(
            ExchangeRateProbeOutcome.builder()
              .sourceDenomination(sourceDenomination)
              .destinationDenomination(receiverDenomination)
              .maxPacketAmount(MaxPacketAmount.builder()
                .maxPacketState(MaxPacketState.ImpreciseMax)
                .value(UnsignedLong.MAX_VALUE)
                .build()
              )
              .lowerBoundRate(exchangeRate)
              .upperBoundRate(exchangeRate)
              .build()
          );
        when(exchangeRateProberMock.getPaymentSharedStateTracker(any()))
          .thenReturn(Optional.of(paymentSharedStateTrackerMock));
        return exchangeRateProberMock;
      }
    };

    final BigDecimal amountToSendInXrp = new BigDecimal("1");
    final PaymentOptions paymentOptions = PaymentOptions.builder()
      .senderAccountDetails(sourceAccountDetails)
      .amountToSend(amountToSendInXrp)
      .destinationPaymentPointer(PaymentPointer.of("$example.com/foo"))
      .build();
    streamPayer.getQuote(paymentOptions)
      .handle((quote, throwable) -> {
        assertThat(quote).isNull();
        assertThat(throwable).isNotNull();
        assertThat(throwable.getCause().getMessage()).contains("No rate found in oracleExchangeRateProvider");
        assertThat(throwable.getCause() instanceof StreamPayerException);
        assertThat(((StreamPayerException) throwable.getCause()).getSendState())
          .isEqualTo(SendState.ExternalRateUnavailable);
        return null;
      }).get();
  }

  @Test
  public void testFailIfExchangeRateIs0() throws ExecutionException, InterruptedException {
    final AccountDetails sourceAccountDetails = this.getSourceAccountDetails();
    final AccountDetails destinationAccountDetails = this.getDestinationAccountDetails();
    final LoopbackLink simulatedLink = this.getLinkForTesting();

    final Ratio exchangeRate = Ratio.ZERO;
    final PaymentSharedStateTracker paymentSharedStateTrackerMock = this.newPaymentSharedStateTrackerMock(
      sourceAccountDetails, destinationAccountDetails, exchangeRate
    );

    final ExchangeRateProvider externalExchangeRateProviderMock = this.newExternalExchangeRateProvider(exchangeRate);

    // By setting a very small FX rate, we can simulate a destination amount of 0.
    StreamPayer streamPayer = new StreamPayer.Default(
      simulatedLink,
      externalExchangeRateProviderMock,
      this.spspMockClient(InterledgerAddress.of("example.receiver"))
    ) {
      @Override
      protected ExchangeRateProber newExchangeRateProber() {
        ExchangeRateProber exchangeRateProberMock = mock(ExchangeRateProber.class);

        when(exchangeRateProberMock.probePath(any()))
          .thenReturn(
            ExchangeRateProbeOutcome.builder()
              .sourceDenomination(sourceAccountDetails.denomination())
              .destinationDenomination(destinationAccountDetails.denomination())
              .maxPacketAmount(MaxPacketAmount.builder()
                .maxPacketState(MaxPacketState.ImpreciseMax)
                .value(UnsignedLong.MAX_VALUE)
                .build()
              )
              .lowerBoundRate(Ratio.ZERO)
              .upperBoundRate(Ratio.ZERO)
              .build()
          );
        when(exchangeRateProberMock.getPaymentSharedStateTracker(any()))
          .thenReturn(Optional.of(paymentSharedStateTrackerMock));
        return exchangeRateProberMock;
      }
    };

    final BigDecimal amountToSendInXrp = new BigDecimal("1");
    final PaymentOptions paymentOptions = PaymentOptions.builder()
      .senderAccountDetails(sourceAccountDetails)
      .amountToSend(amountToSendInXrp)
      .destinationPaymentPointer(PaymentPointer.of("$example.com/foo"))
      .build();
    streamPayer.getQuote(paymentOptions)
      .handle((quote, throwable) -> {
        assertThat(quote).isNull();
        assertThat(throwable).isNotNull();
        assertThat(throwable.getCause().getMessage()).contains("External exchange rate was 0.");
        assertThat(throwable.getCause() instanceof StreamPayerException);
        assertThat(((StreamPayerException) throwable.getCause()).getSendState())
          .isEqualTo(SendState.ExternalRateUnavailable);
        return null;
      }).get();
  }

  @Test
  public void testFailIfProbeRateIsBelowOracleRateIs0() throws ExecutionException, InterruptedException {
    final AccountDetails sourceAccountDetails = this.getSourceAccountDetails();
    final AccountDetails destinationAccountDetails = this.getDestinationAccountDetails();
    final LoopbackLink simulatedLink = this.getLinkForTesting();

    final Ratio externalExchangeRate = Ratio.from(new BigDecimal("1.0"));

    final Ratio trackedLowerBoundRate = Ratio.from(new BigDecimal("0.98"));
    final Ratio trackedUpperBoundRate = Ratio.from(new BigDecimal("0.98"));

    final ExchangeRateTracker exchangeRateTracker = mock(ExchangeRateTracker.class);
    when(exchangeRateTracker.getLowerBoundRate()).thenReturn(trackedLowerBoundRate);
    when(exchangeRateTracker.getUpperBoundRate()).thenReturn(trackedUpperBoundRate);
    final AmountTracker amountTracker = new AmountTracker(exchangeRateTracker);
    final PaymentSharedStateTracker paymentSharedStateTrackerMock = mock(PaymentSharedStateTracker.class);
    when(paymentSharedStateTrackerMock.getStreamConnection()).thenReturn(newStreamConnection(sourceAccountDetails));
    when(paymentSharedStateTrackerMock.getAmountTracker()).thenReturn(amountTracker);
    when(paymentSharedStateTrackerMock.getExchangeRateTracker()).thenReturn(exchangeRateTracker);
    AssetDetailsTracker assetTrackerMock = assetDetailsTrackerMock(sourceAccountDetails, destinationAccountDetails);
    when(paymentSharedStateTrackerMock.getAssetDetailsTracker()).thenReturn(assetTrackerMock);
    when(paymentSharedStateTrackerMock.getPacingTracker()).thenReturn(new PacingTracker());

    final ExchangeRateProvider externalExchangeRateProviderMock = this
      .newExternalExchangeRateProvider(externalExchangeRate);

    // By setting a very small FX rate, we can simulate a destination amount of 0.
    StreamPayer streamPayer = new StreamPayer.Default(
      simulatedLink,
      externalExchangeRateProviderMock,
      this.spspMockClient(InterledgerAddress.of("example.receiver"))
    ) {
      @Override
      protected ExchangeRateProber newExchangeRateProber() {
        ExchangeRateProber exchangeRateProberMock = mock(ExchangeRateProber.class);

        when(exchangeRateProberMock.probePath(any()))
          .thenReturn(
            ExchangeRateProbeOutcome.builder()
              .sourceDenomination(sourceAccountDetails.denomination())
              .destinationDenomination(destinationAccountDetails.denomination())
              .maxPacketAmount(MaxPacketAmount.builder()
                .maxPacketState(MaxPacketState.ImpreciseMax)
                .value(UnsignedLong.MAX_VALUE)
                .build()
              )
              .lowerBoundRate(trackedLowerBoundRate)
              .upperBoundRate(trackedUpperBoundRate)
              .build()
          );
        when(exchangeRateProberMock.getPaymentSharedStateTracker(any()))
          .thenReturn(Optional.of(paymentSharedStateTrackerMock));
        return exchangeRateProberMock;
      }
    };

    final BigDecimal amountToSendInXrp = new BigDecimal("1");
    final PaymentOptions paymentOptions = PaymentOptions.builder()
      .senderAccountDetails(sourceAccountDetails)
      .amountToSend(amountToSendInXrp)
      .destinationPaymentPointer(PaymentPointer.of("$example.com/foo"))
      .slippage(Slippage.ONE_PERCENT)
      .build();
    streamPayer.getQuote(paymentOptions)
      .handle((quote, throwable) -> {
        assertThat(quote).isNull();
        assertThat(throwable).isNotNull();
        assertThat(throwable.getCause().getMessage()).contains(
          "Rate Probed exchange rate of Ratio{numerator=98, denominator=100, isPositive=true, toBigDecimal=0.98} is "
            + "not greater than minimum of 0.9900"
        );
        assertThat(throwable.getCause() instanceof StreamPayerException);
        assertThat(((StreamPayerException) throwable.getCause()).getSendState())
          .isEqualTo(SendState.InsufficientExchangeRate);
        return null;
      }).get();
  }

  @Test
  public void testFailIfProbedRateIs0() throws ExecutionException, InterruptedException {
    final AccountDetails sourceAccountDetails = this.getSourceAccountDetails();
    final AccountDetails destinationAccountDetails = this.getDestinationAccountDetails();
    final LoopbackLink simulatedLink = this.getLinkForTesting();

    final Ratio externalExchangeRate = Ratio.from(new BigDecimal("1.0"));

    final Ratio trackedLowerBoundRate = Ratio.from(new BigDecimal("0"));
    final Ratio trackedUpperBoundRate = Ratio.from(new BigDecimal("0"));

    final ExchangeRateTracker exchangeRateTracker = mock(ExchangeRateTracker.class);
    when(exchangeRateTracker.getLowerBoundRate()).thenReturn(trackedLowerBoundRate);
    when(exchangeRateTracker.getUpperBoundRate()).thenReturn(trackedUpperBoundRate);
    final AmountTracker amountTracker = new AmountTracker(exchangeRateTracker);
    final PaymentSharedStateTracker paymentSharedStateTrackerMock = mock(PaymentSharedStateTracker.class);
    when(paymentSharedStateTrackerMock.getStreamConnection()).thenReturn(newStreamConnection(sourceAccountDetails));
    when(paymentSharedStateTrackerMock.getAmountTracker()).thenReturn(amountTracker);
    when(paymentSharedStateTrackerMock.getExchangeRateTracker()).thenReturn(exchangeRateTracker);
    AssetDetailsTracker assetTrackerMock = assetDetailsTrackerMock(sourceAccountDetails, destinationAccountDetails);
    when(paymentSharedStateTrackerMock.getAssetDetailsTracker()).thenReturn(assetTrackerMock);
    when(paymentSharedStateTrackerMock.getPacingTracker()).thenReturn(new PacingTracker());

    final ExchangeRateProvider externalExchangeRateProviderMock = this
      .newExternalExchangeRateProvider(externalExchangeRate);

    // By setting a very small FX rate, we can simulate a destination amount of 0.
    StreamPayer streamPayer = new StreamPayer.Default(
      simulatedLink,
      externalExchangeRateProviderMock,
      this.spspMockClient(InterledgerAddress.of("example.receiver"))
    ) {
      @Override
      protected ExchangeRateProber newExchangeRateProber() {
        ExchangeRateProber exchangeRateProberMock = mock(ExchangeRateProber.class);

        when(exchangeRateProberMock.probePath(any()))
          .thenReturn(
            ExchangeRateProbeOutcome.builder()
              .sourceDenomination(sourceAccountDetails.denomination())
              .destinationDenomination(destinationAccountDetails.denomination())
              .maxPacketAmount(MaxPacketAmount.builder()
                .maxPacketState(MaxPacketState.ImpreciseMax)
                .value(UnsignedLong.MAX_VALUE)
                .build()
              )
              .lowerBoundRate(trackedLowerBoundRate)
              .upperBoundRate(trackedUpperBoundRate)
              .build()
          );
        when(exchangeRateProberMock.getPaymentSharedStateTracker(any()))
          .thenReturn(Optional.of(paymentSharedStateTrackerMock));
        return exchangeRateProberMock;
      }
    };

    final BigDecimal amountToSendInXrp = new BigDecimal("1");
    final PaymentOptions paymentOptions = PaymentOptions.builder()
      .senderAccountDetails(sourceAccountDetails)
      .amountToSend(amountToSendInXrp)
      .destinationPaymentPointer(PaymentPointer.of("$example.com/foo"))
      .slippage(Slippage.ONE_PERCENT)
      .build();
    streamPayer.getQuote(paymentOptions)
      .handle((quote, throwable) -> {
        assertThat(quote).isNull();
        assertThat(throwable).isNotNull();
        assertThat(throwable.getCause().getMessage()).contains(
          "Rate Probe discovered invalid exchange rates. lowerBoundRate=Ratio{numerator=0, denominator=10, "
            + "isPositive=false, toBigDecimal=0} upperBoundRate=Ratio{numerator=0, denominator=10, isPositive=false, "
            + "toBigDecimal=0}"
        );
        assertThat(throwable.getCause() instanceof StreamPayerException);
        assertThat(((StreamPayerException) throwable.getCause()).getSendState())
          .isEqualTo(SendState.InsufficientExchangeRate);
        return null;
      }).get();
  }

  /**
   * Quote fails if min-rate and max-packet amount would cause rounding errors.
   */
  @Test
  public void testFailIfRoundingErrors() throws ExecutionException, InterruptedException {
    final AccountDetails sourceAccountDetails = AccountDetails.builder()
      .interledgerAddress(InterledgerAddress.of("example.larry.sender"))
      .denomination(Denomination.builder().assetCode("BTC").assetScale((short) 8).build())
      .build();
    final AccountDetails destinationAccountDetails = AccountDetails.builder()
      .interledgerAddress(InterledgerAddress.of("example.larry.receiver"))
      .denomination(Denomination.builder().assetCode("EUR").assetScale((short) 6).build())
      .build();
    final LoopbackLink simulatedLink = this.getLinkForTesting();

    final Ratio externalExchangeRate = Ratio.from(
      new BigDecimal("9814.04").divide(new BigDecimal("1.13"), MathContext.DECIMAL64)
    );

    final Ratio trackedLowerBoundRate = Ratio.builder()
      .numerator(BigInteger.valueOf(86806L))
      .denominator(BigInteger.valueOf(1000L))
      .build();
    final Ratio trackedUpperBoundRate = Ratio.builder()
      .numerator(BigInteger.valueOf(86807L))
      .denominator(BigInteger.valueOf(1000L))
      .build();

    final ExchangeRateTracker exchangeRateTracker = mock(ExchangeRateTracker.class);
    when(exchangeRateTracker.getLowerBoundRate()).thenReturn(trackedLowerBoundRate);
    when(exchangeRateTracker.getUpperBoundRate()).thenReturn(trackedUpperBoundRate);
    final AmountTracker amountTracker = new AmountTracker(exchangeRateTracker);
    final PaymentSharedStateTracker paymentSharedStateTrackerMock = mock(PaymentSharedStateTracker.class);
    when(paymentSharedStateTrackerMock.getStreamConnection()).thenReturn(newStreamConnection(sourceAccountDetails));
    when(paymentSharedStateTrackerMock.getAmountTracker()).thenReturn(amountTracker);
    when(paymentSharedStateTrackerMock.getExchangeRateTracker()).thenReturn(exchangeRateTracker);
    AssetDetailsTracker assetTrackerMock = assetDetailsTrackerMock(sourceAccountDetails, destinationAccountDetails);
    when(paymentSharedStateTrackerMock.getAssetDetailsTracker()).thenReturn(assetTrackerMock);
    when(paymentSharedStateTrackerMock.getPacingTracker()).thenReturn(new PacingTracker());

    final ExchangeRateProvider externalExchangeRateProviderMock = this
      .newExternalExchangeRateProvider(externalExchangeRate);

    // By setting a very small FX rate, we can simulate a destination amount of 0.
    StreamPayer streamPayer = new StreamPayer.Default(
      simulatedLink,
      externalExchangeRateProviderMock,
      this.spspMockClient(InterledgerAddress.of("example.receiver"))
    ) {
      @Override
      protected ExchangeRateProber newExchangeRateProber() {
        ExchangeRateProber exchangeRateProberMock = mock(ExchangeRateProber.class);

        when(exchangeRateProberMock.probePath(any()))
          .thenReturn(
            ExchangeRateProbeOutcome.builder()
              .sourceDenomination(sourceAccountDetails.denomination())
              .destinationDenomination(destinationAccountDetails.denomination())
              .maxPacketAmount(MaxPacketAmount.builder()
                .maxPacketState(MaxPacketState.PreciseMax)
                .value(UnsignedLong.valueOf(1000L)) // <-- This is required to make this test fail properly.
                .build()
              )
              .lowerBoundRate(trackedLowerBoundRate)
              .upperBoundRate(trackedUpperBoundRate)
              .build()
          );
        when(exchangeRateProberMock.getPaymentSharedStateTracker(any()))
          .thenReturn(Optional.of(paymentSharedStateTrackerMock));
        return exchangeRateProberMock;
      }
    };

    final BigDecimal amountToSendInXrp = new BigDecimal(100_000L);
    final PaymentOptions paymentOptions = PaymentOptions.builder()
      .senderAccountDetails(sourceAccountDetails)
      .amountToSend(amountToSendInXrp)
      .destinationPaymentPointer(PaymentPointer.of("$example.com/foo"))
      // Slippage/minExchangeRate is far too close to the real spread/rate
      // to perform the payment without rounding errors, since the max packet
      // amount of 1000 doesn't allow more precision.
      .slippage(Slippage.of(Percentage.of(new BigDecimal("0.00051"))))
      .build();

    streamPayer.getQuote(paymentOptions)
      .handle((quote, throwable) -> {
        assertThat(quote).isNull();
        assertThat(throwable).isNotNull();
        assertThat(throwable.getCause().getMessage()).contains("Rate enforcement may incur rounding errors.");
        assertThat(throwable.getCause() instanceof StreamPayerException);
        assertThat(((StreamPayerException) throwable.getCause()).getSendState())
          .isEqualTo(SendState.ExchangeRateRoundingError);
        return null;
      }).get();
  }

  //////////////////////////////////
  // shiftForNormalization()
  //////////////////////////////////

  @Test
  public void testShiftForNormalization() {
    final LoopbackLink simulatedLink = this.getLinkForTesting();

    StreamPayer.Default streamPayer = new StreamPayer.Default(
      simulatedLink, newExternalExchangeRateProvider(Ratio.ONE),
      this.spspMockClient(InterledgerAddress.of("example.receiver"))
    );

    // 1/1 == 1 (USD/USD)
    BigDecimal rate = Ratio.from(BigInteger.valueOf(1), BigInteger.valueOf(1L)).toBigDecimal();
    assertThat(streamPayer.shiftRateForNormalization(rate, (short) 0, (short) 0)).isEqualTo(new BigDecimal("1"));

    rate = Ratio.from(BigInteger.ONE, BigInteger.ONE).toBigDecimal(); // rate=1.0
    assertThat(streamPayer.shiftRateForNormalization(rate, (short) 0, (short) 2)).isEqualTo(new BigDecimal("0.01"));

    rate = Ratio.from(BigInteger.ONE, BigInteger.valueOf(100L)).toBigDecimal(); // rate=0.01
    assertThat(streamPayer.shiftRateForNormalization(rate, (short) 0, (short) 2)).isEqualTo(new BigDecimal("0.0001"));

    // 1/4 == 0.25 (XRP/USD)
    rate = Ratio.from(BigInteger.valueOf(1), BigInteger.valueOf(4L)).toBigDecimal();
    assertThat(streamPayer.shiftRateForNormalization(rate, (short) 0, (short) 0)).isEqualTo(new BigDecimal("0.25"));

    // 1B/4B == 0.25 (XRP/USD)
    rate = Ratio.from(BigInteger.valueOf(1_000_000_000), BigInteger.valueOf(4_000_000_000L)).toBigDecimal();
    assertThat(streamPayer.shiftRateForNormalization(rate, (short) 0, (short) 0)).isEqualTo(new BigDecimal("0.25"));

    // 1(9)/4000(6) == .25 (XRP/USD)
    rate = Ratio.from(BigInteger.valueOf(1), BigInteger.valueOf(4_000L)).toBigDecimal();
    assertThat(streamPayer.shiftRateForNormalization(rate, (short) 9, (short) 6)).isEqualTo(new BigDecimal("0.25"));

    // 1 XRP(1B MilliDrops, scale=9)) : 4M (milli-dollars, scale=6)) ==> 250,000
    rate = Ratio.from(BigInteger.valueOf(1_000_000_000), BigInteger.valueOf(4_000_000)).toBigDecimal();
    assertThat(streamPayer.shiftRateForNormalization(rate, (short) 9, (short) 6)).isEqualTo(new BigDecimal("250000"));

    assertThat(streamPayer.shiftRateForNormalization(BigDecimal.valueOf(1L), (short) 6, (short) 0))
      .isEqualTo(BigDecimal.valueOf(1_000_000));
    assertThat(streamPayer.shiftRateForNormalization(BigDecimal.valueOf(250), (short) 9, (short) 6))
      .isEqualTo(BigDecimal.valueOf(250_000L));
    assertThat(streamPayer.shiftRateForNormalization(BigDecimal.valueOf(250_000_000), (short) 9, (short) 6))
      .isEqualTo(BigDecimal.valueOf(250_000_000_000L));
  }

  //////////////////////////////////
  // determineScaledExternalRate()
  //////////////////////////////////

  @Test
  public void testDetermineScaledExternalRateWithNullSourceAccount() {
    final StreamPayer.Default streamPayer = this.getStreamPayerDefault();
    expectedException.expect(NullPointerException.class);
    streamPayer.determineScaledExternalRate(
      null, mock(AccountDetails.class), mock(ExchangeRate.class), Slippage.NONE
    );
  }

  @Test
  public void testDetermineScaledExternalRateWithNullDestAccount() {
    final StreamPayer.Default streamPayer = this.getStreamPayerDefault();
    expectedException.expect(NullPointerException.class);
    streamPayer.determineScaledExternalRate(
      mock(AccountDetails.class), null, mock(ExchangeRate.class), Slippage.NONE
    );
  }

  @Test
  public void testDetermineScaledExternalRateWithNullExchangeRate() {
    final StreamPayer.Default streamPayer = this.getStreamPayerDefault();
    expectedException.expect(NullPointerException.class);
    streamPayer.determineScaledExternalRate(
      mock(AccountDetails.class), mock(AccountDetails.class), null, Slippage.NONE
    );
  }

  @Test
  public void testDetermineScaledExternalRateWithNullSlippage() {
    final StreamPayer.Default streamPayer = this.getStreamPayerDefault();
    expectedException.expect(NullPointerException.class);
    streamPayer.determineScaledExternalRate(
      mock(AccountDetails.class), mock(AccountDetails.class), mock(ExchangeRate.class), null
    );
  }

  /**
   * This test simulates a source price of 1 (in EUR, scale=3) and a destination price of 1.12 (in USD, scale=4).
   */
  @Test
  public void testDetermineScaledExternalRateFromJS() {
    final AccountDetails sourceAccountDetailsMock = mock(AccountDetails.class);
    when(sourceAccountDetailsMock.denomination())
      .thenReturn(Optional.of(Denomination.builder().assetCode("EUR").assetScale((short) 3).build()));

    final AccountDetails destinationAccountDetailsMock = mock(AccountDetails.class);
    when(destinationAccountDetailsMock.denomination())
      .thenReturn(Optional.of(Denomination.builder().assetCode("USD").assetScale((short) 4).build()));
    final LoopbackLink simulatedLink = this.getLinkForTesting();

    StreamPayer.Default streamPayer = new Default(
      simulatedLink, newExternalExchangeRateProvider(Ratio.ONE), new SimpleSpspClient()
    );

    // Should be close to 0.8928571428571428
    final BigDecimal simulatedRate = BigDecimal.ONE.divide(new BigDecimal("1.12"), MathContext.DECIMAL64);
    final ExchangeRate externalExchangeRateMock = mock(ExchangeRate.class);
    when(externalExchangeRateMock.getFactor()).thenReturn(new DefaultNumberValue(simulatedRate));
    final Slippage slippageMock = Slippage.ONE_PERCENT;

    ScaledExchangeRate actual = streamPayer.determineScaledExternalRate(
      sourceAccountDetailsMock, destinationAccountDetailsMock, externalExchangeRateMock, slippageMock
    );

    assertThat(actual.inputScale()).isEqualTo((short) 4);
    assertThat(actual.slippage()).isEqualTo(Slippage.ONE_PERCENT);
    assertThat(actual.value()).isEqualTo(new BigDecimal("8.928571428571429"));
    assertThat(actual.lowerBound()).isEqualTo(new BigDecimal("8.8392857142857147100"));
    assertThat(actual.upperBound()).isEqualTo(new BigDecimal("9.0178571428571432900"));
    assertThat(actual.reciprocal()).isEqualTo(new BigDecimal("0.1120000000000000"));
  }

  //////////////////
  // Private Helpers
  //////////////////

  private ExchangeRateProvider newExternalExchangeRateProvider(final Ratio exchangeRate) {
    Objects.requireNonNull(exchangeRate);

    ExchangeRate exchangeRateMock = new ExchangeRateBuilder("usdProvider", RateType.DEFERRED)
      .setBase(CurrencyUnitBuilder.of("USD", "usdProvider").build())
      .setFactor(new DefaultNumberValue(exchangeRate.toBigDecimal()))
      .setTerm(CurrencyUnitBuilder.of("USD", "usdProvider").build())
      .build();

    ExchangeRateProvider exchangeRateProviderMock = mock(ExchangeRateProvider.class);
    when(exchangeRateProviderMock.getExchangeRate(anyString(), anyString())).thenReturn(exchangeRateMock);
    return exchangeRateProviderMock;
  }

  private AccountDetails getSourceAccountDetails() {
    return AccountDetails.builder()
      .interledgerAddress(InterledgerAddress.of("example.unit.test.sender"))
      .denomination(Denominations.XRP_MILLI_DROPS)
      .build();
  }

  private AccountDetails getDestinationAccountDetails() {
    return AccountDetails.builder()
      .interledgerAddress(InterledgerAddress.of("example.unit.test.destination"))
      .denomination(Denominations.XRP_MILLI_DROPS)
      .build();
  }

  private LoopbackLink getLinkForTesting() {
    return getLinkForTesting(Optional.empty());
  }

  private LoopbackLink getLinkForTesting(final InterledgerErrorCode simulatedRejectCode) {
    return getLinkForTesting(Optional.of(simulatedRejectCode));
  }

  private LoopbackLink getLinkForTesting(final Optional<InterledgerErrorCode> simulatedRejectCode) {
    LoopbackLink link = new LoopbackLink(
      () -> LINK_OPERATOR_ADDRESS,
      new LinkSettings() {
        @Override
        public LinkType getLinkType() {
          return LoopbackLink.LINK_TYPE;
        }

        @Override
        public Map<String, Object> getCustomSettings() {
          return simulatedRejectCode
            .map(simulatedRejectCode -> Maps.<String, Object>newHashMap(
              LoopbackLink.SIMULATED_REJECT_ERROR_CODE, InterledgerErrorCode.T02_PEER_BUSY.getCode())
            )
            .orElseGet(HashMap::new);
        }
      },
      new PacketRejector(() -> LINK_OPERATOR_ADDRESS)
    );

    link.setLinkId(LinkId.of("unit-test-loopback-link"));
    return link;
  }

  private SpspClient spspMockClientWithErrors() {
    return new SpspClient() {
      @Override
      public StreamConnectionDetails getStreamConnectionDetails(PaymentPointer paymentPointer)
        throws InvalidReceiverClientException {
        throw new RuntimeException("Oops");
      }

      @Override
      public StreamConnectionDetails getStreamConnectionDetails(HttpUrl spspUrl) throws InvalidReceiverClientException {
        throw new RuntimeException("Oops");
      }
    };
  }

  private SpspClient spspMockClient(final InterledgerAddress receiverAddress) {
    return new SpspClient() {
      @Override
      public StreamConnectionDetails getStreamConnectionDetails(PaymentPointer paymentPointer)
        throws InvalidReceiverClientException {
        return StreamConnectionDetails.builder()
          .destinationAddress(receiverAddress)
          .sharedSecret(SharedSecret.of(new byte[32]))
          .build();
      }

      @Override
      public StreamConnectionDetails getStreamConnectionDetails(HttpUrl spspUrl) throws InvalidReceiverClientException {
        return StreamConnectionDetails.builder()
          .destinationAddress(receiverAddress)
          .sharedSecret(SharedSecret.of(new byte[32]))
          .build();
      }
    };
  }

  private StreamPayer.Default getStreamPayerDefault() {
    final LoopbackLink simulatedLink = this.getLinkForTesting();
    StreamPayer.Default streamPayer = new StreamPayer.Default(
      simulatedLink, newExternalExchangeRateProvider(Ratio.ONE),
      this.spspMockClient(InterledgerAddress.of("example.receiver"))
    );
    return streamPayer;
  }

  private AssetDetailsTracker assetDetailsTrackerMock(
    final AccountDetails sourceAccountDetails, final AccountDetails destinationAccountDetails
  ) {
    AssetDetailsTracker assetDetailsTrackerMock = mock(AssetDetailsTracker.class);
    when(assetDetailsTrackerMock.getSourceAccountDetails()).thenReturn(sourceAccountDetails);
    when(assetDetailsTrackerMock.getDestinationAccountDetails()).thenReturn(destinationAccountDetails);
    return assetDetailsTrackerMock;
  }

  private StreamConnection newStreamConnection(final AccountDetails sourceAccountDetails) {
    Objects.requireNonNull(sourceAccountDetails);
    return new StreamConnection(
      sourceAccountDetails,
      sourceAccountDetails.interledgerAddress(),
      SharedSecret.of(new byte[32])
    );
  }

  private PaymentSharedStateTracker newPaymentSharedStateTrackerMock(
    final AccountDetails sourceAccountDetails,
    final AccountDetails receiverAccountDetails,
    final Ratio exchangeRate
  ) {
    Objects.requireNonNull(sourceAccountDetails);
    Objects.requireNonNull(receiverAccountDetails);
    Objects.requireNonNull(exchangeRate);

    final ExchangeRateTracker exchangeRateTracker = mock(ExchangeRateTracker.class);
    when(exchangeRateTracker.getLowerBoundRate()).thenReturn(exchangeRate);
    when(exchangeRateTracker.getUpperBoundRate()).thenReturn(exchangeRate);
    final AmountTracker amountTracker = new AmountTracker(exchangeRateTracker);
    final PaymentSharedStateTracker paymentSharedStateTrackerMock = mock(PaymentSharedStateTracker.class);
    when(paymentSharedStateTrackerMock.getStreamConnection()).thenReturn(newStreamConnection(sourceAccountDetails));
    when(paymentSharedStateTrackerMock.getAmountTracker()).thenReturn(amountTracker);
    when(paymentSharedStateTrackerMock.getExchangeRateTracker()).thenReturn(exchangeRateTracker);
    AssetDetailsTracker assetTrackerMock = assetDetailsTrackerMock(sourceAccountDetails, receiverAccountDetails);
    when(paymentSharedStateTrackerMock.getAssetDetailsTracker()).thenReturn(assetTrackerMock);
    when(paymentSharedStateTrackerMock.getPacingTracker()).thenReturn(new PacingTracker());

    return paymentSharedStateTrackerMock;
  }

  // TODO: Implement these tests from JS
  //  it('fails on asset detail conflicts')

}