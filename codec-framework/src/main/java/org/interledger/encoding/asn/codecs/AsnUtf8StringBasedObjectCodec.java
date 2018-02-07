package org.interledger.encoding.asn.codecs;

import java.nio.charset.StandardCharsets;

/**
 * An ASN.1 codec for ASN.1 objects that extend UTF8String
 */
public abstract class AsnUtf8StringBasedObjectCodec<T> extends AsnCharStringBasedObjectCodec<T> {

  public AsnUtf8StringBasedObjectCodec(AsnSizeConstraint sizeConstraint) {
    super(sizeConstraint, StandardCharsets.UTF_8);
  }

  public AsnUtf8StringBasedObjectCodec(int fixedSizeConstraint) {
    super(fixedSizeConstraint, StandardCharsets.UTF_8);
  }

  public AsnUtf8StringBasedObjectCodec(int minSize, int maxSize) {
    super(minSize, maxSize, StandardCharsets.UTF_8);
  }



}
