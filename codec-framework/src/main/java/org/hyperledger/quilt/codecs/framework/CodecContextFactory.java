package org.hyperledger.quilt.codecs.framework;

import org.hyperledger.quilt.codecs.oer.OerGeneralizedTimeCodec;
import org.hyperledger.quilt.codecs.oer.OerIA5StringCodec;
import org.hyperledger.quilt.codecs.oer.OerLengthPrefixCodec;
import org.hyperledger.quilt.codecs.oer.OerOctetStringCodec;
import org.hyperledger.quilt.codecs.oer.OerSequenceOfCodec;
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
public class CodecContextFactory {

  /**
   * Create an instance of {@link CodecContext} that encodes and decodes basic types using
   * ASN.1 OER encoding.
   *
   * @return A new instance of {@link CodecContext}.
   */
  public static CodecContext oer() {

    // OER Base...
    return new CodecContext()
        .register(OerUint8.class, new OerUint8Codec())
        .register(OerUint32.class, new OerUint32Codec())
        .register(OerUint64Codec.OerUint64.class, new OerUint64Codec())
        .register(OerUint256.class, new OerUint256Codec())
        .register(OerLengthPrefixCodec.OerLengthPrefix.class, new OerLengthPrefixCodec())
        .register(OerIA5StringCodec.OerIA5String.class, new OerIA5StringCodec())
        .register(OerOctetStringCodec.OerOctetString.class, new OerOctetStringCodec())
        .register(OerGeneralizedTimeCodec.OerGeneralizedTime.class, new OerGeneralizedTimeCodec());
  }



}
