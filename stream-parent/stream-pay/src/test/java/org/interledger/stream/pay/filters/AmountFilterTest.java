package org.interledger.stream.pay.filters;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.interledger.stream.pay.StreamPayerExceptionMatcher.hasSendState;
import static org.interledger.stream.pay.StreamPayerExceptionMatcher.hasErrorCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.interledger.core.InterledgerFulfillPacket;
import org.interledger.core.InterledgerRejectPacket;
import org.interledger.core.fluent.Ratio;
import org.interledger.stream.frames.ErrorCodes;
import org.interledger.stream.frames.StreamCloseFrame;
import org.interledger.stream.frames.StreamFrame;
import org.interledger.stream.frames.StreamMoneyFrame;
import org.interledger.stream.frames.StreamMoneyMaxFrame;
import org.interledger.stream.pay.exceptions.StreamPayerException;
import org.interledger.stream.pay.filters.chain.StreamPacketFilterChain;
import org.interledger.stream.pay.model.ModifiableStreamPacketRequest;
import org.interledger.stream.pay.model.SendState;
import org.interledger.stream.pay.model.StreamPacketReply;
import org.interledger.stream.pay.probing.model.DeliveredExchangeRateBound;
import org.interledger.stream.pay.probing.model.PaymentTargetConditions;
import org.interledger.stream.pay.probing.model.PaymentTargetConditions.PaymentType;
import org.interledger.stream.pay.trackers.AmountTracker;
import org.interledger.stream.pay.trackers.ExchangeRateTracker;
import org.interledger.stream.pay.trackers.MaxPacketAmountTracker;
import org.interledger.stream.pay.trackers.MaxPacketAmountTracker.MaxPacketAmount;
import org.interledger.stream.pay.trackers.PaymentSharedStateTracker;

import com.google.common.collect.Sets;
import com.google.common.primitives.UnsignedLong;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Optional;

/**
 * Unit test for {@link AmountFilter}.
 */
public class AmountFilterTest {

  private static final UnsignedLong UL_TEN = UnsignedLong.valueOf(10L);
  private static final Ratio RATIO_TEN = Ratio.builder().numerator(BigInteger.TEN).denominator(BigInteger.ONE).build();

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
    MockitoAnnotations.openMocks(this);

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
    expectedException.expect(StreamPayerException.class);
    expectedException.expect(hasSendState(SendState.ReceiverProtocolViolation));
    expectedException.expect(hasErrorCode(ErrorCodes.ProtocolViolation));

    when(amountTrackerMock.encounteredProtocolViolation()).thenReturn(true);

    ModifiableStreamPacketRequest request = ModifiableStreamPacketRequest.create().setSourceAmount(
      UL_TEN
    );

    amountFilter.nextState(request);
  }

  @Test
  public void nextStateWithNoPaymentTarget() {
    when(amountTrackerMock.getPaymentTargetConditions()).thenReturn(Optional.empty());

    ModifiableStreamPacketRequest request = ModifiableStreamPacketRequest.create().setSourceAmount(
      UL_TEN
    );
    SendState result = amountFilter.nextState(request);
    assertThat(result).isEqualTo(SendState.Ready);
    assertThat(request.requestFrames().stream()).isEmpty();
  }

  @Test
  public void nextStateWithIncompatibleReceiveMax() {
    expectedException.expect(StreamPayerException.class);
    expectedException.expect(hasSendState(SendState.IncompatibleReceiveMax));
    expectedException.expect(hasErrorCode(ErrorCodes.ApplicationError));
    expectedException.expectMessage(
      "Ending payment: minimum delivery amount is too much for recipient. minDeliveryAmount=1 remoteReceiveMax=Optional[18446744073709551615]"
    );

    this.amountFilter = new AmountFilter(paymentSharedStateTrackerMock) {
      @Override
      protected boolean checkForIncompatibleReceiveMax(AmountTracker amountTracker, PaymentTargetConditions target) {
        return true;
      }
    };

    ModifiableStreamPacketRequest request = ModifiableStreamPacketRequest.create();
    SendState result = amountFilter.nextState(request);
    assertThat(result).isEqualTo(SendState.IncompatibleReceiveMax);
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
        .minExchangeRate(Ratio.ONE)
        .maxPaymentAmountInSenderUnits(BigInteger.TEN)
        .minPaymentAmountInDestinationUnits(BigInteger.ONE)
        .build()
    ));

    final ModifiableStreamPacketRequest request = ModifiableStreamPacketRequest.create();
    SendState result = amountFilter.nextState(request);
    assertThat(result).isEqualTo(SendState.End);
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
        .minExchangeRate(Ratio.ONE)
        .maxPaymentAmountInSenderUnits(BigInteger.TEN)
        .minPaymentAmountInDestinationUnits(BigInteger.ONE)
        .build()
    ));

    final ModifiableStreamPacketRequest request = ModifiableStreamPacketRequest.create();
    SendState result = amountFilter.nextState(request);
    assertThat(result).isEqualTo(SendState.Ready);
    assertThat(request.requestFrames().stream()).isEmpty();
  }

  @Test
  public void nextStateWithFixedDeliveryAndInvalidDeliveryLimit() {
    expectedException.expect(StreamPayerException.class);
    expectedException.expect(hasSendState(SendState.InsufficientExchangeRate));
    expectedException.expect(hasErrorCode(ErrorCodes.NoError));
    expectedException.expectMessage("Payment cannot complete: exchange rate dropped to 0");

    this.amountFilter = new AmountFilter(paymentSharedStateTrackerMock) {
      @Override
      protected boolean checkIfFixedDeliveryPaymentIsComplete(
        AmountTracker amountTracker, PaymentTargetConditions target
      ) {
        return false;
      }

      @Override
      protected boolean moreAvailableToDeliver(AmountTracker amountTracker, PaymentTargetConditions target) {
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
        .minExchangeRate(Ratio.ONE)
        .minPaymentAmountInDestinationUnits(BigInteger.ONE)
        .maxPaymentAmountInSenderUnits(BigInteger.TEN)
        .build()
    ));

    final ModifiableStreamPacketRequest request = ModifiableStreamPacketRequest.create();
    amountFilter.nextState(request);
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
      protected boolean moreAvailableToDeliver(AmountTracker amountTracker, PaymentTargetConditions target) {
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
        return UL_TEN;
      }
    };

    when(amountTrackerMock.getPaymentTargetConditions()).thenReturn(Optional.of(
      PaymentTargetConditions.builder()
        .paymentType(PaymentType.FIXED_DELIVERY)
        .minExchangeRate(Ratio.ONE)
        .minPaymentAmountInDestinationUnits(BigInteger.ONE)
        .maxPaymentAmountInSenderUnits(BigInteger.TEN)
        .build()
    ));

    final ModifiableStreamPacketRequest request = ModifiableStreamPacketRequest.create();
    SendState result = amountFilter.nextState(request);
    assertThat(result).isEqualTo(SendState.Ready);
    assertThat(request.sourceAmount()).isEqualTo(UnsignedLong.valueOf(1L));
    assertThat(request.requestFrames().stream()
      .filter(frame -> frame instanceof StreamMoneyFrame)
      .findAny()).isPresent();
  }

  @Test
  public void nextStateWithFixedDeliveryAndUpdatedSourceAmountWhenDeliveryLimitIsEqual() {
    when(exchangeRateTrackerMock.estimateDestinationAmount(any())).thenReturn(DeliveredExchangeRateBound.builder()
      .lowEndEstimate(UnsignedLong.valueOf(10L))
      .highEndEstimate(UnsignedLong.valueOf(11L))
      .build());

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
        return UL_TEN;
      }
    };

    when(amountTrackerMock.getPaymentTargetConditions()).thenReturn(Optional.of(
      PaymentTargetConditions.builder()
        .paymentType(PaymentType.FIXED_DELIVERY)
        .minExchangeRate(Ratio.ONE)
        .minPaymentAmountInDestinationUnits(BigInteger.ONE)
        .maxPaymentAmountInSenderUnits(BigInteger.TEN)
        .build()
    ));

    final ModifiableStreamPacketRequest request = ModifiableStreamPacketRequest.create();
    SendState result = amountFilter.nextState(request);
    assertThat(result).isEqualTo(SendState.Ready);
    assertThat(request.sourceAmount()).isEqualTo(UL_TEN);
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
      protected boolean moreAvailableToDeliver(AmountTracker amountTracker, PaymentTargetConditions target) {
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
        .minExchangeRate(Ratio.ONE)
        .minPaymentAmountInDestinationUnits(BigInteger.ONE)
        .maxPaymentAmountInSenderUnits(BigInteger.TEN)
        .build()
    ));

    final ModifiableStreamPacketRequest request = ModifiableStreamPacketRequest.create();
    SendState result = amountFilter.nextState(request);
    assertThat(result).isEqualTo(SendState.Ready);
    assertThat(request.sourceAmount()).isEqualTo(UnsignedLong.valueOf(1L));
    assertThat(request.requestFrames().stream()
      .filter(frame -> frame instanceof StreamMoneyFrame)
      .findAny()).isPresent();
  }

  @Test
  public void nextStateWithDeliveryDeficitWillNotCompletePayment() {
    expectedException.expect(StreamPayerException.class);
    expectedException.expect(hasSendState(SendState.InsufficientExchangeRate));
    expectedException.expect(hasErrorCode(ErrorCodes.NoError));
    expectedException.expectMessage("Payment cannot complete because exchange rate dropped below minimum.");

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
      protected UnsignedLong computePacketDeliveryDeficitForNextState(UnsignedLong minDestinationPacketAmount,
        UnsignedLong estimatedDestinationPacketAmount) {
        return UnsignedLong.ONE; // <-- So that paymentComplete inspection is triggered.
      }

      @Override
      protected boolean willPaymentComplete(AmountTracker amountTracker, PaymentTargetConditions target,
        BigInteger availableToSend, UnsignedLong sourcePacketAmount, UnsignedLong estimatedDestinationPacketAmount) {
        return false;
      }
    };

    final ModifiableStreamPacketRequest request = ModifiableStreamPacketRequest.create();
    amountFilter.nextState(request);
  }

  @Test
  public void nextStateWithDeliveryDeficitShortfallLessThanDeficit() {
    expectedException.expect(StreamPayerException.class);
    expectedException.expect(hasSendState(SendState.InsufficientExchangeRate));
    expectedException.expect(hasErrorCode(ErrorCodes.NoError));
    expectedException.expectMessage("Payment cannot complete because exchange rate dropped below minimum.");

    when(amountTrackerMock.getAvailableDeliveryShortfall()).thenReturn(UnsignedLong.ZERO);
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
      protected UnsignedLong computePacketDeliveryDeficitForNextState(UnsignedLong minDestinationPacketAmount,
        UnsignedLong estimatedDestinationPacketAmount) {
        return UnsignedLong.ONE; // <-- So that paymentComplete inspection is triggered.
      }

      @Override
      protected boolean willPaymentComplete(AmountTracker amountTracker, PaymentTargetConditions target,
        BigInteger availableToSend, UnsignedLong sourcePacketAmount, UnsignedLong estimatedDestinationPacketAmount) {
        return true;
      }
    };

    final ModifiableStreamPacketRequest request = ModifiableStreamPacketRequest.create();
    amountFilter.nextState(request);
  }

  @Test
  public void testNextStateWithPositiveDeliveryDeficitButValidPayment() {
    when(amountTrackerMock.getAvailableDeliveryShortfall()).thenReturn(UnsignedLong.ONE);
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
      protected UnsignedLong computePacketDeliveryDeficitForNextState(UnsignedLong minDestinationPacketAmount,
        UnsignedLong estimatedDestinationPacketAmount) {
        return UnsignedLong.ONE; // <-- So that paymentComplete inspection is triggered.
      }

      @Override
      protected boolean willPaymentComplete(AmountTracker amountTracker, PaymentTargetConditions target,
        BigInteger availableToSend, UnsignedLong sourcePacketAmount, UnsignedLong estimatedDestinationPacketAmount) {
        return true;
      }
    };

    final ModifiableStreamPacketRequest request = ModifiableStreamPacketRequest.create();
    SendState result = amountFilter.nextState(request);
    assertThat(result).isEqualTo(SendState.Ready);
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
  public void doFilterWithNoPaymentTargetConditionsNoReply() {
    when(amountTrackerMock.getPaymentTargetConditions()).thenReturn(Optional.empty());

    final ModifiableStreamPacketRequest streamPacketRequestMock = ModifiableStreamPacketRequest.create()
      .setSourceAmount(UnsignedLong.ONE);
    final StreamPacketReply streamPacketReplyMock = mock(StreamPacketReply.class);
    when(streamPacketReplyMock.destinationAmountClaimed()).thenReturn(Optional.empty());
    final StreamPacketFilterChain streamPacketFilterChainMock = mock(StreamPacketFilterChain.class);
    when(streamPacketFilterChainMock.doFilter(streamPacketRequestMock)).thenReturn(streamPacketReplyMock);

    StreamPacketReply reply = amountFilter.doFilter(streamPacketRequestMock, streamPacketFilterChainMock);

    assertThat(reply.exception()).isEmpty();
    assertThat(reply.interledgerPreparePacket()).isEmpty();
    assertThat(reply.isAuthentic()).isFalse();
    assertThat(reply.isFulfill()).isFalse();
    assertThat(reply.isReject()).isFalse();

    verify(amountTrackerMock).addToSourceAmountInFlight(UnsignedLong.ONE);
    verify(amountTrackerMock).addToDestinationAmountInFlight(UnsignedLong.ZERO);
    verify(amountTrackerMock).subtractFromSourceAmountInFlight(UnsignedLong.ONE);
    verify(amountTrackerMock).subtractFromDestinationAmountInFlight(UnsignedLong.ZERO);
    verify(amountTrackerMock, times(2)).getPaymentTargetConditions();
    verifyNoMoreInteractions(amountTrackerMock);
  }

  @Test
  public void doFilterWithNoPaymentTargetConditionsWithFulfillWithAmtClaimedInvalid() {
    when(amountTrackerMock.getPaymentTargetConditions()).thenReturn(Optional.empty());
    this.amountFilter = new AmountFilter(paymentSharedStateTrackerMock) {
      @Override
      protected boolean isDestinationAmountValid(
        UnsignedLong destinationAmount, UnsignedLong minDestinationAmount) {
        return false;
      }
    };

    // Request
    final ModifiableStreamPacketRequest streamPacketRequestMock = ModifiableStreamPacketRequest.create()
      .setMinDestinationAmount(UL_TEN)
      .setSourceAmount(UnsignedLong.ONE);
    // Reply
    final StreamPacketReply streamPacketReplyMock = Mockito.mock(StreamPacketReply.class);
    doCallRealMethod().when(streamPacketReplyMock).handle(any(), any());
    when(streamPacketReplyMock.destinationAmountClaimed()).thenReturn(Optional.of(UnsignedLong.ONE));
    final InterledgerFulfillPacket fulfillResponseMock = mock(InterledgerFulfillPacket.class);
    doCallRealMethod().when(fulfillResponseMock).handle(any(), any());
    when(streamPacketReplyMock.interledgerResponsePacket()).thenReturn(Optional.of(fulfillResponseMock));
    // Chain
    final StreamPacketFilterChain streamPacketFilterChainMock = mock(StreamPacketFilterChain.class);
    when(streamPacketFilterChainMock.doFilter(streamPacketRequestMock)).thenReturn(streamPacketReplyMock);

    StreamPacketReply actualReply = amountFilter.doFilter(streamPacketRequestMock, streamPacketFilterChainMock);

    assertThat(actualReply.exception()).isEmpty();
    assertThat(actualReply.interledgerPreparePacket()).isEmpty();
    assertThat(actualReply.isAuthentic()).isFalse();
    assertThat(actualReply.isFulfill()).isFalse();
    assertThat(actualReply.isReject()).isFalse();

    verify(amountTrackerMock).addToSourceAmountInFlight(UnsignedLong.ONE);
    verify(amountTrackerMock).addToDestinationAmountInFlight(UnsignedLong.ZERO);
    verify(amountTrackerMock).subtractFromSourceAmountInFlight(UnsignedLong.ONE);
    verify(amountTrackerMock).subtractFromDestinationAmountInFlight(UnsignedLong.ZERO);
    verify(amountTrackerMock).addAmountSent(UnsignedLong.ONE);
    verify(amountTrackerMock).addAmountDelivered(UL_TEN);
    verify(amountTrackerMock).setEncounteredProtocolViolation();
    verify(amountTrackerMock, times(2)).getPaymentTargetConditions();
    verifyNoMoreInteractions(amountTrackerMock);
  }

  @Test
  public void doFilterWithNoPaymentTargetConditionsWithFulfillWithNoAmtClaimed() {
    when(amountTrackerMock.getPaymentTargetConditions()).thenReturn(Optional.empty());
    this.amountFilter = new AmountFilter(paymentSharedStateTrackerMock) {
      @Override
      protected boolean isDestinationAmountValid(
        UnsignedLong destinationAmount, UnsignedLong minDestinationAmount) {
        return true;
      }
    };

    // Request
    final ModifiableStreamPacketRequest streamPacketRequestMock = ModifiableStreamPacketRequest.create()
      .setSourceAmount(UnsignedLong.ONE);
    // Reply
    final StreamPacketReply streamPacketReplyMock = Mockito.mock(StreamPacketReply.class);
    when(streamPacketReplyMock.destinationAmountClaimed()).thenReturn(Optional.empty());
    doCallRealMethod().when(streamPacketReplyMock).handle(any(), any());
    final InterledgerFulfillPacket fulfillResponseMock = mock(InterledgerFulfillPacket.class);
    doCallRealMethod().when(fulfillResponseMock).handle(any(), any());
    when(streamPacketReplyMock.interledgerResponsePacket()).thenReturn(Optional.of(fulfillResponseMock));
    // Chain
    final StreamPacketFilterChain streamPacketFilterChainMock = mock(StreamPacketFilterChain.class);
    when(streamPacketFilterChainMock.doFilter(streamPacketRequestMock)).thenReturn(streamPacketReplyMock);

    StreamPacketReply actualReply = amountFilter.doFilter(streamPacketRequestMock, streamPacketFilterChainMock);

    assertThat(actualReply.exception()).isEmpty();
    assertThat(actualReply.interledgerPreparePacket()).isEmpty();
    assertThat(actualReply.isAuthentic()).isFalse();
    assertThat(actualReply.isFulfill()).isFalse();
    assertThat(actualReply.isReject()).isFalse();

    verify(amountTrackerMock).addToSourceAmountInFlight(UnsignedLong.ONE);
    verify(amountTrackerMock).addToDestinationAmountInFlight(UnsignedLong.ZERO);
    verify(amountTrackerMock).subtractFromSourceAmountInFlight(UnsignedLong.ONE);
    verify(amountTrackerMock).subtractFromDestinationAmountInFlight(UnsignedLong.ZERO);
    verify(amountTrackerMock).addAmountSent(UnsignedLong.ONE);
    verify(amountTrackerMock).addAmountDelivered(UnsignedLong.ZERO);
    verify(amountTrackerMock).setEncounteredProtocolViolation();
    verify(amountTrackerMock, times(2)).getPaymentTargetConditions();
    verifyNoMoreInteractions(amountTrackerMock);
  }

  @Test
  public void doFilterWithNoPaymentTargetConditionsWithFulfillWithAmtClaimedValid() {
    when(amountTrackerMock.getPaymentTargetConditions()).thenReturn(Optional.empty());
    this.amountFilter = new AmountFilter(paymentSharedStateTrackerMock) {
      @Override
      protected boolean isDestinationAmountValid(
        UnsignedLong destinationAmount, UnsignedLong minDestinationAmount) {
        return true;
      }
    };

    // Request
    final ModifiableStreamPacketRequest streamPacketRequestMock = ModifiableStreamPacketRequest.create()
      .setSourceAmount(UnsignedLong.ONE);
    // Reply
    final StreamPacketReply streamPacketReplyMock = Mockito.mock(StreamPacketReply.class);
    when(streamPacketReplyMock.destinationAmountClaimed()).thenReturn(Optional.of(UL_TEN));
    doCallRealMethod().when(streamPacketReplyMock).handle(any(), any());
    final InterledgerFulfillPacket fulfillResponseMock = mock(InterledgerFulfillPacket.class);
    doCallRealMethod().when(fulfillResponseMock).handle(any(), any());
    when(streamPacketReplyMock.interledgerResponsePacket()).thenReturn(Optional.of(fulfillResponseMock));
    // Chain
    final StreamPacketFilterChain streamPacketFilterChainMock = mock(StreamPacketFilterChain.class);
    when(streamPacketFilterChainMock.doFilter(streamPacketRequestMock)).thenReturn(streamPacketReplyMock);

    StreamPacketReply actualReply = amountFilter.doFilter(streamPacketRequestMock, streamPacketFilterChainMock);

    assertThat(actualReply.exception()).isEmpty();
    assertThat(actualReply.interledgerPreparePacket()).isEmpty();
    assertThat(actualReply.isAuthentic()).isFalse();
    assertThat(actualReply.isFulfill()).isFalse();
    assertThat(actualReply.isReject()).isFalse();

    verify(amountTrackerMock).addToSourceAmountInFlight(UnsignedLong.ONE);
    verify(amountTrackerMock).addToDestinationAmountInFlight(UnsignedLong.ZERO);
    verify(amountTrackerMock).subtractFromSourceAmountInFlight(UnsignedLong.ONE);
    verify(amountTrackerMock).subtractFromDestinationAmountInFlight(UnsignedLong.ZERO);
    verify(amountTrackerMock).addAmountSent(UnsignedLong.ONE);
    verify(amountTrackerMock).addAmountDelivered(UL_TEN);
    verify(amountTrackerMock, times(2)).getPaymentTargetConditions();
    verifyNoMoreInteractions(amountTrackerMock);
  }

  @Test
  public void doFilterWithNoPaymentTargetConditionsWithReject() {
    when(amountTrackerMock.getPaymentTargetConditions()).thenReturn(Optional.empty());
    this.amountFilter = new AmountFilter(paymentSharedStateTrackerMock) {
      @Override
      protected boolean isDestinationAmountValid(
        UnsignedLong destinationAmount, UnsignedLong minDestinationAmount) {
        return false;
      }
    };

    // Request
    final ModifiableStreamPacketRequest streamPacketRequestMock = ModifiableStreamPacketRequest.create()
      .setSourceAmount(UnsignedLong.ONE);
    // Reply
    final StreamPacketReply streamPacketReplyMock = Mockito.mock(StreamPacketReply.class);
    when(streamPacketReplyMock.destinationAmountClaimed()).thenReturn(Optional.of(UL_TEN));
    doCallRealMethod().when(streamPacketReplyMock).handle(any(), any());
    final InterledgerRejectPacket rejectPacket = mock(InterledgerRejectPacket.class);
    doCallRealMethod().when(rejectPacket).handle(any(), any());
    when(streamPacketReplyMock.interledgerResponsePacket()).thenReturn(Optional.of(rejectPacket));
    // Chain
    final StreamPacketFilterChain streamPacketFilterChainMock = mock(StreamPacketFilterChain.class);
    when(streamPacketFilterChainMock.doFilter(streamPacketRequestMock)).thenReturn(streamPacketReplyMock);

    StreamPacketReply actualReply = amountFilter.doFilter(streamPacketRequestMock, streamPacketFilterChainMock);

    assertThat(actualReply.exception()).isEmpty();
    assertThat(actualReply.interledgerPreparePacket()).isEmpty();
    assertThat(actualReply.isAuthentic()).isFalse();
    assertThat(actualReply.isFulfill()).isFalse();
    assertThat(actualReply.isReject()).isFalse();

    verify(amountTrackerMock).addToSourceAmountInFlight(UnsignedLong.ONE);
    verify(amountTrackerMock).addToDestinationAmountInFlight(UnsignedLong.ZERO);
    verify(amountTrackerMock).subtractFromSourceAmountInFlight(UnsignedLong.ONE);
    verify(amountTrackerMock).subtractFromDestinationAmountInFlight(UnsignedLong.ZERO);
    verify(amountTrackerMock, times(2)).getPaymentTargetConditions();
    verifyNoMoreInteractions(amountTrackerMock);
  }

  @Test
  public void doFilterWithNoPaymentTargetConditionsWithFailedPacket() {
    when(amountTrackerMock.getPaymentTargetConditions()).thenReturn(Optional.empty());
    this.amountFilter = new AmountFilter(paymentSharedStateTrackerMock) {
      @Override
      protected boolean isFailedPacket(UnsignedLong deliveryDeficit, StreamPacketReply streamPacketReply) {
        return true;
      }
    };

    // Request
    final ModifiableStreamPacketRequest streamPacketRequestMock = ModifiableStreamPacketRequest.create()
      .setSourceAmount(UnsignedLong.ONE);
    // Reply
    final StreamPacketReply streamPacketReplyMock = Mockito.mock(StreamPacketReply.class);
    when(streamPacketReplyMock.destinationAmountClaimed()).thenReturn(Optional.of(UL_TEN));
    doCallRealMethod().when(streamPacketReplyMock).handle(any(), any());
    final InterledgerFulfillPacket fulfillResponseMock = mock(InterledgerFulfillPacket.class);
    doCallRealMethod().when(fulfillResponseMock).handle(any(), any());
    when(streamPacketReplyMock.interledgerResponsePacket()).thenReturn(Optional.of(fulfillResponseMock));
    // Chain
    final StreamPacketFilterChain streamPacketFilterChainMock = mock(StreamPacketFilterChain.class);
    when(streamPacketFilterChainMock.doFilter(streamPacketRequestMock)).thenReturn(streamPacketReplyMock);

    StreamPacketReply actualReply = amountFilter.doFilter(streamPacketRequestMock, streamPacketFilterChainMock);

    assertThat(actualReply.exception()).isEmpty();
    assertThat(actualReply.interledgerPreparePacket()).isEmpty();
    assertThat(actualReply.isAuthentic()).isFalse();
    assertThat(actualReply.isFulfill()).isFalse();
    assertThat(actualReply.isReject()).isFalse();

    verify(amountTrackerMock).addToSourceAmountInFlight(UnsignedLong.ONE);
    verify(amountTrackerMock).addToDestinationAmountInFlight(UnsignedLong.ZERO);
    verify(amountTrackerMock).subtractFromSourceAmountInFlight(UnsignedLong.ONE);
    verify(amountTrackerMock).subtractFromDestinationAmountInFlight(UnsignedLong.ZERO);
    verify(amountTrackerMock).addAmountSent(UnsignedLong.ONE);
    verify(amountTrackerMock).addAmountDelivered(UL_TEN);
    verify(amountTrackerMock).increaseDeliveryShortfall(any());
    verify(amountTrackerMock, times(2)).getPaymentTargetConditions();
    verifyNoMoreInteractions(amountTrackerMock);
  }

  @Test
  public void doFilterWithPaymentTargetConditionsWithFulfillWithAmtClaimedValid() {
    final PaymentTargetConditions paymentTargetConditionsMock = mock(PaymentTargetConditions.class);
    when(amountTrackerMock.getPaymentTargetConditions()).thenReturn(Optional.of(paymentTargetConditionsMock));

    when(exchangeRateTrackerMock.estimateSourceAmount(any())).thenReturn(DeliveredExchangeRateBound.builder()
      .lowEndEstimate(UnsignedLong.valueOf(3L))
      .highEndEstimate(UnsignedLong.valueOf(4L))
      .build());

    this.amountFilter = new AmountFilter(paymentSharedStateTrackerMock) {
      @Override
      protected boolean isDestinationAmountValid(
        UnsignedLong destinationAmount, UnsignedLong minDestinationAmount) {
        return true;
      }

      @Override
      protected UnsignedLong computePacketDeliveryDeficitForDoFilter(
        UnsignedLong sourceAmount, Ratio minExchangeRate, UnsignedLong minDestinationAmount
      ) {
        return UnsignedLong.valueOf(5L);
      }
    };

    // Request
    final ModifiableStreamPacketRequest streamPacketRequestMock = ModifiableStreamPacketRequest.create()
      .setMinDestinationAmount(UnsignedLong.valueOf(2L))
      .setSourceAmount(UnsignedLong.ONE);
    // Reply
    final StreamPacketReply streamPacketReplyMock = Mockito.mock(StreamPacketReply.class);
    when(streamPacketReplyMock.destinationAmountClaimed()).thenReturn(Optional.of(UL_TEN));
    doCallRealMethod().when(streamPacketReplyMock).handle(any(), any());
    final InterledgerFulfillPacket fulfillResponseMock = mock(InterledgerFulfillPacket.class);
    doCallRealMethod().when(fulfillResponseMock).handle(any(), any());
    when(streamPacketReplyMock.interledgerResponsePacket()).thenReturn(Optional.of(fulfillResponseMock));
    // Chain
    final StreamPacketFilterChain streamPacketFilterChainMock = mock(StreamPacketFilterChain.class);
    when(streamPacketFilterChainMock.doFilter(streamPacketRequestMock)).thenReturn(streamPacketReplyMock);

    StreamPacketReply actualReply = amountFilter.doFilter(streamPacketRequestMock, streamPacketFilterChainMock);

    assertThat(actualReply.exception()).isEmpty();
    assertThat(actualReply.interledgerPreparePacket()).isEmpty();
    assertThat(actualReply.isAuthentic()).isFalse();
    assertThat(actualReply.isFulfill()).isFalse();
    assertThat(actualReply.isReject()).isFalse();

    verify(amountTrackerMock).addToSourceAmountInFlight(UnsignedLong.ONE);
    verify(amountTrackerMock).addToDestinationAmountInFlight(UnsignedLong.ZERO);
    verify(amountTrackerMock).subtractFromSourceAmountInFlight(UnsignedLong.ONE);
    verify(amountTrackerMock).subtractFromDestinationAmountInFlight(UnsignedLong.valueOf(2L));
    verify(amountTrackerMock).addAmountSent(UnsignedLong.ONE);
    verify(amountTrackerMock).addAmountDelivered(UL_TEN);
    verify(amountTrackerMock, times(3)).getPaymentTargetConditions();
    verify(amountTrackerMock).reduceDeliveryShortfall(UnsignedLong.valueOf(5L));
    verify(exchangeRateTrackerMock).estimateDestinationAmount(any());
    verifyNoMoreInteractions(amountTrackerMock);
  }

  @Test
  public void doFilterWithPaymentTargetConditionsWithFixedSend() {
    final PaymentTargetConditions paymentTargetConditionsMock = mock(PaymentTargetConditions.class);
    when(paymentTargetConditionsMock.paymentType()).thenReturn(PaymentType.FIXED_SEND);
    when(paymentTargetConditionsMock.minExchangeRate()).thenReturn(RATIO_TEN);
    when(amountTrackerMock.getPaymentTargetConditions()).thenReturn(Optional.of(paymentTargetConditionsMock));

    // Request
    final ModifiableStreamPacketRequest streamPacketRequestMock = ModifiableStreamPacketRequest.create()
      .setSourceAmount(UnsignedLong.ONE);
    // Reply
    final StreamPacketReply streamPacketReplyMock = Mockito.mock(StreamPacketReply.class);
    when(streamPacketReplyMock.destinationAmountClaimed()).thenReturn(Optional.of(UL_TEN));
    doCallRealMethod().when(streamPacketReplyMock).handle(any(), any());
    final InterledgerFulfillPacket fulfillResponseMock = mock(InterledgerFulfillPacket.class);
    doCallRealMethod().when(fulfillResponseMock).handle(any(), any());
    when(streamPacketReplyMock.interledgerResponsePacket()).thenReturn(Optional.of(fulfillResponseMock));
    // Chain
    final StreamPacketFilterChain streamPacketFilterChainMock = mock(StreamPacketFilterChain.class);
    when(streamPacketFilterChainMock.doFilter(streamPacketRequestMock)).thenReturn(streamPacketReplyMock);

    StreamPacketReply actualReply = amountFilter.doFilter(streamPacketRequestMock, streamPacketFilterChainMock);

    assertThat(actualReply.exception()).isEmpty();
    assertThat(actualReply.interledgerPreparePacket()).isEmpty();
    assertThat(actualReply.isAuthentic()).isFalse();
    assertThat(actualReply.isFulfill()).isFalse();
    assertThat(actualReply.isReject()).isFalse();

    verify(amountTrackerMock).addToSourceAmountInFlight(any());
    verify(amountTrackerMock).addToDestinationAmountInFlight(any());
    verify(amountTrackerMock).subtractFromSourceAmountInFlight(any());
    verify(amountTrackerMock).subtractFromDestinationAmountInFlight(any());
    verify(amountTrackerMock).addAmountSent(any());
    verify(amountTrackerMock).addAmountDelivered(UL_TEN);
    verify(amountTrackerMock, times(3)).getPaymentTargetConditions();
    verify(amountTrackerMock).reduceDeliveryShortfall(any());
    verify(exchangeRateTrackerMock).estimateDestinationAmount(any());
    verifyNoMoreInteractions(amountTrackerMock);
  }

  @Test
  public void doFilterWithPaymentTargetConditionsWithFixedDelivery() {
    final PaymentTargetConditions paymentTargetConditionsMock = mock(PaymentTargetConditions.class);
    when(paymentTargetConditionsMock.paymentType()).thenReturn(PaymentType.FIXED_DELIVERY);
    when(paymentTargetConditionsMock.minExchangeRate()).thenReturn(RATIO_TEN);
    when(amountTrackerMock.getPaymentTargetConditions()).thenReturn(Optional.of(paymentTargetConditionsMock));

    // Request
    final ModifiableStreamPacketRequest streamPacketRequestMock = ModifiableStreamPacketRequest.create()
      .setSourceAmount(UnsignedLong.ONE);
    // Reply
    final StreamPacketReply streamPacketReplyMock = Mockito.mock(StreamPacketReply.class);
    when(streamPacketReplyMock.destinationAmountClaimed()).thenReturn(Optional.of(UL_TEN));
    doCallRealMethod().when(streamPacketReplyMock).handle(any(), any());
    final InterledgerFulfillPacket fulfillResponseMock = mock(InterledgerFulfillPacket.class);
    doCallRealMethod().when(fulfillResponseMock).handle(any(), any());
    when(streamPacketReplyMock.interledgerResponsePacket()).thenReturn(Optional.of(fulfillResponseMock));
    // Chain
    final StreamPacketFilterChain streamPacketFilterChainMock = mock(StreamPacketFilterChain.class);
    when(streamPacketFilterChainMock.doFilter(streamPacketRequestMock)).thenReturn(streamPacketReplyMock);

    StreamPacketReply actualReply = amountFilter.doFilter(streamPacketRequestMock, streamPacketFilterChainMock);

    assertThat(actualReply.exception()).isEmpty();
    assertThat(actualReply.interledgerPreparePacket()).isEmpty();
    assertThat(actualReply.isAuthentic()).isFalse();
    assertThat(actualReply.isFulfill()).isFalse();
    assertThat(actualReply.isReject()).isFalse();

    verify(amountTrackerMock).addToSourceAmountInFlight(any());
    verify(amountTrackerMock).addToDestinationAmountInFlight(any());
    verify(amountTrackerMock).subtractFromSourceAmountInFlight(any());
    verify(amountTrackerMock).subtractFromDestinationAmountInFlight(any());
    verify(amountTrackerMock).addAmountSent(any());
    verify(amountTrackerMock).addAmountDelivered(UL_TEN);
    verify(amountTrackerMock, times(3)).getPaymentTargetConditions();
    verify(amountTrackerMock).reduceDeliveryShortfall(any());
    verify(exchangeRateTrackerMock).estimateDestinationAmount(any());
    verifyNoMoreInteractions(amountTrackerMock);
  }

  ////////////
  // updateReceiveMax
  ////////////
  @Test
  public void updateReceiveMaxWithNoFrames() {
    final StreamPacketReply streamPacketReplyMock = Mockito.mock(StreamPacketReply.class);

    amountFilter.updateReceiveMax(streamPacketReplyMock);
    verifyNoMoreInteractions(amountTrackerMock);
  }

  @Test
  public void updateReceiveMaxWithStreamFrameNotExpected() {
    // Reply
    final StreamPacketReply streamPacketReplyMock = Mockito.mock(StreamPacketReply.class);
    StreamFrame streamCloseFrame = StreamCloseFrame.builder()
      .streamId(UnsignedLong.ONE)
      .errorCode(ErrorCodes.NoError)
      .errorMessage("foo")
      .build();
    when(streamPacketReplyMock.frames()).thenReturn(Sets.newHashSet(streamCloseFrame));

    amountFilter.updateReceiveMax(streamPacketReplyMock);
    verifyNoMoreInteractions(amountTrackerMock);
  }

  @Test
  public void updateReceiveMaxWithStreamIdNotOne() {
    // Reply
    final StreamPacketReply streamPacketReplyMock = Mockito.mock(StreamPacketReply.class);
    StreamMoneyMaxFrame streamMoneyMaxFrame = StreamMoneyMaxFrame.builder()
      .receiveMax(UnsignedLong.ONE)
      .streamId(UnsignedLong.MAX_VALUE)
      .build();
    when(streamPacketReplyMock.frames()).thenReturn(Sets.newHashSet(streamMoneyMaxFrame));

    amountFilter.updateReceiveMax(streamPacketReplyMock);
    verifyNoMoreInteractions(amountTrackerMock);
  }

  @Test
  public void updateReceiveMaxPacketReceiveMax() {
    when(amountTrackerMock.getRemoteReceivedMax()).thenReturn(Optional.of(UnsignedLong.ONE));

    // Reply
    final StreamPacketReply streamPacketReplyMock = Mockito.mock(StreamPacketReply.class);
    StreamMoneyMaxFrame streamMoneyMaxFrame = StreamMoneyMaxFrame.builder()
      .receiveMax(UnsignedLong.valueOf(3L))
      .streamId(UnsignedLong.ONE)
      .build();
    when(streamPacketReplyMock.frames()).thenReturn(Sets.newHashSet(streamMoneyMaxFrame));

    amountFilter.updateReceiveMax(streamPacketReplyMock);
    verify(amountTrackerMock).updateRemoteMax(UnsignedLong.valueOf(3L));
    verify(amountTrackerMock).getRemoteReceivedMax();
    verifyNoMoreInteractions(amountTrackerMock);
  }

  @Test
  public void updateReceiveMaxUsingRemoteReceivedMax() {
    when(amountTrackerMock.getRemoteReceivedMax()).thenReturn(Optional.of(UnsignedLong.valueOf(2L)));

    // Reply
    final StreamPacketReply streamPacketReplyMock = Mockito.mock(StreamPacketReply.class);
    StreamMoneyMaxFrame streamMoneyMaxFrame = StreamMoneyMaxFrame.builder()
      .receiveMax(UnsignedLong.ONE)
      .streamId(UnsignedLong.ONE)
      .build();
    when(streamPacketReplyMock.frames()).thenReturn(Sets.newHashSet(streamMoneyMaxFrame));

    amountFilter.updateReceiveMax(streamPacketReplyMock);
    verify(amountTrackerMock).updateRemoteMax(UnsignedLong.valueOf(2L));
    verify(amountTrackerMock).getRemoteReceivedMax();
    verifyNoMoreInteractions(amountTrackerMock);
  }

  ////////////
  // isFailedPacket
  ////////////

  @Test
  public void testIsFailedPacketWithFulfillAndPositiveDeliveryDeficit() {
    StreamPacketReply streamPacketReplyMock = mock(StreamPacketReply.class);
    when(streamPacketReplyMock.isReject()).thenReturn(false);
    assertThat(amountFilter.isFailedPacket(UnsignedLong.ONE, streamPacketReplyMock)).isFalse();
  }

  @Test
  public void testIsFailedPacketWithRejectAndPositiveDeliveryDeficit() {
    StreamPacketReply streamPacketReplyMock = mock(StreamPacketReply.class);
    when(streamPacketReplyMock.isReject()).thenReturn(true);
    assertThat(amountFilter.isFailedPacket(UnsignedLong.ONE, streamPacketReplyMock)).isTrue();
  }

  @Test
  public void testIsFailedPacketWithRejectAndNoDeliveryDeficit() {
    StreamPacketReply streamPacketReplyMock = mock(StreamPacketReply.class);
    when(streamPacketReplyMock.isReject()).thenReturn(true);
    assertThat(amountFilter.isFailedPacket(UnsignedLong.ZERO, streamPacketReplyMock)).isFalse();
  }

  ////////////
  // isDestinationAmountValid (Optional)
  ////////////

  @Test
  public void testIsOptDestinationAmountInvalidWhenSmaller() {
    StreamPacketReply streamPacketReplyMock = mock(StreamPacketReply.class);
    when(streamPacketReplyMock.isReject()).thenReturn(false);
    assertThat(amountFilter.isDestinationAmountValid(
      Optional.of(UnsignedLong.ZERO), // <-- destinationAmount
      UnsignedLong.ONE) // <-- minDestinationAmount
    ).isFalse();
  }

  @Test
  public void testIsOptDestinationAmountValidWhenEqual() {
    StreamPacketReply streamPacketReplyMock = mock(StreamPacketReply.class);
    when(streamPacketReplyMock.isReject()).thenReturn(false);
    assertThat(amountFilter.isDestinationAmountValid(
      Optional.of(UnsignedLong.ONE), // <-- destinationAmount
      UnsignedLong.ONE) // <-- minDestinationAmount
    ).isTrue();
  }

  @Test
  public void testIsOptDestinationAmountValidWhenGreater() {
    StreamPacketReply streamPacketReplyMock = mock(StreamPacketReply.class);
    when(streamPacketReplyMock.isReject()).thenReturn(false);
    assertThat(amountFilter.isDestinationAmountValid(
      Optional.of(UnsignedLong.MAX_VALUE), // <-- destinationAmount
      UnsignedLong.ONE) // <-- minDestinationAmount
    ).isTrue();
  }

  @Test
  public void testIsOptDestinationAmountValidWhenEmpty() {
    StreamPacketReply streamPacketReplyMock = mock(StreamPacketReply.class);
    when(streamPacketReplyMock.isReject()).thenReturn(false);
    assertThat(amountFilter.isDestinationAmountValid(
      Optional.empty(), // <-- destinationAmount
      UnsignedLong.ONE) // <-- minDestinationAmount
    ).isFalse();
  }

  ////////////
  // isDestinationAmountValid
  ////////////

  @Test
  public void testIsDestinationAmountInvalidWhenSmaller() {
    StreamPacketReply streamPacketReplyMock = mock(StreamPacketReply.class);
    when(streamPacketReplyMock.isReject()).thenReturn(false);
    assertThat(amountFilter.isDestinationAmountValid(
      UnsignedLong.ZERO, // <-- destinationAmount
      UnsignedLong.ONE) // <-- minDestinationAmount
    ).isFalse();
  }

  @Test
  public void testIsDestinationAmountValidWhenEqual() {
    StreamPacketReply streamPacketReplyMock = mock(StreamPacketReply.class);
    when(streamPacketReplyMock.isReject()).thenReturn(false);
    assertThat(amountFilter.isDestinationAmountValid(
      UnsignedLong.ONE, // <-- destinationAmount
      UnsignedLong.ONE) // <-- minDestinationAmount
    ).isTrue();
  }

  @Test
  public void testIsDestinationAmountValidWhenGreater() {
    StreamPacketReply streamPacketReplyMock = mock(StreamPacketReply.class);
    when(streamPacketReplyMock.isReject()).thenReturn(false);
    assertThat(amountFilter.isDestinationAmountValid(
      UnsignedLong.MAX_VALUE, // <-- destinationAmount
      UnsignedLong.ONE) // <-- minDestinationAmount
    ).isTrue();
  }

  ////////////
  // checkForIncompatibleReceiveMax
  ////////////

  @Test
  public void testCheckForIncompatibleReceiveMaxWhenMaxIsGreater() {
    when(amountTrackerMock.getRemoteReceivedMax()).thenReturn(Optional.of(UnsignedLong.MAX_VALUE));

    final PaymentTargetConditions paymentTargetConditions = PaymentTargetConditions.builder()
      .paymentType(PaymentType.FIXED_SEND)
      .minExchangeRate(Ratio.ONE)
      .minPaymentAmountInDestinationUnits(BigInteger.ONE)
      .maxPaymentAmountInSenderUnits(BigInteger.valueOf(Long.MAX_VALUE))
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
      .minExchangeRate(Ratio.ONE)
      .minPaymentAmountInDestinationUnits(BigInteger.valueOf(2L))
      .maxPaymentAmountInSenderUnits(BigInteger.valueOf(Long.MAX_VALUE))
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
      .minExchangeRate(Ratio.ONE)
      .minPaymentAmountInDestinationUnits(BigInteger.ONE)
      .maxPaymentAmountInSenderUnits(BigInteger.valueOf(Long.MAX_VALUE))
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
      .minExchangeRate(Ratio.ONE)
      .minPaymentAmountInDestinationUnits(BigInteger.ONE)
      .maxPaymentAmountInSenderUnits(BigInteger.valueOf(Long.MAX_VALUE))
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
      .minExchangeRate(Ratio.ONE)
      .minPaymentAmountInDestinationUnits(BigInteger.ONE)
      .maxPaymentAmountInSenderUnits(BigInteger.ONE)
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
      .minExchangeRate(Ratio.ONE)
      .minPaymentAmountInDestinationUnits(BigInteger.ONE)
      .maxPaymentAmountInSenderUnits(BigInteger.ONE)
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
      .minExchangeRate(Ratio.ZERO)
      .minPaymentAmountInDestinationUnits(BigInteger.ZERO)
      .maxPaymentAmountInSenderUnits(BigInteger.ONE)
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
      .minExchangeRate(Ratio.ZERO)
      .minPaymentAmountInDestinationUnits(BigInteger.ZERO)
      .maxPaymentAmountInSenderUnits(BigInteger.ONE)
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
      .minExchangeRate(Ratio.ZERO)
      .minPaymentAmountInDestinationUnits(BigInteger.ZERO)
      .maxPaymentAmountInSenderUnits(BigInteger.ONE)
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
      .minExchangeRate(Ratio.ZERO)
      .minPaymentAmountInDestinationUnits(BigInteger.ZERO)
      .maxPaymentAmountInSenderUnits(BigInteger.ONE)
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
      .minExchangeRate(Ratio.ZERO)
      .minPaymentAmountInDestinationUnits(BigInteger.valueOf(3L))
      .maxPaymentAmountInSenderUnits(BigInteger.ONE)
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
      .minExchangeRate(Ratio.ZERO)
      .minPaymentAmountInDestinationUnits(BigInteger.valueOf(3L))
      .maxPaymentAmountInSenderUnits(BigInteger.ONE)
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
      .minExchangeRate(Ratio.ZERO)
      .minPaymentAmountInDestinationUnits(BigInteger.valueOf(3L))
      .maxPaymentAmountInSenderUnits(BigInteger.ONE)
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
      .minExchangeRate(Ratio.ZERO)
      .minPaymentAmountInDestinationUnits(BigInteger.ONE)
      .maxPaymentAmountInSenderUnits(BigInteger.ONE)
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
      .minExchangeRate(Ratio.ZERO)
      .minPaymentAmountInDestinationUnits(BigInteger.ONE)
      .maxPaymentAmountInSenderUnits(BigInteger.ONE)
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
      .minExchangeRate(Ratio.ZERO)
      .minPaymentAmountInDestinationUnits(BigInteger.ONE)
      .maxPaymentAmountInSenderUnits(BigInteger.ONE)
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
      .minExchangeRate(Ratio.ZERO)
      .minPaymentAmountInDestinationUnits(BigInteger.ONE)
      .maxPaymentAmountInSenderUnits(BigInteger.ONE)
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
      .minExchangeRate(Ratio.ONE)
      .minPaymentAmountInDestinationUnits(BigInteger.ONE)
      .maxPaymentAmountInSenderUnits(BigInteger.valueOf(3L))
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
      .minExchangeRate(Ratio.ONE)
      .minPaymentAmountInDestinationUnits(BigInteger.ONE)
      .maxPaymentAmountInSenderUnits(BigInteger.valueOf(2L))
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
      .minExchangeRate(Ratio.ONE)
      .minPaymentAmountInDestinationUnits(BigInteger.ONE)
      .maxPaymentAmountInSenderUnits(BigInteger.valueOf(2L))
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
      .minExchangeRate(Ratio.ONE)
      .minPaymentAmountInDestinationUnits(BigInteger.ONE)
      .maxPaymentAmountInSenderUnits(BigInteger.ONE)
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
    when(exchangeRateTrackerMock.estimateDestinationAmount(sourceAmount))
      .thenReturn(DeliveredExchangeRateBound.builder()
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
    final UnsignedLong deliveryDeficit = amountFilter.computeMinDestinationPacketAmount(
      UnsignedLong.ONE, // <-- sourceAmount
      Ratio.ONE // <-- minExchangeRate
    );

    assertThat(deliveryDeficit).isEqualTo(UnsignedLong.ONE);
  }

  @Test
  public void testComputeMinDestinationAmountWithHighDecimal() {
    final UnsignedLong deliveryDeficit = amountFilter.computeMinDestinationPacketAmount(
      UnsignedLong.ONE, // <-- sourceAmount
      Ratio.from(new BigDecimal("0.99")) // <-- minExchangeRate
    );

    assertThat(deliveryDeficit).isEqualTo(UnsignedLong.ONE);
  }

  @Test
  public void testComputeMinDestinationAmountWithLowDecimal() {
    final UnsignedLong deliveryDeficit = amountFilter.computeMinDestinationPacketAmount(
      UnsignedLong.ONE, // <-- sourceAmount
      Ratio.from(new BigDecimal("1.01"))// <-- minExchangeRate
    );

    assertThat(deliveryDeficit).isEqualTo(UnsignedLong.valueOf(2L));
  }

  ////////////
  //  computeDeliveryDeficitForNextState
  ////////////

  @Test
  public void testComputeDeliveryDeficitForNextStatePositive() {
    final UnsignedLong deliveryDeficit = amountFilter.computePacketDeliveryDeficitForNextState(
      UnsignedLong.ONE, // <-- minDestinationAmount
      UnsignedLong.ZERO // <-- estimatedDestinationAmount
    );

    assertThat(deliveryDeficit).isEqualTo(UnsignedLong.ONE);
  }

  @Test
  public void testComputeDeliveryDeficitForNextStateZero() {
    final UnsignedLong deliveryDeficit = amountFilter.computePacketDeliveryDeficitForNextState(
      UnsignedLong.ONE, // <-- minDestinationAmount
      UnsignedLong.ONE // <-- estimatedDestinationAmount
    );

    assertThat(deliveryDeficit).isEqualTo(UnsignedLong.ZERO);
  }

  @Test
  public void testComputeDeliveryDeficitForNextStateZero2() {
    final UnsignedLong deliveryDeficit = amountFilter.computePacketDeliveryDeficitForNextState(
      UnsignedLong.ZERO, // <-- minDestinationAmount
      UnsignedLong.ZERO // <-- estimatedDestinationAmount
    );

    assertThat(deliveryDeficit).isEqualTo(UnsignedLong.ZERO);
  }

  @Test
  public void testComputeDeliveryDeficitForNextStateNegative() {
    final UnsignedLong deliveryDeficit = amountFilter.computePacketDeliveryDeficitForNextState(
      UnsignedLong.ZERO, // <-- minDestinationAmount
      UnsignedLong.ONE // <-- estimatedDestinationAmount
    );

    assertThat(deliveryDeficit).isEqualTo(UnsignedLong.ZERO);
  }

  ////////////
  //  computeDeliveryDeficitForDoFilter
  ////////////

  @Test
  public void testComputeDeliveryDeficitForDoFilterPositive() {
    this.amountFilter = new AmountFilter(paymentSharedStateTrackerMock) {
      @Override
      protected UnsignedLong computeMinDestinationPacketAmount(UnsignedLong sourcePacketAmount, Ratio minExchangeRate) {
        return UnsignedLong.ONE;
      }
    };
    final UnsignedLong deliveryDeficit = amountFilter.computePacketDeliveryDeficitForDoFilter(
      UnsignedLong.ONE, // <-- sourceAmount
      Ratio.ONE, // <-- minExchangeRate
      UnsignedLong.ONE // <-- minDestinationAmount
    );

    assertThat(deliveryDeficit).isEqualTo(UnsignedLong.ZERO);
  }

  @Test
  public void testComputeDeliveryDeficitForDoFilterNegative() {
    this.amountFilter = new AmountFilter(paymentSharedStateTrackerMock) {
      @Override
      protected UnsignedLong computeMinDestinationPacketAmount(UnsignedLong sourcePacketAmount, Ratio minExchangeRate) {
        return UnsignedLong.ZERO;
      }
    };
    final UnsignedLong deliveryDeficit = amountFilter.computePacketDeliveryDeficitForDoFilter(
      UnsignedLong.ONE, // <-- sourceAmount
      Ratio.ONE, // <-- minExchangeRate
      UnsignedLong.ONE // <-- minDestinationAmount
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
      .minExchangeRate(Ratio.ZERO)
      .minPaymentAmountInDestinationUnits(BigInteger.valueOf(3L))
      .maxPaymentAmountInSenderUnits(BigInteger.ONE)
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
      .minExchangeRate(Ratio.ZERO)
      .minPaymentAmountInDestinationUnits(BigInteger.valueOf(3L))
      .maxPaymentAmountInSenderUnits(BigInteger.ONE)
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
      .minExchangeRate(Ratio.ZERO)
      .minPaymentAmountInDestinationUnits(BigInteger.valueOf(3L))
      .maxPaymentAmountInSenderUnits(BigInteger.ONE)
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
      .minExchangeRate(Ratio.ZERO)
      .minPaymentAmountInDestinationUnits(BigInteger.valueOf(Long.MAX_VALUE)) // <-- Variable
      .maxPaymentAmountInSenderUnits(BigInteger.ONE)
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
      .minExchangeRate(Ratio.ZERO)
      .minPaymentAmountInDestinationUnits(BigInteger.ONE) // <-- Variable
      .maxPaymentAmountInSenderUnits(BigInteger.ONE)
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
      .minExchangeRate(Ratio.ZERO)
      .minPaymentAmountInDestinationUnits(BigInteger.ZERO) // <-- Variable
      .maxPaymentAmountInSenderUnits(BigInteger.ONE)
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

  //////////////////
  // Private Helpers
  //////////////////

  private void initializeHappyPath() {
    // amountTrackerMock
    when(amountTrackerMock.getAvailableDeliveryShortfall()).thenReturn(UnsignedLong.ZERO);
    when(amountTrackerMock.getAmountSentInSourceUnits()).thenReturn(BigInteger.ONE);
    when(amountTrackerMock.getAmountDeliveredInDestinationUnits()).thenReturn(BigInteger.ONE);
    when(amountTrackerMock.getSourceAmountInFlight()).thenReturn(BigInteger.ZERO);
    when(amountTrackerMock.getDestinationAmountInFlight()).thenReturn(BigInteger.ZERO);
    when(amountTrackerMock.getRemoteReceivedMax()).thenReturn(Optional.of(UnsignedLong.MAX_VALUE));
    when(amountTrackerMock.getPaymentTargetConditions()).thenReturn(Optional.of(
      PaymentTargetConditions.builder()
        .paymentType(PaymentType.FIXED_SEND)
        .minExchangeRate(Ratio.ONE)
        .minPaymentAmountInDestinationUnits(BigInteger.ONE)
        .maxPaymentAmountInSenderUnits(BigInteger.valueOf(Long.MAX_VALUE))
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
      DeliveredExchangeRateBound.builder()
        .lowEndEstimate(UnsignedLong.ONE.plus(UnsignedLong.ONE)) // <-- Assumes 1:1 FX
        .highEndEstimate(UnsignedLong.ONE.plus(UnsignedLong.ONE)) // <-- Assumes 1:1 FX
        .build()
    );
    when(exchangeRateTrackerMock.estimateSourceAmount(any())).thenReturn(
      DeliveredExchangeRateBound.builder().lowEndEstimate(UnsignedLong.ONE).highEndEstimate(UnsignedLong.ONE).build()
    );
  }
}