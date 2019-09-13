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
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.Collection;

/**
 * Parameterized unit tests for encoding an instance of {@link AsnUint8OerSerializer}.
 */
@RunWith(Parameterized.class)
public class OerLengthPrefixCodecTest {

  private final int expectedPayloadLength;
  private final byte[] asn1OerBytes;

  /**
   * Construct an instance of this parameterized test with the supplied inputs.
   *
   * @param expectedPayloadLength An integer representing the length of a payload that should be encoded as a
   *                              length-prefix.
   * @param asn1OerBytes          The expected value, in binary, of the supplied {@code expectedPayloadLength}.
   */
  public OerLengthPrefixCodecTest(final int expectedPayloadLength, final byte[] asn1OerBytes) {
    this.expectedPayloadLength = expectedPayloadLength;
    this.asn1OerBytes = asn1OerBytes;
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
        {128, BaseEncoding.base16().decode("8180")},
        // 5
        {129, BaseEncoding.base16().decode("8181")},
        // 6 (the last number that can be encoded in 1 value octet)
        {255, BaseEncoding.base16().decode("81FF")},
        // 7 (the first number that can be encoded in 2 value octets)
        {256, BaseEncoding.base16().decode("820100")},
        // 8 (Last number that can be encoded in 2 value octets).
        {65535, BaseEncoding.base16().decode("82FFFF")},

        // 9 (First number that can be encoded in 3 value octets).
        {65536, BaseEncoding.base16().decode("83010000")},

        // 10 (Last number that can be encoded in 3 value octets).
        {16777215, BaseEncoding.base16().decode("83FFFFFF")},

        // 11 (First number that can be encoded in 4 value octets).
        {16777216, BaseEncoding.base16().decode("8401000000")},

        // 11 (First number that can be encoded in 4 value octets).
        {Integer.MAX_VALUE, BaseEncoding.base16().decode("847FFFFFFF")},});
  }

  /**
   * Test setup.
   */
  @Before
  public void setUp() throws Exception {
  }

  @Test
  public void read() throws Exception {
    // This stream allows the codec to read the asn1Bytes...
    final ByteArrayInputStream inputStream = new ByteArrayInputStream(this.asn1OerBytes);
    final int actualPayloadLength = OerLengthSerializer.readLength(inputStream);
    assertThat(actualPayloadLength).isEqualTo(expectedPayloadLength);
  }

  @Test
  public void write() throws Exception {
    // Allow the AsnObjectCodec to write to 'outputStream'
    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    OerLengthSerializer.writeLength(expectedPayloadLength, outputStream);
    assertThat(this.asn1OerBytes).isEqualTo(outputStream.toByteArray());
  }

  @Test
  public void writeThenRead() throws Exception {
    // Write octets...
    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    OerLengthSerializer.writeLength(expectedPayloadLength, outputStream);

    // Read octets...
    final ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
    final int actual = OerLengthSerializer.readLength(inputStream);
    assertThat(actual).isEqualTo(expectedPayloadLength);

    // Write octets again...
    final ByteArrayOutputStream outputStream2 = new ByteArrayOutputStream();
    OerLengthSerializer.writeLength(actual, outputStream2);

    // Assert originally written bytes equals newly written bytes.
    assertThat(outputStream.toByteArray()).isEqualTo(outputStream2.toByteArray());
  }

}
