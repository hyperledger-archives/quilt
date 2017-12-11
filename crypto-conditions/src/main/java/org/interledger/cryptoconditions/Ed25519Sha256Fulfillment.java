package org.interledger.cryptoconditions;

import static org.interledger.cryptoconditions.CryptoConditionType.ED25519_SHA256;

import net.i2p.crypto.eddsa.EdDSAEngine;
import net.i2p.crypto.eddsa.EdDSAPublicKey;

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
public class Ed25519Sha256Fulfillment extends FulfillmentBase<Ed25519Sha256Condition>
    implements Fulfillment<Ed25519Sha256Condition> {

  private final EdDSAPublicKey publicKey;
  private final byte[] signature;
  private final String signatureBase64Url;
  private final Ed25519Sha256Condition condition;

  /**
   * Constructs an instance of the fulfillment.
   *
   * @param publicKey An {@link EdDSAPublicKey} associated with this fulfillment and its
   *                  corresponding condition.
   * @param signature A byte array containing the signature associated with this fulfillment.
   */
  public Ed25519Sha256Fulfillment(final EdDSAPublicKey publicKey, final byte[] signature) {
    super(ED25519_SHA256);

    Objects.requireNonNull(publicKey, "EdDSAPublicKey must not be null!");
    Objects.requireNonNull(signature, "Signature must not be null!");

    this.publicKey = publicKey;
    this.signature = Arrays.copyOf(signature, signature.length);
    this.signatureBase64Url = Base64.getUrlEncoder().encodeToString(signature);
    this.condition = new Ed25519Sha256Condition(publicKey);
  }

  /**
   * Returns the public key used.
   *
   * @return The {@link EdDSAPublicKey} for this fulfillment.
   */
  public EdDSAPublicKey getPublicKey() {
    return publicKey;
  }

  /**
   * Returns a copy of the signature linked to this fulfillment.
   *
   * @return A byte array containing the signature for this fulfillment.
   * @deprecated Java 8 does not have the concept of an immutable byte array, so this method allows
   *     external callers to accidentally or intentionally mute the prefix. As such, this method may
   *     be removed in a future version. Prefer {@link #getSignatureBase64Url()} instead.
   */
  @Deprecated
  public byte[] getSignature() {
    return this.signature;
  }

  /**
   * Returns a copy of the signature linked to this fulfillment.
   *
   * @return A {@link String} containing the Base64Url-encoded signature for this fulfillment.
   */
  public String getSignatureBase64Url() {
    return this.signatureBase64Url;
  }

  @Override
  public Ed25519Sha256Condition getCondition() {
    return this.condition;
  }

  @Override
  public boolean verify(final Ed25519Sha256Condition condition, final byte[] message) {
    Objects.requireNonNull(condition,
        "Can't verify a Ed25519Sha256Fulfillment against an null condition.");
    Objects.requireNonNull(message, "Message must not be null!");

    if (!getCondition().equals(condition)) {
      return false;
    }

    try {
      // MessageDigest isn't particularly expensive to construct (see MessageDigest source).
      final MessageDigest messageDigest = MessageDigest.getInstance("SHA-512");
      final Signature edDsaSigner = new EdDSAEngine(messageDigest);
      edDsaSigner.initVerify(publicKey);
      edDsaSigner.update(message);
      return edDsaSigner.verify(signature);
    } catch (InvalidKeyException | NoSuchAlgorithmException | SignatureException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (object == null || getClass() != object.getClass()) {
      return false;
    }
    if (!super.equals(object)) {
      return false;
    }

    Ed25519Sha256Fulfillment that = (Ed25519Sha256Fulfillment) object;

    if (!publicKey.equals(that.publicKey)) {
      return false;
    }
    if (!Arrays.equals(signature, that.signature)) {
      return false;
    }
    return condition.equals(that.condition);
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + publicKey.hashCode();
    result = 31 * result + Arrays.hashCode(signature);
    result = 31 * result + condition.hashCode();
    return result;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("Ed25519Sha256Fulfillment{");
    sb.append("\npublicKey=").append(publicKey);
    sb.append(", \n\tsignature=").append(signatureBase64Url);
    sb.append(", \n\tcondition=").append(condition);
    sb.append(", \n\ttype=").append(getType());
    sb.append("\n}");
    return sb.toString();
  }
}
