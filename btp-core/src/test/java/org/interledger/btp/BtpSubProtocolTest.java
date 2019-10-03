package org.interledger.btp;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class BtpSubProtocolTest {

  private static final long REQUEST_ID = 1;
  private static final BtpSubProtocols SUB_PROTOCOLS = new BtpSubProtocols();
  private static final BtpSubProtocol SUB_PROTOCOL = BtpSubProtocol.builder()
      .protocolName("TEST")
      .contentType(BtpSubProtocol.ContentType.MIME_TEXT_PLAIN_UTF8)
      .data("Test Data".getBytes(StandardCharsets.UTF_8))
      .build();

  static {
    SUB_PROTOCOLS.add(SUB_PROTOCOL);
  }

  @Test
  public void testSubProtocol() {
    final BtpMessage message = BtpMessage.builder()
        .requestId(REQUEST_ID)
        .subProtocols(SUB_PROTOCOLS)
        .build();

    assertEquals(message.getPrimarySubProtocol().getProtocolName(), "TEST");
    assertEquals(message.getPrimarySubProtocol().getContentType(), BtpSubProtocol.ContentType.MIME_TEXT_PLAIN_UTF8);

    BtpSubProtocol protocol =  BtpSubProtocol.builder().protocolName("TESTING").build();
    assertEquals(protocol.getContentType(), BtpSubProtocol.ContentType.MIME_APPLICATION_OCTET_STREAM);
    assertEquals(protocol.getProtocolName(), "TESTING");
    assertEquals(protocol.getData().length, 0);
    assertArrayEquals(protocol.getData(), new byte[0]);

    assertTrue(message.hasSubProtocol("TEST"));
    assertFalse(message.hasSubProtocol("TESTING"));

    assertTrue(message.getSubProtocols().hasSubProtocol("TEST"));
    assertFalse(message.getSubProtocols().hasSubProtocol("TESTING"));

    String protocolToString = "BtpSubProtocol{contentType=MIME_APPLICATION_OCTET_STREAM, protocolName=TESTING, data=}";
    assertEquals(protocol.toString(), protocolToString);
  }

  @Test
  public void recreatingBtpSubProtocols() {
    BtpSubProtocols subProtocolsFromPrimary = BtpSubProtocols.fromPrimarySubProtocol(SUB_PROTOCOL);
    assertEquals(subProtocolsFromPrimary, SUB_PROTOCOLS);

    BtpSubProtocol subProtocol = SUB_PROTOCOLS.getPrimarySubProtocol();
    assertEquals(subProtocol, SUB_PROTOCOL);
  }

  @Test
  public void checkIfSubProtocolsExist() {
    Optional<BtpSubProtocol> subProtocol = SUB_PROTOCOLS.getSubProtocol("TEST");
    subProtocol.ifPresent(btpSubProtocol -> assertEquals(btpSubProtocol, SUB_PROTOCOL));
  }
}
