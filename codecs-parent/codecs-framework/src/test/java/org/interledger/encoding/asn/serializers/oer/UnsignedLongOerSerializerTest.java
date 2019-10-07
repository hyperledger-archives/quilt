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

import com.google.common.primitives.Longs;
import com.google.common.primitives.UnsignedLong;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.util.Arrays;
import java.util.Collection;

/**
 * Parameterized unit tests for encoding an instance of {@link UnsignedLong}.
 */
@RunWith(Parameterized.class)
public class UnsignedLongOerSerializerTest extends AbstractSerializerTest<UnsignedLong> {

  /**
   * Construct an instance of this parameterized test with the supplied inputs.
   *
   * @param inputValue   A {@code int} representing the unsigned 8bit integer to write in OER encoding.
   * @param asn1OerBytes The expected value, in binary, of the supplied {@code intValue}.
   */
  public UnsignedLongOerSerializerTest(final UnsignedLong inputValue, final byte[] asn1OerBytes) {
    super(inputValue, UnsignedLong.class, asn1OerBytes);
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
            {UnsignedLong.ZERO, Longs.toByteArray(0L)},
            // 1
            {UnsignedLong.ONE, Longs.toByteArray(1L)},
            // 2
            {UnsignedLong.valueOf("2"), Longs.toByteArray(2L)},
            // 3
            {UnsignedLong.valueOf("254"), Longs.toByteArray(254L)},
            // 4
            {UnsignedLong.valueOf("255"), Longs.toByteArray(255L)},

            // Two Bytes (16 bits)
            // 5
            {UnsignedLong.valueOf("256"), Longs.toByteArray(256L)},
            // 6
            {UnsignedLong.valueOf("257"), Longs.toByteArray(257L)},
            // 7
            {UnsignedLong.valueOf("65534"), Longs.toByteArray(65534L)},
            // 8
            {UnsignedLong.valueOf("65535"), Longs.toByteArray(65535L)},

            // Three Bytes (24 bits)
            // 9
            {UnsignedLong.valueOf("65536"), Longs.toByteArray(65536L)},
            // 10
            {UnsignedLong.valueOf("65537"), Longs.toByteArray(65537L)},
            // 11
            {UnsignedLong.valueOf("16777214"), Longs.toByteArray(16777214L)},
            // 12
            {UnsignedLong.valueOf("16777215"), Longs.toByteArray(16777215L)},

            // Four Bytes (32 bits)
            // 13
            {UnsignedLong.valueOf("16777216"), Longs.toByteArray(16777216L)},
            // 14
            {UnsignedLong.valueOf("16777217"), Longs.toByteArray(16777217L)},
            // 15
            {UnsignedLong.valueOf("4294967294"), Longs.toByteArray(4294967294L)},
            // 16
            {UnsignedLong.valueOf("4294967295"), Longs.toByteArray(4294967295L)},

            // Five Bytes (40 bits)
            // 17
            {UnsignedLong.valueOf("4294967296"), Longs.toByteArray(4294967296L)},
            // 18
            {UnsignedLong.valueOf("4294967297"), Longs.toByteArray(4294967297L)},
            // 19
            {UnsignedLong.valueOf("1099511627774"), Longs.toByteArray(1099511627774L)},
            // 20
            {UnsignedLong.valueOf("1099511627775"), Longs.toByteArray(1099511627775L)},

            // Six Bytes (48 bits)
            // 21
            {UnsignedLong.valueOf("1099511627776"), Longs.toByteArray(1099511627776L)},
            // 22
            {UnsignedLong.valueOf("1099511627777"), Longs.toByteArray(1099511627777L)},
            // 23
            {UnsignedLong.valueOf("281474976710654"), Longs.toByteArray(281474976710654L)},
            // 24
            {UnsignedLong.valueOf("281474976710655"), Longs.toByteArray(281474976710655L)},

            // Seven Bytes (56 bits)
            // 25
            {UnsignedLong.valueOf("281474976710656"), Longs.toByteArray(281474976710656L)},
            // 26
            {UnsignedLong.valueOf("281474976710657"), Longs.toByteArray(281474976710657L)},
            // 27
            {UnsignedLong.valueOf("72057594037927934"), Longs.toByteArray(72057594037927934L)},
            // 28 (max 7-bit long value)
            {UnsignedLong.valueOf("72057594037927935"), Longs.toByteArray(72057594037927935L)},

            // 29
            {UnsignedLong.valueOf("9223372036854775805"), Longs.toByteArray(Long.MAX_VALUE - 2L)},
            // 30
            {UnsignedLong.valueOf("9223372036854775806"), Longs.toByteArray(Long.MAX_VALUE - 1L)},
            // 31
            {UnsignedLong.valueOf("9223372036854775807"), Longs.toByteArray(Long.MAX_VALUE)},

            // 32 Eight bytes, beyond Long.MAX_VALUE
            {UnsignedLong.valueOf("9223372036854775808"),
                new byte[] {(byte) 0x80, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00}},

            // 33
            {UnsignedLong.valueOf("9223372036854775809"),
                new byte[] {(byte) 0x80, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01}},
            // 33
            {UnsignedLong.valueOf("18446744073709551614"),
                new byte[] {(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
                    (byte) 0xFF, (byte) 0xFF, (byte) 0xFE}},
            // 34
            {UnsignedLong.valueOf("18446744073709551615"),
                new byte[] {(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
                    (byte) 0xFF, (byte) 0xFF, (byte) 0xFF}}
        }
    );
  }
}
