package org.interledger.cryptoconditions.helpers;

import org.interledger.cryptoconditions.CryptoConditionReader;
import org.interledger.cryptoconditions.utils.UnsignedBigInteger;

import net.i2p.crypto.eddsa.EdDSAPublicKey;
import net.i2p.crypto.eddsa.spec.EdDSANamedCurveTable;
import net.i2p.crypto.eddsa.spec.EdDSAPublicKeySpec;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;

/**
 * A helper class to create RSA and Ed25519 keys.
 */
public class TestKeyFactory {

  /**
   * Given a modulus, construct an RSA public key with a known exponent and return it.
   *
   * @param base64UrlEncodedModulus A base64Url-encoded String representing the public key bytes
   *                                for the key to assemble.
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
   * @return A {@link EdDSAPublicKey}.
   */
  public static EdDSAPublicKey constructEdDsaPublicKey(final String base64UrlEncodedPublicKey) {
    final byte[] publicKeyBytes = Base64.getUrlDecoder().decode(base64UrlEncodedPublicKey);
    final EdDSAPublicKeySpec publicKeyspec = new EdDSAPublicKeySpec(
        publicKeyBytes, EdDSANamedCurveTable.getByName(CryptoConditionReader.ED_25519)
    );
    return new EdDSAPublicKey(publicKeyspec);
  }

}
