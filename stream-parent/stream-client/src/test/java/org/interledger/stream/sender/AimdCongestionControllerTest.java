package org.interledger.stream.sender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.interledger.codecs.stream.StreamCodecContextFactory;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerErrorCode;
import org.interledger.core.InterledgerRejectPacket;
import org.interledger.encoding.asn.framework.CodecContextFactory;
import org.interledger.stream.AmountTooLargeErrorData;
import org.interledger.stream.sender.AimdCongestionController.CongestionState;

import com.google.common.primitives.UnsignedLong;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;

/**
 * Unit tests for {@link AimdCongestionController}.
 */
public class AimdCongestionControllerTest {

  private static final UnsignedLong SIX_HUNDRED = UnsignedLong.valueOf(600L);
  private static final InterledgerAddress OPERATOR_ADRESS = InterledgerAddress.of("test.operator");
  private static final UnsignedLong ONE_K = UnsignedLong.valueOf(1000L);
  private static final InterledgerRejectPacket T04_INSUFFICIENT_LIQUIDITY = InterledgerRejectPacket.builder()
      .code(InterledgerErrorCode.T04_INSUFFICIENT_LIQUIDITY)
      .triggeredBy(OPERATOR_ADRESS)
      .message("the error message")
      .build();
  
  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  private AimdCongestionController controller;

  @Before
  public void setUp() {
    this.controller = new AimdCongestionController();
  }

  @Test
  public void constructWithNullStartAmount() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("startAmount must not be null");
    new AimdCongestionController(null, UnsignedLong.ONE, BigDecimal.TEN, CodecContextFactory.oer());
  }

  @Test
  public void constructWithNullIncreaseAmount() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("increaseAmount must not be null");
    new AimdCongestionController(UnsignedLong.ONE, null, BigDecimal.TEN, CodecContextFactory.oer());
  }

  @Test
  public void constructWithNullDecreaseFactor() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("decreaseFactor must not be null");
    new AimdCongestionController(UnsignedLong.ONE, UnsignedLong.ONE, null, CodecContextFactory.oer());
  }

  @Test
  public void constructWithNullCodecContext() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("streamCodecContext must not be null");
    new AimdCongestionController(UnsignedLong.ONE, UnsignedLong.ONE, BigDecimal.TEN, null);
  }

  @Test
  public void doublesMaxAmountOnFulfill() {
    UnsignedLong amount = controller.getMaxAmount();
    controller.prepare(amount);
    controller.fulfill(amount);
    assertThat(controller.getMaxAmount()).isEqualTo(UnsignedLong.valueOf(2000L));

    amount = controller.getMaxAmount();
    controller.prepare(amount);
    controller.fulfill(amount);
    assertThat(controller.getMaxAmount()).isEqualTo(UnsignedLong.valueOf(4000L));

    amount = controller.getMaxAmount();
    controller.prepare(amount);
    controller.fulfill(amount);
    assertThat(controller.getMaxAmount()).isEqualTo(UnsignedLong.valueOf(8000L));
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
    assertThat(controller.getCongestionState()).isEqualTo(CongestionState.SLOW_START);

    UnsignedLong amount = controller.getMaxAmount();
    controller.prepare(amount);
    controller.fulfill(amount);
    assertThat(controller.getMaxAmount()).isEqualTo(UnsignedLong.MAX_VALUE);

    controller.prepare(amount);
    controller.fulfill(amount);
    assertThat(controller.getMaxAmount()).isEqualTo(UnsignedLong.MAX_VALUE);
  }

  @Test
  public void additiveIncrease() {
    this.controller.setCongestionState(CongestionState.AVOID_CONGESTION);

    for (long i = 1; i <= 5; i++) {
      UnsignedLong amount = UnsignedLong.valueOf(i).times(ONE_K);
      controller.prepare(amount);
      controller.fulfill(amount);
      assertThat(controller.getMaxAmount()).isEqualTo((ONE_K.times(UnsignedLong.valueOf(i)).plus(ONE_K)));
    }
  }

  @Test
  public void multiplicativeDecrease() {
    this.controller.setCongestionState(CongestionState.AVOID_CONGESTION);

    UnsignedLong amount = controller.getMaxAmount();
    controller.prepare(amount);
    controller.reject(amount, T04_INSUFFICIENT_LIQUIDITY);
    assertThat(controller.getMaxAmount()).isEqualTo(UnsignedLong.valueOf(500L));

    amount = controller.getMaxAmount();
    controller.prepare(amount);
    controller.reject(amount, T04_INSUFFICIENT_LIQUIDITY);
    assertThat(controller.getMaxAmount()).isEqualTo(UnsignedLong.valueOf(250L));

    amount = controller.getMaxAmount();
    controller.prepare(amount);
    controller.reject(amount, T04_INSUFFICIENT_LIQUIDITY);
    assertThat(controller.getMaxAmount()).isEqualTo(UnsignedLong.valueOf(125L));

    amount = controller.getMaxAmount();
    controller.prepare(amount);
    controller.reject(amount, T04_INSUFFICIENT_LIQUIDITY);
    assertThat(controller.getMaxAmount()).isEqualTo(UnsignedLong.valueOf(62L));

    amount = controller.getMaxAmount();
    controller.prepare(amount);
    controller.reject(amount, T04_INSUFFICIENT_LIQUIDITY);
    assertThat(controller.getMaxAmount()).isEqualTo(UnsignedLong.valueOf(31L));

    amount = controller.getMaxAmount();
    controller.prepare(amount);
    controller.reject(amount, T04_INSUFFICIENT_LIQUIDITY);
    assertThat(controller.getMaxAmount()).isEqualTo(UnsignedLong.valueOf(15L));

    amount = controller.getMaxAmount();
    controller.prepare(amount);
    controller.reject(amount, T04_INSUFFICIENT_LIQUIDITY);
    assertThat(controller.getMaxAmount()).isEqualTo(UnsignedLong.valueOf(7L));

    amount = controller.getMaxAmount();
    controller.prepare(amount);
    controller.reject(amount, T04_INSUFFICIENT_LIQUIDITY);
    assertThat(controller.getMaxAmount()).isEqualTo(UnsignedLong.valueOf(3L));

    amount = controller.getMaxAmount();
    controller.prepare(amount);
    controller.reject(amount, T04_INSUFFICIENT_LIQUIDITY);
    assertThat(controller.getMaxAmount()).isEqualTo(UnsignedLong.valueOf(1L));

    amount = controller.getMaxAmount();
    controller.prepare(amount);
    controller.reject(amount, T04_INSUFFICIENT_LIQUIDITY);
    assertThat(controller.getMaxAmount()).isEqualTo(UnsignedLong.valueOf(1L));

    amount = controller.getMaxAmount();
    controller.prepare(amount);
    controller.reject(amount, T04_INSUFFICIENT_LIQUIDITY);
    assertThat(controller.getMaxAmount()).isEqualTo(UnsignedLong.valueOf(1L));
  }

  @Test
  public void aimdCombined() {
    this.controller.setCongestionState(CongestionState.AVOID_CONGESTION);

    UnsignedLong amount = controller.getMaxAmount();
    controller.prepare(amount);
    controller.fulfill(amount);
    assertThat(controller.getMaxAmount()).isEqualTo(UnsignedLong.valueOf(2000L));

    amount = controller.getMaxAmount();
    controller.prepare(amount);
    controller.fulfill(amount);
    assertThat(controller.getMaxAmount()).isEqualTo(UnsignedLong.valueOf(3000L));

    amount = controller.getMaxAmount();
    controller.prepare(amount);
    controller.reject(amount, T04_INSUFFICIENT_LIQUIDITY);
    assertThat(controller.getMaxAmount()).isEqualTo(UnsignedLong.valueOf(1500L));

    amount = controller.getMaxAmount();
    controller.prepare(amount);
    controller.fulfill(amount);
    assertThat(controller.getMaxAmount()).isEqualTo(UnsignedLong.valueOf(2500L));
  }

  @Test
  public void maxPacketAmount() throws IOException {
    this.controller.setCongestionState(CongestionState.AVOID_CONGESTION);
    assertThat(controller.getMaxAmount()).isEqualTo(ONE_K);

    controller.prepare(ONE_K);
    assertThat(controller.getMaxAmount()).isEqualTo(UnsignedLong.ZERO);

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
    assertThat(controller.getMaxAmount()).isEqualTo(UnsignedLong.valueOf(100L));

    for (int i = 0; i < 100; i++) {
      UnsignedLong amount = controller.getMaxAmount();
      controller.prepare(amount);
      assertThat(controller.getMaxAmount()).isEqualTo(UnsignedLong.valueOf(100L));
      controller.fulfill(amount);
      assertThat(controller.getMaxAmount()).isEqualTo(UnsignedLong.valueOf(100L));
    }
  }

  @Test
  public void hasInFlight() {
    controller.prepare(UnsignedLong.ONE);
    assertThat(controller.hasInFlight()).isTrue();
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
    assertThat(controller.getCongestionState()).isEqualTo(CongestionState.AVOID_CONGESTION);

    UnsignedLong amount = controller.getMaxAmount();
    controller.prepare(amount);
    controller.fulfill(amount);
    assertThat(controller.getMaxAmount()).isEqualTo(UnsignedLong.MAX_VALUE);

    controller.prepare(amount);
    controller.fulfill(amount);
    assertThat(controller.getMaxAmount()).isEqualTo(UnsignedLong.MAX_VALUE);
  }

  @Test
  public void trackingAmountInFlight() {
    controller.setMaxPacketAmount(SIX_HUNDRED);
    assertThat(controller.getMaxPacketAmount()).isEqualTo(Optional.of(SIX_HUNDRED));

    controller.prepare(UnsignedLong.valueOf(100L));
    assertThat(controller.getMaxAmount()).isEqualTo(SIX_HUNDRED);

    controller.prepare(SIX_HUNDRED);
    assertThat(controller.getMaxAmount()).isEqualTo(UnsignedLong.valueOf(1000L - 600L - 100L));
  }

  @Test
  public void handleF08RejectionWithNullPrepareAmount() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("prepareAmount must not be null");
    controller.handleF08Rejection(null, mock(InterledgerRejectPacket.class));
  }

  @Test
  public void handleF08RejectionNoDataMaxLessThanPrepare() {
    InterledgerRejectPacket rejectPacket = interledgerRejectPacket();
    controller.setMaxPacketAmount(UnsignedLong.valueOf(4L));
    // would compute to 5 by halving prepare, but controller is set to 4
    assertThat(controller.handleF08Rejection(UnsignedLong.valueOf(10L), rejectPacket))
        .isEqualTo(UnsignedLong.valueOf(4L));
  }

  @Test
  public void handleF08RejectionNoDataMaxEqualToPrepare() {
    InterledgerRejectPacket rejectPacket = interledgerRejectPacket();
    controller.setMaxPacketAmount(UnsignedLong.valueOf(5L));
    assertThat(controller.handleF08Rejection(UnsignedLong.valueOf(10L), rejectPacket))
        .isEqualTo(UnsignedLong.valueOf(5L));
  }

  @Test
  public void handleF08RejectionNoDataMaxGreaterThanPrepare() {
    InterledgerRejectPacket rejectPacket = interledgerRejectPacket();
    controller.setMaxPacketAmount(UnsignedLong.valueOf(6L));
    assertThat(controller.handleF08Rejection(UnsignedLong.valueOf(10L), rejectPacket))
        .isEqualTo(UnsignedLong.valueOf(5L));
  }

  @Test
  public void handleF08RejectionWithNoData() {
    InterledgerRejectPacket rejectPacket = interledgerRejectPacket();
    assertThat(controller.handleF08Rejection(UnsignedLong.ZERO, rejectPacket)).isEqualTo(UnsignedLong.ONE);
    assertThat(controller.handleF08Rejection(UnsignedLong.ONE, rejectPacket)).isEqualTo(UnsignedLong.ONE);
    assertThat(controller.handleF08Rejection(UnsignedLong.valueOf(2L), rejectPacket)).isEqualTo(UnsignedLong.ONE);
    assertThat(
        controller.handleF08Rejection(UnsignedLong.valueOf(500L), rejectPacket)).isEqualTo(UnsignedLong.valueOf(250L)
    );
    assertThat(
        controller.handleF08Rejection(ONE_K, rejectPacket)).isEqualTo(UnsignedLong.valueOf(500L)
    );
    assertThat(controller.handleF08Rejection(UnsignedLong.MAX_VALUE, rejectPacket))
        .isEqualTo(UnsignedLong.MAX_VALUE.dividedBy(UnsignedLong.valueOf(2L)));
  }

  @Test
  public void handleF08RejectionWithGarbageData() {
    InterledgerRejectPacket rejectPacket = InterledgerRejectPacket.builder().from(interledgerRejectPacket())
        .data(new byte[32]).build();

    assertThat(controller.handleF08Rejection(UnsignedLong.ZERO, rejectPacket)).isEqualTo(UnsignedLong.ONE);
    assertThat(controller.handleF08Rejection(UnsignedLong.ONE, rejectPacket)).isEqualTo(UnsignedLong.ONE);
    assertThat(controller.handleF08Rejection(UnsignedLong.valueOf(2L), rejectPacket)).isEqualTo(UnsignedLong.ONE);
    assertThat(
        controller.handleF08Rejection(UnsignedLong.valueOf(500L), rejectPacket)).isEqualTo(UnsignedLong.valueOf(250L)
    );
    assertThat(
        controller.handleF08Rejection(ONE_K, rejectPacket)).isEqualTo(UnsignedLong.valueOf(500L)
    );
    assertThat(controller.handleF08Rejection(UnsignedLong.MAX_VALUE, rejectPacket))
        .isEqualTo(UnsignedLong.MAX_VALUE.dividedBy(UnsignedLong.valueOf(2L)));
  }

  @Test
  public void handleF08RejectionWithPrepareLessThanMax() {
    InterledgerRejectPacket rejectPacket = interledgerRejectPacket(Optional.of(
        AmountTooLargeErrorData.builder()
            .receivedAmount(UnsignedLong.valueOf(2L))
            .maximumAmount(UnsignedLong.valueOf(2L))
            .build()
    ));
    assertThat(controller.handleF08Rejection(UnsignedLong.ONE, rejectPacket)).isEqualTo(UnsignedLong.ONE);
  }

  @Test
  public void handleF08RejectionWithPrepareEqualToMax() {
    InterledgerRejectPacket rejectPacket = interledgerRejectPacket(Optional.of(
        AmountTooLargeErrorData.builder()
            .receivedAmount(UnsignedLong.valueOf(2L))
            .maximumAmount(UnsignedLong.valueOf(2L))
            .build()
    ));
    assertThat(controller.handleF08Rejection(UnsignedLong.valueOf(2L), rejectPacket))
        .isEqualTo(UnsignedLong.valueOf(2L));
  }

  @Test
  public void handleF08RejectionWithPrepareGreaterThanMax() {
    InterledgerRejectPacket rejectPacket = interledgerRejectPacket(Optional.of(
        AmountTooLargeErrorData.builder()
            .receivedAmount(UnsignedLong.valueOf(2L))
            .maximumAmount(UnsignedLong.valueOf(2L))
            .build()
    ));
    assertThat(controller.handleF08Rejection(ONE_K, rejectPacket)).isEqualTo(ONE_K);
  }

  @Test
  public void handleF08RejectionWithPrepareGreaterThanMaxWithRoundingDownBelowPoint5() {
    InterledgerRejectPacket rejectPacket = interledgerRejectPacket(Optional.of(
        AmountTooLargeErrorData.builder()
            .receivedAmount(UnsignedLong.valueOf(3L))
            .maximumAmount(UnsignedLong.valueOf(2L))
            .build()
    ));
    assertThat(controller.handleF08Rejection(UnsignedLong.valueOf(10L), rejectPacket))
        .isEqualTo(UnsignedLong.valueOf(6L));
  }

  @Test
  public void handleF08RejectionWithPrepareGreaterThanMaxWithRoundingDownAbovePoint5() {
    InterledgerRejectPacket rejectPacket = interledgerRejectPacket(Optional.of(
        AmountTooLargeErrorData.builder()
            .receivedAmount(UnsignedLong.valueOf(7L))
            .maximumAmount(UnsignedLong.valueOf(2L))
            .build()
    ));
    assertThat(controller.handleF08Rejection(UnsignedLong.valueOf(13L), rejectPacket))
        .isEqualTo(UnsignedLong.valueOf(3L));
  }

  @Test
  public void handleF08RejectionWithDivergentMaxReceived() {
    InterledgerRejectPacket rejectPacket = interledgerRejectPacket(Optional.of(
        AmountTooLargeErrorData.builder()
            .receivedAmount(ONE_K)
            .maximumAmount(UnsignedLong.valueOf(2L))
            .build()
    ));
    assertThat(controller.handleF08Rejection(ONE_K, rejectPacket)).isEqualTo(UnsignedLong.valueOf(2L));
  }

  @Test
  public void handleF08RejectionDivideByZeroHalves() {
    InterledgerRejectPacket rejectPacket = interledgerRejectPacket(Optional.of(
        AmountTooLargeErrorData.builder()
            .receivedAmount(UnsignedLong.ZERO)
            .maximumAmount(UnsignedLong.valueOf(10L))
            .build()
    ));
    assertThat(controller.handleF08Rejection(ONE_K, rejectPacket)).isEqualTo(UnsignedLong.valueOf(500L));
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
