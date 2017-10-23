package org.interledger.codecs;

import org.interledger.InterledgerAddress;
import org.interledger.codecs.oer.OerGeneralizedTimeCodec;
import org.interledger.codecs.oer.OerGeneralizedTimeCodec.OerGeneralizedTime;
import org.interledger.codecs.oer.OerIA5StringCodec;
import org.interledger.codecs.oer.OerIA5StringCodec.OerIA5String;
import org.interledger.codecs.oer.OerLengthPrefixCodec;
import org.interledger.codecs.oer.OerLengthPrefixCodec.OerLengthPrefix;
import org.interledger.codecs.oer.OerOctetStringCodec;
import org.interledger.codecs.oer.OerOctetStringCodec.OerOctetString;
import org.interledger.codecs.oer.OerSequenceOfAddressCodec;
import org.interledger.codecs.oer.OerSequenceOfAddressCodec.OerSequenceOfAddress;
import org.interledger.codecs.oer.OerUint256Codec;
import org.interledger.codecs.oer.OerUint256Codec.OerUint256;
import org.interledger.codecs.oer.OerUint32Codec;
import org.interledger.codecs.oer.OerUint32Codec.OerUint32;
import org.interledger.codecs.oer.OerUint64Codec;
import org.interledger.codecs.oer.OerUint64Codec.OerUint64;
import org.interledger.codecs.oer.OerUint8Codec;
import org.interledger.codecs.oer.OerUint8Codec.OerUint8;
import org.interledger.codecs.oer.ilp.ConditionOerCodec;
import org.interledger.codecs.oer.ilp.FulfillmentOerCodec;
import org.interledger.codecs.oer.ilp.InterledgerAddressOerCodec;
import org.interledger.codecs.oer.ilp.InterledgerPacketTypeOerCodec;
import org.interledger.codecs.oer.ilp.InterledgerPaymentOerCodec;
import org.interledger.codecs.oer.ilp.InterledgerProtocolProtocolErrorOerCodec;
import org.interledger.codecs.oer.ilqp.QuoteByDestinationAmountRequestOerCodec;
import org.interledger.codecs.oer.ilqp.QuoteByDestinationAmountResponseOerCodec;
import org.interledger.codecs.oer.ilqp.QuoteBySourceAmountRequestOerCodec;
import org.interledger.codecs.oer.ilqp.QuoteBySourceAmountResponseOerCodec;
import org.interledger.codecs.oer.ilqp.QuoteLiquidityRequestOerCodec;
import org.interledger.codecs.oer.ilqp.QuoteLiquidityResponseOerCodec;
import org.interledger.codecs.oer.ipr.InterledgerPaymentRequestOerCodec;
import org.interledger.codecs.packettypes.InterledgerPacketType;
import org.interledger.codecs.psk.PskMessageBinaryCodec;
import org.interledger.cryptoconditions.Condition;
import org.interledger.cryptoconditions.Fulfillment;
import org.interledger.cryptoconditions.PreimageSha256Condition;
import org.interledger.cryptoconditions.PreimageSha256Fulfillment;
import org.interledger.ilp.InterledgerPayment;
import org.interledger.ilp.InterledgerProtocolError;
import org.interledger.ilqp.QuoteByDestinationAmountRequest;
import org.interledger.ilqp.QuoteByDestinationAmountResponse;
import org.interledger.ilqp.QuoteBySourceAmountRequest;
import org.interledger.ilqp.QuoteBySourceAmountResponse;
import org.interledger.ilqp.QuoteLiquidityRequest;
import org.interledger.ilqp.QuoteLiquidityResponse;
import org.interledger.ipr.InterledgerPaymentRequest;
import org.interledger.psk.PskMessage;

/**
 * A factory class for constructing a CodecContext that can read and write Interledger objects using
 * ASN.1 OER encoding.
 */
public class CodecContextFactory {

  /**
   * Create an instance of {@link CodecContext} that encodes and decodes Interledger packets using
   * ASN.1 OER encoding.
   *
   * @return A new instance of {@link CodecContext}.
   */
  public static CodecContext interledger() {

    // OER Base...
    return new CodecContext()
      .register(OerUint8.class, new OerUint8Codec())
      .register(OerUint32.class, new OerUint32Codec())
      .register(OerUint64.class, new OerUint64Codec())
      .register(OerUint256.class, new OerUint256Codec())
      .register(OerLengthPrefix.class, new OerLengthPrefixCodec())
      .register(OerIA5String.class, new OerIA5StringCodec())
      .register(OerOctetString.class, new OerOctetStringCodec())
      .register(OerGeneralizedTime.class, new OerGeneralizedTimeCodec())
      .register(OerSequenceOfAddress.class, new OerSequenceOfAddressCodec())

      // ILP
      .register(InterledgerAddress.class, new InterledgerAddressOerCodec())
      .register(InterledgerPacketType.class, new InterledgerPacketTypeOerCodec())
      .register(InterledgerPayment.class, new InterledgerPaymentOerCodec())
      .register(InterledgerProtocolError.class, new InterledgerProtocolProtocolErrorOerCodec())
      .register(InterledgerPaymentRequest.class, new InterledgerPaymentRequestOerCodec())
      .register(Condition.class, new ConditionOerCodec())
      .register(PreimageSha256Condition.class, new ConditionOerCodec())
      .register(Fulfillment.class, new FulfillmentOerCodec())
      .register(PreimageSha256Fulfillment.class, new FulfillmentOerCodec())

      // ILQP
      .register(QuoteByDestinationAmountRequest.class,
        new QuoteByDestinationAmountRequestOerCodec())
      .register(QuoteByDestinationAmountResponse.class,
        new QuoteByDestinationAmountResponseOerCodec())
      .register(QuoteBySourceAmountRequest.class, new QuoteBySourceAmountRequestOerCodec())
      .register(QuoteBySourceAmountResponse.class, new QuoteBySourceAmountResponseOerCodec())
      .register(QuoteLiquidityRequest.class, new QuoteLiquidityRequestOerCodec())
      .register(QuoteLiquidityResponse.class, new QuoteLiquidityResponseOerCodec())

      // PSK
      .register(PskMessage.class, new PskMessageBinaryCodec());
  }


  public static CodecContext interledgerJson() {
    throw new RuntimeException("Not yet implemented!");
  }

  public static CodecContext interledgerProtobuf() {
    throw new RuntimeException("Not yet implemented!");
  }

}
