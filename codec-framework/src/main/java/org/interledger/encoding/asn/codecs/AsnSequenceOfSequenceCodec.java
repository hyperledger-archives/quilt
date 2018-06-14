package org.interledger.encoding.asn.codecs;

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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

public class AsnSequenceOfSequenceCodec<L extends List<T>, T> extends AsnObjectCodecBase<L> {

  private final Supplier<AsnSequenceCodec<T>> supplier;
  private final Supplier<L> listSupplier;
  private ArrayList<AsnSequenceCodec<T>> codecs;

  public AsnSequenceOfSequenceCodec(
      Supplier<L> listSupplier,
      Supplier<AsnSequenceCodec<T>> sequenceCodecSupplier) {
    this.supplier = sequenceCodecSupplier;
    this.listSupplier = listSupplier;
  }

  public int size() {
    Objects.requireNonNull(codecs);
    return this.codecs.size();
  }


  /**
   * Set the size of the sequence. This must be called before {@link #getCodecAt(int)}.
   *
   * @param size size of the sequence.
   */
  public void setSize(int size) {
    this.codecs = new ArrayList<>(size);
    for (int i = 0; i < size; i++) {
      this.codecs.add(i, supplier.get());
    }
  }

  public AsnSequenceCodec<T> getCodecAt(int index) {
    Objects.requireNonNull(codecs);
    return this.codecs.get(index);
  }

  /**
   * Decode and return the value read into the codec during serialization.
   *
   * @return the decoded object
   */
  @Override
  public L decode() {
    Objects.requireNonNull(codecs);
    L list = listSupplier.get();
    for (AsnSequenceCodec<T> codec : codecs) {
      list.add(codec.decode());
    }
    return list;
  }

  /**
   * Encode the provided value into the codec to be written during serialization.
   *
   * @param values the value to encode
   */
  @Override
  public void encode(L values) {
    this.codecs = new ArrayList<>(values.size());
    for (T value : values) {
      AsnSequenceCodec<T> codec = supplier.get();
      codec.encode(value);
      this.codecs.add(codec);
    }
  }

}
