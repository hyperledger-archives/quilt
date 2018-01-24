package org.interledger.codecs.asn;


import java.util.function.Consumer;

/**
 * A base for wrappers that map an ASN.1 definition to a native type
 *
 * @param <T> the native type represented by this ASN.1 object
 */
public abstract class AsnObject<T> {

  private T value;
  private Consumer<T> valueChangeListener;

  public T getValue() {
    return value;
  }

  public void setValue(T value) {
    this.value = value;

    if (hasValueChangeListener()) {
      valueChangeListener.accept(value);
    }

  }

  public final boolean hasValueChangeListener() {
    return valueChangeListener != null;
  }

  public final void setValueChangeListener(Consumer<T> listener) {
    if (hasValueChangeListener()) {
      throw new IllegalStateException("Can't overwrite an existing listener.");
    }
    this.valueChangeListener = listener;
  }

  public final void removeValueChangeListener() {
    valueChangeListener = null;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    AsnObject other = (AsnObject) obj;

    return getValue().equals(other.getValue());
  }

  @Override
  public int hashCode() {
    return getValue().hashCode();
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("AsnObject{");
    sb.append("value=").append(value);
    sb.append('}');
    return sb.toString();
  }

}
