package org.interledger.cryptoconditions;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.interledger.cryptoconditions.helpers.TestFulfillmentFactory.PREFIX1;
import static org.interledger.cryptoconditions.helpers.TestFulfillmentFactory.PREFIX2;
import static org.interledger.cryptoconditions.helpers.TestFulfillmentFactory.PREIMAGE1;

import org.interledger.cryptoconditions.PrefixSha256Condition.AbstractPrefixSha256Condition;
import org.interledger.cryptoconditions.helpers.TestConditionFactory;

import com.google.common.io.BaseEncoding;
import org.junit.Test;

import java.net.URI;

/**
 * Unit tests for {@link PrefixSha256Condition}.
 */
public class PrefixSha256ConditionTest extends AbstractCryptoConditionTest {

  /**
   * Tests concurrently creating an instance of {@link PrefixSha256Condition}. This test validates
   * the fix for Github issue #40 where construction of this class was not thread-safe.
   *
   * @see "https://github.com/interledger/java-crypto-conditions/issues/40"
   * @see "https://github.com/junit-team/junit4/wiki/multithreaded-code-and-concurrency"
   */
  @Test
  public void testConstructionUsingMultipleThreads() throws Exception {
    final Runnable runnableTest = () -> {

      final PrefixSha256Condition prefixSha256Condition = TestConditionFactory
          .constructPrefixSha256Condition(PREFIX1);

      assertThat(prefixSha256Condition.getType(), is(CryptoConditionType.PREFIX_SHA256));
      assertThat(prefixSha256Condition.getCost(), is(2081L));
      assertThat(CryptoConditionUri.toUri(prefixSha256Condition), is(URI.create(
          "ni:///sha-256;KrV_YYvQMc_mAKpg73Kngfld3lFoZdUQ8FEtQf4m13g?cost=2081&fpt=prefix-sha-256"
              + "&subtypes=preimage-sha-256")));

      assertThat(BaseEncoding.base64().encode(prefixSha256Condition.getFingerprint()),
          is("KrV/YYvQMc/mAKpg73Kngfld3lFoZdUQ8FEtQf4m13g="));
      assertThat(BaseEncoding.base64().encode(AbstractPrefixSha256Condition
              .constructFingerprintContents(PREFIX1.getBytes(), 16384,
                  TestConditionFactory.constructPreimageCondition(PREIMAGE1))),
          is("MDqAC09yZGVyLTEyMzQ1gQJAAKInoCWAIPtvBFTa+6zsEP17L6yVcS74wWN/GTWZR2jx2KbCG5h+gQEu"));
    };

    // Run single-threaded...
    this.runConcurrent(1, runnableTest);
    // Run multi-threaded...
    this.runConcurrent(runnableTest);
  }

  @Test
  public void equalsHashcodeTest() {
    final PrefixSha256Condition prefixSha256Condition1 = TestConditionFactory
        .constructPrefixSha256Condition(PREFIX1);
    final PrefixSha256Condition prefixSha256Condition2 = TestConditionFactory
        .constructPrefixSha256Condition(PREFIX2);

    assertThat(prefixSha256Condition1.equals(prefixSha256Condition1), is(true));
    assertThat(prefixSha256Condition2.equals(prefixSha256Condition2), is(true));
    assertThat(prefixSha256Condition1.equals(prefixSha256Condition2), is(false));
    assertThat(prefixSha256Condition2.equals(prefixSha256Condition1), is(false));
  }

  @Test
  public void toStringTest() {
    final PrefixSha256Condition prefixSha256Condition1 = TestConditionFactory
        .constructPrefixSha256Condition(PREFIX1);
    assertThat(prefixSha256Condition1.toString(), is(
        "PrefixSha256Condition{subtypes=[PREIMAGE-SHA-256], type=PREFIX-SHA-256, "
            + "fingerprint=KrV_YYvQMc_mAKpg73Kngfld3lFoZdUQ8FEtQf4m13g, cost=2081}"
    ));
  }
}
