package org.interledger.core.asn.framework;

import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerErrorCode;
import org.interledger.core.InterledgerFulfillPacket;
import org.interledger.core.InterledgerPacket;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerRejectPacket;
import org.interledger.core.asn.codecs.AsnConditionCodec;
import org.interledger.core.asn.codecs.AsnFulfillmentCodec;
import org.interledger.core.asn.codecs.AsnInterledgerAddressCodec;
import org.interledger.core.asn.codecs.AsnInterledgerErrorCodeCodec;
import org.interledger.core.asn.codecs.AsnInterledgerFulfillPacketCodec;
import org.interledger.core.asn.codecs.AsnInterledgerPacketCodec;
import org.interledger.core.asn.codecs.AsnInterledgerPreparePacketCodec;
import org.interledger.core.asn.codecs.AsnInterledgerRejectPacketCodec;
import org.interledger.core.asn.codecs.AsnTimestampCodec;
import org.interledger.cryptoconditions.Condition;
import org.interledger.cryptoconditions.Fulfillment;
import org.interledger.encoding.asn.framework.CodecContext;
import org.interledger.encoding.asn.framework.CodecContextFactory;
import org.interledger.encoding.asn.serializers.oer.AsnCharStringOerSerializer;
import org.interledger.encoding.asn.serializers.oer.AsnOctetStringOerSerializer;
import org.interledger.encoding.asn.serializers.oer.AsnSequenceOerSerializer;

import java.time.Instant;

/**
 * A factory class for constructing a CodecContext that can read and write Interledger objects using
 * ASN.1 OER encoding.
 */
public class InterledgerCodecContextFactory {

  /**
   * Create an instance of {@link CodecContext} that encodes and decodes Interledger packets using
   * ASN.1 OER encoding.
   *
   * @return A new instance of {@link CodecContext}.
   */
  public static CodecContext oer() {

    return CodecContextFactory.getContext(CodecContextFactory.OCTET_ENCODING_RULES)
        .register(Instant.class, AsnTimestampCodec::new)
        .register(Condition.class, AsnConditionCodec::new,
            new AsnOctetStringOerSerializer())
        .register(Fulfillment.class, AsnFulfillmentCodec::new,
            new AsnOctetStringOerSerializer())
        .register(InterledgerAddress.class, AsnInterledgerAddressCodec::new,
            new AsnCharStringOerSerializer())
        .register(InterledgerErrorCode.class, AsnInterledgerErrorCodeCodec::new,
            new AsnCharStringOerSerializer())
        .register(InterledgerFulfillPacket.class, AsnInterledgerFulfillPacketCodec::new,
            new AsnSequenceOerSerializer())
        .register(InterledgerPacket.class, AsnInterledgerPacketCodec::new,
            new AsnSequenceOerSerializer())
        .register(InterledgerPreparePacket.class, AsnInterledgerPreparePacketCodec::new,
            new AsnSequenceOerSerializer())
        .register(InterledgerRejectPacket.class, AsnInterledgerRejectPacketCodec::new,
            new AsnSequenceOerSerializer());

  }

}
