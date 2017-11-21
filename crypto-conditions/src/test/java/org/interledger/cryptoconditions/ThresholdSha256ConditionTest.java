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
}
