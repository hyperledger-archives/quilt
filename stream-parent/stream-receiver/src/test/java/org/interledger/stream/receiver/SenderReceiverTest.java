package org.interledger.stream.receiver;

import org.interledger.codecs.stream.StreamCodecContextFactory;
import org.interledger.core.InterledgerAddress;
import org.interledger.stream.SendMoneyResult;
import org.interledger.stream.crypto.JavaxStreamEncryptionService;
import org.interledger.stream.receiver.testutils.SimulatedILPv4Network;
import org.interledger.stream.receiver.testutils.SimulatedPathConditions;
import org.interledger.stream.sender.SimpleStreamSender;
import org.interledger.stream.sender.StreamSender;

import com.google.common.io.BaseEncoding;
import com.google.common.primitives.UnsignedLong;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A unit tests that simulates network connectivity between a sender and a receiver in order isolate and control various
 * network conditions as part of a STREAM payment.
 */
public class SenderReceiverTest {

  private static final InterledgerAddress ILP_ADDRESS = InterledgerAddress.of("test.xpring-dev.rs1");
  private static final InterledgerAddress SENDER_ADDRESS = ILP_ADDRESS.with("java_stream_client");
  private static final InterledgerAddress RECEIVER_ADDRESS = ILP_ADDRESS.with("java_stream_receiver");
  private static final String assetCode = "USD";
  private static final short assetScale = 100;

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private SimulatedILPv4Network simulatedILPv4Network;

  // Used to derive sub-secrets when the left link is acting as a STREAM receiver.
  private byte[] leftServerSecret;

  // Used to derive sub-secrets when the right link is acting as a STREAM receiver.
  private byte[] rightServerSecret;

  private byte[] sharedSecret;
  private InterledgerAddress destinationAddress;

  @Before
  public void setup() {
    this.leftServerSecret = BaseEncoding.base16()
        .decode("9DCE76B1A20EC8D3DB05AD579F3293402743767692F935A0BF06B30D2728439D");
    this.rightServerSecret = BaseEncoding.base16()
        .decode("9DCE76B1A20EC8D3DB05AD579F3293402743767692F935A0BF06B30D2728439D");

    this.simulatedILPv4Network = new SimulatedILPv4Network(
        SimulatedPathConditions.builder().build(),
        SimulatedPathConditions.builder().build()
    );

    final StreamReceiver rightStreamReceiver = new StatelessStreamReceiver(
        () -> rightServerSecret,
        new SpspStreamConnectionGenerator(),
        new JavaxStreamEncryptionService(),
        StreamCodecContextFactory.oer()
    );

    simulatedILPv4Network.getRightLink().registerLinkHandler(incomingPreparePacket ->
        rightStreamReceiver.receiveMoney(incomingPreparePacket, RECEIVER_ADDRESS, assetCode, assetScale)
    );

    this.sharedSecret = Base64.getDecoder().decode("R5FMgJ1fOSg3SztrMwKAS9KaGJuVYAUeLstWt8ZP6mk=");
    this.destinationAddress = InterledgerAddress
        .of("test.xpring-dev.rs1.java_stream_receiver.Khml7p2S2JrKWsOSJBTlQDWK5Wz7xiHHvKA8hqS-zHU"); // TODO: Get from SPSP
  }

  @Test
  public void testSendFromLeftToRight() {
    final UnsignedLong paymentAmount = UnsignedLong.valueOf(1000);

    StreamSender streamSender = new SimpleStreamSender(
        new JavaxStreamEncryptionService(), simulatedILPv4Network.getLeftLink()
    );

    final SendMoneyResult sendMoneyResult = streamSender
        .sendMoney(sharedSecret, SENDER_ADDRESS, destinationAddress, paymentAmount).join();

    assertThat(sendMoneyResult.amountDelivered()).isEqualTo(paymentAmount);
    assertThat(sendMoneyResult.originalAmount()).isEqualTo(paymentAmount);
    assertThat(sendMoneyResult.numFulfilledPackets()).isEqualTo(2);
    assertThat(sendMoneyResult.numRejectPackets()).isEqualTo(0);

    logger.info("Payment Sent: {}", sendMoneyResult);
  }

}
