package org.interledger.stream.receiver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.interledger.stream.SenderAmountMode.RECEIVER_AMOUNT;
import static org.interledger.stream.SenderAmountMode.SENDER_AMOUNT;

import org.interledger.codecs.stream.StreamCodecContextFactory;
import org.interledger.core.Immutable;
import org.interledger.core.InterledgerAddress;
import org.interledger.link.Link;
import org.interledger.spsp.StreamConnectionDetails;
import org.interledger.stream.Denomination;
import org.interledger.stream.SendMoneyRequest;
import org.interledger.stream.SendMoneyResult;
import org.interledger.stream.calculators.ExchangeRateCalculator;
import org.interledger.stream.calculators.NoOpExchangeRateCalculator;
import org.interledger.stream.crypto.JavaxStreamEncryptionService;
import org.interledger.stream.crypto.StreamEncryptionService;
import org.interledger.stream.receiver.testutils.SimulatedIlpv4Network;
import org.interledger.stream.receiver.testutils.SimulatedPathConditions;
import org.interledger.stream.sender.FixedReceiverAmountPaymentTracker;
import org.interledger.stream.sender.FixedSenderAmountPaymentTracker;
import org.interledger.stream.sender.SimpleStreamSender;
import org.interledger.stream.sender.StreamSender;

import com.google.common.collect.Lists;
import com.google.common.io.BaseEncoding;
import com.google.common.primitives.UnsignedLong;
import org.assertj.core.data.Offset;
import org.immutables.value.Value.Derived;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * A unit tests that simulates network connectivity between a sender and a receiver in order isolate and control various
 * network conditions as part of a STREAM payment.
 */
public class SenderReceiverTest {

  private static final String SHARED_SECRET_HEX = "9DCE76B1A20EC8D3DB05AD579F3293402743767692F935A0BF06B30D2728439D";

  private static final InterledgerAddress LEFT_ILP_ADDRESS = InterledgerAddress.of("test.xpring-dev.left");
  private static final InterledgerAddress LEFT_SENDER_ADDRESS = LEFT_ILP_ADDRESS.with("left_stream_sender");
  private static final InterledgerAddress LEFT_RECEIVER_ADDRESS = LEFT_ILP_ADDRESS.with("left_stream_receiver");

  private static final InterledgerAddress RIGHT_ILP_ADDRESS = InterledgerAddress.of("test.xpring-dev.right");
  private static final InterledgerAddress RIGHT_SENDER_ADDRESS = RIGHT_ILP_ADDRESS.with("right_stream_sender");
  private static final InterledgerAddress RIGHT_RECEIVER_ADDRESS = RIGHT_ILP_ADDRESS.with("right_stream_receiver");

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private StreamNode leftStreamNode;
  private StreamNode rightStreamNode;

  private SimulatedIlpv4Network simulatedIlpNetwork;

  private static StreamNode initNode(
      Link link,
      InterledgerAddress senderAddress,
      InterledgerAddress receiverAddress
  ) {
    final byte[] serverSecret = BaseEncoding.base16().decode(SHARED_SECRET_HEX);
    final StreamEncryptionService streamEncryptionService = new JavaxStreamEncryptionService();

    SimpleStreamSender streamSender = new SimpleStreamSender(
        streamEncryptionService, link
    );

    StatelessStreamReceiver streamReceiver = new StatelessStreamReceiver(
        () -> serverSecret,
        new SpspStreamConnectionGenerator(),
        streamEncryptionService,
        StreamCodecContextFactory.oer()
    );

    return StreamNode.builder()
        .serverSecret(serverSecret)
        .senderAddress(senderAddress)
        .receiverAddress(receiverAddress)
        .streamSender(streamSender)
        .streamReceiver(streamReceiver)
        .denomination(Denomination.builder().assetCode("XRP").assetScale((short) 6).build())
        .link(link)
        .build();
  }

  private static <T> List<T> awaitResults(List<CompletableFuture<T>> futures) {
    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    return futures.stream().map(CompletableFuture::join).collect(Collectors.toList());
  }

  @Before
  public void setup() {
    this.initIlpNetworkForStream();
  }

  @Test
  public void testSendFromLeftToRight() {
    final UnsignedLong paymentAmount = UnsignedLong.valueOf(1000);

    final SendMoneyResult sendMoneyResult = sendMoney(leftStreamNode, rightStreamNode, paymentAmount);

    assertThat(sendMoneyResult.amountDelivered()).isEqualTo(paymentAmount);
    assertThat(sendMoneyResult.originalAmount()).isEqualTo(paymentAmount);
    assertThat(sendMoneyResult.numFulfilledPackets()).isEqualTo(1);
    assertThat(sendMoneyResult.numRejectPackets()).isEqualTo(0);

    logger.info("Payment Sent: {}", sendMoneyResult);
  }

  @Test
  public void testSendFromLeftToRightWithFixedReceiverAmountExactPathRate() {
    final UnsignedLong amountToDeliver = UnsignedLong.valueOf(10000);

    BigDecimal expectedPathRate = new BigDecimal("1.5");
    BigDecimal actualPathRate = new BigDecimal("1.5");
    this.initIlpNetworkForStream(new SimulatedIlpv4Network(
        SimulatedPathConditions.builder().currentExchangeRateSupplier(() -> actualPathRate).build(),
        SimulatedPathConditions.builder().build()
    ));

    SimpleStreamSender sender = new SimpleStreamSender(leftStreamNode.link());
    final StreamConnectionDetails connectionDetails = rightStreamNode.getNewStreamConnectionDetails();

    SendMoneyResult sendMoneyResult = sender.sendMoney(
        SendMoneyRequest.builder()
            .sourceAddress(leftStreamNode.senderAddress())
            .amount(amountToDeliver)
            .senderAmountMode(RECEIVER_AMOUNT)
            .denomination(leftStreamNode.denomination())
            .destinationAddress(connectionDetails.destinationAddress())
            .sharedSecret(connectionDetails.sharedSecret())
            .paymentTracker(
                new FixedReceiverAmountPaymentTracker(amountToDeliver,
                    new FixedRateExchangeCalculator(expectedPathRate))
            )
            .timeout(Duration.ofMillis(1000L))
            .build())
        .join();

    assertThat(sendMoneyResult.amountDelivered()).isEqualTo(amountToDeliver);
    assertThat(sendMoneyResult.originalAmount()).isEqualTo(amountToDeliver);
    assertThat(sendMoneyResult.numFulfilledPackets()).isGreaterThan(1);
    assertThat(sendMoneyResult.numRejectPackets()).isEqualTo(0);
    assertThat(sendMoneyResult.successfulPayment()).isTrue();

    logger.info("Payment Sent: {}", sendMoneyResult);
  }

  @Test
  public void testSendFromLeftToRightWithFixedReceiverAmountOverestimatedPathRate() {
    final UnsignedLong paymentAmount = UnsignedLong.valueOf(10000);

    // simulating get a better exchange rate than we expected in which case payments should get fulfilled because
    // the receiver gets more than they were supposed to
    BigDecimal expectedPathExchange = new BigDecimal("1.4");
    BigDecimal actualPathExchange = new BigDecimal("1.5");
    this.initIlpNetworkForStream(new SimulatedIlpv4Network(
        SimulatedPathConditions.builder().currentExchangeRateSupplier(() -> actualPathExchange).build(),
        SimulatedPathConditions.builder().build()
    ));

    SimpleStreamSender sender = new SimpleStreamSender(leftStreamNode.link());
    final StreamConnectionDetails connectionDetails = rightStreamNode.getNewStreamConnectionDetails();

    SendMoneyResult sendMoneyResult = sender.sendMoney(
        SendMoneyRequest.builder()
            .sharedSecret(connectionDetails.sharedSecret())
            .amount(paymentAmount)
            .senderAmountMode(RECEIVER_AMOUNT)
            .denomination(leftStreamNode.denomination())
            .destinationAddress(connectionDetails.destinationAddress())
            .sourceAddress(leftStreamNode.senderAddress())
            .paymentTracker(
                new FixedReceiverAmountPaymentTracker(paymentAmount,
                    new FixedRateExchangeCalculator(expectedPathExchange))
            )
            .timeout(Duration.ofMillis(1000L))
            .build()
    ).join();

    assertThat(sendMoneyResult.amountDelivered()).isGreaterThanOrEqualTo(paymentAmount);
    assertThat(sendMoneyResult.originalAmount()).isEqualTo(paymentAmount);
    assertThat(sendMoneyResult.amountSent()).isLessThan(paymentAmount);
    assertThat(sendMoneyResult.numFulfilledPackets()).isGreaterThan(1);
    assertThat(sendMoneyResult.numRejectPackets()).isEqualTo(0);
    assertThat(sendMoneyResult.successfulPayment()).isTrue();

    logger.info("Payment Sent: {}", sendMoneyResult);
  }

  @Test
  public void testSendFromLeftToRightWithFixedReceiverAmountUnderestimatedPathRate() {
    final UnsignedLong paymentAmount = UnsignedLong.valueOf(10000);

    // simulating get a worse exchange rate than we expected in which case payments should get rejected because
    // the receiver gets less than they were supposed to
    BigDecimal expectedPathExchange = new BigDecimal("1.5");
    BigDecimal actualPathExchange = new BigDecimal("1.4"); // lower rate than expected
    this.initIlpNetworkForStream(new SimulatedIlpv4Network(
        SimulatedPathConditions.builder().currentExchangeRateSupplier(() -> actualPathExchange).build(),
        SimulatedPathConditions.builder().build()
    ));

    SimpleStreamSender sender = new SimpleStreamSender(leftStreamNode.link());
    final StreamConnectionDetails connectionDetails = rightStreamNode.getNewStreamConnectionDetails();

    SendMoneyResult sendMoneyResult = sender.sendMoney(
        SendMoneyRequest.builder()
            .sourceAddress(leftStreamNode.senderAddress())
            .amount(paymentAmount)
            .senderAmountMode(RECEIVER_AMOUNT)
            .denomination(leftStreamNode.denomination())
            .destinationAddress(connectionDetails.destinationAddress())
            .sharedSecret(connectionDetails.sharedSecret())
            .paymentTracker(new FixedReceiverAmountPaymentTracker(paymentAmount,
                new FixedRateExchangeCalculator(expectedPathExchange)))
            .timeout(Duration.ofMillis(1000))
            .build())
        .join();

    assertThat(sendMoneyResult.amountDelivered()).isEqualTo(UnsignedLong.ZERO);
    assertThat(sendMoneyResult.originalAmount()).isEqualTo(paymentAmount);
    assertThat(sendMoneyResult.amountSent()).isEqualTo(UnsignedLong.ZERO);
    assertThat(sendMoneyResult.numFulfilledPackets()).isEqualTo(0);
    assertThat(sendMoneyResult.numRejectPackets()).isGreaterThan(0);
    assertThat(sendMoneyResult.successfulPayment()).isFalse();

    logger.info("Payment Sent: {}", sendMoneyResult);
  }

  @Test
  public void testSendFromRightToLeft() {
    final UnsignedLong paymentAmount = UnsignedLong.valueOf(1000);

    final StreamConnectionDetails connectionDetails = leftStreamNode.getNewStreamConnectionDetails();
    final SendMoneyResult sendMoneyResult = rightStreamNode.streamSender().sendMoney(
        SendMoneyRequest.builder()
            .sourceAddress(rightStreamNode.senderAddress())
            .amount(paymentAmount)
            .senderAmountMode(SENDER_AMOUNT)
            .denomination(rightStreamNode.denomination())
            .destinationAddress(connectionDetails.destinationAddress())
            .sharedSecret(connectionDetails.sharedSecret())
            .paymentTracker(new FixedSenderAmountPaymentTracker(paymentAmount, new NoOpExchangeRateCalculator()))
            .timeout(Duration.ofMillis(10000))
            .build()
    ).join();

    assertThat(sendMoneyResult.amountDelivered()).isEqualTo(paymentAmount);
    assertThat(sendMoneyResult.originalAmount()).isEqualTo(paymentAmount);
    assertThat(sendMoneyResult.numFulfilledPackets()).isEqualTo(1);
    assertThat(sendMoneyResult.numRejectPackets()).isEqualTo(0);

    logger.info("Payment Sent: {}", sendMoneyResult);
  }

  @Test
  public void testSendCannotDetermineReceiverDenomination() {
    final UnsignedLong paymentAmount = UnsignedLong.valueOf(1000);

    final SendMoneyResult sendMoneyResult = sendMoney(rightStreamNode, leftStreamNode, paymentAmount);

    assertThat(sendMoneyResult.amountDelivered()).isEqualTo(paymentAmount);
    assertThat(sendMoneyResult.originalAmount()).isEqualTo(paymentAmount);
    assertThat(sendMoneyResult.numFulfilledPackets()).isEqualTo(1);
    assertThat(sendMoneyResult.numRejectPackets()).isEqualTo(0);

    logger.info("Payment Sent: {}", sendMoneyResult);
  }

  /**
   * Simulate a left-to-right STREAM with 20% packet loss due to T03 errors.
   */
  @Test
  public void testSendFromLeftToRightWith20PercentLoss() {
    final UnsignedLong paymentAmount = UnsignedLong.valueOf(1000000);
    final float rejectionPercentage = 0.2f;

    final SendMoneyResult sendMoneyResult = this.sendMoneyWithLossHelper(paymentAmount, rejectionPercentage);

    assertThat(sendMoneyResult.numFulfilledPackets()).isCloseTo(10, Offset.offset(1));
    assertThat(sendMoneyResult.numRejectPackets()).isGreaterThanOrEqualTo(0);
  }

  /**
   * Simulate a left-to-right STREAM with 50% packet loss due to T03 errors.
   */
  @Test
  public void testSendFromLeftToRightWith50PercentLoss() {
    final UnsignedLong paymentAmount = UnsignedLong.valueOf(1000000);
    final float rejectionPercentage = 0.5f;

    final SendMoneyResult sendMoneyResult = this.sendMoneyWithLossHelper(paymentAmount, rejectionPercentage);

    assertThat(sendMoneyResult.numFulfilledPackets()).isCloseTo(10, Offset.offset(1));
    assertThat(sendMoneyResult.numRejectPackets()).isGreaterThanOrEqualTo(1);
  }

  /**
   * Simulate a left-to-right STREAM with 90% packet loss due to T03 errors.
   */
  @Test
  public void testSendFromLeftToRightWith90PercentLoss() {
    final UnsignedLong paymentAmount = UnsignedLong.valueOf(2000);
    final float rejectionPercentage = 0.9f;

    final SendMoneyResult sendMoneyResult = this.sendMoneyWithLossHelper(paymentAmount, rejectionPercentage);

    assertThat(sendMoneyResult.numFulfilledPackets()).isCloseTo(2, Offset.offset(1));
    assertThat(sendMoneyResult.numRejectPackets()).isGreaterThanOrEqualTo(1);
  }

  @Test
  public void testSendFromLeftToRightWithSmallMaxPacketValueInNetwork() {
    final UnsignedLong paymentAmount = UnsignedLong.valueOf(100);

    final SimulatedIlpv4Network simulatedIlpNetwork = new SimulatedIlpv4Network(
        SimulatedPathConditions.builder()
            .maxPacketAmount(() -> UnsignedLong.ONE)
            .build(),
        SimulatedPathConditions.builder().build()
    );
    this.initIlpNetworkForStream(simulatedIlpNetwork);

    final SendMoneyResult sendMoneyResult = sendMoney(leftStreamNode, rightStreamNode, paymentAmount);

    assertThat(sendMoneyResult.amountDelivered()).isEqualTo(paymentAmount);
    assertThat(sendMoneyResult.originalAmount()).isEqualTo(paymentAmount);
    assertThat(sendMoneyResult.numFulfilledPackets()).isEqualTo(100);
    assertThat(sendMoneyResult.numRejectPackets()).isGreaterThanOrEqualTo(1);

    logger.info("Payment Sent: {}", sendMoneyResult);
  }

  @Test
  public void sendBothDirections() {
    final UnsignedLong paymentAmount = UnsignedLong.valueOf(10000);
    SendMoneyResult leftToRightResult = sendMoney(leftStreamNode, rightStreamNode, paymentAmount);
    SendMoneyResult rightToLeftResult = sendMoney(rightStreamNode, leftStreamNode, paymentAmount);

    Lists.newArrayList(leftToRightResult, rightToLeftResult).forEach(result -> {
      assertThat(result.successfulPayment()).isTrue();
      assertThat(result.amountDelivered()).isEqualTo(paymentAmount);
      assertThat(result.originalAmount()).isEqualTo(paymentAmount);
      assertThat(result.numFulfilledPackets()).isGreaterThan(1);
      assertThat(result.numRejectPackets()).isEqualTo(0);
    });
  }

  @Test
  public void sendFromLeftToRightMultiThreadedSharedSender() {
    final UnsignedLong paymentAmount = UnsignedLong.valueOf(10000);
    int parallelism = 40;
    int runCount = 100;

    SimpleStreamSender sender = new SimpleStreamSender(leftStreamNode.link());

    List<CompletableFuture<SendMoneyResult>> results =
        runInParallel(parallelism, runCount, () -> sendMoney(sender, leftStreamNode, rightStreamNode, paymentAmount));

    awaitResults(results).forEach(result -> {
      assertThat(result.numFulfilledPackets()).isGreaterThan(1);
      assertThat(result.numRejectPackets()).isEqualTo(0);
      assertThat(result.successfulPayment()).isTrue();
      assertThat(result.amountDelivered()).isEqualTo(paymentAmount);
      assertThat(result.originalAmount()).isEqualTo(paymentAmount);
    });
  }

  @Test
  public void sendBothDirectionsMultiThreaded() {
    final UnsignedLong paymentAmount = UnsignedLong.valueOf(100000);
    int parallelism = 50;
    int runs = 200;

    // queue up left-to-right send
    SimpleStreamSender sendLeft = new SimpleStreamSender(leftStreamNode.link());
    List<CompletableFuture<SendMoneyResult>> results =
        runInParallel(parallelism, runs, () -> sendMoney(sendLeft, leftStreamNode, rightStreamNode, paymentAmount));

    // queue up right-to-left send
    SimpleStreamSender sendRight = new SimpleStreamSender(leftStreamNode.link());
    results.addAll(
        runInParallel(parallelism, runs, () -> sendMoney(sendRight, rightStreamNode, leftStreamNode, paymentAmount)));

    awaitResults(results).forEach(result -> {
      assertThat(result.successfulPayment()).isTrue();
      assertThat(result.amountDelivered()).isEqualTo(paymentAmount);
      assertThat(result.originalAmount()).isEqualTo(paymentAmount);
      assertThat(result.numFulfilledPackets()).isGreaterThan(1);
      assertThat(result.numRejectPackets()).isEqualTo(0);
    });
  }

  /////////////////
  // Helper Methods
  /////////////////

  private SendMoneyResult sendMoney(StreamNode fromNode, StreamNode toNode, UnsignedLong paymentAmount) {
    return sendMoney(new SimpleStreamSender(fromNode.link()), fromNode, toNode, paymentAmount);
  }

  private SendMoneyResult sendMoney(
      StreamSender sender, StreamNode fromNode, StreamNode toNode, UnsignedLong paymentAmount
  ) {
    final StreamConnectionDetails connectionDetails = toNode.getNewStreamConnectionDetails();
    return sender.sendMoney(
        SendMoneyRequest.builder()
            .sourceAddress(fromNode.senderAddress())
            .amount(paymentAmount)
            .senderAmountMode(SENDER_AMOUNT)
            .denomination(fromNode.denomination())
            .destinationAddress(connectionDetails.destinationAddress())
            .sharedSecret(connectionDetails.sharedSecret())
            .paymentTracker(new FixedSenderAmountPaymentTracker(paymentAmount, new NoOpExchangeRateCalculator()))
            .timeout(Duration.ofMillis(10000))
            .build())
        .join();
  }

  /**
   * Helper method to test lossy ILPv4 network percentages.
   */
  private SendMoneyResult sendMoneyWithLossHelper(final UnsignedLong paymentAmount, final float rejectionPercentage) {
    this.initIlpNetworkForStream(new SimulatedIlpv4Network(
        SimulatedPathConditions.builder().packetRejectionPercentage(rejectionPercentage).build(),
        SimulatedPathConditions.builder().build()
    ));

    final SendMoneyResult sendMoneyResult = sendMoney(leftStreamNode, rightStreamNode, paymentAmount);

    assertThat(sendMoneyResult.amountDelivered()).isEqualTo(paymentAmount);
    assertThat(sendMoneyResult.originalAmount()).isEqualTo(paymentAmount);

    logger.info("Payment Sent: {}", sendMoneyResult);
    return sendMoneyResult;
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

    this.leftStreamNode = this.initLeftNode();
    this.simulatedIlpNetwork.getLeftToRightLink().registerLinkHandler(incomingPreparePacket ->
        leftStreamNode.streamReceiver()
            .receiveMoney(incomingPreparePacket, LEFT_RECEIVER_ADDRESS, leftStreamNode.denomination())
    );

    this.rightStreamNode = this.initRightNode();
    this.simulatedIlpNetwork.getRightToLeftLink().registerLinkHandler(incomingPreparePacket ->
        rightStreamNode.streamReceiver()
            .receiveMoney(incomingPreparePacket, RIGHT_RECEIVER_ADDRESS, rightStreamNode.denomination())
    );
  }

  private StreamNode initLeftNode() {
    return initNode(simulatedIlpNetwork.getLeftToRightLink(), LEFT_SENDER_ADDRESS, LEFT_RECEIVER_ADDRESS);
  }

  private StreamNode initRightNode() {
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

  /**
   * An internal class that encapsulates all the details needed for a particular STREAM node (regardless of if its a
   * sender or receiver) for purposes of this test harness.
   */
  @Immutable
  public interface StreamNode {

    static StreamNodeBuilder builder() {
      return new StreamNodeBuilder();
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
    Link link();

    /**
     * The {@link StreamSender} that this node uses to send STREAM payments.
     *
     * @return The {@link StreamSender}.
     */
    StreamSender streamSender();

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

  /**
   * An {@link ExchangeRateCalculator} that specifies a fixed exchange rate forever.
   */
  static class FixedRateExchangeCalculator implements ExchangeRateCalculator {

    // exchange rate from sender unit's to receiver unit's
    // if rate = 0.9 then 1 sender unit = 0.9 receiver units
    private final BigDecimal senderUnitsPerReceiverUnits;

    FixedRateExchangeCalculator(BigDecimal senderUnitsPerReceiverUnits) {
      this.senderUnitsPerReceiverUnits = senderUnitsPerReceiverUnits;
    }

    @Override
    public UnsignedLong calculateAmountToSend(UnsignedLong amountToReceive,
        Denomination sendDenomination,
        Denomination receiveDenomination) {
      return UnsignedLong.valueOf(new BigDecimal(amountToReceive.bigIntegerValue())
          .divide(senderUnitsPerReceiverUnits, RoundingMode.FLOOR).toBigInteger()); // FIXME how to round
    }

    @Override
    public UnsignedLong calculateMinAmountToAccept(UnsignedLong sendAmount,
        Denomination sendDenomination,
        Optional<Denomination> expectedReceivedDenomination) {
      return UnsignedLong.valueOf(new BigDecimal(sendAmount.bigIntegerValue())
          .multiply(senderUnitsPerReceiverUnits).toBigInteger()); // FIXME how to round
    }
  }

}
