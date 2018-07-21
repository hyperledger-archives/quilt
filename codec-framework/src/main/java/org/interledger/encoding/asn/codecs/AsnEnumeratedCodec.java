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

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * ASN.1 object codec that containss an ASN.1 codec to encode/decode an ASN.1 ENUMERATED type.
 *
 * <p>This base class should be used for codecs where the ASN.1 object is a subclass of an ASN.1
 * ENUMERATED.
 *
 * <p>Codecs that extend this object can call {@link #getValue()} and
 * {@link #setValue(Collection)} in their implementations of {@link #decode()} and {@link
 * #encode(Object)} respectively.
 */
public class AsnEnumeratedCodec<T> extends AsnObjectCodecBase<Collection<T>> {

  private final AsnUintCodec varUintCodec;
  // A single codec for each value in an enumerated list.
  private final AsnObjectCodec<T> enumerateTypeCodec;

  private Collection<T> values;

  /**
   * Default constructor.
   *
   * @param fieldCodec An {@link AsnObjectCodec} instance that will be used to encoded each object
   *                   in the Collection associated with this Codec.
   */
  public AsnEnumeratedCodec(final AsnObjectCodec<T> fieldCodec) {
    this.enumerateTypeCodec = Objects.requireNonNull(fieldCodec);
    this.varUintCodec = new AsnUintCodec();
  }

  /**
   * Decode and return the value read into the codec during serialization.
   *
   * @return the decoded object
   */
  @Override
  public Collection<T> decode() {
    final List<T> enumeratedItems = new ArrayList();
    final BigInteger numElements = varUintCodec.decode();

    for (BigInteger bi = BigInteger.ZERO; // i == 0
        bi.compareTo(numElements) < 0; // i < numElements
        bi = bi.add(BigInteger.ONE)) { // i++
      enumeratedItems.add(enumerateTypeCodec.decode());
    }

    return enumeratedItems;
  }

  /**
   * Encode the provided value into the codec to be written during serialization.
   *
   * @param values the value to encode
   */
  @Override
  public void encode(final Collection<T> values) {
    Objects.requireNonNull(values);
    //final Collection<T> values = getValue();
    this.setValue(values);

    varUintCodec.encode(BigInteger.valueOf(values.size()));

    Objects.requireNonNull(values).stream()
        .forEach(enumerateTypeCodec::encode);
  }

  /**
   * Get and decoded the value from the codec.
   *
   * @return the decoded filed value
   */
  public final Collection<T> getValue() {
    return this.values;
  }

  /**
   * Set and encoded a value using the codec at the given index.
   *
   * @param values the value of the field that is being encoded.
   */
  public final void setValue(final Collection<T> values) {
    this.values = Objects.requireNonNull(values);
    this.onValueChangedEvent();
  }

  /**
   * Get the codec for this enumerated type.
   *
   * @return the codec for the field at the specified index
   */
  public AsnObjectCodec getCodec() {
    return this.enumerateTypeCodec;
  }

  @Override
  public boolean equals(Object obj) {

    if (this == obj) {
      return true;
    }

    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    AsnEnumeratedCodec that = (AsnEnumeratedCodec) obj;

    return enumerateTypeCodec.equals(that.enumerateTypeCodec);
  }

  @Override
  public int hashCode() {
    return enumerateTypeCodec.hashCode();
  }

  @Override
  public String toString() {
    return "AsnSequenceCodec{"
        + "enumerateTypeCodec="
        + enumerateTypeCodec
        + '}';
  }


}
