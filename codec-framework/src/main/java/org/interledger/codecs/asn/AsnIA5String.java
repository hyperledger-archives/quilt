package org.interledger.codecs.asn;

import java.nio.charset.StandardCharsets;

/**
 * An ASN.1 IA5String represented internally as {@link String}.
 */
public class AsnIA5String extends AsnCharString {

  public AsnIA5String(AsnSizeConstraint sizeConstraint) {
    super(sizeConstraint, StandardCharsets.US_ASCII);
  }

  @Override
  public String toString() {
    return "IA5String{"
        + "value='" + getValue() + '\''
        + '}';
  }
}
