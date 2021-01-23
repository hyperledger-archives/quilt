package org.interledger.stream.receiver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.interledger.codecs.stream.StreamCodecContextFactory;
import org.interledger.core.Immutable;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.fluent.Percentage;
import org.interledger.fx.DefaultOracleExchangeRateService;
import org.interledger.fx.Denomination;
import org.interledger.link.Link;
import org.interledger.spsp.PaymentPointer;
import org.interledger.spsp.StreamConnectionDetails;
import org.interledger.spsp.client.InvalidReceiverClientException;
import org.interledger.spsp.client.SpspClient;
import org.interledger.stream.crypto.AesGcmStreamSharedSecretCrypto;
import org.interledger.stream.crypto.StreamPacketEncryptionService;
import org.interledger.stream.model.AccountDetails;
import org.interledger.stream.pay.StreamPayer;
import org.interledger.stream.pay.model.PaymentOptions;
import org.interledger.stream.pay.model.PaymentReceipt;
import org.interledger.stream.pay.model.SendState;
import org.interledger.stream.receiver.testutils.SimulatedIlpv4Network;
import org.interledger.stream.receiver.testutils.SimulatedPathConditions;

import ch.qos.logback.classic.Level;
import com.google.common.collect.Lists;
import com.google.common.io.BaseEncoding;
import com.google.common.primitives.UnsignedLong;
import okhttp3.HttpUrl;
import org.immutables.value.Value.Derived;
import org.javamoney.moneta.spi.DefaultNumberValue;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.money.CurrencyUnit;
import javax.money.convert.ExchangeRate;
import javax.money.convert.ExchangeRateProvider;

/**
 * A unit tests that simulates network connectivity between a sender and a receiver in order to isolate and control
 * various network conditions as part of a STREAM payment. This test uses two nodes that have the same Denomination
 * settings, and validates the sender/receiver using a {@link StreamPayer} and a {@link StatelessStreamReceiver}.
 */
@SuppressWarnings( {"OptionalGetWithoutIsPresent", "deprecation"})
public class SenderReceiverWithStreamPayerTest {

  private static final String SHARED_SECRET_HEX = "9DCE76B1A20EC8D3DB05AD579F3293402743767692F935A0BF06B30D2728439D";

  private static final InterledgerAddress LEFT_ILP_ADDRESS = InterledgerAddress.of("example.quilt-dev.left");
  private static final InterledgerAddress LEFT_SENDER_ADDRESS = LEFT_ILP_ADDRESS.with("left_stream_sender");
  private static final InterledgerAddress LEFT_RECEIVER_ADDRESS = LEFT_ILP_ADDRESS.with("left_stream_receiver");

  private static final InterledgerAddress RIGHT_ILP_ADDRESS = InterledgerAddress.of("example.quilt-dev.right");
  private static final InterledgerAddress RIGHT_SENDER_ADDRESS = RIGHT_ILP_ADDRESS.with("right_stream_sender");
  private static final InterledgerAddress RIGHT_RECEIVER_ADDRESS = RIGHT_ILP_ADDRESS.with("right_stream_receiver");

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private StreamPayerNode leftStreamPayerNode;
  private StreamPayerNode rightStreamPayerNode;

  private SimulatedIlpv4Network simulatedIlpNetwork;

  private Level previousLogValue;

  /**
   * Turn-down the logger because it's causing issues with CircleCI.
   */
  @Before
  public void setUp() {
    final ch.qos.logback.classic.Logger logger
      = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
    this.previousLogValue = logger.getLevel();
    logger.setLevel(Level.INFO);
    this.initIlpNetworkForStream();
  }

  @After
  public void tearDown() {
    final ch.qos.logback.classic.Logger logger
      = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
    logger.setLevel(previousLogValue);
  }

  ////////////////
  // Left to Right
  ////////////////

  @Test
  public void sendFromLeftToRight() {
    // PAY
    final BigDecimal paymentAmount = BigDecimal.valueOf(1000);
    final PaymentReceipt paymentReceipt = pay(leftStreamPayerNode, rightStreamPayerNode, paymentAmount);

    assertThat(paymentReceipt.successfulPayment()).isTrue();
    assertThat(paymentReceipt.amountSentInSendersUnits()).isEqualTo(BigInteger.valueOf(1000000000));
    assertThat(paymentReceipt.amountDeliveredInDestinationUnits()).isEqualTo(BigInteger.valueOf(1000000000));
    assertThat(paymentReceipt.amountLeftToSendInSendersUnits()).isEqualTo(BigInteger.ZERO);
    assertThat(paymentReceipt.paymentStatistics().numFulfilledPackets()).isEqualTo(1);
    assertThat(paymentReceipt.paymentStatistics().numRejectPackets()).isZero();
    assertThat(paymentReceipt.paymentStatistics().numTotalPackets()).isEqualTo(1);
    assertThat(paymentReceipt.paymentStatistics().packetFailurePercentage()).isEqualTo(Percentage.ZERO_PERCENT);
    assertThat(paymentReceipt.paymentStatistics().lowerBoundExchangeRate().toBigDecimal())
      .isEqualTo(new BigDecimal("1"));
    assertThat(paymentReceipt.paymentStatistics().upperBoundExchangeRate().toBigDecimal())
      .isEqualTo(new BigDecimal("1.000000001"));
    assertThat(paymentReceipt.paymentStatistics().paymentDuration()).isGreaterThan(Duration.ZERO);

    logger.info("PaymentReceipt: {}", paymentReceipt);
  }

  @Test
  public void sendFromLeftToRightWithSmallerMaxPacket() {
    this.initIlpNetworkForStream(new SimulatedIlpv4Network(
      SimulatedPathConditions.builder()
        .maxPacketAmount(() -> UnsignedLong.valueOf(20000L))
        .build(),
      SimulatedPathConditions.builder().build()
    ));

    // PAY
    final BigDecimal paymentAmount = BigDecimal.valueOf(0.9); // <-- Send 1 XRP
    final PaymentReceipt paymentReceipt = pay(leftStreamPayerNode, rightStreamPayerNode, paymentAmount);

    assertThat(paymentReceipt.successfulPayment()).isTrue();
    assertThat(paymentReceipt.amountSentInSendersUnits()).isEqualTo(BigInteger.valueOf(900000));
    assertThat(paymentReceipt.amountDeliveredInDestinationUnits()).isEqualTo(BigInteger.valueOf(900000));
    assertThat(paymentReceipt.amountLeftToSendInSendersUnits()).isEqualTo(BigInteger.ZERO);
    assertThat(paymentReceipt.paymentStatistics().numFulfilledPackets()).isEqualTo(45);
    assertThat(paymentReceipt.paymentStatistics().numRejectPackets()).isPositive(); // Should be _some_ reject's
    assertThat(paymentReceipt.paymentStatistics().numTotalPackets()).isGreaterThan(45);
    assertThat(paymentReceipt.paymentStatistics().packetFailurePercentage()).isBetween(
      Percentage.ONE_PERCENT, Percentage.FIVE_PERCENT
    );
    assertThat(paymentReceipt.paymentStatistics().lowerBoundExchangeRate().toBigDecimal())
      .isEqualTo(new BigDecimal("1"));
    assertThat(paymentReceipt.paymentStatistics().upperBoundExchangeRate().toBigDecimal())
      .isEqualTo(new BigDecimal("1.00005"));
    assertThat(paymentReceipt.paymentStatistics().paymentDuration()).isGreaterThan(Duration.ZERO);

    logger.info("PaymentReceipt: {}", paymentReceipt);
  }

  @Test
  public void sendFromLeftToRightWithBetterRateThanAllowed() {
    // Note that the actual FX rate is 1.0, so simulating a better exchange rate than we expected in which case
    // payments should get fulfilled because the receiver gets more than they were supposed to.
    BigDecimal actualPathExchange = new BigDecimal("1.5");
    this.initIlpNetworkForStream(new SimulatedIlpv4Network(
      SimulatedPathConditions.builder()
        .currentExchangeRateSupplier(() -> actualPathExchange)
        .maxPacketAmount(() -> UnsignedLong.valueOf(20000L))
        .build(),
      SimulatedPathConditions.builder().build()
    ));

    // PAY
    final BigDecimal paymentAmount = BigDecimal.valueOf(1); // <-- Send 1 XRP
    final PaymentReceipt paymentReceipt = pay(leftStreamPayerNode, rightStreamPayerNode, paymentAmount);

    assertThat(paymentReceipt.successfulPayment()).isTrue();
    assertThat(paymentReceipt.amountSentInSendersUnits()).isEqualTo(BigInteger.valueOf(1000000));
    assertThat(paymentReceipt.amountDeliveredInDestinationUnits()).isEqualTo(BigInteger.valueOf(1500000));
    assertThat(paymentReceipt.amountLeftToSendInSendersUnits()).isEqualTo(BigInteger.ZERO);
    assertThat(paymentReceipt.paymentStatistics().numFulfilledPackets()).isEqualTo(50);
    assertThat(paymentReceipt.paymentStatistics().numRejectPackets()).isPositive(); // <-- Expect some rejections.
    assertThat(paymentReceipt.paymentStatistics().numTotalPackets()).isGreaterThan(50);
    assertThat(paymentReceipt.paymentStatistics().packetFailurePercentage()).isGreaterThan(Percentage.ONE_PERCENT);
    assertThat(paymentReceipt.paymentStatistics().packetFailurePercentage()).isLessThan(Percentage.FIVE_PERCENT);
    assertThat(paymentReceipt.paymentStatistics().lowerBoundExchangeRate().toBigDecimal())
      .isEqualTo(new BigDecimal("1.5"));
    assertThat(paymentReceipt.paymentStatistics().upperBoundExchangeRate().toBigDecimal())
      .isEqualTo(new BigDecimal("1.50005"));
    assertThat(paymentReceipt.paymentStatistics().paymentDuration()).isGreaterThan(Duration.ZERO);

    logger.info("PaymentReceipt: {}", paymentReceipt);
  }

  @Test
  public void sendFromLeftToRightWithWorseRateThanAllowed() {
    // Note that the actual FX rate is 1.0, so simulating a worse exchange rate than we expected in which case
    // payments should get fulfilled because the receiver gets more than they were supposed to.
    BigDecimal actualPathExchange = new BigDecimal("0.5");
    this.initIlpNetworkForStream(new SimulatedIlpv4Network(
      SimulatedPathConditions.builder()
        .currentExchangeRateSupplier(() -> actualPathExchange)
        .maxPacketAmount(() -> UnsignedLong.valueOf(20000L))
        .build(),
      SimulatedPathConditions.builder().build()
    ));

    // PAY
    final BigDecimal paymentAmount = BigDecimal.valueOf(1); // <-- Send 1 XRP
    final PaymentReceipt paymentReceipt = pay(leftStreamPayerNode, rightStreamPayerNode, paymentAmount);

    assertThat(paymentReceipt.successfulPayment()).isFalse();
    assertThat(paymentReceipt.amountSentInSendersUnits()).isEqualTo(BigInteger.ZERO);
    assertThat(paymentReceipt.amountDeliveredInDestinationUnits()).isEqualTo(BigInteger.ZERO);
    assertThat(paymentReceipt.amountLeftToSendInSendersUnits()).isEqualTo(BigInteger.valueOf(1000000L));
    assertThat(paymentReceipt.paymentStatistics().numFulfilledPackets()).isEqualTo(0);
    assertThat(paymentReceipt.paymentStatistics().numRejectPackets()).isGreaterThan(1); // <-- Expect >1 rejections.
    assertThat(paymentReceipt.paymentStatistics().numTotalPackets()).isGreaterThan(0);
    assertThat(paymentReceipt.paymentStatistics().packetFailurePercentage())
      .isEqualByComparingTo(Percentage.ONE_HUNDRED_PERCENT);
    assertThat(paymentReceipt.paymentStatistics().lowerBoundExchangeRate().toBigDecimal())
      .isEqualTo(new BigDecimal("1"));
    assertThat(paymentReceipt.paymentStatistics().upperBoundExchangeRate().toBigDecimal())
      .isEqualTo(new BigDecimal("0.50005"));
    assertThat(paymentReceipt.paymentStatistics().paymentDuration()).isGreaterThan(Duration.ZERO);

    assertThat(paymentReceipt.paymentError()).isPresent();
    assertThat(paymentReceipt.paymentError().get().getSendState()).isEqualTo(SendState.InsufficientExchangeRate);
    assertThat(paymentReceipt.paymentError().get().getMessage())
      .contains("Payment cannot complete because exchange rate dropped below minimum");

    logger.info("PaymentReceipt: {}", paymentReceipt);
  }

  /**
   * Simulate a left-to-right STREAM with 20% packet loss due to T03 errors.
   */
  @Test
  public void sendFromLeftToRightWith20PercentLoss() {
    final Percentage rejectionPercentage = Percentage.of(new BigDecimal("0.2"));
    this.initIlpNetworkForStream(new SimulatedIlpv4Network(
      SimulatedPathConditions.builder()
        .packetRejectionPercentage(rejectionPercentage.value().floatValue())
        .maxPacketAmount(() -> UnsignedLong.valueOf(20000L))
        .build(),
      SimulatedPathConditions.builder().build()
    ));

    // PAY
    final BigDecimal paymentAmount = BigDecimal.valueOf(1); // <-- Send 1 XRP
    final PaymentReceipt paymentReceipt = pay(leftStreamPayerNode, rightStreamPayerNode, paymentAmount);

    assertThat(paymentReceipt.successfulPayment()).isTrue();
    assertThat(paymentReceipt.amountSentInSendersUnits()).isEqualTo(BigInteger.valueOf(1000000));
    assertThat(paymentReceipt.amountDeliveredInDestinationUnits()).isEqualTo(BigInteger.valueOf(1000000));
    assertThat(paymentReceipt.amountLeftToSendInSendersUnits()).isEqualTo(BigInteger.ZERO);
    assertThat(paymentReceipt.paymentStatistics().numFulfilledPackets()).isEqualTo(50);
    assertThat(paymentReceipt.paymentStatistics().numRejectPackets()).isPositive(); // Should be _some_ rejects.
    assertThat(paymentReceipt.paymentStatistics().numTotalPackets()).isGreaterThan(50);
    // Should be somewhere around 20%.
    assertThat(paymentReceipt.paymentStatistics().packetFailurePercentage()).isBetween(
      Percentage.of(new BigDecimal("0.10")),
      Percentage.of(new BigDecimal("0.30"))
    );
    assertThat(paymentReceipt.paymentStatistics().lowerBoundExchangeRate().toBigDecimal())
      .isEqualTo(new BigDecimal("1"));
    assertThat(paymentReceipt.paymentStatistics().upperBoundExchangeRate().toBigDecimal())
      .isEqualTo(new BigDecimal("1.00005"));
    assertThat(paymentReceipt.paymentStatistics().paymentDuration()).isGreaterThan(Duration.ZERO);

    logger.info("PaymentReceipt: {}", paymentReceipt);
  }

  /**
   * Simulate a left-to-right STREAM with 50% packet loss due to T03 errors.
   */
  @Test
  public void sendFromLeftToRightWith50PercentLoss() {
    final Percentage rejectionPercentage = Percentage.of(new BigDecimal("0.5"));
    this.initIlpNetworkForStream(new SimulatedIlpv4Network(
      SimulatedPathConditions.builder()
        .packetRejectionPercentage(rejectionPercentage.value().floatValue())
        .maxPacketAmount(() -> UnsignedLong.valueOf(20000L))
        .build(),
      SimulatedPathConditions.builder().build()
    ));

    // PAY
    final BigDecimal paymentAmount = BigDecimal.valueOf(1); // <-- Send 1 XRP
    final PaymentReceipt paymentReceipt = pay(leftStreamPayerNode, rightStreamPayerNode, paymentAmount);

    assertThat(paymentReceipt.successfulPayment()).isTrue();
    assertThat(paymentReceipt.amountSentInSendersUnits()).isEqualTo(BigInteger.valueOf(1000000));
    assertThat(paymentReceipt.amountDeliveredInDestinationUnits()).isEqualTo(BigInteger.valueOf(1000000));
    assertThat(paymentReceipt.amountLeftToSendInSendersUnits()).isEqualTo(BigInteger.ZERO);
    assertThat(paymentReceipt.paymentStatistics().numFulfilledPackets()).isEqualTo(50);
    assertThat(paymentReceipt.paymentStatistics().numRejectPackets()).isGreaterThan(40); // <-- Expect >40 rejections.
    assertThat(paymentReceipt.paymentStatistics().numTotalPackets()).isGreaterThan(90);
    // Should be somewhere around 20%.
    assertThat(paymentReceipt.paymentStatistics().packetFailurePercentage()).isBetween(
      Percentage.of(new BigDecimal("0.40")),
      Percentage.of(new BigDecimal("0.60"))
    );
    assertThat(paymentReceipt.paymentStatistics().lowerBoundExchangeRate().toBigDecimal())
      .isEqualTo(new BigDecimal("1"));
    assertThat(paymentReceipt.paymentStatistics().upperBoundExchangeRate().toBigDecimal())
      .isEqualTo(new BigDecimal("1.00005"));
    assertThat(paymentReceipt.paymentStatistics().paymentDuration()).isGreaterThan(Duration.ZERO);

    logger.info("PaymentReceipt: {}", paymentReceipt);
  }

  /**
   * Simulate a left-to-right STREAM with 90% packet loss due to T03 errors.
   */
  @Test
  public void sendFromLeftToRightWith90PercentLoss() {
    final Percentage rejectionPercentage = Percentage.of(new BigDecimal("0.9"));
    this.initIlpNetworkForStream(new SimulatedIlpv4Network(
      SimulatedPathConditions.builder()
        .packetRejectionPercentage(rejectionPercentage.value().floatValue())
        .maxPacketAmount(() -> UnsignedLong.valueOf(20000L))
        .build(),
      SimulatedPathConditions.builder().build()
    ));

    // PAY
    final BigDecimal paymentAmount = BigDecimal.valueOf(0.1); // <-- Send 1 XRP
    final PaymentReceipt paymentReceipt = pay(leftStreamPayerNode, rightStreamPayerNode, paymentAmount);

    assertThat(paymentReceipt.successfulPayment()).isTrue();
    assertThat(paymentReceipt.amountSentInSendersUnits()).isEqualTo(BigInteger.valueOf(100000));
    assertThat(paymentReceipt.amountDeliveredInDestinationUnits()).isEqualTo(BigInteger.valueOf(100000));
    assertThat(paymentReceipt.amountLeftToSendInSendersUnits()).isEqualTo(BigInteger.ZERO);
    assertThat(paymentReceipt.paymentStatistics().numFulfilledPackets()).isEqualTo(5);
    assertThat(paymentReceipt.paymentStatistics().numRejectPackets()).isPositive(); // Should be _some_ rejects.
    assertThat(paymentReceipt.paymentStatistics().numTotalPackets()).isGreaterThan(5);
    // Should be somewhere around 20%.
    assertThat(paymentReceipt.paymentStatistics().packetFailurePercentage()).isBetween(
      Percentage.of(new BigDecimal("0.80")),
      Percentage.of(new BigDecimal("0.99"))
    );
    assertThat(paymentReceipt.paymentStatistics().lowerBoundExchangeRate().toBigDecimal())
      .isEqualTo(new BigDecimal("1"));
    assertThat(paymentReceipt.paymentStatistics().upperBoundExchangeRate().toBigDecimal())
      .isEqualTo(new BigDecimal("1.00005"));
    assertThat(paymentReceipt.paymentStatistics().paymentDuration()).isGreaterThan(Duration.ZERO);

    logger.info("PaymentReceipt: {}", paymentReceipt);
  }

  ////////////////
  // Right to Left
  ////////////////

  @Test
  public void sendFromRightToLeft() {
    // PAY
    final BigDecimal paymentAmount = BigDecimal.valueOf(1000);
    final PaymentReceipt paymentReceipt = pay(rightStreamPayerNode, leftStreamPayerNode, paymentAmount);

    assertThat(paymentReceipt.successfulPayment()).isTrue();
    assertThat(paymentReceipt.amountSentInSendersUnits()).isEqualTo(BigInteger.valueOf(1000000000));
    assertThat(paymentReceipt.amountDeliveredInDestinationUnits()).isEqualTo(BigInteger.valueOf(1000000000));
    assertThat(paymentReceipt.amountLeftToSendInSendersUnits()).isEqualTo(BigInteger.ZERO);
    assertThat(paymentReceipt.paymentStatistics().numFulfilledPackets()).isEqualTo(1);
    assertThat(paymentReceipt.paymentStatistics().numRejectPackets()).isZero();
    assertThat(paymentReceipt.paymentStatistics().numTotalPackets()).isEqualTo(1);
    assertThat(paymentReceipt.paymentStatistics().packetFailurePercentage()).isEqualTo(Percentage.ZERO_PERCENT);
    assertThat(paymentReceipt.paymentStatistics().lowerBoundExchangeRate().toBigDecimal())
      .isEqualTo(new BigDecimal("1"));
    assertThat(paymentReceipt.paymentStatistics().upperBoundExchangeRate().toBigDecimal())
      .isEqualTo(new BigDecimal("1.000000001"));
    assertThat(paymentReceipt.paymentStatistics().paymentDuration()).isGreaterThan(Duration.ZERO);

    logger.info("PaymentReceipt: {}", paymentReceipt);
  }

  @Test
  public void sendFromRightToLeftWithSmallerMaxPacket() {
    this.initIlpNetworkForStream(new SimulatedIlpv4Network(
      SimulatedPathConditions.builder().build(),
      SimulatedPathConditions.builder()
        .maxPacketAmount(() -> UnsignedLong.valueOf(20000L))
        .build()
    ));

    // PAY
    final BigDecimal paymentAmount = BigDecimal.valueOf(1);
    final PaymentReceipt paymentReceipt = pay(rightStreamPayerNode, leftStreamPayerNode, paymentAmount);

    assertThat(paymentReceipt.successfulPayment()).isTrue();
    assertThat(paymentReceipt.amountSentInSendersUnits()).isEqualTo(BigInteger.valueOf(1000000));
    assertThat(paymentReceipt.amountDeliveredInDestinationUnits()).isEqualTo(BigInteger.valueOf(1000000));
    assertThat(paymentReceipt.amountLeftToSendInSendersUnits()).isEqualTo(BigInteger.ZERO);
    assertThat(paymentReceipt.paymentStatistics().numFulfilledPackets()).isEqualTo(50);
    assertThat(paymentReceipt.paymentStatistics().numRejectPackets()).isEqualTo(1);
    assertThat(paymentReceipt.paymentStatistics().numTotalPackets()).isEqualTo(51);
    assertThat(paymentReceipt.paymentStatistics().packetFailurePercentage()).isBetween(
      Percentage.ONE_PERCENT, Percentage.FIVE_PERCENT
    );
    assertThat(paymentReceipt.paymentStatistics().lowerBoundExchangeRate().toBigDecimal())
      .isEqualTo(new BigDecimal("1"));
    assertThat(paymentReceipt.paymentStatistics().upperBoundExchangeRate().toBigDecimal())
      .isEqualTo(new BigDecimal("1.00005"));
    assertThat(paymentReceipt.paymentStatistics().paymentDuration()).isGreaterThan(Duration.ZERO);

    logger.info("PaymentReceipt: {}", paymentReceipt);
  }

  @Test
  public void sendFromRightToLeftWithBetterRateThanAllowed() {
    // Note that the actual FX rate is 1.0, so simulating a better exchange rate than we expected in which case
    // payments should get fulfilled because the receiver gets more than they were supposed to.
    BigDecimal actualPathExchange = new BigDecimal("1.5");
    this.initIlpNetworkForStream(new SimulatedIlpv4Network(
      SimulatedPathConditions.builder().build(),
      SimulatedPathConditions.builder()
        .currentExchangeRateSupplier(() -> actualPathExchange)
        .maxPacketAmount(() -> UnsignedLong.valueOf(20000L))
        .build()
    ));

    // PAY
    final BigDecimal paymentAmount = BigDecimal.valueOf(1); // <-- Send 1 XRP
    final PaymentReceipt paymentReceipt = pay(rightStreamPayerNode, leftStreamPayerNode, paymentAmount);

    assertThat(paymentReceipt.successfulPayment()).isTrue();
    assertThat(paymentReceipt.amountSentInSendersUnits()).isEqualTo(BigInteger.valueOf(1000000));
    assertThat(paymentReceipt.amountDeliveredInDestinationUnits()).isEqualTo(BigInteger.valueOf(1500000));
    assertThat(paymentReceipt.amountLeftToSendInSendersUnits()).isEqualTo(BigInteger.ZERO);
    assertThat(paymentReceipt.paymentStatistics().numFulfilledPackets()).isEqualTo(50);
    assertThat(paymentReceipt.paymentStatistics().numRejectPackets()).isPositive(); // <-- Expect some rejections.
    assertThat(paymentReceipt.paymentStatistics().numTotalPackets()).isGreaterThan(50);
    assertThat(paymentReceipt.paymentStatistics().packetFailurePercentage()).isGreaterThan(Percentage.ONE_PERCENT);
    assertThat(paymentReceipt.paymentStatistics().packetFailurePercentage()).isLessThan(Percentage.FIVE_PERCENT);
    assertThat(paymentReceipt.paymentStatistics().lowerBoundExchangeRate().toBigDecimal())
      .isEqualTo(new BigDecimal("1.5"));
    assertThat(paymentReceipt.paymentStatistics().upperBoundExchangeRate().toBigDecimal())
      .isEqualTo(new BigDecimal("1.50005"));
    assertThat(paymentReceipt.paymentStatistics().paymentDuration()).isGreaterThan(Duration.ZERO);

    logger.info("PaymentReceipt: {}", paymentReceipt);
  }

  @Test
  public void sendFromRightToLeftWithWorseRateThanAllowed() {
    // Note that the actual FX rate is 1.0, so simulating a worse exchange rate than we expected in which case
    // payments should get fulfilled because the receiver gets more than they were supposed to.
    BigDecimal actualPathExchange = new BigDecimal("0.5");
    this.initIlpNetworkForStream(new SimulatedIlpv4Network(
      SimulatedPathConditions.builder().build(),
      SimulatedPathConditions.builder()
        .currentExchangeRateSupplier(() -> actualPathExchange)
        .maxPacketAmount(() -> UnsignedLong.valueOf(20000L))
        .build()
    ));

    // PAY
    final BigDecimal paymentAmount = BigDecimal.valueOf(1); // <-- Send 1 XRP
    final PaymentReceipt paymentReceipt = pay(rightStreamPayerNode, leftStreamPayerNode, paymentAmount);

    assertThat(paymentReceipt.successfulPayment()).isFalse();
    assertThat(paymentReceipt.amountSentInSendersUnits()).isEqualTo(BigInteger.ZERO);
    assertThat(paymentReceipt.amountDeliveredInDestinationUnits()).isEqualTo(BigInteger.ZERO);
    assertThat(paymentReceipt.amountLeftToSendInSendersUnits()).isEqualTo(BigInteger.valueOf(1000000L));
    assertThat(paymentReceipt.paymentStatistics().numFulfilledPackets()).isEqualTo(0);
    assertThat(paymentReceipt.paymentStatistics().numRejectPackets()).isGreaterThan(1); // <-- Expect >1 rejections.
    assertThat(paymentReceipt.paymentStatistics().numTotalPackets()).isGreaterThan(1);
    assertThat(paymentReceipt.paymentStatistics().packetFailurePercentage())
      .isEqualByComparingTo(Percentage.ONE_HUNDRED_PERCENT);
    assertThat(paymentReceipt.paymentStatistics().lowerBoundExchangeRate().toBigDecimal())
      .isEqualTo(new BigDecimal("1"));
    assertThat(paymentReceipt.paymentStatistics().upperBoundExchangeRate().toBigDecimal())
      .isEqualTo(new BigDecimal("0.50005"));
    assertThat(paymentReceipt.paymentStatistics().paymentDuration()).isGreaterThan(Duration.ZERO);

    assertThat(paymentReceipt.paymentError()).isPresent();
    assertThat(paymentReceipt.paymentError().get().getSendState()).isEqualTo(SendState.InsufficientExchangeRate);
    assertThat(paymentReceipt.paymentError().get().getMessage())
      .contains("Payment cannot complete because exchange rate dropped below minimum");

    logger.info("PaymentReceipt: {}", paymentReceipt);
  }

  /**
   * Simulate a right-to-left STREAM with 20% packet loss due to T03 errors.
   */
  @Test
  public void sendFromRightToLeftWith20PercentLoss() {
    final Percentage rejectionPercentage = Percentage.of(new BigDecimal("0.2"));
    this.initIlpNetworkForStream(new SimulatedIlpv4Network(
      SimulatedPathConditions.builder().build(),
      SimulatedPathConditions.builder()
        .packetRejectionPercentage(rejectionPercentage.value().floatValue())
        .maxPacketAmount(() -> UnsignedLong.valueOf(20000L))
        .build()
    ));

    // PAY
    final BigDecimal paymentAmount = BigDecimal.valueOf(1); // <-- Send 1 XRP
    final PaymentReceipt paymentReceipt = pay(rightStreamPayerNode, leftStreamPayerNode, paymentAmount);

    assertThat(paymentReceipt.successfulPayment()).isTrue();
    assertThat(paymentReceipt.amountSentInSendersUnits()).isEqualTo(BigInteger.valueOf(1000000));
    assertThat(paymentReceipt.amountDeliveredInDestinationUnits()).isEqualTo(BigInteger.valueOf(1000000));
    assertThat(paymentReceipt.amountLeftToSendInSendersUnits()).isEqualTo(BigInteger.ZERO);
    assertThat(paymentReceipt.paymentStatistics().numFulfilledPackets()).isEqualTo(50);
    assertThat(paymentReceipt.paymentStatistics().numRejectPackets()).isPositive(); // Should be _some_ rejects.
    assertThat(paymentReceipt.paymentStatistics().numTotalPackets()).isGreaterThan(50);
    // Should be somewhere around 20%.
    assertThat(paymentReceipt.paymentStatistics().packetFailurePercentage()).isBetween(
      Percentage.of(new BigDecimal("0.10")),
      Percentage.of(new BigDecimal("0.30"))
    );
    assertThat(paymentReceipt.paymentStatistics().lowerBoundExchangeRate().toBigDecimal())
      .isEqualTo(new BigDecimal("1"));
    assertThat(paymentReceipt.paymentStatistics().upperBoundExchangeRate().toBigDecimal())
      .isEqualTo(new BigDecimal("1.00005"));
    assertThat(paymentReceipt.paymentStatistics().paymentDuration()).isGreaterThan(Duration.ZERO);

    logger.info("PaymentReceipt: {}", paymentReceipt);
  }

  /**
   * Simulate a right-to-left STREAM with 50% packet loss due to T03 errors.
   */
  @Test
  public void sendFromRightToLeftWith50PercentLoss() {
    final Percentage rejectionPercentage = Percentage.of(new BigDecimal("0.5"));
    this.initIlpNetworkForStream(new SimulatedIlpv4Network(
      SimulatedPathConditions.builder().build(),
      SimulatedPathConditions.builder()
        .packetRejectionPercentage(rejectionPercentage.value().floatValue())
        .maxPacketAmount(() -> UnsignedLong.valueOf(20000L))
        .build()
    ));

    // PAY
    final BigDecimal paymentAmount = BigDecimal.valueOf(1); // <-- Send 1 XRP
    final PaymentReceipt paymentReceipt = pay(rightStreamPayerNode, leftStreamPayerNode, paymentAmount);

    assertThat(paymentReceipt.successfulPayment()).isTrue();
    assertThat(paymentReceipt.amountSentInSendersUnits()).isEqualTo(BigInteger.valueOf(1000000));
    assertThat(paymentReceipt.amountDeliveredInDestinationUnits()).isEqualTo(BigInteger.valueOf(1000000));
    assertThat(paymentReceipt.amountLeftToSendInSendersUnits()).isEqualTo(BigInteger.ZERO);
    assertThat(paymentReceipt.paymentStatistics().numFulfilledPackets()).isEqualTo(50);
    assertThat(paymentReceipt.paymentStatistics().numRejectPackets()).isPositive(); // <-- Expect ~>40 rejections.
    assertThat(paymentReceipt.paymentStatistics().numTotalPackets()).isGreaterThan(51);
    // Should be somewhere around 20%.
    assertThat(paymentReceipt.paymentStatistics().packetFailurePercentage()).isBetween(
      Percentage.of(new BigDecimal("0.40")),
      Percentage.of(new BigDecimal("0.60"))
    );
    assertThat(paymentReceipt.paymentStatistics().lowerBoundExchangeRate().toBigDecimal())
      .isEqualTo(new BigDecimal("1"));
    assertThat(paymentReceipt.paymentStatistics().upperBoundExchangeRate().toBigDecimal())
      .isEqualTo(new BigDecimal("1.00005"));
    assertThat(paymentReceipt.paymentStatistics().paymentDuration()).isGreaterThan(Duration.ZERO);

    logger.info("PaymentReceipt: {}", paymentReceipt);
  }

  /**
   * Simulate a right-to-left STREAM with 90% packet loss due to T03 errors.
   */
  @Test
  public void sendFromRightToLeftWith90PercentLoss() {
    final Percentage rejectionPercentage = Percentage.of(new BigDecimal("0.9"));
    this.initIlpNetworkForStream(new SimulatedIlpv4Network(
      SimulatedPathConditions.builder().build(),
      SimulatedPathConditions.builder()
        .packetRejectionPercentage(rejectionPercentage.value().floatValue())
        .maxPacketAmount(() -> UnsignedLong.valueOf(20000L))
        .build()
    ));

    // PAY
    final BigDecimal paymentAmount = BigDecimal.valueOf(0.1); // <-- Send 1 XRP
    final PaymentReceipt paymentReceipt = pay(rightStreamPayerNode, leftStreamPayerNode, paymentAmount);

    assertThat(paymentReceipt.successfulPayment()).isTrue();
    assertThat(paymentReceipt.amountSentInSendersUnits()).isEqualTo(BigInteger.valueOf(100000));
    assertThat(paymentReceipt.amountDeliveredInDestinationUnits()).isEqualTo(BigInteger.valueOf(100000));
    assertThat(paymentReceipt.amountLeftToSendInSendersUnits()).isEqualTo(BigInteger.ZERO);
    assertThat(paymentReceipt.paymentStatistics().numFulfilledPackets()).isEqualTo(5);
    assertThat(paymentReceipt.paymentStatistics().numRejectPackets()).isPositive(); // Should be _some_ rejects.
    assertThat(paymentReceipt.paymentStatistics().numTotalPackets()).isGreaterThan(5);
    // Should be somewhere around 20%.
    assertThat(paymentReceipt.paymentStatistics().packetFailurePercentage()).isBetween(
      Percentage.of(new BigDecimal("0.80")),
      Percentage.of(new BigDecimal("0.99"))
    );
    assertThat(paymentReceipt.paymentStatistics().lowerBoundExchangeRate().toBigDecimal())
      .isEqualTo(new BigDecimal("1"));
    assertThat(paymentReceipt.paymentStatistics().upperBoundExchangeRate().toBigDecimal())
      .isEqualTo(new BigDecimal("1.00005"));
    assertThat(paymentReceipt.paymentStatistics().paymentDuration()).isGreaterThan(Duration.ZERO);

    logger.info("PaymentReceipt: {}", paymentReceipt);
  }

  ////////////////
  // Both Directions
  ////////////////

  /**
   * While not multi-threaded, this test ensure that two subsequent "pay" operations do not share any state.
   */
  @Test
  public void sendBothDirections() {
    final BigDecimal paymentAmount = new BigDecimal(1); // <-- Send 1 XRP, or 1x10^6 units.
    PaymentReceipt leftToRightResult = pay(leftStreamPayerNode, rightStreamPayerNode, paymentAmount);
    PaymentReceipt rightToLeftResult = pay(rightStreamPayerNode, leftStreamPayerNode, paymentAmount);

    Lists.newArrayList(leftToRightResult, rightToLeftResult).forEach(paymentReceipt -> {
      assertThat(paymentReceipt.successfulPayment()).isTrue();
      assertThat(paymentReceipt.amountSentInSendersUnits()).isEqualTo(BigInteger.valueOf(1000000));
      assertThat(paymentReceipt.amountDeliveredInDestinationUnits()).isEqualTo(BigInteger.valueOf(1000000));
      assertThat(paymentReceipt.amountLeftToSendInSendersUnits()).isEqualTo(BigInteger.ZERO);
      assertThat(paymentReceipt.paymentStatistics().numFulfilledPackets()).isEqualTo(1);
      assertThat(paymentReceipt.paymentStatistics().numRejectPackets()).isZero();
      assertThat(paymentReceipt.paymentStatistics().numTotalPackets()).isEqualTo(1);
      assertThat(paymentReceipt.paymentStatistics().packetFailurePercentage()).isEqualTo(Percentage.ZERO_PERCENT);
      assertThat(paymentReceipt.paymentStatistics().lowerBoundExchangeRate().toBigDecimal())
        .isEqualTo(new BigDecimal("1"));
      assertThat(paymentReceipt.paymentStatistics().upperBoundExchangeRate().toBigDecimal())
        .isEqualTo(new BigDecimal("1.000001"));
      assertThat(paymentReceipt.paymentStatistics().paymentDuration()).isGreaterThan(Duration.ZERO);
    });
  }

  /**
   * Ensures that multiple left-to-right send operations (in parallel) don't conflict with each other.
   */
  @Test
  public void sendFromLeftToRightMultiThreadedSharedSender() {
    final int parallelism = 40;
    final int runCount = 100;

    this.initIlpNetworkForStream(new SimulatedIlpv4Network(
      SimulatedPathConditions.builder()
        .maxPacketAmount(() -> UnsignedLong.valueOf(50000))
        .build(),
      SimulatedPathConditions.builder().build()
    ));

    final BigDecimal paymentAmount = new BigDecimal(1); // <-- Send 1 XRP, or 1x10^6 units.
    List<CompletableFuture<PaymentReceipt>> results =
      runInParallel(parallelism, runCount, () -> pay(leftStreamPayerNode, rightStreamPayerNode, paymentAmount));

    awaitResults(results).forEach(paymentReceipt -> {
      assertThat(paymentReceipt.successfulPayment()).isTrue();
      assertThat(paymentReceipt.amountSentInSendersUnits()).isEqualTo(BigInteger.valueOf(1000000));
      assertThat(paymentReceipt.amountDeliveredInDestinationUnits()).isEqualTo(BigInteger.valueOf(1000000));
      assertThat(paymentReceipt.amountLeftToSendInSendersUnits()).isEqualTo(BigInteger.ZERO);
      assertThat(paymentReceipt.paymentStatistics().numFulfilledPackets()).isEqualTo(20);
      assertThat(paymentReceipt.paymentStatistics().numRejectPackets()).isPositive();
      assertThat(paymentReceipt.paymentStatistics().numTotalPackets()).isGreaterThan(20);
      assertThat(paymentReceipt.paymentStatistics().packetFailurePercentage()).isGreaterThan(Percentage.ZERO_PERCENT);
      assertThat(paymentReceipt.paymentStatistics().lowerBoundExchangeRate().toBigDecimal())
        .isEqualTo(new BigDecimal("1"));
      assertThat(paymentReceipt.paymentStatistics().upperBoundExchangeRate().toBigDecimal()).isBetween(
        BigDecimal.ONE, BigDecimal.valueOf(1.1)
      );
      assertThat(paymentReceipt.paymentStatistics().paymentDuration()).isGreaterThan(Duration.ZERO);
    });
  }

  @Test
  public void sendBothDirectionsMultiThreaded() {
    this.initIlpNetworkForStream(new SimulatedIlpv4Network(
      SimulatedPathConditions.builder()
        .maxPacketAmount(() -> UnsignedLong.valueOf(60000))
        .build(),
      SimulatedPathConditions.builder()
        .maxPacketAmount(() -> UnsignedLong.valueOf(40000))
        .build()
    ));

    final BigDecimal paymentAmount = new BigDecimal(1); // <-- Send 1 XRP, or 1x10^6 units.
    final int parallelism = 50;
    final int runs = 200;

    // queue up left-to-right send
    final List<CompletableFuture<PaymentReceipt>> results =
      runInParallel(parallelism, runs, () -> pay(leftStreamPayerNode, rightStreamPayerNode, paymentAmount));

    // queue up right-to-left send
    results.addAll(
      runInParallel(parallelism, runs, () -> pay(rightStreamPayerNode, leftStreamPayerNode, paymentAmount))
    );

    awaitResults(results).forEach(paymentReceipt -> {
      assertThat(paymentReceipt.successfulPayment()).isTrue();
      assertThat(paymentReceipt.amountSentInSendersUnits()).isEqualTo(BigInteger.valueOf(1000000));
      assertThat(paymentReceipt.amountDeliveredInDestinationUnits()).isEqualTo(BigInteger.valueOf(1000000));
      assertThat(paymentReceipt.amountLeftToSendInSendersUnits()).isEqualTo(BigInteger.ZERO);
      assertThat(paymentReceipt.paymentStatistics().numFulfilledPackets()).isBetween(15, 30);
      assertThat(paymentReceipt.paymentStatistics().numRejectPackets()).isLessThan(5);
      assertThat(paymentReceipt.paymentStatistics().numTotalPackets()).isPositive();
      assertThat(paymentReceipt.paymentStatistics().packetFailurePercentage()).isGreaterThan(Percentage.ZERO_PERCENT);
      assertThat(paymentReceipt.paymentStatistics().lowerBoundExchangeRate().toBigDecimal()).isBetween(
        BigDecimal.ONE, BigDecimal.valueOf(1.1)
      );
      assertThat(paymentReceipt.paymentStatistics().paymentDuration()).isGreaterThan(Duration.ZERO);
    });
  }

  /////////////////
  // Helper Methods
  /////////////////

  private PaymentReceipt pay(
    final StreamPayerNode fromNode, final StreamPayerNode toNode, final BigDecimal paymentAmount
  ) {
    Objects.requireNonNull(fromNode);
    Objects.requireNonNull(toNode);
    Objects.requireNonNull(paymentAmount);

    final AccountDetails sourceAccountDetails = AccountDetails.builder()
      .interledgerAddress(fromNode.senderAddress())
      .denomination(Denomination.builder()
        .assetScale(fromNode.denomination().assetScale())
        .assetCode(fromNode.denomination().assetCode())
        .build())
      .build();

    final PaymentOptions paymentOptions = PaymentOptions.builder()
      .senderAccountDetails(sourceAccountDetails)
      .destinationPaymentPointer(
        PaymentPointer.of("$example.com/" + toNode.link().getLinkId()) // <-- toNode is the receiver
      )
      .amountToSend(paymentAmount)
      .expectedDestinationDenomination(toNode.denomination()) // <-- toNode is the receiver
      .build();

    // PAY
    return fromNode.streamPayer().getQuote(paymentOptions)
      .handle((quote, throwable) -> {
        if (throwable != null) {
          logger.error(throwable.getMessage(), throwable);
        }

        return fromNode.streamPayer().pay(quote).join();
      }).join();
  }

  /**
   * Initialize the STREAM network with default Simulated Path Conditions on each payment path.
   */
  private void initIlpNetworkForStream() {
    final SimulatedIlpv4Network simulatedIlpNetwork = new SimulatedIlpv4Network(
      SimulatedPathConditions.builder().build(),
      SimulatedPathConditions.builder().build()
    );
    this.initIlpNetworkForStream(simulatedIlpNetwork);
  }

  private void initIlpNetworkForStream(final SimulatedIlpv4Network simulatedIlpNetwork) {
    this.simulatedIlpNetwork = Objects.requireNonNull(simulatedIlpNetwork);

    this.leftStreamPayerNode = this.initLeftNode();
    this.simulatedIlpNetwork.getLeftToRightLink().registerLinkHandler(incomingPreparePacket ->
      leftStreamPayerNode.streamReceiver().receiveMoney(
        incomingPreparePacket, LEFT_RECEIVER_ADDRESS,
        org.interledger.stream.Denomination.from(leftStreamPayerNode.denomination())
      )
    );

    this.rightStreamPayerNode = this.initRightNode();
    this.simulatedIlpNetwork.getRightToLeftLink().registerLinkHandler(incomingPreparePacket ->
      rightStreamPayerNode.streamReceiver().receiveMoney(
        incomingPreparePacket, RIGHT_RECEIVER_ADDRESS,
        org.interledger.stream.Denomination.from(rightStreamPayerNode.denomination())
      )
    );
  }

  private StreamPayerNode initLeftNode() {
    final byte[] serverSecret = BaseEncoding.base16().decode(SHARED_SECRET_HEX);
    final AesGcmStreamSharedSecretCrypto streamSharedSecretCrypto = new AesGcmStreamSharedSecretCrypto();

    // Can represent the streamReceiver for either node (for purposes of setting up this left node).
    final StreamReceiver streamReceiver = new StatelessStreamReceiver(
      () -> serverSecret,
      new SpspStreamConnectionGenerator(),
      streamSharedSecretCrypto,
      StreamCodecContextFactory.oer()
    );

    // The client is used by the left node, simulating a connection to the right node.
    final StreamConnectionDetails connectionDetailsForRightNode = streamReceiver.setupStream(RIGHT_RECEIVER_ADDRESS);
    final SpspClient mockSpspClient = new SpspClient() {
      @Override
      public StreamConnectionDetails getStreamConnectionDetails(PaymentPointer paymentPointer)
        throws InvalidReceiverClientException {
        return connectionDetailsForRightNode;
      }

      @Override
      public StreamConnectionDetails getStreamConnectionDetails(HttpUrl spspUrl) throws InvalidReceiverClientException {
        return connectionDetailsForRightNode;
      }
    };

    final Link<?> ilpLink = simulatedIlpNetwork.getLeftToRightLink();
    final StreamPayer streamPayer = new StreamPayer.Default(
      new StreamPacketEncryptionService(
        StreamCodecContextFactory.oer(),
        streamSharedSecretCrypto
      ),
      ilpLink,
      new DefaultOracleExchangeRateService(mockExchangeRateProvider()),
      mockSpspClient
    );

    return StreamPayerNode.builder()
      .serverSecret(serverSecret)
      .senderAddress(LEFT_SENDER_ADDRESS)
      .receiverAddress(LEFT_RECEIVER_ADDRESS)
      .streamPayer(streamPayer)
      .streamReceiver(streamReceiver)
      .denomination(Denomination.builder().assetCode("XRP").assetScale((short) 6).build())
      .link(ilpLink)
      .build();
  }

  private StreamPayerNode initRightNode() {
    final byte[] serverSecret = BaseEncoding.base16().decode(SHARED_SECRET_HEX);
    final AesGcmStreamSharedSecretCrypto streamSharedSecretCrypto = new AesGcmStreamSharedSecretCrypto();

    // Can represent the streamReceiver for either node (for purposes of setting up this right node).
    final StreamReceiver streamReceiver = new StatelessStreamReceiver(
      () -> serverSecret,
      new SpspStreamConnectionGenerator(),
      streamSharedSecretCrypto,
      StreamCodecContextFactory.oer()
    );

    // The client is used by the right node, simulating a connection to the left node.
    final StreamConnectionDetails connectionDetailsForLeftNode = streamReceiver.setupStream(LEFT_RECEIVER_ADDRESS);
    final SpspClient mockSpspClient = new SpspClient() {
      @Override
      public StreamConnectionDetails getStreamConnectionDetails(PaymentPointer paymentPointer)
        throws InvalidReceiverClientException {
        return connectionDetailsForLeftNode;
      }

      @Override
      public StreamConnectionDetails getStreamConnectionDetails(HttpUrl spspUrl) throws InvalidReceiverClientException {
        return connectionDetailsForLeftNode;
      }
    };

    final Link<?> ilpLink = simulatedIlpNetwork.getRightToLeftLink();
    final StreamPayer streamPayer = new StreamPayer.Default(
      new StreamPacketEncryptionService(
        StreamCodecContextFactory.oer(),
        streamSharedSecretCrypto
      ),
      ilpLink,
      new DefaultOracleExchangeRateService(mockExchangeRateProvider()),
      mockSpspClient
    );

    return StreamPayerNode.builder()
      .serverSecret(serverSecret)
      .senderAddress(RIGHT_SENDER_ADDRESS)
      .receiverAddress(RIGHT_RECEIVER_ADDRESS)
      .streamPayer(streamPayer)
      .streamReceiver(streamReceiver)
      .denomination(Denomination.builder().assetCode("XRP").assetScale((short) 6).build())
      .link(ilpLink)
      .build();
  }

  private <T> List<CompletableFuture<T>> runInParallel(int parallelism, int runCount, Callable<T> task) {
    ExecutorService executorService = Executors.newFixedThreadPool(parallelism);
    List<CompletableFuture<T>> tasks = IntStream.range(0, runCount)
      .mapToObj((taskId) -> CompletableFuture.supplyAsync(() -> {
        logger.info("Starting task " + taskId);
        try {
          T result = task.call();
          logger.info("Finished task " + taskId);
          return result;
        } catch (Exception e) {
          logger.warn("Failed task " + taskId, e);
          throw new RuntimeException(e);
        }
      }, executorService))
      .collect(Collectors.toList());
    executorService.shutdown();
    return tasks;
  }

  private static ExchangeRateProvider mockExchangeRateProvider() {
    final ExchangeRateProvider exchangeRateProvider = mock(ExchangeRateProvider.class);

    {
      ExchangeRate xrpUsdRate = mock(ExchangeRate.class);
      when(xrpUsdRate.getFactor()).thenReturn(DefaultNumberValue.of(new BigDecimal("0.2429546")));
      when(exchangeRateProvider.getExchangeRate("XRP", "USD")).thenReturn(xrpUsdRate);
      CurrencyUnit baseCurrencyUnit = mock(CurrencyUnit.class);
      when(xrpUsdRate.getBaseCurrency()).thenReturn(baseCurrencyUnit);
      when(xrpUsdRate.getCurrency()).thenReturn(baseCurrencyUnit);
    }
    {
      ExchangeRate identityRate = mock(ExchangeRate.class);
      when(identityRate.getFactor()).thenReturn(DefaultNumberValue.of(BigDecimal.ONE));
      when(exchangeRateProvider.getExchangeRate("XRP", "XRP")).thenReturn(identityRate);
      when(exchangeRateProvider.getExchangeRate("USD", "USD")).thenReturn(identityRate);
      CurrencyUnit baseCurrencyUnit = mock(CurrencyUnit.class);
      when(identityRate.getBaseCurrency()).thenReturn(baseCurrencyUnit);
      when(identityRate.getCurrency()).thenReturn(baseCurrencyUnit);
    }

    return exchangeRateProvider;
  }

  private static <T> List<T> awaitResults(List<CompletableFuture<T>> futures) {
    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    return futures.stream().map(CompletableFuture::join).collect(Collectors.toList());
  }

  /**
   * An internal class that encapsulates all the details needed for a particular STREAM node (regardless of if its a
   * sender or receiver) for purposes of this test harness.
   */
  @Immutable
  public interface StreamPayerNode {

    static StreamPayerNodeBuilder builder() {
      return new StreamPayerNodeBuilder();
    }

    /**
     * The server secret for this node.
     */
    byte[] serverSecret();

    /**
     * The {@link InterledgerAddress} that this node will use to send STREAM payments from.
     */
    InterledgerAddress senderAddress();

    /**
     * The {@link InterledgerAddress} that this node will use to receive STREAM payment to.
     */
    InterledgerAddress receiverAddress();

    /**
     * The denomination that this Node has with its immediate peer (this is not part of Link because it's possible to
     * have multiple links to the same peer).
     */
    Denomination denomination();

    /**
     * The {@link Link} that this node uses to communicate to its immediate peer with.
     *
     * @return the {@link Link}.
     */
    Link<?> link();

    /**
     * The {@link StreamPayer} that this node uses to send STREAM payments.
     *
     * @return The {@link StreamPayer}.
     */
    StreamPayer streamPayer();

    /**
     * The {@link StreamReceiver} that this node uses to receive STREAM payments on.
     *
     * @return The {@link StreamReceiver}.
     */
    StreamReceiver streamReceiver();

    /**
     * Constructs new {@link StreamConnectionDetails} for this node.
     *
     * @return An instance of {@link StreamConnectionDetails}.
     */
    @Derived
    default StreamConnectionDetails getNewStreamConnectionDetails() {
      return new SpspStreamConnectionGenerator().generateConnectionDetails(this::serverSecret, receiverAddress());
    }

  }
}
