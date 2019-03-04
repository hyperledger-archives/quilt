package org.interledger.ildcp;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.fail;

import org.junit.Test;

/**
 * Unit tests for {@link IldcpResponsePacket}.
 */
public class IldcpResponsePacketTest {

  @Test
  public void testBuilder() {
    final IldcpResponsePacket actual = IldcpResponsePacket.builder().data(new byte[32]).build();

    assertThat(actual.getFulfillment(), is(IldcpResponsePacket.EXECUTION_FULFILLMENT));
    assertThat(actual.getData(), is(new byte[32]));
  }

  @Test(expected = IllegalStateException.class)
  public void testEmptyBuilder() {
    try {
      IldcpResponsePacket.builder().build();
      fail();
    } catch (IllegalStateException e) {
      assertThat(
          e.getMessage(),
          is("Cannot build IldcpResponsePacket, some of required attributes are not set [data]")
      );
      throw e;
    }
  }

  @Test
  public void testEqualsHashcode() {

    final IldcpResponsePacket first = IldcpResponsePacket.builder().data(new byte[32]).build();
    final IldcpResponsePacket second = IldcpResponsePacket.builder().data(new byte[32]).build();
    final IldcpResponsePacket third = IldcpResponsePacket.builder().data(new byte[64]).build();

    assertThat(first.equals(second), is(true));
    assertThat(second.equals(first), is(true));
    assertThat(third, is(not(first)));

    assertThat(first.hashCode(), is(second.hashCode()));
    assertThat(second.hashCode(), is(first.hashCode()));
    assertThat(third, is(not(first.hashCode())));
  }

  @Test
  public void testToString() {
    final IldcpResponsePacket first = IldcpResponsePacket.builder().data(new byte[32]).build();

    assertThat(
        first.toString(),
        is("IldcpResponsePacket{fulfillment=ImmutableInterledgerFulfillment[preimage=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=, condition=Condition{hash=Zmh6rfhivXdsj8GLjp+OIAiXFIVu4jOzkCpZHQ1fKSU=}], data=[0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]}")
    );
  }
}