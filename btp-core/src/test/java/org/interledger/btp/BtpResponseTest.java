package org.interledger.btp;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.nio.charset.StandardCharsets;

public class BtpResponseTest {

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
    final BtpResponse message = BtpResponse.builder()
        .requestId(REQUEST_ID)
        .subProtocols(SUB_PROTOCOLS)
        .build();

    assertEquals(message.getType(), BtpMessageType.RESPONSE);
  }

  @Test
  public void builder() {

    final BtpResponse message = BtpResponse.builder()
        .requestId(REQUEST_ID)
        .subProtocols(SUB_PROTOCOLS)
        .build();

    assertEquals(message.getPrimarySubProtocol(), SUB_PROTOCOL);

  }

}