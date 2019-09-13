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

import org.interledger.encoding.asn.codecs.AsnOctetStringBasedObjectCodec;
import org.interledger.encoding.asn.codecs.AsnOpenTypeCodec;
import org.interledger.encoding.asn.codecs.AsnSizeConstraint;
import org.interledger.encoding.asn.framework.CodecContext;
import org.interledger.encoding.asn.framework.CodecContextFactory;

import com.google.common.io.BaseEncoding;
import org.junit.Assert;
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
 * Parameterized unit tests for encoding an instance of {@link AsnOctetStringOerSerializer}.
 */
@RunWith(Parameterized.class)
public class OpenTypeOerSerializerTest {

  private final byte[] asn1ByteValue;
  private final byte[] octetBytes;
  private CodecContext codecContext;

  /**
   * Construct an instance of this parameterized test with the supplied inputs.
   *
   * @param asn1Bytes  A byte array representing octets to be encoded.
   * @param octetBytes The expected value, in binary, of the supplied {@code asn1Bytes}.
   */
  public OpenTypeOerSerializerTest(final byte[] asn1Bytes, final byte[] octetBytes) {
    this.asn1ByteValue = asn1Bytes;
    this.octetBytes = octetBytes;
  }

  /**
   * The data for this test...
   */
  @Parameters
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][] {
        // [asn1_bytes][octet_bytes]
        // 0
        {BaseEncoding.base16().decode("0100"), "".getBytes()},
        // 1
        {BaseEncoding.base16().decode("020101"), BaseEncoding.base16().decode("01")},
        // 2
        {BaseEncoding.base16().decode("0403616263"), "abc".getBytes()},
        // 3
        {BaseEncoding.base16().decode("0C0B68656C6C6F20776F726C64"), "hello world".getBytes()},
        // 4
        {BaseEncoding.base16().decode("0B0A672E746573742E666F6F"), "g.test.foo".getBytes()},
        // 5
        {BaseEncoding.base16()
            .decode("8204028203FF672E746573742E313032342E4141414141414141414"
            + "1414141414141414141414141414141414141414141414141414141414141414141414141"
            + "4141414141414141414141414141414141414141414141414141414141414141414141414"
            + "1414141414141414141414141414141414141414141414141414141414141414141414141"
            + "4141414141414141414141414141414141414141414141414141414141414141414141414"
            + "1414141414141414141414141414141414141414141414141414141414141414141414141"
            + "4141414141414141414141414141414141414141414141414141414141414141414141414"
            + "1414141414141414141414141414141414141414141414141414141414141414141414141"
            + "4141414141414141414141414141414141414141414141414141414141414141414141414"
            + "1414141414141414141414141414141414141414141414141414141414141414141414141"
            + "4141414141414141414141414141414141414141414141414141414141414141414141414"
            + "1414141414141414141414141414141414141414141414141414141414141414141414141"
            + "4141414141414141414141414141414141414141414141414141414141414141414141414"
            + "1414141414141414141414141414141414141414141414141414141414141414141414141"
            + "4141414141414141414141414141414141414141414141414141414141414141414141414"
            + "1414141414141414141414141414141414141414141414141414141414141414141414141"
            + "4141414141414141414141414141414141414141414141414141414141414141414141414"
            + "1414141414141414141414141414141414141414141414141414141414141414141414141"
            + "4141414141414141414141414141414141414141414141414141414141414141414141414"
            + "1414141414141414141414141414141414141414141414141414141414141414141414141"
            + "4141414141414141414141414141414141414141414141414141414141414141414141414"
            + "1414141414141414141414141414141414141414141414141414141414141414141414141"
            + "4141414141414141414141414141414141414141414141414141414141414141414141414"
            + "1414141414141414141414141414141414141414141414141414141414141414141414141"
            + "4141414141414141414141414141414141414141414141414141414141414141414141414"
            + "1414141414141414141414141414141414141414141414141414141414141414141414141"
            + "4141414141414141414141414141414141414141414141414141414141414141414141414"
            + "1414141414141414141414141414141414141414141414141414141414141414141414141"
            + "41414141414141414141414141414141"),

            ("g.test.1024.AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA").getBytes()},});
  }

  /**
   * Test setup.
   */
  @Before
  public void setUp() throws Exception {
    // Register the codec to be tested...
    codecContext = CodecContextFactory.oer();
    codecContext.register(TestType.class, TestOpenTypeCodec::new);
  }

  @Test
  public void readTest() throws Exception {
    // This stream allows the codec to read the asn1Bytes...
    ByteArrayInputStream inputStream = new ByteArrayInputStream(asn1ByteValue);

    // Assert that the coded read bytes that equal what the harness put into octetBytes.
    final TestType actualValue = codecContext.read(TestType.class, inputStream);
    assertThat(actualValue.bytes).isEqualTo(octetBytes);
  }

  @Test
  public void writeTest() throws Exception {
    // Allow the AsnObjectCodec to write to 'outputStream'
    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    codecContext.write(new TestType(octetBytes), outputStream);

    // Assert that the bytes written to 'outputStream' match the contents that the harness put
    // into asn1ByteValue.
    Assert.assertArrayEquals(asn1ByteValue, outputStream.toByteArray());
  }

  @Test
  public void writeReadWriteTest() throws Exception {
    // Write octets...
    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    codecContext.write(new TestType(octetBytes), outputStream);

    // Read octets...
    final ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
    final TestType actual = codecContext.read(TestType.class, inputStream);
    assertThat(actual.bytes).isEqualTo(octetBytes);

    // Write octets again...
    final ByteArrayOutputStream outputStream2 = new ByteArrayOutputStream();
    codecContext.write(actual, outputStream2);

    // Assert originally written bytes equals newly written bytes.
    Assert.assertArrayEquals(outputStream.toByteArray(), outputStream2.toByteArray());
  }

  private static class TestType {

    public byte[] bytes;

    TestType(byte[] bytes) {
      this.bytes = bytes;
    }
  }

  private static class TestTypeCodec extends AsnOctetStringBasedObjectCodec<TestType> {

    public TestTypeCodec() {
      super(AsnSizeConstraint.UNCONSTRAINED);
    }

    @Override
    public TestType decode() {
      return new TestType(this.getBytes());
    }

    @Override
    public void encode(TestType value) {
      this.setBytes(value.bytes);
    }
  }

  private static class TestOpenTypeCodec extends AsnOpenTypeCodec<TestType> {

    public TestOpenTypeCodec() {
      super(new TestTypeCodec());
    }
  }
}
