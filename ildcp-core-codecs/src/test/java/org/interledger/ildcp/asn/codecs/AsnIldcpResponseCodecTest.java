package org.interledger.ildcp.asn.codecs;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import org.interledger.core.InterledgerAddress;
import org.interledger.ildcp.IldcpResponse;
import org.interledger.ildcp.asn.framework.IldcpCodecContextFactory;

import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

/**
 * Unit tests for {@link AsnIldcpResponseCodec}.
 */
public class AsnIldcpResponseCodecTest {

  private static final InterledgerAddress FOO_ADDRESS = InterledgerAddress.of("example.foo");
  private static final String BTC = "BTC";

  private AsnIldcpResponseCodec codec;

  private IldcpResponse RESPONSE = IldcpResponse.builder()
      .clientAddress(FOO_ADDRESS)
      .assetScale((short) 9)
      .assetCode(BTC)
      .build();

  @Before
  public void setUp() {
    codec = new AsnIldcpResponseCodec();
  }

  @Test
  public void decode() {
    codec.encode(RESPONSE);
    assertThat(codec.getCodecAt(0).decode(), is(FOO_ADDRESS));
    assertThat(codec.getCodecAt(1).decode(), is((short) 9));
    assertThat(codec.getCodecAt(2).decode(), is(BTC));
  }

  @Test
  public void encode() {
    codec.encode(RESPONSE);
    final IldcpResponse actual = codec.decode();
    assertThat(actual, is(RESPONSE));
  }

  @Test
  public void readWrite() throws IOException {
    // Write
    final ByteArrayOutputStream os = new ByteArrayOutputStream();
    IldcpCodecContextFactory.oer().write(RESPONSE, os);
    assertThat(Base64.getEncoder().encodeToString(os.toByteArray()), is("C2V4YW1wbGUuZm9vCQNCVEM="));

    // Read
    final ByteArrayInputStream is = new ByteArrayInputStream(os.toByteArray());
    final IldcpResponse decodedValue = IldcpCodecContextFactory.oer().read(IldcpResponse.class, is);
    assertThat(decodedValue, is(RESPONSE));
  }
}