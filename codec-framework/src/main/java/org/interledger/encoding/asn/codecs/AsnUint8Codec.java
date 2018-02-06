package org.interledger.encoding.asn.codecs;

/**
 * An ASN.1 codec for UInt8 objects that decodes them into {@link Integer} values.
 */
public class AsnUint8Codec extends AsnPrimitiveCodec<Integer> {

  private Integer value;

  public AsnUint8Codec() {
    super(new AsnSizeConstraint(0,1));
  }

  @Override
  public Integer decode() {
    return value;
  }

  @Override
  public void encode(Integer value) {

    if (value > 255 || value < 0) {
      throw new IllegalArgumentException(
          "Uint8 only supports values from 0 to 255, value "
              + value + " is out of range.");
    }

    this.value = value;

    onEncodeEvent(value);
  }

  @Override
  public String toString() {
    return "AsnUint8Codec{"
        + "value=" + decode()
        + '}';
  }
}