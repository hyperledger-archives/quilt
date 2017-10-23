package org.interledger.cryptoconditions;

import net.i2p.crypto.eddsa.EdDSAPublicKey;
import org.interledger.cryptoconditions.der.DerOutputStream;
import org.interledger.cryptoconditions.der.DerTag;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Objects;


/**
 * Implementation of a crypto-condition using the ED-25519 and SHA-256 functions.
 */
public final class Ed25519Sha256Condition extends Sha256Condition implements SimpleCondition {

  /**
   * The public key and signature are a fixed size therefore the cost for an ED25519
   * crypto-condition is fixed at 131072.
   */
  private static final long COST = 131072L;

  /**
   * Constructs an instance of the condition.
   *
   * @param key A {@link EdDSAPublicKey} used to create the fingerprint.
   */
  public Ed25519Sha256Condition(final EdDSAPublicKey key) {
    super(
        CryptoConditionType.ED25519_SHA256,
        COST,
        hashFingerprintContents(constructFingerprintContents(key))
    );
  }

  /**
   * <p>Constructs an instance of the condition with the given fingerprint and cost.</p>
   *
   * <p>This constructor _should_ be primarily used by CODECs, whereas developers should, in
   * general, use the {@link #Ed25519Sha256Condition(EdDSAPublicKey)} constructor, or else create an
   * actual fulfillment via {@link Ed25519Sha256Fulfillment#Ed25519Sha256Fulfillment(EdDSAPublicKey,
   * byte[])} and then generate a condition from that object.</p>
   *
   * @param fingerprint The binary representation of the fingerprint for this condition.
   */
  public Ed25519Sha256Condition(final byte[] fingerprint) {
    super(CryptoConditionType.ED25519_SHA256, COST, fingerprint);
  }

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
}
