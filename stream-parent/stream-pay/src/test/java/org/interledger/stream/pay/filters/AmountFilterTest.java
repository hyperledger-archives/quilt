package org.interledger.stream.pay.filters;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.interledger.core.fluent.Ratio;
import org.interledger.stream.crypto.SharedSecret;
import org.interledger.stream.frames.ErrorCodes;
import org.interledger.stream.pay.model.ModifiableStreamPacketRequest;
import org.interledger.stream.pay.model.SendState;
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
import java.util.Objects;
import java.util.Optional;

/**
 * Unit test for {@link AmountFilter}.
 */
public class AmountFilterTest {

  private static final SharedSecret SHARED_SECRET = SharedSecret.of(new byte[32]);

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

    // amountTrackerMock
    when(amountTrackerMock.getRemoteReceivedMax()).thenReturn(Optional.of(UnsignedLong.MAX_VALUE));
    when(amountTrackerMock.getPaymentTargetConditions()).thenReturn(Optional.of(
      PaymentTargetConditions.builder()
        .paymentType(PaymentType.FIXED_SEND)
        .minExchangeRate(BigDecimal.ONE)
        .minDeliveryAmount(BigInteger.ONE)
        .maxSourceAmount(BigInteger.ONE)
        .build()
    ));
    when(amountTrackerMock.getAvailableDeliveryShortfall()).thenReturn(BigInteger.ZERO);
    when(amountTrackerMock.getSourceAmountInFlight()).thenReturn(BigInteger.ZERO);
    when(amountTrackerMock.getDestinationAmountInFlight()).thenReturn(BigInteger.ZERO);
    when(amountTrackerMock.getAmountSentInSourceUnits()).thenReturn(BigInteger.ZERO);
    when(amountTrackerMock.getAmountDeliveredInDestinationUnits()).thenReturn(BigInteger.ZERO);

    // maxPacketAmountTrackerMock
    when(maxPacketAmountTrackerMock.getMaxPacketAmount()).thenReturn(MaxPacketAmount.unknownMax());
    when(maxPacketAmountTrackerMock.getNextMaxPacketAmount()).thenReturn(UnsignedLong.MAX_VALUE);
    when(maxPacketAmountTrackerMock.verifiedPathCapacity()).thenReturn(UnsignedLong.ZERO);

    // exchangeRateTrackerMock
    when(exchangeRateTrackerMock.getLowerBoundRate()).thenReturn(Ratio.ONE);
    when(exchangeRateTrackerMock.getUpperBoundRate()).thenReturn(Ratio.ONE);
    when(exchangeRateTrackerMock.estimateDestinationAmount(UnsignedLong.valueOf(10L))).thenReturn(
      ExchangeRateBound.builder()
        .lowEndEstimate(UnsignedLong.valueOf(10L))
        .highEndEstimate(UnsignedLong.valueOf(10L))
        .build()
    );
    when(exchangeRateTrackerMock.estimateSourceAmount(any())).thenReturn(
      ExchangeRateBound.builder().lowEndEstimate(UnsignedLong.ONE).highEndEstimate(UnsignedLong.ONE).build()
    );

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
  }

  /**
   * When min-delivery amount is greater than the receive-max.
   */
  @Test
  public void nextStateWithIncompatibleReceiveMax() {
    when(amountTrackerMock.getRemoteReceivedMax()).thenReturn(Optional.of(UnsignedLong.ONE));
    when(amountTrackerMock.getPaymentTargetConditions()).thenReturn(Optional.of(
      PaymentTargetConditions.builder()
        .paymentType(PaymentType.FIXED_SEND)
        .minExchangeRate(BigDecimal.ONE)
        .minDeliveryAmount(BigInteger.TEN) // <-- Makes the receiveMax of one invalid.
        .maxSourceAmount(BigInteger.ONE)
        .build()
    ));
    ModifiableStreamPacketRequest request = ModifiableStreamPacketRequest.create().setSourceAmount(
      UnsignedLong.valueOf(10L)
    );
    SendState result = amountFilter.nextState(request);
    assertThat(result).isEqualTo(SendState.IncompatibleReceiveMax);
    assertThat(request.streamErrorCodeForConnectionClose()).isEqualTo(ErrorCodes.ApplicationError);
  }
  /////////////////
  // Fixed Send
  /////////////////

  @Test
  public void nextStateWithAmountSentEqualToTarget() {
    when(amountTrackerMock.getAmountSentInSourceUnits()).thenReturn(BigInteger.TEN);
    when(amountTrackerMock.getSourceAmountInFlight()).thenReturn(BigInteger.ZERO); // <-- Nothing in-flight
    when(amountTrackerMock.getPaymentTargetConditions()).thenReturn(Optional.of(
      PaymentTargetConditions.builder()
        .paymentType(PaymentType.FIXED_SEND)
        .minExchangeRate(BigDecimal.ONE)
        .minDeliveryAmount(BigInteger.ONE)
        .maxSourceAmount(BigInteger.TEN) // <-- Must match amountTrackerMock.getAmountSentInSourceUnits()
        .build()
    ));

    ModifiableStreamPacketRequest request = ModifiableStreamPacketRequest.create().setSourceAmount(
      UnsignedLong.valueOf(10L)
    );
    SendState result = amountFilter.nextState(request);
    assertThat(result).isEqualTo(SendState.End);
    assertThat(request.streamErrorCodeForConnectionClose()).isEqualTo(ErrorCodes.NoError);
  }

  @Test
  public void nextStateWithAmountSentEqualToTargetWithInFlight() {
    when(amountTrackerMock.getAmountSentInSourceUnits()).thenReturn(BigInteger.TEN);
    when(amountTrackerMock.getSourceAmountInFlight()).thenReturn(BigInteger.ONE); // <-- Something in-flight
    when(amountTrackerMock.getPaymentTargetConditions()).thenReturn(Optional.of(
      PaymentTargetConditions.builder()
        .paymentType(PaymentType.FIXED_SEND)
        .minExchangeRate(BigDecimal.ONE)
        .minDeliveryAmount(BigInteger.ONE)
        .maxSourceAmount(BigInteger.TEN) // <-- Must match amountTrackerMock.getAmountSentInSourceUnits()
        .build()
    ));

    ModifiableStreamPacketRequest request = ModifiableStreamPacketRequest.create().setSourceAmount(
      UnsignedLong.valueOf(10L)
    );
    SendState result = amountFilter.nextState(request);
    assertThat(result).isEqualTo(SendState.Wait);
    assertThat(request.streamErrorCodeForConnectionClose()).isEqualTo(ErrorCodes.NoError);
  }

  // anyInFlight=false; anyAvailableToSend=false
  // anyInFlight=false; anyAvailableToSend=true
  // anyInFlight=true; anyAvailableToSend=false
  // anyInFlight=true; anyAvailableToSend=true

  @Test
  public void nextStateWithAnyInFlightFalseAndAnyToSendFalse() {
    this.initializeMoreToSendForFixedSend(
      BigInteger.ONE, // <-- Total to send.
      BigInteger.ONE, // <-- AnyLeftToSend == false
      BigInteger.ONE,
      BigInteger.ZERO, // <-- Any in-flight == false
      BigInteger.ZERO
    );

    ModifiableStreamPacketRequest request = ModifiableStreamPacketRequest.create().setSourceAmount(
      UnsignedLong.ONE
    );
    SendState result = amountFilter.nextState(request);
    assertThat(result).isEqualTo(SendState.End);
    assertThat(request.streamErrorCodeForConnectionClose()).isEqualTo(ErrorCodes.NoError);
  }

  @Test
  public void nextStateWithAnyInFlightFalseAndAnyToSendTrue() {
    this.initializeMoreToSendForFixedSend(
      BigInteger.valueOf(10L), // <-- Total to send.
      BigInteger.ZERO, // <-- AnyLeftToSend == true
      BigInteger.ZERO,
      BigInteger.ZERO, // <-- Any in-flight == false
      BigInteger.ZERO
    );

    ModifiableStreamPacketRequest request = ModifiableStreamPacketRequest.create().setSourceAmount(
      UnsignedLong.valueOf(10L)
    );
    SendState result = amountFilter.nextState(request);
    assertThat(result).isEqualTo(SendState.Ready);
    assertThat(request.streamErrorCodeForConnectionClose()).isEqualTo(ErrorCodes.NoError);
  }

  @Test
  public void nextStateWithAnyInFlightTrueAndAnyToSendFalse() {
    this.initializeMoreToSendForFixedSend(
      BigInteger.TEN, // <-- Total to send.
      BigInteger.TEN, // <-- AnyLeftToSend == false
      BigInteger.TEN,
      BigInteger.ONE, // <-- Any in-flight == true
      BigInteger.ONE
    );

    ModifiableStreamPacketRequest request = ModifiableStreamPacketRequest.create().setSourceAmount(
      UnsignedLong.valueOf(10L)
    );
    SendState result = amountFilter.nextState(request);
    assertThat(result).isEqualTo(SendState.Wait);
    assertThat(request.streamErrorCodeForConnectionClose()).isEqualTo(ErrorCodes.NoError);
  }

  @Test
  public void nextStateWithAnyInFlightTrueAndAnyToSendTrue() {
    this.initializeMoreToSendForFixedSend(
      BigInteger.TEN, // <-- Total to send.
      BigInteger.ONE, // <-- AnyLeftToSend == true
      BigInteger.ONE,
      BigInteger.ONE, // <-- Any in-flight == true
      BigInteger.ONE
    );

    ModifiableStreamPacketRequest request = ModifiableStreamPacketRequest.create().setSourceAmount(
      UnsignedLong.valueOf(10L)
    );
    SendState result = amountFilter.nextState(request);
    assertThat(result).isEqualTo(SendState.Ready);
    assertThat(request.streamErrorCodeForConnectionClose()).isEqualTo(ErrorCodes.NoError);
  }

  /////////////////
  // Fixed Delivery
  /////////////////

  @Test
  public void nextStateWithFixedDeliveryFullyPaid() {
    this.initializeMoreToSendForFixedDelivery(
      BigInteger.valueOf(2L), // <-- Total to send = 1
      BigInteger.ONE, // <-- AmountSent = 1
      BigInteger.valueOf(2L), // <-- AmountDelivered = 2
      BigInteger.ZERO,// <-- Source in-flight = 0
      BigInteger.ZERO // <-- Dest amt in-flight = 0
    );

    ModifiableStreamPacketRequest request = ModifiableStreamPacketRequest.create().setSourceAmount(
      UnsignedLong.valueOf(2L)
    );
    SendState result = amountFilter.nextState(request);
    assertThat(result).isEqualTo(SendState.End); // <-- Stop Sending!
    assertThat(request.streamErrorCodeForConnectionClose()).isEqualTo(ErrorCodes.NoError);
  }

  @Test
  public void nextStateWithFixedDeliveryNotYetFullyPaid() {
    this.initializeMoreToSendForFixedDelivery(
      BigInteger.valueOf(2L), // <-- Total to send = 1
      BigInteger.ZERO, // <-- AmountSent = 1
      BigInteger.ZERO, // <-- AmountDelivered = 2
      BigInteger.ZERO,// <-- Source in-flight = 0
      BigInteger.ZERO // <-- Dest amt in-flight = 0
    );

    ModifiableStreamPacketRequest request = ModifiableStreamPacketRequest.create().setSourceAmount(
      UnsignedLong.valueOf(2L)
    );
    SendState result = amountFilter.nextState(request);
    assertThat(result).isEqualTo(SendState.Ready); // <-- Send more!
    assertThat(request.streamErrorCodeForConnectionClose()).isEqualTo(ErrorCodes.NoError);
  }

  @Test
  public void nextStateWithFixedDeliveryNotYetFullyPaidWithEnoughInFlight() {
    this.initializeMoreToSendForFixedDelivery(
      BigInteger.valueOf(2L), // <-- Total to send = 1
      BigInteger.ZERO, // <-- AmountSent = 0
      BigInteger.ZERO, // <-- AmountDelivered = 0
      BigInteger.ONE,// <-- Source in-flight = 0
      BigInteger.valueOf(2L) // <-- Dest amt in-flight = 0
    );

    ModifiableStreamPacketRequest request = ModifiableStreamPacketRequest.create().setSourceAmount(
      UnsignedLong.valueOf(2L)
    );
    SendState result = amountFilter.nextState(request);
    assertThat(result).isEqualTo(SendState.Wait); // <-- Don't send more, but wait.
    assertThat(request.streamErrorCodeForConnectionClose()).isEqualTo(ErrorCodes.NoError);
  }

  @Test
  public void nextStateWithFixedDeliveryWithDeliveryDeficit() {
    this.initializeMoreToSendForFixedSend(
      BigInteger.valueOf(11L), // <-- Total to send = 1
      BigInteger.valueOf(9L), // <-- AmountSent = 0
      BigInteger.valueOf(18L), // <-- AmountDelivered = 0
      BigInteger.ONE,// <-- Source in-flight = 0
      BigInteger.valueOf(2L) // <-- Dest amt in-flight = 0
    );

    when(amountTrackerMock.getPaymentTargetConditions()).thenReturn(Optional.of(
      PaymentTargetConditions.builder()
        .paymentType(PaymentType.FIXED_SEND)
        .minExchangeRate(BigDecimal.ONE)
        .minDeliveryAmount(BigInteger.valueOf(11L))
        .maxSourceAmount(BigInteger.valueOf(Long.MAX_VALUE))
        .build()
    ));

    ModifiableStreamPacketRequest request = ModifiableStreamPacketRequest.create().setSourceAmount(
      UnsignedLong.valueOf(10L)
    );
    SendState result = amountFilter.nextState(request);
    assertThat(result).isEqualTo(SendState.InsufficientExchangeRate);
    assertThat(request.streamErrorCodeForConnectionClose()).isEqualTo(ErrorCodes.NoError);
  }

  @Test
  public void nextStateWithFixedDeliveryWithDeliveryDeficitThatDoesNotCompletePayment() {
    BigInteger amountToSend = BigInteger.valueOf(13L);

    this.initializeMoreToSendForFixedDelivery(
      amountToSend,
      BigInteger.valueOf(6L),
      BigInteger.valueOf(11L),
      BigInteger.ONE,
      BigInteger.valueOf(2L)
    );

    when(amountTrackerMock.getPaymentTargetConditions()).thenReturn(Optional.of(
      PaymentTargetConditions.builder()
        .paymentType(PaymentType.FIXED_DELIVERY)
        .minExchangeRate(BigDecimal.ONE)
        .minDeliveryAmount(BigInteger.valueOf(14L))
        .maxSourceAmount(BigInteger.valueOf(Long.MAX_VALUE))
        .build()
    ));

    when(exchangeRateTrackerMock
      .estimateSourceAmount(any()))
      .thenReturn(
        ExchangeRateBound.builder()
          .lowEndEstimate(UnsignedLong.ZERO)
          .highEndEstimate(UnsignedLong.ZERO) // <-- Triggers bad rates.
          .build()
      );

    ModifiableStreamPacketRequest request = ModifiableStreamPacketRequest.create().setSourceAmount(
      UnsignedLong.valueOf(13L)
    );
    SendState result = amountFilter.nextState(request);
    assertThat(result).isEqualTo(SendState.InsufficientExchangeRate);
    assertThat(request.streamErrorCodeForConnectionClose()).isEqualTo(ErrorCodes.NoError);
  }

  ////////////
  // doFilter
  ////////////

  @Test
  public void doFilter() {
  }

  ////////////
  // updateReceiveMax
  ////////////

  @Test
  public void updateReceiveMax() {
  }

  //////////////////
  // Private Helpers
  //////////////////

  private void initializeMoreToSendForFixedSend(
    final BigInteger totalToSend,
    final BigInteger amountSent, final BigInteger amountDelivered,
    final BigInteger sourceAmountInFlight, final BigInteger destinationAmountInFlight
  ) {
    Objects.requireNonNull(totalToSend);
    Objects.requireNonNull(amountSent);
    Objects.requireNonNull(amountDelivered);
    Objects.requireNonNull(sourceAmountInFlight);
    Objects.requireNonNull(destinationAmountInFlight);

    when(amountTrackerMock.getAvailableDeliveryShortfall()).thenReturn(BigInteger.ZERO);
    when(amountTrackerMock.getAmountSentInSourceUnits()).thenReturn(amountSent);
    when(amountTrackerMock.getAmountDeliveredInDestinationUnits()).thenReturn(amountDelivered);
    when(amountTrackerMock.getSourceAmountInFlight()).thenReturn(sourceAmountInFlight);
    when(amountTrackerMock.getDestinationAmountInFlight()).thenReturn(destinationAmountInFlight);

    when(amountTrackerMock.getPaymentTargetConditions()).thenReturn(Optional.of(
      PaymentTargetConditions.builder()
        .paymentType(PaymentType.FIXED_SEND)
        .minExchangeRate(BigDecimal.ONE)
        .minDeliveryAmount(totalToSend)
        .maxSourceAmount(totalToSend)
        .build()
    ));

    when(exchangeRateTrackerMock.estimateDestinationAmount(any())).thenReturn(
      ExchangeRateBound.builder()
        .lowEndEstimate(UnsignedLong.valueOf(totalToSend.add(BigInteger.ONE))) // <-- Assumes 1:1 FX
        .highEndEstimate(UnsignedLong.valueOf(totalToSend.add(BigInteger.ONE))) // <-- Assumes 1:1 FX
        .build()
    );
  }

  private void initializeMoreToSendForFixedDelivery(
    final BigInteger totalToSendInDestinationUnits,
    final BigInteger amountSentInSendersUnits, final BigInteger amountDeliveredInDestinationUnits,
    final BigInteger sourceAmountInFlight, final BigInteger destinationAmountInFlight
  ) {
    Objects.requireNonNull(totalToSendInDestinationUnits);
    Objects.requireNonNull(amountSentInSendersUnits);
    Objects.requireNonNull(amountDeliveredInDestinationUnits);
    Objects.requireNonNull(sourceAmountInFlight);
    Objects.requireNonNull(destinationAmountInFlight);

    when(amountTrackerMock.getAvailableDeliveryShortfall()).thenReturn(BigInteger.ZERO);
    when(amountTrackerMock.getAmountSentInSourceUnits()).thenReturn(amountSentInSendersUnits);
    when(amountTrackerMock.getAmountDeliveredInDestinationUnits()).thenReturn(amountDeliveredInDestinationUnits);
    when(amountTrackerMock.getSourceAmountInFlight()).thenReturn(sourceAmountInFlight);
    when(amountTrackerMock.getDestinationAmountInFlight()).thenReturn(destinationAmountInFlight);

    when(amountTrackerMock.getPaymentTargetConditions()).thenReturn(Optional.of(
      PaymentTargetConditions.builder()
        .paymentType(PaymentType.FIXED_DELIVERY)
        .minExchangeRate(BigDecimal.ONE)
        .minDeliveryAmount(totalToSendInDestinationUnits)
        .maxSourceAmount(totalToSendInDestinationUnits)
        .build()
    ));

    when(exchangeRateTrackerMock
      .estimateDestinationAmount(any()))
      .thenReturn(
        ExchangeRateBound.builder()
          .lowEndEstimate(UnsignedLong.valueOf(amountSentInSendersUnits.add(BigInteger.valueOf(2L))))
          .highEndEstimate(UnsignedLong.valueOf(amountSentInSendersUnits.add(BigInteger.valueOf(2L))))
          .build()
      );

  }
}