package org.interledger.encoding.asn.codecs;

/*-
 * ========================LICENSE_START=================================
 * Interledger Codec Framework
 * %%
 * Copyright (C) 2017 - 2019 Hyperledger and its contributors
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =========================LICENSE_END==================================
 */

import static org.assertj.core.api.Assertions.assertThat;

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
   * @param expectedUint         The expected value, as a {@link BigInteger}, of {@code expectedEncodedBytes}, once
   *                             encoded.
   * @param expectedEncodedBytes The expected encoded value, in bytes, of {@code expectedUint}.
   */
  public AsnUintCodecTest(
      final BigInteger expectedUint, final byte[] expectedEncodedBytes) {
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
        },
        {
            BigInteger.valueOf(2147483647L),
            BaseEncoding.base16().decode("7FFFFFFF")
        },
        {
            BigInteger.valueOf(2147483648L),
            BaseEncoding.base16().decode("80000000")
        },
        {
            BigInteger.valueOf(9223372036854775807L),
            BaseEncoding.base16().decode("7FFFFFFFFFFFFFFF")
        },
        {
            new BigInteger("9223372036854775808"),
            BaseEncoding.base16().decode("8000000000000000")
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
      assertThat(e.getMessage()).isEqualTo("value must be positive or zero");
      throw e;
    }
  }

  @Test
  public void decode() {
    codec.setBytes(this.expectedEncodedBytes);
    assertThat(codec.decode()).isEqualTo(this.expectedUint);
  }

  @Test
  public void encode() {
    codec.encode(expectedUint);
    assertThat(codec.getBytes()).isEqualTo(this.expectedEncodedBytes);
  }

  @Test
  public void encodeThenDecode() {
    codec.encode(expectedUint);
    assertThat(codec.decode()).isEqualTo(this.expectedUint);
  }
}
