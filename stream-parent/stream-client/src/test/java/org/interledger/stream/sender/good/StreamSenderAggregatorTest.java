package org.interledger.stream.sender.good;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import org.interledger.core.InterledgerAddress;
import org.interledger.core.SharedSecret;
import org.interledger.link.Link;
import org.interledger.fx.Denomination;
import org.interledger.stream.StreamConnection;
import org.interledger.stream.crypto.StreamEncryptionUtils;
import org.interledger.stream.fx.ExchangeRateService;
import org.interledger.stream.good.SendMoneyRequest;
import org.interledger.stream.good.SendMoneyRequestBuilder;
import org.interledger.stream.sender.CongestionController;
import org.interledger.stream.sender.good.DefaultStreamSender.StreamSenderAggregator;

import com.google.common.primitives.UnsignedLong;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ExecutorService;

public class StreamSenderAggregatorTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Mock
  private ExecutorService perPacketExecutorServiceMock;
  @Mock
  private StreamConnection streamConnectionMock;
  @Mock
  private CongestionController congestionControllerMock;
  @Mock
  private StreamEncryptionUtils streamEncryptionUtilsMock;
  @Mock
  private PaymentTracker paymentTrackerMock;
  @Mock
  private ExchangeRateService exchangeRateServiceMock;
  @Mock
  private Link linkMock;

  private StreamSenderAggregator streamSenderAggregator;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  //////////////////////////
  // Compute Min Dest Amount
  //////////////////////////

  /**
   * Due to mocking, this will only return the first "run" of this send-money, which will only send 10 units out of the
   * total 50 that should be sent.
   */
  @Test
  public void computePrepareAmountsWithNoFX() {
    final UnsignedLong senderAmount = UnsignedLong.valueOf(50L);
    final BigDecimal scaledExchangeRate = BigDecimal.ZERO;
    when(exchangeRateServiceMock.getScaledExchangeRate(any(), any(), any())).thenReturn(scaledExchangeRate);
    when(paymentTrackerMock.getOriginalAmountLeft()).thenReturn(senderAmount);
    when(congestionControllerMock.getAmountLeftInWindow()).thenReturn(senderAmount);
    when(exchangeRateServiceMock.convert(eq(senderAmount), eq(scaledExchangeRate))).thenReturn(
        UnsignedLong
            .valueOf(new BigDecimal(senderAmount.bigIntegerValue()).multiply(scaledExchangeRate).toBigIntegerExact())
    );

    final SendMoneyRequest sendMoneyRequest = sendMoneyRequestForTesting()
        .senderAmount(senderAmount)
        .build();
    this.streamSenderAggregator = new StreamSenderAggregator(
        perPacketExecutorServiceMock, streamConnectionMock, congestionControllerMock, streamEncryptionUtilsMock,
        paymentTrackerMock, exchangeRateServiceMock, linkMock, sendMoneyRequest
    );
    PrepareAmounts preparedAmounts = streamSenderAggregator.computePrepareAmounts();
    assertThat(preparedAmounts.amountToSend()).isEqualTo(UnsignedLong.valueOf(50L));
    assertThat(preparedAmounts.minimumAmountToAccept()).isEqualTo(UnsignedLong.valueOf(0L));
  }

  /**
   * Due to mocking, this will only return the first "run" of this send-money, which will only send 10 units out of the
   * total 50 that should be sent.
   */
  @Test
  public void computePrepareAmountsWithFXEqual1() {
    final UnsignedLong senderAmount = UnsignedLong.valueOf(50L);
    final BigDecimal scaledExchangeRate = new BigDecimal("1.0").divide(new BigDecimal("1.0"));
    when(exchangeRateServiceMock.getScaledExchangeRate(any(), any(), any())).thenReturn(scaledExchangeRate);
    when(paymentTrackerMock.getOriginalAmountLeft()).thenReturn(senderAmount);
    when(congestionControllerMock.getAmountLeftInWindow()).thenReturn(senderAmount);
    when(exchangeRateServiceMock.convert(eq(senderAmount), eq(scaledExchangeRate))).thenReturn(
        UnsignedLong
            .valueOf(new BigDecimal(senderAmount.bigIntegerValue()).multiply(scaledExchangeRate).toBigIntegerExact())
    );

    final SendMoneyRequest sendMoneyRequest = sendMoneyRequestForTesting()
        .senderAmount(senderAmount)
        .build();
    this.streamSenderAggregator = new StreamSenderAggregator(
        perPacketExecutorServiceMock, streamConnectionMock, congestionControllerMock, streamEncryptionUtilsMock,
        paymentTrackerMock, exchangeRateServiceMock, linkMock, sendMoneyRequest
    );
    PrepareAmounts preparedAmounts = streamSenderAggregator.computePrepareAmounts();
    assertThat(preparedAmounts.amountToSend()).isEqualTo(UnsignedLong.valueOf(50L));
    assertThat(preparedAmounts.minimumAmountToAccept()).isEqualTo(UnsignedLong.valueOf(50L));
  }

  /**
   * Due to mocking, this will only return the first "run" of this send-money, which will only send 10 units out of the
   * total 50 that should be sent.
   */
  @Test
  public void computePrepareAmountsWithFXEqual2to1WithNoSlippage() {
    final UnsignedLong senderAmount = UnsignedLong.valueOf(50L);
    final BigDecimal scaledExchangeRate = new BigDecimal("2.0").divide(new BigDecimal("1.0"));
    when(exchangeRateServiceMock.getScaledExchangeRate(any(), any(), any())).thenReturn(scaledExchangeRate);
    when(paymentTrackerMock.getOriginalAmountLeft()).thenReturn(senderAmount);
    when(congestionControllerMock.getAmountLeftInWindow()).thenReturn(senderAmount);
    when(exchangeRateServiceMock.convert(eq(senderAmount), eq(scaledExchangeRate))).thenReturn(
        UnsignedLong
            .valueOf(new BigDecimal(senderAmount.bigIntegerValue()).multiply(scaledExchangeRate).toBigIntegerExact())
    );

    final SendMoneyRequest sendMoneyRequest = sendMoneyRequestForTesting()
        .senderAmount(senderAmount)
        .build();
    this.streamSenderAggregator = new StreamSenderAggregator(
        perPacketExecutorServiceMock, streamConnectionMock, congestionControllerMock, streamEncryptionUtilsMock,
        paymentTrackerMock, exchangeRateServiceMock, linkMock, sendMoneyRequest
    );
    PrepareAmounts preparedAmounts = streamSenderAggregator.computePrepareAmounts();
    assertThat(preparedAmounts.amountToSend()).isEqualTo(UnsignedLong.valueOf(50L));
    assertThat(preparedAmounts.minimumAmountToAccept()).isEqualTo(UnsignedLong.valueOf(100L));
  }

  /**
   * Due to mocking, this will only return the first "run" of this send-money, which will only send 10 units out of the
   * total 50 that should be sent.
   */
  @Test
  public void computePrepareAmountsWithFXEqual1to2WithNoSlippage() {
    final UnsignedLong senderAmount = UnsignedLong.valueOf(50L);
    final BigDecimal scaledExchangeRate = new BigDecimal("1.0").divide(new BigDecimal("2.0"));
    when(exchangeRateServiceMock.getScaledExchangeRate(any(), any(), any())).thenReturn(scaledExchangeRate);
    when(paymentTrackerMock.getOriginalAmountLeft()).thenReturn(senderAmount);
    when(congestionControllerMock.getAmountLeftInWindow()).thenReturn(senderAmount);
    when(exchangeRateServiceMock.convert(eq(senderAmount), eq(scaledExchangeRate))).thenReturn(
        UnsignedLong
            .valueOf(new BigDecimal(senderAmount.bigIntegerValue()).multiply(scaledExchangeRate).toBigIntegerExact())
    );

    final SendMoneyRequest sendMoneyRequest = sendMoneyRequestForTesting()
        .senderAmount(senderAmount)
        .build();
    this.streamSenderAggregator = new StreamSenderAggregator(
        perPacketExecutorServiceMock, streamConnectionMock, congestionControllerMock, streamEncryptionUtilsMock,
        paymentTrackerMock, exchangeRateServiceMock, linkMock, sendMoneyRequest
    );
    PrepareAmounts preparedAmounts = streamSenderAggregator.computePrepareAmounts();
    assertThat(preparedAmounts.amountToSend()).isEqualTo(UnsignedLong.valueOf(50L));
    assertThat(preparedAmounts.minimumAmountToAccept()).isEqualTo(UnsignedLong.valueOf(25L));
  }

  /**
   * Due to mocking, this will only return the first "run" of this send-money, which will only send 10 units out of the
   * total 50 that should be sent.
   */
  @Test
  public void computePrepareAmountsWithFXEqual2to1WithSlippage() {
    final UnsignedLong senderAmount = UnsignedLong.valueOf(50L);
    // Simulate 10% slippage.
    final BigDecimal scaledExchangeRate = new BigDecimal("2.0").divide(new BigDecimal("1.1"), 20, RoundingMode.HALF_UP);
    when(exchangeRateServiceMock.getScaledExchangeRate(any(), any(), any())).thenReturn(scaledExchangeRate);
    when(paymentTrackerMock.getOriginalAmountLeft()).thenReturn(senderAmount);
    when(congestionControllerMock.getAmountLeftInWindow()).thenReturn(senderAmount);
    when(exchangeRateServiceMock.convert(eq(senderAmount), eq(scaledExchangeRate))).thenReturn(
        UnsignedLong.valueOf(new BigDecimal(senderAmount.bigIntegerValue()).multiply(scaledExchangeRate).toBigInteger())
    );

    final SendMoneyRequest sendMoneyRequest = sendMoneyRequestForTesting()
        .senderAmount(senderAmount)
        .build();
    this.streamSenderAggregator = new StreamSenderAggregator(
        perPacketExecutorServiceMock, streamConnectionMock, congestionControllerMock, streamEncryptionUtilsMock,
        paymentTrackerMock, exchangeRateServiceMock, linkMock, sendMoneyRequest
    );
    PrepareAmounts preparedAmounts = streamSenderAggregator.computePrepareAmounts();
    assertThat(preparedAmounts.amountToSend()).isEqualTo(UnsignedLong.valueOf(50L));
    // Normally, the min acceptable amount would be 100 since the FX rate is 2:1. However, we allow slippage of 10%, so
    // we tolerate a bit less as the min-receive amount.
    assertThat(preparedAmounts.minimumAmountToAccept()).isEqualTo(UnsignedLong.valueOf(90L));
  }

  //////////////////
  // Private Helpers
  //////////////////

  private SendMoneyRequestBuilder sendMoneyRequestForTesting() {
    return SendMoneyRequest.builder()
        .senderAmount(UnsignedLong.ONE)
        .senderDenomination(Denomination.builder().assetScale((short) 2).assetCode("ABC").build())
        .senderAddress(InterledgerAddress.of("example.sender"))
        .receiverDenomination(Denomination.builder().assetScale((short) 2).assetCode("XYZ").build())
        .destinationAddress(InterledgerAddress.of("example.receiver"))
        .maxSlippagePercent(BigDecimal.ZERO)
        .paymentTimeout(Duration.of(1, ChronoUnit.MINUTES))
        .sharedSecret(SharedSecret.of(new byte[32]));
  }
}
