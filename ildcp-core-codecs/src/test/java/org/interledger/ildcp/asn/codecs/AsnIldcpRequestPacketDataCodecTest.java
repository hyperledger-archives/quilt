package org.interledger.ildcp.asn.codecs;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import org.interledger.ildcp.IldcpRequestPacket;
import org.interledger.ildcp.asn.framework.IldcpCodecContextFactory;

import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.time.Instant;

/**
 * Unit tests for {@link AsnIldcpRequestPacketDataCodec}.
 */
public class AsnIldcpRequestPacketDataCodecTest {

  private static final Instant NOW = Instant.parse("2019-12-25T01:02:03.590Z");
  private AsnIldcpRequestPacketDataCodec codec;
  private IldcpRequestPacket packet;

  @Before
  public void setUp() {
    packet = IldcpRequestPacket.builder().expiresAt(NOW).build();
    codec = new AsnIldcpRequestPacketDataCodec();
  }

  @Test
  public void encode() {
    codec.encode(packet);

    assertThat(codec.getValueAt(0), is(BigInteger.ZERO)); // Amount
    assertThat(codec.getValueAt(1), is(NOW)); // Expiry
    assertThat(codec.getValueAt(2), is(IldcpRequestPacket.EXECUTION_CONDITION)); // Condition
    assertThat(codec.getValueAt(3), is(IldcpRequestPacket.PEER_DOT_CONFIG)); // Dest Address
    assertThat(codec.getValueAt(4), is(new byte[0])); // Data
  }

  @Test
  public void decode() throws IOException {
    final ByteArrayOutputStream os = new ByteArrayOutputStream();
    IldcpCodecContextFactory.oer().write(packet, os);

    final IldcpRequestPacket actual = IldcpCodecContextFactory.oer()
        .read(IldcpRequestPacket.class, new ByteArrayInputStream(os.toByteArray()));
    assertThat(actual, is(packet));
  }
}