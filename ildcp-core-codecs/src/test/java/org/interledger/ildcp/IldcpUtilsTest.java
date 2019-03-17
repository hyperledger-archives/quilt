package org.interledger.ildcp;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.fail;

import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerFulfillPacket;
import org.interledger.core.InterledgerFulfillment;
import org.interledger.ildcp.asn.codecs.IldcpCodecException;

import org.junit.Test;

/**
 * Unit tests for {@link IldcpUtils}.
 */
public class IldcpUtilsTest {

  private static final InterledgerAddress FOO_ADDRESS = InterledgerAddress.of("example.foo");
  private static final String BTC = "BTC";

  private IldcpResponse ILDCP_RESPONSE = IldcpResponse.builder()
      .clientAddress(FOO_ADDRESS)
      .assetScale((short) 9)
      .assetCode(BTC)
      .build();

  @Test
  public void fromIldcpResponse() {
    final IldcpResponsePacket expectedResponse = IldcpResponsePacket.builder()
        .ildcpResponse(ILDCP_RESPONSE)
        .build();

    final IldcpResponsePacket packet = IldcpUtils.fromIldcpResponse(ILDCP_RESPONSE);
    assertThat(packet.getFulfillment(), is(expectedResponse.getFulfillment()));
    assertThat(packet.getIldcpResponse(), is(ILDCP_RESPONSE));
  }

  @Test
  public void toIldcpResponse() {
    final IldcpResponsePacket responsePacket = IldcpUtils.fromIldcpResponse(ILDCP_RESPONSE);
    final InterledgerFulfillPacket fulfillPacket = IldcpResponsePacket.builder()
        .ildcpResponse(ILDCP_RESPONSE)
        .data(responsePacket.getData())
        .build();
    final IldcpResponse actualResponse = IldcpUtils.toIldcpResponse(fulfillPacket);
    assertThat(actualResponse, is(ILDCP_RESPONSE));
  }

  @Test(expected = IldcpCodecException.class)
  public void toIldcpResponseWithNonZeroButInvalidData() {
    try {
      final IldcpResponsePacket packet = IldcpResponsePacket.builder()
          .ildcpResponse(ILDCP_RESPONSE).data(new byte[1]).build();
      IldcpUtils.toIldcpResponse(packet);
      fail();
    } catch (IldcpCodecException e) {
      assertThat(e.getMessage(), is("Packet must have a data payload containing an encoded instance of IldcpResponse"));
      throw e;
    }
  }

  @Test(expected = IldcpCodecException.class)
  public void toIldcpResponseWithNoData() {
    try {
      final InterledgerFulfillPacket fulfillmentWithoutData = InterledgerFulfillPacket.builder()
          .fulfillment(InterledgerFulfillment.of(new byte[32]))
          .build();
      IldcpUtils.toIldcpResponse(fulfillmentWithoutData);
    } catch (IldcpCodecException e) {
      assertThat(e.getMessage(), is("Packet must have a data payload containing an encoded instance of IldcpResponse"));
      throw e;
    }
  }
}