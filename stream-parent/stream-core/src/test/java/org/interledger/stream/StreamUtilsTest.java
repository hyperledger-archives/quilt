package org.interledger.stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

import org.interledger.core.InterledgerFulfillment;

import com.google.common.primitives.UnsignedLong;
import org.junit.Test;

/**
 * Unit tests for {@link StreamUtils}.
 *
 * Note: Test values were taken from the Interledger-rs project, which mimic the Interledger-js test values.
 *
 * @see "https://github.com/interledger-rs/interledger-rs/blob/master/crates/interledger-stream/src/crypto.rs"
 */
public class StreamUtilsTest {

  private static final byte[] SHARED_SECRET = new byte[] {
      (byte) 126, (byte) 219, (byte) 117, (byte) 93, (byte) 118, (byte) 248, (byte) 249, (byte) 211, (byte) 20,
      (byte) 211, (byte) 65, (byte) 110, (byte) 237, (byte) 80, (byte) 253, (byte) 179, (byte) 81, (byte) 146,
      (byte) 229, (byte) 67, (byte) 231, (byte) 49, (byte) 92, (byte) 127, (byte) 254, (byte) 230, (byte) 144,
      (byte) 102, (byte) 103, (byte) 166, (byte) 150, (byte) 36
  };

  private static final byte[] DATA = new byte[] {
      (byte) 119, (byte) 248, (byte) 213, (byte) 234, (byte) 63, (byte) 200, (byte) 224, (byte) 140, (byte) 212,
      (byte) 222, (byte) 105, (byte) 159, (byte) 246, (byte) 203, (byte) 66, (byte) 155, (byte) 151, (byte) 172,
      (byte) 68, (byte) 24, (byte) 76, (byte) 232, (byte) 90, (byte) 10, (byte) 237, (byte) 146, (byte) 189, (byte) 73,
      (byte) 248, (byte) 196, (byte) 177, (byte) 108, (byte) 115, (byte) 223
  };

  private static final byte[] FULFILLMENT = new byte[] {
      (byte) 24, (byte) 6, (byte) 56, (byte) 73, (byte) 229, (byte) 236, (byte) 88, (byte) 227, (byte) 82, (byte) 112,
      (byte) 152, (byte) 49, (byte) 152, (byte) 73, (byte) 182, (byte) 183, (byte) 198, (byte) 7, (byte) 233,
      (byte) 124, (byte) 119, (byte) 65, (byte) 13, (byte) 68, (byte) 54, (byte) 108, (byte) 120, (byte) 193, (byte) 59,
      (byte) 226, (byte) 107, (byte) 39
  };

  @Test
  public void generatedFulfillableFulfillment() {
    InterledgerFulfillment fulfillment = StreamUtils.generatedFulfillableFulfillment(SHARED_SECRET, DATA);

    assertThat(fulfillment.getPreimage()).isEqualTo(FULFILLMENT);
    assertThat(fulfillment).isEqualTo(InterledgerFulfillment.of(FULFILLMENT));
  }

  @Test(expected = NullPointerException.class)
  public void minWithNullFirst() {
    try {
      StreamUtils.min(null, UnsignedLong.ONE);
      fail();
    } catch (NullPointerException e) {
      assertThat(e.getMessage()).isNull();
      throw e;
    }
  }

  @Test(expected = NullPointerException.class)
  public void minWithNullSecond() {
    try {
      StreamUtils.min(UnsignedLong.ONE, null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e.getMessage()).isNull();
      throw e;
    }
  }

  @Test
  public void minTests() {
    assertThat(StreamUtils.min(UnsignedLong.ZERO, UnsignedLong.ZERO)).isEqualTo(UnsignedLong.ZERO);
    assertThat(StreamUtils.min(UnsignedLong.ONE, UnsignedLong.ZERO)).isEqualTo(UnsignedLong.ZERO);
    assertThat(StreamUtils.min(UnsignedLong.ZERO, UnsignedLong.ONE)).isEqualTo(UnsignedLong.ZERO);
    assertThat(StreamUtils.min(UnsignedLong.MAX_VALUE, UnsignedLong.ZERO)).isEqualTo(UnsignedLong.ZERO);
    assertThat(StreamUtils.min(UnsignedLong.ZERO, UnsignedLong.MAX_VALUE)).isEqualTo(UnsignedLong.ZERO);
    assertThat(StreamUtils.min(UnsignedLong.ONE, UnsignedLong.MAX_VALUE)).isEqualTo(UnsignedLong.ONE);
    assertThat(StreamUtils.min(UnsignedLong.MAX_VALUE, UnsignedLong.ONE)).isEqualTo(UnsignedLong.ONE);
  }

  @Test
  public void maxTests() {
    assertThat(StreamUtils.max(UnsignedLong.ZERO, UnsignedLong.ZERO)).isEqualTo(UnsignedLong.ZERO);
    assertThat(StreamUtils.max(UnsignedLong.ONE, UnsignedLong.ZERO)).isEqualTo(UnsignedLong.ONE);
    assertThat(StreamUtils.max(UnsignedLong.ZERO, UnsignedLong.ONE)).isEqualTo(UnsignedLong.ONE);
    assertThat(StreamUtils.max(UnsignedLong.MAX_VALUE, UnsignedLong.ZERO)).isEqualTo(UnsignedLong.MAX_VALUE);
    assertThat(StreamUtils.max(UnsignedLong.ZERO, UnsignedLong.MAX_VALUE)).isEqualTo(UnsignedLong.MAX_VALUE);
    assertThat(StreamUtils.max(UnsignedLong.ONE, UnsignedLong.MAX_VALUE)).isEqualTo(UnsignedLong.MAX_VALUE);
    assertThat(StreamUtils.max(UnsignedLong.MAX_VALUE, UnsignedLong.ONE)).isEqualTo(UnsignedLong.MAX_VALUE);
  }
}
