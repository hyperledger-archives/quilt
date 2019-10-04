package org.interledger.link;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerErrorCode;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerRejectPacket;

import com.google.common.primitives.UnsignedLong;
import org.junit.Before;
import org.junit.Test;

import java.time.Instant;

/**
 * Unit tests for {@link PacketRejector}.
 */
public class PacketRejectorTest {

  private static final LinkId LINK_ID = LinkId.of("fooLink");
  private static final InterledgerPreparePacket PREPARE_PACKET = InterledgerPreparePacket.builder()
      .expiresAt(Instant.now())
      .amount(UnsignedLong.ONE)
      .destination(InterledgerAddress.of("test.destination"))
      .executionCondition(PingLoopbackLink.PING_PROTOCOL_CONDITION)
      .build();
  private static final InterledgerAddress OPERATOR_ADDRESS = InterledgerAddress.of("test.operator");

  private PacketRejector packetRejector;

  @Before
  public void setup() {
    this.packetRejector = new PacketRejector(() -> OPERATOR_ADDRESS);
  }

  @Test(expected = NullPointerException.class)
  public void nullConstructor() {
    try {
      new PacketRejector(null);
      fail();
    } catch (Exception e) {
      assertThat(e.getMessage()).isEqualTo("operatorAddressSupplier must not be null");
      throw e;
    }
  }

  @Test(expected = NullPointerException.class)
  public void nullLinkId() {
    try {
      packetRejector.reject(null, PREPARE_PACKET, InterledgerErrorCode.T02_PEER_BUSY, "");
      fail();
    } catch (NullPointerException e) {
      assertThat(e.getMessage()).isEqualTo("rejectingLinkId must not be null");
      throw e;
    }
  }

  @Test(expected = NullPointerException.class)
  public void nullPreparePacket() {
    try {
      packetRejector.reject(LINK_ID, null, InterledgerErrorCode.T02_PEER_BUSY, "");
      fail();
    } catch (NullPointerException e) {
      assertThat(e.getMessage()).isEqualTo("preparePacket must not be null");
      throw e;
    }
  }

  @Test(expected = NullPointerException.class)
  public void nullErrorCode() {
    try {
      packetRejector.reject(LINK_ID, PREPARE_PACKET, null, "");
      fail();
    } catch (NullPointerException e) {
      assertThat(e.getMessage()).isEqualTo("errorCode must not be null");
      throw e;
    }
  }

  @Test(expected = NullPointerException.class)
  public void nullErrorMessage() {
    try {
      packetRejector.reject(LINK_ID, PREPARE_PACKET, InterledgerErrorCode.T02_PEER_BUSY, null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e.getMessage()).isEqualTo("errorMessage must not be null");
      throw e;
    }
  }

  @Test
  public void testReject() {
    InterledgerRejectPacket reject = packetRejector
        .reject(LINK_ID, PREPARE_PACKET, InterledgerErrorCode.T02_PEER_BUSY, "the error");

    assertThat(reject.getCode()).isEqualTo(InterledgerErrorCode.T02_PEER_BUSY);
    assertThat(reject.getTriggeredBy().get()).isEqualTo(OPERATOR_ADDRESS);
    assertThat(reject.getMessage()).isEqualTo("the error");
    assertThat(reject.getData().length).isEqualTo(0);
  }

}
