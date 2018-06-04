package org.interledger.btp;

import static org.junit.Assert.*;

import org.junit.Test;

import java.nio.charset.StandardCharsets;

public class BtpPacketTest {

  private static final long REQUEST_ID = 1;
  private static final BtpSubProtocols SUB_PROTOCOLS = new BtpSubProtocols();
  private static final BtpSubProtocol SUB_PROTOCOL_1 = BtpSubProtocol.builder()
      .protocolName("TEST")
      .contentType(BtpSubProtocolContentType.MIME_TEXT_PLAIN_UTF8)
      .data("Test Data".getBytes(StandardCharsets.UTF_8))
      .build();

  private static final BtpSubProtocol SUB_PROTOCOL_2 = BtpSubProtocol.builder()
      .protocolName("TEST2")
      .contentType(BtpSubProtocolContentType.MIME_TEXT_PLAIN_UTF8)
      .data("Test Data".getBytes(StandardCharsets.UTF_8))
      .build();

  static {
    SUB_PROTOCOLS.add(SUB_PROTOCOL_1);
    SUB_PROTOCOLS.add(SUB_PROTOCOL_2);
  }

  @Test
  public void getSubProtocols() {
    final BtpMessage message = BtpMessage.builder()
        .requestId(REQUEST_ID)
        .subProtocols(SUB_PROTOCOLS)
        .build();

    assertEquals(message.getSubProtocols(), SUB_PROTOCOLS);
    assertEquals(message.getSubProtocols().size(), 2);
  }

  @Test
  public void getPrimarySubProtocol() {
    final BtpMessage message = BtpMessage.builder()
        .requestId(REQUEST_ID)
        .subProtocols(SUB_PROTOCOLS)
        .build();

    assertEquals(message.getPrimarySubProtocol(), SUB_PROTOCOL_1);
  }

  @Test
  public void getSubProtocol() {
    final BtpMessage message = BtpMessage.builder()
        .requestId(REQUEST_ID)
        .subProtocols(SUB_PROTOCOLS)
        .build();

    assertEquals(message.getSubProtocol("TEST"), SUB_PROTOCOL_1);
    assertEquals(message.getSubProtocol("TEST2"), SUB_PROTOCOL_2);
  }

  @Test
  public void hasSubProtocol() {
    final BtpMessage message = BtpMessage.builder()
        .requestId(REQUEST_ID)
        .subProtocols(SUB_PROTOCOLS)
        .build();

    assertTrue(message.hasSubProtocol("TEST"));
    assertTrue(message.hasSubProtocol("TEST2"));
    assertFalse(message.hasSubProtocol("OTHER"));

  }
}