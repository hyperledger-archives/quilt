package org.interledger.quilt.jackson.cryptoconditions;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.BaseEncoding;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Objects;
import java.util.Optional;
import net.i2p.crypto.eddsa.EdDSAPrivateKey;
import net.i2p.crypto.eddsa.EdDSAPublicKey;
import net.i2p.crypto.eddsa.spec.EdDSANamedCurveSpec;
import net.i2p.crypto.eddsa.spec.EdDSANamedCurveTable;
import net.i2p.crypto.eddsa.spec.EdDSAPrivateKeySpec;
import net.i2p.crypto.eddsa.spec.EdDSAPublicKeySpec;
import org.interledger.cryptoconditions.Condition;
import org.interledger.cryptoconditions.CryptoConditionReader;
import org.interledger.cryptoconditions.Fulfillment;
import org.interledger.quilt.jackson.cryptoconditions.CryptoConditionsModule.Encoding;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * Validates the functionality of {@link CryptoConditionsModule}.
 */
public class AbstractCryptoConditionsModuleTest {

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

  protected static class CryptoConditionsContainer {

    @JsonProperty("condition")
    private final Optional<Condition> condition;

    @JsonProperty("fulfillment")
    private final Optional<Fulfillment> fulfillment;

    @JsonCreator
    public CryptoConditionsContainer(
        @JsonProperty("condition") final Optional<Condition> condition,
        @JsonProperty("fulfillment") final Optional<Fulfillment> fulfillment
    ) {
      this.condition = Objects.requireNonNull(condition);
      this.fulfillment = Objects.requireNonNull(fulfillment);
    }

    public Optional<Condition> getCondition() {
      return condition;
    }

    public Optional<Fulfillment> getFulfillment() {
      return fulfillment;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      CryptoConditionsContainer that = (CryptoConditionsContainer) o;
      return Objects.equals(getCondition(), that.getCondition()) &&
          Objects.equals(getFulfillment(), that.getFulfillment());
    }

    @Override
    public int hashCode() {

      return Objects.hash(getCondition(), getFulfillment());
    }

    @Override
    public String toString() {
      return "CryptoConditionsContainer{" +
          "condition=" + condition +
          ", fulfillment=" + fulfillment +
          '}';
    }
  }
}