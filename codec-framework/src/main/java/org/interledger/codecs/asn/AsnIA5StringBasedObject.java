package org.interledger.codecs.asn;

import java.nio.charset.StandardCharsets;

/**
 * An ASN.1 IA5String represented internally as {@link String}.
 */
public abstract class AsnIA5StringBasedObject<T> extends AsnCharStringBasedObject<T> {

  public AsnIA5StringBasedObject(AsnSizeConstraint sizeConstraint) {
    super(sizeConstraint, StandardCharsets.US_ASCII);
  }

}
