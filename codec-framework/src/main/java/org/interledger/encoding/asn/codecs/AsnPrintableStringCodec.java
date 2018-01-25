package org.interledger.encoding.asn.codecs;

/**
 * An ASN.1 codec for PrintableString objects.
 */
public class AsnPrintableStringCodec extends AsnPrintableStringBasedObjectCodec<String> {

  public AsnPrintableStringCodec(AsnSizeConstraint sizeConstraint) {
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
    return "PrintableString{"
        + "value='" + decode() + '\''
        + '}';
  }
}
