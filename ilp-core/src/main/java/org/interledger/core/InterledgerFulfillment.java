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
 * <p>Represents an Interledger Fulfillment that can be used in an {@link
 * InterledgerFulfillPacket}.</p>
 *
 * <p>This is a wrapper around the 32 byte pre-image of a SHA-256 hash digest.</p>
 *
 * @see InterledgerCondition
 */
public interface InterledgerFulfillment extends Comparable<InterledgerFulfillment> {

  /**
   * Create a new immutable InterledgerFulfillment from the provided 32 pre-image.
   *
   * @param preimage 32-byte pre-image
   *
   * @return the fulfillment containing the supplied pre-image.
   */
  static InterledgerFulfillment from(byte[] preimage) {
    Objects.requireNonNull(preimage, "preimage cannot be null");
    return new ImmutableInterledgerFulfillment(preimage);
  }

  /**
   * <p>Get the internal pre-image from the fulfillment.</p>
   *
   * <p>Implementations should return a safe copy from the data to preserve the immutability from
   * the fulfillment.</p>
   *
   * @return the 32 byte preimage
   */
  byte[] getPreimage();

  /**
   * <p>Get the condition that is valid for this fulfillment.</p>
   *
   * <p>Implementations MUST ensure that <code>f.validateCondition(f.getCondition())</code> always
   * returns <code>true</code>.</p>
   *
   * @return the condition for this fulfillment.
   */
  InterledgerCondition getCondition();

  /**
   * <p>Check that the provided condition is valid for this fulfillment.</p>
   *
   * <p>A valid condition is the 32 byte SHA-256 hash digest from the 32 byte opreimage represented
   * by this fulfillment.</p>
   *
   * @param condition an InterledgerCondition
   *
   * @return tru if this condition is valid for this fulfillment.
   */
  boolean validateCondition(InterledgerCondition condition);

  /**
   * An immutable implementation from InterledgerFulfillment optimized for efficient operations that
   * only create copies from the internal data as required and only performs late generation from
   * the hash when required.
   */
  class ImmutableInterledgerFulfillment implements InterledgerFulfillment {

    private final byte[] preimageBytes = new byte[32];
    private final InterledgerCondition condition;

    protected ImmutableInterledgerFulfillment(byte[] preimageBytes) {
      if (preimageBytes.length != 32) {
        throw new IllegalArgumentException(
            "Interledger preimages must be exactly 32 preimageBytes.");
      }
      System.arraycopy(preimageBytes, 0, this.preimageBytes, 0, 32);

      try {
        // MessageDigest is not threadsafe, but is cheap to construct...
        final MessageDigest digest = MessageDigest.getInstance("SHA-256");
        final byte[] hash = digest.digest(preimageBytes);
        this.condition = InterledgerCondition.from(hash);
      } catch (NoSuchAlgorithmException e) {
        //This should never happen as all JVMs ship with a SHA-256 digest implementation
        throw new InterledgerRuntimeException(
            "Unable to get SHA-256 message digest instance.", e
        );
      }
    }

    @Override
    public InterledgerCondition getCondition() {
      return this.condition;
    }

    @Override
    public byte[] getPreimage() {
      return Arrays.copyOf(preimageBytes, 32);
    }

    @Override
    public boolean validateCondition(final InterledgerCondition condition) {
      Objects.requireNonNull(condition, "condition must not be null!");
      return this.getCondition().equals(condition);
    }

    @Override
    public boolean equals(Object other) {
      if (other == this) {
        return true;
      }

      if (other instanceof InterledgerFulfillment) {

        //Only call getHash() if we have to (avoid array copy)
        byte[] otherBytes = (other instanceof ImmutableInterledgerFulfillment)
            ? ((ImmutableInterledgerFulfillment) other).preimageBytes
            : ((InterledgerFulfillment) other).getPreimage();

        return Arrays.equals(preimageBytes, otherBytes);
      }

      return false;

    }

    @Override
    public int compareTo(final InterledgerFulfillment other) {

      if (other == null) {
        return 1;
      }

      if (other == this) {
        return 0;
      }

      //Only call getHash() if we have to (avoid array copy)
      byte[] otherBytes = (other instanceof ImmutableInterledgerFulfillment)
          ? ((ImmutableInterledgerFulfillment) other).preimageBytes
          : other.getPreimage();

      if (otherBytes == this.preimageBytes) {
        return 0;
      }

      for (int i = 0; i < 32; i++) {
        if (this.preimageBytes[i] != otherBytes[i]) {
          return this.preimageBytes[i] - otherBytes[i];
        }
      }

      return 0;
    }

  }

}
