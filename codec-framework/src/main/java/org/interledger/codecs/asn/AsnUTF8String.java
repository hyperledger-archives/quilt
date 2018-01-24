package org.interledger.codecs.asn;

import java.nio.charset.StandardCharsets;

/**
 * An ASN.1 UTF-8 String represented internally as {@link String}.
 */
public class AsnUTF8String extends AsnUTF8StringBasedObject<String> {

  public AsnUTF8String(AsnSizeConstraint sizeConstraint) {
    super(sizeConstraint);
  }

  @Override
  protected String decode() {
    return getCharString();
  }

  @Override
  protected void encode(String value) {
    setCharString(value);
  }

  @Override
  public String toString() {
    return "AsnUTF8String{"
        + "value='" + getValue() + '\''
        + '}';
  }
}