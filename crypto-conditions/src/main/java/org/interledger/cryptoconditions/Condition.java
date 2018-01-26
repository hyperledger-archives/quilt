package org.interledger.cryptoconditions;

import org.immutables.value.Value;

/**
 * An implementation of a crypto-conditions Condition.
 *
 * @see "https://datatracker.ietf.org/doc/draft-thomas-crypto-conditions/"
 */
public interface Condition extends Comparable<Condition> {

  /**
   * The type identifier representing the condition type.
   *
   * @return the type of this condition
   */
  CryptoConditionType getType();

  /**
   * <p>A fingerprint is a binary (aka "octet") string uniquely representing the condition with
   * respect to other conditions of the same type. Implementations which index conditions MUST use
   * the  entire string or binary encoded condition as the key - not just the fingerprint - as
   * different conditions of different rsa may have the same fingerprint. The length and contents of
   * the fingerprint are defined by the condition type. The fingerprint is a cryptographically
   * secure hash of the data which defines the condition, such as a public key.</p>
   *
   * <p>WARNING: This method MUST perform a safe copy of the internal data to protect immutability.
   * Callers should use {@link #getFingerprintBase64Url()} instead if possible.</p>
   *
   * @return A read-only byte array that contains the unique fingerprint of this condition.
   */
  @Deprecated
  byte[] getFingerprint();

  /**
   * <p>A fingerprint is a binary (aka "octet") string uniquely representing the condition with
   * respect to other conditions of the same type. Implementations which index conditions MUST use
   * the entire string or binary encoded condition as the key - not just the fingerprint - as
   * different conditions of different rsa may have the same fingerprint. The length and contents of
   * the fingerprint are defined by the condition type. The fingerprint is a cryptographically
   * secure hash of the data which defines the condition, such as a public key.</p>
   *
   * @return A Base64Url-encoded {@link String} that contains the unique fingerprint of this
   *     condition.
   */
  String getFingerprintBase64Url();

  /**
   * The estimated "cost" of processing a fulfillment of this condition. For details of how to
   * calculate this number see the Crypto-conditions specification.
   *
   * @return the cost of validating the fulfillment of this condition
   */
  long getCost();

  /**
   * An abstract implementation of {@link Condition} that provides setup for generated
   * <tt>Immutables</tt> implementations of this library.
   *
   * @see "http://immutables.github.io/"
   */
  abstract class AbstractCondition implements Condition {

    @Value.Check
    protected void check() {
      if (this.getFingerprint().length != 32) {
        throw new IllegalArgumentException("Fingerprint must be 32 bytes.");
      }

      if (this.getCost() < 0) {
        throw new IllegalArgumentException("Cost must be positive!");
      }
    }
  }
}