package org.interledger.codecs.oer;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertArrayEquals;

import org.interledger.codecs.CodecContext;
import org.interledger.codecs.oer.OerLengthPrefixCodec.OerLengthPrefix;
import org.interledger.codecs.oer.OerOctetStringCodec.OerOctetString;

import com.google.common.io.BaseEncoding;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.Collection;

/**
 * Parameterized unit tests for encoding an instance of {@link OerOctetStringCodec}.
 */
@RunWith(Parameterized.class)
public class OerOctetStringCodecTest {

  private CodecContext codecContext;
  private OerOctetStringCodec oerOctetStringCodec;
  private final byte[] asn1ByteValue;
  private final byte[] octetBytes;

  /**
   * Construct an instance of this parameterized test with the supplied inputs.
   *
   * @param asn1Bytes  A byte array representing octets to be encoded.
   * @param octetBytes The expected value, in binary, of the supplied {@code asn1Bytes}.
   */
  public OerOctetStringCodecTest(final byte[] asn1Bytes, final byte[] octetBytes) {
    this.asn1ByteValue = asn1Bytes;
    this.octetBytes = octetBytes;
  }

  /**
   * The data for this test...
   */
  @Parameters
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][]{
        // [asn1_bytes][octet_bytes]
        // 0
        {BaseEncoding.base16().decode("00"), "".getBytes()},
        // 1
        {BaseEncoding.base16().decode("0101"), BaseEncoding.base16().decode("01")},
        // 2
        {BaseEncoding.base16().decode("03616263"), "abc".getBytes()},
        // 3
        {BaseEncoding.base16().decode("0B68656C6C6F20776F726C64"), "hello world".getBytes()},
        // 4
        {BaseEncoding.base16().decode("0A672E746573742E666F6F"), "g.test.foo".getBytes()},
        // 5
        {BaseEncoding.base16()
            .decode("8203FF672E746573742E313032342E4141414141414141414"
            + "1414141414141414141414141414141414141414141414141414141414141414141414141"
            + "4141414141414141414141414141414141414141414141414141414141414141414141414"
            + "1414141414141414141414141414141414141414141414141414141414141414141414141"
            + "4141414141414141414141414141414141414141414141414141414141414141414141414"
            + "1414141414141414141414141414141414141414141414141414141414141414141414141"
            + "4141414141414141414141414141414141414141414141414141414141414141414141414"
            + "1414141414141414141414141414141414141414141414141414141414141414141414141"
            + "4141414141414141414141414141414141414141414141414141414141414141414141414"
            + "1414141414141414141414141414141414141414141414141414141414141414141414141"
            + "4141414141414141414141414141414141414141414141414141414141414141414141414"
            + "1414141414141414141414141414141414141414141414141414141414141414141414141"
            + "4141414141414141414141414141414141414141414141414141414141414141414141414"
            + "1414141414141414141414141414141414141414141414141414141414141414141414141"
            + "4141414141414141414141414141414141414141414141414141414141414141414141414"
            + "1414141414141414141414141414141414141414141414141414141414141414141414141"
            + "4141414141414141414141414141414141414141414141414141414141414141414141414"
            + "1414141414141414141414141414141414141414141414141414141414141414141414141"
            + "4141414141414141414141414141414141414141414141414141414141414141414141414"
            + "1414141414141414141414141414141414141414141414141414141414141414141414141"
            + "4141414141414141414141414141414141414141414141414141414141414141414141414"
            + "1414141414141414141414141414141414141414141414141414141414141414141414141"
            + "4141414141414141414141414141414141414141414141414141414141414141414141414"
            + "1414141414141414141414141414141414141414141414141414141414141414141414141"
            + "4141414141414141414141414141414141414141414141414141414141414141414141414"
            + "1414141414141414141414141414141414141414141414141414141414141414141414141"
            + "4141414141414141414141414141414141414141414141414141414141414141414141414"
            + "1414141414141414141414141414141414141414141414141414141414141414141414141"
            + "41414141414141414141414141414141"),

            ("g.test.1024.AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA").getBytes()},});
  }

  /**
   * Test setup.
   */
  @Before
  public void setUp() throws Exception {
    // Register the codec to be tested...
    final OerLengthPrefixCodec oerLengthPrefixCode = new OerLengthPrefixCodec();
    oerOctetStringCodec = new OerOctetStringCodec();
    codecContext = new CodecContext().register(OerLengthPrefix.class, oerLengthPrefixCode)
        .register(OerOctetString.class, oerOctetStringCodec);
  }

  @Test
  public void readTest() throws Exception {
    // This stream allows the codec to read the asn1Bytes...
    ByteArrayInputStream inputStream = new ByteArrayInputStream(asn1ByteValue);

    // Assert that the coded read bytes that equal what the harness put into octetBytes.
    final byte[] actualValue = oerOctetStringCodec.read(codecContext, inputStream).getValue();
    assertThat(actualValue, is(octetBytes));
  }

  @Test
  public void writeTest() throws Exception {
    // Allow the Codec to write to 'outputStream'
    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    oerOctetStringCodec.write(codecContext, new OerOctetString(octetBytes), outputStream);

    // Assert that the bytes written to 'outputStream' match the contents that the harness put
    // into asn1ByteValue.
    assertArrayEquals(asn1ByteValue, outputStream.toByteArray());
  }

  @Test
  public void writeReadWriteTest() throws Exception {
    // Write octets...
    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    oerOctetStringCodec.write(codecContext, new OerOctetString(octetBytes), outputStream);

    // Read octets...
    final ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
    final OerOctetString actual = oerOctetStringCodec.read(codecContext, inputStream);
    assertThat(actual.getValue(), is(octetBytes));

    // Write octets again...
    final ByteArrayOutputStream outputStream2 = new ByteArrayOutputStream();
    oerOctetStringCodec.write(codecContext, actual, outputStream2);

    // Assert originally written bytes equals newly written bytes.
    assertArrayEquals(outputStream.toByteArray(), outputStream2.toByteArray());
  }
}
