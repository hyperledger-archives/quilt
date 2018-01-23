package org.interledger.codecs.asn;

/**
 * ASN.1 fixed-length 8-bit integer represented internally as an {@link Integer}.
 */
public class AsnUint8 extends AsnPrimitive<Integer> {

  public AsnUint8() {
    super(new AsnSizeConstraint(0,1));
  }

  @Override
  public void setValue(Integer value) {

    if (value > 255 || value < 0) {
      throw new IllegalArgumentException(
          "Uint8 only supports values from 0 to 255, value "
              + value + " is out of range.");
    }

    super.setValue(value);
  }

  @Override
  public String toString() {
    return "AsnUint8{"
        + "value=" + getValue()
        + '}';
  }
}