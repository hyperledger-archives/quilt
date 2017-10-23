package org.interledger.codecs.oer;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertArrayEquals;

import org.interledger.codecs.CodecContext;
import org.interledger.codecs.oer.OerLengthPrefixCodec.OerLengthPrefix;

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
 * Parameterized unit tests for encoding an instance of {@link OerUint8Codec}.
 */
@RunWith(Parameterized.class)
public class OerLengthPrefixCodecTest {

  private CodecContext codecContext;
  private OerLengthPrefixCodec oerLengthPrefixCodec;
  private final int expectedPayloadLength;
  private final byte[] asn1OerBytes;

  /**
   * Construct an instance of this parameterized test with the supplied inputs.
   *
   * @param expectedPayloadLength An integer representing the length of a payload that should be
   *                              encoded as a length-prefix.
   * @param asn1OerBytes          The expected value, in binary, of the supplied {@code
   *                              expectedPayloadLength}.
   */
  public OerLengthPrefixCodecTest(final int expectedPayloadLength, final byte[] asn1OerBytes) {
    this.expectedPayloadLength = expectedPayloadLength;
    this.asn1OerBytes = asn1OerBytes;
  }

  /**
   * The data for this test...
   */
  @Parameters
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][]{
        // [input_value][num_octets_written][byte_values]
        // 0
        {0, BaseEncoding.base16().decode("00")},
        // 1
        {1, BaseEncoding.base16().decode("01")},
        // 2
        {2, BaseEncoding.base16().decode("02")},
        // 3 (the last number that can be encoded in 1 overall octet)
        {127, BaseEncoding.base16().decode("7F")},
        // 4 (the first number that can be encoded in 1 value octet)
        {128, BaseEncoding.base16().decode("8180")},
        // 5
        {129, BaseEncoding.base16().decode("8181")},
        // 6 (the last number that can be encoded in 1 value octet)
        {255, BaseEncoding.base16().decode("81FF")},
        // 7 (the first number that can be encoded in 2 value octets)
        {256, BaseEncoding.base16().decode("820100")},
        // 8 (Last number that can be encoded in 2 value octets).
        {65535, BaseEncoding.base16().decode("82FFFF")},

        // 9 (First number that can be encoded in 3 value octets).
        {65536, BaseEncoding.base16().decode("83010000")},

        // 10 (Last number that can be encoded in 3 value octets).
        {16777215, BaseEncoding.base16().decode("83FFFFFF")},

        // 11 (First number that can be encoded in 4 value octets).
        {16777216, BaseEncoding.base16().decode("8401000000")},

        // 11 (First number that can be encoded in 4 value octets).
        {Integer.MAX_VALUE, BaseEncoding.base16().decode("847FFFFFFF")},});
  }

  /**
   * Test setup.
   */
  @Before
  public void setUp() throws Exception {
    // Register the codec to be tested...
    oerLengthPrefixCodec = new OerLengthPrefixCodec();
    codecContext = new CodecContext().register(OerLengthPrefix.class, oerLengthPrefixCodec);
  }

  @Test
  public void read() throws Exception {
    // This stream allows the codec to read the asn1Bytes...
    final ByteArrayInputStream inputStream = new ByteArrayInputStream(this.asn1OerBytes);
    final int actualPayloadLength =
        oerLengthPrefixCodec.read(codecContext, inputStream).getLength();
    assertThat(actualPayloadLength, is(expectedPayloadLength));
  }

  @Test
  public void write() throws Exception {
    // Allow the Codec to write to 'outputStream'
    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    oerLengthPrefixCodec.write(codecContext, new OerLengthPrefix(expectedPayloadLength),
        outputStream);
    assertArrayEquals(this.asn1OerBytes, outputStream.toByteArray());
  }

  @Test
  public void writeThenRead() throws Exception {
    // Write octets...
    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    oerLengthPrefixCodec.write(codecContext, new OerLengthPrefix(this.expectedPayloadLength),
        outputStream);

    // Read octets...
    final ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
    final OerLengthPrefix actual = oerLengthPrefixCodec.read(codecContext, inputStream);
    assertThat(actual.getLength(), is(expectedPayloadLength));

    // Write octets again...
    final ByteArrayOutputStream outputStream2 = new ByteArrayOutputStream();
    oerLengthPrefixCodec.write(codecContext, actual, outputStream2);

    // Assert originally written bytes equals newly written bytes.
    assertArrayEquals(outputStream.toByteArray(), outputStream2.toByteArray());
  }

}
