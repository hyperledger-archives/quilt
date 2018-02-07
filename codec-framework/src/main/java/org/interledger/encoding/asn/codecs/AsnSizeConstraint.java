package org.interledger.encoding.asn.codecs;

/**
 * A simple data structure for representing ASN.1 size constraints.
 */
public class AsnSizeConstraint {

  private final int min;
  private final int max;

  /**
   * Create a new size constraint for a fixed sized field.
   * @param fixedSize the size of the field
   */
  public AsnSizeConstraint(int fixedSize) {
    this.min = fixedSize;
    this.max = fixedSize;
  }

  /**
   * Create a new size constraint for a variable length field.
   * @param min The minimum size of the field.
   * @param max The maximum size of the field.
   */
  public AsnSizeConstraint(int min, int max) {
    this.min = min;
    this.max = max;
  }

  /**
   * A flag indicating if this object represents a size constraint or not.
   *
   * @return false if this object represents a constrained size.
   */
  public boolean isUnconstrained() {
    return max == 0 && min == 0;
  }

  /**
   * A flag indicating if this object represents a fixed size constraint.
   *
   * @return true if the object has a fixed size.
   */
  public boolean isFixedSize() {
    return max != 0 && max == min;
  }

  /**
   * Get the maximum size represented by this constraint.
   *
   * @return the max size or 0 if {@link #isUnconstrained()} is true.
   */
  public int getMax() {
    return max;
  }

  /**
   * Get the minimum size represented by this constraint.
   *
   * @return the min size or 0 if {@link #isUnconstrained()} is true.
   */
  public int getMin() {
    return min;
  }

  /**
   * A {@link AsnSizeConstraint} that represents an unconstrained size.
   *
   * <p>The following {@code AsnSizeConstraint.UNCONSTRAINED.isUnconstrained()} always returns true.
   */
  public static final AsnSizeConstraint UNCONSTRAINED = new AsnSizeConstraint(0, 0);

}
