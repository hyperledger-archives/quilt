package org.hyperledger.quilt.codecs.oer;

import com.google.common.io.BaseEncoding;
import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.hyperledger.quilt.codecs.framework.CodecContext;
import org.hyperledger.quilt.codecs.oer.OerUint8Codec.OerUint8;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

/**
 * Parameterized unit tests for encoding an instance of {@link OerUint8Codec}.
 */
@RunWith(Parameterized.class)
public class OerUint8CodecTest {

  private CodecContext codecContext;
  private OerUint8Codec oerUint8Codec;
  private final int inputValue;
  private final byte[] asn1OerBytes;

  /**
   * Construct an instance of this parameterized test with the supplied inputs.
   *
   * @param inputValue   A {@code int} representing the unsigned 8bit integer to write in OER
   *                     encoding.
   * @param asn1OerBytes The expected value, in binary, of the supplied {@code intValue}.
   */
  public OerUint8CodecTest(final int inputValue, final byte[] asn1OerBytes) {
    this.inputValue = inputValue;
    this.asn1OerBytes = asn1OerBytes;
  }

  /**
   * The data for this test...
   */
  @Parameters
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][]{
        // Input Value as a int; Expected byte[] in ASN.1
        // 0
        {0, BaseEncoding.base16().decode("00")},
        // 1
        {1, BaseEncoding.base16().decode("01")},
        // 2
        {2, BaseEncoding.base16().decode("02")},
        // 3
        {127, BaseEncoding.base16().decode("7F")},
        // 4
        {128, BaseEncoding.base16().decode("80")},
        // 5
        {254, BaseEncoding.base16().decode("FE")},
        // 6
        {255, BaseEncoding.base16().decode("FF")},});
  }

  /**
   * Test setup.
   */
  @Before
  public void setUp() throws Exception {
    // Register the codec to be tested...
    oerUint8Codec = new OerUint8Codec();
    codecContext = new CodecContext().register(OerUint8.class, oerUint8Codec);
  }

  @Test
  public void read() throws Exception {
    final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(asn1OerBytes);
    final int actualValue = oerUint8Codec.read(codecContext, byteArrayInputStream).getValue();
    MatcherAssert.assertThat(actualValue, CoreMatchers.is(inputValue));
  }

  @Test
  public void write() throws Exception {
    final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    oerUint8Codec.write(codecContext, new OerUint8(inputValue), byteArrayOutputStream);
    MatcherAssert.assertThat(byteArrayOutputStream.toByteArray(), CoreMatchers.is(asn1OerBytes));
  }

  @Test
  public void writeThenRead() throws Exception {
    // Write...
    final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    oerUint8Codec.write(codecContext, new OerUint8(inputValue), byteArrayOutputStream);
    MatcherAssert.assertThat(byteArrayOutputStream.toByteArray(), CoreMatchers.is(asn1OerBytes));

    // Read...
    final ByteArrayInputStream byteArrayInputStream =
        new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
    final OerUint8 decodedValue = oerUint8Codec.read(codecContext, byteArrayInputStream);

    // Write...
    final ByteArrayOutputStream byteArrayOutputStream2 = new ByteArrayOutputStream();
    oerUint8Codec.write(codecContext, decodedValue, byteArrayOutputStream2);
    MatcherAssert.assertThat(byteArrayOutputStream2.toByteArray(), CoreMatchers.is(asn1OerBytes));
  }

  /**
   * Validate an overflow amount.
   */
  @Test(expected = IllegalArgumentException.class)
  public void write8BitUInt_Overflow() throws IOException {
    try {
      final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
      oerUint8Codec.write(codecContext, new OerUint8(256), byteArrayOutputStream);
    } catch (IllegalArgumentException e) {
      MatcherAssert.assertThat(e.getMessage(),
          CoreMatchers.is("Interledger UInt8 values may only contain up to 8 bits!"));
      throw e;
    }
  }
}
