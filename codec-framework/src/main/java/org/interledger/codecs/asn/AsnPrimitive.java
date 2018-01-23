package org.interledger.codecs.asn;


/**
 * A base for wrappers that map an ASN.1 definition to a native type
 *
 * @param <T> the native type represented by this ASN.1 object
 */
public abstract class AsnPrimitive<T> extends AsnObject<T> {

  private final AsnSizeConstraint sizeConstraint;

  public AsnPrimitive(AsnSizeConstraint sizeConstraint) {
    this.sizeConstraint = sizeConstraint;
  }

  public final AsnSizeConstraint getSizeConstraint() {
    return this.sizeConstraint;
  }

}
