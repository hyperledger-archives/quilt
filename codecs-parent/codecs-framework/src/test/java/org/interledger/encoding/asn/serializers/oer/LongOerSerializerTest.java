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

import org.interledger.encoding.asn.codecs.AsnUint32Codec;

import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

/**
 * Parameterized unit tests for encoding an instance of {@link AsnUint32Codec}.
 */
@RunWith(Parameterized.class)
public class LongOerSerializerTest extends AbstractSerializerTest<Long> {

  /**
   * Construct an instance of this parameterized test with the supplied inputs.
   *
   * @param inputValue   A {@code int} representing the unsigned 8bit integer to write in OER encoding.
   * @param asn1OerBytes The expected value, in binary, of the supplied {@code intValue}.
   */
  public LongOerSerializerTest(final long inputValue, final byte[] asn1OerBytes) {
    super(inputValue, Long.class, asn1OerBytes);
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
            {0L, Ints.toByteArray(0)},
            // 1
            {1L, Ints.toByteArray(1)},
            // 2
            {2L, Ints.toByteArray(2)},
            // 3
            {254L, Ints.toByteArray(254)},
            // 4
            {255L, Ints.toByteArray(255)},

            // Two Bytes (16 bits)
            // 5
            {256L, Ints.toByteArray(256)},
            // 6
            {257L, Ints.toByteArray(257)},
            // 7
            {65534L, Ints.toByteArray(65534)},
            // 8
            {65535L, Ints.toByteArray(65535)},

            // Three Bytes (24 bits)
            // 9
            {65536L, Ints.toByteArray(65536)},
            // 10
            {65537L, Ints.toByteArray(65537)},
            // 11
            {16777214L, Ints.toByteArray(16777214)},
            // 12
            {16777215L, Ints.toByteArray(16777215)},

            // Four Bytes (32 bits)
            // 13
            {16777216L, Ints.toByteArray(16777216)},
            // 14
            {16777217L, Ints.toByteArray(16777217)},
            // 15 bits set. we use Longs to create a byte array, but resize from 8 to 4 bytes
            {4294967294L, Arrays.copyOfRange(Longs.toByteArray(4294967294L), 4, 8)},
            // 16 bits set. we use Longs to create a byte array, but resize from 8 to 4 bytes
            {4294967295L, Arrays.copyOfRange(Longs.toByteArray(4294967295L), 4, 8)}
        }
    );
  }

  /**
   * Validate an overflow amount.
   */
  @Test(expected = IllegalArgumentException.class)
  public void write32BitUInt_Overflow() throws IOException {
    try {
      final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
      codecContext.write(4294967296L, byteArrayOutputStream);
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage()).isEqualTo("Uint32 only supports values from 0 to 4294967295, "
          + "value 4294967296 is out of range.");
      throw e;
    }
  }
}
