package org.interledger.encoding.asn.codecs;


/**
 * A base for codecs for primitive ASN.1 types.
 */
public abstract class AsnPrimitiveCodec<T> extends AsnObjectCodecBase<T> {

  private final AsnSizeConstraint sizeConstraint;

  public AsnPrimitiveCodec(AsnSizeConstraint sizeConstraint) {
    this.sizeConstraint = sizeConstraint;
  }

  public AsnPrimitiveCodec(int fixedSizeConstraint) {
    this.sizeConstraint = new AsnSizeConstraint(fixedSizeConstraint);
  }

  public AsnPrimitiveCodec(int minSize, int maxSize) {
    this.sizeConstraint = new AsnSizeConstraint(minSize, maxSize);
  }

  public final AsnSizeConstraint getSizeConstraint() {
    return this.sizeConstraint;
  }

}
