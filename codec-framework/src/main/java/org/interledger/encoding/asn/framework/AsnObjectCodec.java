package org.interledger.encoding.asn.framework;

/**
 * A codec to encode/decode an object of type {@link T} into an ASN.1 form ready for serialization.
 *
 * @param <T> the type of object that is encoded/decoded by this codec.
 */
public interface AsnObjectCodec<T> {

  /**
   * Decode and return the value read into the codec during serialization.
   *
   * @return the decoded object
   */
  T decode();

  /**
   * Encode the provided value into the codec to be written during serialization.
   *
   * @param value the value to encode
   */
  void encode(T value);

}
