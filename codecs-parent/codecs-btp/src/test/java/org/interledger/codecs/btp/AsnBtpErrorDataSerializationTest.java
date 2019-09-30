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

import org.interledger.btp.BtpError;
import org.interledger.btp.BtpErrorCode;
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
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;

/**
 * Parameterized tests for encoding and decoding {@link AsnBtpGeneralizedTimeCodec} instances.
 */
@RunWith(Parameterized.class)
public class AsnBtpErrorDataSerializationTest {

  private CodecContext codecContext;

  // An input Instant that will be used as a source so that the codec can produce its output (may have too much
  // information which we want to simulate)
  private String btpErrorBase64Bytes;

  private long expectedRequestId;
  private Instant expectedTriggeredAt;
  private BtpErrorCode expectedErrorCode;
  private String expectedErrorDataBase64Bytes;

  // The value we expect the CodecContext to emit as a String (which will ultimately be serialized).
  private String expectedStringValue;

  // The value we expect the CodecContext to produce when decoding an array of BTP bytes.
  //private Instant expectedDecodedInstant;

  /**
   * Construct an instance of this parameterized test with the supplied inputs.
   *
   * @param btpErrorBase64Bytes A {@link String} of base64 encoded {@link BtpError} bytes.
   * @param expectedRequestId {@code long} expected request id.
   * @param expectedErrorCode {@link BtpErrorCode} expected error code.
   * @param expectedTriggeredAt {@link Instant} expected triggeredAt time.
   * @param expectedErrorDataBase64Bytes A {@link String} of expected base64 error bytes.
   */
  public AsnBtpErrorDataSerializationTest(
      final String btpErrorBase64Bytes,
      final long expectedRequestId,
      final BtpErrorCode expectedErrorCode,
      final Instant expectedTriggeredAt,
      final String expectedErrorDataBase64Bytes
  // final String expectedStringValue,
  // final Instant expectedDecodedInstant
  ) {
    this.btpErrorBase64Bytes = btpErrorBase64Bytes;

    this.expectedRequestId = expectedRequestId;
    this.expectedErrorCode = expectedErrorCode;
    this.expectedTriggeredAt = expectedTriggeredAt;
    this.expectedErrorDataBase64Bytes = expectedErrorDataBase64Bytes;
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
            "Ag4CATs9RjAwEE5vdEFjY2VwdGVkRXJyb3ITMjAxODA5MjcxNTEyMTQuNDUyWhJpbnZhbGlkIGF1dGhfdG9rZW4BAA==",
            235012411L,
            BtpErrorCode.F00_NotAcceptedError,
            Instant.parse("2018-09-27T15:12:14.452Z"),
            "aW52YWxpZCBhdXRoX3Rva2Vu"
        },
    });
  }
  //
  //  /**
  //   * Convenience method to convert the expected time string into the byte format that would be found on the wire.
  //   *
  //   * @param value The string to convert. Must not be null.
  //   *
  //   * @return A byte[] representing the expected representation of the string as would be found on the wire.
  //   */
  //  public static byte[] encodeString(String value) {
  //    final byte[] lengthByte = new byte[1];
  //    final byte[] stringBytes = value.getBytes(StandardCharsets.US_ASCII);
  //    final byte[] lengthPrefixed = new byte[stringBytes.length + 1];
  //    lengthByte[0] = Byte.valueOf(stringBytes.length + "");
  //    System.arraycopy(lengthByte, 0, lengthPrefixed, 0, lengthByte.length);
  //    System.arraycopy(stringBytes, 0, lengthPrefixed, lengthByte.length, stringBytes.length);
  //    return lengthPrefixed;
  //  }

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
    ByteArrayInputStream inputStream = new ByteArrayInputStream(
        BaseEncoding.base64().decode(this.btpErrorBase64Bytes)
    );

    /*
     * assert that the codec reads the sequence of bytes as a string and converts into the expected BtpError
     */
    final BtpError actualValue = codecContext.read(BtpError.class, inputStream);

    assertThat(actualValue.getRequestId()).isEqualTo(this.expectedRequestId);
    assertThat(actualValue.getErrorCode()).isEqualTo(this.expectedErrorCode);
    assertThat(actualValue.getTriggeredAt()).isEqualTo(this.expectedTriggeredAt);
    assertThat(actualValue.getErrorData()).isEqualTo(BaseEncoding.base64().decode(this.expectedErrorDataBase64Bytes));
  }

  @Test
  public void write() throws Exception {
    final BtpError btpError = BtpError.builder()
        .requestId(expectedRequestId)
        .triggeredAt(expectedTriggeredAt)
        .errorCode(expectedErrorCode)
        .errorData(BaseEncoding.base64().decode(expectedErrorDataBase64Bytes))
        .build();

    /* let the codec write the value to a stream */
    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    codecContext.write(btpError, outputStream);

    final String actualBytesB64 = BaseEncoding.base64().encode(outputStream.toByteArray());

    /* check that the value written matches what we expect */
    assertThat(actualBytesB64).isEqualTo(this.btpErrorBase64Bytes);
  }

  @Test
  public void writeThenRead() throws Exception {
    final BtpError btpError = BtpError.builder()
        .requestId(expectedRequestId)
        .triggeredAt(expectedTriggeredAt)
        .errorCode(expectedErrorCode)
        .errorData(BaseEncoding.base64().decode(expectedErrorDataBase64Bytes))
        .build();
    /* write the time value out */
    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    codecContext.write(btpError, outputStream);

    /* read the data back */
    final ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
    final BtpError actual = codecContext.read(BtpError.class, inputStream);

    assertThat(actual).isEqualTo(btpError);

    /* write the data just read back to a stream ... */
    final ByteArrayOutputStream outputStream2 = new ByteArrayOutputStream();
    codecContext.write(actual, outputStream2);

    /* check that the originally written bytes are identical to the newly written bytes */
    Assert.assertArrayEquals(outputStream.toByteArray(), outputStream2.toByteArray());
  }
}
