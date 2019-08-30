package org.interledger.stream.congestion;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.fail;

import org.interledger.core.InterledgerErrorCode;
import org.interledger.core.InterledgerRejectPacket;
import org.interledger.stream.congestion.AimdCongestionController.CongestionState;

import com.google.common.primitives.UnsignedLong;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Unit tests for {@link AimdCongestionController}.
 */
public class AimdCongestionControllerTest {

  public static final UnsignedLong SIX_HUNDRED = UnsignedLong.valueOf(600L);
  private static final UnsignedLong ONE_K = UnsignedLong.valueOf(1000L);
  private static final InterledgerRejectPacket T04_INSUFFICIENT_LIQUIDITY = InterledgerRejectPacket.builder()
      .code(InterledgerErrorCode.T04_INSUFFICIENT_LIQUIDITY)
      .build();
  private AimdCongestionController controller;

  @Before
  public void setUp() {
    this.controller = new AimdCongestionController();
  }

  @Test(expected = NullPointerException.class)
  public void constructWithNullStartAmount() {
    try {
      new AimdCongestionController(null, UnsignedLong.ONE, BigDecimal.TEN);
      fail();
    } catch (NullPointerException e) {
      assertThat(e.getMessage(), is("startAmount must not be null"));
      throw e;
    }
  }

  @Test(expected = NullPointerException.class)
  public void constructWithNullIncreaseAmount() {
    try {
      new AimdCongestionController(UnsignedLong.ONE, null, BigDecimal.TEN);
      fail();
    } catch (NullPointerException e) {
      assertThat(e.getMessage(), is("increaseAmount must not be null"));
      throw e;
    }
  }

  @Test(expected = NullPointerException.class)
  public void constructWithNullDecreaseFactor() {
    try {
      new AimdCongestionController(UnsignedLong.ONE, UnsignedLong.ONE, null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e.getMessage(), is("decreaseFactor must not be null"));
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
        BigDecimal.valueOf(2L)
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
  public void maxPacketAmount() {
    this.controller.setCongestionState(CongestionState.AVOID_CONGESTION);
    assertThat(controller.getMaxAmount(), is(ONE_K));

    controller.prepare(ONE_K);
    controller.reject(ONE_K, InterledgerRejectPacket.builder()
        .code(InterledgerErrorCode.F08_AMOUNT_TOO_LARGE)
        // TODO: MaxPacketDetails.
        //.data()
        .build());

    assertThat(controller.getMaxAmount(), is(UnsignedLong.valueOf(100L)));

    UnsignedLong amount = controller.getMaxAmount();
    controller.prepare(amount);
    controller.fulfill(amount);
    assertThat(controller.getMaxAmount(), is(UnsignedLong.valueOf(100L)));

    amount = controller.getMaxAmount();
    controller.prepare(amount);
    controller.fulfill(amount);
    assertThat(controller.getMaxAmount(), is(UnsignedLong.valueOf(100L)));
  }

  @Test
  public void doesntOverflowU64WithAvoidCongestion() {
    this.controller = new AimdCongestionController(
        UnsignedLong.MAX_VALUE.minus(UnsignedLong.ONE),
        ONE_K,
        BigDecimal.valueOf(2L)
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
  public void minWithNullFirst() {
    try {
      controller.min(null, UnsignedLong.ONE);
      fail();
    } catch (NullPointerException e) {
      assertThat(e.getMessage(), is(nullValue()));
      throw e;
    }
  }

  @Test(expected = NullPointerException.class)
  public void minWithNullSecond() {
    try {
      controller.min(UnsignedLong.ONE, null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e.getMessage(), is(nullValue()));
      throw e;
    }
  }

  @Test
  public void minTests() {
    assertThat(controller.min(UnsignedLong.ZERO, UnsignedLong.ZERO), is(UnsignedLong.ZERO));
    assertThat(controller.min(UnsignedLong.ONE, UnsignedLong.ZERO), is(UnsignedLong.ZERO));
    assertThat(controller.min(UnsignedLong.ZERO, UnsignedLong.ONE), is(UnsignedLong.ZERO));
    assertThat(controller.min(UnsignedLong.MAX_VALUE, UnsignedLong.ZERO), is(UnsignedLong.ZERO));
    assertThat(controller.min(UnsignedLong.ZERO, UnsignedLong.MAX_VALUE), is(UnsignedLong.ZERO));
    assertThat(controller.min(UnsignedLong.ONE, UnsignedLong.MAX_VALUE), is(UnsignedLong.ONE));
    assertThat(controller.min(UnsignedLong.MAX_VALUE, UnsignedLong.ONE), is(UnsignedLong.ONE));
  }
}
