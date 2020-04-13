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
import org.interledger.encoding.asn.codecs.AsnUint16Codec;
import org.interledger.encoding.asn.framework.CodecContext;
import org.interledger.encoding.asn.framework.CodecContextFactory;

import com.google.common.io.BaseEncoding;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

/**
 * * Parameterized unit tests for encoding a "Sequence Of" (i.e., an Array) of an Sequence (i.e., an object) containing
 * a {@link Integer} via {@link AsnSequenceOfSequenceOerSerializer}.
 */
@RunWith(Parameterized.class)
public class SequenceOfSequenceIntOerSerializerTest {

  private final int[][] inputValue;
  private final byte[] asn1OerBytes;

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private CodecContext codecContext;

  /**
   * Construct an instance of this parameterized test with the supplied inputs.
   *
   * @param inputValue   A {@code int} representing the unsigned 8bit integer to write in OER encoding.
   * @param asn1OerBytes The expected value, in binary, of the supplied {@code intValue}.
   */
  public SequenceOfSequenceIntOerSerializerTest(final int[][] inputValue, final byte[] asn1OerBytes) {
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
            new int[][] {
                new int[] {0, 0, 0}
            },
            BaseEncoding.base16().decode("0101000000000000")
        },
        {
            new int[][] {
                new int[] {1, 2, 3},
                new int[] {1, 2, 3}
            },
            BaseEncoding.base16().decode("0102000100020003000100020003")
        },
        {
            new int[][] {
                new int[] {0, 1, 255},
                new int[] {0, 1, 255},
                new int[] {0, 1, 255},
                new int[] {0, 1, 255},
                new int[] {255, 0, 1}
            },
            BaseEncoding.base16().decode("01050000000100FF0000000100FF0000000100FF0000000100FF00FF00000001")
        }
    });
  }

  /**
   * Test setup.
   */
  @Before
  public void setUp() {
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
    for (int i = 0; i < inputValue.length; i++) {
      TestSequence sequence = actualValue.get(i);
      for (int j = 0; j < inputValue[i].length; j++) {
        assertThat(sequence.getNumbers()[j]).isEqualTo(inputValue[i][j]);
      }
    }

  }

  @Test
  public void write() throws Exception {
    final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    TestSequenceOfSequence sequences = new TestSequenceOfSequence();
    for (int i = 0; i < inputValue.length; i++) {
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
    for (int i = 0; i < inputValue.length; i++) {
      sequences.add(i, new TestSequence(inputValue[i]));
    }
    codecContext.write(sequences, byteArrayOutputStream);
    assertThat(byteArrayOutputStream.toByteArray()).isEqualTo(asn1OerBytes);

    // Read...
    final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
    final TestSequenceOfSequence decodedValue = codecContext.read(TestSequenceOfSequence.class, byteArrayInputStream);

    // Write...
    final ByteArrayOutputStream byteArrayOutputStream2 = new ByteArrayOutputStream();
    codecContext.write(decodedValue, byteArrayOutputStream2);
    assertThat(byteArrayOutputStream2.toByteArray()).isEqualTo(asn1OerBytes);
  }

  /**
   * This implementation guards against DOS attacks by not allocating arrays based upon values in ASN.1 OER packets. For
   * example, one vector would be to assemble a Stream packet that with a quantity field set to {@link Short#MAX_VALUE},
   * but only supply a single sequence. Naive implementations might accidentally allocate arrays off of this value,
   * which could cause the program to exhaust the total avialble memory in the JVM, which would be a DOS vector. While
   * this implementation doesn't allocate arrays in that manner, this test validates that bytes are not read past the
   * total available bytes in the InputStream.
   */
  @Test
  public void readWithLengthOneTooBig() throws IOException {
    expectedException.expect(IOException.class);
    expectedException.expectMessage("Unable to properly decode 2 bytes (could only read 0 bytes)");

    // Write 1 SEQUENCE...
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    TestSequenceOfSequence sequences = new TestSequenceOfSequence();
    for (int i = 0; i < 1; i++) {
      sequences.add(i, new TestSequence(1, 2, 3));
    }
    codecContext.write(sequences, byteArrayOutputStream);

    // Twiddle the byte at index=1 to falsely indicate that the total SEQUENCES is 2.
    // byte[1] = 0x02
    byte[] copiedByteArray = byteArrayOutputStream.toByteArray();
    copiedByteArray[1] = (byte) 0x02;

    // Read...
    assertThat(copiedByteArray.length).isEqualTo(8);
    final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(copiedByteArray);
    codecContext.read(TestSequenceOfSequence.class, byteArrayInputStream); // throws exception
  }

  /**
   * This implementation guards against DOS attacks by not allocating arrays based upon values in ASN.1 OER packets. For
   * example, one vector would be to assemble a Stream packet that with a quantity field set to {@link Short#MAX_VALUE},
   * but only supply a single sequence. Naive implementations might accidentally allocate arrays off of this value,
   * which could cause the program to exhaust the total avialble memory in the JVM, which would be a DOS vector. While
   * this implementation doesn't allocate arrays in that manner, this test validates that bytes are not read past the
   * total available bytes in the InputStream.
   */
  @Test
  public void readWithLengthWayTooBig() throws IOException {
    expectedException.expect(IOException.class);
    expectedException.expectMessage("Unable to properly decode 2 bytes (could only read 0 bytes)");

    // Write 1 SEQUENCE...
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    TestSequenceOfSequence sequences = new TestSequenceOfSequence();
    for (int i = 0; i < 1; i++) {
      sequences.add(i, new TestSequence(1, 2, 3));
    }
    codecContext.write(sequences, byteArrayOutputStream);

    // Twiddle the byte at index=1 to falsely indicate that the total SEQUENCES is 255
    // byte[1] = 0xFF
    byte[] copiedByteArray = byteArrayOutputStream.toByteArray();
    copiedByteArray[1] = (byte) 0xFF;

    // Read...
    assertThat(copiedByteArray.length).isEqualTo(8);
    final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(copiedByteArray);
    codecContext.read(TestSequenceOfSequence.class, byteArrayInputStream); // throws exception
  }

  /**
   * A sequence of 3 UInt8 values.
   */
  private static class TestSequence {

    private int[] numbers;

    TestSequence(int... numbers) {
      this.numbers = numbers;
    }

    int[] getNumbers() {
      return numbers;
    }
  }

  private static class TestSequenceOfSequence extends ArrayList<TestSequence> {

  }

  private static class TestSequenceCodec extends AsnSequenceCodec<TestSequence> {

    public TestSequenceCodec() {
      super(
          new AsnUint16Codec(),
          new AsnUint16Codec(),
          new AsnUint16Codec()
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
