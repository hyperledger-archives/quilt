package org.interledger.btp;

import static org.junit.Assert.*;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

public class BtpRuntimeExceptionTest {
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

  @Test
  public void toBtpError() {

    BtpRuntimeException exception = new BtpRuntimeException(BtpErrorCode.F00_NotAcceptedError, ERROR_NAME);
    BtpError error = exception.toBtpError(REQUEST_ID);

    assertEquals(error.getErrorCode(), BtpErrorCode.F00_NotAcceptedError);
    assertEquals(error.getErrorName(), ERROR_NAME);
    assertEquals(error.getTriggeredAt(), exception.getTriggeredAt());

    assertTrue(new String(error.getErrorData()).startsWith("org.interledger.btp.BtpRuntimeException: Test Error"));

  }

  @Test
  public void toBtpErrorWithSubProtocols() {

    BtpRuntimeException exception = new BtpRuntimeException(BtpErrorCode.F00_NotAcceptedError, ERROR_NAME);
    BtpError error = exception.toBtpError(REQUEST_ID, SUB_PROTOCOLS);

    assertEquals(error.getErrorCode(), BtpErrorCode.F00_NotAcceptedError);
    assertEquals(error.getErrorName(), ERROR_NAME);
    assertEquals(error.getTriggeredAt(), exception.getTriggeredAt());

    assertTrue(new String(error.getErrorData()).startsWith("org.interledger.btp.BtpRuntimeException: Test Error"));

    assertEquals(error.getSubProtocols(), SUB_PROTOCOLS);
  }
}