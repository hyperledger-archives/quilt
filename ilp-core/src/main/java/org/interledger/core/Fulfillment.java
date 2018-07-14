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

import org.immutables.value.Value;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Objects;

/**
 * The fulfillment of an {@link InterledgerCondition}.
 *
 * <p>The standard for Interledger payments is for the fulfillment to be the pre-image of a SHA-256
 * hash (the condition).
 *
 * <p>The fulfillment (pre-image) must be exactly 32 hashBytes.
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
          .condition(InterledgerCondition.from(hash))
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
   * Get the {@link InterledgerCondition} that is fulfilled by this Fulfillment.
   *
   * @return a condition representing the SHA-256 hash of this preimage.
   */
  InterledgerCondition getCondition();

  /**
   * Validate a given condition against this fulfillment.
   *
   * @param condition The condition to compare against.
   *
   * @return true if this fulfillment fulfills the given condition.
   */
  @Value.Derived
  default boolean validate(final InterledgerCondition condition) {
    Objects.requireNonNull(condition, "condition must not be null!");
    return this.getCondition().equals(condition);
  }

  /**
   * Immutables method to enforce preconditions on the data for a fulfillment.
   */
  @Value.Check
  default void check() {
    if (getPreimage().length != 32) {
      throw new IllegalArgumentException("Preimage must be 32 hashBytes.");
    }
  }
}
