package org.interledger.core;

import org.immutables.value.Value.Immutable;

/**
 * The execution condition attached to all transfers in an Interledger payment.
 *
 * <p>Interledger relies on conditional payments where each transfer that is part of a payment is
 * conditional upon the presentation of a fulfillment.
 *
 * <p>The standard for conditions is to use the SHA-256 hash of a pre-image. The pre-image is
 * therefor the fulfillment of the condition.
 *
 * @see Fulfillment
 */
@Immutable
public interface Condition {

  /**
   * Build a new Condition using the provided hash.
   *
   * @param hash A SHA-256 hash representing a Condition.
   *
   * @return a {@link Condition} instance.
   */
  static Condition of(byte[] hash) {
    return ImmutableCondition.builder().hash(hash).build();
  }

  /**
   * Get the SHA-256 hash of this condition.
   *
   * @return a {@code byte[]} of exactly 32 bytes
   */
  byte[] getHash();
}