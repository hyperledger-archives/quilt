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
 * <p>Represents an Interledger Condition (as present in an ILP Prepare packet).</p>
 *
 * <p>This is a wrapper around a 32 byte SHA-256 hash digest providing an immutable
 * implementation.</p>
 *
 * @see InterledgerFulfillment
 */
public interface InterledgerCondition extends Comparable<InterledgerCondition> {

  /**
   * <p>Create a new immutable {@link InterledgerCondition} from the provided 32-hashBytes.</p>
   *
   * <p>This method is typically only used during deserialization. To generate a condition based
   * on an fulfillment use {@link InterledgerFulfillment#getCondition()}.</p>
   *
   * @param hashBytes A 32-byte SHA-256 hash digest.
   *
   * @return The {@link InterledgerCondition} containing the supplied hashBytes.
   */
  static InterledgerCondition from(final byte[] hashBytes) {
    Objects.requireNonNull(hashBytes, "hashBytes cannot be null");
    return new ImmutableInterledgerCondition(hashBytes);
  }

  /**
   * <p>Create a new immutable InterledgerCondition from the InterledgerCondition.</p>
   *
   * <p>Note that this method is optimized to perform fewer array copies than {@link
   * InterledgerCondition#from(byte[])}.</p>
   *
   * @param interledgerCondition An existing {@link InterledgerCondition} to copy from.
   *
   * @return a new immutable copy of the input interledgerCondition
   */
  static InterledgerCondition from(final InterledgerCondition interledgerCondition) {
    Objects.requireNonNull(interledgerCondition, "interledgerCondition cannot be null");
    //Only call getHashBytes() if we have to (avoid array copy)
    byte[] otherBytes = (interledgerCondition instanceof ImmutableInterledgerCondition)
        ? ((ImmutableInterledgerCondition) interledgerCondition).hashBytes
        : interledgerCondition.getHashBytes();
    return new ImmutableInterledgerCondition(otherBytes);
  }

  /**
   * Get the internal hashBytes from the condition.
   *
   * <p>Implementations should return a safe copy from the data to preserve the immutability from
   * the condition.
   *
   * @return the 32 byte condition
   */
  byte[] getHashBytes();

  /**
   * An immutable implementation from InterledgerCondition optimized for efficient operations that
   * only create copies from the internal data as required.
   */
  final class ImmutableInterledgerCondition implements InterledgerCondition {

    //Package private so it is accessible from InterledgerFulfillment to avoid copying if possible
    final byte[] hashBytes = new byte[32];

    private ImmutableInterledgerCondition(byte[] hashBytes) {
      if (hashBytes.length != 32) {
        throw new IllegalArgumentException(
            "InterledgerCondition must be created with exactly 32 bytes.");
      }
      System.arraycopy(hashBytes, 0, this.hashBytes, 0, 32);
    }

    @Override
    public boolean equals(Object other) {
      if (other == this) {
        return true;
      }

      if (other instanceof InterledgerCondition) {

        //Only call getHashBytes() if we have to (avoid array copy)
        byte[] otherBytes = (other instanceof ImmutableInterledgerCondition)
            ? ((ImmutableInterledgerCondition) other).hashBytes
            : ((InterledgerCondition) other).getHashBytes();

        return Arrays.equals(hashBytes, otherBytes);
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
      int hashCode = 5381;
      hashCode += (hashCode << 5) + Arrays.hashCode(hashBytes);
      return hashCode;
    }

    /**
     * Prints the immutable value {@code Condition} with attribute values.
     *
     * @return A string representation of the value
     */
    @Override
    public String toString() {
      return "Condition{"
          + "hash=" + Arrays.toString(hashBytes)
          + "}";
    }

    @Override
    public byte[] getHashBytes() {
      return Arrays.copyOf(this.hashBytes, 32);
    }

    @Override
    public int compareTo(InterledgerCondition other) {

      if (other == null) {
        return 1;
      }

      if (other == this) {
        return 0;
      }

      //Only call getHashBytes() if we have to (avoid array copy)
      byte[] otherBytes = (other instanceof ImmutableInterledgerCondition)
          ? ((ImmutableInterledgerCondition) other).hashBytes
          : other.getHashBytes();

      if (otherBytes == this.hashBytes) {
        return 0;
      }

      for (int i = 0; i < 32; i++) {
        if (this.hashBytes[i] != otherBytes[i]) {
          return this.hashBytes[i] - otherBytes[i];
        }
      }

      return 0;
    }

  }

}
