package org.interledger.btp.asn.codecs;

import org.interledger.btp.BtpErrorCode;
import org.interledger.encoding.asn.codecs.AsnIA5StringCodec;
import org.interledger.encoding.asn.codecs.AsnOctetStringCodec;
import org.interledger.encoding.asn.codecs.AsnSizeConstraint;
import org.interledger.encoding.asn.codecs.AsnUtf8StringCodec;

import org.interledger.btp.BtpError;

/**
 * Used to encode/decode the body of a MESSAGE and RESPONSE
 */
public class AsnBtpErrorDataCodec extends AsnBtpPacketDataCodec<BtpError> {


  public AsnBtpErrorDataCodec(long requestId) {
    super(
        requestId,
        new AsnUtf8StringCodec(new AsnSizeConstraint(3)), //Code
        new AsnUtf8StringCodec(AsnSizeConstraint.UNCONSTRAINED), //Name
        new AsnIA5StringCodec(AsnSizeConstraint.UNCONSTRAINED), //TODO Parse time
        new AsnOctetStringCodec(AsnSizeConstraint.UNCONSTRAINED), //Data
        new AsnBtpSubProtocolsCodec() //SubProtocols
    );
  }

  /**
   * Decode and return the value read into the codec during serialization.
   *
   * @return the decoded object
   */
  @Override
  public BtpError decode() {
    return BtpError.builder()
        .requestId(getRequestId())
        .errorCode(BtpErrorCode.fromString(getValueAt(0)))
        .errorName(getValueAt(1))
        .triggeredAt(getValueAt(2))
        .errorData(getValueAt(3))
        .subProtocols(getValueAt(4))
        .build();
  }

  /**
   * Encode the provided value into the codec to be written during serialization.
   *
   * @param value the value to encode
   */
  @Override
  public void encode(BtpError value) {
    setValueAt(0, value.getErrorCode().getCode());
    setValueAt(1, value.getErrorName());
    setValueAt(2, value.getTriggeredAt());
    setValueAt(3, value.getErrorData());
    setValueAt(4, value.getSubProtocols());
  }

}
