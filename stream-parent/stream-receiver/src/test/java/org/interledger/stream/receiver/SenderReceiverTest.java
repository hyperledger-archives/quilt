package org.interledger.stream.receiver;

import static org.assertj.core.api.Assertions.assertThat;

import org.interledger.codecs.stream.StreamCodecContextFactory;
import org.interledger.core.Immutable;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.SharedSecret;
import org.interledger.link.Link;
import org.interledger.spsp.StreamConnectionDetails;
import org.interledger.stream.Denomination;
import org.interledger.stream.SendMoneyResult;
import org.interledger.stream.crypto.JavaxStreamEncryptionService;
import org.interledger.stream.crypto.StreamEncryptionService;
import org.interledger.stream.receiver.testutils.SimulatedIlpv4Network;
import org.interledger.stream.receiver.testutils.SimulatedPathConditions;
import org.interledger.stream.sender.SimpleStreamSender;
import org.interledger.stream.sender.StreamSender;

import com.google.common.io.BaseEncoding;
import com.google.common.primitives.UnsignedLong;
import org.assertj.core.data.Offset;
import org.immutables.value.Value.Derived;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

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

  @Before
  public void setup() {
    this.initIlpNetworkForStream();
  }

  @Test
  public void testSendFromLeftToRight() {
    final UnsignedLong paymentAmount = UnsignedLong.valueOf(1000);

    final StreamConnectionDetails connectionDetails = leftStreamNode.getNewStreamConnectionDetails();
    final SendMoneyResult sendMoneyResult = leftStreamNode.streamSender().sendMoney(
        SharedSecret.of(connectionDetails.sharedSecret().key()),
        LEFT_SENDER_ADDRESS,
        connectionDetails.destinationAddress(),
        paymentAmount
    ).join();

    assertThat(sendMoneyResult.amountDelivered()).isEqualTo(paymentAmount);
    assertThat(sendMoneyResult.originalAmount()).isEqualTo(paymentAmount);
    assertThat(sendMoneyResult.numFulfilledPackets()).isEqualTo(1);
    assertThat(sendMoneyResult.numRejectPackets()).isEqualTo(0);

    logger.info("Payment Sent: {}", sendMoneyResult);
  }

  @Test
  public void testSendFromRightToLeft() {
    final UnsignedLong paymentAmount = UnsignedLong.valueOf(1000);

    final StreamConnectionDetails connectionDetails = rightStreamNode.getNewStreamConnectionDetails();
    final SendMoneyResult sendMoneyResult = rightStreamNode.streamSender().sendMoney(
        SharedSecret.of(connectionDetails.sharedSecret().key()),
        RIGHT_SENDER_ADDRESS,
        connectionDetails.destinationAddress(),
        paymentAmount
    ).join();

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

    final StreamConnectionDetails connectionDetails = leftStreamNode.getNewStreamConnectionDetails();
    final SendMoneyResult sendMoneyResult = leftStreamNode.streamSender().sendMoney(
        SharedSecret.of(connectionDetails.sharedSecret().key()),
        LEFT_SENDER_ADDRESS,
        connectionDetails.destinationAddress(),
        paymentAmount
    ).join();

    assertThat(sendMoneyResult.amountDelivered()).isEqualTo(paymentAmount);
    assertThat(sendMoneyResult.originalAmount()).isEqualTo(paymentAmount);
    assertThat(sendMoneyResult.numFulfilledPackets()).isEqualTo(100);
    assertThat(sendMoneyResult.numRejectPackets()).isGreaterThanOrEqualTo(1);

    logger.info("Payment Sent: {}", sendMoneyResult);
  }

  /////////////////
  // Helper Methods
  /////////////////

  /**
   * Helper method to test lossy ILPv4 network percentages.
   */
  private SendMoneyResult sendMoneyWithLossHelper(final UnsignedLong paymentAmount, final float rejectionPercentage) {
    this.initIlpNetworkForStream(new SimulatedIlpv4Network(
        SimulatedPathConditions.builder().packetRejectionPercentage(rejectionPercentage).build(),
        SimulatedPathConditions.builder().build()
    ));

    final StreamConnectionDetails connectionDetails = leftStreamNode.getNewStreamConnectionDetails();
    final SendMoneyResult sendMoneyResult = leftStreamNode.streamSender().sendMoney(
        SharedSecret.of(connectionDetails.sharedSecret().key()),
        LEFT_SENDER_ADDRESS,
        connectionDetails.destinationAddress(),
        paymentAmount
    ).join();

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
    final byte[] serverSecret = BaseEncoding.base16().decode(SHARED_SECRET_HEX);
    final StreamEncryptionService streamEncryptionService = new JavaxStreamEncryptionService();

    SimpleStreamSender streamSender = new SimpleStreamSender(
        streamEncryptionService, simulatedIlpNetwork.getLeftToRightLink()
    );

    StatelessStreamReceiver streamReceiver = new StatelessStreamReceiver(
        () -> serverSecret,
        new SpspStreamConnectionGenerator(),
        streamEncryptionService,
        StreamCodecContextFactory.oer()
    );

    return StreamNode.builder()
        .serverSecret(serverSecret)
        .senderAddress(LEFT_SENDER_ADDRESS)
        .receiverAddress(RIGHT_RECEIVER_ADDRESS)
        .streamSender(streamSender)
        .streamReceiver(streamReceiver)
        .denomination(Denomination.builder().assetCode("XRP").assetScale((short) 6).build())
        .link(simulatedIlpNetwork.getLeftToRightLink())
        .build();
  }

  private StreamNode initRightNode() {
    final byte[] serverSecret = BaseEncoding.base16().decode(SHARED_SECRET_HEX);
    final StreamEncryptionService streamEncryptionService = new JavaxStreamEncryptionService();

    SimpleStreamSender streamSender = new SimpleStreamSender(
        streamEncryptionService, simulatedIlpNetwork.getRightToLeftLink()
    );

    StatelessStreamReceiver streamReceiver = new StatelessStreamReceiver(
        () -> serverSecret,
        new SpspStreamConnectionGenerator(),
        streamEncryptionService,
        StreamCodecContextFactory.oer()
    );

    return StreamNode.builder()
        .serverSecret(serverSecret)
        .senderAddress(RIGHT_SENDER_ADDRESS)
        .receiverAddress(RIGHT_RECEIVER_ADDRESS)
        .streamSender(streamSender)
        .streamReceiver(streamReceiver)
        .denomination(Denomination.builder().assetCode("XRP").assetScale((short) 6).build())
        .link(simulatedIlpNetwork.getRightToLeftLink())
        .build();
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
     */
    Link link();

    /**
     * The {@link StreamSender} that this node uses to send STREAM payments.
     */
    StreamSender streamSender();

    /**
     * The {@link StreamReceiver} that this node uses to receive STREAM payments on.
     */
    StreamReceiver streamReceiver();

    @Derived
    default StreamConnectionDetails getNewStreamConnectionDetails() {
      return new SpspStreamConnectionGenerator().generateConnectionDetails(this::serverSecret, receiverAddress());
    }

  }
}
