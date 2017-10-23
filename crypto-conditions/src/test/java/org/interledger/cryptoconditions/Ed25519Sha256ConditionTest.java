package org.interledger.cryptoconditions;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import com.google.common.io.BaseEncoding;

import net.i2p.crypto.eddsa.EdDSAPrivateKey;
import net.i2p.crypto.eddsa.EdDSAPublicKey;
import net.i2p.crypto.eddsa.spec.EdDSANamedCurveSpec;
import net.i2p.crypto.eddsa.spec.EdDSANamedCurveTable;
import net.i2p.crypto.eddsa.spec.EdDSAPrivateKeySpec;
import net.i2p.crypto.eddsa.spec.EdDSAPublicKeySpec;

import org.junit.Test;

import java.net.URI;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;

/**
 * Unit tests for {@link Ed25519Sha256Condition}.
 */
public class Ed25519Sha256ConditionTest extends AbstractCryptoConditionTest {

  private static final byte[] TEST_PUBKEY = BaseEncoding.base64()
      .decode("Nq4bl8V3rmr7ApTpGDn6ex+TMnkbnyxdWGgZAl9KLx0=");

  private static final byte[] TEST_PRIVKEY = BaseEncoding.base64()
      .decode("A59Fwv0b/smwKKpDy66asxKeFME63RYiK0Rj6Aaf3To=");

  /**
   * Tests concurrently creating an instance of {@link Ed25519Sha256Condition}. This test validates
   * the fix for Github issue #40 where construction of this class was not thread-safe.
   *
   * @see "https://github.com/interledger/java-crypto-conditions/issues/40"
   * @see "https://github.com/junit-team/junit4/wiki/multithreaded-code-and-concurrency"
   */
  @Test
  public void testConstructionUsingMultipleThreads() throws Exception {
    final KeyPair keypair = this.constructEd25519KeyPair();

    final Runnable runnableTest = () -> {
      final Ed25519Sha256Condition ed25519Sha256Condition = new Ed25519Sha256Condition(
          (EdDSAPublicKey) keypair.getPublic());

      assertThat(ed25519Sha256Condition.getType(), is(CryptoConditionType.ED25519_SHA256));
      assertThat(ed25519Sha256Condition.getCost(), is(131072L));
      assertThat(CryptoConditionUri.toUri(ed25519Sha256Condition), is(URI.create(
          "ni:///sha-256;aJ5kk1zn2qrQQO5QhYZXoGigv0Y5rSafiV3BUM1F9hM?cost=131072&"
              + "fpt=ed25519-sha-256")));
      assertThat(BaseEncoding.base64().encode(ed25519Sha256Condition.getFingerprint()),
          is("aJ5kk1zn2qrQQO5QhYZXoGigv0Y5rSafiV3BUM1F9hM="));
      assertThat(BaseEncoding.base64().encode(ed25519Sha256Condition
              .constructFingerprintContents((EdDSAPublicKey) keypair.getPublic())),
          is("MCKAIDauG5fFd65q+wKU6Rg5+nsfkzJ5G58sXVhoGQJfSi8d"));
    };

    this.runConcurrent(1, runnableTest);
    this.runConcurrent(runnableTest);
  }

  /**
   * Helper method to construct an instance of {@link KeyPair} containing keys for testing purposes.
   *
   * @return An instance of {@link KeyPair}.
   */
  protected KeyPair constructEd25519KeyPair() throws InvalidKeySpecException {
    final EdDSANamedCurveSpec edParams = EdDSANamedCurveTable
        .getByName(CryptoConditionReader.ED_25519);
    assert (edParams != null);

    final EdDSAPublicKeySpec pubKeySpec = new EdDSAPublicKeySpec(TEST_PUBKEY, edParams);
    final PublicKey pubKey = new EdDSAPublicKey(pubKeySpec);

    final EdDSAPrivateKeySpec privateKeySpec = new EdDSAPrivateKeySpec(TEST_PRIVKEY, edParams);
    final PrivateKey privKey = new EdDSAPrivateKey(privateKeySpec);

    return new KeyPair(pubKey, privKey);
  }
}
