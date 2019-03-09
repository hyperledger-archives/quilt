package org.interledger.ildcp.asn.codecs;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerFulfillPacket;
import org.interledger.core.InterledgerFulfillment;
import org.interledger.ildcp.IldcpResponse;
import org.interledger.ildcp.IldcpResponsePacket;
import org.interledger.ildcp.asn.framework.IldcpCodecContextFactory;

import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

/**
 * Unit tests for {@link AsnIldcpResponsePacketDataCodec}.
 */
public class AsnIldcpResponsePacketDataCodecTest {

  private static final InterledgerAddress FOO_ADDRESS = InterledgerAddress.of("example.foo");
  private static final String BTC = "BTC";
  private static final IldcpResponse TEST_RESPONSE = IldcpResponse.builder()
      .clientAddress(FOO_ADDRESS)
      .assetScale((short) 9)
      .assetCode(BTC)
      .build();

  private AsnIldcpResponsePacketDataCodec codec;
  private IldcpResponsePacket packet;

  @Before
  public void setUp() {
    packet = IldcpResponsePacket.builder().ildcpResponse(TEST_RESPONSE).build();
    codec = new AsnIldcpResponsePacketDataCodec();
  }

  @Test
  public void encode() {
    codec.encode(packet);

    assertThat(codec.getValueAt(0), is(InterledgerFulfillment.of(new byte[32]))); // fulfillment

    final byte[] encodedIldcpResponseBytes = codec.getValueAt(1);
    assertThat(Base64.getEncoder().encodeToString(encodedIldcpResponseBytes), is("C2V4YW1wbGUuZm9vCQNCVEM="));
  }

  @Test
  public void decode() throws IOException {
    final ByteArrayOutputStream os = new ByteArrayOutputStream();
    IldcpCodecContextFactory.oer().write(packet, os);

    final IldcpResponsePacket actual = IldcpCodecContextFactory.oer()
        .read(IldcpResponsePacket.class, new ByteArrayInputStream(os.toByteArray()));
    assertThat(actual, is(packet));
  }

}