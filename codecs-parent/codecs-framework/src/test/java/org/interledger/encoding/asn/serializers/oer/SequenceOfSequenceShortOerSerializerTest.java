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

import org.interledger.encoding.asn.codecs.AsnSequenceCodec;
import org.interledger.encoding.asn.codecs.AsnSequenceOfSequenceCodec;
import org.interledger.encoding.asn.codecs.AsnUint8Codec;
import org.interledger.encoding.asn.framework.CodecContext;
import org.interledger.encoding.asn.framework.CodecContextFactory;

import com.google.common.io.BaseEncoding;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

/**
 * Parameterized unit tests for encoding an instance of {@link Short}.
 */
@RunWith(Parameterized.class)
public class SequenceOfSequenceShortOerSerializerTest {

  private final short[][] inputValue;
  private final byte[] asn1OerBytes;
  private CodecContext codecContext;

  /**
   * Construct an instance of this parameterized test with the supplied inputs.
   *
   * @param inputValue   A {@code short} representing the unsigned 8bit shorteger to write in OER encoding.
   * @param asn1OerBytes The expected value, in binary, of the supplied {@code shortValue}.
   */
  public SequenceOfSequenceShortOerSerializerTest(
      final short[][] inputValue, final byte[] asn1OerBytes
  ) {
    this.inputValue = inputValue;
    this.asn1OerBytes = asn1OerBytes;
  }

  /**
   * The data for this test...
   */
  @Parameters
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][] {
        {
            new short[][] {
                new short[] {0, 0, 0}
            },
            BaseEncoding.base16().decode("0101000000")
        },
        {
            new short[][] {
                new short[] {1, 2, 3},
                new short[] {1, 2, 3}
            },
            BaseEncoding.base16().decode("0102010203010203")
        },
        {
            new short[][] {
                new short[] {0, 1, 255},
                new short[] {0, 1, 255},
                new short[] {0, 1, 255},
                new short[] {0, 1, 255},
                new short[] {255, 0, 1}
            },
            BaseEncoding.base16().decode("01050001FF0001FF0001FF0001FFFF0001")
        }
    });
  }

  /**
   * Test setup.
   */
  @Before
  public void setUp() throws Exception {
    // Register the codec to be tested...
    codecContext = CodecContextFactory.oer();
    codecContext.register(TestSequence.class, TestSequenceCodec::new);
    codecContext.register(TestSequenceOfSequence.class, TestSequenceOfSequenceCodec::new);
  }

  @Test
  public void read() throws Exception {
    final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(asn1OerBytes);
    final TestSequenceOfSequence actualValue = codecContext
        .read(TestSequenceOfSequence.class, byteArrayInputStream);
    for (short i = 0; i < inputValue.length; i++) {
      TestSequence sequence = actualValue.get(i);
      for (short j = 0; j < inputValue[i].length; j++) {
        assertThat(sequence.getNumbers()[j]).isEqualTo(inputValue[i][j]);
      }
    }

  }

  @Test
  public void write() throws Exception {
    final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    TestSequenceOfSequence sequences = new TestSequenceOfSequence();
    for (short i = 0; i < inputValue.length; i++) {
      sequences.add(i, new TestSequence(inputValue[i]));
    }
    codecContext.write(sequences, byteArrayOutputStream);
    assertThat(byteArrayOutputStream.toByteArray()).isEqualTo(asn1OerBytes);
  }

  @Test
  public void writeThenRead() throws Exception {
    // Write...
    final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    TestSequenceOfSequence sequences = new TestSequenceOfSequence();
    for (short i = 0; i < inputValue.length; i++) {
      sequences.add(i, new TestSequence(inputValue[i]));
    }
    codecContext.write(sequences, byteArrayOutputStream);
    assertThat(byteArrayOutputStream.toByteArray()).isEqualTo(asn1OerBytes);

    // Read...
    final ByteArrayInputStream byteArrayInputStream =
        new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
    final TestSequenceOfSequence decodedValue = codecContext
        .read(TestSequenceOfSequence.class, byteArrayInputStream);

    // Write...
    final ByteArrayOutputStream byteArrayOutputStream2 = new ByteArrayOutputStream();
    codecContext.write(decodedValue, byteArrayOutputStream2);
    assertThat(byteArrayOutputStream2.toByteArray()).isEqualTo(asn1OerBytes);
  }

  /**
   * A sequence of 3 UInt8 values.
   */
  private static class TestSequence {

    private short[] numbers;

    TestSequence(short... numbers) {
      this.numbers = numbers;
    }

    short[] getNumbers() {
      return numbers;
    }
  }

  private static class TestSequenceOfSequence extends ArrayList<TestSequence> {

  }

  private static class TestSequenceCodec extends AsnSequenceCodec<TestSequence> {

    public TestSequenceCodec() {
      super(
          new AsnUint8Codec(),
          new AsnUint8Codec(),
          new AsnUint8Codec()
      );
    }

    @Override
    public TestSequence decode() {
      return new TestSequence(getValueAt(0), getValueAt(1), getValueAt(2));
    }

    @Override
    public void encode(TestSequence value) {
      setValueAt(0, value.getNumbers()[0]);
      setValueAt(1, value.getNumbers()[1]);
      setValueAt(2, value.getNumbers()[2]);
    }
  }

  private static class TestSequenceOfSequenceCodec
      extends AsnSequenceOfSequenceCodec<TestSequenceOfSequence, TestSequence> {

    public TestSequenceOfSequenceCodec() {
      super(TestSequenceOfSequence::new, TestSequenceCodec::new);
    }
  }
}
