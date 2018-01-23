package org.interledger.codecs.asn;

/**
 * ASN.1 fixed-length 32-bit integer represented internally as a {@link Long}.
 */
public class AsnUint32 extends AsnOctetStringBasedObject<Long> {

  public AsnUint32() {
    super(new AsnSizeConstraint(4));
  }

  @Override
  protected Long decode() {
    byte[] bytes = getBytes();
    long value = 0;
    for (int i = 0; i <= 3; i++) {
      value <<= Byte.SIZE;
      value |= (bytes[i] & 0xFF);
    }
    return value;
  }

  @Override
  protected void encode(Long value) {

    if (value > 4294967295L || value < 0) {
      throw new IllegalArgumentException(
          "Uint32 only supports values from 0 to 4294967295, value "
              + value + " is out of range.");
    }

    byte[] bytes = new byte[4];
    for (int i = 0; i <= 3; i++) {
      bytes[i] = ((byte) ((value >> (Byte.SIZE * (3 - i))) & 0xFF));
    }
    setBytes(bytes);
  }

  @Override
  public String toString() {
    return "AsnUint32{"
        + "value=" + getValue()
        + '}';
  }
}