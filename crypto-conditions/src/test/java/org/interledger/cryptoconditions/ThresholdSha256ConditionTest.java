package org.interledger.cryptoconditions;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.interledger.cryptoconditions.CryptoConditionType.THRESHOLD_SHA256;
import static org.interledger.cryptoconditions.helpers.TestFulfillmentFactory.MESSAGE;
import static org.interledger.cryptoconditions.helpers.TestFulfillmentFactory.MESSAGE2;
import static org.interledger.cryptoconditions.helpers.TestFulfillmentFactory.PREIMAGE1;
import static org.interledger.cryptoconditions.helpers.TestFulfillmentFactory.PREIMAGE2;

import com.google.common.collect.ImmutableList;
import org.interledger.cryptoconditions.ThresholdSha256Condition.AbstractThresholdSha256Condition;
import org.interledger.cryptoconditions.der.DerEncodingException;
import org.interledger.cryptoconditions.helpers.TestConditionFactory;

import com.google.common.collect.Lists;
import com.google.common.io.BaseEncoding;
import org.hamcrest.CoreMatchers;
import org.junit.Test;

/**
 * Unit tests for {@link ThresholdSha256Condition}.
 */
public class ThresholdSha256ConditionTest extends AbstractCryptoConditionTest {

  /**
   * Tests concurrently creating an instance of {@link ThresholdSha256Condition}. This test
   * validates the fix for Github issue #40 where construction of this class was not thread-safe.
   *
   * @see "https://github.com/interledger/java-crypto-conditions/issues/40"
   * @see "https://github.com/junit-team/junit4/wiki/multithreaded-code-and-concurrency"
   */
  @Test
  public void testConstructionUsingMultipleThreads() throws Exception {
    final Runnable runnableTest = () -> {

      final ThresholdSha256Condition thresholdSha256Condition = TestConditionFactory
          .constructThresholdCondition(MESSAGE);

      assertThat(thresholdSha256Condition.getType(), is(THRESHOLD_SHA256));
      assertThat(thresholdSha256Condition.getCost(), CoreMatchers.is(1033L));
      assertThat(CryptoConditionUri.toUri(thresholdSha256Condition).toString(), is(
          "ni:///sha-256;W-kFFQRd_dtz60dK3Jq0wr-DEDWHLFh8D1TQHCTi75I?cost=1033&"
              + "fpt=threshold-sha-256&subtypes=preimage-sha-256"));

      assertThat(BaseEncoding.base64().encode(thresholdSha256Condition.getFingerprint()),
          is("W+kFFQRd/dtz60dK3Jq0wr+DEDWHLFh8D1TQHCTi75I="));
      assertThat(BaseEncoding.base64().encode(AbstractThresholdSha256Condition
              .constructFingerprintContents(1,
                  Lists.newArrayList(TestConditionFactory.constructPreimageCondition(MESSAGE)))),
          is("MCyAAQGhJ6AlgCBjEgvXn574GWJrrBNHDMuo/bgLkhTNwoj1GUDp77vDnYEBCQ=="));

      try {
        assertThat(BaseEncoding.base64()
                .encode(CryptoConditionWriter.writeCondition(thresholdSha256Condition)),
            is("oiqAIFvpBRUEXf3bc+tHStyatMK/gxA1hyxYfA9U0Bwk4u+SgQIECYICB4A="));
      } catch (DerEncodingException e) {
        throw new RuntimeException(e);
      }
    };

    this.runConcurrent(1, runnableTest);
    this.runConcurrent(runnableTest);
  }

  /**
   * Constructs a Threshold Condition using two duplicate conditions, and asserts that only one
   * is used in total.
   */
  @Test
  public void testDuplicateSubConditions() {
    final PreimageSha256Condition subcondition1 = TestConditionFactory
        .constructPreimageCondition(PREIMAGE1);

    // Adding two of the same condition is allowed...
    final ThresholdSha256Condition condition = ThresholdSha256Condition.from(
        1, Lists.newArrayList(subcondition1, subcondition1)
    );

    assertThat(condition.getSubtypes().contains(CryptoConditionType.PREIMAGE_SHA256), is(true));
    assertThat(condition.getSubtypes().size(), is(1));
    assertThat(condition.getFingerprintBase64Url(),
        is("iLaI2QGeH1orfViO1kQAguKb2uFioqG3rxI2ERa7eJE"));
    assertThat(condition.getCost(), is(2094L));
    assertThat(condition.getType(), is(CryptoConditionType.THRESHOLD_SHA256));
  }

  /**
   * Constructs a Threshold Condition using two conditions, and asserts that the
   * resulting conditions is valid.
   */
  @Test
  public void testOneOfTwoThresholdCondition() {
    final PreimageSha256Condition subcondition1 = TestConditionFactory
        .constructPreimageCondition(PREIMAGE1);
    final PreimageSha256Condition subcondition2 = TestConditionFactory
        .constructPreimageCondition(PREIMAGE2);

    // Adding two of the same condition is allowed...
    final ThresholdSha256Condition condition = ThresholdSha256Condition.from(
        1, Lists.newArrayList(subcondition1, subcondition2)
    );

    assertThat(condition.getSubtypes().contains(CryptoConditionType.PREIMAGE_SHA256), is(true));
    assertThat(condition.getSubtypes().size(), is(1));
    assertThat(condition.getFingerprintBase64Url(),
        is("cFYYmVSDhC_rSX6DPWQUTwG7iuWpQODWJXffjL8ROXM"));
    assertThat(condition.getCost(), is(2094L));
    assertThat(condition.getType(), is(CryptoConditionType.THRESHOLD_SHA256));
  }

  /**
   * Constructs a Threshold Condition using an immutable list of two conditions, and asserts that
   * the resulting condition is valid.
   */
  @Test
  public void testImmutableListOfSubconditions() {
      final PreimageSha256Condition subcondition1 = TestConditionFactory
          .constructPreimageCondition(PREIMAGE1);
      final PreimageSha256Condition subcondition2 = TestConditionFactory
          .constructPreimageCondition(PREIMAGE2);


      final ThresholdSha256Condition condition = ThresholdSha256Condition.from(
              1, ImmutableList.of(subcondition1, subcondition2)
      );

      assertThat(condition.getSubtypes().contains(CryptoConditionType.PREIMAGE_SHA256), is(true));
      assertThat(condition.getFingerprintBase64Url(),
              is("cFYYmVSDhC_rSX6DPWQUTwG7iuWpQODWJXffjL8ROXM"));
      assertThat(condition.getCost(), is(2094L));
      assertThat(condition.getType(), is(CryptoConditionType.THRESHOLD_SHA256));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testIncorrectThreshold() {
    final PreimageSha256Condition subcondition = TestConditionFactory
        .constructPreimageCondition(PREIMAGE1);

    try {
      ThresholdSha256Condition.from(10, Lists.newArrayList(subcondition));
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage(),
          is("Threshold must be less than or equal to the number of subconditions!"));
      throw e;
    }
  }

  @Test
  public void equalsHashcodeTest() {
    final ThresholdSha256Condition thresholdSha256Condition1 = TestConditionFactory
        .constructThresholdCondition(MESSAGE);
    final ThresholdSha256Condition thresholdSha256Condition2 = TestConditionFactory
        .constructThresholdCondition(MESSAGE2);

    assertThat(thresholdSha256Condition1.equals(thresholdSha256Condition1), is(true));
    assertThat(thresholdSha256Condition2.equals(thresholdSha256Condition2), is(true));
    assertThat(thresholdSha256Condition1.equals(thresholdSha256Condition2), is(false));
    assertThat(thresholdSha256Condition2.equals(thresholdSha256Condition1), is(false));
  }

  @Test
  public void toStringTest() {
    final ThresholdSha256Condition thresholdSha256Condition = TestConditionFactory
        .constructThresholdCondition(MESSAGE);
    assertThat(thresholdSha256Condition.toString(), is(
        "ThresholdSha256Condition{subtypes=[PREIMAGE-SHA-256], type=THRESHOLD-SHA-256, "
            + "fingerprint=W-kFFQRd_dtz60dK3Jq0wr-DEDWHLFh8D1TQHCTi75I, cost=1033}"
    ));
  }
}
