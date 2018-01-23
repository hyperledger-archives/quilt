package org.interledger.codecs.asn;

import static java.lang.String.format;

import org.interledger.codecs.framework.CodecException;

import java.util.Arrays;
import java.util.Objects;

/**
 * ASN.1 object that extends an Octet String.
 */
public abstract class AsnOctetStringBasedObject<T> extends AsnPrimitive<T> {

  private byte[] bytes;

  public AsnOctetStringBasedObject(AsnSizeConstraint sizeConstraintInOctets) {
    super(sizeConstraintInOctets);
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

  public final byte[] getBytes() {
    return bytes;
  }

  public final void setBytes(byte[] bytes) {
    Objects.requireNonNull(bytes);
    validateSize(bytes);
    this.bytes = bytes;
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

  protected abstract T decode();

  protected abstract void encode(T value);

  @Override
  public boolean equals(Object obj) {

    if (this == obj) {
      return true;
    }

    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    AsnOctetStringBasedObject that = (AsnOctetStringBasedObject) obj;

    return Arrays.equals(bytes, that.bytes);
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(bytes);
  }

  @Override
  public String toString() {
    return "AsnOctetStringBasedObject{"
        + "bytes="
        + Arrays.toString(bytes)
        + '}';
  }
}