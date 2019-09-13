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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

/**
 * Parameterized unit tests for encoding an instance of {@link Short}.
 */
@RunWith(Parameterized.class)
public class ShortOerSerializerTest extends AbstractSerializerTest<Short> {

  /**
   * Construct an instance of this parameterized test with the supplied inputs.
   *
   * @param inputValue   A {@code int} representing the unsigned 8bit integer to write in OER encoding.
   * @param asn1OerBytes The expected value, in binary, of the supplied {@code intValue}.
   */
  public ShortOerSerializerTest(final short inputValue, final byte[] asn1OerBytes) {
    super(inputValue, Short.class, asn1OerBytes);
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
            {(short) 0, B16.decode("00")},
            // 1
            {(short) 1, B16.decode("01")},
            // 2
            {(short) 2, B16.decode("02")},
            // 3
            {(short) 254, B16.decode("FE")},
            // 4
            {(short) 255, B16.decode("FF")},
        }
    );
  }

  /**
   * Validate an overflow amount.
   */
  @Test(expected = IllegalArgumentException.class)
  public void write8BitUInt_Overflow() throws IOException {
    try {
      final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
      codecContext.write((short) 256, byteArrayOutputStream);
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage()).isEqualTo("Uint8 only supports values from 0 to 255, value 256 is out of range.");
      throw e;
    }
  }
}
