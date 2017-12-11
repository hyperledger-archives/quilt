package org.interledger.codecs;

import org.interledger.InterledgerAddress;
import org.interledger.InterledgerPacketType;
import org.interledger.SequenceOfInterledgerAddresses;
import org.interledger.cryptoconditions.Condition;
import org.interledger.cryptoconditions.Fulfillment;
import org.interledger.cryptoconditions.PreimageSha256Condition;
import org.interledger.cryptoconditions.PreimageSha256Fulfillment;
import org.interledger.ilp.InterledgerPayment;
import org.interledger.ilp.InterledgerProtocolError;
import org.interledger.ilp.oer.ConditionOerCodec;
import org.interledger.ilp.oer.FulfillmentOerCodec;
import org.interledger.ilp.oer.InterledgerAddressOerCodec;
import org.interledger.ilp.oer.InterledgerPacketTypeOerCodec;
import org.interledger.ilp.oer.InterledgerPaymentOerCodec;
import org.interledger.ilp.oer.InterledgerProtocolProtocolErrorOerCodec;
import org.interledger.ilp.oer.SequenceOfInterledgerAddressesOerCodec;
import org.interledger.ilqp.QuoteByDestinationAmountRequest;
import org.interledger.ilqp.QuoteByDestinationAmountResponse;
import org.interledger.ilqp.QuoteBySourceAmountRequest;
import org.interledger.ilqp.QuoteBySourceAmountResponse;
import org.interledger.ilqp.QuoteLiquidityRequest;
import org.interledger.ilqp.QuoteLiquidityResponse;
import org.interledger.ilqp.oer.QuoteByDestinationAmountRequestOerCodec;
import org.interledger.ilqp.oer.QuoteByDestinationAmountResponseOerCodec;
import org.interledger.ilqp.oer.QuoteBySourceAmountRequestOerCodec;
import org.interledger.ilqp.oer.QuoteBySourceAmountResponseOerCodec;
import org.interledger.ilqp.oer.QuoteLiquidityRequestOerCodec;
import org.interledger.ilqp.oer.QuoteLiquidityResponseOerCodec;

import org.hyperledger.quilt.codecs.framework.CodecContext;
import org.hyperledger.quilt.codecs.oer.OerGeneralizedTimeCodec;
import org.hyperledger.quilt.codecs.oer.OerIA5StringCodec;
import org.hyperledger.quilt.codecs.oer.OerLengthPrefixCodec;
import org.hyperledger.quilt.codecs.oer.OerOctetStringCodec;
import org.hyperledger.quilt.codecs.oer.OerUint256Codec;
import org.hyperledger.quilt.codecs.oer.OerUint256Codec.OerUint256;
import org.hyperledger.quilt.codecs.oer.OerUint32Codec;
import org.hyperledger.quilt.codecs.oer.OerUint32Codec.OerUint32;
import org.hyperledger.quilt.codecs.oer.OerUint64Codec;
import org.hyperledger.quilt.codecs.oer.OerUint8Codec;
import org.hyperledger.quilt.codecs.oer.OerUint8Codec.OerUint8;

/**
 * A factory class for constructing a CodecContext that can read and write Interledger objects using
 * ASN.1 OER encoding.
 */
public class InterledgerCodecContextFactory {

  /**
   * Create an instance of {@link CodecContext} that encodes and decodes Interledger packets using
   * ASN.1 OER encoding.
   *
   * @return A new instance of {@link InterledgerCodecContext}.
   */
  public static InterledgerCodecContext oer() {

    // OER Base...
    return new InterledgerCodecContext()
      .register(OerUint8.class, new OerUint8Codec())
      .register(OerUint32.class, new OerUint32Codec())
      .register(OerUint64Codec.OerUint64.class, new OerUint64Codec())
      .register(OerUint256.class, new OerUint256Codec())
      .register(OerLengthPrefixCodec.OerLengthPrefix.class, new OerLengthPrefixCodec())
      .register(OerIA5StringCodec.OerIA5String.class, new OerIA5StringCodec())
      .register(OerOctetStringCodec.OerOctetString.class, new OerOctetStringCodec())
      .register(OerGeneralizedTimeCodec.OerGeneralizedTime.class, new OerGeneralizedTimeCodec())

      // ILP
      .register(InterledgerAddress.class, new InterledgerAddressOerCodec())
      .register(SequenceOfInterledgerAddresses.class,
          new SequenceOfInterledgerAddressesOerCodec())
      .register(InterledgerPacketType.class, new InterledgerPacketTypeOerCodec())
      .register(InterledgerPayment.class, new InterledgerPaymentOerCodec())
      .register(InterledgerProtocolError.class, new InterledgerProtocolProtocolErrorOerCodec())
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
      .register(QuoteLiquidityResponse.class, new QuoteLiquidityResponseOerCodec());
  }


  public static CodecContext interledgerJson() {
    throw new RuntimeException("Not yet implemented!");
  }

  public static CodecContext interledgerProtobuf() {
    throw new RuntimeException("Not yet implemented!");
  }

}
