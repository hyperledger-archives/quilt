package org.interledger.codecs.asn;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * ASN.1 sequence represented internally as a {@link List}.
 */
public abstract class AsnSequence<T> extends AsnObject<T> {

  private final List<AsnObject> sequence;

  public AsnSequence(final AsnObject... elements) {
    this.sequence = Arrays.asList(elements);
  }

  @Override
  public final T getValue() {
    return decode();
  }

  @Override
  public final void setValue(T value) {
    encode(value);
  }

  public int size() {
    return sequence.size();
  }

  protected abstract T decode();

  protected abstract void encode(T value);

  protected AsnSequence setElementAt(int index, AsnObject object) {
    Objects.requireNonNull(object);
    sequence.set(index, object);
    return this;
  }

  public final <U extends AsnObject<V>, V> V getValueAt(int index) {
    return ((U) getElementAt(index)).getValue();
  }

  public final <U extends AsnObject<V>, V> AsnSequence<T> setValueAt(int index, V value) {
    ((U) getElementAt(index)).setValue(value);
    return this;
  }

  public AsnObject getElementAt(int index) {
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

    AsnSequence that = (AsnSequence) obj;

    return sequence.equals(that.sequence);
  }

  @Override
  public int hashCode() {
    return sequence.hashCode();
  }

  @Override
  public String toString() {
    return "AsnSequence{"
        + "sequence="
        + Arrays.toString(sequence.toArray())
        + '}';
  }
}