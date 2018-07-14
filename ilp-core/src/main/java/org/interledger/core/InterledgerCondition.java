package org.interledger.core;

/*-
 * ========================LICENSE_START=================================
 * Interledger Core
 * %%
 * Copyright (C) 2017 - 2018 Hyperledger and its contributors
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =========================LICENSE_END==================================
 */

import java.util.Arrays;
import java.util.Objects;

/**
 * Represents an Interledger Condition (as present in an ILP Prepare packet).
 * <p>
 * This is a wrapper around a 32 byte SHA-256 hash digest providing an immutable implementation.
 *
 * @see {@link InterledgerFulfillment}
 */
public interface InterledgerCondition extends Comparable<InterledgerCondition> {

  /**
   * Create a new immutable {@link InterledgerCondition} from the provided 32-bytes.
   * <p>
   * This method is typically only used during deserialization. To generate a condition based on an
   * fulfillment use {@link InterledgerFulfillment#getCondition()}.
   *
   * @param bytes A 32-byte SHA-256 hash digest
   *
   * @return The {@link InterledgerCondition} containing the supplied bytes.
   */
  static InterledgerCondition from(final byte[] bytes) {
    Objects.requireNonNull(bytes, "bytes cannot be null");
    return new ImmutableInterledgerCondition(bytes);
  }

  /**
   * Create a new immutable InterledgerCondition from the InterledgerCondition.
   * <p>
   * Note that this method is optimized to perform fewer array copies than {@link
   * InterledgerCondition#from(byte[])}.
   *
   * @param interledgerCondition An existing {@link InterledgerCondition} to copy from.
   *
   * @return a new immutable copy of the input interledgerCondition
   */
  static InterledgerCondition from(final InterledgerCondition interledgerCondition) {
    Objects.requireNonNull(interledgerCondition, "interledgerCondition cannot be null");
    //Only call getBytes() if we have to (avoid array copy)
    byte[] otherBytes = (interledgerCondition instanceof ImmutableInterledgerCondition)
        ? ((ImmutableInterledgerCondition) interledgerCondition).bytes
        : interledgerCondition.getBytes();
    return new ImmutableInterledgerCondition(otherBytes);
  }

  /**
   * Get the internal bytes from the condition.
   *
   * <p>Implementations should return a safe copy from the data to preserve the immutability from
   * the condition.
   *
   * @return the 32 byte condition
   */
  byte[] getBytes();

  /**
   * An immutable implementation from InterledgerCondition optimized for efficient operations that
   * only create copies from the internal data as required.
   */
  final class ImmutableInterledgerCondition implements InterledgerCondition {

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

    /**
     * Computes a hash code from attributes: {@code hash}.
     *
     * @return hashCode value
     */
    @Override
    public int hashCode() {
      int h = 5381;
      h += (h << 5) + Arrays.hashCode(bytes);
      return h;
    }

    /**
     * Prints the immutable value {@code Condition} with attribute values.
     *
     * @return A string representation of the value
     */
    @Override
    public String toString() {
      return "Condition{"
          + "hash=" + Arrays.toString(bytes)
          + "}";
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
