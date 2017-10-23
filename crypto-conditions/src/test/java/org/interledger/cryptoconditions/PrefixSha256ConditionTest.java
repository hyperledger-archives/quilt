package org.interledger.cryptoconditions;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

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

      final PreimageSha256Condition preimageSha256Condition = new PreimageSha256Condition(
          MESSAGE_PREIMAGE.getBytes());
      final PrefixSha256Condition prefixSha256Condition = new PrefixSha256Condition(
          AUTHOR.getBytes(), 16384,
          preimageSha256Condition);

      assertThat(prefixSha256Condition.getType(), is(CryptoConditionType.PREFIX_SHA256));
      assertThat(prefixSha256Condition.getCost(), is(17463L));
      assertThat(CryptoConditionUri.toUri(prefixSha256Condition), is(URI.create(
          "ni:///sha-256;hfHFYbd93301v75b1967nmZ4uLx8Pf_UYqCYYTfT_MI?cost=17463&fpt=prefix-sha-"
              + "256&subtypes=preimage-sha-256")));

      assertThat(BaseEncoding.base64().encode(prefixSha256Condition.getFingerprint()),
          is("hfHFYbd93301v75b1967nmZ4uLx8Pf/UYqCYYTfT/MI="));
      assertThat(BaseEncoding.base64().encode(prefixSha256Condition
              .constructFingerprintContents(AUTHOR.getBytes(), 16384, preimageSha256Condition)),
          is("MDiACURvYyBCcm93boECQACiJ6AlgCD7bwRU2vus7BD9ey+slXEu+MFjfxk1mUdo8dimwhuYfoEBLg=="));
    };

    this.runConcurrent(1, runnableTest);
    this.runConcurrent(runnableTest);
  }
}
