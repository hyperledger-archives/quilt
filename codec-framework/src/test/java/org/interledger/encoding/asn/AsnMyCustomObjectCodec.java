package org.interledger.encoding.asn;

import org.interledger.encoding.MyCustomObject;
import org.interledger.encoding.asn.codecs.AsnOctetStringCodec;
import org.interledger.encoding.asn.codecs.AsnSequenceCodec;
import org.interledger.encoding.asn.codecs.AsnSizeConstraint;
import org.interledger.encoding.asn.codecs.AsnUint32Codec;
import org.interledger.encoding.asn.codecs.AsnUint64Codec;
import org.interledger.encoding.asn.codecs.AsnUint8Codec;
import org.interledger.encoding.asn.codecs.AsnUtf8StringCodec;

import java.math.BigInteger;

public class AsnMyCustomObjectCodec extends AsnSequenceCodec<MyCustomObject> {

  /**
   * Codec for the custom test object.
   */
  public AsnMyCustomObjectCodec() {

    super(
        new AsnUtf8StringCodec(AsnSizeConstraint.UNCONSTRAINED),
        new AsnUtf8StringCodec(4),
        new AsnUint8Codec(),
        new AsnUint32Codec(),
        new AsnUint64Codec(),
        new AsnOctetStringCodec(AsnSizeConstraint.UNCONSTRAINED),
        new AsnOctetStringCodec(32));

  }

  @Override
  public MyCustomObject decode() {
    return MyCustomObject.builder()
      .utf8StringProperty(getValueAt(0))
      .fixedLengthUtf8StringProperty(getValueAt(1))
      .uint8Property(getValueAt(2))
      .uint32Property(getValueAt(3))
      .uint64Property(getValueAt(4))
      .octetStringProperty(getValueAt(5))
      .fixedLengthOctetStringProperty(getValueAt(6))
      .build();
  }

  @Override
  public void encode(MyCustomObject value) {
    setValueAt(0, value.getUtf8StringProperty());
    setValueAt(1, value.getFixedLengthUtf8StringProperty());
    setValueAt(2, value.getUint8Property());
    setValueAt(3, value.getUint32Property());
    setValueAt(4, value.getUint64Property());
    setValueAt(5, value.getOctetStringProperty());
    setValueAt(6, value.getFixedLengthOctetStringProperty());
  }

}
