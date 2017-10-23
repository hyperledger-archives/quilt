package org.interledger.codecs.oer;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.interledger.codecs.CodecContext;
import org.interledger.codecs.oer.OerUint256Codec.OerUint256;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.Collection;

// TODO: Need negative tests

/**
 * Parameterized unit tests for encoding an instance of {@link OerUint256Codec}.
 */
@RunWith(Parameterized.class)
public class OerUint256CodecTest {

  private CodecContext codecContext;
  private OerUint256Codec oerUint256Codec;
  private final byte[] inputValue;
  private final byte[] asn1OerBytes;

  /**
   * Construct an instance of this parameterized test with the supplied inputs.
   *
   * @param inputValue   A {@code byte[]} representing the unsigned 256bit integer to write in OER
   *                     encoding.
   * @param asn1OerBytes The expected value, in binary, of the supplied {@code intValue}.
   */
  public OerUint256CodecTest(final byte[] inputValue, final byte[] asn1OerBytes) {
    this.inputValue = inputValue;
    this.asn1OerBytes = asn1OerBytes;
  }

  /**
   * The data for this test...
   */
  @Parameters
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][]{
        // Input Value as a byte[]; Expected same byte[] in ASN.1
        {new byte[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0},
            new byte[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0}},
        {new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 1, 2, 3, 4, 5, 6,
            7, 8, 9, 0, 1, 2},
            new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 1, 2, 3, 4, 5,
                6, 7, 8, 9, 0, 1, 2}},});
  }

  /**
   * Test setup.
   */
  @Before
  public void setUp() throws Exception {
    // Register the codec to be tested...
    oerUint256Codec = new OerUint256Codec();
    codecContext = new CodecContext().register(OerUint256.class, oerUint256Codec);
  }

  @Test
  public void read() throws Exception {
    final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(asn1OerBytes);
    final byte[] actualValue = oerUint256Codec.read(codecContext, byteArrayInputStream).getValue();
    assertThat(actualValue, is(inputValue));
  }

  @Test
  public void write() throws Exception {
    final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    oerUint256Codec.write(codecContext, new OerUint256(inputValue), byteArrayOutputStream);
    assertThat(byteArrayOutputStream.toByteArray(), is(this.asn1OerBytes));
  }

  @Test
  public void writeThenRead() throws Exception {
    // Write...
    final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    oerUint256Codec.write(codecContext, new OerUint256(inputValue), byteArrayOutputStream);
    assertThat(byteArrayOutputStream.toByteArray(), is(asn1OerBytes));

    // Read...
    final ByteArrayInputStream byteArrayInputStream =
        new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
    final OerUint256 decodedValue = oerUint256Codec.read(codecContext, byteArrayInputStream);

    // Write...
    final ByteArrayOutputStream byteArrayOutputStream2 = new ByteArrayOutputStream();
    oerUint256Codec.write(codecContext, decodedValue, byteArrayOutputStream2);
    assertThat(byteArrayOutputStream2.toByteArray(), is(asn1OerBytes));
  }
}
