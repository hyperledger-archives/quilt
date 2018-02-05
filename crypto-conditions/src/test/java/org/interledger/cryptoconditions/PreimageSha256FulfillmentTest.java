package org.interledger.cryptoconditions;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Base64;

/**
 * Unit tests {@link PreimageSha256Fulfillment}.
 */
public class PreimageSha256FulfillmentTest {

  private static final String PREIMAGE = "when this baby hits 88 miles per hour";
  private static final String PREIMAGE2 = "Nobody calls me chicken!";
  private static final String ENCODED_PREIMAGE
      = "d2hlbiB0aGlzIGJhYnkgaGl0cyA4OCBtaWxlcyBwZXIgaG91cg==";
  private static final String ENCODED_FINGERPRINT = "iL1xV1F0IvtokoaU1n2eVOvcwhy4me4vroUKg8vFnOE";
  private static final byte[] FINGERPRINT_BYTES = Base64.getUrlDecoder()
      .decode(ENCODED_FINGERPRINT);

  private static final PreimageSha256Condition TEST_CONDITION
      = PreimageSha256Condition.fromCostAndFingerprint(
      37,
      FINGERPRINT_BYTES
  );

  @Test(expected = NullPointerException.class)
  public final void testNullPreimage() {
    PreimageSha256Fulfillment.from(null);
  }

  @Test
  public final void testGetCondition() {
    final PreimageSha256Fulfillment fulfillment
        = PreimageSha256Fulfillment.from(PREIMAGE.getBytes());
    assertEquals("Wrong condition", TEST_CONDITION, fulfillment.getCondition());
  }

  @Test
  public final void testValidate() {
    final PreimageSha256Fulfillment actual
        = PreimageSha256Fulfillment.from(PREIMAGE.getBytes());
    assertTrue("Invalid condition", actual.verify(TEST_CONDITION, new byte[]{}));
  }

  @Test
  public final void testValidateWithEmptyMessage() {
    final PreimageSha256Fulfillment actual
        = PreimageSha256Fulfillment.from(PREIMAGE.getBytes());
    assertTrue("Invalid condition", actual.verify(TEST_CONDITION));
  }

  @Test
  public void testGettersAndSetters() {
    final PreimageSha256Fulfillment actual
        = PreimageSha256Fulfillment.from(PREIMAGE.getBytes());
    assertThat(actual.getEncodedPreimage(), is(ENCODED_PREIMAGE));
    assertThat(actual.getType(), is(CryptoConditionType.PREIMAGE_SHA256));
    assertThat(actual.getCondition(), is(not(nullValue())));
  }

  @Test
  public void equalsHashcode() {
    final PreimageSha256Fulfillment fulfillment1
        = PreimageSha256Fulfillment.from(PREIMAGE.getBytes());
    final PreimageSha256Fulfillment fulfillment2
        = PreimageSha256Fulfillment.from(PREIMAGE.getBytes());
    final PreimageSha256Fulfillment fulfillment3
        = PreimageSha256Fulfillment.from(PREIMAGE2.getBytes());

    assertThat(fulfillment1.equals(fulfillment1), is(true));
    assertThat(fulfillment2.equals(fulfillment2), is(true));
    assertThat(fulfillment3.equals(fulfillment3), is(true));

    assertThat(fulfillment1.equals(fulfillment2), is(true));
    assertThat(fulfillment1.equals(fulfillment3), is(false));

    assertThat(fulfillment2.equals(fulfillment1), is(true));
    assertThat(fulfillment2.equals(fulfillment3), is(false));

    assertThat(fulfillment3.equals(fulfillment1), is(false));
    assertThat(fulfillment3.equals(fulfillment2), is(false));

    assertThat(fulfillment1.hashCode(), is(fulfillment2.hashCode()));
    assertThat(fulfillment1.hashCode() == fulfillment3.hashCode(), is(false));
  }

  @Test
  public void testToString() {
    final PreimageSha256Fulfillment fulfillment
        = PreimageSha256Fulfillment.from(PREIMAGE.getBytes());

    assertThat(fulfillment.toString(),
        is("PreimageSha256Fulfillment{"
            + "encodedPreimage=d2hlbiB0aGlzIGJhYnkgaGl0cyA4OCBtaWxlcyBwZXIgaG91cg==, "
            + "type=PREIMAGE-SHA-256, "
            + "condition=PreimageSha256Condition{type=PREIMAGE-SHA-256, "
            + "fingerprint=iL1xV1F0IvtokoaU1n2eVOvcwhy4me4vroUKg8vFnOE,"
            + " cost=37"
            + "}}"));
  }
}