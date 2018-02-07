package org.interledger.cryptoconditions;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import org.interledger.cryptoconditions.PreimageSha256Fulfillment.AbstractPreimageSha256Fulfillment;

import com.google.common.io.BaseEncoding;
import org.hamcrest.CoreMatchers;
import org.junit.Test;

import java.net.URI;

/**
 * Unit tests for {@link PreimageSha256Condition}.
 */
public class PreimageSha256ConditionTest extends AbstractCryptoConditionTest {

  private static final String PREIMAGE = "when this baby hits 88 miles per hour";
  private static final String PREIMAGE2 = "Nobody calls me chicken!";
  private static final String ENCODED_PREIMAGE
      = "d2hlbiB0aGlzIGJhYnkgaGl0cyA4OCBtaWxlcyBwZXIgaG91cg==";
  private static final String ENCODED_FINGERPRINT = "iL1xV1F0IvtokoaU1n2eVOvcwhy4me4vroUKg8vFnOE=";
  private static final URI CONDITION_URI = URI.create("ni:///sha-256;"
      + "iL1xV1F0IvtokoaU1n2eVOvcwhy4me4vroUKg8vFnOE?cost=37&fpt=preimage-sha-256");

  /**
   * Tests concurrently creating an instance of {@link PreimageSha256Condition}. This test
   * validates the fix for Github issue #40 where construction of this class was not thread-safe.
   *
   * @see "https://github.com/interledger/java-crypto-conditions/issues/40"
   * @see "https://github.com/junit-team/junit4/wiki/multithreaded-code-and-concurrency"
   */
  @Test
  public void testConstructionUsingMultipleThreads() throws Exception {
    final Runnable runnableTest = () -> {
      final PreimageSha256Condition condition
          = PreimageSha256Fulfillment.from(PREIMAGE.getBytes()).getCondition();

      assertThat(condition.getType(), is(CryptoConditionType.PREIMAGE_SHA256));
      assertThat(condition.getCost(), is(37L));
      assertThat(CryptoConditionUri.toUri(condition), is(CONDITION_URI));

      assertThat(BaseEncoding.base64().encode(condition.getFingerprint()), is(ENCODED_FINGERPRINT));
      assertThat(
          BaseEncoding.base64().encode(
              AbstractPreimageSha256Fulfillment.constructFingerprint(
                  PREIMAGE.getBytes()
              )
          ),
          is(ENCODED_PREIMAGE));
    };

    // Run single-threaded...
    this.runConcurrent(1, runnableTest);
    // Run multi-threaded...
    this.runConcurrent(runnableTest);
  }

  @Test
  public void equalsHashcode() {
    final PreimageSha256Condition condition1
        = PreimageSha256Fulfillment.from(PREIMAGE.getBytes()).getCondition();
    final PreimageSha256Condition condition2
        = PreimageSha256Fulfillment.from(PREIMAGE.getBytes()).getCondition();
    final PreimageSha256Condition condition3
        = PreimageSha256Fulfillment.from(PREIMAGE2.getBytes()).getCondition();

    assertThat(condition1.equals(condition1), CoreMatchers.is(true));
    assertThat(condition2.equals(condition2), CoreMatchers.is(true));
    assertThat(condition3.equals(condition3), CoreMatchers.is(true));

    assertThat(condition1.equals(condition2), CoreMatchers.is(true));
    assertThat(condition1.equals(condition3), CoreMatchers.is(false));

    assertThat(condition2.equals(condition1), CoreMatchers.is(true));
    assertThat(condition2.equals(condition3), CoreMatchers.is(false));

    assertThat(condition3.equals(condition1), CoreMatchers.is(false));
    assertThat(condition3.equals(condition2), CoreMatchers.is(false));

    assertThat(condition1.hashCode(), CoreMatchers.is(condition2.hashCode()));
    assertThat(condition1.hashCode() == condition3.hashCode(), CoreMatchers.is(false));
  }

  @Test
  public void testToString() {
    final PreimageSha256Condition condition
        = PreimageSha256Fulfillment.from(PREIMAGE.getBytes()).getCondition();

    assertThat(condition.toString(),
        CoreMatchers
            .is("PreimageSha256Condition{type=PREIMAGE-SHA-256, "
                + "fingerprint=iL1xV1F0IvtokoaU1n2eVOvcwhy4me4vroUKg8vFnOE, cost=37}"));
  }
}
