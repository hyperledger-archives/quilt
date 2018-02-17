package org.interledger.cryptoconditions;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.interledger.cryptoconditions.helpers.TestFulfillmentFactory.PREIMAGE1;
import static org.interledger.cryptoconditions.helpers.TestFulfillmentFactory.constructPrefixSha256Fulfillment;
import static org.junit.Assert.assertTrue;

import org.interledger.cryptoconditions.helpers.TestFulfillmentFactory;

import org.junit.Test;

import java.util.Base64;
import java.util.UUID;

/**
 * Unit tests {@link PrefixSha256Fulfillment}.
 */
public class PrefixSha256FulfillmentTest extends AbstractCryptoConditionTest {

  private static final String PREFIX = "when this baby hits 88 miles per hour";
  private static final String PREFIX2 = "Nobody calls me chicken!";
  private static final String ENCODED_PREFIX
      = "d2hlbiB0aGlzIGJhYnkgaGl0cyA4OCBtaWxlcyBwZXIgaG91cg==";
  private static final String ENCODED_FINGERPRINT = "-28EVNr7rOwQ_XsvrJVxLvjBY38ZNZlHaPHYpsIbmH4";

  private static final PreimageSha256Fulfillment SUBFULFILLMENT = TestFulfillmentFactory
      .constructPreimageFulfillment(PREIMAGE1);

  /**
   * Tests concurrently creating an instance of {@link RsaSha256Fulfillment}. This test validates
   * the fix for Github issue #40 where construction of this class was not thread-safe.
   *
   * @see "https://github.com/interledger/java-crypto-conditions/issues/40"
   * @see "https://github.com/junit-team/junit4/wiki/multithreaded-code-and-concurrency"
   */
  @Test
  public void testConstructionUsingMultipleThreads() throws Exception {
    final Runnable runnableTest = () -> {
      final PrefixSha256Fulfillment preimageSha256Fulfillment =
          constructPrefixSha256Fulfillment(UUID.randomUUID().toString());
      assertThat(preimageSha256Fulfillment.getType(), is(CryptoConditionType.PREFIX_SHA256));
      assertThat(preimageSha256Fulfillment.verify(preimageSha256Fulfillment.getDerivedCondition()),
          is(true));
    };

    // Run single-threaded...
    this.runConcurrent(1, runnableTest);
    // Run multi-threaded...
    this.runConcurrent(runnableTest);
  }

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
    assertTrue("Invalid condition", actual.verify(actual.getDerivedCondition(), new byte[]{}));
  }

  @Test
  public final void testValidateDerivedConditionWithEmptyMessage() {
    final PrefixSha256Fulfillment actual = TestFulfillmentFactory
        .constructPrefixSha256Fulfillment(PREFIX);
    assertTrue("Invalid condition", actual.verify(actual.getDerivedCondition()));
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
    assertThat(actual.getDerivedCondition(), is(not(nullValue())));
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
            + "prefix=" + ENCODED_PREFIX + ", "
            + "maxMessageLength=1000, "
            + "subfulfillment=PreimageSha256Fulfillment{"
            + "encodedPreimage=Um9hZHM_IFdoZXJlIHdlJ3JlIGdvaW5nLCB3ZSBkb24ndCBuZWVkIHJvYWRzLg==, "
            + "type=PREIMAGE-SHA-256, "
            + "derivedCondition=PreimageSha256Condition{"
            + "type=PREIMAGE-SHA-256, "
            + "fingerprint=" + ENCODED_FINGERPRINT + ", "
            + "cost=46}}, "
            + "type=PREFIX-SHA-256, "
            + "derivedCondition=PrefixSha256Condition{"
            + "subtypes=[PREIMAGE-SHA-256], "
            + "type=PREFIX-SHA-256, "
            + "fingerprint=2ugoaAzCSomLbveq9nNmSJp5X-esBSjBw5IGFgvYF9w, "
            + "cost=2107"
            + "}}"));
  }
}