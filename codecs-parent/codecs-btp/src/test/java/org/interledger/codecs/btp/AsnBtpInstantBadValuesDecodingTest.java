package org.interledger.codecs.btp;

/*-
 * ========================LICENSE_START=================================
 * Bilateral Transfer Protocol Core Codecs
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

import org.interledger.encoding.asn.framework.CodecContext;
import org.interledger.encoding.asn.framework.CodecException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;

/**
 * Parameterized tests for encoding and decoding {@link AsnBtpGeneralizedTimeCodec} instances.
 */
@RunWith(Parameterized.class)
public class AsnBtpInstantBadValuesDecodingTest {

  private CodecContext codecContext;

  private String stringValue;

  /**
   * Construct an instance of this parameterized test with the supplied inputs.
   *
   * @param stringValue The expected value, as a {@link String}, in 'YYYYMMDDHHMMSS.fffZ' format.
   */
  public AsnBtpInstantBadValuesDecodingTest(final String stringValue) {
    this.stringValue = stringValue;
  }

  /**
   * The data for this test. Note that in some cases the input and output format differ, because we will always include
   * a full time portion. we therefore need input, expected value, and expected output data to properly test.
   */
  @Parameters
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][] {
        /*
         * test parameters are arrays of the form [input string representation] [Zoned date time
         * representation]
         */
        // 0
        {
            "20170630010203000", // No Z
        },
        // 1
        {
            "20171224235312.431+0200", //  INVALID, not UTC
        },
        // 2
        {
            "20171224215312.4318Z", // INVALID, too much precision
        },
        // 3
        {
            "20171224161432,279Z", // INVALID, wrong decimal element
        },
        // 4
        {
            "20171324161432.279Z", // INVALID, month out of range
        },
        // This is technically invalid, but this implementation tolerates it when parsing.
        //{
        //    "20171224230000.20Z", // INVALID, spurious trailing zero
        //},
        // 5
        {
            "20171224230000.Z", // INVALID, spurious decimal point
        },
        // 6
        {
            "2017122421531Z", // INVALID, missing seconds
        },
        // 7
        {
            "201712242153Z ", // INVALID, missing digit in seconds
        },
        // 8
        {
            "2017122421Z", // INVALID, missing seconds and minutes
        },
        // 9
        {
            "20171224215312.4318Z",
        },
        // 10
        // Pending https://github.com/interledger/rfcs/issues/481
        // {
        //     "20161231235960.852Z",
        // },
        // 10
        {
            "2017010112",
        },
        // 11
        {
            "201701011213",
        },
        // 12
        {
            "20170101121314",
        },
        // 13
        {
            "2017010112131401",
        },

    });
  }

  /**
   * Convenience method to convert the expected time string into the byte format that would be found on the wire.
   *
   * @param value The string to convert. Must not be null.
   *
   * @return A byte[] representing the expected representation of the string as would be found on the wire.
   */
  public static byte[] encodeString(String value) {
    final byte[] lengthByte = new byte[1];
    final byte[] stringBytes = value.getBytes(StandardCharsets.US_ASCII);
    final byte[] lengthPrefixed = new byte[stringBytes.length + 1];
    lengthByte[0] = Byte.valueOf(stringBytes.length + "");
    System.arraycopy(lengthByte, 0, lengthPrefixed, 0, lengthByte.length);
    System.arraycopy(stringBytes, 0, lengthPrefixed, lengthByte.length, stringBytes.length);
    return lengthPrefixed;
  }

  /**
   * Test setup.
   */
  @Before
  public void setUp() {
    codecContext = BtpCodecContextFactory.oer();
  }

  @Test(expected = CodecException.class)
  public void read() throws Exception {
    final ByteArrayInputStream inputStream = new ByteArrayInputStream(encodeString(stringValue));

    try {
      codecContext.read(Instant.class, inputStream);
      Assert.fail();
    } catch (CodecException e) {
      assertThat(e.getMessage()).startsWith("Invalid format:");
      throw e;
    }
  }

}
