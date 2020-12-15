package org.interledger.stream.pay;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.interledger.codecs.stream.StreamCodecContextFactory;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerErrorCode;
import org.interledger.core.InterledgerFulfillment;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.fluent.Percentage;
import org.interledger.core.fluent.Ratio;
import org.interledger.fx.Denomination;
import org.interledger.fx.Denominations;
import org.interledger.fx.ScaledExchangeRate;
import org.interledger.fx.Slippage;
import org.interledger.link.LinkId;
import org.interledger.link.LinkSettings;
import org.interledger.link.LinkType;
import org.interledger.link.LoopbackLink;
import org.interledger.link.PacketRejector;
import org.interledger.link.spsp.StatelessSpspReceiverLinkSettings;
import org.interledger.link.spsp.StatelessStreamReceiverLink;
import org.interledger.spsp.PaymentPointer;
import org.interledger.spsp.StreamConnectionDetails;
import org.interledger.spsp.client.InvalidReceiverClientException;
import org.interledger.spsp.client.SimpleSpspClient;
import org.interledger.spsp.client.SpspClient;
import org.interledger.stream.StreamPacket;
import org.interledger.stream.crypto.AesGcmStreamEncryptionService;
import org.interledger.stream.crypto.SharedSecret;
import org.interledger.stream.crypto.StreamEncryptionUtils;
import org.interledger.stream.frames.ConnectionAssetDetailsFrame;
import org.interledger.stream.frames.StreamFrame;
import org.interledger.stream.frames.StreamFrameType;
import org.interledger.stream.model.AccountDetails;
import org.interledger.stream.pay.StreamPayer.Default;
import org.interledger.stream.pay.exceptions.StreamPayerException;
import org.interledger.stream.pay.model.PaymentOptions;
import org.interledger.stream.pay.model.Quote;
import org.interledger.stream.pay.model.Receipt;
import org.interledger.stream.pay.model.SendState;
import org.interledger.stream.pay.probing.ExchangeRateProber;
import org.interledger.stream.pay.probing.model.ExchangeRateProbeOutcome;
import org.interledger.stream.pay.trackers.AmountTracker;
import org.interledger.stream.pay.trackers.AssetDetailsTracker;
import org.interledger.stream.pay.trackers.ExchangeRateTracker;
import org.interledger.stream.pay.trackers.MaxPacketAmountTracker;
import org.interledger.stream.pay.trackers.MaxPacketAmountTracker.MaxPacketAmount;
import org.interledger.stream.pay.trackers.MaxPacketAmountTracker.MaxPacketState;
import org.interledger.stream.pay.trackers.PacingTracker;
import org.interledger.stream.pay.trackers.PaymentSharedStateTracker;
import org.interledger.stream.receiver.SpspStreamConnectionGenerator;
import org.interledger.stream.receiver.StatelessStreamReceiver;

import com.google.common.primitives.UnsignedLong;
import okhttp3.HttpUrl;
import org.assertj.core.util.Maps;
import org.javamoney.moneta.CurrencyUnitBuilder;
import org.javamoney.moneta.convert.ExchangeRateBuilder;
import org.javamoney.moneta.spi.DefaultNumberValue;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.MockitoAnnotations;
import org.testcontainers.shaded.com.google.common.collect.ImmutableList;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import javax.money.convert.ExchangeRate;
import javax.money.convert.ExchangeRateProvider;
import javax.money.convert.RateType;

/**
 * Unit tests for {@link StreamPayer.Default}.
 */
public class StreamPayerDefaultTest {

  public static final byte[] SERVER_SECRET_BYTES = new byte[32];

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private StreamEncryptionUtils streamEncryptionUtils;

  private static final InterledgerAddress LINK_OPERATOR_ADDRESS = InterledgerAddress.of("example.connector");

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);

    this.streamEncryptionUtils = new StreamEncryptionUtils(
      StreamCodecContextFactory.oer(), new AesGcmStreamEncryptionService()
    );
  }

  //////////////////////////////////
  // Quoting Flows
  //////////////////////////////////

  @Test
  public void getQuoteFailsIfPaymentPointerCannotResolve() throws ExecutionException, InterruptedException {
    final AccountDetails sourceAccountDetails = getSourceAccountDetails();
    final LoopbackLink simulatedLink = this.getLinkForTesting();

    StreamPayer streamPayer = new Default(
      streamEncryptionUtils, simulatedLink, newExternalExchangeRateProvider(Ratio.ONE), new SimpleSpspClient()
    );

    final BigDecimal amountToSendInXrp = new BigDecimal("1000");
    final PaymentOptions paymentOptions = PaymentOptions.builder()
      .senderAccountDetails(sourceAccountDetails)
      .amountToSend(amountToSendInXrp)
      .destinationPaymentPointer(PaymentPointer.of("$example.com/foo"))
      .build();
    Throwable error = streamPayer.getQuote(paymentOptions).handle((quote, throwable) -> {
      assertThat(quote).isNull();
      assertThat(throwable).isNotNull();
      return throwable;
    }).get();

    assertThat(error.getCause().getMessage()).isEqualTo("Unable to obtain STREAM connection details via SPSP.");
    assertThat(error.getCause() instanceof StreamPayerException).isTrue();
    assertThat(((StreamPayerException) error.getCause()).getSendState()).isEqualTo(SendState.QueryFailed);
  }

  @Test
  public void getQuoteFailsIfSpspResponseIsInvalid() throws ExecutionException, InterruptedException {
    final AccountDetails sourceAccountDetails = getSourceAccountDetails();
    final LoopbackLink simulatedLink = this.getLinkForTesting();

    StreamPayer streamPayer = new StreamPayer.Default(
      streamEncryptionUtils,
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
    streamPayer.getQuote(paymentOptions).handle(($, throwable) -> {
      assertThat(throwable.getCause().getMessage()).isEqualTo("Unable to obtain STREAM connection details via SPSP.");
      assertThat(throwable.getCause() instanceof StreamPayerException).isTrue();
      assertThat(((StreamPayerException) throwable.getCause()).getSendState()).isEqualTo(SendState.QueryFailed);
      return null;
    }).get();
  }

  @Test
  public void getQuoteFailsIfSlippageIsInvalid() {
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Slippage must be a percentage between 0% and 100% (inclusive)");

    final AccountDetails sourceAccountDetails = getSourceAccountDetails();
    final LoopbackLink simulatedLink = this.getLinkForTesting();

    StreamPayer streamPayer = new StreamPayer.Default(
      streamEncryptionUtils, simulatedLink, newExternalExchangeRateProvider(Ratio.ONE), new SimpleSpspClient()
    );

    final BigDecimal amountToSendInXrp = new BigDecimal("1000");

    streamPayer.getQuote(PaymentOptions.builder()
      .senderAccountDetails(sourceAccountDetails)
      .amountToSend(amountToSendInXrp)
      .destinationPaymentPointer(PaymentPointer.of("$example.com/foo"))
      .slippage(Slippage.of(Percentage.of(BigDecimal.valueOf(200L))))
      .build());
  }

  @Test
  public void getQuoteFailsIfLinkBusy() throws ExecutionException, InterruptedException {
    final AccountDetails sourceAccountDetails = getSourceAccountDetails();
    final LoopbackLink simulatedLink = this.getLinkForTesting(Optional.of(InterledgerErrorCode.T02_PEER_BUSY));

    StreamPayer streamPayer = new StreamPayer.Default(
      streamEncryptionUtils, simulatedLink, newExternalExchangeRateProvider(Ratio.ONE),
      this.spspMockClient()
    );

    final BigDecimal amountToSendInXrp = new BigDecimal("1000");
    final PaymentOptions paymentOptions = PaymentOptions.builder()
      .senderAccountDetails(sourceAccountDetails)
      .amountToSend(amountToSendInXrp)
      .destinationPaymentPointer(PaymentPointer.of("$example.com/foo"))
      .build();
    Throwable error = streamPayer.getQuote(paymentOptions)
      .handle(($, throwable) -> {
        assertThat($).isNull();
        assertThat(throwable).isNotNull();
        return throwable;
      })
      .get();
    assertThat(error.getCause().getMessage()).isEqualTo("No lowerBoundRate was detected from the receiver");
    assertThat(error.getCause() instanceof StreamPayerException).isTrue();
    assertThat(((StreamPayerException) error.getCause()).getSendState()).isEqualTo(SendState.RateProbeFailed);
  }

  @Test
  public void getQuoteFailsIfLinkCannotConnect() throws ExecutionException, InterruptedException {
    final AccountDetails sourceAccountDetails = getSourceAccountDetails();
    final LoopbackLink simulatedLink = this.getLinkForTesting(Optional.of(InterledgerErrorCode.F00_BAD_REQUEST));

    StreamPayer streamPayer = new StreamPayer.Default(
      streamEncryptionUtils, simulatedLink, newExternalExchangeRateProvider(Ratio.ONE),
      this.spspMockClient()
    );

    final BigDecimal amountToSendInXrp = new BigDecimal("1000");
    final PaymentOptions paymentOptions = PaymentOptions.builder()
      .senderAccountDetails(sourceAccountDetails)
      .amountToSend(amountToSendInXrp)
      .destinationPaymentPointer(PaymentPointer.of("$example.com/foo"))
      .build();
    Throwable error = streamPayer.getQuote(paymentOptions)
      .handle(($, throwable) -> {
        assertThat($).isNull();
        assertThat(throwable).isNotNull();
        return throwable;
      })
      .get();
    assertThat(error.getCause().getMessage()).isEqualTo("No lowerBoundRate was detected from the receiver");
    assertThat(error.getCause() instanceof StreamPayerException).isTrue();
    assertThat(((StreamPayerException) error.getCause()).getSendState()).isEqualTo(SendState.RateProbeFailed);
  }

  @Test
  public void getQuoteFailsOnIncompatibleAddressSchemes() throws ExecutionException, InterruptedException {
    final AccountDetails sourceAccountDetails = getSourceAccountDetails();
    final LoopbackLink simulatedLink = this.getLinkForTesting();

    StreamPayer streamPayer = new StreamPayer.Default(
      streamEncryptionUtils,
      // Incompatible address scheme (`private` vs `example`)
      simulatedLink, newExternalExchangeRateProvider(Ratio.ONE),
      this.spspMockClient(
        this.getSourceAccountDetails(),
        AccountDetails.builder()
          .interledgerAddress(InterledgerAddress.of("private.receiever"))
          .denomination(Denominations.USD)
          .build()
      )
    );

    final BigDecimal amountToSendInXrp = new BigDecimal("1000");
    final PaymentOptions paymentOptions = PaymentOptions.builder()
      .senderAccountDetails(sourceAccountDetails)
      .amountToSend(amountToSendInXrp)
      .destinationPaymentPointer(PaymentPointer.of("$example.com/foo"))
      .build();
    Throwable error = streamPayer.getQuote(paymentOptions)
      .handle((quote, throwable) -> {
        assertThat(quote).isNull();
        assertThat(throwable).isNotNull();
        return throwable;
      })
      .get();

    assertThat(error.getCause().getMessage()).contains("Quote failed: incompatible sender/receiver address schemes.");
    assertThat(error.getCause() instanceof StreamPayerException).isTrue();
    assertThat(((StreamPayerException) error.getCause()).getSendState())
      .isEqualTo(SendState.IncompatibleInterledgerNetworks);
  }

  @Test
  public void getQuoteFailsOnNegativeSendAmount() throws ExecutionException, InterruptedException {
    final AccountDetails sourceAccountDetails = getSourceAccountDetails();
    final LoopbackLink simulatedLink = this.getLinkForTesting();

    StreamPayer streamPayer = new StreamPayer.Default(
      streamEncryptionUtils,
      simulatedLink, newExternalExchangeRateProvider(Ratio.ONE),
      this.spspMockClient()
    );

    final BigDecimal amountToSendInXrp = new BigDecimal("-1000");
    final PaymentOptions paymentOptions = PaymentOptions.builder()
      .senderAccountDetails(sourceAccountDetails)
      .amountToSend(amountToSendInXrp)
      .destinationPaymentPointer(PaymentPointer.of("$example.com/foo"))
      .build();
    streamPayer.getQuote(paymentOptions)
      .handle(($, throwable) -> {
        assertThat(throwable.getCause().getMessage())
          .contains("Payment source amount must be a non-negative amount.");
        assertThat(throwable.getCause() instanceof StreamPayerException).isTrue();
        assertThat(((StreamPayerException) throwable.getCause()).getSendState())
          .isEqualTo(SendState.InvalidSourceAmount);
        return null;
      })
      .get();
  }

  @Test
  public void getQuoteFailsOnSendAmountMorePreciseThanReceiveAmount() throws ExecutionException, InterruptedException {
    final AccountDetails sourceAccountDetails = AccountDetails.builder()
      .interledgerAddress(InterledgerAddress.of("example.unit.test.sender"))
      .denomination(Denomination.builder()
        .assetCode("USD")
        .assetScale((short) 3) // Asset scale only allows 3 units of precision
        .build())
      .build();
    final LoopbackLink simulatedLink = this.getLinkForTesting();

    StreamPayer streamPayer = new StreamPayer.Default(
      streamEncryptionUtils, simulatedLink, newExternalExchangeRateProvider(Ratio.ONE),
      this.spspMockClient()
    );

    final BigDecimal amountToSendInXrp = new BigDecimal("100.0001");
    final PaymentOptions paymentOptions = PaymentOptions.builder()
      .senderAccountDetails(sourceAccountDetails)
      .amountToSend(amountToSendInXrp)
      .destinationPaymentPointer(PaymentPointer.of("$example.com/foo"))
      .build();
    streamPayer.getQuote(paymentOptions)
      .handle(($, throwable) -> {
        assertThat(throwable.getCause().getMessage()).contains("Invalid source scale");
        assertThat(throwable.getCause() instanceof StreamPayerException).isTrue();
        assertThat(((StreamPayerException) throwable.getCause()).getSendState())
          .isEqualTo(SendState.InvalidSourceAmount);
        return null;
      })
      .get();
  }

  @Test
  public void getQuoteFailsIfAmountToDeliverIsZero() throws ExecutionException, InterruptedException {
    final AccountDetails sourceAccountDetails = this.getSourceAccountDetails();
    final AccountDetails destinationAccountDetails = this.getDestinationAccountDetails();
    final LoopbackLink simulatedLink = this.getLinkForTesting();

    final Ratio externalExchangeRate = Ratio.ONE;
    final Ratio trackedLowerBoundRate = externalExchangeRate;
    final Ratio trackedUpperBoundRate = externalExchangeRate;

    final PaymentSharedStateTracker paymentSharedStateTrackerMock = this.newPaymentSharedStateTrackerMock(
      sourceAccountDetails, destinationAccountDetails, trackedLowerBoundRate, trackedUpperBoundRate
    );

    final ExchangeRateProvider externalExchangeRateProviderMock = this
      .newExternalExchangeRateProvider(externalExchangeRate);

    StreamPayer streamPayer = new StreamPayer.Default(
      streamEncryptionUtils,
      simulatedLink,
      externalExchangeRateProviderMock,
      this.spspMockClient()
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

    final BigDecimal amountToSendInXrp = new BigDecimal("0");
    final PaymentOptions paymentOptions = PaymentOptions.builder()
      .senderAccountDetails(sourceAccountDetails)
      .amountToSend(amountToSendInXrp)
      .destinationPaymentPointer(PaymentPointer.of("$example.com/foo"))
      .build();
    Throwable error = streamPayer.getQuote(paymentOptions)
      .handle((quote, throwable) -> {
        assertThat(quote).isNull();
        assertThat(throwable).isNotNull();
        return throwable;
      })
      .get();

    assertThat(error.getCause().getMessage()).contains("targetAmount must be greater-than 0");
  }

  @Test
  public void getQuoteFailsIfMaxPacketAmountIsZero() throws ExecutionException, InterruptedException {
    final AccountDetails sourceAccountDetails = this.getSourceAccountDetails();
    final AccountDetails destinationAccountDetails = this.getDestinationAccountDetails();
    final LoopbackLink simulatedLink = this.getLinkForTesting();

    final Ratio externalExchangeRate = Ratio.builder().numerator(BigInteger.ONE).denominator(BigInteger.ONE).build();
    final Ratio trackedLowerBoundRate = externalExchangeRate;
    final Ratio trackedUpperBoundRate = externalExchangeRate;

    final PaymentSharedStateTracker paymentSharedStateTrackerMock = this.newPaymentSharedStateTrackerMock(
      sourceAccountDetails, destinationAccountDetails, trackedLowerBoundRate, trackedUpperBoundRate
    );
    final ExchangeRateProvider externalExchangeRateProviderMock = this
      .newExternalExchangeRateProvider(externalExchangeRate);

    StreamPayer streamPayer = new StreamPayer.Default(
      streamEncryptionUtils,
      simulatedLink,
      externalExchangeRateProviderMock,
      this.spspMockClient()
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
      .build();
    Throwable error = streamPayer.getQuote(paymentOptions)
      .handle((quote, throwable) -> {
        assertThat(quote).isNull();
        assertThat(throwable).isNotNull();
        return throwable;
      }).get();

    assertThat(error.getCause().getMessage()).contains(
      "Rate enforcement may incur rounding errors. maxPacketAmount=0 is below proposed minimum of 100"
    );
    assertThat(error.getCause() instanceof StreamPayerException).isTrue();
    assertThat(((StreamPayerException) error.getCause()).getSendState()).isEqualTo(SendState.ExchangeRateRoundingError);
  }

  @Test
  public void getQuoteFailsIfNoDestinationAssetDetails() throws ExecutionException, InterruptedException {
    final AccountDetails sourceAccountDetails = this.getSourceAccountDetails();

    final Ratio externalExchangeRate = Ratio.ONE;

    final ExchangeRateProvider externalExchangeRateProviderMock = this
      .newExternalExchangeRateProvider(externalExchangeRate);

    final StatelessStreamReceiver statelessStreamReceiver = new StatelessStreamReceiver(
      () -> SERVER_SECRET_BYTES,
      new SpspStreamConnectionGenerator(),
      new AesGcmStreamEncryptionService(),
      StreamCodecContextFactory.oer()
    ) {
      @Override
      protected List<StreamFrame> constructResponseFrames(StreamPacket streamPacket, Denomination denomination) {
        List<StreamFrame> existingFrames = super.constructResponseFrames(streamPacket, denomination);
        return existingFrames.stream()
          // Always exclude ConnectionAssetDetails
          .filter(streamFrame -> streamFrame.streamFrameType() != StreamFrameType.ConnectionAssetDetails)
          .collect(Collectors.toList());
      }
    };
    final StatelessStreamReceiverLink simulatedLink = new StatelessStreamReceiverLink(
      () -> LINK_OPERATOR_ADDRESS,
      StatelessSpspReceiverLinkSettings.builder()
        .maxPacketAmount(UnsignedLong.MAX_VALUE)
        .assetCode("USD")
        .assetScale(0)
        .build(),
      statelessStreamReceiver
    );
    simulatedLink.setLinkId(LinkId.of("unit-test-loopback-link"));

    StreamPayer streamPayer = new StreamPayer.Default(
      streamEncryptionUtils, simulatedLink, externalExchangeRateProviderMock, this.spspMockClient()
    );
    final BigDecimal amountToSendInXrp = new BigDecimal("0.000000001");
    final PaymentOptions paymentOptions = PaymentOptions.builder()
      .senderAccountDetails(sourceAccountDetails)
      .amountToSend(amountToSendInXrp)
      .destinationPaymentPointer(PaymentPointer.of("$example.com/foo"))
      .build();
    Throwable error = streamPayer.getQuote(paymentOptions)
      .handle((quote, throwable) -> {
        assertThat(quote).isNull();
        assertThat(throwable).isNotNull();
        return throwable;
      })
      .get();

    assertThat(error.getCause().getMessage())
      .contains("Receiver denomination is required for FX (Receiver never shared asset details)");
    assertThat(error.getCause() instanceof StreamPayerException).isTrue();
    assertThat(((StreamPayerException) error.getCause()).getSendState()).isEqualTo(SendState.UnknownDestinationAsset);
  }

  @Test
  public void getQuoteFailsWithDestinationAssetDetailsConflict() throws ExecutionException, InterruptedException {
    final AccountDetails sourceAccountDetails = this.getSourceAccountDetails();
    final AccountDetails destinationAccountDetails = this.getDestinationAccountDetails();

    final StatelessStreamReceiver statelessStreamReceiver = new StatelessStreamReceiver(
      () -> SERVER_SECRET_BYTES,
      new SpspStreamConnectionGenerator(),
      new AesGcmStreamEncryptionService(),
      StreamCodecContextFactory.oer()
    ) {
      @Override
      protected List<StreamFrame> constructResponseFrames(StreamPacket streamPacket,
        Denomination denomination) {
        return ImmutableList.<StreamFrame>builder().addAll(
          super.constructResponseFrames(streamPacket, denomination))
          .add(ConnectionAssetDetailsFrame.builder().sourceDenomination(Denominations.USD).build())
          .add(ConnectionAssetDetailsFrame.builder().sourceDenomination(Denominations.EUR).build())
          .build();
      }
    };
    final StatelessStreamReceiverLink statelessStreamReceiverLink = new StatelessStreamReceiverLink(
      () -> LINK_OPERATOR_ADDRESS,
      StatelessSpspReceiverLinkSettings.builder()
        .maxPacketAmount(UnsignedLong.MAX_VALUE)
        .assetCode("USD")
        .assetScale(0)
        .build(),
      statelessStreamReceiver
    );
    statelessStreamReceiverLink.setLinkId(LinkId.of("unit-test-loopback-link"));

    final Ratio externalExchangeRate = Ratio.ONE;
    final ExchangeRateProvider externalExchangeRateProviderMock = this
      .newExternalExchangeRateProvider(externalExchangeRate);

    final SpspClient mockedSpspClient = new SpspClient() {
      @Override
      public StreamConnectionDetails getStreamConnectionDetails(PaymentPointer paymentPointer)
        throws InvalidReceiverClientException {
        return statelessStreamReceiver.setupStream(destinationAccountDetails.interledgerAddress());
      }

      @Override
      public StreamConnectionDetails getStreamConnectionDetails(HttpUrl spspUrl) throws InvalidReceiverClientException {
        return statelessStreamReceiver.setupStream(destinationAccountDetails.interledgerAddress());
      }
    };

    // By setting a very small FX rate, we can simulate a destination amount of 0.
    StreamPayer streamPayer = new StreamPayer.Default(
      streamEncryptionUtils,
      statelessStreamReceiverLink,
      externalExchangeRateProviderMock,
      mockedSpspClient
    );

    final BigDecimal amountToSend = BigDecimal.ONE;
    final PaymentOptions paymentOptions = PaymentOptions.builder()
      .senderAccountDetails(sourceAccountDetails)
      .amountToSend(amountToSend)
      .destinationPaymentPointer(PaymentPointer.of("$example.com/foo"))
      .build();

    Throwable error = streamPayer.getQuote(paymentOptions)
      .handle(($, throwable) -> {
        assertThat($).isNull();
        assertThat(throwable).isNotNull();
        return throwable;
      })
      .get();

    assertThat(error.getCause().getMessage())
      .contains("Only one ConnectionAssetDetails frame allowed on a single connection");
    assertThat(error.getCause() instanceof StreamPayerException).isTrue();
    assertThat(((StreamPayerException) error.getCause()).getSendState()).isEqualTo(SendState.DestinationAssetConflict);
  }

  @Test
  public void getQuoteFailsIfNoSourceInExternalOracle() throws ExecutionException, InterruptedException {

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

    // By setting a very small FX rate, we can simulate a destination amount of 0.
    final Ratio externalExchangeRate = Ratio.ONE;
    final Ratio trackedLowerBoundRate = externalExchangeRate;
    final Ratio trackedUpperBoundRate = externalExchangeRate;

    final PaymentSharedStateTracker paymentSharedStateTrackerMock = this.newPaymentSharedStateTrackerMock(
      sourceAccountDetails, destinationAccountDetails, trackedLowerBoundRate, trackedUpperBoundRate
    );

    final ExchangeRateProvider exchangeRateProviderMock = newExternalExchangeRateProvider(externalExchangeRate);
    doThrow(new RuntimeException("no rate found")).when(exchangeRateProviderMock).getExchangeRate("FOO", "USD");

    // By setting a very small FX rate, we can simulate a destination amount of 0.
    StreamPayer streamPayer = new StreamPayer.Default(
      streamEncryptionUtils, simulatedLink, exchangeRateProviderMock,
      this.spspMockClient()
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
      .build();
    Throwable error = streamPayer.getQuote(paymentOptions)
      .handle((quote, throwable) -> {
        assertThat(quote).isNull();
        assertThat(throwable).isNotNull();
        return throwable;
      })
      .get();

    assertThat(error.getCause().getMessage()).contains("No rate found in oracleExchangeRateProvider");
    assertThat(error.getCause() instanceof StreamPayerException).isTrue();
    assertThat(((StreamPayerException) error.getCause()).getSendState()).isEqualTo(SendState.ExternalRateUnavailable);
  }

  @Test
  public void getQuoteFailsIfNoDestAssetInExternalOracle() throws ExecutionException, InterruptedException {
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

    // By setting a very small FX rate, we can simulate a destination amount of 0.
    final Ratio externalExchangeRate = Ratio.ONE;
    final Ratio trackedLowerBoundRate = externalExchangeRate;
    final Ratio trackedUpperBoundRate = externalExchangeRate;

    final PaymentSharedStateTracker paymentSharedStateTrackerMock = this.newPaymentSharedStateTrackerMock(
      sourceAccountDetails, destinationAccountDetails, trackedLowerBoundRate, trackedUpperBoundRate
    );

    final ExchangeRateProvider exchangeRateProviderMock = newExternalExchangeRateProvider(externalExchangeRate);
    doThrow(new RuntimeException("no rate found")).when(exchangeRateProviderMock).getExchangeRate("USD", "FOO");

    // By setting a very small FX rate, we can simulate a destination amount of 0.
    StreamPayer streamPayer = new StreamPayer.Default(
      streamEncryptionUtils, simulatedLink, exchangeRateProviderMock,
      this.spspMockClient()
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
      .build();
    Throwable error = streamPayer.getQuote(paymentOptions)
      .handle((quote, throwable) -> {
        assertThat(quote).isNull();
        assertThat(throwable).isNotNull();
        return throwable;
      }).get();

    assertThat(error.getCause().getMessage()).contains("No rate found in oracleExchangeRateProvider");
    assertThat(error.getCause() instanceof StreamPayerException).isTrue();
    assertThat(((StreamPayerException) error.getCause()).getSendState()).isEqualTo(SendState.ExternalRateUnavailable);
  }

  @Test
  public void getQuoteFailsIfExternalExchangeRateIs0() throws ExecutionException, InterruptedException {
    final AccountDetails sourceAccountDetails = this.getSourceAccountDetails();
    final AccountDetails destinationAccountDetails = this.getDestinationAccountDetails();
    final LoopbackLink simulatedLink = this.getLinkForTesting();

    final Ratio externalExchangeRate = Ratio.ZERO;
    final Ratio trackedLowerBoundRate = externalExchangeRate;
    final Ratio trackedUpperBoundRate = externalExchangeRate;

    final PaymentSharedStateTracker paymentSharedStateTrackerMock = this.newPaymentSharedStateTrackerMock(
      sourceAccountDetails, destinationAccountDetails, trackedLowerBoundRate, trackedUpperBoundRate
    );

    final ExchangeRateProvider externalExchangeRateProviderMock = this
      .newExternalExchangeRateProvider(externalExchangeRate);

    StreamPayer streamPayer = new StreamPayer.Default(
      streamEncryptionUtils,
      simulatedLink,
      externalExchangeRateProviderMock,
      this.spspMockClient()
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
      .build();
    Throwable error = streamPayer.getQuote(paymentOptions)
      .handle((quote, throwable) -> {
        assertThat(quote).isNull();
        assertThat(throwable).isNotNull();
        return throwable;
      }).get();

    assertThat(error.getCause().getMessage()).contains("External exchange rate was 0.");
    assertThat(error.getCause() instanceof StreamPayerException).isTrue();
    assertThat(((StreamPayerException) error.getCause()).getSendState()).isEqualTo(SendState.ExternalRateUnavailable);
  }

  @Test
  public void getQuoteFailsIfTrackedRateIsOneBelowOracleRate() throws ExecutionException, InterruptedException {
    final AccountDetails sourceAccountDetails = this.getSourceAccountDetails();
    final AccountDetails destinationAccountDetails = this.getDestinationAccountDetails();
    final LoopbackLink simulatedLink = this.getLinkForTesting();

    final Ratio externalExchangeRate = Ratio.ONE;
    final Ratio trackedLowerBoundRate = Ratio.from(new BigDecimal("0.99"));
    final Ratio trackedUpperBoundRate = Ratio.from(new BigDecimal("0.99"));

    final PaymentSharedStateTracker paymentSharedStateTrackerMock = this.newPaymentSharedStateTrackerMock(
      sourceAccountDetails, destinationAccountDetails, trackedLowerBoundRate, trackedUpperBoundRate
    );

    final ExchangeRateProvider externalExchangeRateProviderMock = this
      .newExternalExchangeRateProvider(externalExchangeRate);

    // By setting a very small FX rate, we can simulate a destination amount of 0.
    StreamPayer streamPayer = new StreamPayer.Default(
      streamEncryptionUtils,
      simulatedLink,
      externalExchangeRateProviderMock,
      this.spspMockClient()
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
      .slippage(Slippage.NONE)
      .build();
    Throwable error = streamPayer.getQuote(paymentOptions)
      .handle((quote, throwable) -> {
        assertThat(quote).isNull();
        assertThat(throwable).isNotNull();
        return throwable;
      }).get();

    assertThat(error).isNotNull();
    assertThat(error.getCause().getMessage()).contains(
      "Rate-probed exchange-rate of 0.99 is less-than than the minimum exchange-rate of 1"
    );
    assertThat(error.getCause() instanceof StreamPayerException).isTrue();
    assertThat(((StreamPayerException) error.getCause()).getSendState()).isEqualTo(SendState.InsufficientExchangeRate);
  }

  @Test
  public void getQuoteFailsIfTrackedRateIsTwoBelowOracleRate() throws ExecutionException, InterruptedException {
    final AccountDetails sourceAccountDetails = this.getSourceAccountDetails();
    final AccountDetails destinationAccountDetails = this.getDestinationAccountDetails();
    final LoopbackLink simulatedLink = this.getLinkForTesting();

    final Ratio externalExchangeRate = Ratio.ONE;
    final Ratio trackedLowerBoundRate = Ratio.from(new BigDecimal("0.98"));
    final Ratio trackedUpperBoundRate = Ratio.from(new BigDecimal("0.98"));

    final PaymentSharedStateTracker paymentSharedStateTrackerMock = this.newPaymentSharedStateTrackerMock(
      sourceAccountDetails, destinationAccountDetails, trackedLowerBoundRate, trackedUpperBoundRate
    );

    final ExchangeRateProvider externalExchangeRateProviderMock = this
      .newExternalExchangeRateProvider(externalExchangeRate);

    // By setting a very small FX rate, we can simulate a destination amount of 0.
    StreamPayer streamPayer = new StreamPayer.Default(
      streamEncryptionUtils, simulatedLink,
      externalExchangeRateProviderMock,
      this.spspMockClient()
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
      .slippage(Slippage.NONE)
      .build();
    Throwable error = streamPayer.getQuote(paymentOptions)
      .handle((quote, throwable) -> {
        assertThat(quote).isNull();
        assertThat(throwable).isNotNull();
        return throwable;
      }).get();

    assertThat(error).isNotNull();
    assertThat(error.getCause().getMessage()).contains(
      "Rate-probed exchange-rate of 0.98 is less-than than the minimum exchange-rate of 1"
    );
    assertThat(error.getCause() instanceof StreamPayerException).isTrue();
    assertThat(((StreamPayerException) error.getCause()).getSendState()).isEqualTo(SendState.InsufficientExchangeRate);
  }

  @Test
  public void getQuoteFailsIfTrackedRateIs0() throws ExecutionException, InterruptedException {
    final AccountDetails sourceAccountDetails = this.getSourceAccountDetails();
    final AccountDetails destinationAccountDetails = this.getDestinationAccountDetails();
    final LoopbackLink simulatedLink = this.getLinkForTesting();

    // By setting a very small FX rate, we can simulate a destination amount of 0.
    final Ratio externalExchangeRate = Ratio.ONE;
    final Ratio trackedLowerBoundRate = Ratio.ZERO;
    final Ratio trackedUpperBoundRate = Ratio.ZERO;

    final PaymentSharedStateTracker paymentSharedStateTrackerMock = this.newPaymentSharedStateTrackerMock(
      sourceAccountDetails, destinationAccountDetails, trackedLowerBoundRate, trackedUpperBoundRate
    );

    final ExchangeRateProvider externalExchangeRateProviderMock = this
      .newExternalExchangeRateProvider(externalExchangeRate);

    // By setting a very small FX rate, we can simulate a destination amount of 0.
    StreamPayer streamPayer = new StreamPayer.Default(
      streamEncryptionUtils, simulatedLink,
      externalExchangeRateProviderMock,
      this.spspMockClient()
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
    Throwable error = streamPayer.getQuote(paymentOptions)
      .handle((quote, throwable) -> {
        assertThat(quote).isNull();
        assertThat(throwable).isNotNull();
        return throwable;
      }).get();

    assertThat(error).isNotNull();
    assertThat(error.getCause().getMessage()).contains("Rate Probe discovered invalid exchange rates.");
    assertThat(error.getCause() instanceof StreamPayerException).isTrue();
    assertThat(((StreamPayerException) error.getCause()).getSendState()).isEqualTo(SendState.InsufficientExchangeRate);
  }

  /**
   * Quote fails if min-rate and max-packet amount would cause rounding errors.
   */
  @Test
  public void getQuoteFailsIfRoundingErrors() throws ExecutionException, InterruptedException {
    final AccountDetails sourceAccountDetails = AccountDetails.builder()
      .interledgerAddress(InterledgerAddress.of("example.larry.sender"))
      .denomination(Denomination.builder().assetCode("BTC").assetScale((short) 8).build())
      .build();
    final AccountDetails destinationAccountDetails = AccountDetails.builder()
      .interledgerAddress(InterledgerAddress.of("example.larry.receiver"))
      .denomination(Denomination.builder().assetCode("EUR").assetScale((short) 6).build())
      .build();
    final LoopbackLink simulatedLink = this.getLinkForTesting(InterledgerErrorCode.F99_APPLICATION_ERROR);

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

    final PaymentSharedStateTracker paymentSharedStateTrackerMock = this.newPaymentSharedStateTrackerMock(
      sourceAccountDetails, destinationAccountDetails, trackedLowerBoundRate, trackedUpperBoundRate
    );

    final ExchangeRateProvider externalExchangeRateProviderMock = this
      .newExternalExchangeRateProvider(externalExchangeRate);

    // By setting a very small FX rate, we can simulate a destination amount of 0.
    StreamPayer streamPayer = new StreamPayer.Default(
      streamEncryptionUtils, simulatedLink,
      externalExchangeRateProviderMock,
      this.spspMockClient()
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

    Throwable error = streamPayer.getQuote(paymentOptions)
      .handle((quote, throwable) -> {
        assertThat(quote).isNull();
        assertThat(throwable).isNotNull();
        return throwable;
      }).get();

    assertThat(error).isNotNull();
    assertThat(error.getCause().getMessage()).contains(
      "Rate-probed exchange-rate of 86.806 is less-than than the minimum exchange-rate of 86.8056180495575233622");
    assertThat(error.getCause() instanceof StreamPayerException).isTrue();
    assertThat(((StreamPayerException) error.getCause()).getSendState()).isEqualTo(SendState.InsufficientExchangeRate);
  }

  /**
   * Validates that the implementation discovers a Precise max-packet amount from F08s that have no metadata.
   */
  @Test
  public void getQuoteDiscoversMaxPacketAmountWithoutMetadata() throws ExecutionException, InterruptedException {
    final AccountDetails sourceAccountDetails = AccountDetails.builder()
      .interledgerAddress(InterledgerAddress.of("example.unit.test.sender"))
      .denomination(Denominations.USD)
      .build();
    final AccountDetails destinationAccountDetails = AccountDetails.builder()
      .interledgerAddress(InterledgerAddress.of("example.larry.receiver"))
      .denomination(Denominations.USD)
      .build();

    final UnsignedLong maxPacketAmount = UnsignedLong.valueOf(300324);

    // Link with a MaxPacket Amount of 300324
    final StatelessStreamReceiver statelessStreamReceiver = new StatelessStreamReceiver(
      () -> SERVER_SECRET_BYTES,
      new SpspStreamConnectionGenerator(),
      new AesGcmStreamEncryptionService(),
      StreamCodecContextFactory.oer()
    );
    final StatelessStreamReceiverLink simulatedLink = new StatelessStreamReceiverLink(
      () -> LINK_OPERATOR_ADDRESS,
      StatelessSpspReceiverLinkSettings.builder()
        .maxPacketAmount(maxPacketAmount)
        .assetCode("USD")
        .assetScale(0)
        .build(),
      statelessStreamReceiver
    );
    simulatedLink.setLinkId(LinkId.of("unit-test-loopback-link"));

    final Ratio externalExchangeRate = Ratio.ONE;
    final ExchangeRateProvider externalExchangeRateProviderMock = this
      .newExternalExchangeRateProvider(externalExchangeRate);

    final SpspClient mockedSpspClient = new SpspClient() {
      @Override
      public StreamConnectionDetails getStreamConnectionDetails(PaymentPointer paymentPointer)
        throws InvalidReceiverClientException {
        return statelessStreamReceiver.setupStream(destinationAccountDetails.interledgerAddress());
      }

      @Override
      public StreamConnectionDetails getStreamConnectionDetails(HttpUrl spspUrl) throws InvalidReceiverClientException {
        return statelessStreamReceiver.setupStream(destinationAccountDetails.interledgerAddress());
      }
    };

    // By setting a very small FX rate, we can simulate a destination amount of 0.
    StreamPayer streamPayer = new StreamPayer.Default(
      streamEncryptionUtils,
      simulatedLink,
      externalExchangeRateProviderMock,
      mockedSpspClient
    );

    final BigDecimal amountToSend = new BigDecimal(4);
    final PaymentOptions paymentOptions = PaymentOptions.builder()
      .senderAccountDetails(sourceAccountDetails)
      .amountToSend(amountToSend)
      .destinationPaymentPointer(PaymentPointer.of("$example.com/foo"))
      .slippage(Slippage.ONE_PERCENT)
      .build();

    final Quote quote = streamPayer.getQuote(paymentOptions).get();
    assertThat(quote).isNotNull();
    assertThat(quote.paymentSharedStateTracker().getMaxPacketAmountTracker().getMaxPacketAmount()).isEqualTo(
      MaxPacketAmount.builder()
        .maxPacketState(MaxPacketState.PreciseMax)
        .value(maxPacketAmount)
        .build());
  }

  @Test
  public void getQuoteIfTrackedRateIsOneWithNoSlippage() throws ExecutionException, InterruptedException {
    final AccountDetails sourceAccountDetails = this.getSourceAccountDetails();
    final AccountDetails destinationAccountDetails = this.getDestinationAccountDetails();
    final LoopbackLink simulatedLink = this.getLinkForTesting();

    // By setting a very small FX rate, we can simulate a destination amount of 0.
    final Ratio externalExchangeRate = Ratio.ONE;
    final Ratio trackedLowerBoundRate = externalExchangeRate;
    final Ratio trackedUpperBoundRate = externalExchangeRate;

    final PaymentSharedStateTracker paymentSharedStateTrackerMock = this.newPaymentSharedStateTrackerMock(
      sourceAccountDetails, destinationAccountDetails, trackedLowerBoundRate, trackedUpperBoundRate
    );

    final ExchangeRateProvider externalExchangeRateProviderMock = this
      .newExternalExchangeRateProvider(externalExchangeRate);

    // By setting a very small FX rate, we can simulate a destination amount of 0.
    StreamPayer streamPayer = new StreamPayer.Default(
      streamEncryptionUtils, simulatedLink,
      externalExchangeRateProviderMock,
      this.spspMockClient()
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
      .slippage(Slippage.NONE)
      .build();
    final Quote quote = streamPayer.getQuote(paymentOptions)
      .handle(($, throwable) -> {
        assertThat($).isNotNull();
        assertThat(throwable).isNull();
        return $;
      }).get();

    assertThat(quote.estimatedPaymentOutcome().minDeliveryAmountInWholeDestinationUnits()).isEqualTo(BigInteger.ONE);
    assertThat(quote.estimatedPaymentOutcome().maxSendAmountInWholeSourceUnits()).isEqualTo(BigInteger.ONE);
    assertThat(quote.estimatedPaymentOutcome().estimatedNumberOfPackets()).isEqualTo(BigInteger.ONE);
  }

  ////////////////
  // Payment Tests
  ////////////////

  /**
   * Validates that the implementation discovers a Precise max-packet amount from F08s that have no metadata.
   */
  @Test
  public void getPayDeliversNoValue() {
    final AccountDetails sourceAccountDetails = this.getSourceAccountDetails();
    final AccountDetails destinationAccountDetails = this.getDestinationAccountDetails();

    // Link with a MaxPacket Amount of 300324
    final StatelessStreamReceiver statelessStreamReceiver = new StatelessStreamReceiver(
      () -> SERVER_SECRET_BYTES,
      new SpspStreamConnectionGenerator(),
      new AesGcmStreamEncryptionService(),
      StreamCodecContextFactory.oer()
    ) {
      @Override
      protected boolean isFulfillable(
        InterledgerPreparePacket preparePacket, InterledgerFulfillment fulfillment
      ) {
        return false; // <-- No packet should fulfill, but Stream responses still work on rejects.
      }
    };
    final StatelessStreamReceiverLink simulatedLink = new StatelessStreamReceiverLink(
      () -> LINK_OPERATOR_ADDRESS,
      StatelessSpspReceiverLinkSettings.builder()
        .maxPacketAmount(UnsignedLong.MAX_VALUE)
        .assetCode("USD")
        .assetScale(0)
        .build(),
      statelessStreamReceiver
    );
    simulatedLink.setLinkId(LinkId.of("unit-test-loopback-link"));

    final Ratio externalExchangeRate = Ratio.ONE;
    final ExchangeRateProvider externalExchangeRateProviderMock = this
      .newExternalExchangeRateProvider(externalExchangeRate);

    final SpspClient mockedSpspClient = new SpspClient() {
      @Override
      public StreamConnectionDetails getStreamConnectionDetails(PaymentPointer paymentPointer)
        throws InvalidReceiverClientException {
        return statelessStreamReceiver.setupStream(destinationAccountDetails.interledgerAddress());
      }

      @Override
      public StreamConnectionDetails getStreamConnectionDetails(HttpUrl spspUrl) throws InvalidReceiverClientException {
        return statelessStreamReceiver.setupStream(destinationAccountDetails.interledgerAddress());
      }
    };

    // By setting a very small FX rate, we can simulate a destination amount of 0.
    StreamPayer streamPayer = new StreamPayer.Default(
      streamEncryptionUtils,
      simulatedLink,
      externalExchangeRateProviderMock,
      mockedSpspClient
    );

    final BigDecimal amountToSend = new BigDecimal(4);
    final PaymentOptions paymentOptions = PaymentOptions.builder()
      .senderAccountDetails(sourceAccountDetails)
      .amountToSend(amountToSend)
      .destinationPaymentPointer(PaymentPointer.of("$example.com/foo"))
      .slippage(Slippage.ONE_PERCENT)
      .build();

    final Quote quote = streamPayer.getQuote(paymentOptions).join();
    Receipt receipt = streamPayer.pay(quote).join();

    // Assert that nothing was delivered.
    assertThat(receipt).isNotNull();
    assertThat(receipt.amountDeliveredInDestinationUnits()).isEqualTo(BigInteger.ZERO);
    assertThat(receipt.amountSentInSendersUnits()).isEqualTo(BigInteger.ZERO);
  }

  /**
   * When the probed exchange rate is equal to the minimum exchange rate, the payment may fail: consider if those rates
   * are both 1.0001 and I send a 100 unit packet, 100 will get delivered (intermediary rounds down), but 101 is the
   * minimum destination amount (must be rounded up).
   *
   * @see "https://github.com/interledgerjs/interledgerjs/issues/167"
   */
  @Test
  public void getPayWhenProbedAmountIsJustAboveMinimum() throws ExecutionException, InterruptedException {

    final AccountDetails sourceAccountDetails = this.getSourceAccountDetails();
    final AccountDetails destinationAccountDetails = this.getDestinationAccountDetails();

    final StatelessStreamReceiverLink simulatedLink = this.getFulfillableLinkForTesting(true);

    // By setting a very small FX rate, we can simulate a destination amount of 0.
    final Ratio externalExchangeRate = Ratio.builder()
      .numerator(BigInteger.valueOf(10001L))
      .denominator(BigInteger.valueOf(10000L))
      .build();
    final Ratio trackedLowerBoundRate = externalExchangeRate;
    final Ratio trackedUpperBoundRate = externalExchangeRate;

    final PaymentSharedStateTracker paymentSharedStateTrackerMock = this.newPaymentSharedStateTrackerMock(
      sourceAccountDetails, destinationAccountDetails, trackedLowerBoundRate, trackedUpperBoundRate
    );

    final ExchangeRateProvider externalExchangeRateProviderMock = this
      .newExternalExchangeRateProvider(externalExchangeRate);

    // By setting a very small FX rate, we can simulate a destination amount of 0.
    StreamPayer streamPayer = new StreamPayer.Default(
      streamEncryptionUtils,
      simulatedLink,
      externalExchangeRateProviderMock,
      this.spspMockClient()
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

    final BigDecimal amountToSendInXrp = new BigDecimal("0.0000001000");
    final PaymentOptions paymentOptions = PaymentOptions.builder()
      .senderAccountDetails(sourceAccountDetails)
      .amountToSend(amountToSendInXrp)
      .destinationPaymentPointer(PaymentPointer.of("$example.com/foo"))
      .slippage(Slippage.NONE)
      .build();
    Throwable error = streamPayer.getQuote(paymentOptions)
      .handle(($, throwable) -> {
        assertThat($).isNull();
        assertThat(throwable).isNotNull();
        return throwable;
      }).get();

    assertThat(error.getCause().getMessage())
      .contains("Rate-probed exchange-rate of 1.0001 is less-than than the minimum exchange-rate of 1.0001");
    assertThat(error.getCause() instanceof StreamPayerException).isTrue();
    assertThat(((StreamPayerException) error.getCause()).getSendState()).isEqualTo(SendState.InsufficientExchangeRate);
  }

  // TODO: Connection-spec tests.

  //////////////////////////////////
  // shiftForNormalization()
  //////////////////////////////////

  @Test
  public void testShiftForNormalization() {
    final LoopbackLink simulatedLink = this.getLinkForTesting();

    StreamPayer.Default streamPayer = new StreamPayer.Default(
      streamEncryptionUtils,
      simulatedLink,
      newExternalExchangeRateProvider(Ratio.ONE),
      this.spspMockClient()
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
      streamEncryptionUtils, simulatedLink, newExternalExchangeRateProvider(Ratio.ONE), new SimpleSpspClient()
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
    assertThat(actual.lowerBound()).isEqualTo(new BigDecimal("8.83928571428571471"));
    assertThat(actual.upperBound()).isEqualTo(new BigDecimal("9.01785714285714329"));
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

  // TODO Replace this with getFulfillableLinkForTesting and rename to getLinkForTesting
  private LoopbackLink getLinkForTesting() {
    return getLinkForTesting(Optional.empty());
  }

  private StatelessStreamReceiverLink getFulfillableLinkForTesting(final boolean fulfillable) {
    final StatelessStreamReceiver statelessStreamReceiver = new StatelessStreamReceiver(
      () -> SERVER_SECRET_BYTES,
      new SpspStreamConnectionGenerator(),
      new AesGcmStreamEncryptionService(),
      StreamCodecContextFactory.oer()
    ) {
      @Override
      protected boolean isFulfillable(
        InterledgerPreparePacket preparePacket, InterledgerFulfillment fulfillment
      ) {
        return fulfillable; // <-- No packet should fulfill, but Stream responses still work on rejects.
      }
    };
    final StatelessStreamReceiverLink simulatedLink = new StatelessStreamReceiverLink(
      () -> LINK_OPERATOR_ADDRESS,
      StatelessSpspReceiverLinkSettings.builder()
        .maxPacketAmount(UnsignedLong.MAX_VALUE)
        .assetCode("USD")
        .assetScale(0)
        .build(),
      statelessStreamReceiver
    );
    simulatedLink.setLinkId(LinkId.of("unit-test-loopback-link"));

    return simulatedLink;
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

  /**
   * A mock client that simulates what the Stateless Stream Receiver would have returned.
   */
  private SpspClient spspMockClient() {
    return this.spspMockClient(getSourceAccountDetails(), getDestinationAccountDetails());
  }

  /**
   * A mock client that simulates what the Stateless Stream Receiver would have returned.
   */
  private SpspClient spspMockClient(
    final AccountDetails sourceAccountDetails, final AccountDetails destinationAccountDetails
  ) {
    return new SpspClient() {
      @Override
      public StreamConnectionDetails getStreamConnectionDetails(PaymentPointer paymentPointer)
        throws InvalidReceiverClientException {
        final StreamConnection streamConnection = newStreamConnection(sourceAccountDetails, destinationAccountDetails);
        return StreamConnectionDetails.builder()
          .destinationAddress(streamConnection.getDestinationAddress())
          .sharedSecret(streamConnection.getSharedSecret())
          .build();
      }

      @Override
      public StreamConnectionDetails getStreamConnectionDetails(HttpUrl spspUrl) throws InvalidReceiverClientException {
        final StreamConnection streamConnection = newStreamConnection(sourceAccountDetails, destinationAccountDetails);
        return StreamConnectionDetails.builder()
          .destinationAddress(streamConnection.getDestinationAddress())
          .sharedSecret(streamConnection.getSharedSecret())
          .build();
      }
    };
  }

  private StreamPayer.Default getStreamPayerDefault() {
    final LoopbackLink simulatedLink = this.getLinkForTesting();
    return new StreamPayer.Default(
      streamEncryptionUtils,
      simulatedLink,
      newExternalExchangeRateProvider(Ratio.ONE),
      this.spspMockClient() // <-- Use the default sender/dest account details.
    );
  }

  private AssetDetailsTracker assetDetailsTrackerMock(
    final AccountDetails sourceAccountDetails, final AccountDetails destinationAccountDetails
  ) {
    AssetDetailsTracker assetDetailsTrackerMock = mock(AssetDetailsTracker.class);
    when(assetDetailsTrackerMock.getSourceAccountDetails()).thenReturn(sourceAccountDetails);
    when(assetDetailsTrackerMock.getDestinationAccountDetails()).thenReturn(destinationAccountDetails);
    return assetDetailsTrackerMock;
  }

  private StreamConnection newStreamConnection(
    final AccountDetails sourceAccountDetails, final AccountDetails destinationAccountDetails
  ) {
    Objects.requireNonNull(sourceAccountDetails);
    Objects.requireNonNull(destinationAccountDetails);

    final SpspStreamConnectionGenerator generator = new SpspStreamConnectionGenerator();
    // This ensures a real receiver can decrypt stream frames.
    final StreamConnectionDetails connectionDetails = generator.generateConnectionDetails(
      () -> SERVER_SECRET_BYTES, destinationAccountDetails.interledgerAddress()
    );

    return new StreamConnection(
      sourceAccountDetails,
      connectionDetails.destinationAddress(),
      connectionDetails.sharedSecret()
    );
  }

  private PaymentSharedStateTracker newPaymentSharedStateTrackerMock(
    final AccountDetails sourceAccountDetails,
    final AccountDetails receiverAccountDetails,
    final Ratio lowerBoundExchangeRate,
    final Ratio upperBoundExchangeRate
  ) {
    Objects.requireNonNull(sourceAccountDetails);
    Objects.requireNonNull(receiverAccountDetails);
    Objects.requireNonNull(lowerBoundExchangeRate);
    Objects.requireNonNull(upperBoundExchangeRate);

    final TestableExchangeRateTracker exchangeRateTracker = new TestableExchangeRateTracker();
    exchangeRateTracker.setRateBounds(lowerBoundExchangeRate, upperBoundExchangeRate);

    final AmountTracker amountTracker = new AmountTracker(exchangeRateTracker);
    final PaymentSharedStateTracker paymentSharedStateTrackerMock = mock(PaymentSharedStateTracker.class);
    when(paymentSharedStateTrackerMock.getStreamConnection())
      .thenReturn(newStreamConnection(sourceAccountDetails, receiverAccountDetails));
    when(paymentSharedStateTrackerMock.getAmountTracker()).thenReturn(amountTracker);
    when(paymentSharedStateTrackerMock.getExchangeRateTracker()).thenReturn(exchangeRateTracker);
    AssetDetailsTracker assetTrackerMock = assetDetailsTrackerMock(sourceAccountDetails, receiverAccountDetails);
    when(paymentSharedStateTrackerMock.getAssetDetailsTracker()).thenReturn(assetTrackerMock);
    when(paymentSharedStateTrackerMock.getPacingTracker()).thenReturn(new PacingTracker());
    when(paymentSharedStateTrackerMock.getMaxPacketAmountTracker()).thenReturn(new MaxPacketAmountTracker());
    return paymentSharedStateTrackerMock;
  }

  private SharedSecret obtainSharedSecretFromServer(final InterledgerAddress receiverAddress) {
    return new SpspStreamConnectionGenerator().generateConnectionDetails(
      () -> SERVER_SECRET_BYTES, receiverAddress
    ).sharedSecret();
  }

  private static class TestableExchangeRateTracker extends ExchangeRateTracker {

    @Override
    public void setRateBounds(Ratio lowerBoundRate, Ratio upperBoundRate) {
      super.setRateBounds(lowerBoundRate, upperBoundRate);
    }
  }


}