package org.interledger.encoding.asn.codecs;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.interledger.encoding.asn.framework.CodecException;

import org.junit.Test;

/**
 * Unit tests for {@link AsnOctetStringBasedObjectCodec}.
 */
public class AsnOctetStringBasedObjectCodecTest {

  @Test
  public void getAndSetBytesUnconstrained() {
    final AsnOctetStringBasedObjectCodec codec = createCodec(AsnSizeConstraint.UNCONSTRAINED);
    final byte[] bytes = new byte[1];
    codec.setBytes(bytes);
    assertThat(codec.getBytes(), is(bytes));
  }

  @Test
  public void setBytesToZero() {
    final AsnOctetStringBasedObjectCodec codec = createCodec(new AsnSizeConstraint(0));
    final byte[] bytes = new byte[0];
    codec.setBytes(bytes);
    assertThat(codec.getBytes(), is(bytes));
  }

  @Test(expected = CodecException.class)
  public void setBytesTooBig() {
    final AsnOctetStringBasedObjectCodec codec = createCodec(new AsnSizeConstraint(0, 1));
    final byte[] bytes = new byte[2];
    try {
      codec.setBytes(bytes);
    } catch (CodecException e) {
      assertThat(e.getMessage(), is("Invalid octet string length. Expected < 1, got 2"));
      throw e;
    }
  }

  @Test(expected = CodecException.class)
  public void setBytesTooBigForFixedSize() {
    final AsnOctetStringBasedObjectCodec codec = createCodec(new AsnSizeConstraint(1, 1));
    final byte[] bytes = new byte[2];
    try {
      codec.setBytes(bytes);
    } catch (CodecException e) {
      assertThat(e.getMessage(), is("Invalid octet string length. Expected 1, got 2"));
      throw e;
    }
  }

  @Test(expected = CodecException.class)
  public void setBytesTooSmall() {
    final AsnOctetStringBasedObjectCodec codec = createCodec(new AsnSizeConstraint(1, 2));
    final byte[] bytes = new byte[0];
    try {
      codec.setBytes(bytes);
    } catch (CodecException e) {
      assertThat(e.getMessage(), is("Invalid octet string length. Expected > 1, got 0"));
      throw e;
    }
  }

  @Test(expected = CodecException.class)
  public void setBytesTooSmallForFixedSize() {
    final AsnOctetStringBasedObjectCodec codec = createCodec(new AsnSizeConstraint(1, 1));
    final byte[] bytes = new byte[0];
    try {
      codec.setBytes(bytes);
    } catch (CodecException e) {
      assertThat(e.getMessage(), is("Invalid octet string length. Expected 1, got 0"));
      throw e;
    }
  }

  @Test
  public void testSetEquals() {
    final AsnOctetStringBasedObjectCodec codec1 = createCodec(new AsnSizeConstraint(1, 1));
    final AsnOctetStringBasedObjectCodec codec2 = createCodec(new AsnSizeConstraint(1));
    final AsnOctetStringBasedObjectCodec codec3 = createCodec(new AsnSizeConstraint(0));

    assertThat(codec1, is(codec2));
    assertThat(codec1.equals(codec2), is(true));
    assertThat(codec2.equals(codec1), is(true));
    assertThat(codec1.equals(codec3), is(false));
    assertThat(codec2.equals(codec3), is(false));
    assertThat(codec3.equals(codec1), is(false));
  }

  private AsnOctetStringBasedObjectCodec createCodec(AsnSizeConstraint constraint) {
    return new AsnOctetStringBasedObjectCodec(constraint) {
      @Override
      public Object decode() {
        return "foo";
      }

      @Override
      public void encode(Object value) {
        // No-op.
      }
    };
  }
}