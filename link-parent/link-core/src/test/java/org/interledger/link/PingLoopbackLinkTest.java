package org.interledger.link;

import static org.assertj.core.api.Assertions.assertThat;
import static org.interledger.link.PingLoopbackLink.PING_PROTOCOL_CONDITION;
import static org.interledger.link.PingLoopbackLink.PING_PROTOCOL_FULFILLMENT;
import static org.junit.Assert.fail;

import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerCondition;
import org.interledger.core.InterledgerErrorCode;
import org.interledger.core.InterledgerFulfillment;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerRejectPacket;
import org.interledger.core.DateUtils;

import com.google.common.primitives.UnsignedLong;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Unit tests for {@link PingLoopbackLink}.
 */
public class PingLoopbackLinkTest {

  private static final InterledgerAddress OPERATOR_ADDRESS = InterledgerAddress.of("test.foo");

  private static final InterledgerRejectPacket EXPECTED_REJECT_PACKET = InterledgerRejectPacket.builder()
      .triggeredBy(OPERATOR_ADDRESS)
      .code(InterledgerErrorCode.F00_BAD_REQUEST)
      .message("error message")
      .build();

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  @Mock
  LinkSettings linkSettingsMock;

  private PingLoopbackLink link;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    this.link = new PingLoopbackLink(() -> OPERATOR_ADDRESS, linkSettingsMock);
  }

  @Test(expected = RuntimeException.class)
  public void registerLinkHandler() {
    try {
      link.registerLinkHandler(incomingPreparePacket -> null);
    } catch (Exception e) {
      assertThat(e.getMessage()).isEqualTo(
          "PingLoopback links never have incoming data, and thus should not have a registered DataHandler.");
      throw e;
    }
  }

  @Test(expected = NullPointerException.class)
  public void sendPacketWithNull() {
    try {
      link.sendPacket(null);
    } catch (NullPointerException e) {
      assertThat(e.getMessage()).isEqualTo("preparePacket must not be null!");
      throw e;
    }
  }

  @Test
  public void testFulfillmentFromBase64() {
    byte[] bytes = Base64.getDecoder().decode("cGluZ3BpbmdwaW5ncGluZ3BpbmdwaW5ncGluZ3Bpbmc=");
    final InterledgerFulfillment expectedFulfillment = InterledgerFulfillment.of(bytes);

    assertThat(expectedFulfillment).isEqualTo(PING_PROTOCOL_FULFILLMENT);
    assertThat(expectedFulfillment.getCondition()).isEqualTo(PING_PROTOCOL_CONDITION);
    assertThat(expectedFulfillment.validateCondition(PING_PROTOCOL_CONDITION)).isEqualTo(true);
  }

  @Test
  public void testFulfillmentFromAscii() {
    final InterledgerFulfillment expectedFulfillment = InterledgerFulfillment
        .of("pingpingpingpingpingpingpingping".getBytes(StandardCharsets.US_ASCII));

    assertThat(expectedFulfillment).isEqualTo(PING_PROTOCOL_FULFILLMENT);
    assertThat(expectedFulfillment.getCondition()).isEqualTo(PING_PROTOCOL_CONDITION);
    assertThat(expectedFulfillment.validateCondition(PING_PROTOCOL_CONDITION)).isEqualTo(true);
  }

  @Test
  public void testConditionFromBase64() {
    byte[] bytes = Base64.getDecoder().decode("jAC8DGFPZPfh4AtZpXuvXFe2oRmpDVSvSJg2oT+bx34=");
    final InterledgerCondition expectedCondition = InterledgerCondition.of(bytes);

    assertThat(expectedCondition).isEqualTo(PING_PROTOCOL_CONDITION);
    assertThat(PING_PROTOCOL_FULFILLMENT.getCondition()).isEqualTo(PING_PROTOCOL_CONDITION);
    assertThat(PING_PROTOCOL_FULFILLMENT.validateCondition(PING_PROTOCOL_CONDITION)).isEqualTo(true);
  }

  @Test
  public void sendPacketWithInvalidCondition() {
    final InterledgerPreparePacket preparePacket = InterledgerPreparePacket.builder()
        .amount(UnsignedLong.valueOf(10L))
        .executionCondition(InterledgerCondition.of(new byte[32]))
        .destination(OPERATOR_ADDRESS)
        .expiresAt(DateUtils.now())
        .build();

    link.sendPacket(preparePacket).handle(fulfillPacket -> {
          logger.error("InterledgerFulfillPacket: {}", fulfillPacket);
          fail("Expected a Reject!");
        },
        rejectPacket -> {
          assertThat(rejectPacket.getCode()).isEqualTo(InterledgerErrorCode.F00_BAD_REQUEST);
          assertThat(rejectPacket.getTriggeredBy().isPresent()).isEqualTo(true);
          assertThat(rejectPacket.getTriggeredBy()).isEqualTo(EXPECTED_REJECT_PACKET.getTriggeredBy());
        }
    );
  }

  /**
   * A packet with an invalid destination address should never make its way into this link, so this tests expects a
   * Fulfill even though that's counter-intuitive -- this is because the Link itself does not destination address
   * checking.
   */
  @Test
  public void sendPacketWithInvalidAddress() {
    final InterledgerPreparePacket preparePacket = InterledgerPreparePacket.builder()
        .amount(UnsignedLong.valueOf(10L))
        .executionCondition(PING_PROTOCOL_CONDITION)
        .destination(InterledgerAddress.of("example.foo"))
        .expiresAt(DateUtils.now())
        .build();

    link.sendPacket(preparePacket).handle(
        fulfillPacket -> assertThat(fulfillPacket.getFulfillment()).isEqualTo(PING_PROTOCOL_FULFILLMENT),
        rejectPacket -> {
          logger.error("InterledgerRejectPacket: {}", rejectPacket);
          fail("Expected a Fulfill!");
        }
    );
  }

  @Test
  public void sendPacket() {
    final InterledgerPreparePacket preparePacket = InterledgerPreparePacket.builder()
        .amount(UnsignedLong.valueOf(10L))
        .executionCondition(PING_PROTOCOL_CONDITION)
        .destination(OPERATOR_ADDRESS)
        .expiresAt(DateUtils.now())
        .build();

    link.sendPacket(preparePacket).handle(
        fulfillPacket -> assertThat(fulfillPacket.getFulfillment()).isEqualTo(PING_PROTOCOL_FULFILLMENT),
        rejectPacket -> {
          logger.error("InterledgerRejectPacket: {}", rejectPacket);
          fail("Expected a Fulfill!");
        }
    );
  }

}
