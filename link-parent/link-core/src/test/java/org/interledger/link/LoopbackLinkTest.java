package org.interledger.link;

import static org.assertj.core.api.Assertions.assertThat;
import static org.interledger.link.LoopbackLink.SIMULATED_REJECT_ERROR_CODE;
import static org.junit.Assert.fail;

import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerConstants;
import org.interledger.core.InterledgerErrorCode;
import org.interledger.core.InterledgerPreparePacket;

import com.google.common.collect.Maps;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

/**
 * Unit tests for {@link LoopbackLink}.
 */
public class LoopbackLinkTest {

  private static final InterledgerAddress OPERATOR_ADDRESS = InterledgerAddress.of("test.foo");

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private PacketRejector packetRejector;

  private LoopbackLink link;

  @Before
  public void setUp() {
    this.packetRejector = new PacketRejector(() -> OPERATOR_ADDRESS);

    this.link = new LoopbackLink(
        () -> OPERATOR_ADDRESS,
        LinkSettings.builder().linkType(LoopbackLink.LINK_TYPE).build(),
        packetRejector
    );
  }

  @Test(expected = RuntimeException.class)
  public void registerLinkHandler() {
    try {
      link.registerLinkHandler(incomingPreparePacket -> null);
    } catch (Exception e) {
      assertThat(e.getMessage())
          .isEqualTo("Loopback links never have incoming data, and thus should not have a registered DataHandler.");
      throw e;
    }
  }

  @Test(expected = NullPointerException.class)
  public void sendPacketWithNull() {
    try {
      link.sendPacket(null);
    } catch (NullPointerException e) {
      assertThat(e.getMessage()).isEqualTo("preparePacket must not be null");
      throw e;
    }
  }

  @Test
  public void sendPacket() {
    this.link = new LoopbackLink(
        () -> OPERATOR_ADDRESS,
        LinkSettings.builder().linkType(LoopbackLink.LINK_TYPE).build(),
        packetRejector
    );
    link.setLinkId(LinkId.of("foo"));

    final InterledgerPreparePacket preparePacket = preparePacket();
    link.sendPacket(preparePacket()).handle(
        fulfillPacket -> {
          assertThat(fulfillPacket.getFulfillment()).isEqualTo(LoopbackLink.LOOPBACK_FULFILLMENT);
          assertThat(fulfillPacket.getData()).isEqualTo(preparePacket.getData());
        },
        rejectPacket -> {
          logger.error("rejectPacket={}", rejectPacket);
          fail("Expected a Fulfill");
        }
    );
  }

  @Test
  public void sendT02Packet() {
    final Map<String, String> customSettings = Maps.newHashMap();
    customSettings.put(SIMULATED_REJECT_ERROR_CODE, InterledgerErrorCode.T02_PEER_BUSY_CODE);
    this.link = new LoopbackLink(
        () -> OPERATOR_ADDRESS,
        LinkSettings.builder().linkType(LoopbackLink.LINK_TYPE).customSettings(customSettings).build(),
        packetRejector
    );
    link.setLinkId(LinkId.of("foo"));

    link.sendPacket(preparePacket()).handle(
        fulfillPacket -> {
          logger.error("interledgerRejectPacket={}", fulfillPacket);
          fail("Expected a Reject");
        },
        rejectPacket -> {
          assertThat(rejectPacket.getCode()).isEqualTo(InterledgerErrorCode.T02_PEER_BUSY);
        }
    );
  }

  @Test(expected = RuntimeException.class)
  public void sendT99Packet() {
    final Map<String, String> customSettings = Maps.newHashMap();
    customSettings.put(SIMULATED_REJECT_ERROR_CODE, InterledgerErrorCode.T99_APPLICATION_ERROR_CODE);
    this.link = new LoopbackLink(
        () -> OPERATOR_ADDRESS,
        LinkSettings.builder().linkType(LoopbackLink.LINK_TYPE).customSettings(customSettings).build(),
        packetRejector
    );
    link.setLinkId(LinkId.of("foo"));

    try {
      link.sendPacket(preparePacket()).handle(
          fulfillPacket -> {
            logger.error("interledgerRejectPacket={}", fulfillPacket);
            fail("Expected a Reject");
          },
          rejectPacket -> {
            logger.error("interledgerRejectPacket={}", rejectPacket);
            fail("Expected a Reject");
          }
      );
    } catch (RuntimeException e) {
      assertThat(e.getMessage()).isEqualTo("T99 APPLICATION ERROR");
      throw e;
    }
  }

  private InterledgerPreparePacket preparePacket() {
    return InterledgerPreparePacket.builder()
        .amount(BigInteger.TEN)
        .executionCondition(InterledgerConstants.ALL_ZEROS_CONDITION)
        .destination(OPERATOR_ADDRESS)
        .expiresAt(Instant.now())
        .data(new byte[32])
        .build();
  }
}
