package org.interledger.cryptoconditions;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.interledger.cryptoconditions.helpers.TestKeyFactory.RSA_MODULUS;

import org.interledger.cryptoconditions.RsaSha256Condition.AbstractRsaSha256Condition;
import org.interledger.cryptoconditions.helpers.TestConditionFactory;
import org.interledger.cryptoconditions.helpers.TestKeyFactory;

import com.google.common.io.BaseEncoding;
import org.junit.Test;

import java.net.URI;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;

/**
 * Unit tests for {@link Ed25519Sha256Condition}.
 */
public class RsaSha256ConditionTest extends AbstractCryptoConditionTest {

  /**
   * Tests concurrently creating an instance of {@link Ed25519Sha256Condition}. This test
   * validates the fix for Github issue #40 where construction of this class was not thread-safe.
   *
   * @see "https://github.com/interledger/java-crypto-conditions/issues/40"
   * @see "https://github.com/junit-team/junit4/wiki/multithreaded-code-and-concurrency"
   */
  @Test
  public void testConstructionUsingMultipleThreads() throws Exception {
    final RSAPublicKey rsaPublicKey = TestKeyFactory.constructRsaPublicKey(RSA_MODULUS);
    final Runnable runnableTest = () -> {
      final RsaSha256Condition rsaSha256Condition = RsaSha256Condition.from(rsaPublicKey);

      assertThat(rsaSha256Condition.getType(), is(CryptoConditionType.RSA_SHA256));
      assertThat(rsaSha256Condition.getCost(), is(65536L));
      assertThat(CryptoConditionUri.toUri(rsaSha256Condition), is(URI.create(
          "ni:///sha-256;sx-oIG5Op-UVM3s7Mwgrh3ZRgBCF7YT7Ta6yR79pjX8?cost=65536&fpt=rsa-sha-256")));

      assertThat(Base64.getUrlEncoder().withoutPadding()
              .encodeToString(rsaSha256Condition.getFingerprint()),
          is("sx-oIG5Op-UVM3s7Mwgrh3ZRgBCF7YT7Ta6yR79pjX8"));
      assertThat(rsaSha256Condition.getFingerprintBase64Url(),
          is("sx-oIG5Op-UVM3s7Mwgrh3ZRgBCF7YT7Ta6yR79pjX8"));
      assertThat(BaseEncoding.base64()
              .encode(AbstractRsaSha256Condition.constructFingerprintContents(rsaPublicKey)),
          is("MIIBBICCAQDh74sk1vdrCcge13UqomLwRPBKh01DgJ0xzqYS+ZsMl6i0N0FT4+7z1mYWhD4OQcKTJkt"
              + "xthc9sc8NbNVYxYZXcG/PCX9wTEg+Wcv9/Vs+57yA10DF4PBH8+hfwNdYFXdqbz8jxdxeeXE5poguODNqS"
              + "l+zYTdiD/NmPbrjKEcoAYYvcvL4eyArnImt181bCgdvfFPjUDn2ftF+yBXltDBcxjGXBo1eblebpt5fTj5"
              + "X315OBy/yzkxm60UjOXOHUnWWOfAle/V9vVxEP7UVjM4KPTatx7oB8zoLttuyv5idYHES8jRNmT535WPB0"
              + "2He31falu8s/GhfACtjgkalswm5"));
    };

    // Run single-threaded...
    this.runConcurrent(1, runnableTest);
    // Run multi-threaded...
    this.runConcurrent(runnableTest);
  }

  @Test
  public void equalsHashcodeTest() {
    final RsaSha256Condition rsaSha256Condition1 = TestConditionFactory
        .constructRsaSha256Condition(
            (RSAPublicKey) TestKeyFactory.generateRandomRsaKeyPair().getPublic()
        );
    final RsaSha256Condition rsaSha256Condition2 = TestConditionFactory
        .constructRsaSha256Condition(
            (RSAPublicKey) TestKeyFactory.generateRandomRsaKeyPair().getPublic()
        );

    assertThat(rsaSha256Condition1.equals(rsaSha256Condition1), is(true));
    assertThat(rsaSha256Condition2.equals(rsaSha256Condition2), is(true));
    assertThat(rsaSha256Condition1.equals(rsaSha256Condition2), is(false));
    assertThat(rsaSha256Condition2.equals(rsaSha256Condition1), is(false));
  }

  @Test
  public void toStringTest() {
    final RsaSha256Condition rsaSha256Condition = TestConditionFactory
        .constructRsaSha256Condition(
            TestKeyFactory.constructRsaPublicKey(RSA_MODULUS)
        );
    assertThat(rsaSha256Condition.toString(), is(
        "RsaSha256Condition{type=RSA-SHA-256, "
            + "fingerprint=sx-oIG5Op-UVM3s7Mwgrh3ZRgBCF7YT7Ta6yR79pjX8, cost=65536}"
    ));
  }

}
