package org.interledger.stream.receiver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.interledger.codecs.stream.StreamCodecContextFactory;
import org.interledger.core.Immutable;
import org.interledger.core.InterledgerAddress;
import org.interledger.fx.Denomination;
import org.interledger.link.Link;
import org.interledger.spsp.PaymentPointer;
import org.interledger.spsp.StreamConnectionDetails;
import org.interledger.spsp.client.InvalidReceiverClientException;
import org.interledger.spsp.client.SpspClient;
import org.interledger.stream.connection.StreamConnection;
import org.interledger.stream.crypto.AesGcmStreamSharedSecretCrypto;
import org.interledger.stream.crypto.StreamPacketEncryptionService;
import org.interledger.stream.crypto.StreamSharedSecret;
import org.interledger.stream.model.AccountDetails;
import org.interledger.stream.pay.StreamPayer;
import org.interledger.stream.pay.model.PaymentReceipt;
import org.interledger.stream.pay.model.Quote;
import org.interledger.stream.receiver.testutils.SimulatedIlpv4Network;
import org.interledger.stream.receiver.testutils.SimulatedPathConditions;

import ch.qos.logback.classic.Level;
import com.google.common.io.BaseEncoding;
import com.google.common.primitives.UnsignedLong;
import okhttp3.HttpUrl;
import org.immutables.value.Value.Derived;
import org.javamoney.moneta.spi.DefaultNumberValue;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
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
 * A unit tests that simulates network connectivity between a sender and a receiver in order isolate and control various
 * network conditions as part of a STREAM payment.
 */
public class SenderReceiverWithStreamPayerTest {

  private static final String SHARED_SECRET_HEX = "9DCE76B1A20EC8D3DB05AD579F3293402743767692F935A0BF06B30D2728439D";

  private static final InterledgerAddress LEFT_ILP_ADDRESS = InterledgerAddress.of("test.xpring-dev.left");
  private static final InterledgerAddress LEFT_SENDER_ADDRESS = LEFT_ILP_ADDRESS.with("left_stream_sender");
  private static final InterledgerAddress LEFT_RECEIVER_ADDRESS = LEFT_ILP_ADDRESS.with("left_stream_receiver");

  private static final InterledgerAddress RIGHT_ILP_ADDRESS = InterledgerAddress.of("test.xpring-dev.right");
  private static final InterledgerAddress RIGHT_SENDER_ADDRESS = RIGHT_ILP_ADDRESS.with("right_stream_sender");
  private static final InterledgerAddress RIGHT_RECEIVER_ADDRESS = RIGHT_ILP_ADDRESS.with("right_stream_receiver");

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private StreamPayerNode leftStreamPayerNode;
  private StreamPayerNode rightStreamPayerNode;

  private SimulatedIlpv4Network simulatedIlpNetwork;

  private static StreamPayerNode initNode(
    Link<?> link,
    InterledgerAddress senderAddress,
    InterledgerAddress receiverAddress
  ) {
    final byte[] serverSecret = BaseEncoding.base16().decode(SHARED_SECRET_HEX);
    final AesGcmStreamSharedSecretCrypto streamSharedSecretCrypto = new AesGcmStreamSharedSecretCrypto();

    return initNode(
      link,
      senderAddress,
      receiverAddress,
      new StatelessStreamReceiver(
        () -> serverSecret,
        new SpspStreamConnectionGenerator(),
        streamSharedSecretCrypto,
        StreamCodecContextFactory.oer()
      )
    );
  }

  private static StreamPayerNode initNode(
    Link<?> ilpLink,
    InterledgerAddress senderAddress,
    InterledgerAddress receiverAddress,
    StreamReceiver streamReceiver
  ) {
    final AesGcmStreamSharedSecretCrypto streamSharedSecretCrypto = new AesGcmStreamSharedSecretCrypto();
    final byte[] serverSecret = BaseEncoding.base16().decode(SHARED_SECRET_HEX);

    final StreamConnectionDetails connectionDetails = streamReceiver.setupStream(receiverAddress);
    final SpspClient spspClientMock = new SpspClient() {
      @Override
      public StreamConnectionDetails getStreamConnectionDetails(PaymentPointer paymentPointer)
        throws InvalidReceiverClientException {
        return connectionDetails;
      }

      @Override
      public StreamConnectionDetails getStreamConnectionDetails(HttpUrl spspUrl) throws InvalidReceiverClientException {
        return connectionDetails;
      }
    };

    final StreamPayer streamPayer = new StreamPayer.Default(
      new StreamPacketEncryptionService(
        StreamCodecContextFactory.oer(),
        streamSharedSecretCrypto
      ),
      ilpLink,
      mockExchangeRateProvider(),
      spspClientMock
    );

    return StreamPayerNode.builder()
      .serverSecret(serverSecret)
      .senderAddress(senderAddress)
      .receiverAddress(receiverAddress)
      .streamPayer(streamPayer)
      .streamReceiver(streamReceiver)
      .denomination(Denomination.builder().assetCode("XRP").assetScale((short) 6).build())
      .link(ilpLink)
      .build();
  }

  private static <T> List<T> awaitResults(List<CompletableFuture<T>> futures) {
    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    return futures.stream().map(CompletableFuture::join).collect(Collectors.toList());
  }

  @Before
  public void setup() {
    final ch.qos.logback.classic.Logger logger
      = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
    logger.setLevel(Level.INFO);
    this.initIlpNetworkForStream();
  }

  //  @Test
//  public void testSendFromLeftToRight() {
//    final UnsignedLong paymentAmount = UnsignedLong.valueOf(1000);
//
//    final SendMoneyResult sendMoneyResult = sendMoney(leftStreamNode, rightStreamNode, paymentAmount);
//
//    assertThat(sendMoneyResult.amountDelivered()).isEqualTo(paymentAmount);
//    assertThat(sendMoneyResult.originalAmount()).isEqualTo(paymentAmount);
//    assertThat(sendMoneyResult.numFulfilledPackets()).isEqualTo(1);
//    assertThat(sendMoneyResult.numRejectPackets()).isEqualTo(0);
//
//    logger.info("Payment Sent: {}", sendMoneyResult);
//  }
//
//  @Test
//  public void testSendFromLeftToRightWithFixedReceiverAmountExactPathRate() {
//    final UnsignedLong amountToDeliver = UnsignedLong.valueOf(10000);
//
//    BigDecimal expectedPathRate = new BigDecimal("1.5");
//    BigDecimal actualPathRate = new BigDecimal("1.5");
//    this.initIlpNetworkForStream(new SimulatedIlpv4Network(
//      SimulatedPathConditions.builder().currentExchangeRateSupplier(() -> actualPathRate).build(),
//      SimulatedPathConditions.builder().build()
//    ));
//
//    SimpleStreamSender sender = new SimpleStreamSender(leftStreamNode.link());
//    final StreamConnectionDetails connectionDetails = rightStreamNode.getNewStreamConnectionDetails();
//
//    SendMoneyResult sendMoneyResult = sender.sendMoney(
//      SendMoneyRequest.builder()
//        .sourceAddress(leftStreamNode.senderAddress())
//        .amount(amountToDeliver)
//        .denomination(leftStreamNode.denomination())
//        .destinationAddress(connectionDetails.destinationAddress())
//        .sharedSecret(connectionDetails.sharedSecret())
//        .paymentTracker(
//          new FixedReceiverAmountPaymentTracker(amountToDeliver,
//            new FixedRateExchangeCalculator(expectedPathRate))
//        )
//        .timeout(Duration.ofMillis(1000L))
//        .build())
//      .join();
//
//    assertThat(sendMoneyResult.amountDelivered()).isEqualTo(amountToDeliver);
//    assertThat(sendMoneyResult.originalAmount()).isEqualTo(amountToDeliver);
//    assertThat(sendMoneyResult.numFulfilledPackets()).isGreaterThan(1);
//    assertThat(sendMoneyResult.numRejectPackets()).isEqualTo(0);
//    assertThat(sendMoneyResult.successfulPayment()).isTrue();
//
//    logger.info("Payment Sent: {}", sendMoneyResult);
//  }
//
//  @Test
//  public void testSendFromLeftToRightWithFixedReceiverAmountOverestimatedPathRate() {
//    final UnsignedLong paymentAmount = UnsignedLong.valueOf(10000);
//
//    // simulating get a better exchange rate than we expected in which case payments should get fulfilled because
//    // the receiver gets more than they were supposed to
//    BigDecimal expectedPathExchange = new BigDecimal("1.4");
//    BigDecimal actualPathExchange = new BigDecimal("1.5");
//    this.initIlpNetworkForStream(new SimulatedIlpv4Network(
//      SimulatedPathConditions.builder().currentExchangeRateSupplier(() -> actualPathExchange).build(),
//      SimulatedPathConditions.builder().build()
//    ));
//
//    SimpleStreamSender sender = new SimpleStreamSender(leftStreamNode.link());
//    final StreamConnectionDetails connectionDetails = rightStreamNode.getNewStreamConnectionDetails();
//
//    SendMoneyResult sendMoneyResult = sender.sendMoney(
//      SendMoneyRequest.builder()
//        .sharedSecret(connectionDetails.sharedSecret())
//        .amount(paymentAmount)
//        .denomination(leftStreamNode.denomination())
//        .destinationAddress(connectionDetails.destinationAddress())
//        .sourceAddress(leftStreamNode.senderAddress())
//        .paymentTracker(
//          new FixedReceiverAmountPaymentTracker(paymentAmount,
//            new FixedRateExchangeCalculator(expectedPathExchange))
//        )
//        .timeout(Duration.ofMillis(1000L))
//        .build()
//    ).join();
//
//    assertThat(sendMoneyResult.amountDelivered()).isGreaterThanOrEqualTo(paymentAmount);
//    assertThat(sendMoneyResult.originalAmount()).isEqualTo(paymentAmount);
//    assertThat(sendMoneyResult.amountSent()).isLessThan(paymentAmount);
//    assertThat(sendMoneyResult.numFulfilledPackets()).isGreaterThan(1);
//    assertThat(sendMoneyResult.numRejectPackets()).isEqualTo(0);
//    assertThat(sendMoneyResult.successfulPayment()).isTrue();
//
//    logger.info("Payment Sent: {}", sendMoneyResult);
//  }
//
//  @Test
//  public void testSendFromLeftToRightWithFixedReceiverAmountUnderestimatedPathRate() {
//    final UnsignedLong paymentAmount = UnsignedLong.valueOf(10000);
//
//    // simulating get a worse exchange rate than we expected in which case payments should get rejected because
//    // the receiver gets less than they were supposed to
//    BigDecimal expectedPathExchange = new BigDecimal("1.5");
//    BigDecimal actualPathExchange = new BigDecimal("1.4"); // lower rate than expected
//    this.initIlpNetworkForStream(new SimulatedIlpv4Network(
//      SimulatedPathConditions.builder().currentExchangeRateSupplier(() -> actualPathExchange).build(),
//      SimulatedPathConditions.builder().build()
//    ));
//
//    SimpleStreamSender sender = new SimpleStreamSender(leftStreamNode.link());
//    final StreamConnectionDetails connectionDetails = rightStreamNode.getNewStreamConnectionDetails();
//
//    SendMoneyResult sendMoneyResult = sender.sendMoney(
//      SendMoneyRequest.builder()
//        .sourceAddress(leftStreamNode.senderAddress())
//        .amount(paymentAmount)
//        .denomination(leftStreamNode.denomination())
//        .destinationAddress(connectionDetails.destinationAddress())
//        .sharedSecret(connectionDetails.sharedSecret())
//        .paymentTracker(new FixedReceiverAmountPaymentTracker(paymentAmount,
//          new FixedRateExchangeCalculator(expectedPathExchange)))
//        .timeout(Duration.ofMillis(1000))
//        .build())
//      .join();
//
//    assertThat(sendMoneyResult.amountDelivered()).isEqualTo(UnsignedLong.ZERO);
//    assertThat(sendMoneyResult.originalAmount()).isEqualTo(paymentAmount);
//    assertThat(sendMoneyResult.amountSent()).isEqualTo(UnsignedLong.ZERO);
//    assertThat(sendMoneyResult.numFulfilledPackets()).isEqualTo(0);
//    assertThat(sendMoneyResult.numRejectPackets()).isGreaterThan(0);
//    assertThat(sendMoneyResult.successfulPayment()).isFalse();
//
//    logger.info("Payment Sent: {}", sendMoneyResult);
//  }
//
  @Test
  public void testSendFromRightToLeft() {
    final UnsignedLong paymentAmount = UnsignedLong.valueOf(1000);

    final StreamConnectionDetails connectionDetails = leftStreamPayerNode.getNewStreamConnectionDetails();

    final AccountDetails sourceAccountDetails = AccountDetails.builder()
      .interledgerAddress(rightStreamPayerNode.senderAddress())
      .denomination(Denomination.builder()
        .assetScale(rightStreamPayerNode.denomination().assetScale())
        .assetCode(rightStreamPayerNode.denomination().assetCode())
        .build())
      .build();

    final AccountDetails destinationAccountDetails = AccountDetails.builder()
      .interledgerAddress(connectionDetails.destinationAddress())
      .build();

    final Quote quote = Quote.builder()
      .sourceAccount(sourceAccountDetails)
      .destinationAccount(destinationAccountDetails)
      .streamConnection(new StreamConnection(
        sourceAccountDetails,
        connectionDetails.destinationAddress(),
        StreamSharedSecret.of(connectionDetails.sharedSecret().key())
      ))
      .build();

    final PaymentReceipt paymentReceipt = rightStreamPayerNode.streamPayer().pay(quote).join();

//      SendMoneyRequest.builder()
//        .sourceAddress(rightStreamNode.senderAddress())
//        .amount(paymentAmount)
//        .denomination(rightStreamNode.denomination())
//        .destinationAddress(connectionDetails.destinationAddress())
//        .sharedSecret(connectionDetails.sharedSecret())
//        .paymentTracker(new FixedSenderAmountPaymentTracker(paymentAmount, new NoOpExchangeRateCalculator()))
//        .timeout(Duration.ofMillis(10000))
//        .build()
//    ).join();

    assertThat(paymentReceipt.amountDeliveredInDestinationUnits()).isEqualTo(paymentAmount);
    assertThat(paymentReceipt.amountDeliveredInDestinationUnits()).isEqualTo(paymentAmount);

    //assertThat(paymentReceipt.originalQuote().paymentSharedStateTracker().getExchangeRateTracker().).isEqualTo(1);
    //assertThat(paymentReceipt.numRejectPackets()).isEqualTo(0);

    logger.info("PaymentReceipt: {}", paymentReceipt);
  }
//
//  /**
//   * Validates that the send operation does not complete if the preflight check fails.
//   */
//  @Test
//  public void testSendCannotDetermineReceiverDenomination() {
//    final UnsignedLong paymentAmount = UnsignedLong.valueOf(1000);
//
//    final byte[] serverSecret = BaseEncoding.base16().decode(SHARED_SECRET_HEX);
//    final StreamEncryptionService streamEncryptionService = new JavaxStreamEncryptionService();
//    final StreamReceiver brokenStreamReceiver = new AlwaysEmptyStreamReceiver(
//      () -> serverSecret,
//      new SpspStreamConnectionGenerator(),
//      streamEncryptionService,
//      StreamCodecContextFactory.oer()
//    );
//    this.rightStreamNode = initNode(simulatedIlpNetwork.getRightToLeftLink(), RIGHT_SENDER_ADDRESS,
//      RIGHT_RECEIVER_ADDRESS, brokenStreamReceiver);
//    this.simulatedIlpNetwork.getRightToLeftLink().unregisterLinkHandler();
//    this.simulatedIlpNetwork.getRightToLeftLink().registerLinkHandler(incomingPreparePacket ->
//      brokenStreamReceiver.receiveMoney(incomingPreparePacket, RIGHT_RECEIVER_ADDRESS, rightStreamNode.denomination())
//    );
//
//    final SimpleStreamSender sender = new SimpleStreamSender(leftStreamNode.link());
//    final StreamConnectionDetails connectionDetails = leftStreamNode.getNewStreamConnectionDetails();
//
//    final SendMoneyResult sendMoneyResult = sender.sendMoney(
//      SendMoneyRequest.builder()
//        .sourceAddress(leftStreamNode.senderAddress())
//        .amount(paymentAmount)
//        .denomination(leftStreamNode.denomination())
//        .destinationAddress(connectionDetails.destinationAddress())
//        .sharedSecret(connectionDetails.sharedSecret())
//        .paymentTracker(new FixedReceiverAmountPaymentTracker(paymentAmount, new NoOpExchangeRateCalculator()))
//        .timeout(Duration.ofMillis(10000))
//        .build())
//      .join();
//
//    assertThat(sendMoneyResult.amountDelivered()).isEqualTo(UnsignedLong.ZERO);
//    assertThat(sendMoneyResult.originalAmount()).isEqualTo(paymentAmount);
//    assertThat(sendMoneyResult.numFulfilledPackets()).isEqualTo(0);
//    assertThat(sendMoneyResult.numRejectPackets()).isEqualTo(0);
//    assertThat(sendMoneyResult.successfulPayment()).isFalse();
//    assertThat(sendMoneyResult.amountLeftToSend()).isEqualTo(UnsignedLong.valueOf(1000L));
//
//    logger.info("Payment Sent: {}", sendMoneyResult);
//  }
//
//  /**
//   * Simulate a left-to-right STREAM with 20% packet loss due to T03 errors.
//   */
//  @Test
//  public void testSendFromLeftToRightWith20PercentLoss() {
//    final UnsignedLong paymentAmount = UnsignedLong.valueOf(1000000);
//    final float rejectionPercentage = 0.2f;
//
//    final SendMoneyResult sendMoneyResult = this.sendMoneyWithLossHelper(paymentAmount, rejectionPercentage);
//
//    assertThat(sendMoneyResult.numFulfilledPackets()).isCloseTo(10, Offset.offset(1));
//    assertThat(sendMoneyResult.numRejectPackets()).isGreaterThanOrEqualTo(0);
//  }
//
//  /**
//   * Simulate a left-to-right STREAM with 50% packet loss due to T03 errors.
//   */
//  @Test
//  public void testSendFromLeftToRightWith50PercentLoss() {
//    final UnsignedLong paymentAmount = UnsignedLong.valueOf(1000000);
//    final float rejectionPercentage = 0.5f;
//
//    final SendMoneyResult sendMoneyResult = this.sendMoneyWithLossHelper(paymentAmount, rejectionPercentage);
//
//    assertThat(sendMoneyResult.numFulfilledPackets()).isCloseTo(10, Offset.offset(1));
//    assertThat(sendMoneyResult.numRejectPackets()).isGreaterThanOrEqualTo(1);
//  }
//
//  /**
//   * Simulate a left-to-right STREAM with 90% packet loss due to T03 errors.
//   */
//  @Test
//  public void testSendFromLeftToRightWith90PercentLoss() {
//    final UnsignedLong paymentAmount = UnsignedLong.valueOf(2000);
//    final float rejectionPercentage = 0.9f;
//
//    final SendMoneyResult sendMoneyResult = this.sendMoneyWithLossHelper(paymentAmount, rejectionPercentage);
//
//    assertThat(sendMoneyResult.numFulfilledPackets()).isCloseTo(2, Offset.offset(1));
//    assertThat(sendMoneyResult.numRejectPackets()).isGreaterThanOrEqualTo(1);
//  }
//
//  @Test
//  public void testSendFromLeftToRightWithSmallMaxPacketValueInNetwork() {
//    final UnsignedLong paymentAmount = UnsignedLong.valueOf(100);
//
//    final SimulatedIlpv4Network simulatedIlpNetwork = new SimulatedIlpv4Network(
//      SimulatedPathConditions.builder()
//        .maxPacketAmount(() -> UnsignedLong.ONE)
//        .build(),
//      SimulatedPathConditions.builder().build()
//    );
//    this.initIlpNetworkForStream(simulatedIlpNetwork);
//
//    final SendMoneyResult sendMoneyResult = sendMoney(leftStreamNode, rightStreamNode, paymentAmount);
//
//    assertThat(sendMoneyResult.amountDelivered()).isEqualTo(paymentAmount);
//    assertThat(sendMoneyResult.originalAmount()).isEqualTo(paymentAmount);
//    assertThat(sendMoneyResult.numFulfilledPackets()).isEqualTo(100);
//    assertThat(sendMoneyResult.numRejectPackets()).isGreaterThanOrEqualTo(1);
//
//    logger.info("Payment Sent: {}", sendMoneyResult);
//  }
//
//  @Test
//  public void sendBothDirections() {
//    final UnsignedLong paymentAmount = UnsignedLong.valueOf(10000);
//    SendMoneyResult leftToRightResult = sendMoney(leftStreamNode, rightStreamNode, paymentAmount);
//    SendMoneyResult rightToLeftResult = sendMoney(rightStreamNode, leftStreamNode, paymentAmount);
//
//    Lists.newArrayList(leftToRightResult, rightToLeftResult).forEach(result -> {
//      assertThat(result.successfulPayment()).isTrue();
//      assertThat(result.amountDelivered()).isEqualTo(paymentAmount);
//      assertThat(result.originalAmount()).isEqualTo(paymentAmount);
//      assertThat(result.numFulfilledPackets()).isGreaterThan(1);
//      assertThat(result.numRejectPackets()).isEqualTo(0);
//    });
//  }
//
//  @Test
//  public void sendFromLeftToRightMultiThreadedSharedSender() {
//    final UnsignedLong paymentAmount = UnsignedLong.valueOf(10000);
//    int parallelism = 40;
//    int runCount = 100;
//
//    SimpleStreamSender sender = new SimpleStreamSender(leftStreamNode.link());
//
//    List<CompletableFuture<SendMoneyResult>> results =
//      runInParallel(parallelism, runCount, () -> sendMoney(sender, leftStreamNode, rightStreamNode, paymentAmount));
//
//    awaitResults(results).forEach(result -> {
//      assertThat(result.numFulfilledPackets()).isGreaterThan(1);
//      assertThat(result.numRejectPackets()).isEqualTo(0);
//      assertThat(result.successfulPayment()).isTrue();
//      assertThat(result.amountDelivered()).isEqualTo(paymentAmount);
//      assertThat(result.originalAmount()).isEqualTo(paymentAmount);
//    });
//  }
//
//  @Test
//  public void sendBothDirectionsMultiThreaded() {
//    final UnsignedLong paymentAmount = UnsignedLong.valueOf(100000);
//    int parallelism = 50;
//    int runs = 200;
//
//    // queue up left-to-right send
//    SimpleStreamSender sendLeft = new SimpleStreamSender(leftStreamNode.link());
//    List<CompletableFuture<SendMoneyResult>> results =
//      runInParallel(parallelism, runs, () -> sendMoney(sendLeft, leftStreamNode, rightStreamNode, paymentAmount));
//
//    // queue up right-to-left send
//    SimpleStreamSender sendRight = new SimpleStreamSender(leftStreamNode.link());
//    results.addAll(
//      runInParallel(parallelism, runs, () -> sendMoney(sendRight, rightStreamNode, leftStreamNode, paymentAmount)));
//
//    awaitResults(results).forEach(result -> {
//      assertThat(result.successfulPayment()).isTrue();
//      assertThat(result.amountDelivered()).isEqualTo(paymentAmount);
//      assertThat(result.originalAmount()).isEqualTo(paymentAmount);
//      assertThat(result.numFulfilledPackets()).isGreaterThan(1);
//      assertThat(result.numRejectPackets()).isEqualTo(0);
//    });
//  }

  /////////////////
  // Helper Methods
  /////////////////

//  private SendMoneyResult sendMoney(StreamNode fromNode, StreamNode toNode, UnsignedLong paymentAmount) {
//    return sendMoney(new SimpleStreamSender(fromNode.link()), fromNode, toNode, paymentAmount);
//  }
//
//  private SendMoneyResult sendMoney(
//    StreamSender sender, StreamNode fromNode, StreamNode toNode, UnsignedLong paymentAmount
//  ) {
//    final StreamConnectionDetails connectionDetails = toNode.getNewStreamConnectionDetails();
//    return sender.sendMoney(
//      SendMoneyRequest.builder()
//        .sourceAddress(fromNode.senderAddress())
//        .amount(paymentAmount)
//        .denomination(fromNode.denomination())
//        .destinationAddress(connectionDetails.destinationAddress())
//        .sharedSecret(connectionDetails.sharedSecret())
//        .paymentTracker(new FixedSenderAmountPaymentTracker(paymentAmount, new NoOpExchangeRateCalculator()))
//        .timeout(Duration.ofMillis(10000))
//        .build())
//      .join();
//  }
//
//  /**
//   * Helper method to test lossy ILPv4 network percentages.
//   */
//  private SendMoneyResult sendMoneyWithLossHelper(final UnsignedLong paymentAmount, final float rejectionPercentage) {
//    this.initIlpNetworkForStream(new SimulatedIlpv4Network(
//      SimulatedPathConditions.builder().packetRejectionPercentage(rejectionPercentage).build(),
//      SimulatedPathConditions.builder().build()
//    ));
//
//    final SendMoneyResult sendMoneyResult = sendMoney(leftStreamNode, rightStreamNode, paymentAmount);
//
//    assertThat(sendMoneyResult.amountDelivered()).isEqualTo(paymentAmount);
//    assertThat(sendMoneyResult.originalAmount()).isEqualTo(paymentAmount);
//
//    logger.info("Payment Sent: {}", sendMoneyResult);
//    return sendMoneyResult;
//  }

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
    return initNode(simulatedIlpNetwork.getLeftToRightLink(), LEFT_SENDER_ADDRESS, LEFT_RECEIVER_ADDRESS);
  }

  private StreamPayerNode initRightNode() {
    return initNode(simulatedIlpNetwork.getRightToLeftLink(), RIGHT_SENDER_ADDRESS, RIGHT_RECEIVER_ADDRESS);
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
