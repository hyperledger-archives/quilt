package org.interledger.encoding.asn.framework;

/**
 * A functional interface for functions that produce new instances of {@link AsnObjectCodec}.
 *
 * @param <T> the type of object that is encoded/decoded by the codec supplied.
 */
@FunctionalInterface
public interface AsnObjectCodecSupplier<T> {

  /**
   * Get a new instance of {@link T}.
   *
   * @return a new instance of {@link T}.
   */
  AsnObjectCodec<T> get();
}