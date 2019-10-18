package org.interledger.encoding.asn.framework;

/*-
 * ========================LICENSE_START=================================
 * Interledger Codec Framework
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

/**
 * A functional interface for functions that produce new instances of {@link AsnObjectCodec}.
 *
 * @param <T> the type of object that is encoded/decoded by the codec supplied.
 */
@FunctionalInterface
public interface AsnObjectCodecSupplier<T> {

  /**
   * Get a new instance of {@code T}.
   *
   * @return a new instance of {@code T}.
   */
  AsnObjectCodec<T> get();
}
