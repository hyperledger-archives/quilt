package org.interledger.encoding.asn.codecs;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * Unit test for {@link AsnSequenceOfSequenceCodec}.
 */
public class AsnSequenceOfSequenceCodecTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private AsnSequenceOfSequenceCodec codec;

  @Before
  public void setup() {
    // Used to uniquely identify sub-codecs.
    final AtomicInteger subCodecIdentifier = new AtomicInteger(0);

    final Supplier<ArrayList> listSupplier = () -> new ArrayList<>(5);
    final Supplier<AsnSequenceCodec<Integer>> subCodecSupplier
      = () -> new TestSubCodec(subCodecIdentifier.getAndIncrement());

    codec = new AsnSequenceOfSequenceCodec(listSupplier, subCodecSupplier);
  }

  /**
   * Validates that the codecs List supplier passed in during construction is always used to create a new list.
   */
  @Test
  public void testConstructedCodecsSizeAlwaysNonZero() {
    // Always use the same list, instead of the supplier creating a new list on every call.
    final Supplier<ArrayList> listSupplier = () -> new ArrayList<>(0);

    final Supplier<AsnSequenceCodec<Integer>> subCodecSupplier = () -> new TestSubCodec(123);
    codec = new AsnSequenceOfSequenceCodec(listSupplier, subCodecSupplier);

    for (int i = 0; i < 20; i++) {
      // If the supplier isn't translated correctly, then it will be re-used, and the size of codecs will grow, which is
      // an error in the implementation. This test validates this doesn't happen.
      codec.getCodecAt(i);

      List list = codec.decode();
      assertThat(list.size()).isEqualTo(i + 1);
    }
  }

  @Test
  public void getCodecAtNegativeIndex() {
    expectedException.expect(IndexOutOfBoundsException.class);
    codec.getCodecAt(-1);
  }

  /**
   * Asserts that a small codec list will grow dynamically, and that an ordered access succeeds properly.
   */
  @Test
  public void getCodecWithListThatIsEmpty() {
    // Assemble a list with 5 values.
    final ArrayList initialValues = new ArrayList();
    for (int i = 0; i < 5; i++) {
      initialValues.add(i, i);
    }
    // This merely sets the internal `codecs` object to have a length of 5.
    codec.encode(initialValues); // Assign empty list.

    // Actual test
    final int size = 20;
    for (int i = 0; i < size; i++) {
      final TestSubCodec subCodec = (TestSubCodec) codec.getCodecAt(i);
      assertThat(subCodec).isNotNull();
      assertThat(subCodec.getIdentifier()).isEqualTo(i);
    }
    assertThat(codec.size()).isEqualTo(size);

    // Do this cycle again to assert that a re-loop through the codec keeps all of its indices correct.
    for (int i = 0; i < size; i++) {
      final TestSubCodec subCodec = (TestSubCodec) codec.getCodecAt(i);
      assertThat(subCodec).isNotNull();
      assertThat(subCodec.getIdentifier()).isEqualTo(i);
    }
    assertThat(codec.size()).isEqualTo(size);
  }

  /**
   * We want to ensure that getCodec returns the proper value on successive calls with the same index, even when the
   * index being asked for is potentially far away from the nearest valid index. This test validates that the algorithm
   * behind getCodec doesn't assume sub-codecs are added in-order, but may be requested & added in any order.
   */
  @Test
  public void getCodecAtWithRandomAccess() {
    expectedException.expect(IndexOutOfBoundsException.class);
    codec.getCodecAt(100);
  }

  @Test
  public void testEncodeWithNullCodecsList() {
    expectedException.expect(NullPointerException.class);
    codec.encode(null);
  }

  /**
   * A class used for testing.
   */
  private static class TestSubCodec extends AsnSequenceCodec<Integer> {

    // Used to uniquely identify the instance of sub-codec that is constructed so that this test harness can validate
    // index lookups properly.
    private int identifier;

    TestSubCodec(int identifier) {
      this.identifier = identifier;
    }

    @Override
    public Integer decode() {
      // NOTE: Unused, defined only to satisfy Java.
      return identifier;
    }

    @Override
    public void encode(final Integer value) {
      // NOTE: Unused, defined only to satisfy Java.
      this.identifier = value;
    }

    int getIdentifier() {
      return identifier;
    }
  }
}
