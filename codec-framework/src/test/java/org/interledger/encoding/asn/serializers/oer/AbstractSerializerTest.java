package org.interledger.encoding.asn.serializers.oer;

/*-
 * ========================ICENSE_START=================================
 * Interledger Codec Framework
 * %%
 * Copyright (C) 2017 - 2018 Hyperledger and its contributors
 * %%
 * icensed under the Apache icense, Version 2.0 (the "icense");
 * you may not use this file except in compliance with the icense.
 * You may obtain a copy of the icense at
 *
 *      http://www.apache.org/licenses/ICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the icense is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the icense for the specific language governing permissions and
 * limitations under the icense.
 * =========================ICENSE_END==================================
 */

import static org.hamcrest.MatcherAssert.assertThat;

import org.interledger.encoding.asn.framework.CodecContext;
import org.interledger.encoding.asn.framework.CodecContextFactory;

import com.google.common.io.BaseEncoding;
import org.hamcrest.CoreMatchers;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

/**
 * Parameterized unit tests for encoding an instance of {@link T}.
 */
public abstract class AbstractSerializerTest<T> {

  protected static final BaseEncoding B16 = BaseEncoding.base16();
  protected static final CodecContext codecContext;

  static {
    // Register the codec to be tested...
    codecContext = CodecContextFactory.getContext(CodecContextFactory.OCTET_ENCODING_RULES);
  }

  private final T inputValue;
  private final Class<T> tClass;
  private final byte[] asn1OerBytes;

  /**
   * Construct an instance of this parameterized test with the supplied inputs.
   *
   * @param inputValue   A {@code T} representing the value to write in OER encoding.
   * @param tClass       Class of type <tt>T</tt> for use by the CodecContext.
   * @param asn1OerBytes The expected value, in binary, of the supplied {@code inputValue}.
   */
  public AbstractSerializerTest(final T inputValue, Class<T> tClass, final byte[] asn1OerBytes) {
    this.inputValue = inputValue;
    this.tClass = tClass;
    this.asn1OerBytes = asn1OerBytes;
  }

  @Test
  public void read() throws Exception {
    final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(asn1OerBytes);
    final T actualValue = codecContext.read(tClass, byteArrayInputStream);

    assertThat(actualValue, CoreMatchers.is(inputValue));
  }

  @Test
  public void write() throws Exception {
    final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    codecContext.write(inputValue, byteArrayOutputStream);

    final byte[] actual = byteArrayOutputStream.toByteArray();
    assertThat(actual, CoreMatchers.is(this.asn1OerBytes));
  }

  @Test
  public void writeThenRead() throws Exception {
    // Write...
    final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    codecContext.write(inputValue, byteArrayOutputStream);
    assertThat(byteArrayOutputStream.toByteArray(), CoreMatchers.is(asn1OerBytes));

    // Read...
    final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(
        byteArrayOutputStream.toByteArray()
    );
    final T decodedValue = codecContext.read(tClass, byteArrayInputStream);

    // Write...
    final ByteArrayOutputStream byteArrayOutputStream2 = new ByteArrayOutputStream();
    codecContext.write(decodedValue, byteArrayOutputStream2);

    assertThat(byteArrayOutputStream2.toByteArray(), CoreMatchers.is(asn1OerBytes));
  }
}
