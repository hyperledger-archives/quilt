package org.interledger.ilqp.oer;

import org.interledger.InterledgerAddress;
import org.interledger.InterledgerPacket;
import org.interledger.codecs.InterledgerCodecContext;
import org.interledger.codecs.InterledgerCodecContextFactory;
import org.interledger.ilqp.LiquidityCurve;
import org.interledger.ilqp.LiquidityPoint;
import org.interledger.ilqp.QuoteByDestinationAmountRequest;
import org.interledger.ilqp.QuoteByDestinationAmountResponse;
import org.interledger.ilqp.QuoteBySourceAmountRequest;
import org.interledger.ilqp.QuoteBySourceAmountResponse;
import org.interledger.ilqp.QuoteLiquidityRequest;
import org.interledger.ilqp.QuoteLiquidityResponse;

import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.hyperledger.quilt.codecs.framework.Codec;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Unit tests to validate the {@link Codec} functionality for all Interledger packets.
 */
@RunWith(Parameterized.class)
public class IlqpCodecTests {

  // first data value (0) is default
  @Parameter
  public InterledgerPacket packet;

  /**
   * The data for this test...
   */
  @Parameters
  public static Object[] data() {
    return new Object[]{
        QuoteBySourceAmountRequest.Builder.builder()
            .destinationAccount(InterledgerAddress.of("test1.foo"))
            .sourceAmount(BigInteger.valueOf(100L))
            .destinationHoldDuration(Duration.ofSeconds(30)).build(),
        QuoteBySourceAmountResponse.Builder.builder()
            .destinationAmount(BigInteger.valueOf(95L))
            .sourceHoldDuration(Duration.ofSeconds(30)).build(),
        QuoteByDestinationAmountRequest.Builder.builder()
            .destinationAccount(InterledgerAddress.of("test2.foo"))
            .destinationAmount(BigInteger.valueOf(100L))
            .destinationHoldDuration(Duration.ofSeconds(35)).build(),
        QuoteByDestinationAmountResponse.Builder.builder()
            .sourceAmount(BigInteger.valueOf(105L))
            .sourceHoldDuration(Duration.ofMinutes(1)).build(),
        QuoteLiquidityRequest.Builder.builder()
            .destinationAccount(InterledgerAddress.of("test3.foo"))
            .destinationHoldDuration(Duration.ofMinutes(5)).build(),
        QuoteLiquidityResponse.Builder.builder()
            .liquidityCurve(
                LiquidityCurve.Builder
                    .builder()
                    .liquidityPoint(LiquidityPoint.Builder.builder()
                        .inputAmount(BigInteger.ZERO).outputAmount(BigInteger.ZERO).build())
                    .liquidityPoint(LiquidityPoint.Builder.builder()
                        .inputAmount(BigInteger.ONE).outputAmount(BigInteger.ONE).build())
                    .liquidityPoint(LiquidityPoint.Builder.builder()
                        .inputAmount(BigInteger.valueOf(5)).outputAmount(BigInteger.TEN).build())
                    .build())
            .appliesTo(InterledgerAddress.of("test1.foo"))
            .sourceHoldDuration(Duration.of(10, ChronoUnit.MINUTES))
            .expiresAt(Instant.now())
            .build()};
  }

  @Test
  public void testName() throws Exception {
    final InterledgerCodecContext context = InterledgerCodecContextFactory.oer();

    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    context.write(packet, outputStream);

    final ByteArrayInputStream byteArrayInputStream =
        new ByteArrayInputStream(outputStream.toByteArray());

    final InterledgerPacket decodedPacket = context.read(byteArrayInputStream);
    MatcherAssert.assertThat(decodedPacket.getClass().getName(),
        CoreMatchers.is(packet.getClass().getName()));
    MatcherAssert.assertThat(decodedPacket, CoreMatchers.is(packet));
  }
}
