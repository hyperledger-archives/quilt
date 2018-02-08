package org.interledger.encoding.asn.codecs;

/**
 * An ASN.1 codec for UTF8String objects.
 */
public class AsnUtf8StringCodec extends AsnUtf8StringBasedObjectCodec<String> {

  public AsnUtf8StringCodec(AsnSizeConstraint sizeConstraint) {
    super(sizeConstraint);
  }

  public AsnUtf8StringCodec(int fixedSizeConstraint) {
    super(fixedSizeConstraint);
  }

  public AsnUtf8StringCodec(int minSize, int maxSize) {
    super(minSize, maxSize);
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
    return "AsnUtf8StringCodec{"
        + "value='" + decode() + '\''
        + '}';
  }
}