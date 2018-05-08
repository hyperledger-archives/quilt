package org.interledger.encoding.asn.codecs;

import org.interledger.encoding.asn.framework.AsnObjectCodec;

import java.util.function.Consumer;

/**
 * A base for ASN.1 codecs.
 *
 * @param <T> the type that is encoded/decoded using this codec
 */
public abstract class AsnObjectCodecBase<T> implements AsnObjectCodec<T> {

  private Consumer<AsnObjectCodecBase<T>> valueChangedEventListener;

  /**
   * Signal that the internal value of has been changed in this codec.
   */
  protected void onValueChangedEvent() {
    if (hasValueChangedEventListener()) {
      valueChangedEventListener.accept(this);
    }
  }

  /**
   * Indicates if this object has a listener for value changes.
   *
   * @return true if there is a listener
   */
  public final boolean hasValueChangedEventListener() {
    return valueChangedEventListener != null;
  }

  /**
   * Set a listener that will be notified when a value is encoded into this codec.
   *
   * @param listener The listener that accepts an instance of {@link T}, the new value.
   * @throws IllegalStateException if a there is already a listener.
   */
  public final void setValueChangedEventListener(Consumer<AsnObjectCodecBase<T>> listener) {
    if (hasValueChangedEventListener()) {
      throw new IllegalStateException("Can't overwrite an existing listener.");
    }
    this.valueChangedEventListener = listener;
  }

  /**
   * Remove the value change listener (if one is attached).
   */
  public final void removeEncodeEventListener() {
    valueChangedEventListener = null;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    AsnObjectCodecBase other = (AsnObjectCodecBase) obj;

    return decode().equals(other.decode());
  }

  @Override
  public int hashCode() {
    return decode().hashCode();
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("AsnObjectCodec{");
    sb.append("value=").append(decode());
    sb.append('}');
    return sb.toString();
  }

}
