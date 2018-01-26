package org.interledger.cryptoconditions;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import org.interledger.cryptoconditions.Ed25519Sha256Condition.AbstractEd25519Sha256Condition;
import org.interledger.cryptoconditions.helpers.TestConditionFactory;
import org.interledger.cryptoconditions.helpers.TestKeyFactory;

import com.google.common.io.BaseEncoding;
import net.i2p.crypto.eddsa.EdDSAPublicKey;
import org.junit.Test;

import java.net.URI;
import java.security.KeyPair;

/**
 * Unit tests for {@link Ed25519Sha256Condition}.
 */
public class Ed25519Sha256ConditionTest extends AbstractCryptoConditionTest {

  /**
   * Tests concurrently creating an instance of {@link Ed25519Sha256Condition}. This test
   * validates the fix for Github issue #40 where construction of this class was not thread-safe.
   *
   * @see "https://github.com/interledger/java-crypto-conditions/issues/40"
   * @see "https://github.com/junit-team/junit4/wiki/multithreaded-code-and-concurrency"
   */
  @Test
  public void testConstructionUsingMultipleThreads() throws Exception {
    final KeyPair keypair = TestKeyFactory.constructEd25519KeyPair();

    final Runnable runnableTest = () -> {
      final Ed25519Sha256Condition ed25519Sha256Condition = Ed25519Sha256Condition.from(
          (EdDSAPublicKey) keypair.getPublic()
      );

      assertThat(ed25519Sha256Condition.getType(), is(CryptoConditionType.ED25519_SHA256));
      assertThat(ed25519Sha256Condition.getCost(), is(131072L));
      assertThat(CryptoConditionUri.toUri(ed25519Sha256Condition), is(URI.create(
          "ni:///sha-256;aJ5kk1zn2qrQQO5QhYZXoGigv0Y5rSafiV3BUM1F9hM?cost=131072&"
              + "fpt=ed25519-sha-256")));
      assertThat(BaseEncoding.base64().encode(ed25519Sha256Condition.getFingerprint()),
          is("aJ5kk1zn2qrQQO5QhYZXoGigv0Y5rSafiV3BUM1F9hM="));
      assertThat(BaseEncoding.base64().encode(AbstractEd25519Sha256Condition
              .constructFingerprintContents((EdDSAPublicKey) keypair.getPublic())),
          is("MCKAIDauG5fFd65q+wKU6Rg5+nsfkzJ5G58sXVhoGQJfSi8d"));
    };

    // Run single-threaded...
    this.runConcurrent(1, runnableTest);

    // Run multi-threaded...
    this.runConcurrent(runnableTest);
  }

  @Test
  public void equalsHashcodeTest() {
    final Ed25519Sha256Condition ed25519Sha256Condition1 = TestConditionFactory
        .constructEd25519Sha256Condition(
            (EdDSAPublicKey) TestKeyFactory.generateRandomEd25519KeyPair().getPublic()
        );
    final Ed25519Sha256Condition ed25519Sha256Condition2 = TestConditionFactory
        .constructEd25519Sha256Condition(
            (EdDSAPublicKey) TestKeyFactory.generateRandomEd25519KeyPair().getPublic()
        );

    assertThat(ed25519Sha256Condition1.equals(ed25519Sha256Condition1), is(true));
    assertThat(ed25519Sha256Condition2.equals(ed25519Sha256Condition2), is(true));
    assertThat(ed25519Sha256Condition1.equals(ed25519Sha256Condition2), is(false));
    assertThat(ed25519Sha256Condition2.equals(ed25519Sha256Condition1), is(false));
  }

  @Test
  public void toStringTest() {
    final Ed25519Sha256Condition ed25519Sha256Condition = TestConditionFactory
        .constructEd25519Sha256Condition(
            (EdDSAPublicKey) TestKeyFactory.constructEd25519KeyPair().getPublic()
        );

    assertThat(ed25519Sha256Condition.toString(), is(
        "Ed25519Sha256Condition{type=ED25519-SHA-256, "
            + "fingerprint=aJ5kk1zn2qrQQO5QhYZXoGigv0Y5rSafiV3BUM1F9hM, cost=131072}"
    ));
  }
}
