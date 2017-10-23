package org.interledger.codecs;

import static junit.framework.TestCase.assertTrue;

import org.interledger.InterledgerAddress;
import org.interledger.codecs.oer.OerIA5StringCodec.OerIA5String;
import org.interledger.codecs.oer.OerLengthPrefixCodec.OerLengthPrefix;
import org.interledger.codecs.oer.OerOctetStringCodec.OerOctetString;
import org.interledger.codecs.oer.OerUint64Codec.OerUint64;
import org.interledger.codecs.oer.OerUint8Codec.OerUint8;
import org.interledger.codecs.packettypes.InterledgerPacketType;
import org.interledger.ilp.InterledgerPayment;
import org.interledger.ilqp.QuoteByDestinationAmountRequest;
import org.interledger.ilqp.QuoteByDestinationAmountResponse;
import org.interledger.ilqp.QuoteBySourceAmountRequest;
import org.interledger.ilqp.QuoteBySourceAmountResponse;
import org.interledger.ilqp.QuoteLiquidityRequest;
import org.interledger.ilqp.QuoteLiquidityResponse;
import org.interledger.ipr.InterledgerPaymentRequest;
import org.interledger.psk.PskMessage;

import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link CodecContextFactory}.
 */
public class CodecContextFactoryTest {

  private CodecContext context;

  @Before
  public void setup() {
    context = CodecContextFactory.interledger();
  }

  @Test
  public void interledger() throws Exception {
    assertTrue(context.hasRegisteredCodec(OerUint8.class));
    assertTrue(context.hasRegisteredCodec(OerUint64.class));
    assertTrue(context.hasRegisteredCodec(OerOctetString.class));
    assertTrue(context.hasRegisteredCodec(OerLengthPrefix.class));
    assertTrue(context.hasRegisteredCodec(OerIA5String.class));

    assertTrue(context.hasRegisteredCodec(InterledgerAddress.class));
    assertTrue(context.hasRegisteredCodec(InterledgerPacketType.class));
    assertTrue(context.hasRegisteredCodec(InterledgerPayment.class));
    assertTrue(context.hasRegisteredCodec(InterledgerPaymentRequest.class));

    //TODO Finish liquidity response
    assertTrue(context.hasRegisteredCodec(QuoteLiquidityRequest.class));
    //assertTrue(context.hasRegisteredCodec(QuoteLiquidityResponse.class));
    assertTrue(context.hasRegisteredCodec(QuoteByDestinationAmountRequest.class));
    assertTrue(context.hasRegisteredCodec(QuoteByDestinationAmountResponse.class));
    assertTrue(context.hasRegisteredCodec(QuoteBySourceAmountRequest.class));
    assertTrue(context.hasRegisteredCodec(QuoteBySourceAmountResponse.class));

    assertTrue(context.hasRegisteredCodec(PskMessage.class));
  }

}
