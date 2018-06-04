package org.interledger.btp;

import static org.junit.Assert.*;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

public class BtpErrorTest {

  private static final long REQUEST_ID = 1;
  private static final String ERROR_NAME = "Test Error";
  private static final byte[] ERROR_DATA = new byte[] {0, 1, 2};
  private static final Instant TRIGGERED_AT = Instant.now();
  private static final BtpSubProtocols SUB_PROTOCOLS = new BtpSubProtocols();

  static {
    SUB_PROTOCOLS.add(BtpSubProtocol.builder()
    .protocolName("TEST")
    .contentType(BtpSubProtocolContentType.MIME_TEXT_PLAIN_UTF8)
    .data("Test Data".getBytes(StandardCharsets.UTF_8))
    .build());
  }

  private final BtpError error = BtpError.builder()
      .requestId(REQUEST_ID)
      .errorCode(BtpErrorCode.F00_NotAcceptedError)
      .errorName(ERROR_NAME)
      .errorData(ERROR_DATA)
      .triggeredAt(TRIGGERED_AT)
      .subProtocols(SUB_PROTOCOLS)
      .build();


  @Test
  public void getType() {
    assertEquals(error.getType(), BtpMessageType.ERROR);
  }

  @Test
  public void getErrorCode() {
    assertEquals(error.getErrorCode(), BtpErrorCode.F00_NotAcceptedError);
  }

  @Test
  public void getErrorName() {
    assertEquals(error.getErrorName(), ERROR_NAME);
  }

  @Test
  public void getTriggeredAt() {
    assertEquals(error.getTriggeredAt(), TRIGGERED_AT);
  }

  @Test
  public void getErrorData() {
    assertArrayEquals(error.getErrorData(), ERROR_DATA);
  }

}