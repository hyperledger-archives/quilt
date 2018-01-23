package org.interledger.codecs.asn;

@FunctionalInterface
public interface AsnObjectSupplier<T> {

  AsnObject<T> get();
}