package org.interledger.encoding.asn.serializers.oer;

/*-
 * ========================LICENSE_START=================================
 * Interledger Codec Framework
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
import org.interledger.encoding.asn.framework.CodecContextFactory;

import com.google.common.io.BaseEncoding;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;

/**
 * Parameterized unit tests for encoding an instance of {@link T}.
 */
public abstract class AbstractSerializerTest<T> {

  protected static final BaseEncoding B16 = BaseEncoding.base16();
  protected static final CodecContext codecContext;

  static {
    // Register the codec to be tested...
    codecContext = CodecContextFactory.oer();
  }

  private final T inputValue;
  private final Class<T> clazz;
  private final byte[] asn1OerBytes;

  /**
   * Construct an instance of this parameterized test with the supplied inputs.
   *
   * @param inputValue   A {@code T} representing the value to write in OER encoding.
   * @param clazz        Class of type <tt>T</tt> for use by the CodecContext.
   * @param asn1OerBytes The expected value, in binary, of the supplied {@code inputValue}.
   */
  public AbstractSerializerTest(final T inputValue, Class<T> clazz, final byte[] asn1OerBytes) {
    this.inputValue = inputValue;
    this.clazz = clazz;
    this.asn1OerBytes = asn1OerBytes;
  }

  @Test
  public void read() throws Exception {
    final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(asn1OerBytes);
    final T actualValue = codecContext.read(clazz, byteArrayInputStream);

    assertThat(actualValue).isEqualTo(inputValue);
  }

  @Test
  public void write() throws Exception {
    final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    codecContext.write(inputValue, byteArrayOutputStream);

    final String actual = Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray());
    assertThat(actual).isEqualTo(Base64.getEncoder().encodeToString(this.asn1OerBytes));
  }

  @Test
  public void writeThenRead() throws Exception {
    // Write...
    final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    codecContext.write(inputValue, byteArrayOutputStream);
    assertThat(byteArrayOutputStream.toByteArray()).isEqualTo(asn1OerBytes);

    // Read...
    final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(
        byteArrayOutputStream.toByteArray()
    );
    final T decodedValue = codecContext.read(clazz, byteArrayInputStream);

    // Write...
    final ByteArrayOutputStream byteArrayOutputStream2 = new ByteArrayOutputStream();
    codecContext.write(decodedValue, byteArrayOutputStream2);

    assertThat(byteArrayOutputStream2.toByteArray()).isEqualTo(asn1OerBytes);
  }
}
