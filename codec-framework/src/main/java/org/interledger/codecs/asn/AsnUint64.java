package org.interledger.codecs.asn;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.util.Arrays;

/**
 * ASN.1 fixed-length 64-bit integer represented internally as a {@link BigInteger}.
 */
public class AsnUint64 extends AsnOctetStringBasedObject<BigInteger> {

  public AsnUint64() {
    super(new AsnSizeConstraint(8));
  }

  @Override
  protected BigInteger decode() {
    return new BigInteger(1, getBytes());
  }

  @Override
  protected void encode(BigInteger value) {

    if (value.bitLength() > 64 || value.compareTo(BigInteger.ZERO) < 0) {
      throw new IllegalArgumentException(
          "Uint64 only supports values from 0 to 18446744073709551615, value "
              + value.toString(10) + " is out of range.");
    }

    byte[] bytes = value.toByteArray();
    ByteArrayOutputStream baos = new ByteArrayOutputStream(8);

    /* BigInteger's toByteArray writes data in two's complement, so positive values requiring 64
     * bits will include a leading byte set to 0 which we don't want. */
    if (bytes.length == 9) {
      baos.write(bytes, 1, 8);
      setBytes(baos.toByteArray());
      return;
    }

    /* BigInteger.toByteArray will return the smallest byte array possible. We are committed
     * to a fixed number of bytes, so we might need to pad the value out. */
    for (int i = 0; i < 8 - bytes.length; i++) {
      baos.write(0);
    }
    baos.write(bytes, 0, bytes.length);

    setBytes(baos.toByteArray());

  }

}