package org.interledger.cryptoconditions;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import org.interledger.cryptoconditions.helpers.TestKeyFactory;

import com.google.common.io.BaseEncoding;
import org.junit.Test;

import java.net.URI;
import java.security.interfaces.RSAPublicKey;

/**
 * Unit tests for {@link Ed25519Sha256Condition}.
 */
public class RsaSha256ConditionTest extends AbstractCryptoConditionTest {

  private static final String MODULUS = "4e-LJNb3awnIHtd1KqJi8ETwSodNQ4CdMc6mEvmbDJeotDdBU-Pu89ZmFo"
      + "Q-DkHCkyZLcbYXPbHPDWzVWMWGV3Bvzwl_cExIPlnL_f1bPue8gNdAxeDwR_PoX8DXWBV3am8_I8XcXnlxOaaILjgz"
      + "akpfs2E3Yg_zZj264yhHKAGGL3Ly-HsgK5yJrdfNWwoHb3xT41A59n7RfsgV5bQwXMYxlwaNXm5Xm6beX04-V99eTg"
      + "cv8s5MZutFIzlzh1J1ljnwJXv1fb1cRD-1FYzOCj02rce6AfM6C7bbsr-YnWBxEvI0TZk-d-VjwdNh3t9X2pbvLPxo"
      + "XwArY4JGpbMJuQ";

  /**
   * Tests concurrently creating an instance of {@link Ed25519Sha256Condition}. This test validates
   * the fix for Github issue #40 where construction of this class was not thread-safe.
   *
   * @see "https://github.com/interledger/java-crypto-conditions/issues/40"
   * @see "https://github.com/junit-team/junit4/wiki/multithreaded-code-and-concurrency"
   */
  @Test
  public void testConstructionUsingMultipleThreads() throws Exception {
    final RSAPublicKey rsaPublicKey = TestKeyFactory.constructRsaPublicKey(MODULUS);
    final Runnable runnableTest = () -> {
      final RsaSha256Condition rsaSha256Condition = new RsaSha256Condition(rsaPublicKey);

      assertThat(rsaSha256Condition.getType(), is(CryptoConditionType.RSA_SHA256));
      assertThat(rsaSha256Condition.getCost(), is(65536L));
      assertThat(CryptoConditionUri.toUri(rsaSha256Condition), is(URI.create(
          "ni:///sha-256;sx-oIG5Op-UVM3s7Mwgrh3ZRgBCF7YT7Ta6yR79pjX8?cost=65536&fpt=rsa-sha-256")));

      assertThat(BaseEncoding.base64().encode(rsaSha256Condition.getFingerprint()),
          is("sx+oIG5Op+UVM3s7Mwgrh3ZRgBCF7YT7Ta6yR79pjX8="));
      assertThat(BaseEncoding.base64()
              .encode(rsaSha256Condition.constructFingerprintContents(rsaPublicKey)),
          is("MIIBBICCAQDh74sk1vdrCcge13UqomLwRPBKh01DgJ0xzqYS+ZsMl6i0N0FT4+7z1mYWhD4OQcKTJkt"
              + "xthc9sc8NbNVYxYZXcG/PCX9wTEg+Wcv9/Vs+57yA10DF4PBH8+hfwNdYFXdqbz8jxdxeeXE5poguODNqS"
              + "l+zYTdiD/NmPbrjKEcoAYYvcvL4eyArnImt181bCgdvfFPjUDn2ftF+yBXltDBcxjGXBo1eblebpt5fTj5"
              + "X315OBy/yzkxm60UjOXOHUnWWOfAle/V9vVxEP7UVjM4KPTatx7oB8zoLttuyv5idYHES8jRNmT535WPB0"
              + "2He31falu8s/GhfACtjgkalswm5"));
    };

    this.runConcurrent(1, runnableTest);
    this.runConcurrent(runnableTest);
  }

}
