package org.interledger.core.asn.codecs;

/*-
 * ========================LICENSE_START=================================
 * Interledger Core Codecs
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

import org.interledger.core.asn.framework.InterledgerCodecContextFactory;
import org.interledger.encoding.asn.framework.CodecContext;

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
 * Parameterized tests for encoding and decoding {@link AsnTimestampCodec} instances.
 */
@RunWith(Parameterized.class)
public class InstantOerSerializerTest {

  private CodecContext codecContext;

  private String stringValue;
  private Instant timeValue;

  /**
   * Construct an instance of this parameterized test with the supplied inputs.
   *
   * @param stringValue The expected value, as a {@link String}, in 'YYYYMMDDHHMMSS.fffZ' format.
   * @param timeValue   The same time as an instance of {@link Instant}.
   */
  public InstantOerSerializerTest(final String stringValue, final Instant timeValue) {
    this.stringValue = stringValue;
    this.timeValue = timeValue;
  }

  /**
   * The data for this test. Note that in some cases the input and output format differ, because we
   * will always include a full time portion. we therefore need input, expected value, and expected
   * output data to properly test.
   */
  @Parameters
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][] {
        /*
         * test parameters are arrays of the form [input string representation] [Zoned date time
         * representation]
         */
        {
            "20170630010203000",
            Instant.from(ZonedDateTime.of(2017, 6, 30, 1, 2, 3, 0, ZoneId.of("Z")))
        },
        {
            "20170630010203100",
            Instant.from(
                ZonedDateTime.of(2017, 6, 30, 1, 2, 3, (int) TimeUnit.MILLISECONDS.toNanos(100),
                    ZoneId.of("Z")))
        },
        {
            "20170630010203100",
            Instant.from(
                ZonedDateTime.of(2017, 6, 30, 3, 2, 3, (int) TimeUnit.MILLISECONDS.toNanos(100),
                    ZoneId.of("+02:00")))
        },
        {
            "20170630010203100",
            Instant.from(ZonedDateTime
                .of(2017, 6, 29, 23, 2, 3, (int) TimeUnit.MILLISECONDS.toNanos(100),
                    ZoneId.of("-02:00")))}
    });
  }

  /**
   * Convenience method to convert the expected time string into the byte format that would be found
   * on the wire.
   *
   * @param value The string to convert. Must not be null.
   * @return A byte[] representing the expected representation of the string as would be found on
   *          the wire.
   */
  public static byte[] encodeString(String value) {
    return value.getBytes(StandardCharsets.US_ASCII);
  }

  /**
   * Test setup.
   */
  @Before
  public void setUp() throws Exception {
    codecContext = InterledgerCodecContextFactory.oer();
  }

  @Test
  public void read() throws Exception {
    /* push the test input bytes into an inputstream */
    ByteArrayInputStream inputStream = new ByteArrayInputStream(encodeString(stringValue));

    /*
     * assert that the codec reads the sequence of bytes as a string and converts into the expected
     * zoned date time
     */
    final Instant actualValue = codecContext.read(Instant.class, inputStream);

    /* ensure the times are the same, taking into account timezones */
    Assert.assertTrue("expected time : " + timeValue + " got : " + actualValue,
        timeValue.equals(actualValue));
  }

  @Test
  public void write() throws Exception {
    /* let the codec write the value to a stream */
    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    codecContext.write(timeValue, outputStream);

    /* check that the value written matches what we expect */
    Assert.assertArrayEquals("expected time : " + timeValue + " got " + outputStream.toString(),
        encodeString(stringValue), outputStream.toByteArray());
  }

  @Test
  public void writeThenRead() throws Exception {
    /* write the time value out */
    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    codecContext.write(timeValue, outputStream);

    /* read the data back */
    final ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
    final Instant actual = codecContext.read(Instant.class, inputStream);

    /* assert that the times are equivalent (when represented in the same timezone) */
    Assert.assertTrue("expected time: " + timeValue, actual.equals(timeValue));

    /* write the data just read back to a stream ... */
    final ByteArrayOutputStream outputStream2 = new ByteArrayOutputStream();
    codecContext.write(actual, outputStream2);

    /* check that the originally written bytes are identical to the newly written bytes */
    Assert.assertArrayEquals(outputStream.toByteArray(), outputStream2.toByteArray());
  }
}
