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
import java.util.Arrays;
import java.util.Collection;

/**
 * Parameterized tests for encoding specific byte  values and asserting expected instances of {@link Instant}.
 */
@RunWith(Parameterized.class)
public class AsnBtpInstantSerializationBytesTest {

  private CodecContext codecContext;

  // An input Instant that will be used as a source so that the codec can produce its output (may have too much
  // information which we want to simulate)
  private String inputBytesHex;

  // The value we expect the CodecContext to emit as a String (which will ultimately be serialized).
  private Instant expectedInstant;

  /**
   * Construct an instance of this parameterized test with the supplied inputs.
   *
   * @param inputBytesHex   An array of bytes that should be decoded into {@link #expectedInstant}.
   * @param expectedInstant An {@link Instant} that should be encoded into {@link #inputBytesHex}.
   */
  public AsnBtpInstantSerializationBytesTest(final String inputBytesHex, final Instant expectedInstant) {
    this.inputBytesHex = inputBytesHex.replace(" ", "");
    this.expectedInstant = expectedInstant;
  }

  /**
   * The data for this test. Note that in some cases the input and output format differ, because we will always include
   * a full time portion. we therefore need input, expected value, and expected output data to properly test.
   */
  @Parameters
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][] {
        // Values taken from IL-RFC-30
        // https://github.com/interledger/rfcs/blob/master/0030-notes-on-oer-encoding/0030-notes-on-oer-encoding.md#examples-4

        /*
         * test parameters are arrays of the form:
         * [Instant representation] [input string representation] [Instant representation]
         */
        // 0
        {
            "13323031 37313232 34313631 3433322E 3237395A",
            Instant.parse("2017-12-24T16:14:32.279Z")
        },
        // 1
        {
            "11323031 37313232 34313631 3433322E 325A",
            Instant.parse("2017-12-24T16:14:32.200Z")
        },
        // 2
        {
            "0F323031 37313232 35303030 3030305A",
            Instant.parse("2017-12-25T00:00:00.000Z")
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

  @Test
  public void read() throws Exception {
    /* push the test input bytes into an inputstream */
    ByteArrayInputStream inputStream = new ByteArrayInputStream(BaseEncoding.base16().decode(this.inputBytesHex));

    /*
     * assert that the codec reads the sequence of bytes as a string and converts into the expected
     * zoned date time
     */
    final Instant actualInstant = codecContext.read(Instant.class, inputStream);

    /* ensure the times are the same, taking into account timezones */
    Assert.assertTrue("Expected time : " + this.expectedInstant + " got : " + actualInstant,
        expectedInstant.equals(actualInstant));
  }

  @Test
  public void write() throws Exception {
    /* let the codec write the value to a stream */
    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    codecContext.write(expectedInstant, outputStream);

    /* check that the value written matches what we expect */
    Assert.assertArrayEquals(
        String.format(
            "Expected H[%s] (`%s`) but instead found H[%s] (`%s`)",
            this.inputBytesHex,
            new String(BaseEncoding.base16().decode(this.inputBytesHex)),
            BaseEncoding.base16().encode(outputStream.toByteArray()),
            outputStream.toString()
        ),
        BaseEncoding.base16().decode(this.inputBytesHex), outputStream.toByteArray()
    );
  }

  @Test
  public void writeThenRead() throws Exception {
    /* write the time value out */
    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    codecContext.write(expectedInstant, outputStream);

    /* read the data back */
    final ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
    final Instant actual = codecContext.read(Instant.class, inputStream);

    /* assert that the times are equivalent (when represented in the same timezone) */
    Assert.assertTrue("expected time: " + expectedInstant, actual.equals(expectedInstant));

    /* write the data just read back to a stream ... */
    final ByteArrayOutputStream outputStream2 = new ByteArrayOutputStream();
    codecContext.write(actual, outputStream2);

    /* check that the originally written bytes are identical to the newly written bytes */
    Assert.assertArrayEquals(outputStream.toByteArray(), outputStream2.toByteArray());
  }
}
