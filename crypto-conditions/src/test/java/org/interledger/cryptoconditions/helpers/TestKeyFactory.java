package org.interledger.cryptoconditions.helpers;

import org.interledger.cryptoconditions.CryptoConditionReader;
import org.interledger.cryptoconditions.utils.UnsignedBigInteger;

import com.google.common.io.BaseEncoding;
import net.i2p.crypto.eddsa.EdDSAPrivateKey;
import net.i2p.crypto.eddsa.EdDSAPublicKey;
import net.i2p.crypto.eddsa.spec.EdDSANamedCurveSpec;
import net.i2p.crypto.eddsa.spec.EdDSANamedCurveTable;
import net.i2p.crypto.eddsa.spec.EdDSAPrivateKeySpec;
import net.i2p.crypto.eddsa.spec.EdDSAPublicKeySpec;

import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAKeyGenParameterSpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;

/**
 * A helper class to create RSA and Ed25519 keys.
 */
public class TestKeyFactory {

  public static final String RSA_MODULUS =
      "4e-LJNb3awnIHtd1KqJi8ETwSodNQ4CdMc6mEvmbDJeotDdBU-Pu89ZmFo"
          + "Q-DkHCkyZLcbYXPbHPDWzVWMWGV3Bvzwl_cExIPlnL_f1bPue8gNdAxeDwR_PoX8DXWBV3am8_I8XcXnlxOaaILjgz"
          + "akpfs2E3Yg_zZj264yhHKAGGL3Ly-HsgK5yJrdfNWwoHb3xT41A59n7RfsgV5bQwXMYxlwaNXm5Xm6beX04-V99eTg"
          + "cv8s5MZutFIzlzh1J1ljnwJXv1fb1cRD-1FYzOCj02rce6AfM6C7bbsr-YnWBxEvI0TZk-d-VjwdNh3t9X2pbvLPxo"
          + "XwArY4JGpbMJuQ";

  public static final byte[] TEST_ED25519_PUBKEY = BaseEncoding.base64()
      .decode("Nq4bl8V3rmr7ApTpGDn6ex+TMnkbnyxdWGgZAl9KLx0=");
  public static final byte[] TEST_ED25519_PRIVKEY = BaseEncoding.base64()
      .decode("A59Fwv0b/smwKKpDy66asxKeFME63RYiK0Rj6Aaf3To=");

  /**
   * Generate an RSA KeyPair.
   *
   * @return A randomly generated {@link KeyPair}.
   */
  public static KeyPair generateRandomRsaKeyPair() {
    try {
      final KeyPairGenerator rsaKpg = KeyPairGenerator.getInstance("RSA");
      rsaKpg.initialize(new RSAKeyGenParameterSpec(2048, new BigInteger("65537")));
      final KeyPair rsaKeyPair = rsaKpg.generateKeyPair();
      return rsaKeyPair;
    } catch (InvalidAlgorithmParameterException | NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Given a modulus, construct an RSA public key with a known exponent and return it.
   *
   * @param base64UrlEncodedModulus A base64Url-encoded String representing the public key bytes for
   *                                the key to assemble.
   *
   * @return A {@link EdDSAPublicKey}.
   */
  public static RSAPublicKey constructRsaPublicKey(final String base64UrlEncodedModulus) {
    try {
      final byte[] modulusBytes = Base64.getUrlDecoder().decode(base64UrlEncodedModulus);
      final BigInteger modulus = UnsignedBigInteger.fromUnsignedByteArray(modulusBytes);
      final BigInteger exponent = BigInteger.valueOf(65537);
      final RSAPublicKeySpec spec = new RSAPublicKeySpec(modulus, exponent);
      final KeyFactory keyFactory = KeyFactory.getInstance("RSA");
      return (RSAPublicKey) keyFactory.generatePublic(spec);
    } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
      throw new RuntimeException("Error creating RSA key.", e);
    }
  }

  /**
   * Given a public key, construct an EdDSA public key and return it.
   *
   * @param base64UrlEncodedPublicKey A base64Url-encoded String representing the public key bytes
   *                                  for the key to assemble.
   *
   * @return A {@link EdDSAPublicKey}.
   */
  public static EdDSAPublicKey constructEdDsaPublicKey(final String base64UrlEncodedPublicKey) {
    final byte[] publicKeyBytes = Base64.getUrlDecoder().decode(base64UrlEncodedPublicKey);
    final EdDSAPublicKeySpec publicKeyspec = new EdDSAPublicKeySpec(
        publicKeyBytes, EdDSANamedCurveTable.getByName(CryptoConditionReader.ED_25519)
    );
    return new EdDSAPublicKey(publicKeyspec);
  }

  /**
   * Helper method to construct an instance of {@link KeyPair} containing keys for testing
   * purposes.
   *
   * @return An instance of {@link KeyPair}.
   */
  public static KeyPair constructEd25519KeyPair() {
    final EdDSANamedCurveSpec edParams = EdDSANamedCurveTable
        .getByName(CryptoConditionReader.ED_25519);
    assert (edParams != null);

    final EdDSAPublicKeySpec pubKeySpec = new EdDSAPublicKeySpec(TEST_ED25519_PUBKEY, edParams);
    final PublicKey pubKey = new EdDSAPublicKey(pubKeySpec);

    final EdDSAPrivateKeySpec privateKeySpec = new EdDSAPrivateKeySpec(TEST_ED25519_PRIVKEY,
        edParams);
    final PrivateKey privKey = new EdDSAPrivateKey(privateKeySpec);

    return new KeyPair(pubKey, privKey);
  }

  /**
   * Generate an EdDSA Ed25519 KeyPair.
   *
   * @return A randomly generated {@link KeyPair}.
   */
  public static KeyPair generateRandomEd25519KeyPair() {
    final net.i2p.crypto.eddsa.KeyPairGenerator edDsaKpg = new net.i2p.crypto.eddsa.KeyPairGenerator();
    final KeyPair edDsaKeyPair = edDsaKpg.generateKeyPair();
    return edDsaKeyPair;
  }

}
