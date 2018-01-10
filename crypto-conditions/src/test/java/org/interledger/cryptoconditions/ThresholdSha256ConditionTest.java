package org.interledger.cryptoconditions;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.interledger.cryptoconditions.CryptoConditionType.THRESHOLD_SHA256;

import org.interledger.cryptoconditions.der.DerEncodingException;

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

      final PreimageSha256Condition preimageCondition = new PreimageSha256Condition(
          AUTHOR.getBytes());

      final ThresholdSha256Condition thresholdSha256Condition = new ThresholdSha256Condition(
          1, Lists.newArrayList(preimageCondition)
      );

      assertThat(thresholdSha256Condition.getType(), is(THRESHOLD_SHA256));
      assertThat(thresholdSha256Condition.getCost(), CoreMatchers.is(1033L));
      assertThat(CryptoConditionUri.toUri(thresholdSha256Condition).toString(), is(
          "ni:///sha-256;W-kFFQRd_dtz60dK3Jq0wr-DEDWHLFh8D1TQHCTi75I?cost=1033&"
              + "fpt=threshold-sha-256&subtypes=preimage-sha-256"));

      assertThat(BaseEncoding.base64().encode(thresholdSha256Condition.getFingerprint()),
          is("W+kFFQRd/dtz60dK3Jq0wr+DEDWHLFh8D1TQHCTi75I="));
      assertThat(BaseEncoding.base64().encode(thresholdSha256Condition
              .constructFingerprintContents(1, Lists.newArrayList(preimageCondition))),
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
   * Tests the cost of a {@link ThresholdSha256Condition}. This test
   * validates the enhancement for Github issue #10 where algorithm is
   * reminded.
   *
   * <p>For example, if a threshold crypto-condition contains 5
   * sub-conditions with costs of 64, 84, 82, 64 and 84, and has a
   * threshold of 3, the cost is equal to the sum of the largest three
   * sub-condition costs (82 + 84 + 84 = 250) plus 1024 times the
   * number of sub-conditions (1024 * 5 = 5120): 5370.</p>
   *
   * @see "https://github.com/hyperledger/quilt/issues/10"
   * @see "https://github.com/interledger/java-crypto-conditions/issues/78"
   */
  @Test
  public void testCost() {
    final ThresholdSha256Condition thresholdSha256Condition = new ThresholdSha256Condition(
        3, Lists.newArrayList(
            new PreimageSha256Condition(new byte[64]),
            new PreimageSha256Condition(new byte[84]),
            new PreimageSha256Condition(new byte[82]),
            new PreimageSha256Condition(new byte[64]),
            new PreimageSha256Condition(new byte[84])
        )
    );

    assertThat(thresholdSha256Condition.getCost(), is(5370l));
  }

  /**
   * Tests failure in the cost computation of a {@link ThresholdSha256Condition}.
   * This validates failure upon sub-conditions underflow (i.e. the number of
   * sub-conditions is lower than the condition threshold).
   */
  @Test(expected = IllegalArgumentException.class)
  public void testCost_subconditionsUnderflow() {
    new ThresholdSha256Condition(
        3, Lists.newArrayList(
            new PreimageSha256Condition(new byte[64]),
            new PreimageSha256Condition(new byte[84])
        )
    );
  }

}
