package org.interledger.codecs.asn;

import java.nio.charset.StandardCharsets;

/**
 * An ASN.1 UTF-8 String represented internally as {@link String}.
 */
public class AsnUTF8String extends AsnCharString {

  public AsnUTF8String(AsnSizeConstraint sizeConstraint) {
    super(sizeConstraint, StandardCharsets.UTF_8);
  }

  @Override
  public String toString() {
    return "AsnUTF8String{"
        + "value='" + getValue() + '\''
        + '}';
  }
}