package org.interledger.cryptoconditions;

import static org.interledger.cryptoconditions.CryptoConditionType.RSA_SHA256;

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
public class RsaSha256Fulfillment extends FulfillmentBase<RsaSha256Condition>
    implements Fulfillment<RsaSha256Condition> {

  public static final BigInteger PUBLIC_EXPONENT = BigInteger.valueOf(65537);
  public static final String SHA_256_WITH_RSA_PSS = "SHA256withRSA/PSS";

  private final RSAPublicKey publicKey;
  private final byte[] signature;
  private final String signatureBase64Url;
  private final RsaSha256Condition condition;

  /**
   * Constructs an instance of the fulfillment.
   *
   * @param publicKey An {@link RSAPublicKey} to be used with this fulfillment.
   * @param signature A byte array that contains a binary representation of the signature associated
   *                  with this fulfillment.
   */
  public RsaSha256Fulfillment(final RSAPublicKey publicKey, final byte[] signature) {
    super(RSA_SHA256);
    Objects.requireNonNull(publicKey, "PublicKey must not be null!");
    Objects.requireNonNull(signature, "Signature must not be null!");

    this.publicKey = publicKey;
    this.signature = Arrays.copyOf(signature, signature.length);
    this.signatureBase64Url = Base64.getUrlEncoder().encodeToString(signature);
    this.condition = new RsaSha256Condition(publicKey);
  }

  /**
   * Returns the public key used in this fulfillment.
   *
   * @return The {@link RSAPublicKey} for this fulfillment.
   */
  public RSAPublicKey getPublicKey() {
    return publicKey;
  }

  /**
   * Returns a copy of the signature used in this fulfillment.
   *
   * @return A {@link byte[]} containing the signature for this fulfillment.
   *
   * @deprecated Java 8 does not have the concept of an immutable byte array, so this method allows
   *     external callers to accidentally or intentionally mute the prefix. As such, this method may
   *     be removed in a future version. Prefer {@link #getSignatureBase64Url()} instead.
   */
  @Deprecated
  public byte[] getSignature() {
    return this.signature;
  }

  /**
   * Returns the signature used in this fulfillment.
   *
   * @return A {@link String} containing the Base64Url-encoded signature for this fulfillment.
   */
  public String getSignatureBase64Url() {
    return this.signatureBase64Url;
  }

  @Override
  public RsaSha256Condition getCondition() {
    return this.condition;
  }

  @Override
  public boolean verify(final RsaSha256Condition condition, final byte[] message) {
    Objects.requireNonNull(condition,
        "Can't verify a RsaSha256Fulfillment against an null condition.");
    Objects.requireNonNull(message, "Message must not be null!");

    if (!getCondition().equals(condition)) {
      return false;
    }

    try {
      Signature rsaSigner = Signature.getInstance(SHA_256_WITH_RSA_PSS);
      rsaSigner.initVerify(publicKey);
      rsaSigner.update(message);
      return rsaSigner.verify(signature);
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

    RsaSha256Fulfillment that = (RsaSha256Fulfillment) object;

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
    final StringBuilder sb = new StringBuilder("RsaSha256Fulfillment{");
    sb.append("\n\tpublicKey=").append(publicKey);
    sb.append(", \n\tsignature=").append(signatureBase64Url);
    sb.append(", \n\tcondition=").append(condition);
    sb.append(", \n\ttype=").append(getType());
    sb.append("\n}");
    return sb.toString();
  }
}
