package org.interledger.encoding.asn.codecs;

import org.interledger.encoding.asn.framework.AsnObjectCodec;

import java.util.function.Consumer;

/**
 * A base for ASN.1 codecs.
 *
 * @param <T> the type that is encoded/decoded using this codec
 */
public abstract class AsnObjectCodecBase<T> implements AsnObjectCodec<T> {

  private Consumer<T> encodeEventListener;

  /**
   * Signal that a value has been encoded into this codec.
   *
   * @param value The value encoded into the codec
   */
  protected void onEncodeEvent(T value) {
    if (hasEncodeEventListener()) {
      encodeEventListener.accept(value);
    }
  }

  /**
   * Indicates if this object has a listener for value changes.
   *
   * @return true if there is a listener
   */
  public final boolean hasEncodeEventListener() {
    return encodeEventListener != null;
  }

  /**
   * Set a listener that will be notified when a value is encoded into this codec.
   *
   * @param listener The listener that accepts an instance of {@link T}, the new value.
   * @throws IllegalStateException if a there is already a listener.
   */
  public final void setEncodeEventListener(Consumer<T> listener) {
    if (hasEncodeEventListener()) {
      throw new IllegalStateException("Can't overwrite an existing listener.");
    }
    this.encodeEventListener = listener;
  }

  /**
   * Remove the value change listener (if one is attached).
   */
  public final void removeEncodeEventListener() {
    encodeEventListener = null;
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
