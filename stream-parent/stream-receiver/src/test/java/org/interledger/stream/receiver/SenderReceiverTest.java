package org.interledger.stream.receiver;

import static org.assertj.core.api.Assertions.assertThat;

import org.interledger.codecs.stream.StreamCodecContextFactory;
import org.interledger.core.InterledgerAddress;
import org.interledger.spsp.StreamConnectionDetails;
import org.interledger.stream.SendMoneyResult;
import org.interledger.stream.crypto.JavaxStreamEncryptionService;
import org.interledger.stream.receiver.testutils.SimulatedILPv4Network;
import org.interledger.stream.receiver.testutils.SimulatedPathConditions;
import org.interledger.stream.sender.SimpleStreamSender;

import com.google.common.io.BaseEncoding;
import com.google.common.primitives.UnsignedLong;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Base64;
import java.util.Objects;

/**
 * A unit tests that simulates network connectivity between a sender and a receiver in order isolate and control various
 * network conditions as part of a STREAM payment.
 */
public class SenderReceiverTest {

  private static final InterledgerAddress LEFT_ILP_ADDRESS = InterledgerAddress.of("test.xpring-dev.left");
  private static final InterledgerAddress LEFT_SENDER_ADDRESS = LEFT_ILP_ADDRESS.with("left_stream_sender");
  private static final InterledgerAddress LEFT_RECEIVER_ADDRESS = LEFT_ILP_ADDRESS.with("left_stream_receiver");

  private static final InterledgerAddress RIGHT_ILP_ADDRESS = InterledgerAddress.of("test.xpring-dev.right");
  private static final InterledgerAddress RIGHT_SENDER_ADDRESS = RIGHT_ILP_ADDRESS.with("right_stream_sender");
  private static final InterledgerAddress RIGHT_RECEIVER_ADDRESS = RIGHT_ILP_ADDRESS.with("right_stream_receiver");

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  // Used to derive sub-secrets when the left link is acting as a STREAM receiver.
  private byte[] leftServerSecret;
  private StreamReceiver leftStreamReceiver;

  private StreamConnectionGenerator streamConnectionGenerator;
  private SimulatedILPv4Network simulatedILPv4Network;

  // Used to derive sub-secrets when the right link is acting as a STREAM receiver.
  private byte[] rightServerSecret;
  private StreamReceiver rightStreamReceiver;

  @Before
  public void setup() {
    this.leftServerSecret = BaseEncoding.base16()
        .decode("9DCE76B1A20EC8D3DB05AD579F3293402743767692F935A0BF06B30D2728439D");
    this.rightServerSecret = BaseEncoding.base16()
        .decode("9DCE76B1A20EC8D3DB05AD579F3293402743767692F935A0BF06B30D2728439D");

    this.streamConnectionGenerator = new SpspStreamConnectionGenerator();
    this.initIlpv4NetworkForSTREAM();
  }

  @Test
  public void testSendFromLeftToRight() {
    final UnsignedLong paymentAmount = UnsignedLong.valueOf(1000);

    SimpleStreamSender streamSender = new SimpleStreamSender(
        new JavaxStreamEncryptionService(), simulatedILPv4Network.getLeftToRightLink()
    );

    final StreamConnectionDetails connectionDetails = this
        .getIlpAddressForStreamReception(rightServerSecret, RIGHT_RECEIVER_ADDRESS);

    final SendMoneyResult sendMoneyResult = streamSender.sendMoney(
        connectionDetails.sharedSecret().key(),
        LEFT_SENDER_ADDRESS,
        connectionDetails.destinationAddress(),
        paymentAmount
    ).join();

    assertThat(sendMoneyResult.amountDelivered()).isEqualTo(paymentAmount);
    assertThat(sendMoneyResult.originalAmount()).isEqualTo(paymentAmount);
    assertThat(sendMoneyResult.numFulfilledPackets()).isEqualTo(2);
    assertThat(sendMoneyResult.numRejectPackets()).isEqualTo(0);

    logger.info("Payment Sent: {}", sendMoneyResult);
  }

  @Test
  public void testSendFromRightToLeft() {
    final UnsignedLong paymentAmount = UnsignedLong.valueOf(1000);

    SimpleStreamSender streamSender = new SimpleStreamSender(
        new JavaxStreamEncryptionService(), simulatedILPv4Network.getRightToLeftLink()
    );

    final StreamConnectionDetails connectionDetails = this
        .getIlpAddressForStreamReception(leftServerSecret, LEFT_RECEIVER_ADDRESS);

    final SendMoneyResult sendMoneyResult = streamSender.sendMoney(
        Base64.getDecoder().decode(connectionDetails.sharedSecret()),
        RIGHT_SENDER_ADDRESS,
        connectionDetails.destinationAddress(),
        paymentAmount
    ).join();

    assertThat(sendMoneyResult.amountDelivered()).isEqualTo(paymentAmount);
    assertThat(sendMoneyResult.originalAmount()).isEqualTo(paymentAmount);
    assertThat(sendMoneyResult.numFulfilledPackets()).isEqualTo(2);
    assertThat(sendMoneyResult.numRejectPackets()).isEqualTo(0);

    logger.info("Payment Sent: {}", sendMoneyResult);
  }

  /**
   * Simulate a left-to-right STREAM with high levels of packet loss.
   */
  @Test
  public void testSendFromLeftToRightWithHighLoss() {
    final UnsignedLong paymentAmount = UnsignedLong.valueOf(100000);

    this.initIlpv4NetworkForSTREAM(new SimulatedILPv4Network(
        SimulatedPathConditions.builder().packetRejectionPercentage(.5f).build(),
        SimulatedPathConditions.builder().build()
    ));

    SimpleStreamSender streamSender = new SimpleStreamSender(
        new JavaxStreamEncryptionService(), simulatedILPv4Network.getLeftToRightLink()
    );

    final StreamConnectionDetails connectionDetails = this
        .getIlpAddressForStreamReception(rightServerSecret, RIGHT_RECEIVER_ADDRESS);

    final SendMoneyResult sendMoneyResult = streamSender.sendMoney(
        Base64.getDecoder().decode(connectionDetails.sharedSecret()),
        LEFT_SENDER_ADDRESS,
        connectionDetails.destinationAddress(),
        paymentAmount
    ).join();

    assertThat(sendMoneyResult.amountDelivered()).isEqualTo(paymentAmount);
    assertThat(sendMoneyResult.originalAmount()).isEqualTo(paymentAmount);
    // Depending on the probability of rejection, we expect the number of fulfill packets to be _at least_ 5, but
    // perhaps more.
    assertThat(sendMoneyResult.numFulfilledPackets()).isGreaterThanOrEqualTo(5);
    assertThat(sendMoneyResult.numFulfilledPackets()).isLessThanOrEqualTo(20);
    assertThat(sendMoneyResult.numRejectPackets()).isEqualTo(0);

    logger.info("Payment Sent: {}", sendMoneyResult);


  }

  /////////////////
  // Helper Methods
  /////////////////

  /**
   * Initialize the STREAM network with default Simulated Path Conditions on each payment path.
   */
  private void initIlpv4NetworkForSTREAM() {
    this.initIlpv4NetworkForSTREAM(this.simulatedILPv4Network = new SimulatedILPv4Network(
        SimulatedPathConditions.builder().build(),
        SimulatedPathConditions.builder().build()
    ));
  }

  private void initIlpv4NetworkForSTREAM(final SimulatedILPv4Network simulatedILPv4Network) {
    this.simulatedILPv4Network = Objects.requireNonNull(simulatedILPv4Network);
    this.initLeftNode();
    this.initRightNode();
  }

  private void initLeftNode() {
    this.leftStreamReceiver = new StatelessStreamReceiver(
        () -> leftServerSecret,
        new SpspStreamConnectionGenerator(),
        new JavaxStreamEncryptionService(),
        StreamCodecContextFactory.oer()
    );

    simulatedILPv4Network.getLeftToRightLink().registerLinkHandler(incomingPreparePacket ->
        leftStreamReceiver.receiveMoney(incomingPreparePacket, incomingPreparePacket.getDestination())
    );
  }

  private void initRightNode() {
    this.rightStreamReceiver = new StatelessStreamReceiver(
        () -> rightServerSecret,
        new SpspStreamConnectionGenerator(),
        new JavaxStreamEncryptionService(),
        StreamCodecContextFactory.oer()
    );

    simulatedILPv4Network.getRightLink().registerLinkHandler(incomingPreparePacket ->
        rightStreamReceiver.receiveMoney(incomingPreparePacket, RECEIVER_ADDRESS)
    );
  }

  /**
   * Take an ILP address can determine the _actual_ receiver address that will work properly for the shared secret
   * configured by this test harness.
   *
   * @param serverSecret    Random bytes that are used to derive secrets for the STREAM protocol (typically held my a
   *                        single server like the receiver).
   * @param receiverAddress A baseline receiver address (e.g., `g.bob`).
   *
   * @return A {@link StreamConnectionDetails} that is derived using {@code serverSecret}.
   */
  private StreamConnectionDetails getIlpAddressForStreamReception(
      final byte[] serverSecret,
      final InterledgerAddress receiverAddress
  ) {
    return this.streamConnectionGenerator.generateConnectionDetails(() -> serverSecret, receiverAddress);
  }
}
