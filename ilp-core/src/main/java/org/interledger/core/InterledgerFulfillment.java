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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Objects;

/**
 * Represents an Interledger Fulfillment (as present in an ILP Fulfill packet).
 *
 * <p>This is a wrapper around the 32 byte pre-image from a SHA-256 hash digest.
 *
 * @see InterledgerCondition
 */
public interface InterledgerFulfillment extends Comparable<InterledgerFulfillment> {

  /**
   * Create a new immutable InterledgerFulfillment from the provided 32-bytes.
   *
   * @param bytes 32-byte preimage
   * @return the fulfillment containing these bytes
   */
  static InterledgerFulfillment from(byte[] bytes) {
    Objects.requireNonNull(bytes, "bytes cannot be null");
    return new ImmutableInterledgerFulfillment(bytes);
  }

  /**
   * Get the internal bytes from the fulfillment.
   *
   * <p>Implementations should return a safe copy from the data to preserve the immutability from the fulfillment.
   *
   * @return the 32 byte preimage
   */
  byte[] getBytes();

  /**
   * Get the condition that is valid for this fulfillment.
   *
   * <p>Implementations MUST ensure that <code>f.validateCondition(f.getCondition())</code> always returns
   * <code>true</code>.
   *
   * @return the condition for this fulfillment.
   */
  InterledgerCondition getCondition();

  /**
   * Check that the provided condition is valid for this fulfillment.
   *
   * <p>A valid condition is the 32 byte SHA-256 hash digest from the 32 byte opreimage represented by this fulfillment.
   *
   * @param condition an InterledgerCondition
   * @return tru if this condition is valid for this fulfillment.
   */
  boolean validateCondition(InterledgerCondition condition);

  /**
   * An immutable implementation from InterledgerFulfillment optimized for efficient operations that only create copies
   * from the internal data as required and only performs late generation from the hash when required.
   */
  class ImmutableInterledgerFulfillment implements InterledgerFulfillment {

    private final byte[] bytes = new byte[32];

    private InterledgerCondition condition;
    private byte[] conditionBytes;

    protected ImmutableInterledgerFulfillment(final byte[] bytes) {
      if (bytes.length != 32) {
        throw new IllegalArgumentException("InterledgerFulfillment must be exactly 32 bytes.");
      }
      System.arraycopy(bytes, 0, this.bytes, 0, 32);
    }

    @Override
    public InterledgerCondition getCondition() {
      if (this.condition == null) {
        try {
          MessageDigest digest = MessageDigest.getInstance("SHA-256");
          this.conditionBytes = digest.digest(bytes);
          this.condition = InterledgerCondition.from(conditionBytes);
        } catch (NoSuchAlgorithmException e) {
          //This should never happen as all JVMs ship with a SHA-256 digest implementation
          throw new RuntimeException("Unable to get SHA-256 message digest instance.", e);
        }
      }
      return condition;
    }

    @Override
    public byte[] getBytes() {
      return Arrays.copyOf(bytes, 32);
    }

    @Override
    public boolean validateCondition(InterledgerCondition condition) {
      Objects.requireNonNull(condition, "condition can't be null");
      byte[] otherBytes = (condition instanceof InterledgerCondition.ImmutableInterledgerCondition)
          ? ((InterledgerCondition.ImmutableInterledgerCondition) condition).bytes
          : condition.getBytes();
      return Arrays.equals(conditionBytes, otherBytes);
    }

    @Override
    public boolean equals(Object other) {
      if (other == this) {
        return true;
      }

      if (other instanceof InterledgerFulfillment) {

        //Only call getBytes() if we have to (avoid array copy)
        byte[] otherBytes = (other instanceof ImmutableInterledgerFulfillment)
            ? ((ImmutableInterledgerFulfillment) other).bytes
            : ((InterledgerFulfillment) other).getBytes();

        return Arrays.equals(bytes, otherBytes);
      }

      return false;

    }

    @Override
    public int compareTo(InterledgerFulfillment other) {

      if (other == null) {
        return 1;
      }

      if (other == this) {
        return 0;
      }

      //Only call getBytes() if we have to (avoid array copy)
      byte[] otherBytes = (other instanceof ImmutableInterledgerFulfillment)
          ? ((ImmutableInterledgerFulfillment) other).bytes
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
