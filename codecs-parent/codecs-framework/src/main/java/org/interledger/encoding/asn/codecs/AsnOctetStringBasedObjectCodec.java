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

import static java.lang.String.format;

import org.interledger.encoding.asn.framework.CodecException;

import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;

/**
 * ASN.1 object codec that uses a byte array string as an intermediary encoding.
 *
 * <p>This base class should be used for codecs where the ASN.1 object extends an ASN.1
 * octet string or integer.
 *
 * <p>Codecs that extend this object can call {@link #getBytes()} and
 * {@link #setBytes(byte[])} in their implementations of {@link #decode()} and {@link #encode(Object)} respectively.
 *
 * <p>The serializers for this object must call {@link #getBytes()} and
 * {@link #setBytes(byte[])} when reading from a stream. The values passed and returned must not contain any length
 * prefixes or tags.
 */
public abstract class AsnOctetStringBasedObjectCodec<T> extends AsnPrimitiveCodec<T> {

  protected static final byte[] EMPTY_BYTES = new byte[0];

  private byte[] bytes;

  public AsnOctetStringBasedObjectCodec(AsnSizeConstraint sizeConstraintInOctets) {
    super(sizeConstraintInOctets);
  }

  public AsnOctetStringBasedObjectCodec(int fixedSizeConstraint) {
    super(fixedSizeConstraint);
  }

  public AsnOctetStringBasedObjectCodec(int minSize, int maxSize) {
    super(minSize, maxSize);
  }


  /**
   * Get the internal byte array representation of this object.
   *
   * <p>Implementations should call this method within their {@link #decode()} implementation to get
   * the bytes that were read by the serializer.
   *
   * @return the internal byte array representation of this object.
   */
  public final byte[] getBytes() {
    return bytes == null ? EMPTY_BYTES : bytes;
  }

  /**
   * Set the internal byte array.
   *
   * <p>Implementations should call this method within their {@link #encode(Object)} implementation
   * to set the bytes that should be written by the serializer.
   *
   * <p>The provided String will be validated against the codec's size constraint a
   * {@link CodecException} will be thrown if it is invalid
   *
   * @param bytes the {@link String} representing the value of this object
   *
   * @throws CodecException if the provided charString fails validation
   */
  public final void setBytes(byte[] bytes) {
    Objects.requireNonNull(bytes);
    validateSize(bytes);
    this.bytes = bytes;
    this.onValueChangedEvent();
  }

  private void validateSize(byte[] bytes) {
    if (getSizeConstraint().isUnconstrained()) {
      return;
    }

    if (getSizeConstraint().isFixedSize()) {
      if (bytes.length != getSizeConstraint().getMax()) {
        throw new CodecException(format("Invalid octet string length. Expected %s, got %s",
            getSizeConstraint().getMax(), bytes.length));
      }
    } else {
      if (bytes.length < getSizeConstraint().getMin()) {
        throw new CodecException(format("Invalid octet string length. Expected > %s, got %s",
            getSizeConstraint().getMin(), bytes.length));
      }
      if (bytes.length > getSizeConstraint().getMax()) {
        throw new CodecException(format("Invalid octet string length. Expected < %s, got %s",
            getSizeConstraint().getMax(), bytes.length));
      }
    }

  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    if (!super.equals(obj)) {
      return false;
    }

    AsnOctetStringBasedObjectCodec<?> that = (AsnOctetStringBasedObjectCodec<?>) obj;

    return Arrays.equals(bytes, that.bytes);
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + Arrays.hashCode(bytes);
    return result;
  }

  @Override
  public String toString() {
    return "AsnOctetStringBasedObjectCodec{"
        + "bytes="
        + Base64.getEncoder().encodeToString(bytes)
        + '}';
  }
}
