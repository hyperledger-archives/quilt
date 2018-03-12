package org.interledger.encoding.asn.codecs;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;

/**
 * An ASN.1 codec for UInt64 objects that decodes them into {@link BigInteger} values..
 */
public class AsnUintCodec extends AsnOctetStringBasedObjectCodec<BigInteger> {

  public AsnUintCodec() {
    super(AsnSizeConstraint.UNCONSTRAINED);
  }

  @Override
  public BigInteger decode() {
    return new BigInteger(1, getBytes());
  }

  @Override
  public void encode(BigInteger value) {


    if (value.compareTo(BigInteger.ZERO) <= 0) {
      throw new IllegalArgumentException("value must be positive or zero");
    }

    byte[] bytes = value.toByteArray();

    // BigInteger's toByteArray writes data in two's complement,
    // so positive values may have a leading 0x00 byte.
    if (bytes[0] == 0x00) {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      baos.write(bytes, 1, bytes.length - 1);
      setBytes(baos.toByteArray());
      return;
    }

    setBytes(bytes);

  }

}