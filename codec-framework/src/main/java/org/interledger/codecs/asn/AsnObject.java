package org.interledger.codecs.asn;


/**
 * A base for wrappers that map an ASN.1 definition to a native type
 *
 * @param <T> the native type represented by this ASN.1 object
 */
public abstract class AsnObject<T> {

  private T value;

  public AsnObject() {
  }

  public T getValue() {
    return value;
  }

  public void setValue(T value) {
    this.value = value;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    AsnGeneralizedTime other = (AsnGeneralizedTime) obj;

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
