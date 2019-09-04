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
 * A codec to encode/decode an object of type {@link T} into an ASN.1 form ready for serialization.
 *
 * @param <T> the type of object that is encoded/decoded by this codec.
 */
public interface AsnObjectCodec<T> {

  /**
   * Decode and return the value read into the codec during serialization.
   *
   * @return the decoded object
   */
  T decode();

  /**
   * Encode the provided value into the codec to be written during serialization.
   *
   * @param value the value to encode
   */
  void encode(T value);

}
