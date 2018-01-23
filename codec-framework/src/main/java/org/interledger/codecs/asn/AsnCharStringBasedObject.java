package org.interledger.codecs.asn;

import static java.lang.String.format;

import org.interledger.codecs.framework.CodecException;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Objects;

/**
 * ASN.1 object that extends an Octet String.
 */
public abstract class AsnCharStringBasedObject<T> extends AsnPrimitive<T> {

  private String charString;

  private final Charset characterSet;

  public AsnCharStringBasedObject(AsnSizeConstraint sizeConstraint, Charset characterSet) {
    super(sizeConstraint);
    this.characterSet = characterSet;
  }

  public Charset getCharacterSet() {
    return characterSet;
  }

  @Override
  public final T getValue() {
    return decode();
  }

  @Override
  public final void setValue(T value) {
    Objects.requireNonNull(value);
    encode(value);
  }

  public final String getCharString() {
    return charString;
  }

  public final void setCharString(String charString) {
    Objects.requireNonNull(charString);
    validateSize(charString);
    this.charString = charString;
  }

  protected abstract T decode();

  protected abstract void encode(T value);

  private void validateSize(String charString) {
    if(getSizeConstraint().isUnconstrained()) {
      return;
    }

    if(getSizeConstraint().isFixedSize()) {
      if(charString.length() != getSizeConstraint().getMax()) {
        throw new CodecException(format("Invalid character string length. Expected %s, got %s",
            getSizeConstraint().getMax(), charString.length()));
      }
    } else {
      if(charString.length() < getSizeConstraint().getMin()) {
        throw new CodecException(format("Invalid character string length. Expected > %s, got %s",
            getSizeConstraint().getMin(), charString.length()));
      }
      if(charString.length() > getSizeConstraint().getMax()) {
        throw new CodecException(format("Invalid character string length. Expected < %s, got %s",
            getSizeConstraint().getMax(), charString.length()));
      }
    }

  }

  @Override
  public String toString() {
    return "AsnCharStringBasedObject{"
        + "string="
        + charString
        + '}';
  }
}