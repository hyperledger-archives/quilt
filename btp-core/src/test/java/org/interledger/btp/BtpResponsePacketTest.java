package org.interledger.btp;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

import org.interledger.btp.BtpSubProtocol.ContentType;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Unit tests for {@link BtpResponsePacket}.
 */
public class BtpResponsePacketTest {

  private static final long REQUEST_ID = 1;
  private static final BtpSubProtocols SUB_PROTOCOLS = new BtpSubProtocols();
  private static final BtpSubProtocol SUB_PROTOCOL_1 = BtpSubProtocol.builder()
      .protocolName("TEST")
      .contentType(ContentType.MIME_TEXT_PLAIN_UTF8)
      .data("Test Data".getBytes(StandardCharsets.UTF_8))
      .build();

  private static final BtpSubProtocol SUB_PROTOCOL_2 = BtpSubProtocol.builder()
      .protocolName("TEST2")
      .contentType(ContentType.MIME_TEXT_PLAIN_UTF8)
      .data("Test Data".getBytes(StandardCharsets.UTF_8))
      .build();

  private static final BtpResponse BTP_RESPONSE = BtpResponse.builder()
      .requestId(REQUEST_ID)
      .subProtocols(SUB_PROTOCOLS)
      .build();

  private static final BtpError BTP_ERROR = BtpError.builder()
      .requestId(REQUEST_ID)
      .subProtocols(SUB_PROTOCOLS)
      .triggeredAt(Instant.now())
      .errorCode(BtpErrorCode.F00_NotAcceptedError)
      .build();

  static {
    SUB_PROTOCOLS.add(SUB_PROTOCOL_1);
    SUB_PROTOCOLS.add(SUB_PROTOCOL_2);
  }

  @Test
  public void handleBtpResponse() {
    final AtomicBoolean handled = new AtomicBoolean();
    assertThat(handled.get(), is(false));
    BTP_RESPONSE.handle(
        (btpResponse) -> handled.set(true),
        (btpError) -> fail("btpError handler should not be called.")
    );
    assertThat(handled.get(), is(true));
  }

  @Test
  public void handleBtpError() {
    final AtomicBoolean handled = new AtomicBoolean();
    assertThat(handled.get(), is(false));
    BTP_ERROR.handle(
        (btpResponse) -> fail("btpResponse handler should not be called."),
        (btpError) -> handled.set(true)
    );
    assertThat(handled.get(), is(true));
  }

  @Test
  public void handleAndReturnBtpResponse() {
    final AtomicBoolean handled = new AtomicBoolean();
    assertThat(handled.get(), is(false));
    BtpResponsePacket response = BTP_RESPONSE.handleAndReturn(
        (btpResponse) -> handled.set(true),
        (btpError) -> fail("btpError handler should not be called.")
    );
    assertThat(handled.get(), is(true));
    assertThat(response, is(BTP_RESPONSE));
  }

  @Test
  public void handleAndReturnBtpError() {
    final AtomicBoolean handled = new AtomicBoolean();
    assertThat(handled.get(), is(false));
    BtpResponsePacket response = BTP_ERROR.handleAndReturn(
        (btpResponse) -> fail("btpResponse handler should not be called."),
        (btpError) -> handled.set(true)
    );
    assertThat(handled.get(), is(true));
    assertThat(response, is(BTP_ERROR));
  }

  @Test
  public void testMap() {
    assertThat(BTP_RESPONSE.map(($) -> 1, ($) -> 2), is(1));
    assertThat(BTP_ERROR.map(($) -> 1, ($) -> 2), is(2));
  }

}
