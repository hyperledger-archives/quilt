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

import org.interledger.encoding.asn.framework.AsnObjectCodec;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * ASN.1 object codec that represents a {@link List} of other ASN.1 codecs to encode/decode an
 * ASN.1 SEQUENCE
 *
 * <p>This base class should be used for codecs where the ASN.1 object is a subclass of an ASN.1
 * SEQUENCE.
 *
 * <p>Codecs that extend this object can call {@link #getValueAt(int)} and
 * {@link #setValueAt(int, Object)} in their implementations of {@link #decode()} and
 * {@link #encode(Object)} respectively.
 *
 * <p>Codecs that extend this object can also call {@link #getCodecAt(int)} and
 * {@link #setCodecAt(int, AsnObjectCodec)} to access the internal codecs for each field.
 *
 */
public abstract class AsnSequenceCodec<T> extends AsnObjectCodecBase<T> {

  private final List<AsnObjectCodec> sequence;

  /**
   * Default constructor.
   *
   * @param fields a sequence of {@link AsnObjectCodec} instances.
   */
  public AsnSequenceCodec(final AsnObjectCodec... fields) {
    this.sequence = Arrays.asList(fields);
  }

  /**
   * Get the size of the sequence.
   *
   * @return the size of the sequence
   */
  public int size() {
    return sequence.size();
  }

  /**
   * Set the codec to use for encoding/decoding the field at the given index.
   *
   * <p>Implementations can use this function to dynamically set the codecs for a field that has not
   * yet been encoded (usually as a result of the value encoded into another field).

   * @see AsnObjectCodecBase#setValueChangedEventListener(Consumer)
   *
   * @param index Index of the field in the sequence.
   * @param codec Codec to use of encoding/decoding the field.
   */
  protected void setCodecAt(int index, AsnObjectCodec codec) {
    Objects.requireNonNull(codec);
    sequence.set(index, codec);
  }

  /**
   * Get and decoded the value from the codec at the given index.
   *
   * @param index the index of the field that is being decoded
   * @param <U> the type of the codec at the given index
   * @param <V> the type of the value that will be decoded
   * @return the decoded filed value
   */
  public final <U extends AsnObjectCodecBase<V>, V> V getValueAt(int index) {
    return ((U) getCodecAt(index)).decode();
  }

  /**
   * Set and encoded a value using the codec at the given index.
   *
   * @param index the index of the field that is being encoded.
   * @param value the value of the field that is being encoded.
   * @param <U> the type of the codec at the given index.
   * @param <V> the type of the value that will be encoded.
   */
  public final <U extends AsnObjectCodecBase<V>, V> void setValueAt(int index, V value) {
    ((U) getCodecAt(index)).encode(value);
    this.onValueChangedEvent();
  }

  /**
   * Get the codec for the field at the specified index.
   *
   * @param index index of the field
   * @return the codec for the field at the specified index
   */
  public AsnObjectCodec getCodecAt(int index) {
    return sequence.get(index);
  }

  @Override
  public boolean equals(Object obj) {

    if (this == obj) {
      return true;
    }

    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    AsnSequenceCodec that = (AsnSequenceCodec) obj;

    return sequence.equals(that.sequence);
  }

  @Override
  public int hashCode() {
    return sequence.hashCode();
  }

  @Override
  public String toString() {
    return "AsnSequenceCodec{"
        + "sequence="
        + Arrays.toString(sequence.toArray())
        + '}';
  }
}
