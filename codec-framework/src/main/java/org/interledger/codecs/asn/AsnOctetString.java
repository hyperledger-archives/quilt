package org.interledger.codecs.asn;

/**
 * ASN.1 Octet String represented internally as a byte array.
 */
public class AsnOctetString extends AsnOctetStringBasedObject<byte[]> {

  public AsnOctetString(AsnSizeConstraint sizeConstraint) {
    super(sizeConstraint);
  }

  @Override
  protected byte[] decode() {
    return getBytes();
  }

  @Override
  protected void encode(byte[] value) {
    setBytes(value);
  }

}