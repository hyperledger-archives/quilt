package org.interledger.link;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.fail;

import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerErrorCode;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerRejectPacket;

import org.junit.Before;
import org.junit.Test;

import java.math.BigInteger;
import java.time.Instant;
import java.util.Optional;

/**
 * Unit tests for {@link PacketRejector}.
 */
public class PacketRejectorTest {

  private static final LinkId LINK_ID = LinkId.of("fooLink");
  private static final InterledgerPreparePacket PREPARE_PACKET = InterledgerPreparePacket.builder()
      .expiresAt(Instant.now())
      .amount(BigInteger.ONE)
      .destination(InterledgerAddress.of("test.destination"))
      .executionCondition(PingLoopbackLink.PING_PROTOCOL_CONDITION)
      .build();
  private static final InterledgerAddress OPERATOR_ADDRESS = InterledgerAddress.of("test.operator");

  private PacketRejector packetRejector;

  @Before
  public void setup() {
    this.packetRejector = new PacketRejector(() -> Optional.of(OPERATOR_ADDRESS));
  }

  @Test
  public void nullConstructor() {
    try {
      new PacketRejector(null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e.getMessage(), is("operatorAddressSupplier must not be null"));
      throw e;
    }
  }

  @Test(expected = NullPointerException.class)
  public void nullLinkId() {
    try {
      packetRejector.reject(null, PREPARE_PACKET, InterledgerErrorCode.T02_PEER_BUSY, "");
      fail();
    } catch (NullPointerException e) {
      assertThat(e.getMessage(), is("rejectingLinkId must not be null"));
      throw e;
    }
  }

  @Test(expected = NullPointerException.class)
  public void nullPreparePacket() {
    try {
      packetRejector.reject(LINK_ID, null, InterledgerErrorCode.T02_PEER_BUSY, "");
      fail();
    } catch (NullPointerException e) {
      assertThat(e.getMessage(), is("preparePacket must not be null"));
      throw e;
    }
  }

  @Test(expected = NullPointerException.class)
  public void nullErrorCode() {
    try {
      packetRejector.reject(LINK_ID, PREPARE_PACKET, null, "");
      fail();
    } catch (NullPointerException e) {
      assertThat(e.getMessage(), is("errorCode must not be null"));
      throw e;
    }
  }

  @Test(expected = NullPointerException.class)
  public void nullErrorMessage() {
    try {
      packetRejector.reject(LINK_ID, PREPARE_PACKET, InterledgerErrorCode.T02_PEER_BUSY, null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e.getMessage(), is("errorMessage must not be null"));
      throw e;
    }
  }

  @Test
  public void testReject() {
    InterledgerRejectPacket reject = packetRejector
        .reject(LINK_ID, PREPARE_PACKET, InterledgerErrorCode.T02_PEER_BUSY, "the error");

    assertThat(reject.getCode(), is(InterledgerErrorCode.T02_PEER_BUSY));
    assertThat(reject.getTriggeredBy().get(), is(OPERATOR_ADDRESS));
    assertThat(reject.getMessage(), is("the error"));
    assertThat(reject.getData().length, is(0));
  }

}
