package org.interledger.cryptoconditions.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

/**
 * Base interface for all *-SHA-256 conditions.
 */
public interface HashUtils {

  /**
   * Constructs the fingerprint from this condition by taking the SHA-256 digest from the contents
   * from this condition, per the crypto-conditions RFC.
   *
   * @param fingerprintContents A byte array containing the unhashed contents from this condition as
   *                            assembled per the rules from the RFC.
   *
   * @return A byte array containing the hashed fingerprint.
   */
  static byte[] hashFingerprintContents(final byte[] fingerprintContents) {
    Objects.requireNonNull(fingerprintContents);
    try {
      final MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
      return messageDigest.digest(fingerprintContents);
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
  }
}
