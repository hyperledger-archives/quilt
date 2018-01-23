package org.interledger.codecs;

import org.interledger.InterledgerAddress;
import org.interledger.InterledgerPacket;
import org.interledger.codecs.framework.CodecContext;
import org.interledger.codecs.framework.CodecContextFactory;
import org.interledger.codecs.ilp.asn.AsnCondition;
import org.interledger.codecs.ilp.asn.AsnFulfillment;
import org.interledger.codecs.ilp.asn.AsnInterledgerAddress;
import org.interledger.codecs.ilp.asn.AsnInterledgerErrorCode;
import org.interledger.codecs.ilp.asn.AsnInterledgerFulfillPacket;
import org.interledger.codecs.ilp.asn.AsnInterledgerPacket;
import org.interledger.codecs.ilp.asn.AsnInterledgerPreparePacket;
import org.interledger.codecs.ilp.asn.AsnInterledgerRejectPacket;
import org.interledger.codecs.oer.AsnCharStringOerSerializer;
import org.interledger.codecs.oer.AsnOctetStringOerSerializer;
import org.interledger.codecs.oer.AsnSequenceOerSerializer;
import org.interledger.cryptoconditions.Condition;
import org.interledger.cryptoconditions.Fulfillment;
import org.interledger.ilp.InterledgerErrorCode;
import org.interledger.ilp.InterledgerFulfillPacket;
import org.interledger.ilp.InterledgerPreparePacket;
import org.interledger.ilp.InterledgerRejectPacket;

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
        .register(Condition.class, AsnCondition::new,
            new AsnOctetStringOerSerializer())
        .register(Fulfillment.class, AsnFulfillment::new,
            new AsnOctetStringOerSerializer())
        .register(InterledgerAddress.class, AsnInterledgerAddress::new,
            new AsnCharStringOerSerializer())
        .register(InterledgerErrorCode.class, AsnInterledgerErrorCode::new,
            new AsnCharStringOerSerializer())
        .register(InterledgerFulfillPacket.class, AsnInterledgerFulfillPacket::new,
            new AsnSequenceOerSerializer())
        .register(InterledgerPacket.class, AsnInterledgerPacket::new,
            new AsnSequenceOerSerializer())
        .register(InterledgerPreparePacket.class, AsnInterledgerPreparePacket::new,
            new AsnSequenceOerSerializer())
        .register(InterledgerRejectPacket.class, AsnInterledgerRejectPacket::new,
            new AsnSequenceOerSerializer());

  }

}
