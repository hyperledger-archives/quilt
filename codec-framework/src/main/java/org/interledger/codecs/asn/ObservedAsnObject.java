package org.interledger.codecs.asn;


import java.util.function.Consumer;

/**
 * A base for wrappers that map an ASN.1 definition to a native type
 *
 * @param <T> the native type represented by this ASN.1 object
 */
public class ObservedAsnObject<T> extends AsnObject<T> {

  private AsnObject<T> innerObject;
  private Consumer<T> observer;

  public T getValue() {
    return innerObject.getValue();
  }

  public void setValue(T value) {
    innerObject.setValue(value);

  }



}
