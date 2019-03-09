package org.interledger.ildcp.asn.framework;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.interledger.encoding.asn.framework.CodecContextFactory.OCTET_ENCODING_RULES;

import org.interledger.core.InterledgerAddress;
import org.interledger.encoding.asn.framework.CodecContext;
import org.interledger.encoding.asn.framework.CodecContextFactory;
import org.interledger.ildcp.IldcpRequestPacket;
import org.interledger.ildcp.IldcpResponse;
import org.interledger.ildcp.IldcpResponsePacket;
import org.interledger.ildcp.asn.codecs.AsnIldcpResponseCodec;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.Base64;

/**
 * Unit tests for {@link IldcpCodecContextFactory}.
 */
public class IldcpCodecContextFactoryTest {

  private static final InterledgerAddress FOO_ADDRESS = InterledgerAddress.of("example.foo");
  private static final String BTC = "BTC";
  private static final Instant NOW = Instant.parse("2019-12-25T01:02:03.996Z");

  private static final IldcpResponse TEST_RESPONSE = IldcpResponse.builder()
      .clientAddress(FOO_ADDRESS)
      .assetScale((short) 9)
      .assetCode(BTC)
      .build();

  @Test
  public void testReadWriteRequestPacket() throws IOException {
    final IldcpRequestPacket requestPacket = IldcpRequestPacket.builder().expiresAt(NOW).build();

    final ByteArrayOutputStream os = new ByteArrayOutputStream();
    IldcpCodecContextFactory.oer().write(requestPacket, os);

    final String base64Bytes = Base64.getEncoder().encodeToString(os.toByteArray());
    assertThat(base64Bytes,
        is("DEYAAAAAAAAAADIwMTkxMjI1MDEwMjAzOTk2Zmh6rfhivXdsj8GLjp+OIAiXFIVu4jOzkCpZHQ1fKSULcGVlci5jb25maWcA"));

    final IldcpRequestPacket decodedPacket = IldcpCodecContextFactory.oer()
        .read(IldcpRequestPacket.class, new ByteArrayInputStream(os.toByteArray()));
    assertThat(decodedPacket, is(requestPacket));
  }


  @Test
  public void testReadWriteResponsePacket() throws IOException {
    final ByteArrayOutputStream os = new ByteArrayOutputStream();
    final IldcpResponsePacket responsePacket = IldcpResponsePacket.builder().ildcpResponse(TEST_RESPONSE).build();
    IldcpCodecContextFactory.oer().write(responsePacket, os);

    final IldcpResponsePacket decodedResponsePacket = IldcpCodecContextFactory.oer()
        .read(IldcpResponsePacket.class, new ByteArrayInputStream(os.toByteArray()));

    assertThat(decodedResponsePacket, is(responsePacket));
  }

  @Test
  public void register() throws IOException {
    final CodecContext codecContext = CodecContextFactory.getContext(OCTET_ENCODING_RULES);
    codecContext.register(IldcpResponse.class, AsnIldcpResponseCodec::new);

    final ByteArrayOutputStream os = new ByteArrayOutputStream();
    IldcpCodecContextFactory.oer().write(TEST_RESPONSE, os);

    final String base64Bytes = Base64.getEncoder().encodeToString(os.toByteArray());
    assertThat(base64Bytes, is("C2V4YW1wbGUuZm9vCQNCVEM="));
  }
}