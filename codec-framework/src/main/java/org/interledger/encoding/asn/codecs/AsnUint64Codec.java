package org.interledger.encoding.asn.codecs;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;

/**
 * An ASN.1 codec for UInt64 objects that decodes them into {@link Long} values..
 */
public class AsnUint64Codec extends AsnOctetStringBasedObjectCodec<Long> {

  public AsnUint64Codec() {
    super(new AsnSizeConstraint(8));
  }

  @Override
  public Long decode() {
    byte[] bytes = getBytes();
    long value = 0;
    for (int i = 0; i <= 7; i++) {
      value <<= Byte.SIZE;
      value |= (bytes[i] & 0xFF);
    }
    return value;
  }

  @Override
  public void encode(Long value) {

    byte[] bytes = new byte[8];
    for (int i = 0; i <= 7; i++) {
      bytes[i] = ((byte) ((value >> (Byte.SIZE * (7 - i))) & 0xFF));
    }
    setBytes(bytes);

  }

}