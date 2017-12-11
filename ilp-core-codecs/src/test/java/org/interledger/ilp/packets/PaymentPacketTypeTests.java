package org.interledger.ilp.packets;

import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.interledger.InterledgerPacketType;
import org.interledger.ilqp.packets.QuoteByDestinationAmountRequestPacketType;
import org.interledger.ilqp.packets.QuoteByDestinationAmountResponsePacketType;
import org.interledger.ilqp.packets.QuoteBySourceAmountRequestPacketType;
import org.interledger.ilqp.packets.QuoteBySourceAmountResponsePacketType;
import org.interledger.ilqp.packets.QuoteLiquidityRequestPacketType;
import org.interledger.ilqp.packets.QuoteLiquidityResponsePacketType;

import org.hyperledger.quilt.codecs.framework.Codec;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import java.util.Arrays;
import java.util.Collection;

/**
 * Unit tests to validate the {@link Codec} functionality for all Interledger packets.
 */
@RunWith(Parameterized.class)
public class PaymentPacketTypeTests {

  // first data value (0) is default
  @Parameter
  public InterledgerPacketType firstPacket;

  @Parameter(1)
  public InterledgerPacketType secondPacket;

  /**
   * The data for this test...
   */
  @Parameters
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][]{
        {new PaymentPacketType(), new PaymentPacketType()},
        {new QuoteByDestinationAmountRequestPacketType(),
            new QuoteByDestinationAmountRequestPacketType()},
        {new QuoteByDestinationAmountResponsePacketType(),
            new QuoteByDestinationAmountResponsePacketType()},
        {new QuoteBySourceAmountRequestPacketType(), new QuoteBySourceAmountRequestPacketType()},
        {new QuoteBySourceAmountResponsePacketType(), new QuoteBySourceAmountResponsePacketType()},
        {new QuoteLiquidityRequestPacketType(), new QuoteLiquidityRequestPacketType()},
        {new QuoteLiquidityResponsePacketType(), new QuoteLiquidityResponsePacketType()},
        {new PaymentPacketType(),
            InterledgerPacketType.fromTypeId(InterledgerPacketType.ILP_PAYMENT_TYPE)},
        {new QuoteByDestinationAmountRequestPacketType(),
            InterledgerPacketType.fromTypeId(
                InterledgerPacketType.ILQP_QUOTE_BY_DESTINATION_AMOUNT_REQUEST_TYPE)},
        {new QuoteByDestinationAmountResponsePacketType(),
            InterledgerPacketType.fromTypeId(
                InterledgerPacketType.ILQP_QUOTE_BY_DESTINATION_AMOUNT_RESPONSE_TYPE)},
        {new QuoteBySourceAmountRequestPacketType(),
            InterledgerPacketType.fromTypeId(
                InterledgerPacketType.ILQP_QUOTE_BY_SOURCE_AMOUNT_REQUEST_TYPE)},
        {new QuoteBySourceAmountResponsePacketType(),
            InterledgerPacketType.fromTypeId(
                InterledgerPacketType.ILQP_QUOTE_BY_SOURCE_AMOUNT_RESPONSE_TYPE)},

        {new QuoteLiquidityRequestPacketType(),
            InterledgerPacketType
                .fromTypeId(InterledgerPacketType.ILQP_QUOTE_LIQUIDITY_REQUEST_TYPE)},
        {new QuoteLiquidityResponsePacketType(), InterledgerPacketType
            .fromTypeId(InterledgerPacketType.ILQP_QUOTE_LIQUIDITY_RESPONSE_TYPE)},});
  }

  @Test
  public void testEqualsHashcode() throws Exception {
    assertThat(firstPacket, is(secondPacket));
    assertThat(secondPacket, is(firstPacket));

    assertTrue(firstPacket.equals(secondPacket));
    assertTrue(secondPacket.equals(firstPacket));

    assertThat(firstPacket.hashCode(), is(secondPacket.hashCode()));
    assertThat(firstPacket, is(secondPacket));
  }
}
