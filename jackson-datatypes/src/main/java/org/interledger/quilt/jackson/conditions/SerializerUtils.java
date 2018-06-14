package org.interledger.quilt.jackson.conditions;

/*-
 * ========================LICENSE_START=================================
 * Interledger Jackson Datatypes
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

import org.interledger.core.Condition;
import org.interledger.core.Fulfillment;

import java.util.Base64;
import java.util.Objects;

/**
 * Utility helpers used by various portions of this library.
 */
public class SerializerUtils {

  /**
   * Helper method to encode a {@link Condition} using the supplied Base64 encoder, which might be
   * Base64 or Base64Url, with or without padding.
   *
   * @param encoder   A {@link Base64.Encoder} to encode with.
   * @param condition A {@link Condition} to encode into Base64 using the supplied encoder.
   *
   * @return The base64-encoded version of {@code condition}.
   */
  public static String encodeBase64(final Base64.Encoder encoder, final Condition condition) {
    Objects.requireNonNull(encoder);
    Objects.requireNonNull(condition);

    return encoder.encodeToString(condition.getHash());
  }

  /**
   * Helper method to encode a {@link Fulfillment} using the supplied Base64 encoder, which might be
   * Base64 or Base64Url, with or without padding.
   *
   * @param encoder     A {@link Base64.Encoder} to encode with.
   * @param fulfillment A {@link Fulfillment} to encode into Base64 using the supplied encoder.
   *
   * @return The base64-encoded version of {@code fulfillment}.
   */
  public static String encodeBase64(final Base64.Encoder encoder, final Fulfillment fulfillment) {
    Objects.requireNonNull(encoder);
    Objects.requireNonNull(fulfillment);

    return encoder.encodeToString(fulfillment.getPreimage());
  }
}
