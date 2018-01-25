package org.interledger.encoding.asn.codecs;

/**
 * An ASN.1 codec for octet string objects.
 */
public class AsnOctetStringCodec extends AsnOctetStringBasedObjectCodec<byte[]> {

  public AsnOctetStringCodec(AsnSizeConstraint sizeConstraint) {
    super(sizeConstraint);
  }

  public AsnOctetStringCodec(int fixedSizeConstraint) {
    super(fixedSizeConstraint);
  }

  public AsnOctetStringCodec(int minSize, int maxSize) {
    super(minSize, maxSize);
  }

  @Override
  public byte[] decode() {
    return getBytes();
  }

  @Override
  public void encode(byte[] value) {
    setBytes(value);
  }

}