package org.interledger.cryptoconditions;

import org.interledger.cryptoconditions.der.DerOutputStream;
import org.interledger.cryptoconditions.der.DerTag;
import org.interledger.cryptoconditions.utils.HashUtils;
import org.interledger.cryptoconditions.utils.UnsignedBigInteger;

import org.immutables.value.Value;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import java.util.Objects;

/**
 * Implementation of a condition based on RSA PKI and the SHA-256 function.
 */
public interface RsaSha256Condition extends Sha256Condition {

  /**
   * <p>Constructs an instance of  {@link RsaSha256Condition}.</p>
   *
   * <p>Development Note: This constructor is _not_ package-private because it may be desirable for
   * a party who only has a public-rsaPublicKey to be able to create an RSA-SHA-256 Condition. For
   * example, it might be useful for some other party who _does_ have access to the RSA private
   * rsaPublicKey to create an RSA condition.</p>
   *
   * @param rsaPublicKey The RSA public rsaPublicKey associated with the condition.
   *
   * @return A newly created, immutable instance of {@link RsaSha256Condition}.
   */
  static RsaSha256Condition from(final RSAPublicKey rsaPublicKey) {
    Objects.requireNonNull(rsaPublicKey);

    final long cost = AbstractRsaSha256Condition.calculateCost(rsaPublicKey);
    final byte[] fingerprint = HashUtils.hashFingerprintContents(
        AbstractRsaSha256Condition.constructFingerprintContents(rsaPublicKey)
    );

    return ImmutableRsaSha256Condition.builder()
        .type(CryptoConditionType.RSA_SHA256)
        .cost(cost)
        .fingerprint(fingerprint)
        .fingerprintBase64Url(Base64.getUrlEncoder().withoutPadding().encodeToString(fingerprint))
        .build();
  }

  /**
   * <p>Constructs an instance of {@link RsaSha256Condition} using a fingerprint and a cost.</p>
   *
   * <p>This constructor _should_ be primarily used by Codecs, whereas developers should, in
   * general, create an actual fulfillment via {@link PreimageSha256Fulfillment
   * #PreimageSha256Fulfillment(byte[])} and then generate a condition from that object.</p>
   *
   * @param cost        The cost associated with this condition.
   * @param fingerprint An instance of byte array that contains the calculated fingerprint for this
   *                    condition.
   *
   * @return A newly created, immutable instance of {@link RsaSha256Condition}.
   */
  static RsaSha256Condition fromCostAndFingerprint(final long cost, final byte[] fingerprint) {
    Objects.requireNonNull(fingerprint);
    return ImmutableRsaSha256Condition.builder()
        .type(CryptoConditionType.RSA_SHA256)
        .cost(cost)
        .fingerprint(fingerprint)
        .fingerprintBase64Url(Base64.getUrlEncoder().withoutPadding().encodeToString(fingerprint))
        .build();
  }

  /**
   * An abstract implementation of {@link RsaSha256Condition} for use by the <tt>immutables</tt>
   * library.
   *
   * @see "https://immutables.github.org"
   */
  @Value.Immutable
  abstract class AbstractRsaSha256Condition extends ConditionBase<RsaSha256Condition> implements
      RsaSha256Condition {

    /**
     * Constructs the fingerprint for this condition.
     * <p/>
     * Note: This method is package-private as (opposed to private) for testing purposes.
     */
    static final byte[] constructFingerprintContents(final RSAPublicKey publicKey) {
      Objects.requireNonNull(publicKey);
      validatePublicKey(publicKey);

      try {
        // Build modulus
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DerOutputStream out = new DerOutputStream(baos);
        out.writeTaggedObject(0, UnsignedBigInteger.toUnsignedByteArray(publicKey.getModulus()));
        out.close();
        byte[] buffer = baos.toByteArray();

        // Wrap SEQUENCE
        baos = new ByteArrayOutputStream();
        out = new DerOutputStream(baos);
        out.writeEncoded(DerTag.CONSTRUCTED.getTag() + DerTag.SEQUENCE.getTag(), buffer);
        out.close();
        return baos.toByteArray();

      } catch (IOException e) {
        throw new UncheckedIOException("DER Encoding Error", e);
      }
    }

    /**
     * Calculates the cost from a condition based on an RSA key as ((modulus size in bytes)^2).
     *
     * @param key The key used in the condition.
     *
     * @return the cost from a condition using this key.
     */
    static final long calculateCost(RSAPublicKey key) {
      return (long) Math.pow(UnsignedBigInteger.toUnsignedByteArray(key.getModulus()).length, 2);
    }

    static final void validatePublicKey(final RSAPublicKey publicKey) {
      // Validate key
      if (publicKey.getPublicExponent().compareTo(RsaSha256Fulfillment.PUBLIC_EXPONENT) != 0) {
        throw new IllegalArgumentException("Public Exponent from RSA key must be 65537.");
      }

      if (publicKey.getModulus().bitLength() <= 1017 || publicKey.getModulus().bitLength() > 4096) {
        throw new IllegalArgumentException(
            "Modulus from RSA key must be greater than 128 bytes and less than 512 bytes.");
      }
    }

    /**
     * Prints the immutable value {@code RsaSha256Condition} with attribute values.
     *
     * @return A string representation of the value
     */
    @Override
    public String toString() {
      return "RsaSha256Condition{"
          + "type=" + getType()
          + ", fingerprint=" + getFingerprintBase64Url()
          + ", cost=" + getCost()
          + "}";
    }
  }
}
