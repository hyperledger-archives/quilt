package org.interledger.codecs.stream;

import org.interledger.encoding.asn.codecs.AsnSequenceCodec;
import org.interledger.encoding.asn.codecs.AsnUint64CodecUL;
import org.interledger.stream.AmountTooLargeErrorData;

public class AsnAmountTooLargeDataCodec extends AsnSequenceCodec<AmountTooLargeErrorData> {

  public AsnAmountTooLargeDataCodec() {
    super(
        new AsnUint64CodecUL(),
        new AsnUint64CodecUL()
    );
  }

  /**
   * Decode and return the value read into the codec during serialization.
   *
   * @return the decoded object
   */
  @Override
  public AmountTooLargeErrorData decode() {
    return AmountTooLargeErrorData.builder()
        .receivedAmount(getValueAt(0))
        .maximumAmount(getValueAt(1))
        .build();
  }

  /**
   * Encode the provided value into the codec to be written during serialization.
   *
   * @param value the value to encode
   */
  @Override
  public void encode(AmountTooLargeErrorData value) {
    setValueAt(0, value.receivedAmount());
    setValueAt(1, value.maximumAmount());
  }
}
