package org.interledger.codecs.asn;

public class AsnSizeConstraint {

  private final int min;
  private final int max;

  public AsnSizeConstraint(int fixedSize) {
    this.min = fixedSize;
    this.max = fixedSize;
  }

  public AsnSizeConstraint(int min, int max) {
    this.min = min;
    this.max = max;
  }

  public boolean isUnconstrained() {
    return max == 0 && min == 0;
  }

  public boolean isFixedSize() {
    return max != 0 && max == min;
  }

  public int getMax() {
    return max;
  }

  public int getMin() {
    return min;
  }

  public static AsnSizeConstraint unconstrained() {
    return new AsnSizeConstraint(0, 0);
  }

}
