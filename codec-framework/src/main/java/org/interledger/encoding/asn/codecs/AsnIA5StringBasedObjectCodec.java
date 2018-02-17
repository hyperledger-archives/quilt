package org.interledger.encoding.asn.codecs;

import java.nio.charset.StandardCharsets;

/**
 * An ASN.1 codec for ASN.1 objects that extend IA5String
 */
public abstract class AsnIA5StringBasedObjectCodec<T> extends AsnCharStringBasedObjectCodec<T> {

  public AsnIA5StringBasedObjectCodec(AsnSizeConstraint sizeConstraint) {
    super(sizeConstraint, StandardCharsets.US_ASCII);
  }

}
