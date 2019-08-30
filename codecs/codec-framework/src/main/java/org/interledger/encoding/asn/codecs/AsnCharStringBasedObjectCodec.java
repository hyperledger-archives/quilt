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

import java.nio.charset.Charset;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * ASN.1 object codec that uses a character string as an intermediary encoding.
 *
 * <p>This base class should be used for codecs where the ASN.1 object is a subclass of an ASN.1
 * string type like IA5String or UTF8String (although the specific subclasses
 * {@link AsnIA5StringBasedObjectCodec} and {@link AsnUtf8StringBasedObjectCodec} should be used in
 * the case of those two types).
 *
 * <p>Codecs that extend this object can call {@link #getCharString()} and
 * {@link #setCharString(String)} in their implementations of {@link #decode()} and
 * {@link #encode(Object)} respectively.
 *
 * <p>The serializers for this object must call {@link #getCharString()} and
 * {@link #setCharString(String)} when reading from a stream. The values passed and returned must
 * not contain any length prefixes or tags.
 *
 */
public abstract class AsnCharStringBasedObjectCodec<T> extends AsnPrimitiveCodec<T> {

  private final Charset characterSet;
  private Predicate<String> validator;
  private String charString;

  /**
   * Default constructor.
   *
   * @param sizeConstraint The size constraint for this ASN.1 object.
   * @param characterSet   The character set to use when encoding and decoding this string from
   *                       the byte stream.
   */
  public AsnCharStringBasedObjectCodec(AsnSizeConstraint sizeConstraint, Charset characterSet) {
    super(sizeConstraint);
    this.characterSet = characterSet;
  }


  public AsnCharStringBasedObjectCodec(int fixedSizeConstraint, Charset characterSet) {
    this(new AsnSizeConstraint(fixedSizeConstraint), characterSet);
  }

  public AsnCharStringBasedObjectCodec(int minSize, int maxSize, Charset characterSet) {
    this(new AsnSizeConstraint(minSize, maxSize), characterSet);
  }

  public Charset getCharacterSet() {
    return characterSet;
  }

  /**
   * Get the internal character string representation of this object.
   *
   * <p>Implementations should call this method within their {@link #decode()} implementation to get
   * the String that was read by the serializer.
   *
   * @return the internal character string representation of this object.
   */
  public final String getCharString() {
    return charString;
  }

  /**
   * Set the internal character string.
   *
   * <p>Implementations should call this method within their {@link #encode(Object)} implementation
   * to set String that should be written by the serializer.
   *
   * <p>The provided String will be validated both against the codec size constraint and the
   * {@link Predicate} set in {@link #setValidator(Predicate)} (if any). If the String
   * fails validation a {@link CodecException} will be thrown.
   *
   * @param charString the {@link String} representing the value of this object
   * @throws CodecException if the provided charString fails validation
   */
  public final void setCharString(String charString) {
    Objects.requireNonNull(charString);
    validateSize(charString);
    validate(charString);
    this.charString = charString;
    this.onValueChangedEvent();
  }

  /**
   * Set a validator for the String representation of the object
   *
   * <p>When (@link {@link #setCharString(String)} is called the String will be validated both
   * against the codecs size constraint and this {@link Predicate}.
   *
   * @param validator a {@link Predicate} that should return false if the String is invalid.
   */
  protected final void setValidator(Predicate<String> validator) {
    this.validator = validator;
  }

  private void validate(String value) {
    if (validator != null) {
      if (!validator.test(value)) {
        throw new CodecException(format("Invalid format: %s", value));
      }
    }
  }

  private void validateSize(String charString) {
    if (getSizeConstraint().isUnconstrained()) {
      return;
    }

    if (getSizeConstraint().isFixedSize()) {
      if (charString.length() != getSizeConstraint().getMax()) {
        throw new CodecException(format("Invalid character string length. Expected %s, got %s",
            getSizeConstraint().getMax(), charString.length()));
      }
    } else {
      if (charString.length() < getSizeConstraint().getMin()) {
        throw new CodecException(format("Invalid character string length. Expected > %s, got %s",
            getSizeConstraint().getMin(), charString.length()));
      }
      if (charString.length() > getSizeConstraint().getMax()) {
        throw new CodecException(format("Invalid character string length. Expected < %s, got %s",
            getSizeConstraint().getMax(), charString.length()));
      }
    }

  }

  @Override
  public String toString() {
    return "AsnCharStringBasedObjectCodec{"
        + "string="
        + charString
        + '}';
  }
}
