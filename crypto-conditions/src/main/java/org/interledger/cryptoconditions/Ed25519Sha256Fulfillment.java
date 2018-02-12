package org.interledger.cryptoconditions;

import net.i2p.crypto.eddsa.EdDSAEngine;
import net.i2p.crypto.eddsa.EdDSAPublicKey;
import org.immutables.value.Value;

import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;

/**
 * An implementation of {@link Fulfillment} for a crypto-condition fulfillment of type
 * "ED25519-SHA256" using the ED-25519 and SHA-256 functions.
 *
 * @see "https://datatracker.ietf.org/doc/draft-thomas-crypto-conditions/"
 */
public interface Ed25519Sha256Fulfillment extends Fulfillment<Ed25519Sha256Condition> {

  /**
   * Constructs an instance of the fulfillment.
   *
   * @param publicKey An {@link EdDSAPublicKey} associated with this fulfillment and its
   *                  corresponding condition.
   * @param signature A byte array containing the signature associated with this fulfillment.
   *
   * @return A newly created, immutable instance of {@link Ed25519Sha256Fulfillment}.
   */
  static Ed25519Sha256Fulfillment from(final EdDSAPublicKey publicKey, final byte[] signature) {
    Objects.requireNonNull(publicKey, "EdDSAPublicKey must not be null!");
    Objects.requireNonNull(signature, "Signature must not be null!");

    final byte[] immutableSignature = Arrays.copyOf(signature, signature.length);
    final String signatureBase64Url = Base64.getUrlEncoder().encodeToString(signature);
    final Ed25519Sha256Condition condition = Ed25519Sha256Condition.from(publicKey);

    return ImmutableEd25519Sha256Fulfillment.builder()
        .type(CryptoConditionType.ED25519_SHA256)
        .publicKey(publicKey)
        .signature(immutableSignature)
        .signatureBase64Url(signatureBase64Url)
        .derivedCondition(condition)
        .build();
  }

  /**
   * Returns the public key used.
   *
   * @return The {@link EdDSAPublicKey} for this fulfillment.
   */
  EdDSAPublicKey getPublicKey();

  /**
   * Returns a copy from the signature linked to this fulfillment.
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
   * Returns a copy from the signature linked to this fulfillment.
   *
   * @return A {@link String} containing the Base64Url-encoded signature for this fulfillment.
   */
  String getSignatureBase64Url();

  /**
   * An abstract implementation of {@link Ed25519Sha256Fulfillment} for use by the
   * <tt>immutables</tt> library.
   *
   * @see "https://immutables.github.org"
   */
  @Value.Immutable
  abstract class AbstractEd25519Sha256Fulfillment implements Ed25519Sha256Fulfillment {

    @Override
    public boolean verify(final Ed25519Sha256Condition condition, final byte[] message) {
      Objects.requireNonNull(condition,
          "Can't verify a Ed25519Sha256Fulfillment against an null condition.");
      Objects.requireNonNull(message, "Message must not be null!");

      if (!getDerivedCondition().equals(condition)) {
        return false;
      }

      try {
        final byte[] signatureBytes = Base64.getUrlDecoder().decode(getSignatureBase64Url());
        // MessageDigest isn't particularly expensive to construct (see MessageDigest source).
        final MessageDigest messageDigest = MessageDigest.getInstance("SHA-512");
        final Signature edDsaSigner = new EdDSAEngine(messageDigest);
        edDsaSigner.initVerify(getPublicKey());
        edDsaSigner.update(message);
        return edDsaSigner.verify(signatureBytes);
      } catch (InvalidKeyException | NoSuchAlgorithmException | SignatureException e) {
        throw new RuntimeException(e);
      }
    }

    /**
     * Prints the immutable value {@code Ed25519Sha256Fulfillment} with attribute values.
     *
     * @return A string representation of the value
     */
    @Override
    public String toString() {
      return "Ed25519Sha256Fulfillment{"
          + "publicKey=" + getPublicKey()
          + ", signature=" + getSignatureBase64Url()
          + ", type=" + getType()
          + ", condition=" + getDerivedCondition()
          + "}";
    }
  }
}
