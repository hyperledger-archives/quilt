package org.interledger.ildcp;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.fail;

import org.interledger.core.InterledgerAddress;

import org.junit.Test;

/**
 * Unit tests for {@link IldcpResponsePacket}.
 */
public class IldcpResponsePacketTest {

  private static final InterledgerAddress FOO_ADDRESS = InterledgerAddress.of("example.foo");
  private static final String BTC = "BTC";

  final IldcpResponse RESPONSE = IldcpResponse.builder()
      .clientAddress(FOO_ADDRESS)
      .assetScale((short) 9)
      .assetCode(BTC)
      .build();

  @Test
  public void testBuilder() {
    final IldcpResponsePacket actual = IldcpResponsePacket.builder().ildcpResponse(RESPONSE).data(new byte[32]).build();

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
          is("Cannot build IldcpResponsePacket, some of required attributes are not set [ildcpResponse]")
      );
      throw e;
    }
  }

  @Test
  public void testEqualsHashcode() {

    final IldcpResponsePacket first = IldcpResponsePacket.builder().ildcpResponse(RESPONSE).data(new byte[32]).build();
    final IldcpResponsePacket second = IldcpResponsePacket.builder().ildcpResponse(RESPONSE).data(new byte[32]).build();
    final IldcpResponsePacket third = IldcpResponsePacket.builder().ildcpResponse(RESPONSE).data(new byte[64]).build();

    assertThat(first.equals(second), is(true));
    assertThat(second.equals(first), is(true));
    assertThat(third, is(not(first)));

    assertThat(first.hashCode(), is(second.hashCode()));
    assertThat(second.hashCode(), is(first.hashCode()));
    assertThat(third, is(not(first.hashCode())));
  }

  @Test
  public void testToString() {
    final IldcpResponsePacket first = IldcpResponsePacket.builder().ildcpResponse(RESPONSE).data(new byte[0]).build();

    assertThat(
        first.toString(),
        is("IldcpResponsePacket{fulfillment=ImmutableInterledgerFulfillment["
            + "preimage=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=, "
            + "condition=Condition{hash=Zmh6rfhivXdsj8GLjp+OIAiXFIVu4jOzkCpZHQ1fKSU=}], "
            + "data=[], "
            + "ildcpResponse=IldcpResponse{clientAddress=InterledgerAddress{value=example.foo}, assetCode=BTC, assetScale=9}"
            + "}")
    );
  }
}