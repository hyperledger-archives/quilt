package org.interledger.encoding.asn.serializers.oer;

/*-
 * ========================LICENSE_START=================================
 * Interledger Codec Framework
 * %%
 * Copyright (C) 2017 - 2018 Hyperledger and its contributors
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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.util.Arrays;
import java.util.Collection;

/**
 * Parameterized unit tests for {@link OerLengthSerializer}.
 */
@RunWith(Parameterized.class)
public class OerLengthSerializerToIntTest {

  private final int expectedPayloadLength;
  private final byte[] numberBytes;

  /**
   * Construct an instance of this parameterized test with the supplied inputs.
   *
   * @param expectedPayloadLength An integer representing an integer payload.
   * @param numberBytes           The expected value, in binary, of the supplied {@code expectedPayloadLength}.
   */
  public OerLengthSerializerToIntTest(final int expectedPayloadLength, final byte[] numberBytes) {
    this.expectedPayloadLength = expectedPayloadLength;
    this.numberBytes = numberBytes;
  }

  /**
   * The data for this test...
   */
  @Parameters
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][] {
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
        {128, BaseEncoding.base16().decode("80")},
        // 5
        {129, BaseEncoding.base16().decode("81")},
        // 6 (the last number that can be encoded in 1 value octet)
        {255, BaseEncoding.base16().decode("FF")},
        // 7 (the first number that can be encoded in 2 value octets)
        {256, BaseEncoding.base16().decode("0100")},
        // 8 (Last number that can be encoded in 2 value octets).
        {65535, BaseEncoding.base16().decode("FFFF")},
        // 9 (First number that can be encoded in 3 value octets).
        {65536, BaseEncoding.base16().decode("010000")},
        // 10 (Last number that can be encoded in 3 value octets).
        {16777215, BaseEncoding.base16().decode("FFFFFF")},
        // 11 (First number that can be encoded in 4 value octets).
        {16777216, BaseEncoding.base16().decode("01000000")},
        // 12 (Largest signed Integer value).
        {Integer.MAX_VALUE, BaseEncoding.base16().decode("7FFFFFFF")},
        // 13 (Smallest signed Integer value).
        {Integer.MIN_VALUE, BaseEncoding.base16().decode("80000000")},
        // 14 (Largest negative signed Integer value).
        {-1, BaseEncoding.base16().decode("FFFFFFFF")},
    });
  }

  @Test
  public void testToInt() {
    assertThat(OerLengthSerializer.toInt(numberBytes)).isEqualTo(expectedPayloadLength);
  }

}
