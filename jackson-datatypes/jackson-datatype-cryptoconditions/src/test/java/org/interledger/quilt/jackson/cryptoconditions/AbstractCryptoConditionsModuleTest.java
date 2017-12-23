package org.interledger.quilt.jackson.cryptoconditions;

import org.interledger.cryptoconditions.CryptoConditionReader;
import org.interledger.cryptoconditions.Ed25519Sha256Condition;
import org.interledger.cryptoconditions.Ed25519Sha256Fulfillment;
import org.interledger.cryptoconditions.Fulfillment;
import org.interledger.cryptoconditions.PrefixSha256Condition;
import org.interledger.cryptoconditions.PrefixSha256Fulfillment;
import org.interledger.cryptoconditions.PreimageSha256Condition;
import org.interledger.cryptoconditions.PreimageSha256Fulfillment;
import org.interledger.cryptoconditions.RsaSha256Condition;
import org.interledger.cryptoconditions.RsaSha256Fulfillment;
import org.interledger.cryptoconditions.ThresholdSha256Condition;
import org.interledger.cryptoconditions.ThresholdSha256Fulfillment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.io.BaseEncoding;
import net.i2p.crypto.eddsa.EdDSAEngine;
import net.i2p.crypto.eddsa.EdDSAPrivateKey;
import net.i2p.crypto.eddsa.EdDSAPublicKey;
import net.i2p.crypto.eddsa.spec.EdDSANamedCurveSpec;
import net.i2p.crypto.eddsa.spec.EdDSANamedCurveTable;
import net.i2p.crypto.eddsa.spec.EdDSAPrivateKeySpec;
import net.i2p.crypto.eddsa.spec.EdDSAPublicKeySpec;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.Before;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.PublicKey;
import java.security.Security;
import java.security.Signature;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Objects;

/**
 * Validates the functionality of {@link CryptoConditionsModule}.
 */
public abstract class AbstractCryptoConditionsModuleTest {

  public static final BigInteger PUBLIC_EXPONENT = BigInteger.valueOf(65537);

  protected static final String RSA_SHA_256_PRIVATE_KEY =
      "MIICWwIBAAKBgQClbkoOcBAXWJpRh9x+qEHRVvLsDjatUqRN/rHmH3rZkdjFE"
          + "Fb/7bFitMDyg6EqiKOU3/Umq3KRy7MHzqv84LHf1c2VCAltWyuLbfXWce9jd8CSHLI8Jwpw4lmOb/idGfEFrML"
          + "T8Ms18pKA4Thrb2TE7yLh4fINDOjP+yJJvZohNwIDAQABAoGAM6UEKpCyfU9UUcqbu9C0R3GhAa+IQ0Cu+YhfK"
          + "ku+kuiUpySsPFaMj5eFOtB8AmbIxqPKCSnx6PESMYhEKfxNmuVf7olqEM5wfD7X5zTkRyejlXRQGlMmgxCcKrr"
          + "Kuig8MbS9L1PD7jfjUs7jT55QO9gMBiKtecbc7og1R8ajsyUCQQDn6JQnIKh3UXJzo1YFPqKhvAyUqnLVXG6GK"
          + "Wst/JZ5SMCnLLzMp+rLNXBuCaHfVaFTW9mzzDQWCzttzT7ajmRDAkEAtp3KHPfU1+yB51uQ/MqHSrzeEj/ScAG"
          + "AqpBHm25I3o1n7ST58Z2FuidYdPVCzSDccj5pYzZKH5QlRSsmmmeZ/QJAKPoTk4ZVvh+KFZy6ylpy6hkMMAieG"
          + "c0nSlVvNsT24Z9VSzTAd3kEJ7vdjdPt4kSDKPOF2Bsw6OQ7L/+gJ4YZeQJAGos485j6cSBJiY1/t57gp3ZoeRK"
          + "ZzfoJ78DlB6yyHtdDAe9b/Ui+RV6utuFnglWCdYCo5OjhQVHRUQqCo/LnKQJAJxVqukEm0kqB86Uoy/sn9WiG+"
          + "ECp9uhuF6RLlP6TGVhLjiL93h5aLjvYqluo2FhBlOshkKz4MrhH8To9JKefTQ==";

  /**
   * Need to add BouncyCastle so we have a provider that supports SHA256withRSA/PSS signatures
   */
  static {
    Provider bc = new BouncyCastleProvider();
    Security.addProvider(bc);
  }

  protected ObjectMapper objectMapper;
  protected Encoding encodingToUse;
  protected String expectedEncodedValue;

  /**
   * Required-args Constructor (used by JUnit's parameterized test annotation).
   *
   * @param encodingToUse        A {@link Encoding} to use for each test run.
   * @param expectedEncodedValue A {@link String} encoded in the above encoding to assert against.
   */
  public AbstractCryptoConditionsModuleTest(
      final Encoding encodingToUse, final String expectedEncodedValue
  ) {
    this.encodingToUse = Objects.requireNonNull(encodingToUse);
    this.expectedEncodedValue = Objects.requireNonNull(expectedEncodedValue);
  }

  //////////////////
  // Protected Helpers
  //////////////////

  protected static KeyPair constructRsaKeyPair() {
    try {
      final PrivateKey privKey = buildRsaPrivKey();

      final KeyFactory keyFactory = KeyFactory.getInstance("RSA");
      final RSAPrivateKey privateKey = buildRsaPrivKey();
      final RSAPublicKeySpec publicKeySpec = new RSAPublicKeySpec(
          privateKey.getModulus(), PUBLIC_EXPONENT
      );
      final PublicKey pubKey = keyFactory.generatePublic(publicKeySpec);
      return new KeyPair(pubKey, privKey);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  protected static RSAPrivateKey buildRsaPrivKey() {
    final byte[] innerKey = BaseEncoding.base64().decode(RSA_SHA_256_PRIVATE_KEY);
    final byte[] result = new byte[innerKey.length + 26];
    System.arraycopy(
        BaseEncoding.base64().decode("MIIEvAIBADANBgkqhkiG9w0BAQEFAASCBKY="),
        0, result, 0, 26
    );
    System.arraycopy(BigInteger.valueOf(result.length - 4).toByteArray(), 0, result, 2, 2);
    System.arraycopy(BigInteger.valueOf(innerKey.length).toByteArray(), 0, result, 24, 2);
    System.arraycopy(innerKey, 0, result, 26, innerKey.length);
    PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(result);

    try {
      final KeyFactory keyFactory = KeyFactory.getInstance("RSA");
      return (RSAPrivateKey) keyFactory.generatePrivate(spec);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Helper method to construct an instance of {@link KeyPair} containing keys for testing
   * purposes.
   *
   * @return An instance of {@link KeyPair}.
   */
  protected static KeyPair constructEd25519KeyPair() {
    final byte[] TEST_PUBKEY = BaseEncoding.base64()
        .decode("Nq4bl8V3rmr7ApTpGDn6ex+TMnkbnyxdWGgZAl9KLx0=");

    final byte[] TEST_PRIVKEY = BaseEncoding.base64()
        .decode("A59Fwv0b/smwKKpDy66asxKeFME63RYiK0Rj6Aaf3To=");

    final EdDSANamedCurveSpec edParams = EdDSANamedCurveTable
        .getByName(CryptoConditionReader.ED_25519);
    assert (edParams != null);

    final EdDSAPublicKeySpec pubKeySpec = new EdDSAPublicKeySpec(TEST_PUBKEY, edParams);
    final PublicKey pubKey = new EdDSAPublicKey(pubKeySpec);

    final EdDSAPrivateKeySpec privateKeySpec = new EdDSAPrivateKeySpec(TEST_PRIVKEY, edParams);
    final PrivateKey privKey = new EdDSAPrivateKey(privateKeySpec);

    return new KeyPair(pubKey, privKey);
  }

  protected static PreimageSha256Condition constructPreimageCondition() {
    final byte[] preimage = "you built a time machine out of a DeLorean?".getBytes();
    return new PreimageSha256Fulfillment(preimage).getCondition();
  }

  protected static PrefixSha256Condition constructPrefixCondition() {
    final byte[] prefix = "I'm your density. I mean, your destiny.".getBytes();
    return new PrefixSha256Condition(prefix, 20, constructPreimageCondition());
  }

  protected static RsaSha256Condition constructRsaCondition() {
    try {
      final KeyFactory keyFactory = KeyFactory.getInstance("RSA");
      final RSAPrivateKey privateKey = buildRsaPrivKey();
      final RSAPublicKeySpec publicKeySpec = new RSAPublicKeySpec(
          privateKey.getModulus(), PUBLIC_EXPONENT
      );
      final PublicKey myPublicKey = keyFactory.generatePublic(publicKeySpec);

      return new RsaSha256Condition((RSAPublicKey) myPublicKey);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  protected static Ed25519Sha256Condition constructEd25519Condition() {
    try {
      final MessageDigest sha512Digest = MessageDigest.getInstance("SHA-512");

      KeyPair edDsaKeyPair = constructEd25519KeyPair();
      Signature edDsaSigner = new EdDSAEngine(sha512Digest);
      edDsaSigner.initSign(edDsaKeyPair.getPrivate());

      final byte[] prefix = "Oh, honey, he's teasing you. Nobody has two television sets."
          .getBytes();
      edDsaSigner.update(prefix);

      final byte[] message = "Marty! You've got to come back with me!".getBytes();
      edDsaSigner.update(message);

      return new Ed25519Sha256Condition((EdDSAPublicKey) edDsaKeyPair.getPublic());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  protected static ThresholdSha256Condition constructThresholdCondition() {
    return new ThresholdSha256Condition(
        2,
        Lists.newArrayList(
            constructPreimageCondition(), constructRsaCondition(), constructPrefixCondition()
        )
    );
  }

  protected static Fulfillment constructFulfillment() {
    return constructPreimageFulfillment();
  }

  protected static PreimageSha256Fulfillment constructPreimageFulfillment() {
    final byte[] preimage = "you built a time machine out of a DeLorean?".getBytes();
    return new PreimageSha256Fulfillment(preimage);
  }

  protected static PrefixSha256Fulfillment constructPrefixFulfillment() {
    final byte[] prefix = "I'm your density. I mean, your destiny.".getBytes();
    return new PrefixSha256Fulfillment(prefix, 20, constructPreimageFulfillment());
  }

  protected static RsaSha256Fulfillment constructRsaFulfillment() {
    try {

      final KeyPair rsaKeyPair = constructRsaKeyPair();
      Signature rsaSigner = Signature.getInstance("SHA256withRSA/PSS");
      rsaSigner.initSign(rsaKeyPair.getPrivate());

      final byte[] message = "Marty, your acting like you haven't seen me in a week.".getBytes();
      rsaSigner.update(message);

      return new RsaSha256Fulfillment(
          (RSAPublicKey) rsaKeyPair.getPublic(),
          "signature".getBytes()
      );
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  protected static Ed25519Sha256Fulfillment constructEd25519Fulfillment() {
    try {
      final MessageDigest sha512Digest = MessageDigest.getInstance("SHA-512");

      KeyPair edDsaKeyPair = constructEd25519KeyPair();
      Signature edDsaSigner = new EdDSAEngine(sha512Digest);
      edDsaSigner.initSign(edDsaKeyPair.getPrivate());

      final byte[] prefix = "Oh, honey, he's teasing you. Nobody has two television sets."
          .getBytes();
      edDsaSigner.update(prefix);

      final byte[] message = "Marty! You've got to come back with me!".getBytes();
      edDsaSigner.update(message);

      final byte[] signature = "5VZDAMNgrHKQhuLMgG6CioSHfx645dl02HPgZSJJAVVfuIIVkKM7rMYeOXAc-bRr0lv18FlbviRlUUFDjnoQCw"
          .getBytes();
      return new Ed25519Sha256Fulfillment((EdDSAPublicKey) edDsaKeyPair.getPublic(), signature);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  protected static ThresholdSha256Fulfillment constructThresholdFulfillment() {
    return new ThresholdSha256Fulfillment(
        Lists.newArrayList(),
        Lists.newArrayList(
            constructPreimageFulfillment(), constructRsaFulfillment(), constructPrefixFulfillment()
        )
    );
  }

  @Before
  public void setup() {
    this.objectMapper = new ObjectMapper()
        .registerModule(new CryptoConditionsModule(encodingToUse));
  }

}