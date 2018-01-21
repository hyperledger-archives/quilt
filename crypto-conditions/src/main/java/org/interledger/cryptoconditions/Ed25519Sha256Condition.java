package org.interledger.cryptoconditions;

import org.interledger.cryptoconditions.der.DerOutputStream;
import org.interledger.cryptoconditions.der.DerTag;
import org.interledger.cryptoconditions.utils.HashUtils;

import net.i2p.crypto.eddsa.EdDSAPublicKey;
import org.immutables.value.Value;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Base64;
import java.util.Objects;

/**
 * Implementation of a crypto-condition using the ED-25519 and SHA-256 functions.
 */
public interface Ed25519Sha256Condition extends Sha256Condition {

  /**
   * Constructs an instance of the condition.
   *
   * @param edDsaPublicKey A {@link EdDSAPublicKey} used to create the fingerprint.
   *
   * @return A newly created, immutable instance of {@link Ed25519Sha256Condition}.
   */
  static Ed25519Sha256Condition from(final EdDSAPublicKey edDsaPublicKey) {

    Objects.requireNonNull(edDsaPublicKey);

    final byte[] fingerprint = HashUtils.hashFingerprintContents(
        AbstractEd25519Sha256Condition.constructFingerprintContents(edDsaPublicKey)
    );

    return ImmutableEd25519Sha256Condition.builder()
        .type(CryptoConditionType.ED25519_SHA256)
        .cost(AbstractEd25519Sha256Condition.COST)
        .fingerprint(fingerprint)
        .fingerprintBase64Url(Base64.getUrlEncoder().withoutPadding().encodeToString(fingerprint))
        .build();
  }

  /**
   * <p>Constructs an instance of {@link Ed25519Sha256Condition} using a fingerprint and a
   * cost.</p>
   *
   * <p>This constructor _should_ be primarily used by CODECs, whereas developers should, in
   * general, use  {@link Ed25519Sha256Condition#from(EdDSAPublicKey)}, or else create an actual
   * fulfillment using {@link Ed25519Sha256Fulfillment} and then generate a condition from that
   * object.</p>
   *
   * @param fingerprint The binary representation of the fingerprint for this condition.
   *
   * @return A newly created, immutable instance of {@link Ed25519Sha256Condition}.
   */
  static Ed25519Sha256Condition fromCostAndFingerprint(final byte[] fingerprint) {

    Objects.requireNonNull(fingerprint);

    return ImmutableEd25519Sha256Condition.builder()
        .type(CryptoConditionType.ED25519_SHA256)
        .cost(AbstractEd25519Sha256Condition.COST)
        .fingerprint(fingerprint)
        .fingerprintBase64Url(Base64.getUrlEncoder().withoutPadding().encodeToString(fingerprint))
        .build();
  }

  /**
   * An abstract implementation of {@link Ed25519Sha256Condition} for use by the
   * <tt>immutables</tt> library.
   *
   * @see "https://immutables.github.org"
   */
  @Value.Immutable
  abstract class AbstractEd25519Sha256Condition extends
      ConditionBase<Ed25519Sha256Condition> implements Ed25519Sha256Condition {

    /**
     * The public key and signature are a fixed size therefore the cost for an ED25519
     * crypto-condition is fixed at 131072.
     */
    static final long COST = 131072L;

    /**
     * Constructs the fingerprint for this condition.
     * <p/>
     * Note: This method is package-private as (opposed to private) for testing purposes.
     */
    static final byte[] constructFingerprintContents(final EdDSAPublicKey publicKey) {
      Objects.requireNonNull(publicKey);

      try {
        // Write public publicKey
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DerOutputStream out = new DerOutputStream(baos);
        out.writeTaggedObject(0, publicKey.getA().toByteArray());
        out.close();
        byte[] buffer = baos.toByteArray();

        // Wrap SEQUENCE
        baos = new ByteArrayOutputStream();
        out = new DerOutputStream(baos);
        out.writeEncoded(DerTag.CONSTRUCTED.getTag() + DerTag.SEQUENCE.getTag(), buffer);
        out.close();

        return baos.toByteArray();

      } catch (IOException ioe) {
        throw new UncheckedIOException("DER Encoding Error", ioe);
      }
    }

    /**
     * Prints the immutable value {@code Ed25519Sha256Condition} with attribute values.
     *
     * @return A string representation of the value
     */
    @Override
    public String toString() {
      return "Ed25519Sha256Condition{"
          + "type=" + getType()
          + ", fingerprint=" + getFingerprintBase64Url()
          + ", cost=" + getCost()
          + "}";
    }
  }
}
