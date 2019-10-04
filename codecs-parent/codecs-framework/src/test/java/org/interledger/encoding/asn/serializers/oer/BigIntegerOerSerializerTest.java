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

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;

/**
 * Parameterized unit tests for encoding an instance of {@link BigInteger}.
 */
@RunWith(Parameterized.class)
public class BigIntegerOerSerializerTest extends AbstractSerializerTest<BigInteger> {

  /**
   * Construct an instance of this parameterized test with the supplied inputs.
   *
   * @param inputValue   A {@code int} representing the unsigned 8bit integer to write in OER encoding.
   * @param asn1OerBytes The expected value, in binary, of the supplied {@code intValue}.
   */
  public BigIntegerOerSerializerTest(final BigInteger inputValue, final byte[] asn1OerBytes) {
    super(inputValue, BigInteger.class, asn1OerBytes);
  }

  /**
   * The data for this test...
   */
  @Parameters
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][]
        {
            // Input Value as a long; Expected byte[] in ASN.1
            // 0
            {BigInteger.ZERO, Base64.getDecoder().decode("AQA=")},
            // 1
            {BigInteger.ONE, Base64.getDecoder().decode("AQE=")},
            // 2
            {new BigInteger("2"), Base64.getDecoder().decode("AQI=")},
            // 3
            {new BigInteger("254"), Base64.getDecoder().decode("Af4=")},
            // 4
            {new BigInteger("255"), Base64.getDecoder().decode("Af8=")},

            // Two Bytes (16 bits)
            // 5
            {new BigInteger("256"), Base64.getDecoder().decode("AgEA")},
            // 6
            {new BigInteger("257"), Base64.getDecoder().decode("AgEB")},
            // 7
            {new BigInteger("65534"), Base64.getDecoder().decode("Av/+")},
            // 8
            {new BigInteger("65535"), Base64.getDecoder().decode("Av//")},

            // Three Bytes (24 bits)
            // 9
            {new BigInteger("65536"), Base64.getDecoder().decode("AwEAAA==")},
            // 10
            {new BigInteger("65537"), Base64.getDecoder().decode("AwEAAQ==")},
            // 11
            {new BigInteger("16777214"), Base64.getDecoder().decode("A////g==")},
            // 12
            {new BigInteger("16777215"), Base64.getDecoder().decode("A////w==")},

            // Four Bytes (32 bits)
            // 13
            {new BigInteger("16777216"), Base64.getDecoder().decode("BAEAAAA=")},
            // 14
            {new BigInteger("16777217"), Base64.getDecoder().decode("BAEAAAE=")},
            // 15
            {new BigInteger("4294967294"), Base64.getDecoder().decode("BP////4=")},
            // 16
            {new BigInteger("4294967295"), Base64.getDecoder().decode("BP////8=")},

            // Five Bytes (40 bits)
            // 17
            {new BigInteger("4294967296"), Base64.getDecoder().decode("BQEAAAAA")},
            // 18
            {new BigInteger("4294967297"), Base64.getDecoder().decode("BQEAAAAB")},
            // 19
            {new BigInteger("1099511627774"), Base64.getDecoder().decode("Bf/////+")},
            // 20
            {new BigInteger("1099511627775"), Base64.getDecoder().decode("Bf//////")},

            // Six Bytes (48 bits)
            // 21
            {new BigInteger("1099511627776"), Base64.getDecoder().decode("BgEAAAAAAA==")},
            // 22
            {new BigInteger("1099511627777"), Base64.getDecoder().decode("BgEAAAAAAQ==")},
            // 23
            {new BigInteger("281474976710654"), Base64.getDecoder().decode("Bv///////g==")},
            // 24
            {new BigInteger("281474976710655"), Base64.getDecoder().decode("Bv///////w==")},

            // Seven Bytes (56 bits)
            // 25
            {new BigInteger("281474976710656"), Base64.getDecoder().decode("BwEAAAAAAAA=")},
            // 26
            {new BigInteger("281474976710657"), Base64.getDecoder().decode("BwEAAAAAAAE=")},
            // 27
            {new BigInteger("72057594037927934"), Base64.getDecoder().decode("B/////////4=")},
            // 28 (max 7-bit long value)
            {new BigInteger("72057594037927935"), Base64.getDecoder().decode("B/////////8=")},

            // 29
            {new BigInteger("9223372036854775805"), Base64.getDecoder().decode("CH/////////9")},
            // 30
            {new BigInteger("9223372036854775806"), Base64.getDecoder().decode("CH/////////+")},
            // 31
            {new BigInteger("9223372036854775807"), Base64.getDecoder().decode("CH//////////")},

            // 32 Eight bytes, beyond Long.MAX_VALUE
            {new BigInteger("9223372036854775808"), Base64.getDecoder().decode("CIAAAAAAAAAA")},

            // 33
            {new BigInteger("9223372036854775809"), Base64.getDecoder().decode("CIAAAAAAAAAB")},
            // 33
            {new BigInteger("18446744073709551614"), Base64.getDecoder().decode("CP/////////+")},
            // 34
            {new BigInteger("18446744073709551615"), Base64.getDecoder().decode("CP//////////")},
        }
    );
  }
}
