package org.interledger.codecs;

import static junit.framework.TestCase.assertTrue;

import org.interledger.InterledgerAddress;
import org.interledger.InterledgerPacketType;
import org.interledger.ilp.InterledgerPayment;
import org.interledger.ilqp.QuoteByDestinationAmountRequest;
import org.interledger.ilqp.QuoteByDestinationAmountResponse;
import org.interledger.ilqp.QuoteBySourceAmountRequest;
import org.interledger.ilqp.QuoteBySourceAmountResponse;
import org.interledger.ilqp.QuoteLiquidityRequest;

import org.hyperledger.quilt.codecs.framework.CodecContext;
import org.hyperledger.quilt.codecs.oer.OerIA5StringCodec.OerIA5String;
import org.hyperledger.quilt.codecs.oer.OerLengthPrefixCodec.OerLengthPrefix;
import org.hyperledger.quilt.codecs.oer.OerOctetStringCodec.OerOctetString;
import org.hyperledger.quilt.codecs.oer.OerUint64Codec.OerUint64;
import org.hyperledger.quilt.codecs.oer.OerUint8Codec.OerUint8;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link InterledgerCodecContextFactory}.
 */
public class InterledgerCodecContextFactoryTest {

  private CodecContext context;

  @Before
  public void setup() {
    context = InterledgerCodecContextFactory.oer();
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

    //TODO Finish liquidity response
    assertTrue(context.hasRegisteredCodec(QuoteLiquidityRequest.class));
    //assertTrue(context.hasRegisteredCodec(QuoteLiquidityResponse.class));
    assertTrue(context.hasRegisteredCodec(QuoteByDestinationAmountRequest.class));
    assertTrue(context.hasRegisteredCodec(QuoteByDestinationAmountResponse.class));
    assertTrue(context.hasRegisteredCodec(QuoteBySourceAmountRequest.class));
    assertTrue(context.hasRegisteredCodec(QuoteBySourceAmountResponse.class));

  }

}
