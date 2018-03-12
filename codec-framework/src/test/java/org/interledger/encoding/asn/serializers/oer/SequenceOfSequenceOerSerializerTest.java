package org.interledger.encoding.asn.serializers.oer;

import org.interledger.encoding.asn.codecs.AsnSequenceCodec;
import org.interledger.encoding.asn.codecs.AsnSequenceOfSequenceCodec;
import org.interledger.encoding.asn.codecs.AsnUint8Codec;
import org.interledger.encoding.asn.framework.CodecContext;
import org.interledger.encoding.asn.framework.CodecContextFactory;

import com.google.common.io.BaseEncoding;
import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.function.Supplier;

/**
 * Parameterized unit tests for encoding an instance of {@link Integer}.
 */
@RunWith(Parameterized.class)
public class SequenceOfSequenceOerSerializerTest {

  private final int[][] inputValue;
  private final byte[] asn1OerBytes;
  private CodecContext codecContext;

  /**
   * Construct an instance of this parameterized test with the supplied inputs.
   *
   * @param inputValue   A {@code int} representing the unsigned 8bit integer to write in OER
   *                     encoding.
   * @param asn1OerBytes The expected value, in binary, of the supplied {@code intValue}.
   */
  public SequenceOfSequenceOerSerializerTest(final int[][] inputValue, final byte[] asn1OerBytes) {
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
          new int[][]{
            new int[]{0,0,0}
          },
          BaseEncoding.base16().decode("0101000000")
        },
        {
          new int[][]{
            new int[]{1,2,3},
            new int[]{1,2,3}
          },
          BaseEncoding.base16().decode("0102010203010203")
        },
        {
          new int[][]{
            new int[]{0,1,255},
            new int[]{0,1,255},
            new int[]{0,1,255},
            new int[]{0,1,255},
            new int[]{255,0,1}
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
    codecContext = CodecContextFactory.getContext(CodecContextFactory.OCTET_ENCODING_RULES);
    codecContext.register(TestSequence.class, TestSequenceCodec::new);
    codecContext.register(TestSequenceOfSequence.class, TestSequenceOfSequenceCodec::new);
  }

  @Test
  public void read() throws Exception {
    final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(asn1OerBytes);
    final TestSequenceOfSequence actualValue = codecContext.read(TestSequenceOfSequence.class, byteArrayInputStream);
    for (int i = 0; i < inputValue.length; i++) {
      TestSequence sequence = actualValue.get(i);
      for (int j = 0; j < inputValue[i].length; j++) {
        MatcherAssert.assertThat(sequence.getNumbers()[j], CoreMatchers.is(inputValue[i][j]));
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
    MatcherAssert.assertThat(byteArrayOutputStream.toByteArray(), CoreMatchers.is(asn1OerBytes));
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
    MatcherAssert.assertThat(byteArrayOutputStream.toByteArray(), CoreMatchers.is(asn1OerBytes));

    // Read...
    final ByteArrayInputStream byteArrayInputStream =
        new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
    final TestSequenceOfSequence decodedValue = codecContext
        .read(TestSequenceOfSequence.class, byteArrayInputStream);

    // Write...
    final ByteArrayOutputStream byteArrayOutputStream2 = new ByteArrayOutputStream();
    codecContext.write(decodedValue, byteArrayOutputStream2);
    MatcherAssert.assertThat(byteArrayOutputStream2.toByteArray(), CoreMatchers.is(asn1OerBytes));
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
