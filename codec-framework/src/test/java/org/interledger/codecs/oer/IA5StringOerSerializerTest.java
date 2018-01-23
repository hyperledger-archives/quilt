package org.interledger.codecs.oer;

import static org.hamcrest.MatcherAssert.assertThat;

import org.interledger.codecs.asn.AsnIA5String;
import org.interledger.codecs.asn.AsnSizeConstraint;
import org.interledger.codecs.framework.CodecContext;
import org.interledger.codecs.framework.CodecContextFactory;

import com.google.common.io.BaseEncoding;
import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.Assert;
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
 * Parameterized unit tests for encoding an instance of {@link String} as an IA5String.
 */
@RunWith(Parameterized.class)
public class IA5StringOerSerializerTest {

  private final String stringValue;
  private final byte[] asn1ByteValue;
  private CodecContext codecContext;

  /**
   * Construct an instance of this parameterized test with the supplied inputs.
   *
   * @param stringValue The expected value, as a {@link String}, of the supplied {@code asn1Bytes}.
   * @param asn1Bytes   A byte array representing octets to be encoded.
   */
  public IA5StringOerSerializerTest(final String stringValue, final byte[] asn1Bytes) {
    this.stringValue = stringValue;
    this.asn1ByteValue = asn1Bytes;
  }

  /**
   * The data for this test...
   */
  @Parameters
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][] {
        // [input_value][num_octets_written][byte_values]
        // 0
        {"", BaseEncoding.base16().decode("00")},
        // 1
        {"a", BaseEncoding.base16().decode("0161")},
        // 2
        {"abc", BaseEncoding.base16().decode("03616263")},
        // 3
        {"hello world", BaseEncoding.base16().decode("0B68656C6C6F20776F726C64")},
        // 4
        {"g.test.foo", BaseEncoding.base16().decode("0A672E746573742E666F6F")},
        // 4
        {"g.test.1024.AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
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
            + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
            BaseEncoding.base16()
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
                + "41414141414141414141414141414141")},});
  }

  /**
   * Test setup.
   */
  @Before
  public void setUp() throws Exception {
    // Register the codec to be tested...
    codecContext = CodecContextFactory.getContext(CodecContextFactory.OCTET_ENCODING_RULES)
        .register(String.class, () -> new AsnIA5String(AsnSizeConstraint.unconstrained()));
  }

  @Test
  public void read() throws Exception {
    // This stream allows the codec to read the asn1Bytes...
    ByteArrayInputStream inputStream = new ByteArrayInputStream(asn1ByteValue);

    // Assert that the coded read bytes that equal what the harness put into octetBytes.
    final String actualValue = codecContext.read(String.class, inputStream);
    MatcherAssert.assertThat(actualValue, CoreMatchers.is(stringValue));
  }

  @Test
  public void write() throws Exception {
    // Allow the AsnObjectCodec to write to 'outputStream'
    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    codecContext.write(stringValue, outputStream);

    // Assert that the bytes written to 'outputStream' match the contents that the harness put
    // into asn1ByteValue.
    Assert.assertArrayEquals(asn1ByteValue, outputStream.toByteArray());
  }

  @Test
  public void writeThenRead() throws Exception {
    // Write octets...
    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    codecContext.write(stringValue, outputStream);

    // Read octets...
    final ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
    final String actual = codecContext.read(String.class, inputStream);
    assertThat(actual, CoreMatchers.is(stringValue));

    // Write octets again...
    final ByteArrayOutputStream outputStream2 = new ByteArrayOutputStream();
    codecContext.write(actual, outputStream2);

    // Assert originally written bytes equals newly written bytes.
    Assert.assertArrayEquals(outputStream.toByteArray(), outputStream2.toByteArray());
  }
}
