package org.interledger.encoding.asn.codecs;

/**
 * An ASN.1 codec for IA5String objects.
 */
public class AsnIA5StringCodec extends AsnIA5StringBasedObjectCodec<String> {

  public AsnIA5StringCodec(AsnSizeConstraint sizeConstraint) {
    super(sizeConstraint);
  }

  @Override
  public String decode() {
    return getCharString();
  }

  @Override
  public void encode(String value) {
    setCharString(value);
  }

  @Override
  public String toString() {
    return "IA5String{"
        + "value='" + decode() + '\''
        + '}';
  }
}
