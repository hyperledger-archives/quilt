package org.interledger.cryptoconditions;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.interledger.cryptoconditions.CryptoConditionType.THRESHOLD_SHA256;
import static org.interledger.cryptoconditions.helpers.TestFulfillmentFactory.MESSAGE;
import static org.interledger.cryptoconditions.helpers.TestFulfillmentFactory.PREIMAGE1;

import org.interledger.cryptoconditions.der.DerEncodingException;
import org.interledger.cryptoconditions.helpers.TestConditionFactory;
import org.interledger.cryptoconditions.helpers.TestFulfillmentFactory;

import com.google.common.collect.Lists;
import com.google.common.io.BaseEncoding;
import org.hamcrest.CoreMatchers;
import org.junit.Test;

/**
 * Unit tests for {@link ThresholdSha256Fulfillment}.
 */
public class ThresholdSha256FulfillmentTest extends AbstractFactoryTest {

  /**
   * Tests concurrently creating an instance of {@link ThresholdSha256Fulfillment}. This test
   * validates the fix for Github issue #40 where construction of this class was not thread-safe.
   *
   * @see "https://github.com/interledger/java-crypto-fulfillments/issues/40"
   * @see "https://github.com/junit-team/junit4/wiki/multithreaded-code-and-concurrency"
   */
  @Test
  public void testConstructionUsingMultipleThreads() throws Exception {
    final Runnable runnableTest = () -> {

      final ThresholdSha256Fulfillment thresholdSha256Fulfillment = TestFulfillmentFactory
          .constructThresholdFulfillment();

      assertThat(thresholdSha256Fulfillment.getType(), is(THRESHOLD_SHA256));
      assertThat(thresholdSha256Fulfillment
          .verify(thresholdSha256Fulfillment.getDerivedCondition(), MESSAGE.getBytes()), is(true));

      try {
        assertThat(BaseEncoding.base64()
                .encode(CryptoConditionWriter.writeFulfillment(thresholdSha256Fulfillment)),
            is("ooGmoHmgMIAuUm9hZHM/IFdoZXJlIHdlJ3JlIGdvaW5nLCB3ZSBkb24ndCBuZWVkIHJvYWRzLqFFg"
                + "AtPcmRlci0xMjM0NYECA+iiMqAwgC5Sb2Fkcz8gV2hlcmUgd2UncmUgZ29pbmcsIHdlIGRvbid0IG5lZ"
                + "WQgcm9hZHMuoSmjJ4Agsx+oIG5Op+UVM3s7Mwgrh3ZRgBCF7YT7Ta6yR79pjX+BAwEAAA=="));
      } catch (DerEncodingException e) {
        throw new RuntimeException(e);
      }
    };

    this.runConcurrent(1, runnableTest);
    this.runConcurrent(runnableTest);
  }

  @Test
  public void equalsHashcodeTest() {
    final ThresholdSha256Fulfillment fulfillment1 = TestFulfillmentFactory
        .constructThresholdFulfillment();
    final ThresholdSha256Fulfillment fulfillment2 = TestFulfillmentFactory
        .constructThresholdFulfillment();
    final ThresholdSha256Fulfillment fulfillment3 = ThresholdSha256Fulfillment.from(
        Lists.newArrayList(TestConditionFactory.constructPreimageCondition(PREIMAGE1)),
        Lists.newArrayList(TestFulfillmentFactory.constructPreimageFulfillment(PREIMAGE1))
    );

    assertThat(fulfillment1.equals(fulfillment1), CoreMatchers.is(true));
    assertThat(fulfillment2.equals(fulfillment2), CoreMatchers.is(true));
    assertThat(fulfillment3.equals(fulfillment3), CoreMatchers.is(true));

    assertThat(fulfillment1.equals(fulfillment2), CoreMatchers.is(true));
    assertThat(fulfillment1.equals(fulfillment3), CoreMatchers.is(false));

    assertThat(fulfillment2.equals(fulfillment1), CoreMatchers.is(true));
    assertThat(fulfillment2.equals(fulfillment3), CoreMatchers.is(false));

    assertThat(fulfillment3.equals(fulfillment1), CoreMatchers.is(false));
    assertThat(fulfillment3.equals(fulfillment2), CoreMatchers.is(false));

    assertThat(fulfillment1.hashCode(), CoreMatchers.is(fulfillment2.hashCode()));
    assertThat(fulfillment1.hashCode() == fulfillment3.hashCode(), CoreMatchers.is(false));
  }

  @Test
  public void toStringTest() {
    final ThresholdSha256Fulfillment thresholdSha256Fulfillment = TestFulfillmentFactory
        .constructThresholdFulfillment();
    assertThat(thresholdSha256Fulfillment.toString(), is(
        "ThresholdSha256Fulfillment{"
            + "subconditions=[RsaSha256Condition{"
            + "type=RSA-SHA-256, "
            + "fingerprint=sx-oIG5Op-UVM3s7Mwgrh3ZRgBCF7YT7Ta6yR79pjX8, "
            + "cost=65536}], "
            + "subfulfillments=[PreimageSha256Fulfillment{"
            + "encodedPreimage=Um9hZHM_IFdoZXJlIHdlJ3JlIGdvaW5nLCB3ZSBkb24ndCBuZWVkIHJvYWRzLg==, "
            + "type=PREIMAGE-SHA-256, "
            + "derivedCondition=PreimageSha256Condition{"
            + "type=PREIMAGE-SHA-256, "
            + "fingerprint=-28EVNr7rOwQ_XsvrJVxLvjBY38ZNZlHaPHYpsIbmH4, "
            + "cost=46}}, "
            + "PrefixSha256Fulfillment{prefix=T3JkZXItMTIzNDU=, "
            + "maxMessageLength=1000, "
            + "subfulfillment=PreimageSha256Fulfillment{"
            + "encodedPreimage=Um9hZHM_IFdoZXJlIHdlJ3JlIGdvaW5nLCB3ZSBkb24ndCBuZWVkIHJvYWRzLg==,"
            + " type=PREIMAGE-SHA-256, "
            + "derivedCondition=PreimageSha256Condition{"
            + "type=PREIMAGE-SHA-256, "
            + "fingerprint=-28EVNr7rOwQ_XsvrJVxLvjBY38ZNZlHaPHYpsIbmH4, "
            + "cost=46}}, "
            + "type=PREFIX-SHA-256, "
            + "derivedCondition=PrefixSha256Condition{subtypes=[PREIMAGE-SHA-256], "
            + "type=PREFIX-SHA-256, fingerprint=KrV_YYvQMc_mAKpg73Kngfld3lFoZdUQ8FEtQf4m13g, "
            + "cost=2081}}], type=THRESHOLD-SHA-256, "
            + "derivedCondition=ThresholdSha256Condition{subtypes=[PREIMAGE-SHA-256, "
            + "PREFIX-SHA-256, RSA-SHA-256], type=THRESHOLD-SHA-256, "
            + "fingerprint=A5TzFI1QC1rIxsIIetjZNk0g2VCsWT22ylqFuZwPxVU, cost=70689}}"
    ));
  }

  @Test
  public void testOneOfTwoThreshold() {
    // Construct a One-of-Two Threshold Condition using the tow different sub-conditions. This means
    // that in order to fulfill this condition, a ThresholdFulfillment containing one or both of the
    // sub-fulfillments must be published.
    final ThresholdSha256Condition oneOfTwoCondition = ThresholdSha256Condition.from(
        1, Lists.newArrayList(subcondition1, subcondition2)
    );

    // In order to fulfill a threshold condition, the count of the sub-fulfillments MUST be equal to
    // the threshold. Thus, construct a Fulfillment with only subfulfillment2, and expect it to  verify
    // properly against oneOfTwoCondition.
    final ThresholdSha256Fulfillment fulfillmentWithFulfillment1 = ThresholdSha256Fulfillment.from(
        Lists.newArrayList(subcondition2),
        Lists.newArrayList(subfulfillment1)
    );
    assertThat(fulfillmentWithFulfillment1.verify(oneOfTwoCondition, new byte[0]), is(true));

    // In order to fulfill a threshold condition, the count of the sub-fulfillments MUST be equal to
    // the threshold. Thus, construct a Fulfillment with only fulfillment2, and expect it to  verify
    // properly against oneOfTwoCondition.
    final ThresholdSha256Fulfillment fulfillmentWithFulfillment2 = ThresholdSha256Fulfillment.from(
        Lists.newArrayList(subcondition1),
        Lists.newArrayList(subfulfillment2)
    );
    assertThat(fulfillmentWithFulfillment2.verify(oneOfTwoCondition, new byte[0]), is(true));

    // No fulfillments
    final ThresholdSha256Fulfillment fulfillmentWithNoFulfillments = ThresholdSha256Fulfillment
        .from(
            Lists.newArrayList(subcondition1, subcondition2),
            Lists.newArrayList()
        );
    assertThat(fulfillmentWithNoFulfillments.verify(oneOfTwoCondition, new byte[0]), is(false));

    // In order to fulfill a threshold condition, the count of the sub-fulfillments MUST be equal to
    // the threshold. Thus, construct a Fulfillment with two fulfillments, and expect it to NOT
    // verify properly against oneOfTwoCondition.
    final ThresholdSha256Fulfillment fulfillmentWithTwoFulfillments = ThresholdSha256Fulfillment
        .from(
            Lists.newArrayList(),
            Lists.newArrayList(subfulfillment1, subfulfillment2)
        );
    assertThat(fulfillmentWithTwoFulfillments.verify(oneOfTwoCondition, new byte[0]), is(false));

    // Same as the above test, but with the same fulfillment twice.
    final ThresholdSha256Fulfillment fulfillmentWithTwoDuplicateFulfillment
        = ThresholdSha256Fulfillment.from(
        Lists.newArrayList(),
        Lists.newArrayList(subfulfillment1, subfulfillment1)
    );
    assertThat(fulfillmentWithTwoDuplicateFulfillment.verify(oneOfTwoCondition, new byte[0]),
        is(false));
  }

  @Test
  public void testTwoOfTwoThreshold() {
    // Construct a Two-of-Two Threshold Condition using both of the above sub-conditions. This means
    // that in order to fulfill this condition, a ThresholdFulfillment containing one or both of the
    // above fulfillments must be published.
    final ThresholdSha256Condition twoOfTwoCondition = ThresholdSha256Condition.from(
        2, Lists.newArrayList(subcondition1, subcondition2)
    );

    // In order to fulfill a threshold condition, the count of the sub-fulfillments MUST be equal to
    // the threshold. Thus, construct a Fulfillment with only subfulfillment2, and expect it to  verify
    // properly against oneOfTwoCondition.
    final ThresholdSha256Fulfillment fulfillmentWithFulfillment1 = ThresholdSha256Fulfillment.from(
        Lists.newArrayList(subcondition2),
        Lists.newArrayList(subfulfillment2)
    );
    assertThat(fulfillmentWithFulfillment1.verify(twoOfTwoCondition, new byte[0]), is(false));

    // In order to fulfill a threshold condition, the count of the sub-fulfillments MUST be equal to
    // the threshold. Thus, construct a Fulfillment with only fulfillment2, and expect it to  verify
    // properly against oneOfTwoCondition.
    final ThresholdSha256Fulfillment fulfillmentWithFulfillment2 = ThresholdSha256Fulfillment.from(
        Lists.newArrayList(subcondition1),
        Lists.newArrayList(subfulfillment2)
    );
    assertThat(fulfillmentWithFulfillment2.verify(twoOfTwoCondition, new byte[0]), is(false));

    // Construct a Fulfillment with both fulfillments, and expect it to verify oneOfTwoCondition properly.
    final ThresholdSha256Fulfillment fulfillmentWithBoth = ThresholdSha256Fulfillment.from(
        Lists.newArrayList(),
        Lists.newArrayList(subfulfillment1, subfulfillment2)
    );
    assertThat(fulfillmentWithBoth.verify(twoOfTwoCondition, new byte[0]), is(true));

    // Construct a Fulfillment with both fulfillments, and expect it to verify oneOfTwoCondition properly.
    final ThresholdSha256Fulfillment fulfillmentWithBothReversed = ThresholdSha256Fulfillment.from(
        Lists.newArrayList(),
        Lists.newArrayList(subfulfillment2, subfulfillment1)
    );
    assertThat(fulfillmentWithBothReversed.verify(twoOfTwoCondition, new byte[0]), is(true));
  }

  /**
   * Tests weighting where at least 2 fulfillments are required, but 1 party has two votes. This is
   * comparable to an escrow transaction, where each party to the escrow has only a single vote, and
   * both votes are required to fulfill a transaction, but the escrow provider can supply two votes
   * to force a decision.
   */
  @Test
  public void testDuplicateConditionsAndFulfillments() {
    final ThresholdSha256Condition condition = ThresholdSha256Condition.from(
        2, Lists
            .newArrayList(subcondition1, subcondition1, subcondition2, subcondition3)
    );

    // Escrow Only (insufficient)
    ThresholdSha256Fulfillment fulfillment = ThresholdSha256Fulfillment.from(
        Lists.newArrayList(subcondition1, subcondition2, subcondition3),
        Lists.newArrayList(subfulfillment1)
    );
    assertThat(fulfillment.verify(condition, new byte[0]), is(false));

    // Escrow Only (sufficient)
    fulfillment = ThresholdSha256Fulfillment.from(
        Lists.newArrayList(subcondition2, subcondition3),
        Lists.newArrayList(subfulfillment1, subfulfillment1)
    );
    assertThat(fulfillment.verify(condition, new byte[0]), is(true));

    // Escrow+Party2
    fulfillment = ThresholdSha256Fulfillment.from(
        Lists.newArrayList(subcondition1, subcondition3),
        Lists.newArrayList(subfulfillment1, subfulfillment2)
    );
    assertThat(fulfillment.verify(condition, new byte[0]), is(true));

    // Escrow+Party3
    fulfillment = ThresholdSha256Fulfillment.from(
        Lists.newArrayList(subcondition1, subcondition2),
        Lists.newArrayList(subfulfillment1, subfulfillment3)
    );
    assertThat(fulfillment.verify(condition, new byte[0]), is(true));

    // Party2+Party3
    fulfillment = ThresholdSha256Fulfillment.from(
        Lists.newArrayList(subcondition1, subcondition1),
        Lists.newArrayList(subfulfillment2, subfulfillment3)
    );
    assertThat(fulfillment.verify(condition, new byte[0]), is(true));

    // Party2+Party3 (w\o escrow condition)
    fulfillment = ThresholdSha256Fulfillment.from(
        Lists.newArrayList(),
        Lists.newArrayList(subfulfillment2, subfulfillment3)
    );
    assertThat(fulfillment.verify(condition, new byte[0]), is(false));
  }
}
