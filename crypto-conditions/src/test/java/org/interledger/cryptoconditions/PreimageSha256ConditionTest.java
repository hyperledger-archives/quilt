package org.interledger.cryptoconditions;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import com.google.common.io.BaseEncoding;

import org.junit.Test;

import java.net.URI;

/**
 * Unit tests for {@link PreimageSha256Condition}.
 */
public class PreimageSha256ConditionTest extends AbstractCryptoConditionTest {

  private static final String PREIMAGE = "aaa";

  private static final URI CONDITION_URI = URI.create("ni:///sha-256;mDSHbc-wXLFnpcJJU-uljErImxrfV_"
      + "KPL50JrxB-6PA?cost=3&fpt=preimage-sha-256");

  /**
   * Tests concurrently creating an instance of {@link PreimageSha256Condition}. This test validates
   * the fix for Github issue #40 where construction of this class was not thread-safe.
   *
   * @see "https://github.com/interledger/java-crypto-conditions/issues/40"
   * @see "https://github.com/junit-team/junit4/wiki/multithreaded-code-and-concurrency"
   */
  @Test
  public void testConstructionUsingMultipleThreads() throws Exception {
    final Runnable runnableTest = () -> {
      final PreimageSha256Condition condition = new PreimageSha256Condition(PREIMAGE.getBytes());

      assertThat(condition.getType(), is(CryptoConditionType.PREIMAGE_SHA256));
      assertThat(condition.getCost(), is(3L));
      assertThat(CryptoConditionUri.toUri(condition), is(CONDITION_URI));

      assertThat(BaseEncoding.base64().encode(condition.getFingerprint()),
          is("mDSHbc+wXLFnpcJJU+uljErImxrfV/KPL50JrxB+6PA="));
      assertThat(
          BaseEncoding.base64().encode(condition.constructFingerprintContents(PREIMAGE.getBytes())),
          is("YWFh"));
    };

    this.runConcurrent(1, runnableTest);
    this.runConcurrent(runnableTest);
  }
}
