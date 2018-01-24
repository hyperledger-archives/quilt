package org.interledger.codecs.asn;

import java.nio.charset.StandardCharsets;

/**
 * An ASN.1 IA5String represented internally as {@link String}.
 */
public class AsnIA5String extends AsnIA5StringBasedObject<String> {

  public AsnIA5String(AsnSizeConstraint sizeConstraint) {
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
    return "IA5String{"
        + "value='" + getValue() + '\''
        + '}';
  }
}
