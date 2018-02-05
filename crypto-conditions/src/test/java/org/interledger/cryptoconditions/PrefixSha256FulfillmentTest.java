package org.interledger.cryptoconditions;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.interledger.cryptoconditions.helpers.TestFulfillmentFactory.PREIMAGE1;
import static org.junit.Assert.assertTrue;

import org.interledger.cryptoconditions.helpers.TestFulfillmentFactory;

import org.junit.Test;

import java.util.Base64;

/**
 * Unit tests {@link PrefixSha256Fulfillment}.
 */
public class PrefixSha256FulfillmentTest {

  private static final String PREFIX = "when this baby hits 88 miles per hour";
  private static final String PREFIX2 = "Nobody calls me chicken!";
  private static final String ENCODED_PREFIX
      = "d2hlbiB0aGlzIGJhYnkgaGl0cyA4OCBtaWxlcyBwZXIgaG91cg==";
  private static final String ENCODED_FINGERPRINT = "iL1xV1F0IvtokoaU1n2eVOvcwhy4me4vroUKg8vFnOE";
  private static final byte[] FINGERPRINT_BYTES = Base64.getUrlDecoder()
      .decode(ENCODED_FINGERPRINT);

  private static final PreimageSha256Fulfillment SUBFULFILLMENT = TestFulfillmentFactory
      .constructPreimageFulfillment(PREIMAGE1);

  private static final PrefixSha256Fulfillment TEST_FULFILLMENT = PrefixSha256Fulfillment.from(
      PREFIX.getBytes(),
      100,
      SUBFULFILLMENT
  );

  @Test(expected = NullPointerException.class)
  public final void testFromWithNullPrefix() {
    PrefixSha256Fulfillment.from(null, 37, SUBFULFILLMENT);
  }

  @Test(expected = IllegalArgumentException.class)
  public final void testFromWithNegativeMaxMessageLength() {
    try {
      PrefixSha256Fulfillment.from(PREFIX.getBytes(), -10, SUBFULFILLMENT);
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage(), is("Maximum message length must not be negative!"));
      throw e;
    }
  }

  @Test(expected = NullPointerException.class)
  public final void testFromWithNullSubFulfillment() {
    PrefixSha256Fulfillment.from(PREFIX.getBytes(), 37, null);
  }

  @Test
  public final void testValidateDerivedCondition() {
    final PrefixSha256Fulfillment actual = TestFulfillmentFactory
        .constructPrefixSha256Fulfillment(PREFIX);
    assertTrue("Invalid condition", actual.verify(actual.getCondition(), new byte[]{}));
  }

  @Test
  public final void testValidateDerivedConditionWithEmptyMessage() {
    final PrefixSha256Fulfillment actual = TestFulfillmentFactory
        .constructPrefixSha256Fulfillment(PREFIX);
    assertTrue("Invalid condition", actual.verify(actual.getCondition()));
  }

  @Test
  public void testGettersAndSetters() {
    final PrefixSha256Fulfillment actual = TestFulfillmentFactory
        .constructPrefixSha256Fulfillment(PREFIX);

    assertThat(actual.getSubfulfillment(),
        is(TestFulfillmentFactory.constructPreimageFulfillment(PREIMAGE1))
    );
    assertThat(actual.getPrefixBase64Url(),
        is("d2hlbiB0aGlzIGJhYnkgaGl0cyA4OCBtaWxlcyBwZXIgaG91cg=="));
    assertThat(actual.getPrefix(), is(Base64.getDecoder().decode(actual.getPrefixBase64Url())));
    assertThat(actual.getType(), is(CryptoConditionType.PREFIX_SHA256));
    assertThat(actual.getCondition(), is(not(nullValue())));
  }

  @Test
  public void equalsHashcode() {
    final PrefixSha256Fulfillment fulfillment1
        = TestFulfillmentFactory.constructPrefixSha256Fulfillment(PREFIX);
    final PrefixSha256Fulfillment fulfillment2
        = TestFulfillmentFactory.constructPrefixSha256Fulfillment(PREFIX);
    final PrefixSha256Fulfillment fulfillment3
        = TestFulfillmentFactory.constructPrefixSha256Fulfillment(PREFIX2);

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
    final PrefixSha256Fulfillment fulfillment
        = TestFulfillmentFactory.constructPrefixSha256Fulfillment(PREFIX);

    assertThat(fulfillment.toString(),
        is("PrefixSha256Fulfillment{"
            + "prefix=d2hlbiB0aGlzIGJhYnkgaGl0cyA4OCBtaWxlcyBwZXIgaG91cg==, "
            + "maxMessageLength=1000, "
            + "subfulfillment=PreimageSha256Fulfillment{"
            + "encodedPreimage=Um9hZHM_IFdoZXJlIHdlJ3JlIGdvaW5nLCB3ZSBkb24ndCBuZWVkIHJvYWRzLg==, "
            + "type=PREIMAGE-SHA-256, "
            + "condition=PreimageSha256Condition{"
            + "type=PREIMAGE-SHA-256, "
            + "fingerprint=-28EVNr7rOwQ_XsvrJVxLvjBY38ZNZlHaPHYpsIbmH4, "
            + "cost=46}}, "
            + "type=PREFIX-SHA-256, "
            + "condition=PrefixSha256Condition{"
            + "subtypes=[PREIMAGE-SHA-256], "
            + "type=PREFIX-SHA-256, "
            + "fingerprint=2ugoaAzCSomLbveq9nNmSJp5X-esBSjBw5IGFgvYF9w, "
            + "cost=2107"
            + "}}"));
  }
}