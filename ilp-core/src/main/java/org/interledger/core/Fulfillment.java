package org.interledger.core;

import org.immutables.value.Value;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Objects;

/**
 * The fulfillment of a {@link Condition}.
 *
 * <p>The standard for Interledger payments is for the fulfillment to be the pre-image of a SHA-256
 * hash (the condition).
 *
 * <p>The fulfillment (pre-image) must be exactly 32 bytes.
 */
@Value.Immutable
public interface Fulfillment {

  /**
   * Get the default builder.
   *
   * @return a {@link ImmutableFulfillment#builder()} instance.
   */
  static ImmutableFulfillment.Builder builder() {
    return ImmutableFulfillment.builder();
  }

  /**
   * Build a new Fulfillment using the provided preimage.
   *
   * @param preimage The preimage representing the fulfillment
   *
   * @return a  {@link Fulfillment} instance
   */
  static Fulfillment of(final byte[] preimage) {
    Objects.requireNonNull(preimage, "preimage must not be null!");

    try {
      // MessageDigest is not threadsafe, but is cheap to construct...
      final MessageDigest digest = MessageDigest.getInstance("SHA-256");
      final byte[] hash = digest.digest(preimage);
      return Fulfillment.builder()
          .preimage(Arrays.copyOf(preimage, 32))
          .condition(Condition.of(hash))
          .build();
    } catch (NoSuchAlgorithmException e) {
      throw new InterledgerRuntimeException(e);
    }
  }

  /**
   * Get the raw pre-image (safe copy).
   *
   * @return 32 byte octet string
   */
  byte[] getPreimage();

  /**
   * Get the {@link Condition} that is fulfilled by this Fulfillment.
   *
   * @return a condition representing the SHA-256 hash of this preimage.
   */
  Condition getCondition();

  /**
   * Validate a given condition against this fulfillment.
   *
   * @param condition The condition to compare against.
   *
   * @return true if this fulfillment fulfills the given condition.
   */
  @Value.Derived
  default boolean validate(final Condition condition) {
    Objects.requireNonNull(condition, "condition must not be null!");
    return this.getCondition().equals(condition);
  }

  /**
   * Immutables method to enforce preconditions on the data for a fulfillment.
   */
  @Value.Check
  default void check() {
    if (getPreimage().length != 32) {
      throw new IllegalArgumentException("Preimage must be 32 bytes.");
    }
  }
}