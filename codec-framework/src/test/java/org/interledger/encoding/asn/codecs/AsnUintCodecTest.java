package org.interledger.encoding.asn.codecs;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import com.google.common.io.BaseEncoding;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;

@RunWith(Parameterized.class)
public class AsnUintCodecTest {

  private final BigInteger expectedUint;
  private final byte[] expectedEncodedBytes;

  private AsnUintCodec codec;

  /**
   * Construct an instance of this parameterized test with the supplied inputs.
   *
   * @param expectedUint         The expected value, as a {@link BigInteger}, of {@code
   *                             expectedEncodedBytes}, once encoded.
   * @param expectedEncodedBytes The expected encoded value, in bytes, of {@code expectedUint}.
   */
  public AsnUintCodecTest(
      final BigInteger expectedUint, final byte[] expectedEncodedBytes
  ) {
    this.expectedUint = Objects.requireNonNull(expectedUint);
    this.expectedEncodedBytes = Objects.requireNonNull(expectedEncodedBytes);
  }

  /**
   * The data for this test...
   */
  @Parameterized.Parameters
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][] {
        {
            BigInteger.ZERO,
            BaseEncoding.base16().decode("00")
        },
        {
            BigInteger.valueOf(1L),
            BaseEncoding.base16().decode("01")
        },
        {
            BigInteger.TEN,
            BaseEncoding.base16().decode("0A")
        },
        {
            BigInteger.valueOf(15L),
            BaseEncoding.base16().decode("0F")
        },
        {
            BigInteger.valueOf(255L),
            BaseEncoding.base16().decode("FF")
        },
        {
            BigInteger.valueOf(1024L),
            BaseEncoding.base16().decode("0400")
        },
        {
            BigInteger.valueOf(16385L),
            BaseEncoding.base16().decode("4001")
        },
        {
            BigInteger.valueOf(65535L),
            BaseEncoding.base16().decode("FFFF")
        }
    });
  }

  @Before
  public void setUp() {
    this.codec = new AsnUintCodec();
  }

  @Test(expected = IllegalArgumentException.class)
  public void encodeNegative() {
    try {
      codec.encode(BigInteger.valueOf(-1L));
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage(), is("value must be positive or zero"));
      throw e;
    }
  }

  @Test
  public void decode() {
    codec.setBytes(this.expectedEncodedBytes);
    assertThat(codec.decode(), is(this.expectedUint));
  }

  @Test
  public void encode() {
    codec.encode(expectedUint);
    assertThat(codec.getBytes(), is(this.expectedEncodedBytes));
  }

  @Test
  public void encodeThenDecode() {
    codec.encode(expectedUint);
    assertThat(codec.decode(), is(this.expectedUint));
  }
}