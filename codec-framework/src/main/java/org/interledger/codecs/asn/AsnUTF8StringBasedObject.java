package org.interledger.codecs.asn;

import java.nio.charset.StandardCharsets;

/**
 * An ASN.1 IA5String represented internally as {@link String}.
 */
public abstract class AsnUTF8StringBasedObject<T> extends AsnCharStringBasedObject<T> {

  public AsnUTF8StringBasedObject(AsnSizeConstraint sizeConstraint) {
    super(sizeConstraint, StandardCharsets.UTF_8);
  }

}
