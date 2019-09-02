package org.interledger.stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import org.interledger.core.InterledgerAddress;
import org.interledger.link.Link;
import org.interledger.stream.StreamClient.SendMoneyResult;
import org.interledger.stream.crypto.JavaxStreamEncryptionService;

import com.google.common.primitives.UnsignedLong;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StreamClientTest {
private static final InterledgerAddress SENDER_ADDRESS = InterledgerAddress.of("test.xpring-dev.rs1.java_stream_client");
  private final Logger logger = LoggerFactory.getLogger(this.getClass());
  private Link link;
  private byte[] sharedSecret;
  private InterledgerAddress destinationAddress;

  @Before
  public void setUp() {
    this.link = null; // TODO: Create Link.
    this.sharedSecret = null; // TODO: Use SPSP client to get shared secret.
    this.destinationAddress = null; // Get from SPSP
  }

  @Test
  public void sendMoney() {
    final UnsignedLong paymentAmount = UnsignedLong.valueOf(1000L);

    StreamClient streamClient = new StreamClient(
        new JavaxStreamEncryptionService(),
        new ConnectionManager()
    );

    final SendMoneyResult sendMoneyResult = streamClient
        .sendMoney(link, sharedSecret, SENDER_ADDRESS, destinationAddress, paymentAmount).join();

    assertThat(sendMoneyResult.amountDelivered(), is(paymentAmount));
    assertThat(sendMoneyResult.originalAmount(), is(paymentAmount));
    assertThat(sendMoneyResult.numFulfilledPackets(), is(10));
    assertThat(sendMoneyResult.numRejectPackets(), is(0));

    logger.info("{}", sendMoneyResult);
  }
}
