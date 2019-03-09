package org.interledger.ildcp.asn.codecs;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

import org.interledger.core.InterledgerAddress;
import org.interledger.encoding.asn.framework.CodecException;
import org.interledger.ildcp.IldcpRequestPacket;
import org.interledger.ildcp.IldcpResponse;
import org.interledger.ildcp.IldcpResponsePacket;
import org.interledger.ildcp.asn.codecs.AsnIldcpPacketCodec.IldcpPacketTypes;

import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link AsnIldcpPacketCodec}.
 */
public class AsnIldcpPacketCodecTest {

  private static final InterledgerAddress FOO_ADDRESS = InterledgerAddress.of("example.foo");
  private static final String BTC = "BTC";

  private AsnIldcpPacketCodec codec;

  private IldcpResponse RESPONSE = IldcpResponse.builder()
      .clientAddress(FOO_ADDRESS)
      .assetScale((short) 9)
      .assetCode(BTC)
      .build();

  @Before
  public void setup() {
    this.codec = new AsnIldcpPacketCodec();
  }


  @Test
  public void encodeDecode() {
    final IldcpRequestPacket packet = IldcpRequestPacket.builder().build();
    codec.encode(packet);
    assertThat(codec.getCodecAt(0).decode(), is((short) 12));
    assertThat(codec.getCodecAt(1).decode(), is(packet));
  }

  @Test
  public void onTypeIdChangedToRequest() {
    final IldcpRequestPacket packet = IldcpRequestPacket.builder().build();
    codec.onTypeIdChanged(IldcpPacketTypes.REQUEST);
    codec.encode(packet);
    assertThat(codec.getCodecAt(0).decode(), is((short) 12));
    assertThat(codec.getCodecAt(1).decode(), is(packet));
  }

  @Test
  public void onTypeIdChangedToResponse() {
    final IldcpResponsePacket packet = IldcpResponsePacket.builder().ildcpResponse(RESPONSE).build();
    codec.onTypeIdChanged(IldcpPacketTypes.RESPONSE);
    codec.encode(packet);
    assertThat(codec.getCodecAt(0).decode(), is((short) 13));
    assertThat(codec.getCodecAt(1).decode(), is(packet));
  }

  @Test(expected = CodecException.class)
  public void onTypeIdChangedToUnsupported() {
    try {
      codec.onTypeIdChanged((short) 50);
      fail();
    } catch (CodecException e) {
      assertThat(e.getMessage(), is("Unknown IL-DCP packet type code: 50"));
      throw e;
    }
  }

}