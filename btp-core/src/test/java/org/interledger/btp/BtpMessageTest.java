package org.interledger.btp;

import static org.junit.Assert.*;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

public class BtpMessageTest {

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
    final BtpMessage message = BtpMessage.builder()
        .requestId(REQUEST_ID)
        .subProtocols(SUB_PROTOCOLS)
        .build();

    assertEquals(message.getType(), BtpMessageType.MESSAGE);
  }

  @Test
  public void builder() {

    final BtpMessage message = BtpMessage.builder()
        .requestId(REQUEST_ID)
        .subProtocols(SUB_PROTOCOLS)
        .build();

    assertEquals(message.getPrimarySubProtocol(), SUB_PROTOCOL);

  }

}