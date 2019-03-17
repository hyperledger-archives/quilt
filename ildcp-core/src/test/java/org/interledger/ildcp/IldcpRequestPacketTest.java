package org.interledger.ildcp;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import org.junit.Test;

import java.math.BigInteger;
import java.time.Instant;

/**
 * Unit tests for {@link IldcpRequestPacket}.
 */
public class IldcpRequestPacketTest {

  @Test
  public void testBuilder() {
    final IldcpRequestPacket actual = IldcpRequestPacket.builder().build();
    assertThat(actual.getAmount(), is(BigInteger.ZERO));
    assertThat(actual.getDestination(), is(IldcpRequestPacket.PEER_DOT_CONFIG));
    assertThat(actual.getExecutionCondition(), is(IldcpRequestPacket.EXECUTION_CONDITION));
    assertThat(actual.getData(), is(new byte[0]));
  }

  @Test
  public void testBuilderWithCustomExpiry() {
    final Instant expiresAt = Instant.parse("2019-12-25T01:02:03.996Z");
    final IldcpRequestPacket actual = IldcpRequestPacket.builder().expiresAt(expiresAt).build();

    assertThat(actual.getDestination(), is(IldcpRequestPacket.PEER_DOT_CONFIG));
    assertThat(actual.getExpiresAt(), is(expiresAt));
    assertThat(actual.getAmount(), is(BigInteger.ZERO));
    assertThat(actual.getExecutionCondition(), is(IldcpRequestPacket.EXECUTION_CONDITION));
    assertThat(actual.getData(), is(new byte[0]));
  }

  @Test
  public void testEqualsHashcode() {
    final Instant expiresAt = Instant.parse("2019-12-25T01:02:03.996Z");
    final IldcpRequestPacket first = IldcpRequestPacket.builder().expiresAt(expiresAt).build();
    final IldcpRequestPacket second = IldcpRequestPacket.builder().expiresAt(expiresAt).build();
    final IldcpRequestPacket third = IldcpRequestPacket.builder().amount(BigInteger.TEN).build();

    assertThat(first.equals(second), is(true));
    assertThat(second.equals(first), is(true));
    assertThat(third, is(not(first)));

    assertThat(first.hashCode(), is(second.hashCode()));
    assertThat(second.hashCode(), is(first.hashCode()));
    assertThat(third, is(not(first.hashCode())));
  }

  @Test
  public void testToString() {
    final Instant expiresAt = Instant.parse("2019-12-25T01:02:03.996Z");
    final IldcpRequestPacket first = IldcpRequestPacket.builder().expiresAt(expiresAt).build();

    assertThat(
        first.toString().startsWith(
            "IldcpRequestPacket{destination=InterledgerAddress{value=peer.config}, amount=0, executionCondition="
                + "Condition{hash=Zmh6rfhivXdsj8GLjp+OIAiXFIVu4jOzkCpZHQ1fKSU=}, expiresAt=2019-12-25T01:02:03.996Z,"
                + " data=[B@"),
        is(true)
    );
  }
}