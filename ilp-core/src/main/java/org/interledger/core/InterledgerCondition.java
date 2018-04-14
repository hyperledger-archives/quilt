package org.interledger.core;

import java.util.Arrays;
import java.util.Objects;

/**
 * Represents an Interledger Condition (as present in an ILP Prepare packet).
 *
 * <p>This is a wrapper around a 32 byte SHA-256 hash digest providing an immutable implementation.
 *
 * @see InterledgerFulfillment
 */
public interface InterledgerCondition extends Comparable<InterledgerCondition> {

  /**
   * Create a new immutable InterledgerCondition from the provided 32-bytes.
   *
   * <p>This method is predominantly used during deserialization. To generate a condition based on an fulfillment
   * use {@link InterledgerFulfillment#getCondition()}.
   *
   * @param bytes 32-byte SHA-256 hash digest
   * @return the condition containing these bytes
   */
  static InterledgerCondition from(byte[] bytes) {
    Objects.requireNonNull(bytes, "bytes cannot be null");
    return new ImmutableInterledgerCondition(bytes);
  }

  /**
   * Create a new immutable InterledgerCondition from the InterledgerCondition
   *
   * <p>Optimized to perform fewer array copies than <code>InterledgerCondtion.from(condition.getBytes()</code>.
   *
   * @param condition An existing condition
   * @return a new immutable copy of the input condition
   */
  static InterledgerCondition from(InterledgerCondition condition) {
    Objects.requireNonNull(condition, "condition cannot be null");
    //Only call getBytes() if we have to (avoid array copy)
    byte[] otherBytes = (condition instanceof ImmutableInterledgerCondition)
        ? ((ImmutableInterledgerCondition) condition).bytes
        : condition.getBytes();
    return new ImmutableInterledgerCondition(otherBytes);
  }

  /**
   * Get the internal bytes from the condition.
   *
   * <p>Implementations should return a safe copy from the data to preserve the immutability from the condition.
   *
   * @return the 32 byte condition
   */
  byte[] getBytes();

  /**
   * An immutable implementation from InterledgerCondition optimized for efficient operations that only create copies
   * from the internal data as required.
   */
  class ImmutableInterledgerCondition implements InterledgerCondition {

    //Package private so it is accessible from InterledgerFulfillment to avoid copying if possible
    final byte[] bytes = new byte[32];

    private ImmutableInterledgerCondition(byte[] bytes) {
      if (bytes.length != 32) {
        throw new IllegalArgumentException("InterledgerCondition must be exactly 32 bytes.");
      }
      System.arraycopy(bytes, 0, this.bytes, 0, 32);
    }

    @Override
    public boolean equals(Object other) {
      if (other == this) {
        return true;
      }

      if (other instanceof InterledgerCondition) {

        //Only call getBytes() if we have to (avoid array copy)
        byte[] otherBytes = (other instanceof ImmutableInterledgerCondition)
            ? ((ImmutableInterledgerCondition) other).bytes
            : ((InterledgerCondition) other).getBytes();

        return Arrays.equals(bytes, otherBytes);
      }

      return false;

    }

    @Override
    public byte[] getBytes() {
      return Arrays.copyOf(this.bytes, 32);
    }

    @Override
    public int compareTo(InterledgerCondition other) {

      if (other == null) {
        return 1;
      }

      if (other == this) {
        return 0;
      }

      //Only call getBytes() if we have to (avoid array copy)
      byte[] otherBytes = (other instanceof ImmutableInterledgerCondition)
          ? ((ImmutableInterledgerCondition) other).bytes
          : other.getBytes();

      if (otherBytes == this.bytes) {
        return 0;
      }

      for (int i = 0; i < 32; i++) {
        if (this.bytes[i] != otherBytes[i]) {
          return this.bytes[i] - otherBytes[i];
        }
      }

      return 0;
    }

  }

}
