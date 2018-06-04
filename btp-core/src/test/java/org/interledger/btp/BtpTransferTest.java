package org.interledger.btp;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;

public class BtpTransferTest {

  private static final long REQUEST_ID = 1;
  private static final BtpSubProtocols SUB_PROTOCOLS = new BtpSubProtocols();
  private static final BtpSubProtocol SUB_PROTOCOL = BtpSubProtocol.builder()
      .protocolName("TEST")
      .contentType(BtpSubProtocolContentType.MIME_TEXT_PLAIN_UTF8)
      .data("Test Data".getBytes(StandardCharsets.UTF_8))
      .build();

  static {
    SUB_PROTOCOLS.add(SUB_PROTOCOL);
  }

  @Test
  public void getType() {

    final BtpTransfer transfer = BtpTransfer.builder()
        .requestId(REQUEST_ID)
        .amount(BigInteger.TEN)
        .subProtocols(SUB_PROTOCOLS)
        .build();

    assertEquals(transfer.getType(), BtpMessageType.TRANSFER);
  }

  @Test
  public void builder() {

    final BtpTransfer transfer = BtpTransfer.builder()
        .requestId(REQUEST_ID)
        .amount(BigInteger.TEN)
        .subProtocols(SUB_PROTOCOLS)
        .build();

    assertEquals(transfer.getAmount(), BigInteger.TEN);
    assertEquals(transfer.getPrimarySubProtocol(), SUB_PROTOCOL);

  }

}