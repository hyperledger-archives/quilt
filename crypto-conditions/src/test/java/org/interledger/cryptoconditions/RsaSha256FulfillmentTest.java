package org.interledger.cryptoconditions;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.interledger.cryptoconditions.helpers.TestFulfillmentFactory.MESSAGE;
import static org.interledger.cryptoconditions.helpers.TestFulfillmentFactory.constructRsaSha256Fulfillment;
import static org.junit.Assert.assertTrue;

import org.interledger.cryptoconditions.helpers.TestKeyFactory;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.hamcrest.CoreMatchers;
import org.junit.Test;

import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.Security;
import java.security.Signature;
import java.security.SignatureException;
import java.security.interfaces.RSAPublicKey;

/**
 * Unit tests for {@link RsaSha256Fulfillment}.
 */
public class RsaSha256FulfillmentTest extends AbstractCryptoConditionTest {

  /**
   * Need to add BouncyCastle so we have a provider that supports SHA256withRSA/PSS signatures
   */
  static {
    Provider bc = new BouncyCastleProvider();
    Security.addProvider(bc);
  }

  /**
   * Tests concurrently creating an instance of {@link RsaSha256Fulfillment}. This test validates
   * the fix for Github issue #40 where construction of this class was not thread-safe.
   *
   * @see "https://github.com/interledger/java-crypto-conditions/issues/40"
   * @see "https://github.com/junit-team/junit4/wiki/multithreaded-code-and-concurrency"
   */
  @Test
  public void testConstructionUsingMultipleThreads() throws Exception {
    final Runnable runnableTest = () -> {
      final RsaSha256Fulfillment rsaSha256Fulfillment =
          constructRsaSha256Fulfillment(
              TestKeyFactory.generateRandomRsaKeyPair()
          );

      assertThat(rsaSha256Fulfillment.getType(), is(CryptoConditionType.RSA_SHA256));
      assertThat(
          rsaSha256Fulfillment.verify(rsaSha256Fulfillment.getCondition(), MESSAGE.getBytes()),
          is(true));
    };

    // Run single-threaded...
    this.runConcurrent(1, runnableTest);
    // Run multi-threaded...
    this.runConcurrent(runnableTest);
  }

  @Test(expected = NullPointerException.class)
  public final void testFromWithNullPublicKey() {
    RsaSha256Fulfillment.from(null, "".getBytes());
  }

  @Test(expected = NullPointerException.class)
  public final void testFromWithNullSignature() {
    RsaSha256Fulfillment
        .from((RSAPublicKey) TestKeyFactory.generateRandomRsaKeyPair().getPublic(), null);
  }

  @Test
  public final void testValidate() {
    final KeyPair rsaKeyPair = TestKeyFactory.generateRandomRsaKeyPair();
    final RsaSha256Fulfillment actual
        = constructRsaSha256Fulfillment(rsaKeyPair);
    assertTrue("Invalid condition", actual.verify(actual.getCondition(), MESSAGE.getBytes()));
  }

  @Test
  public final void testValidateWithEmptyMessage() {
    final KeyPair rsaKeyPair = TestKeyFactory.generateRandomRsaKeyPair();

    final RsaSha256Fulfillment actual;
    try {
      final Signature rsaSigner = Signature.getInstance("SHA256withRSA/PSS");
      rsaSigner.initSign(rsaKeyPair.getPrivate());
      // Empty message...
      byte[] rsaSignature = rsaSigner.sign();
      actual = constructRsaSha256Fulfillment((RSAPublicKey) rsaKeyPair.getPublic(), rsaSignature);
    } catch (NoSuchAlgorithmException | SignatureException | InvalidKeyException e) {
      throw new RuntimeException(e);
    }
    assertTrue("Invalid condition", actual.verify(actual.getCondition()));
  }

  @Test
  public void equalsHashcode() {
    final RsaSha256Fulfillment fulfillment1
        = constructRsaSha256Fulfillment(
        TestKeyFactory.generateRandomRsaKeyPair()
    );
    final RsaSha256Fulfillment fulfillment2 = fulfillment1;
    final RsaSha256Fulfillment fulfillment3
        = constructRsaSha256Fulfillment(
        TestKeyFactory.generateRandomRsaKeyPair()
    );

    assertThat(fulfillment1.equals(fulfillment1), CoreMatchers.is(true));
    assertThat(fulfillment2.equals(fulfillment2), CoreMatchers.is(true));
    assertThat(fulfillment3.equals(fulfillment3), CoreMatchers.is(true));

    assertThat(fulfillment1.equals(fulfillment2), CoreMatchers.is(true));
    assertThat(fulfillment1.equals(fulfillment3), CoreMatchers.is(false));

    assertThat(fulfillment2.equals(fulfillment1), CoreMatchers.is(true));
    assertThat(fulfillment2.equals(fulfillment3), CoreMatchers.is(false));

    assertThat(fulfillment3.equals(fulfillment1), CoreMatchers.is(false));
    assertThat(fulfillment3.equals(fulfillment2), CoreMatchers.is(false));

    assertThat(fulfillment1.hashCode(), CoreMatchers.is(fulfillment2.hashCode()));
    assertThat(fulfillment1.hashCode() == fulfillment3.hashCode(), CoreMatchers.is(false));
  }

  @Test
  public void testToString() {
    final RsaSha256Fulfillment fulfillment = constructRsaSha256Fulfillment(
        TestKeyFactory.generateRandomRsaKeyPair()
    );

    assertTrue(fulfillment.toString().contains("RsaSha256Fulfillment"));
    assertTrue(fulfillment.toString().contains("condition"));
    assertTrue(fulfillment.toString().contains("fingerprint"));
    assertTrue(fulfillment.toString().contains("cost"));
  }

}
