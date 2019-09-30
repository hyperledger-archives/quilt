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

import org.interledger.encoding.asn.framework.CodecContext;

import com.google.common.io.BaseEncoding;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

/**
 * Parameterized tests for encoding and decoding {@link AsnBtpGeneralizedTimeCodec} instances.
 */
@RunWith(Parameterized.class)
public class AsnBtpInstantSerializationTest {

  private CodecContext codecContext;

  // An input Instant that will be used as a source so that the codec can produce its output (may have too much
  // information which we want to simulate)
  private Instant sourceInstantForWriting;

  // The value we expect the CodecContext to emit as a String (which will ultimately be serialized).
  private String expectedStringValue;

  // The value we expect the CodecContext to produce when decoding an array of BTP bytes.
  private Instant expectedDecodedInstant;

  /**
   * Construct an instance of this parameterized test with the supplied inputs.
   *
   * @param sourceInstantForWriting An input Instant that will be used as a source so that the codec can produce its
   *                                output (may have too much // information which we want to simulate)
   * @param expectedStringValue     The value we expect the CodecContext to emit as a {@link String}, in
   *                                'YYYYMMDDHHMMSS.fffZ' format (which will ultimately be serialized).
   * @param expectedDecodedInstant  The value we expect the CodecContext to produce when decoding an array of BTP
   *                                bytes.
   */
  public AsnBtpInstantSerializationTest(
      final Instant sourceInstantForWriting, final String expectedStringValue, final Instant expectedDecodedInstant) {
    this.sourceInstantForWriting = sourceInstantForWriting;
    this.expectedStringValue = expectedStringValue;
    this.expectedDecodedInstant = expectedDecodedInstant;
  }

  /**
   * The data for this test. Note that in some cases the input and output format differ, because we will always include
   * a full time portion. we therefore need input, expected value, and expected output data to properly test.
   */
  @Parameters
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][] {
        /*
         * test parameters are arrays of the form:
         * [Instant representation] [input string representation] [Instant representation]
         */
        // 0
        {
            Instant.parse("2017-12-24T16:14:32.279Z"),
            "20171224161432.279Z",
            Instant.parse("2017-12-24T16:14:32.279Z")
        },
        // 1
        {
            Instant.parse("2017-12-24T16:14:32.279112Z"),
            "20171224161432.279Z", // (rounded)
            Instant.parse("2017-12-24T16:14:32.279Z")
        },
        // 2
        {
            Instant.parse("2017-12-24T16:14:32.270Z"),
            "20171224161432.27Z",
            Instant.parse("2017-12-24T16:14:32.270Z")
        },
        // Pending https://github.com/interledger/rfcs/issues/481
        // Leap Second.
        // {
        //     Instant.parse("2016-12-31T23:59:59.852Z")
        //     "20161231235960.852Z",
        //     Instant.parse("2016-12-31T23:59:59.852Z")
        // },
        // 3
        {
            Instant.parse("2017-12-24T16:14:32.2Z"),
            "20171224161432.2Z",
            Instant.parse("2017-12-24T16:14:32.2Z")
        },
        // 4
        {
            Instant.parse("2017-12-24T16:14:32.002Z"),
            "20171224161432.002Z",
            Instant.parse("2017-12-24T16:14:32.002Z")
        },
        // 5
        {
            Instant.parse("2017-12-24T16:14:32Z"),
            "20171224161432Z",
            Instant.parse("2017-12-24T16:14:32Z")
        },
        // 6
        {
            Instant.parse("2017-12-24T16:14:30Z"),
            "20171224161430Z",
            Instant.parse("2017-12-24T16:14:30Z")
        },
        // 7
        {
            Instant.parse("2017-12-24T16:14:00Z"),
            "20171224161400Z",
            Instant.parse("2017-12-24T16:14:00Z")
        },
        // 8
        {
            Instant.parse("2017-12-24T16:10:00Z"),
            "20171224161000Z",
            Instant.parse("2017-12-24T16:10:00Z")
        },
        // 9
        {
            Instant.parse("2017-12-24T16:00:00Z"),
            "20171224160000Z",
            Instant.parse("2017-12-24T16:00:00Z")
        },
        // 10
        {
            Instant.parse("2017-12-24T10:00:00Z"),
            "20171224100000Z",
            Instant.parse("2017-12-24T10:00:00Z")
        },
        // 11
        {
            Instant.parse("2017-12-24T00:00:00Z"),
            "20171224000000Z",
            Instant.parse("2017-12-24T00:00:00Z")
        },
        // 12
        {
            Instant.parse("2017-12-24T24:00:00Z"),
            "20171225000000Z", // (use correct midnight format)
            Instant.parse("2017-12-24T24:00:00Z")
        },
        // 13
        {
            Instant.from(ZonedDateTime
                .of(2017, 12, 24, 18, 14, 32, (int) TimeUnit.MILLISECONDS.toNanos(0), ZoneId.of("+02:00"))),
            "20171224161432Z", // (converted to UTC)
            Instant.parse("2017-12-24T16:14:32Z")
        },
        // 14
        {
            Instant.parse("9999-12-24T16:14:32.279Z"),
            "99991224161432.279Z",
            Instant.parse("9999-12-24T16:14:32.279Z")
        },
        // 15
        {
            // Wrong representation of midnight, should translate to correct representation.
            Instant.parse("2017-12-24T24:00:00.000Z"),
            "20171225000000Z",
            Instant.parse("2017-12-25T00:00:00.000Z")
        }
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

  @Test
  public void read() throws Exception {
    /* push the test input bytes into an inputstream */
    ByteArrayInputStream inputStream = new ByteArrayInputStream(encodeString(expectedStringValue));

    /*
     * assert that the codec reads the sequence of bytes as a string and converts into the expected
     * zoned date time
     */
    final Instant actualValue = codecContext.read(Instant.class, inputStream);

    /* ensure the times are the same, taking into account timezones */
    Assert.assertTrue("expected time : " + expectedDecodedInstant + " got : " + actualValue,
        expectedDecodedInstant.equals(actualValue));
  }

  @Test
  public void write() throws Exception {
    /* let the codec write the value to a stream */
    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    codecContext.write(sourceInstantForWriting, outputStream);

    /* check that the value written matches what we expect */
    Assert.assertArrayEquals(
        String.format(
            "Expected H[%s] (`%s`) but instead found H[%s] (`%s`)",
            BaseEncoding.base16().encode(encodeString(expectedStringValue)),
            new String(encodeString(expectedStringValue)),
            BaseEncoding.base16().encode(outputStream.toByteArray()),
            outputStream.toString()
        ),
        encodeString(expectedStringValue), outputStream.toByteArray()
    );
  }

  @Test
  public void writeThenRead() throws Exception {
    /* write the time value out */
    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    codecContext.write(expectedDecodedInstant, outputStream);

    /* read the data back */
    final ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
    final Instant actual = codecContext.read(Instant.class, inputStream);

    /* assert that the times are equivalent (when represented in the same timezone) */
    Assert.assertTrue("expected time: " + expectedDecodedInstant, actual.equals(expectedDecodedInstant));

    /* write the data just read back to a stream ... */
    final ByteArrayOutputStream outputStream2 = new ByteArrayOutputStream();
    codecContext.write(actual, outputStream2);

    /* check that the originally written bytes are identical to the newly written bytes */
    Assert.assertArrayEquals(outputStream.toByteArray(), outputStream2.toByteArray());
  }
}
