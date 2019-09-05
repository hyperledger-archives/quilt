package org.interledger.link;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.nullValue;
import static org.interledger.link.LoopbackStatefulLink.SIMULATED_REJECT_ERROR_CODE;
import static org.junit.Assert.fail;

import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerConstants;
import org.interledger.core.InterledgerErrorCode;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerRejectPacket;
import org.interledger.link.events.LinkConnectionEventEmitter;

import com.google.common.collect.Maps;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

/**
 * Unit tests for {@link LoopbackStatefulLink}.
 */
public class LoopbackLinkTest {

  private static final InterledgerAddress OPERATOR_ADDRESS = InterledgerAddress.of("test.foo");

  private static final InterledgerRejectPacket EXPECTED_REJECT_PACKET = InterledgerRejectPacket.builder()
      .triggeredBy(OPERATOR_ADDRESS)
      .code(InterledgerErrorCode.F00_BAD_REQUEST)
      .message("error message")
      .build();

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  @Mock
  LinkConnectionEventEmitter linkConnectionEventEmitterMock;

  private PacketRejector packetRejector;

  private LoopbackStatefulLink link;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    this.packetRejector = new PacketRejector(() -> Optional.of(OPERATOR_ADDRESS));

    this.link = new LoopbackStatefulLink(
        () -> Optional.of(OPERATOR_ADDRESS),
        LinkSettings.builder().linkType(LoopbackStatefulLink.LINK_TYPE).build(),
        linkConnectionEventEmitterMock,
        packetRejector
    );
  }

  @Test
  public void doConnect() throws ExecutionException, InterruptedException {
    assertThat(link.doConnect().get(), is(nullValue()));
  }

  @Test
  public void doDisconnect() throws ExecutionException, InterruptedException {
    assertThat(link.doDisconnect().get(), is(nullValue()));
  }

  @Test(expected = RuntimeException.class)
  public void registerLinkHandler() {
    try {
      link.registerLinkHandler(incomingPreparePacket -> null);
    } catch (Exception e) {
      assertThat(
          e.getMessage(),
          is("Loopback links never have incoming data, and thus should not have a registered DataHandler.")
      );
      throw e;
    }
  }

  @Test(expected = NullPointerException.class)
  public void sendPacketWithNull() {
    try {
      link.sendPacket(null);
    } catch (NullPointerException e) {
      assertThat(e.getMessage(), is("preparePacket must not be null"));
      throw e;
    }
  }

  @Test
  public void sendPacket() {
    this.link = new LoopbackStatefulLink(
        () -> Optional.of(OPERATOR_ADDRESS),
        LinkSettings.builder().linkType(LoopbackStatefulLink.LINK_TYPE).build(),
        linkConnectionEventEmitterMock,
        packetRejector
    );
    link.setLinkId(LinkId.of("foo"));

    final InterledgerPreparePacket preparePacket = preparePacket();
    link.sendPacket(preparePacket()).handle(
        fulfillPacket -> {
          assertThat(fulfillPacket.getFulfillment(), is(LoopbackStatefulLink.LOOPBACK_FULFILLMENT));
          assertThat(fulfillPacket.getData(), is(preparePacket.getData()));
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
    this.link = new LoopbackStatefulLink(
        () -> Optional.of(OPERATOR_ADDRESS),
        LinkSettings.builder().linkType(LoopbackStatefulLink.LINK_TYPE).customSettings(customSettings).build(),
        linkConnectionEventEmitterMock,
        packetRejector
    );
    link.setLinkId(LinkId.of("foo"));

    link.sendPacket(preparePacket()).handle(
        fulfillPacket -> {
          logger.error("interledgerRejectPacket={}", fulfillPacket);
          fail("Expected a Reject");
        },
        rejectPacket -> {
          assertThat(rejectPacket.getCode(), is(InterledgerErrorCode.T02_PEER_BUSY));
        }
    );
  }

  @Test(expected = RuntimeException.class)
  public void sendT99Packet() {
    final Map<String, String> customSettings = Maps.newHashMap();
    customSettings.put(SIMULATED_REJECT_ERROR_CODE, InterledgerErrorCode.T99_APPLICATION_ERROR_CODE);
    this.link = new LoopbackStatefulLink(
        () -> Optional.of(OPERATOR_ADDRESS),
        LinkSettings.builder().linkType(LoopbackStatefulLink.LINK_TYPE).customSettings(customSettings).build(),
        linkConnectionEventEmitterMock,
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
      assertThat(e.getMessage(), is("T99 APPLICATION ERROR"));
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
