package org.interledger.cryptoconditions;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;

/**
 * Abstract base class for the *-SHA-256 condition rsa.
 */
public abstract class Sha256Condition extends ConditionBase {

  private final byte[] fingerprint;
  private final String fingerprintBase64Url;

  /**
   * Constructor that accepts a fingerprint and a cost number.
   *
   * @param type        A {@link CryptoConditionType} that represents the type of this condition.
   * @param cost        A {@link long} representing the anticipated cost of this condition,
   *                    calculated per the rules of the crypto-conditions specification.
   * @param fingerprint The binary representation of the fingerprint for this condition.
   */
  protected Sha256Condition(
      final CryptoConditionType type, final long cost, final byte[] fingerprint
  ) {
    super(type, cost);

    Objects.requireNonNull(fingerprint);
    if (fingerprint.length != 32) {
      throw new IllegalArgumentException("Fingerprint must be 32 bytes.");
    }

    Objects.requireNonNull(fingerprint);
    this.fingerprint = Arrays.copyOf(fingerprint, 32);
    this.fingerprintBase64Url = Base64.getUrlEncoder().withoutPadding()
        .encodeToString(this.fingerprint);
  }

  @Override
  public final byte[] getFingerprint() {
    return Arrays.copyOf(fingerprint, 32);
  }

  @Override
  public final String getFingerprintBase64Url() {
    return this.fingerprintBase64Url;
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

    Sha256Condition that = (Sha256Condition) object;

    return fingerprintBase64Url.equals(that.fingerprintBase64Url);
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + fingerprintBase64Url.hashCode();
    return result;
  }

  /**
   * Constructs the fingerprint of this condition by taking the SHA-256 digest of the contents of
   * this condition, per the crypto-conditions RFC.
   *
   * @param fingerprintContents A byte array containing the unhashed contents of this condition as
   *                            assembled per the rules of the RFC.
   *
   * @return A byte array containing the hashed fingerprint.
   */
  protected static final byte[] hashFingerprintContents(final byte[] fingerprintContents) {
    Objects.requireNonNull(fingerprintContents);
    try {
      final MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
      return messageDigest.digest(fingerprintContents);
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
  }
}
