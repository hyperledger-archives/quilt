package org.interledger.core;

/*-
 * ========================LICENSE_START=================================
 * Interledger Core
 * %%
 * Copyright (C) 2017 - 2018 Interledger
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
