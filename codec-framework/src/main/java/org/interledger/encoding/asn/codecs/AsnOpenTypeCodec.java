package org.interledger.encoding.asn.codecs;

import org.interledger.encoding.asn.framework.AsnObjectCodec;

/**
 * An ASN.1 codec for UInt8 objects that decodes them into {@link Integer} values.
 */
public class AsnOpenTypeCodec<T> extends AsnObjectCodecBase<T> {

  private AsnObjectCodec<T> innerCodec;

  public AsnOpenTypeCodec(AsnObjectCodec<T> innerCodec) {
    this.innerCodec = innerCodec;
  }


  @Override
  public T decode() {
    return innerCodec.decode();
  }

  @Override
  public void encode(T value) {
    this.innerCodec.encode(value);
    this.onValueChangedEvent();
  }

  public AsnObjectCodec<T> getInnerCodec() {
    return innerCodec;
  }
}