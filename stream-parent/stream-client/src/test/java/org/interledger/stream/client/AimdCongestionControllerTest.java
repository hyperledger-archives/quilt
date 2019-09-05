package org.interledger.stream.client;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

import org.interledger.codecs.stream.StreamCodecContextFactory;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerErrorCode;
import org.interledger.core.InterledgerRejectPacket;
import org.interledger.encoding.asn.framework.CodecContextFactory;
import org.interledger.stream.AmountTooLargeErrorData;
import org.interledger.stream.client.AimdCongestionController.CongestionState;

import com.google.common.primitives.UnsignedLong;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;

/**
 * Unit tests for {@link AimdCongestionController}.
 */
public class AimdCongestionControllerTest {

  public static final UnsignedLong SIX_HUNDRED = UnsignedLong.valueOf(600L);
  private static final InterledgerAddress OPERATOR_ADRESS = InterledgerAddress.of("test.operator");
  private static final UnsignedLong ONE_K = UnsignedLong.valueOf(1000L);
  private static final InterledgerRejectPacket T04_INSUFFICIENT_LIQUIDITY = InterledgerRejectPacket.builder()
      .code(InterledgerErrorCode.T04_INSUFFICIENT_LIQUIDITY)
      .triggeredBy(OPERATOR_ADRESS)
      .message("the error message")
      .build();

  private AimdCongestionController controller;

  @Before
  public void setUp() {
    this.controller = new AimdCongestionController();
  }

  @Test(expected = NullPointerException.class)
  public void constructWithNullStartAmount() {
    try {
      new AimdCongestionController(null, UnsignedLong.ONE, BigDecimal.TEN, CodecContextFactory.oer());
      fail();
    } catch (NullPointerException e) {
      assertThat(e.getMessage(), is("startAmount must not be null"));
      throw e;
    }
  }

  @Test(expected = NullPointerException.class)
  public void constructWithNullIncreaseAmount() {
    try {
      new AimdCongestionController(UnsignedLong.ONE, null, BigDecimal.TEN, CodecContextFactory.oer());
      fail();
    } catch (NullPointerException e) {
      assertThat(e.getMessage(), is("increaseAmount must not be null"));
      throw e;
    }
  }

  @Test(expected = NullPointerException.class)
  public void constructWithNullDecreaseFactor() {
    try {
      new AimdCongestionController(UnsignedLong.ONE, UnsignedLong.ONE, null, CodecContextFactory.oer());
      fail();
    } catch (NullPointerException e) {
      assertThat(e.getMessage(), is("decreaseFactor must not be null"));
      throw e;
    }
  }

  @Test(expected = NullPointerException.class)
  public void constructWithNullCodecContext() {
    try {
      new AimdCongestionController(UnsignedLong.ONE, UnsignedLong.ONE, BigDecimal.TEN, null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e.getMessage(), is("streamCodecContext must not be null"));
      throw e;
    }
  }

  @Test
  public void doublesMaxAmountOnFulfill() {
    UnsignedLong amount = controller.getMaxAmount();
    controller.prepare(amount);
    controller.fulfill(amount);
    assertThat(controller.getMaxAmount(), is(UnsignedLong.valueOf(2000L)));

    amount = controller.getMaxAmount();
    controller.prepare(amount);
    controller.fulfill(amount);
    assertThat(controller.getMaxAmount(), is(UnsignedLong.valueOf(4000L)));

    amount = controller.getMaxAmount();
    controller.prepare(amount);
    controller.fulfill(amount);
    assertThat(controller.getMaxAmount(), is(UnsignedLong.valueOf(8000L)));
  }

  @Test
  public void doesntOverflowU64WithSlowStart() {
    this.controller = new AimdCongestionController(
        UnsignedLong.MAX_VALUE.minus(UnsignedLong.ONE),
        ONE_K,
        BigDecimal.valueOf(2L),
        StreamCodecContextFactory.oer()
    );
    this.controller.setCongestionState(CongestionState.SLOW_START);
    assertThat(controller.getCongestionState(), is(CongestionState.SLOW_START));

    UnsignedLong amount = controller.getMaxAmount();
    controller.prepare(amount);
    controller.fulfill(amount);
    assertThat(controller.getMaxAmount(), is(UnsignedLong.MAX_VALUE));

    controller.prepare(amount);
    controller.fulfill(amount);
    assertThat(controller.getMaxAmount(), is(UnsignedLong.MAX_VALUE));
  }

  @Test
  public void additiveIncrease() {
    this.controller.setCongestionState(CongestionState.AVOID_CONGESTION);

    for (long i = 1; i <= 5; i++) {
      UnsignedLong amount = UnsignedLong.valueOf(i).times(ONE_K);
      controller.prepare(amount);
      controller.fulfill(amount);
      assertThat(controller.getMaxAmount(), is((ONE_K.times(UnsignedLong.valueOf(i)).plus(ONE_K))));
    }
  }

  @Test
  public void multiplicativeDecrease() {
    this.controller.setCongestionState(CongestionState.AVOID_CONGESTION);

    UnsignedLong amount = controller.getMaxAmount();
    controller.prepare(amount);
    controller.reject(amount, T04_INSUFFICIENT_LIQUIDITY);
    assertThat(controller.getMaxAmount(), is(UnsignedLong.valueOf(500L)));

    amount = controller.getMaxAmount();
    controller.prepare(amount);
    controller.reject(amount, T04_INSUFFICIENT_LIQUIDITY);
    assertThat(controller.getMaxAmount(), is(UnsignedLong.valueOf(250L)));

    amount = controller.getMaxAmount();
    controller.prepare(amount);
    controller.reject(amount, T04_INSUFFICIENT_LIQUIDITY);
    assertThat(controller.getMaxAmount(), is(UnsignedLong.valueOf(125L)));

    amount = controller.getMaxAmount();
    controller.prepare(amount);
    controller.reject(amount, T04_INSUFFICIENT_LIQUIDITY);
    assertThat(controller.getMaxAmount(), is(UnsignedLong.valueOf(62L)));

    amount = controller.getMaxAmount();
    controller.prepare(amount);
    controller.reject(amount, T04_INSUFFICIENT_LIQUIDITY);
    assertThat(controller.getMaxAmount(), is(UnsignedLong.valueOf(31L)));

    amount = controller.getMaxAmount();
    controller.prepare(amount);
    controller.reject(amount, T04_INSUFFICIENT_LIQUIDITY);
    assertThat(controller.getMaxAmount(), is(UnsignedLong.valueOf(15L)));

    amount = controller.getMaxAmount();
    controller.prepare(amount);
    controller.reject(amount, T04_INSUFFICIENT_LIQUIDITY);
    assertThat(controller.getMaxAmount(), is(UnsignedLong.valueOf(7L)));

    amount = controller.getMaxAmount();
    controller.prepare(amount);
    controller.reject(amount, T04_INSUFFICIENT_LIQUIDITY);
    assertThat(controller.getMaxAmount(), is(UnsignedLong.valueOf(3L)));

    amount = controller.getMaxAmount();
    controller.prepare(amount);
    controller.reject(amount, T04_INSUFFICIENT_LIQUIDITY);
    assertThat(controller.getMaxAmount(), is(UnsignedLong.valueOf(1L)));

    amount = controller.getMaxAmount();
    controller.prepare(amount);
    controller.reject(amount, T04_INSUFFICIENT_LIQUIDITY);
    assertThat(controller.getMaxAmount(), is(UnsignedLong.valueOf(1L)));

    amount = controller.getMaxAmount();
    controller.prepare(amount);
    controller.reject(amount, T04_INSUFFICIENT_LIQUIDITY);
    assertThat(controller.getMaxAmount(), is(UnsignedLong.valueOf(1L)));
  }

  @Test
  public void aimdCombined() {
    this.controller.setCongestionState(CongestionState.AVOID_CONGESTION);

    UnsignedLong amount = controller.getMaxAmount();
    controller.prepare(amount);
    controller.fulfill(amount);
    assertThat(controller.getMaxAmount(), is(UnsignedLong.valueOf(2000L)));

    amount = controller.getMaxAmount();
    controller.prepare(amount);
    controller.fulfill(amount);
    assertThat(controller.getMaxAmount(), is(UnsignedLong.valueOf(3000L)));

    amount = controller.getMaxAmount();
    controller.prepare(amount);
    controller.reject(amount, T04_INSUFFICIENT_LIQUIDITY);
    assertThat(controller.getMaxAmount(), is(UnsignedLong.valueOf(1500L)));

    amount = controller.getMaxAmount();
    controller.prepare(amount);
    controller.fulfill(amount);
    assertThat(controller.getMaxAmount(), is(UnsignedLong.valueOf(2500L)));
  }

  @Test
  public void maxPacketAmount() throws IOException {
    this.controller.setCongestionState(CongestionState.AVOID_CONGESTION);
    assertThat(controller.getMaxAmount(), is(ONE_K));

    controller.prepare(ONE_K);
    assertThat(controller.getMaxAmount(), is(UnsignedLong.ZERO));

    final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    StreamCodecContextFactory.oer().write(AmountTooLargeErrorData.builder()
            .receivedAmount(UnsignedLong.valueOf(100L))
            .maximumAmount(UnsignedLong.valueOf(10L))
            .build(),
        byteArrayOutputStream
    );
    controller.reject(ONE_K, InterledgerRejectPacket.builder()
        .code(InterledgerErrorCode.F08_AMOUNT_TOO_LARGE)
        .data(byteArrayOutputStream.toByteArray())
        .build());
    assertThat(controller.getMaxAmount(), is(UnsignedLong.valueOf(100L)));

    for (int i = 0; i < 100; i++) {
      UnsignedLong amount = controller.getMaxAmount();
      controller.prepare(amount);
      assertThat(controller.getMaxAmount(), is(UnsignedLong.valueOf(100L)));
      controller.fulfill(amount);
      assertThat(controller.getMaxAmount(), is(UnsignedLong.valueOf(100L)));
    }
  }

  @Test
  public void doesntOverflowU64WithAvoidCongestion() {
    this.controller = new AimdCongestionController(
        UnsignedLong.MAX_VALUE.minus(UnsignedLong.ONE),
        ONE_K,
        BigDecimal.valueOf(2L),
        StreamCodecContextFactory.oer()
    );
    this.controller.setCongestionState(CongestionState.AVOID_CONGESTION);
    assertThat(controller.getCongestionState(), is(CongestionState.AVOID_CONGESTION));

    UnsignedLong amount = controller.getMaxAmount();
    controller.prepare(amount);
    controller.fulfill(amount);
    assertThat(controller.getMaxAmount(), is(UnsignedLong.MAX_VALUE));

    controller.prepare(amount);
    controller.fulfill(amount);
    assertThat(controller.getMaxAmount(), is(UnsignedLong.MAX_VALUE));
  }

  @Test
  public void trackingAmountInFlight() {
    controller.setMaxPacketAmount(SIX_HUNDRED);
    assertThat(controller.getMaxPacketAmount(), is(Optional.of(SIX_HUNDRED)));

    controller.prepare(UnsignedLong.valueOf(100L));
    assertThat(controller.getMaxAmount(), is(SIX_HUNDRED));

    controller.prepare(SIX_HUNDRED);
    assertThat(controller.getMaxAmount(), is(UnsignedLong.valueOf(1000L - 600L - 100L)));
  }

  @Test(expected = NullPointerException.class)
  public void handleF08RejectionWithNullPrepareAmount() {
    try {
      controller.handleF08Rejection(null, mock(InterledgerRejectPacket.class));
      fail();
    } catch (NullPointerException e) {
      assertThat(e.getMessage(), is("prepareAmount must not be null"));
      throw e;
    }
  }

  // Scenarios To test
  //
  // data, no max
  // No data, no max

  // no data, max < prepare
  // no data, max == prepare
  // no data, max > prepare

  // data, max < prepare
  // data, max == prepare
  // data, max > prepare

  @Test
  public void handleF08RejectionWithNoData() {
    InterledgerRejectPacket rejectPacket = interledgerRejectPacket();
    assertThat(controller.handleF08Rejection(UnsignedLong.ZERO, rejectPacket), is(UnsignedLong.ONE));
    assertThat(controller.handleF08Rejection(UnsignedLong.ONE, rejectPacket), is(UnsignedLong.ONE));
    assertThat(controller.handleF08Rejection(UnsignedLong.valueOf(2L), rejectPacket), is(UnsignedLong.ONE));
    assertThat(
        controller.handleF08Rejection(UnsignedLong.valueOf(500L), rejectPacket),
        is(UnsignedLong.valueOf(250L))
    );
    assertThat(
        controller.handleF08Rejection(ONE_K, rejectPacket), is(UnsignedLong.valueOf(500L))
    );
    assertThat(
        controller.handleF08Rejection(UnsignedLong.MAX_VALUE, rejectPacket),
        is(UnsignedLong.MAX_VALUE.dividedBy(UnsignedLong.valueOf(2L)))
    );
  }

  @Test
  public void handleF08RejectionWithGarbageData() {
    InterledgerRejectPacket rejectPacket = InterledgerRejectPacket.builder().from(interledgerRejectPacket())
        .data(new byte[32]).build();

    assertThat(controller.handleF08Rejection(UnsignedLong.ZERO, rejectPacket), is(UnsignedLong.ONE));
    assertThat(controller.handleF08Rejection(UnsignedLong.ONE, rejectPacket), is(UnsignedLong.ONE));
    assertThat(controller.handleF08Rejection(UnsignedLong.valueOf(2L), rejectPacket), is(UnsignedLong.ONE));
    assertThat(
        controller.handleF08Rejection(UnsignedLong.valueOf(500L), rejectPacket),
        is(UnsignedLong.valueOf(250L))
    );
    assertThat(
        controller.handleF08Rejection(ONE_K, rejectPacket), is(UnsignedLong.valueOf(500L))
    );
    assertThat(
        controller.handleF08Rejection(UnsignedLong.MAX_VALUE, rejectPacket),
        is(UnsignedLong.MAX_VALUE.dividedBy(UnsignedLong.valueOf(2L)))
    );
  }

  @Test
  public void handleF08RejectionWithPrepareLessThanMax() {
    InterledgerRejectPacket rejectPacket = interledgerRejectPacket(Optional.of(
        AmountTooLargeErrorData.builder()
            .receivedAmount(UnsignedLong.valueOf(2L))
            .maximumAmount(UnsignedLong.valueOf(2L))
            .build()
    ));
    assertThat(controller.handleF08Rejection(UnsignedLong.ONE, rejectPacket), is(UnsignedLong.ONE));
  }

  @Test
  public void handleF08RejectionWithPrepareEqualToMax() {
    InterledgerRejectPacket rejectPacket = interledgerRejectPacket(Optional.of(
        AmountTooLargeErrorData.builder()
            .receivedAmount(UnsignedLong.valueOf(2L))
            .maximumAmount(UnsignedLong.valueOf(2L))
            .build()
    ));
    assertThat(controller.handleF08Rejection(UnsignedLong.ONE, rejectPacket), is(UnsignedLong.ONE));
  }

  @Test
  public void handleF08RejectionWithPrepareGreaterThanMax() {
    InterledgerRejectPacket rejectPacket = interledgerRejectPacket(Optional.of(
        AmountTooLargeErrorData.builder()
            .receivedAmount(UnsignedLong.valueOf(2L))
            .maximumAmount(UnsignedLong.valueOf(2L))
            .build()
    ));
    assertThat(controller.handleF08Rejection(ONE_K, rejectPacket), is(ONE_K));
  }

  @Test
  public void handleF08RejectionWithDivergentMaxReceived() {
    InterledgerRejectPacket rejectPacket = interledgerRejectPacket(Optional.of(
        AmountTooLargeErrorData.builder()
            .receivedAmount(ONE_K)
            .maximumAmount(UnsignedLong.valueOf(2L))
            .build()
    ));
    assertThat(controller.handleF08Rejection(ONE_K, rejectPacket), is(UnsignedLong.valueOf(2L)));
  }

  private InterledgerRejectPacket interledgerRejectPacket() {
    return interledgerRejectPacket(Optional.empty());
  }

  private InterledgerRejectPacket interledgerRejectPacket(
      final Optional<AmountTooLargeErrorData> amountTooLargeErrorData
  ) {
    Objects.requireNonNull(amountTooLargeErrorData);

    final byte[] data = amountTooLargeErrorData.map($ -> {
      final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
      try {
        StreamCodecContextFactory.oer().write($, byteArrayOutputStream);
      } catch (IOException e) {
        throw new RuntimeException(e.getMessage(), e);
      }
      return byteArrayOutputStream.toByteArray();
    }).orElse(new byte[0]);

    return InterledgerRejectPacket.builder()
        .triggeredBy(OPERATOR_ADRESS)
        .message("the error")
        .code(InterledgerErrorCode.F08_AMOUNT_TOO_LARGE)
        .data(data)
        .build();
  }

}
