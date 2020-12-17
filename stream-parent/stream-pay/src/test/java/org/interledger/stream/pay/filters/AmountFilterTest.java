package org.interledger.stream.pay.filters;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.interledger.core.fluent.Ratio;
import org.interledger.stream.frames.ErrorCodes;
import org.interledger.stream.frames.StreamMoneyFrame;
import org.interledger.stream.pay.filters.chain.StreamPacketFilterChain;
import org.interledger.stream.pay.model.ModifiableStreamPacketRequest;
import org.interledger.stream.pay.model.SendState;
import org.interledger.stream.pay.model.StreamPacketReply;
import org.interledger.stream.pay.probing.model.ExchangeRateBound;
import org.interledger.stream.pay.probing.model.PaymentTargetConditions;
import org.interledger.stream.pay.probing.model.PaymentTargetConditions.PaymentType;
import org.interledger.stream.pay.trackers.AmountTracker;
import org.interledger.stream.pay.trackers.ExchangeRateTracker;
import org.interledger.stream.pay.trackers.MaxPacketAmountTracker;
import org.interledger.stream.pay.trackers.MaxPacketAmountTracker.MaxPacketAmount;
import org.interledger.stream.pay.trackers.PaymentSharedStateTracker;

import com.google.common.primitives.UnsignedLong;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Optional;

/**
 * Unit test for {@link AmountFilter}.
 */
public class AmountFilterTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Mock
  private AmountTracker amountTrackerMock;

  @Mock
  private MaxPacketAmountTracker maxPacketAmountTrackerMock;

  @Mock
  private ExchangeRateTracker exchangeRateTrackerMock;

  @Mock
  private PaymentSharedStateTracker paymentSharedStateTrackerMock;

  private AmountFilter amountFilter;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);

    // All Happy-path settings.
    this.initializeHappyPath();

    when(paymentSharedStateTrackerMock.getAmountTracker()).thenReturn(amountTrackerMock);
    when(paymentSharedStateTrackerMock.getMaxPacketAmountTracker()).thenReturn(maxPacketAmountTrackerMock);
    when(paymentSharedStateTrackerMock.getExchangeRateTracker()).thenReturn(exchangeRateTrackerMock);

    this.amountFilter = new AmountFilter(paymentSharedStateTrackerMock);
  }

  ////////////
  // nextState
  ////////////

  @Test
  public void nextState() {
    expectedException.expect(NullPointerException.class);
    amountFilter.nextState(null);
  }

  @Test
  public void nextStateWithProtocolViolation() {
    when(amountTrackerMock.encounteredProtocolViolation()).thenReturn(true);

    ModifiableStreamPacketRequest request = ModifiableStreamPacketRequest.create().setSourceAmount(
      UnsignedLong.valueOf(10L)
    );
    SendState result = amountFilter.nextState(request);
    assertThat(result).isEqualTo(SendState.ReceiverProtocolViolation);
    assertThat(request.streamErrorCodeForConnectionClose()).isEqualTo(ErrorCodes.ProtocolViolation);
    assertThat(request.requestFrames().stream()).isEmpty();
  }

  @Test
  public void nextStateWithNoPaymentTarget() {
    when(amountTrackerMock.getPaymentTargetConditions()).thenReturn(Optional.empty());

    ModifiableStreamPacketRequest request = ModifiableStreamPacketRequest.create().setSourceAmount(
      UnsignedLong.valueOf(10L)
    );
    SendState result = amountFilter.nextState(request);
    assertThat(result).isEqualTo(SendState.Ready);
    assertThat(request.streamErrorCodeForConnectionClose()).isEqualTo(ErrorCodes.NoError);
    assertThat(request.requestFrames().stream()).isEmpty();
  }

  @Test
  public void nextStateWithIncompatibleReceiveMax() {
    this.amountFilter = new AmountFilter(paymentSharedStateTrackerMock) {
      @Override
      protected boolean checkForIncompatibleReceiveMax(
        AmountTracker amountTracker, PaymentTargetConditions target
      ) {
        return true;
      }
    };

    ModifiableStreamPacketRequest request = ModifiableStreamPacketRequest.create();
    SendState result = amountFilter.nextState(request);
    assertThat(result).isEqualTo(SendState.IncompatibleReceiveMax);
    assertThat(request.streamErrorCodeForConnectionClose()).isEqualTo(ErrorCodes.ApplicationError);
    assertThat(request.requestFrames().stream()).isEmpty();
  }

  @Test
  public void nextStateWithPaidFixedSend() {
    this.amountFilter = new AmountFilter(paymentSharedStateTrackerMock) {
      @Override
      protected boolean checkIfFixedSendPaymentIsComplete(AmountTracker amountTracker, PaymentTargetConditions target) {
        return true;
      }
    };

    ModifiableStreamPacketRequest request = ModifiableStreamPacketRequest.create();
    SendState result = amountFilter.nextState(request);
    assertThat(result).isEqualTo(SendState.End);
    assertThat(request.streamErrorCodeForConnectionClose()).isEqualTo(ErrorCodes.NoError);
    assertThat(request.requestFrames().stream()).isEmpty();
  }

  // anyInFlight=false; anyAvailableToSend=false
  // anyInFlight=false; anyAvailableToSend=true
  // anyInFlight=true; anyAvailableToSend=false
  // anyInFlight=true; anyAvailableToSend=true

  @Test
  public void nextStateWithAnyInFlightFalseAndAnyToSendFalse() {
    this.amountFilter = new AmountFilter(paymentSharedStateTrackerMock) {
      @Override
      protected BigInteger computeAmountAvailableToSend(PaymentTargetConditions target) {
        return BigInteger.ZERO; // <-- availableToSend == false
      }
    };
    when(amountTrackerMock.getSourceAmountInFlight()).thenReturn(BigInteger.ZERO); // <-- anyInFlight = false

    final ModifiableStreamPacketRequest request = ModifiableStreamPacketRequest.create();
    SendState result = amountFilter.nextState(request);
    assertThat(result).isEqualTo(SendState.End);
    assertThat(request.streamErrorCodeForConnectionClose()).isEqualTo(ErrorCodes.NoError);
    assertThat(request.requestFrames().stream()).isEmpty();
  }

  @Test
  public void nextStateWithAnyInFlightFalseAndAnyToSendTrue() {
    this.amountFilter = new AmountFilter(paymentSharedStateTrackerMock) {
      @Override
      protected BigInteger computeAmountAvailableToSend(PaymentTargetConditions target) {
        return BigInteger.ONE; // <-- availableToSend = true
      }
    };
    when(amountTrackerMock.getSourceAmountInFlight()).thenReturn(BigInteger.ZERO); // <-- anyInFlight = false

    final ModifiableStreamPacketRequest request = ModifiableStreamPacketRequest.create();
    SendState result = amountFilter.nextState(request);
    assertThat(result).isEqualTo(SendState.Ready);
    assertThat(request.streamErrorCodeForConnectionClose()).isEqualTo(ErrorCodes.NoError);
    assertThat(request.requestFrames().stream()
      .filter(frame -> frame instanceof StreamMoneyFrame)
      .findAny()).isPresent();
  }

  @Test
  public void nextStateWithAnyInFlightTrueAndAnyToSendFalse() {
    this.amountFilter = new AmountFilter(paymentSharedStateTrackerMock) {
      @Override
      protected BigInteger computeAmountAvailableToSend(PaymentTargetConditions target) {
        return BigInteger.ZERO; // <-- availableToSend = false
      }
    };
    when(amountTrackerMock.getSourceAmountInFlight()).thenReturn(BigInteger.ONE); // <-- anyInFlight = true

    final ModifiableStreamPacketRequest request = ModifiableStreamPacketRequest.create();
    SendState result = amountFilter.nextState(request);
    assertThat(result).isEqualTo(SendState.Wait);
    assertThat(request.streamErrorCodeForConnectionClose()).isEqualTo(ErrorCodes.NoError);
    assertThat(request.requestFrames().stream()).isEmpty();
  }

  @Test
  public void nextStateWithAnyInFlightTrueAndAnyToSendTrue() {
    this.amountFilter = new AmountFilter(paymentSharedStateTrackerMock) {
      @Override
      protected BigInteger computeAmountAvailableToSend(PaymentTargetConditions target) {
        return BigInteger.ONE; // <-- availableToSend = true
      }
    };
    when(amountTrackerMock.getSourceAmountInFlight()).thenReturn(BigInteger.ONE); // <-- anyInFlight = true

    final ModifiableStreamPacketRequest request = ModifiableStreamPacketRequest.create();
    SendState result = amountFilter.nextState(request);
    assertThat(result).isEqualTo(SendState.Ready);
    assertThat(request.streamErrorCodeForConnectionClose()).isEqualTo(ErrorCodes.NoError);
    assertThat(request.requestFrames().stream()
      .filter(frame -> frame instanceof StreamMoneyFrame)
      .findAny()).isPresent();
  }

  @Test
  public void nextStateWithFixedDeliveryAndNoMoreToBeDelivered() {
    this.amountFilter = new AmountFilter(paymentSharedStateTrackerMock) {
      @Override
      protected boolean checkIfFixedDeliveryPaymentIsComplete(
        AmountTracker amountTracker, PaymentTargetConditions target
      ) {
        return true;
      }
    };

    when(amountTrackerMock.getPaymentTargetConditions()).thenReturn(Optional.of(
      PaymentTargetConditions.builder()
        .paymentType(PaymentType.FIXED_DELIVERY)
        .minExchangeRate(BigDecimal.ONE)
        .minDeliveryAmount(BigInteger.ONE)
        .maxSourceAmount(BigInteger.TEN)
        .build()
    ));

    final ModifiableStreamPacketRequest request = ModifiableStreamPacketRequest.create();
    SendState result = amountFilter.nextState(request);
    assertThat(result).isEqualTo(SendState.End);
    assertThat(request.streamErrorCodeForConnectionClose()).isEqualTo(ErrorCodes.NoError);
    assertThat(request.requestFrames().stream()).isEmpty();
  }

  @Test
  public void nextStateWithFixedDeliveryAndNoMoreAvailable() {
    this.amountFilter = new AmountFilter(paymentSharedStateTrackerMock) {
      @Override
      protected boolean checkIfFixedDeliveryPaymentIsComplete(
        AmountTracker amountTracker, PaymentTargetConditions target
      ) {
        return false;
      }

      @Override
      protected boolean moreAvailableToDeliver(
        AmountTracker amountTracker, PaymentTargetConditions target
      ) {
        return false;
      }
    };

    when(amountTrackerMock.getPaymentTargetConditions()).thenReturn(Optional.of(
      PaymentTargetConditions.builder()
        .paymentType(PaymentType.FIXED_DELIVERY)
        .minExchangeRate(BigDecimal.ONE)
        .minDeliveryAmount(BigInteger.ONE)
        .maxSourceAmount(BigInteger.TEN)
        .build()
    ));

    final ModifiableStreamPacketRequest request = ModifiableStreamPacketRequest.create();
    SendState result = amountFilter.nextState(request);
    assertThat(result).isEqualTo(SendState.Wait);
    assertThat(request.streamErrorCodeForConnectionClose()).isEqualTo(ErrorCodes.NoError);
    assertThat(request.requestFrames().stream()).isEmpty();
  }

  @Test
  public void nextStateWithFixedDeliveryAndInvalidDeliveryLimit() {
    this.amountFilter = new AmountFilter(paymentSharedStateTrackerMock) {
      @Override
      protected boolean checkIfFixedDeliveryPaymentIsComplete(
        AmountTracker amountTracker, PaymentTargetConditions target
      ) {
        return false;
      }

      @Override
      protected boolean moreAvailableToDeliver(
        AmountTracker amountTracker, PaymentTargetConditions target
      ) {
        return true;
      }

      @Override
      protected boolean isSourceAmountDeliveryLimitInvalid(
        AmountTracker amountTracker, PaymentTargetConditions target
      ) {
        return true;
      }
    };

    when(amountTrackerMock.getPaymentTargetConditions()).thenReturn(Optional.of(
      PaymentTargetConditions.builder()
        .paymentType(PaymentType.FIXED_DELIVERY)
        .minExchangeRate(BigDecimal.ONE)
        .minDeliveryAmount(BigInteger.ONE)
        .maxSourceAmount(BigInteger.TEN)
        .build()
    ));

    final ModifiableStreamPacketRequest request = ModifiableStreamPacketRequest.create();
    SendState result = amountFilter.nextState(request);
    assertThat(result).isEqualTo(SendState.InsufficientExchangeRate);
    assertThat(request.streamErrorCodeForConnectionClose()).isEqualTo(ErrorCodes.NoError);
    assertThat(request.requestFrames().stream()).isEmpty();
  }

  @Test
  public void nextStateWithFixedDeliveryAndUpdatedSourceAmountWhenDeliveryLimitIsHigher() {
    this.amountFilter = new AmountFilter(paymentSharedStateTrackerMock) {

      @Override
      protected boolean checkIfFixedDeliveryPaymentIsComplete(
        AmountTracker amountTracker, PaymentTargetConditions target
      ) {
        return false;
      }

      @Override
      protected boolean moreAvailableToDeliver(
        AmountTracker amountTracker, PaymentTargetConditions target
      ) {
        return true;
      }

      @Override
      protected BigInteger computeAmountAvailableToSend(PaymentTargetConditions target) {
        return BigInteger.ONE;
      }

      @Override
      protected UnsignedLong computeSourceAmountDeliveryLimit(
        AmountTracker amountTracker, PaymentTargetConditions target
      ) {
        return UnsignedLong.valueOf(10L);
      }
    };

    when(amountTrackerMock.getPaymentTargetConditions()).thenReturn(Optional.of(
      PaymentTargetConditions.builder()
        .paymentType(PaymentType.FIXED_DELIVERY)
        .minExchangeRate(BigDecimal.ONE)
        .minDeliveryAmount(BigInteger.ONE)
        .maxSourceAmount(BigInteger.TEN)
        .build()
    ));

    final ModifiableStreamPacketRequest request = ModifiableStreamPacketRequest.create();
    SendState result = amountFilter.nextState(request);
    assertThat(result).isEqualTo(SendState.Ready);
    assertThat(request.streamErrorCodeForConnectionClose()).isEqualTo(ErrorCodes.NoError);
    assertThat(request.sourceAmount()).isEqualTo(UnsignedLong.valueOf(1L));
    assertThat(request.requestFrames().stream()
      .filter(frame -> frame instanceof StreamMoneyFrame)
      .findAny()).isPresent();
  }

  @Test
  public void nextStateWithFixedDeliveryAndUpdatedSourceAmountWhenDeliveryLimitIsEqual() {
    this.amountFilter = new AmountFilter(paymentSharedStateTrackerMock) {

      @Override
      protected boolean checkIfFixedDeliveryPaymentIsComplete(
        AmountTracker amountTracker, PaymentTargetConditions target
      ) {
        return false;
      }

      @Override
      protected boolean moreAvailableToDeliver(
        AmountTracker amountTracker, PaymentTargetConditions target
      ) {
        return true;
      }

      @Override
      protected BigInteger computeAmountAvailableToSend(PaymentTargetConditions target) {
        return BigInteger.valueOf(10L);
      }

      @Override
      protected UnsignedLong computeSourceAmountDeliveryLimit(
        AmountTracker amountTracker, PaymentTargetConditions target
      ) {
        return UnsignedLong.valueOf(10L);
      }
    };

    when(amountTrackerMock.getPaymentTargetConditions()).thenReturn(Optional.of(
      PaymentTargetConditions.builder()
        .paymentType(PaymentType.FIXED_DELIVERY)
        .minExchangeRate(BigDecimal.ONE)
        .minDeliveryAmount(BigInteger.ONE)
        .maxSourceAmount(BigInteger.TEN)
        .build()
    ));

    final ModifiableStreamPacketRequest request = ModifiableStreamPacketRequest.create();
    SendState result = amountFilter.nextState(request);
    assertThat(result).isEqualTo(SendState.Ready);
    assertThat(request.streamErrorCodeForConnectionClose()).isEqualTo(ErrorCodes.NoError);
    assertThat(request.sourceAmount()).isEqualTo(UnsignedLong.valueOf(10L));
    assertThat(request.requestFrames().stream()
      .filter(frame -> frame instanceof StreamMoneyFrame)
      .findAny()).isPresent();
  }

  @Test
  public void nextStateWithFixedDeliveryAndUpdatedSourceAmountWhenDeliveryLimitIsLessThan() {
    this.amountFilter = new AmountFilter(paymentSharedStateTrackerMock) {

      @Override
      protected boolean checkIfFixedDeliveryPaymentIsComplete(
        AmountTracker amountTracker, PaymentTargetConditions target
      ) {
        return false;
      }

      @Override
      protected boolean moreAvailableToDeliver(
        AmountTracker amountTracker, PaymentTargetConditions target
      ) {
        return true;
      }

      @Override
      protected BigInteger computeAmountAvailableToSend(PaymentTargetConditions target) {
        return BigInteger.valueOf(10L);
      }

      @Override
      protected UnsignedLong computeSourceAmountDeliveryLimit(
        AmountTracker amountTracker, PaymentTargetConditions target
      ) {
        return UnsignedLong.ONE;
      }
    };

    when(amountTrackerMock.getPaymentTargetConditions()).thenReturn(Optional.of(
      PaymentTargetConditions.builder()
        .paymentType(PaymentType.FIXED_DELIVERY)
        .minExchangeRate(BigDecimal.ONE)
        .minDeliveryAmount(BigInteger.ONE)
        .maxSourceAmount(BigInteger.TEN)
        .build()
    ));

    final ModifiableStreamPacketRequest request = ModifiableStreamPacketRequest.create();
    SendState result = amountFilter.nextState(request);
    assertThat(result).isEqualTo(SendState.Ready);
    assertThat(request.streamErrorCodeForConnectionClose()).isEqualTo(ErrorCodes.NoError);
    assertThat(request.sourceAmount()).isEqualTo(UnsignedLong.valueOf(1L));
    assertThat(request.requestFrames().stream()
      .filter(frame -> frame instanceof StreamMoneyFrame)
      .findAny()).isPresent();
  }

  @Test
  public void nextStateWithDeliveryDeficitWillNotCompletePayment() {
    this.amountFilter = new AmountFilter(paymentSharedStateTrackerMock) {

      @Override
      protected boolean checkIfFixedDeliveryPaymentIsComplete(
        AmountTracker amountTracker, PaymentTargetConditions target
      ) {
        return false;
      }

      @Override
      protected boolean moreAvailableToDeliver(
        AmountTracker amountTracker, PaymentTargetConditions target
      ) {
        return true;
      }

      @Override
      protected BigInteger computeAmountAvailableToSend(PaymentTargetConditions target) {
        return BigInteger.ONE;
      }

      @Override
      protected UnsignedLong computeSourceAmountDeliveryLimit(
        AmountTracker amountTracker, PaymentTargetConditions target
      ) {
        return UnsignedLong.ONE;
      }

      @Override
      protected UnsignedLong computeDeliveryDeficit(UnsignedLong minDestinationAmount,
        UnsignedLong estimatedDestinationAmount) {
        return UnsignedLong.ONE; // <-- So that paymentComplete inspection is triggered.
      }

      @Override
      protected boolean willPaymentComplete(AmountTracker amountTracker, PaymentTargetConditions target,
        BigInteger availableToSend, UnsignedLong sourceAmount, UnsignedLong estimatedDestinationAmount) {
        return false;
      }
    };

    final ModifiableStreamPacketRequest request = ModifiableStreamPacketRequest.create();
    SendState result = amountFilter.nextState(request);
    assertThat(result).isEqualTo(SendState.InsufficientExchangeRate);
    assertThat(request.streamErrorCodeForConnectionClose()).isEqualTo(ErrorCodes.NoError);
    assertThat(request.requestFrames().stream()).isEmpty();
  }

  @Test
  public void nextStateWithDeliveryDeficitShortfallLessThanDeficit() {
    when(amountTrackerMock.getAvailableDeliveryShortfall()).thenReturn(BigInteger.ZERO);
    this.amountFilter = new AmountFilter(paymentSharedStateTrackerMock) {

      @Override
      protected boolean checkIfFixedDeliveryPaymentIsComplete(
        AmountTracker amountTracker, PaymentTargetConditions target
      ) {
        return false;
      }

      @Override
      protected boolean moreAvailableToDeliver(
        AmountTracker amountTracker, PaymentTargetConditions target
      ) {
        return true;
      }

      @Override
      protected BigInteger computeAmountAvailableToSend(PaymentTargetConditions target) {
        return BigInteger.ONE;
      }

      @Override
      protected UnsignedLong computeSourceAmountDeliveryLimit(
        AmountTracker amountTracker, PaymentTargetConditions target
      ) {
        return UnsignedLong.ONE;
      }

      @Override
      protected UnsignedLong computeDeliveryDeficit(UnsignedLong minDestinationAmount,
        UnsignedLong estimatedDestinationAmount) {
        return UnsignedLong.ONE; // <-- So that paymentComplete inspection is triggered.
      }

      @Override
      protected boolean willPaymentComplete(AmountTracker amountTracker, PaymentTargetConditions target,
        BigInteger availableToSend, UnsignedLong sourceAmount, UnsignedLong estimatedDestinationAmount) {
        return true;
      }
    };

    final ModifiableStreamPacketRequest request = ModifiableStreamPacketRequest.create();
    SendState result = amountFilter.nextState(request);
    assertThat(result).isEqualTo(SendState.InsufficientExchangeRate);
    assertThat(request.streamErrorCodeForConnectionClose()).isEqualTo(ErrorCodes.NoError);
    assertThat(request.requestFrames().stream()).isEmpty();
  }

  @Test
  public void testNextStateWithPositiveDeliveryDeficitButValidPayment() {
    when(amountTrackerMock.getAvailableDeliveryShortfall()).thenReturn(BigInteger.ONE);
    this.amountFilter = new AmountFilter(paymentSharedStateTrackerMock) {

      @Override
      protected boolean checkIfFixedDeliveryPaymentIsComplete(
        AmountTracker amountTracker, PaymentTargetConditions target
      ) {
        return false;
      }

      @Override
      protected boolean moreAvailableToDeliver(
        AmountTracker amountTracker, PaymentTargetConditions target
      ) {
        return true;
      }

      @Override
      protected BigInteger computeAmountAvailableToSend(PaymentTargetConditions target) {
        return BigInteger.ONE;
      }

      @Override
      protected UnsignedLong computeEstimatedDestinationAmount(UnsignedLong sourceAmount) {
        return UnsignedLong.MAX_VALUE;
      }

      @Override
      protected UnsignedLong computeSourceAmountDeliveryLimit(
        AmountTracker amountTracker, PaymentTargetConditions target
      ) {
        return UnsignedLong.ONE;
      }

      @Override
      protected UnsignedLong computeDeliveryDeficit(UnsignedLong minDestinationAmount,
        UnsignedLong estimatedDestinationAmount) {
        return UnsignedLong.ONE; // <-- So that paymentComplete inspection is triggered.
      }

      @Override
      protected boolean willPaymentComplete(AmountTracker amountTracker, PaymentTargetConditions target,
        BigInteger availableToSend, UnsignedLong sourceAmount, UnsignedLong estimatedDestinationAmount) {
        return true;
      }
    };

    final ModifiableStreamPacketRequest request = ModifiableStreamPacketRequest.create();
    SendState result = amountFilter.nextState(request);
    assertThat(result).isEqualTo(SendState.Ready);
    assertThat(request.streamErrorCodeForConnectionClose()).isEqualTo(ErrorCodes.NoError);
    assertThat(request.sourceAmountIsSet()).isTrue();
    assertThat(request.sourceAmount()).isEqualTo(UnsignedLong.ONE);
    assertThat(request.minDestinationAmount()).isEqualTo(UnsignedLong.MAX_VALUE);
    assertThat(request.requestFrames().stream()
      .filter(frame -> frame instanceof StreamMoneyFrame)
      .findAny()).isPresent();
  }

  ////////////
  // doFilter
  ////////////

  @Test
  public void doFilter() {

    this.amountFilter = new AmountFilter(paymentSharedStateTrackerMock) {
    };

    final ModifiableStreamPacketRequest request = ModifiableStreamPacketRequest.create();
    final StreamPacketFilterChain streamPacketFilterChain = mock(StreamPacketFilterChain.class);

    StreamPacketReply reply = amountFilter.doFilter(request, streamPacketFilterChain);

    assertThat(reply.exception()).isNotPresent();
    assertThat(reply.interledgerPreparePacket()).isNotPresent();
    assertThat(reply.isAuthentic()).isTrue();
    assertThat(reply.isFulfill()).isFalse();
    assertThat(reply.isReject()).isFalse();
  }

  ////////////
  // updateReceiveMax
  ////////////

  // TODO
  @Test
  public void updateReceiveMax() {
  }

  ////////////
  // checkForIncompatibleReceiveMax
  ////////////

  @Test
  public void testCheckForIncompatibleReceiveMaxWhenMaxIsGreater() {
    when(amountTrackerMock.getRemoteReceivedMax()).thenReturn(Optional.of(UnsignedLong.MAX_VALUE));

    final PaymentTargetConditions paymentTargetConditions = PaymentTargetConditions.builder()
      .paymentType(PaymentType.FIXED_SEND)
      .minExchangeRate(BigDecimal.ONE)
      .minDeliveryAmount(BigInteger.ONE)
      .maxSourceAmount(BigInteger.valueOf(Long.MAX_VALUE))
      .build();
    when(amountTrackerMock.getPaymentTargetConditions()).thenReturn(Optional.of(paymentTargetConditions));

    final boolean actual = amountFilter.checkForIncompatibleReceiveMax(
      amountTrackerMock, paymentTargetConditions
    );
    assertThat(actual).isFalse();
  }

  @Test
  public void testCheckForIncompatibleReceiveMaxWhenMaxIsSmaller() {
    when(amountTrackerMock.getRemoteReceivedMax()).thenReturn(Optional.of(UnsignedLong.ONE));

    final PaymentTargetConditions paymentTargetConditions = PaymentTargetConditions.builder()
      .paymentType(PaymentType.FIXED_SEND)
      .minExchangeRate(BigDecimal.ONE)
      .minDeliveryAmount(BigInteger.valueOf(2L))
      .maxSourceAmount(BigInteger.valueOf(Long.MAX_VALUE))
      .build();
    when(amountTrackerMock.getPaymentTargetConditions()).thenReturn(Optional.of(paymentTargetConditions));

    boolean actual = amountFilter.checkForIncompatibleReceiveMax(
      amountTrackerMock, paymentTargetConditions
    );
    assertThat(actual).isTrue();
  }

  ////////////
  //  checkIfFixedSendPaymentIsComplete
  ////////////

  @Test
  public void checkIfFixedSendPaymentIsCompleteFalseFalse() {
    when(amountTrackerMock.getAmountSentInSourceUnits()).thenReturn(BigInteger.ONE); // <-- Not Equals
    when(amountTrackerMock.getSourceAmountInFlight()).thenReturn(BigInteger.ONE); // <-- Not positive

    final PaymentTargetConditions paymentTargetConditions = PaymentTargetConditions.builder()
      .paymentType(PaymentType.FIXED_SEND)
      .minExchangeRate(BigDecimal.ONE)
      .minDeliveryAmount(BigInteger.ONE)
      .maxSourceAmount(BigInteger.valueOf(Long.MAX_VALUE))
      .build();
    when(amountTrackerMock.getPaymentTargetConditions()).thenReturn(Optional.of(paymentTargetConditions));

    final boolean actual = amountFilter.checkIfFixedSendPaymentIsComplete(
      amountTrackerMock, paymentTargetConditions
    );
    assertThat(actual).isFalse();
  }

  @Test
  public void checkIfFixedSendPaymentIsCompleteFalseTrue() {
    when(amountTrackerMock.getAmountSentInSourceUnits()).thenReturn(BigInteger.ONE); // <-- Not Equals
    when(amountTrackerMock.getSourceAmountInFlight()).thenReturn(BigInteger.ZERO); // <-- Not positive

    final PaymentTargetConditions paymentTargetConditions = PaymentTargetConditions.builder()
      .paymentType(PaymentType.FIXED_SEND)
      .minExchangeRate(BigDecimal.ONE)
      .minDeliveryAmount(BigInteger.ONE)
      .maxSourceAmount(BigInteger.valueOf(Long.MAX_VALUE))
      .build();
    when(amountTrackerMock.getPaymentTargetConditions()).thenReturn(Optional.of(paymentTargetConditions));

    final boolean actual = amountFilter.checkIfFixedSendPaymentIsComplete(
      amountTrackerMock, paymentTargetConditions
    );
    assertThat(actual).isFalse();
  }

  @Test
  public void checkIfFixedSendPaymentIsCompleteTrueFalse() {
    when(amountTrackerMock.getAmountSentInSourceUnits()).thenReturn(BigInteger.ONE); // <-- Equals
    when(amountTrackerMock.getSourceAmountInFlight()).thenReturn(BigInteger.ONE); // <-- Is positive

    final PaymentTargetConditions paymentTargetConditions = PaymentTargetConditions.builder()
      .paymentType(PaymentType.FIXED_SEND)
      .minExchangeRate(BigDecimal.ONE)
      .minDeliveryAmount(BigInteger.ONE)
      .maxSourceAmount(BigInteger.ONE)
      .build();
    when(amountTrackerMock.getPaymentTargetConditions()).thenReturn(Optional.of(paymentTargetConditions));

    final boolean actual = amountFilter.checkIfFixedSendPaymentIsComplete(
      amountTrackerMock, paymentTargetConditions
    );
    assertThat(actual).isFalse();
  }

  @Test
  public void checkIfFixedSendPaymentIsCompleteTrueTrue() {
    when(amountTrackerMock.getAmountSentInSourceUnits()).thenReturn(BigInteger.ONE); // <-- Equals
    when(amountTrackerMock.getSourceAmountInFlight()).thenReturn(BigInteger.ZERO); // <-- Not positive

    final PaymentTargetConditions paymentTargetConditions = PaymentTargetConditions.builder()
      .paymentType(PaymentType.FIXED_SEND)
      .minExchangeRate(BigDecimal.ONE)
      .minDeliveryAmount(BigInteger.ONE)
      .maxSourceAmount(BigInteger.ONE)
      .build();
    when(amountTrackerMock.getPaymentTargetConditions()).thenReturn(Optional.of(paymentTargetConditions));

    final boolean actual = amountFilter.checkIfFixedSendPaymentIsComplete(
      amountTrackerMock, paymentTargetConditions
    );
    assertThat(actual).isTrue();
  }

  ////////////////
  // checkIfFixedDeliveryPaymentIsComplete
  ////////////////

  @Test
  public void testCheckIfFixedDeliveryPaymentIsCompleteWhenRemainingToDeliverFalseAndAnyInFlightFalse() {
    when(amountTrackerMock.getSourceAmountInFlight()).thenReturn(BigInteger.ZERO); // <-- AnyInFlight = false
    this.amountFilter = new AmountFilter(paymentSharedStateTrackerMock) {
      @Override
      protected BigInteger computeRemainingAmountToBeDelivered(
        AmountTracker amountTracker, PaymentTargetConditions target
      ) {
        return BigInteger.ZERO; // <-- remainingToDeliver = false
      }
    };

    final PaymentTargetConditions paymentTargetConditions = PaymentTargetConditions.builder()
      .paymentType(PaymentType.FIXED_SEND)
      .minExchangeRate(BigDecimal.ZERO)
      .minDeliveryAmount(BigInteger.ZERO)
      .maxSourceAmount(BigInteger.ONE)
      .build();
    when(amountTrackerMock.getPaymentTargetConditions()).thenReturn(Optional.of(paymentTargetConditions));

    final boolean actual = amountFilter.checkIfFixedDeliveryPaymentIsComplete(
      amountTrackerMock, paymentTargetConditions
    );
    assertThat(actual).isTrue();
  }

  @Test
  public void testCheckIfFixedDeliveryPaymentIsCompleteWhenRemainingToDeliverFalseAndAnyInFlightTrue() {
    when(amountTrackerMock.getSourceAmountInFlight()).thenReturn(BigInteger.ONE); // <-- AnyInFlight = true
    this.amountFilter = new AmountFilter(paymentSharedStateTrackerMock) {
      @Override
      protected BigInteger computeRemainingAmountToBeDelivered(
        AmountTracker amountTracker, PaymentTargetConditions target
      ) {
        return BigInteger.ONE; // <-- AnyToDeliver = false
      }
    };

    final PaymentTargetConditions paymentTargetConditions = PaymentTargetConditions.builder()
      .paymentType(PaymentType.FIXED_SEND)
      .minExchangeRate(BigDecimal.ZERO)
      .minDeliveryAmount(BigInteger.ZERO)
      .maxSourceAmount(BigInteger.ONE)
      .build();
    when(amountTrackerMock.getPaymentTargetConditions()).thenReturn(Optional.of(paymentTargetConditions));

    final boolean actual = amountFilter.checkIfFixedDeliveryPaymentIsComplete(
      amountTrackerMock, paymentTargetConditions
    );
    assertThat(actual).isFalse();
  }

  @Test
  public void testCheckIfFixedDeliveryPaymentIsCompleteWhenRemainingToDeliverTrueAndAnyInFlightFalse() {
    when(amountTrackerMock.getSourceAmountInFlight()).thenReturn(BigInteger.ZERO); // <-- AnyInFlight = false
    this.amountFilter = new AmountFilter(paymentSharedStateTrackerMock) {
      @Override
      protected BigInteger computeRemainingAmountToBeDelivered(
        AmountTracker amountTracker, PaymentTargetConditions target
      ) {
        return BigInteger.ONE; // <-- AnyToDeliver = true
      }
    };

    final PaymentTargetConditions paymentTargetConditions = PaymentTargetConditions.builder()
      .paymentType(PaymentType.FIXED_SEND)
      .minExchangeRate(BigDecimal.ZERO)
      .minDeliveryAmount(BigInteger.ZERO)
      .maxSourceAmount(BigInteger.ONE)
      .build();
    when(amountTrackerMock.getPaymentTargetConditions()).thenReturn(Optional.of(paymentTargetConditions));

    final boolean actual = amountFilter.checkIfFixedDeliveryPaymentIsComplete(
      amountTrackerMock, paymentTargetConditions
    );
    assertThat(actual).isFalse();
  }

  @Test
  public void testCheckIfFixedDeliveryPaymentIsCompleteWhenRemainingToDeliverTrueAndAnyInFlightTrue() {
    when(amountTrackerMock.getSourceAmountInFlight()).thenReturn(BigInteger.ONE); // <-- AnyInFlight = true
    this.amountFilter = new AmountFilter(paymentSharedStateTrackerMock) {
      @Override
      protected BigInteger computeRemainingAmountToBeDelivered(
        AmountTracker amountTracker, PaymentTargetConditions target
      ) {
        return BigInteger.ONE; // <-- AnyToDeliver = true
      }
    };

    final PaymentTargetConditions paymentTargetConditions = PaymentTargetConditions.builder()
      .paymentType(PaymentType.FIXED_SEND)
      .minExchangeRate(BigDecimal.ZERO)
      .minDeliveryAmount(BigInteger.ZERO)
      .maxSourceAmount(BigInteger.ONE)
      .build();
    when(amountTrackerMock.getPaymentTargetConditions()).thenReturn(Optional.of(paymentTargetConditions));

    final boolean actual = amountFilter.checkIfFixedDeliveryPaymentIsComplete(
      amountTrackerMock, paymentTargetConditions
    );
    assertThat(actual).isFalse();
  }

  ////////////
  //  computeRemainingAmountToBeDelivered
  ////////////

  @Test
  public void testComputeRemainingAmountToBeDeliveredIsPositive() {
    when(amountTrackerMock.getAmountDeliveredInDestinationUnits()).thenReturn(BigInteger.valueOf(2L));
    final PaymentTargetConditions paymentTargetConditions = PaymentTargetConditions.builder()
      .paymentType(PaymentType.FIXED_DELIVERY)
      .minExchangeRate(BigDecimal.ZERO)
      .minDeliveryAmount(BigInteger.valueOf(3L))
      .maxSourceAmount(BigInteger.ONE)
      .build();
    when(amountTrackerMock.getPaymentTargetConditions()).thenReturn(Optional.of(paymentTargetConditions));

    final BigInteger actual = amountFilter
      .computeRemainingAmountToBeDelivered(amountTrackerMock, paymentTargetConditions);
    assertThat(actual).isEqualTo(BigInteger.ONE);
  }

  @Test
  public void testComputeRemainingAmountToBeDeliveredIsZero() {
    when(amountTrackerMock.getAmountDeliveredInDestinationUnits()).thenReturn(BigInteger.valueOf(3L));
    final PaymentTargetConditions paymentTargetConditions = PaymentTargetConditions.builder()
      .paymentType(PaymentType.FIXED_DELIVERY)
      .minExchangeRate(BigDecimal.ZERO)
      .minDeliveryAmount(BigInteger.valueOf(3L))
      .maxSourceAmount(BigInteger.ONE)
      .build();
    when(amountTrackerMock.getPaymentTargetConditions()).thenReturn(Optional.of(paymentTargetConditions));

    final BigInteger actual = amountFilter
      .computeRemainingAmountToBeDelivered(amountTrackerMock, paymentTargetConditions);
    assertThat(actual).isEqualTo(BigInteger.ZERO);
  }

  @Test
  public void testComputeRemainingAmountToBeDeliveredIsNegative() {
    when(amountTrackerMock.getAmountDeliveredInDestinationUnits()).thenReturn(BigInteger.valueOf(4L));
    final PaymentTargetConditions paymentTargetConditions = PaymentTargetConditions.builder()
      .paymentType(PaymentType.FIXED_DELIVERY)
      .minExchangeRate(BigDecimal.ZERO)
      .minDeliveryAmount(BigInteger.valueOf(3L))
      .maxSourceAmount(BigInteger.ONE)
      .build();
    when(amountTrackerMock.getPaymentTargetConditions()).thenReturn(Optional.of(paymentTargetConditions));

    final BigInteger actual = amountFilter
      .computeRemainingAmountToBeDelivered(amountTrackerMock, paymentTargetConditions);
    assertThat(actual).isEqualTo(BigInteger.valueOf(-1));
  }

  ////////////
  // moreAvailableToDeliver
  ////////////

  @Test
  public void testMoreAvailableToDeliverBothZero() {

    //computeRemainingAmountToBeDelivered subtract
    // amountTracker.destInFlight ==> isPositive.

    this.amountFilter = new AmountFilter(paymentSharedStateTrackerMock) {
      @Override
      protected BigInteger computeRemainingAmountToBeDelivered(
        AmountTracker amountTracker, PaymentTargetConditions target
      ) {
        return BigInteger.ZERO;
      }
    };

    final PaymentTargetConditions paymentTargetConditions = PaymentTargetConditions.builder()
      .paymentType(PaymentType.FIXED_SEND)
      .minExchangeRate(BigDecimal.ZERO)
      .minDeliveryAmount(BigInteger.ONE)
      .maxSourceAmount(BigInteger.ONE)
      .build();
    when(amountTrackerMock.getDestinationAmountInFlight()).thenReturn(BigInteger.ZERO);

    final boolean actual = amountFilter.moreAvailableToDeliver(amountTrackerMock, paymentTargetConditions);
    assertThat(actual).isFalse();
  }

  @Test
  public void testMoreAvailableToDeliverBothPositive() {
    this.amountFilter = new AmountFilter(paymentSharedStateTrackerMock) {
      @Override
      protected BigInteger computeRemainingAmountToBeDelivered(
        AmountTracker amountTracker, PaymentTargetConditions target
      ) {
        return BigInteger.ONE;
      }
    };

    final PaymentTargetConditions paymentTargetConditions = PaymentTargetConditions.builder()
      .paymentType(PaymentType.FIXED_SEND)
      .minExchangeRate(BigDecimal.ZERO)
      .minDeliveryAmount(BigInteger.ONE)
      .maxSourceAmount(BigInteger.ONE)
      .build();
    when(amountTrackerMock.getDestinationAmountInFlight()).thenReturn(BigInteger.ONE);

    final boolean actual = amountFilter.moreAvailableToDeliver(amountTrackerMock, paymentTargetConditions);
    assertThat(actual).isFalse();
  }

  @Test
  public void testMoreAvailableToDeliverNegativeSum() {
    this.amountFilter = new AmountFilter(paymentSharedStateTrackerMock) {
      @Override
      protected BigInteger computeRemainingAmountToBeDelivered(
        AmountTracker amountTracker, PaymentTargetConditions target
      ) {
        return BigInteger.ZERO;
      }
    };

    final PaymentTargetConditions paymentTargetConditions = PaymentTargetConditions.builder()
      .paymentType(PaymentType.FIXED_SEND)
      .minExchangeRate(BigDecimal.ZERO)
      .minDeliveryAmount(BigInteger.ONE)
      .maxSourceAmount(BigInteger.ONE)
      .build();
    when(amountTrackerMock.getDestinationAmountInFlight()).thenReturn(BigInteger.ONE);

    final boolean actual = amountFilter.moreAvailableToDeliver(amountTrackerMock, paymentTargetConditions);
    assertThat(actual).isFalse();
  }

  @Test
  public void testMoreAvailableToDeliverPositiveSum() {
    this.amountFilter = new AmountFilter(paymentSharedStateTrackerMock) {
      @Override
      protected BigInteger computeRemainingAmountToBeDelivered(
        AmountTracker amountTracker, PaymentTargetConditions target
      ) {
        return BigInteger.ONE;
      }
    };

    final PaymentTargetConditions paymentTargetConditions = PaymentTargetConditions.builder()
      .paymentType(PaymentType.FIXED_SEND)
      .minExchangeRate(BigDecimal.ZERO)
      .minDeliveryAmount(BigInteger.ONE)
      .maxSourceAmount(BigInteger.ONE)
      .build();
    when(amountTrackerMock.getDestinationAmountInFlight()).thenReturn(BigInteger.ZERO);

    final boolean actual = amountFilter.moreAvailableToDeliver(amountTrackerMock, paymentTargetConditions);
    assertThat(actual).isTrue();
  }

  ////////////
  // computeAmountAvailableToSend
  ////////////

  @Test
  public void testComputeAmountAvailableToSendWhenPositive() {
    when(amountTrackerMock.getAmountSentInSourceUnits()).thenReturn(BigInteger.ONE); // <-- Not Equals
    when(amountTrackerMock.getSourceAmountInFlight()).thenReturn(BigInteger.ONE); // <-- Not positive

    final PaymentTargetConditions paymentTargetConditions = PaymentTargetConditions.builder()
      .paymentType(PaymentType.FIXED_SEND)
      .minExchangeRate(BigDecimal.ONE)
      .minDeliveryAmount(BigInteger.ONE)
      .maxSourceAmount(BigInteger.valueOf(3L))
      .build();
    when(amountTrackerMock.getPaymentTargetConditions()).thenReturn(Optional.of(paymentTargetConditions));

    final BigInteger actual = amountFilter.computeAmountAvailableToSend(paymentTargetConditions);
    assertThat(actual).isEqualTo(BigInteger.ONE);
  }

  @Test
  public void testComputeAmountAvailableToSendWhenZero() {
    when(amountTrackerMock.getAmountSentInSourceUnits()).thenReturn(BigInteger.ONE); // <-- Not Equals
    when(amountTrackerMock.getSourceAmountInFlight()).thenReturn(BigInteger.ONE); // <-- Not positive

    final PaymentTargetConditions paymentTargetConditions = PaymentTargetConditions.builder()
      .paymentType(PaymentType.FIXED_SEND)
      .minExchangeRate(BigDecimal.ONE)
      .minDeliveryAmount(BigInteger.ONE)
      .maxSourceAmount(BigInteger.valueOf(2L))
      .build();
    when(amountTrackerMock.getPaymentTargetConditions()).thenReturn(Optional.of(paymentTargetConditions));

    final BigInteger actual = amountFilter.computeAmountAvailableToSend(paymentTargetConditions);
    assertThat(actual).isEqualTo(BigInteger.ZERO);
  }

  @Test
  public void testComputeAmountAvailableToSendWhenNegative() {
    when(amountTrackerMock.getAmountSentInSourceUnits()).thenReturn(BigInteger.valueOf(2L)); // <-- Not Equals
    when(amountTrackerMock.getSourceAmountInFlight()).thenReturn(BigInteger.ONE); // <-- Not positive

    final PaymentTargetConditions paymentTargetConditions = PaymentTargetConditions.builder()
      .paymentType(PaymentType.FIXED_SEND)
      .minExchangeRate(BigDecimal.ONE)
      .minDeliveryAmount(BigInteger.ONE)
      .maxSourceAmount(BigInteger.valueOf(2L))
      .build();
    when(amountTrackerMock.getPaymentTargetConditions()).thenReturn(Optional.of(paymentTargetConditions));

    final BigInteger actual = amountFilter.computeAmountAvailableToSend(paymentTargetConditions);
    assertThat(actual).isEqualTo(BigInteger.valueOf(-1));
  }

  ////////////
  // computeAmountAvailableToDeliver
  ////////////

  @Test
  public void testComputeAmountAvailableToDeliverWhenPositive() {
    this.amountFilter = new AmountFilter(paymentSharedStateTrackerMock) {
      @Override
      protected BigInteger computeRemainingAmountToBeDelivered(
        AmountTracker amountTracker, PaymentTargetConditions target
      ) {
        return BigInteger.ONE;
      }
    };
    when(amountTrackerMock.getDestinationAmountInFlight()).thenReturn(BigInteger.ZERO);

    final BigInteger actual = amountFilter
      .computeAmountAvailableToDeliver(amountTrackerMock, mock(PaymentTargetConditions.class));
    assertThat(actual).isEqualTo(BigInteger.ONE);
  }

  @Test
  public void testComputeAmountAvailableToDelliverWhenZero() {
    this.amountFilter = new AmountFilter(paymentSharedStateTrackerMock) {
      @Override
      protected BigInteger computeRemainingAmountToBeDelivered(
        AmountTracker amountTracker, PaymentTargetConditions target
      ) {
        return BigInteger.ONE;
      }
    };
    when(amountTrackerMock.getDestinationAmountInFlight()).thenReturn(BigInteger.ONE);

    final BigInteger actual = amountFilter
      .computeAmountAvailableToDeliver(amountTrackerMock, mock(PaymentTargetConditions.class));
    assertThat(actual).isEqualTo(BigInteger.ZERO);
  }

  @Test
  public void testComputeAmountAvailableToDeliverWhenNegative() {
    this.amountFilter = new AmountFilter(paymentSharedStateTrackerMock) {
      @Override
      protected BigInteger computeRemainingAmountToBeDelivered(
        AmountTracker amountTracker, PaymentTargetConditions target
      ) {
        return BigInteger.ZERO;
      }
    };
    when(amountTrackerMock.getDestinationAmountInFlight()).thenReturn(BigInteger.ONE);

    final BigInteger actual = amountFilter
      .computeAmountAvailableToDeliver(amountTrackerMock, mock(PaymentTargetConditions.class));
    assertThat(actual).isEqualTo(BigInteger.valueOf(-1L));
  }

  ////////////
  //  computeSourceAmountDeliveryLimit
  ////////////

  @Test
  public void testComputeSourceAmountDeliveryLimit() {
    this.amountFilter = new AmountFilter(paymentSharedStateTrackerMock) {
      @Override
      protected BigInteger computeAmountAvailableToDeliver(
        AmountTracker amountTracker, PaymentTargetConditions target
      ) {
        return BigInteger.ONE; // <-- This minus getDestinationAmountInFlight
      }
    };

    final PaymentTargetConditions paymentTargetConditions = PaymentTargetConditions.builder()
      .paymentType(PaymentType.FIXED_SEND)
      .minExchangeRate(BigDecimal.ONE)
      .minDeliveryAmount(BigInteger.ONE)
      .maxSourceAmount(BigInteger.ONE)
      .build();
    when(amountTrackerMock.getPaymentTargetConditions()).thenReturn(Optional.of(paymentTargetConditions));

    amountFilter.computeSourceAmountDeliveryLimit(amountTrackerMock, paymentTargetConditions);
    verify(exchangeRateTrackerMock).estimateSourceAmount(any());
  }

  ////////////
  //  isSourceDeliveryLimitInvalid
  ////////////

  @Test
  public void testIsSourceDeliveryLimitInvalidFalse() {
    this.amountFilter = new AmountFilter(paymentSharedStateTrackerMock) {
      @Override
      protected UnsignedLong computeSourceAmountDeliveryLimit(
        AmountTracker amountTracker, PaymentTargetConditions target
      ) {
        return UnsignedLong.ONE; // <-- This minus getDestinationAmountInFlight
      }
    };

    assertThat(amountFilter.isSourceAmountDeliveryLimitInvalid(amountTrackerMock, mock(PaymentTargetConditions.class)))
      .isFalse();
  }

  @Test
  public void testIsSourceDeliveryLimitInvalidTrue() {
    this.amountFilter = new AmountFilter(paymentSharedStateTrackerMock) {
      @Override
      protected UnsignedLong computeSourceAmountDeliveryLimit(
        AmountTracker amountTracker, PaymentTargetConditions target
      ) {
        return UnsignedLong.ZERO; // <-- This minus getDestinationAmountInFlight
      }
    };

    assertThat(amountFilter.isSourceAmountDeliveryLimitInvalid(amountTrackerMock, mock(PaymentTargetConditions.class)))
      .isTrue();
  }

  //////////////////
  // computeEstimatedDestinationAmount
  //////////////////

  @Test
  public void testComputeEstimatedDestinationAmount() {
    UnsignedLong sourceAmount = UnsignedLong.ONE;
    when(exchangeRateTrackerMock.estimateDestinationAmount(sourceAmount)).thenReturn(ExchangeRateBound.builder()
      .lowEndEstimate(UnsignedLong.ONE)
      .highEndEstimate(UnsignedLong.ZERO)
      .build());
    final UnsignedLong deliveryDeficit = amountFilter.computeEstimatedDestinationAmount(sourceAmount);

    assertThat(deliveryDeficit).isEqualTo(UnsignedLong.ONE);
    verify(exchangeRateTrackerMock).estimateDestinationAmount(any());
  }

  //////////////////
  // computeMinDestinationAmount
  //////////////////

  @Test
  public void testComputeMinDestinationAmountWhenEqual() {
    final UnsignedLong deliveryDeficit = amountFilter.computeMinDestinationAmount(
      UnsignedLong.ONE, // <-- sourceAmount
      BigDecimal.ONE // <-- minExchangeRate
    );

    assertThat(deliveryDeficit).isEqualTo(UnsignedLong.valueOf(2L));
  }

  @Test
  public void testComputeMinDestinationAmountWithHighDecimal() {
    final UnsignedLong deliveryDeficit = amountFilter.computeMinDestinationAmount(
      UnsignedLong.ONE, // <-- sourceAmount
      new BigDecimal("0.99") // <-- minExchangeRate
    );

    assertThat(deliveryDeficit).isEqualTo(UnsignedLong.ONE);
  }

  @Test
  public void testComputeMinDestinationAmountWithLowDecimal() {
    final UnsignedLong deliveryDeficit = amountFilter.computeMinDestinationAmount(
      UnsignedLong.ONE, // <-- sourceAmount
      new BigDecimal("1.01") // <-- minExchangeRate
    );

    assertThat(deliveryDeficit).isEqualTo(UnsignedLong.valueOf(2L));
  }

  ////////////
  //  computeDeliveryDeficit
  ////////////

  @Test
  public void testComputeDeliveryDeficitPositive() {
    final UnsignedLong deliveryDeficit = amountFilter.computeDeliveryDeficit(
      UnsignedLong.ONE, // <-- minDestinationAmount
      UnsignedLong.ZERO // <-- estimatedDestinationAmount
    );

    assertThat(deliveryDeficit).isEqualTo(UnsignedLong.ONE);
  }

  @Test
  public void testComputeDeliveryDeficitZero() {
    final UnsignedLong deliveryDeficit = amountFilter.computeDeliveryDeficit(
      UnsignedLong.ONE, // <-- minDestinationAmount
      UnsignedLong.ONE // <-- estimatedDestinationAmount
    );

    assertThat(deliveryDeficit).isEqualTo(UnsignedLong.ZERO);
  }

  @Test
  public void testComputeDeliveryDeficitZero2() {
    final UnsignedLong deliveryDeficit = amountFilter.computeDeliveryDeficit(
      UnsignedLong.ZERO, // <-- minDestinationAmount
      UnsignedLong.ZERO // <-- estimatedDestinationAmount
    );

    assertThat(deliveryDeficit).isEqualTo(UnsignedLong.ZERO);
  }

  @Test
  public void testComputeDeliveryDeficitNegative() {
    final UnsignedLong deliveryDeficit = amountFilter.computeDeliveryDeficit(
      UnsignedLong.ZERO, // <-- minDestinationAmount
      UnsignedLong.ONE // <-- estimatedDestinationAmount
    );

    assertThat(deliveryDeficit).isEqualTo(UnsignedLong.ZERO);
  }

  ////////////
  //  willPaymentComplete
  ////////////

  @Test
  public void testWillFixedSendPaymentCompleteWhenSourceEqualsAvailable() {
    final PaymentTargetConditions paymentTargetConditions = PaymentTargetConditions.builder()
      .paymentType(PaymentType.FIXED_SEND)
      .minExchangeRate(BigDecimal.ZERO)
      .minDeliveryAmount(BigInteger.valueOf(3L))
      .maxSourceAmount(BigInteger.ONE)
      .build();
    when(amountTrackerMock.getPaymentTargetConditions()).thenReturn(Optional.of(paymentTargetConditions));

    final boolean willPaymentComplete = amountFilter.willPaymentComplete(
      amountTrackerMock, paymentTargetConditions,
      BigInteger.ONE, // <-- available to send
      UnsignedLong.ONE, // <-- Original source amount
      UnsignedLong.ONE // <-- Estimated delivery amount.
    );

    assertThat(willPaymentComplete).isTrue();
  }

  @Test
  public void testWillFixedSendPaymentCompleteWhenSourceGreaterThanAvailable() {
    final PaymentTargetConditions paymentTargetConditions = PaymentTargetConditions.builder()
      .paymentType(PaymentType.FIXED_SEND)
      .minExchangeRate(BigDecimal.ZERO)
      .minDeliveryAmount(BigInteger.valueOf(3L))
      .maxSourceAmount(BigInteger.ONE)
      .build();
    when(amountTrackerMock.getPaymentTargetConditions()).thenReturn(Optional.of(paymentTargetConditions));

    final boolean willPaymentComplete = amountFilter.willPaymentComplete(
      amountTrackerMock, paymentTargetConditions,
      BigInteger.ONE, // <-- available to send
      UnsignedLong.MAX_VALUE, // <-- Original source amount
      UnsignedLong.ONE // <-- Estimated delivery amount.
    );

    assertThat(willPaymentComplete).isFalse();
  }

  @Test
  public void testWillFixedSendPaymentCompleteWhenSourceLessThanAvailable() {
    final PaymentTargetConditions paymentTargetConditions = PaymentTargetConditions.builder()
      .paymentType(PaymentType.FIXED_SEND)
      .minExchangeRate(BigDecimal.ZERO)
      .minDeliveryAmount(BigInteger.valueOf(3L))
      .maxSourceAmount(BigInteger.ONE)
      .build();
    when(amountTrackerMock.getPaymentTargetConditions()).thenReturn(Optional.of(paymentTargetConditions));

    final boolean willPaymentComplete = amountFilter.willPaymentComplete(
      amountTrackerMock, paymentTargetConditions,
      BigInteger.valueOf(2L), // <-- available to send
      UnsignedLong.ONE, // <-- Original source amount
      UnsignedLong.ONE // <-- Estimated delivery amount.
    );

    assertThat(willPaymentComplete).isFalse();
  }

  @Test
  public void testWillFixedDeliveryPaymentCompleteWhenMinDeliveryIsGreater() {
    final PaymentTargetConditions paymentTargetConditions = PaymentTargetConditions.builder()
      .paymentType(PaymentType.FIXED_DELIVERY)
      .minExchangeRate(BigDecimal.ZERO)
      .minDeliveryAmount(BigInteger.valueOf(Long.MAX_VALUE)) // <-- Variable
      .maxSourceAmount(BigInteger.ONE)
      .build();
    when(amountTrackerMock.getPaymentTargetConditions()).thenReturn(Optional.of(paymentTargetConditions));

    final boolean willPaymentComplete = amountFilter.willPaymentComplete(
      amountTrackerMock, paymentTargetConditions,
      BigInteger.ZERO, // <-- available to send
      UnsignedLong.ZERO, // <-- Original source amount
      UnsignedLong.ZERO // <-- Estimated delivery amount.
    );

    assertThat(willPaymentComplete).isFalse();
  }

  @Test
  public void testWillFixedDeliveryPaymentCompleteWhenMinDeliveryIsEqual() {
    final PaymentTargetConditions paymentTargetConditions = PaymentTargetConditions.builder()
      .paymentType(PaymentType.FIXED_DELIVERY)
      .minExchangeRate(BigDecimal.ZERO)
      .minDeliveryAmount(BigInteger.ONE) // <-- Variable
      .maxSourceAmount(BigInteger.ONE)
      .build();
    when(amountTrackerMock.getPaymentTargetConditions()).thenReturn(Optional.of(paymentTargetConditions));

    final boolean willPaymentComplete = amountFilter.willPaymentComplete(
      amountTrackerMock, paymentTargetConditions,
      BigInteger.ZERO, // <-- available to send
      UnsignedLong.ZERO, // <-- Original source amount
      UnsignedLong.ZERO // <-- Estimated delivery amount.
    );

    assertThat(willPaymentComplete).isTrue();
  }

  @Test
  public void testWillFixedDeliveryPaymentCompleteWhenMinDeliveryIsLessThan() {
    final PaymentTargetConditions paymentTargetConditions = PaymentTargetConditions.builder()
      .paymentType(PaymentType.FIXED_DELIVERY)
      .minExchangeRate(BigDecimal.ZERO)
      .minDeliveryAmount(BigInteger.ZERO) // <-- Variable
      .maxSourceAmount(BigInteger.ONE)
      .build();
    when(amountTrackerMock.getPaymentTargetConditions()).thenReturn(Optional.of(paymentTargetConditions));

    final boolean willPaymentComplete = amountFilter.willPaymentComplete(
      amountTrackerMock, paymentTargetConditions,
      BigInteger.ZERO, // <-- available to send
      UnsignedLong.ZERO, // <-- Original source amount
      UnsignedLong.ZERO // <-- Estimated delivery amount.
    );

    assertThat(willPaymentComplete).isTrue();
  }

  // TODO: Implement Kincaid's fix.

  //////////////////
  // Private Helpers
  //////////////////

  private void initializeHappyPath() {
    // amountTrackerMock
    when(amountTrackerMock.getAvailableDeliveryShortfall()).thenReturn(BigInteger.ZERO);
    when(amountTrackerMock.getAmountSentInSourceUnits()).thenReturn(BigInteger.ONE);
    when(amountTrackerMock.getAmountDeliveredInDestinationUnits()).thenReturn(BigInteger.ONE);
    when(amountTrackerMock.getSourceAmountInFlight()).thenReturn(BigInteger.ZERO);
    when(amountTrackerMock.getDestinationAmountInFlight()).thenReturn(BigInteger.ZERO);
    when(amountTrackerMock.getRemoteReceivedMax()).thenReturn(Optional.of(UnsignedLong.MAX_VALUE));
    when(amountTrackerMock.getPaymentTargetConditions()).thenReturn(Optional.of(
      PaymentTargetConditions.builder()
        .paymentType(PaymentType.FIXED_SEND)
        .minExchangeRate(BigDecimal.ONE)
        .minDeliveryAmount(BigInteger.ONE)
        .maxSourceAmount(BigInteger.valueOf(Long.MAX_VALUE))
        .build()
    ));

    // maxPacketAmountTrackerMock
    when(maxPacketAmountTrackerMock.getMaxPacketAmount()).thenReturn(MaxPacketAmount.unknownMax());
    when(maxPacketAmountTrackerMock.getNextMaxPacketAmount()).thenReturn(UnsignedLong.MAX_VALUE);
    when(maxPacketAmountTrackerMock.verifiedPathCapacity()).thenReturn(UnsignedLong.ZERO);

    // exchangeRateTrackerMock
    when(exchangeRateTrackerMock.getLowerBoundRate()).thenReturn(Ratio.ONE);
    when(exchangeRateTrackerMock.getUpperBoundRate()).thenReturn(Ratio.ONE);
    when(exchangeRateTrackerMock.estimateDestinationAmount(any())).thenReturn(
      ExchangeRateBound.builder()
        .lowEndEstimate(UnsignedLong.ONE.plus(UnsignedLong.ONE)) // <-- Assumes 1:1 FX
        .highEndEstimate(UnsignedLong.ONE.plus(UnsignedLong.ONE)) // <-- Assumes 1:1 FX
        .build()
    );
    when(exchangeRateTrackerMock.estimateSourceAmount(any())).thenReturn(
      ExchangeRateBound.builder().lowEndEstimate(UnsignedLong.ONE).highEndEstimate(UnsignedLong.ONE).build()
    );
  }
}