package org.interledger.stream.pay.trackers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.interledger.core.AmountTooLargeErrorData;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerErrorCode;
import org.interledger.core.InterledgerRejectPacket;
import org.interledger.stream.pay.trackers.MaxPacketAmountTracker.MaxPacketState;

import com.google.common.primitives.UnsignedLong;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.MockitoAnnotations;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Unit tests for {@link MaxPacketAmountTracker}.
 */
@SuppressWarnings( {"checkstyle:MissingJavadocMethod"})
public class MaxPacketAmountTrackerTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private MaxPacketAmountTracker maxPacketAmountTracker;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);

    this.maxPacketAmountTracker = new MaxPacketAmountTracker();
  }

  ////////////////////////
  // reduceMaxPacketAmount
  ////////////////////////

  @Test
  public void reduceMaxPacketAmountWithNullPacket() {
    expectedException.expect(NullPointerException.class);
    maxPacketAmountTracker.reduceMaxPacketAmount(null, UnsignedLong.ONE);
  }

  @Test
  public void reduceMaxPacketAmountWithNullSourceAmount() {
    expectedException.expect(NullPointerException.class);
    maxPacketAmountTracker.reduceMaxPacketAmount(mock(InterledgerRejectPacket.class), null);
  }

  @Test
  public void reduceMaxPacketAmountWithNoAmountTooLargeAndNonPositiveSource() {
    final InterledgerRejectPacket rejectPacket = InterledgerRejectPacket.builder()
      .code(InterledgerErrorCode.T00_INTERNAL_ERROR)
      .triggeredBy(InterledgerAddress.of("example.foo"))
      .build();
    maxPacketAmountTracker.reduceMaxPacketAmount(rejectPacket, UnsignedLong.ZERO);

    assertThat(maxPacketAmountTracker.getNoCapacityAvailable()).isTrue();
  }

  @Test
  public void reduceMaxPacketAmountWithNoAmountTooLargeAndMaxPacketAmountUnknown() {
    final AtomicBoolean adjustPathCapacityCalled = new AtomicBoolean();

    this.maxPacketAmountTracker = new MaxPacketAmountTracker() {
      @Override
      public MaxPacketAmount getMaxPacketAmount() {
        return MaxPacketAmount.builder()
          .maxPacketState(MaxPacketState.UnknownMax)
          .value(UnsignedLong.MAX_VALUE)
          .build();
      }

      @Override
      public synchronized void adjustPathCapacity(UnsignedLong ackAmount) {
        adjustPathCapacityCalled.set(true);
      }
    };

    final InterledgerRejectPacket rejectPacket = InterledgerRejectPacket.builder()
      .code(InterledgerErrorCode.T00_INTERNAL_ERROR)
      .triggeredBy(InterledgerAddress.of("example.foo"))
      .build();
    maxPacketAmountTracker.reduceMaxPacketAmount(rejectPacket, UnsignedLong.valueOf(2L));

    assertThat(adjustPathCapacityCalled.get()).isTrue();
    assertThat(maxPacketAmountTracker.getNoCapacityAvailable()).isFalse();
    assertThat(maxPacketAmountTracker.getMaxPacketAmount().maxPacketState()).isEqualTo(MaxPacketState.UnknownMax);
    assertThat(maxPacketAmountTracker.getMaxPacketAmount().value()).isEqualTo(UnsignedLong.MAX_VALUE);
    assertThat(maxPacketAmountTracker.getNextMaxPacketAmount()).isEqualTo(UnsignedLong.ONE);
  }

  @Test
  public void reduceMaxPacketAmountWithNoAmountTooLargeAndMaxPacketAmountImprecise() {
    final AtomicBoolean adjustPathCapacityCalled = new AtomicBoolean();
    this.maxPacketAmountTracker = new MaxPacketAmountTracker() {
      @Override
      public MaxPacketAmount getMaxPacketAmount() {
        return MaxPacketAmount.builder()
          .maxPacketState(MaxPacketState.ImpreciseMax)
          .value(UnsignedLong.MAX_VALUE)
          .build();
      }

      @Override
      public synchronized void adjustPathCapacity(UnsignedLong ackAmount) {
        adjustPathCapacityCalled.set(true);
      }
    };

    final InterledgerRejectPacket rejectPacket = InterledgerRejectPacket.builder()
      .code(InterledgerErrorCode.T00_INTERNAL_ERROR)
      .triggeredBy(InterledgerAddress.of("example.foo"))
      .build();
    maxPacketAmountTracker.reduceMaxPacketAmount(rejectPacket, UnsignedLong.valueOf(2L));

    assertThat(adjustPathCapacityCalled.get()).isTrue();
    assertThat(maxPacketAmountTracker.getNoCapacityAvailable()).isFalse();
    assertThat(maxPacketAmountTracker.getMaxPacketAmount().maxPacketState()).isEqualTo(MaxPacketState.ImpreciseMax);
    assertThat(maxPacketAmountTracker.getMaxPacketAmount().value()).isEqualTo(UnsignedLong.MAX_VALUE);
    assertThat(maxPacketAmountTracker.getNextMaxPacketAmount()).isEqualTo(UnsignedLong.valueOf("9223372036854775808"));
  }

  @Test
  public void reduceMaxPacketAmountWithNoAmountTooLargeAndMaxPacketAmountPrecise() {
    final AtomicBoolean adjustPathCapacityCalled = new AtomicBoolean();
    this.maxPacketAmountTracker = new MaxPacketAmountTracker() {
      @Override
      public MaxPacketAmount getMaxPacketAmount() {
        return MaxPacketAmount.builder()
          .maxPacketState(MaxPacketState.PreciseMax)
          .value(UnsignedLong.MAX_VALUE)
          .build();
      }

      @Override
      public synchronized void adjustPathCapacity(UnsignedLong ackAmount) {
        adjustPathCapacityCalled.set(true);
      }
    };

    final InterledgerRejectPacket rejectPacket = InterledgerRejectPacket.builder()
      .code(InterledgerErrorCode.T00_INTERNAL_ERROR)
      .triggeredBy(InterledgerAddress.of("example.foo"))
      .build();
    maxPacketAmountTracker.reduceMaxPacketAmount(rejectPacket, UnsignedLong.valueOf(2L));

    assertThat(adjustPathCapacityCalled.get()).isFalse();
    assertThat(maxPacketAmountTracker.getNoCapacityAvailable()).isFalse();
    assertThat(maxPacketAmountTracker.getMaxPacketAmount().maxPacketState()).isEqualTo(MaxPacketState.PreciseMax);
    assertThat(maxPacketAmountTracker.getMaxPacketAmount().value()).isEqualTo(UnsignedLong.MAX_VALUE);
    assertThat(maxPacketAmountTracker.getNextMaxPacketAmount()).isEqualTo(UnsignedLong.MAX_VALUE);
  }

  @Test
  public void reduceMaxPacketAmountWithAmountTooLargeErrorDataNotGreater() {
    final AtomicBoolean adjustPathCapacityCalled = new AtomicBoolean();
    this.maxPacketAmountTracker = new MaxPacketAmountTracker() {
      @Override
      public MaxPacketAmount getMaxPacketAmount() {
        return MaxPacketAmount.builder()
          .maxPacketState(MaxPacketState.PreciseMax)
          .value(UnsignedLong.MAX_VALUE)
          .build();
      }

      @Override
      public synchronized void adjustPathCapacity(UnsignedLong ackAmount) {
        adjustPathCapacityCalled.set(true);
      }
    };

    final InterledgerRejectPacket rejectPacket = InterledgerRejectPacket.builder()
      .code(InterledgerErrorCode.T00_INTERNAL_ERROR)
      .triggeredBy(InterledgerAddress.of("example.foo"))
      .typedData(AmountTooLargeErrorData.builder()
        .maximumAmount(UnsignedLong.ONE)
        .receivedAmount(UnsignedLong.ONE)
        .build())
      .build();
    maxPacketAmountTracker.reduceMaxPacketAmount(rejectPacket, UnsignedLong.valueOf(2L));

    assertThat(adjustPathCapacityCalled.get()).isFalse();
    assertThat(maxPacketAmountTracker.getNoCapacityAvailable()).isFalse();
    assertThat(maxPacketAmountTracker.getMaxPacketAmount().maxPacketState()).isEqualTo(MaxPacketState.PreciseMax);
    assertThat(maxPacketAmountTracker.getMaxPacketAmount().value()).isEqualTo(UnsignedLong.MAX_VALUE);
    assertThat(maxPacketAmountTracker.getNextMaxPacketAmount()).isEqualTo(UnsignedLong.MAX_VALUE);
  }

  @Test
  public void reduceMaxPacketAmountWithAmountTooLargeErrorDataTotalReceivedIsGreater() {
    final AtomicBoolean adjustPathCapacityCalled = new AtomicBoolean();
    this.maxPacketAmountTracker = new MaxPacketAmountTracker() {
      @Override
      public MaxPacketAmount getMaxPacketAmount() {
        return MaxPacketAmount.builder()
          .maxPacketState(MaxPacketState.PreciseMax)
          .value(UnsignedLong.MAX_VALUE)
          .build();
      }

      @Override
      public synchronized void adjustPathCapacity(UnsignedLong ackAmount) {
        adjustPathCapacityCalled.set(true);
      }
    };

    final InterledgerRejectPacket rejectPacket = InterledgerRejectPacket.builder()
      .code(InterledgerErrorCode.T00_INTERNAL_ERROR)
      .triggeredBy(InterledgerAddress.of("example.foo"))
      .typedData(AmountTooLargeErrorData.builder()
        .maximumAmount(UnsignedLong.ONE)
        .receivedAmount(UnsignedLong.MAX_VALUE)
        .build())
      .build();
    maxPacketAmountTracker.reduceMaxPacketAmount(rejectPacket, UnsignedLong.valueOf(2L));

    assertThat(adjustPathCapacityCalled.get()).isFalse();
    assertThat(maxPacketAmountTracker.getNoCapacityAvailable()).isTrue();
    assertThat(maxPacketAmountTracker.getMaxPacketAmount().maxPacketState()).isEqualTo(MaxPacketState.PreciseMax);
    assertThat(maxPacketAmountTracker.getMaxPacketAmount().value()).isEqualTo(UnsignedLong.MAX_VALUE);
    assertThat(maxPacketAmountTracker.getNextMaxPacketAmount()).isEqualTo(UnsignedLong.MAX_VALUE);
  }

  ////////////////////////
  // adjustPathCapacity
  ////////////////////////

  @Test
  public void adjustPathCapacity() {
    expectedException.expect(NullPointerException.class);
    maxPacketAmountTracker.adjustPathCapacity(null);
  }

  @Test
  public void adjustPathCapacityWithZeroAckUnknown() {
    this.maxPacketAmountTracker = new MaxPacketAmountTracker() {
      @Override
      public MaxPacketAmount getMaxPacketAmount() {
        return MaxPacketAmount.unknownMax();
      }
    };
    maxPacketAmountTracker.adjustPathCapacity(UnsignedLong.ZERO);

    assertThat(maxPacketAmountTracker.verifiedPathCapacity()).isEqualTo(UnsignedLong.ZERO);
    assertThat(maxPacketAmountTracker.getMaxPacketAmount().maxPacketState()).isEqualTo(MaxPacketState.UnknownMax);
    assertThat(maxPacketAmountTracker.getMaxPacketAmount().value()).isEqualTo(UnsignedLong.MAX_VALUE);
    assertThat(maxPacketAmountTracker.getNextMaxPacketAmount()).isEqualTo(UnsignedLong.MAX_VALUE);
  }

  @Test
  public void adjustPathCapacityWithZeroAckImpreciseWithNewPathCapacity() {
    this.maxPacketAmountTracker = new MaxPacketAmountTracker() {
      @Override
      public MaxPacketAmount getMaxPacketAmount() {
        return MaxPacketAmount.builder()
          .maxPacketState(MaxPacketState.ImpreciseMax)
          .value(UnsignedLong.valueOf(5L))
          .build();
      }
    };
    maxPacketAmountTracker.adjustPathCapacity(UnsignedLong.valueOf(4L));

    assertThat(maxPacketAmountTracker.verifiedPathCapacity()).isEqualTo(UnsignedLong.valueOf(4L));
    assertThat(maxPacketAmountTracker.getMaxPacketAmount().maxPacketState()).isEqualTo(MaxPacketState.ImpreciseMax);
    assertThat(maxPacketAmountTracker.getMaxPacketAmount().value()).isEqualTo(UnsignedLong.valueOf(5L));
    assertThat(maxPacketAmountTracker.getNextMaxPacketAmount()).isEqualTo(UnsignedLong.valueOf(5L));
  }

  @Test
  public void adjustPathCapacityWithZeroAckPrecise() {
    this.maxPacketAmountTracker = new MaxPacketAmountTracker() {
      @Override
      public MaxPacketAmount getMaxPacketAmount() {
        return MaxPacketAmount.builder()
          .maxPacketState(MaxPacketState.PreciseMax)
          .value(UnsignedLong.valueOf(2L))
          .build();
      }
    };
    maxPacketAmountTracker.adjustPathCapacity(UnsignedLong.ZERO);

    assertThat(maxPacketAmountTracker.verifiedPathCapacity()).isEqualTo(UnsignedLong.ZERO);
    assertThat(maxPacketAmountTracker.getMaxPacketAmount().maxPacketState()).isEqualTo(MaxPacketState.PreciseMax);
    assertThat(maxPacketAmountTracker.getMaxPacketAmount().value()).isEqualTo(UnsignedLong.valueOf(2L));
    assertThat(maxPacketAmountTracker.getNextMaxPacketAmount()).isEqualTo(UnsignedLong.MAX_VALUE);
  }

  ////////////////////////
  // getMaxPacketAmount
  ////////////////////////

  @Test
  public void getMaxPacketAmount() {
    assertThat(maxPacketAmountTracker.getMaxPacketAmount().maxPacketState()).isEqualTo(MaxPacketState.UnknownMax);
    assertThat(maxPacketAmountTracker.getMaxPacketAmount().value()).isEqualTo(UnsignedLong.MAX_VALUE);
  }

  ////////////////////////
  // getNoCapacityAvailable
  ////////////////////////

  @Test
  public void getNoCapacityAvailable() {
    assertThat(maxPacketAmountTracker.getNoCapacityAvailable()).isFalse();
  }

  ////////////////////////
  // verifiedPathCapacity
  ////////////////////////

  @Test
  public void verifiedPathCapacity() {
    assertThat(maxPacketAmountTracker.verifiedPathCapacity()).isEqualTo(UnsignedLong.ZERO);
  }

  ////////////////////////
  // getNextMaxPacketAmount
  ////////////////////////

  @Test
  public void getNextMaxPacketAmount() {
    assertThat(maxPacketAmountTracker.getNextMaxPacketAmount()).isEqualTo(UnsignedLong.MAX_VALUE);
  }
}