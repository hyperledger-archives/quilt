package org.interledger.cryptoconditions;

import org.immutables.value.Value;

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;
import java.security.interfaces.RSAPublicKey;
import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;

/**
 * An implementation of {@link Fulfillment} for a crypto-condition fulfillment of type "RSA-SHA-256"
 * based upon an RSA key and the SHA-256 function.
 *
 * @see "https://datatracker.ietf.org/doc/draft-thomas-crypto-conditions/"
 */
public interface RsaSha256Fulfillment extends Fulfillment<RsaSha256Condition> {

  BigInteger PUBLIC_EXPONENT = BigInteger.valueOf(65537);

  /**
   * Constructs an instance of {@link RsaSha256Fulfillment}.
   *
   * @param publicKey An {@link RSAPublicKey} to be used with this fulfillment.
   * @param signature A byte array that contains a binary representation of the signature associated
   *                  with this fulfillment.
   *
   * @return A newly created, immutable instance of {@link RsaSha256Fulfillment}.
   */
  static RsaSha256Fulfillment from(final RSAPublicKey publicKey, final byte[] signature) {
    Objects.requireNonNull(publicKey, "PublicKey must not be null!");
    Objects.requireNonNull(signature, "Signature must not be null!");

    final byte[] immutableSignature = Arrays.copyOf(signature, signature.length);
    final String signatureBase64Url = Base64.getUrlEncoder().encodeToString(signature);
    final RsaSha256Condition condition = RsaSha256Condition.from(publicKey);

    return ImmutableRsaSha256Fulfillment.builder()
        .type(CryptoConditionType.RSA_SHA256)
        .publicKey(publicKey)
        .signature(immutableSignature)
        .signatureBase64Url(signatureBase64Url)
        .condition(condition)
        .build();
  }

  /**
   * Returns the public key used in this fulfillment.
   *
   * @return The {@link RSAPublicKey} for this fulfillment.
   */
  RSAPublicKey getPublicKey();

  /**
   * Returns a copy from the signature used in this fulfillment.
   *
   * @return A byte array containing the signature for this fulfillment.
   *
   * @deprecated Java 8 does not have the concept from an immutable byte array, so this method
   *     allows external callers to accidentally or intentionally mute the prefix. As such, this
   *     method may be removed in a future version. Prefer {@link #getSignatureBase64Url()}
   *     instead.
   */
  @Deprecated
  byte[] getSignature();

  /**
   * Returns the signature used in this fulfillment.
   *
   * @return A {@link String} containing the Base64Url-encoded signature for this fulfillment.
   */
  String getSignatureBase64Url();

  /**
   * An abstract implementation of {@link RsaSha256Fulfillment} for use by the <tt>immutables</tt>
   * library.
   *
   * @see "https://immutables.github.org"
   */
  @Value.Immutable
  abstract class AbstractRsaSha256Fulfillment implements RsaSha256Fulfillment {

    private static final String SHA_256_WITH_RSA_PSS = "SHA256withRSA/PSS";

    @Override
    public boolean verify(final RsaSha256Condition condition, final byte[] message) {
      Objects.requireNonNull(condition,
          "Can't verify a RsaSha256Fulfillment against an null condition.");
      Objects.requireNonNull(message, "Message must not be null!");

      if (!getCondition().equals(condition)) {
        return false;
      }

      try {
        final byte[] signatureBytes = Base64.getUrlDecoder().decode(getSignatureBase64Url());
        final Signature rsaSigner = Signature.getInstance(SHA_256_WITH_RSA_PSS);
        rsaSigner.initVerify(getPublicKey());
        rsaSigner.update(message);
        return rsaSigner.verify(signatureBytes);
      } catch (InvalidKeyException | NoSuchAlgorithmException | SignatureException e) {
        throw new RuntimeException(e);
      }
    }

    /**
     * Prints the immutable value {@code RsaSha256Fulfillment} with attribute values.
     *
     * @return A string representation of the value
     */
    @Override
    public String toString() {
      return "RsaSha256Fulfillment{"
          + "publicKey=" + getPublicKey()
          + ", signature=" + getSignatureBase64Url()
          + ", type=" + getType()
          + ", condition=" + getCondition()
          + "}";
    }
  }
}
