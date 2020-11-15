package org.interledger.stream.sender.good;

import static org.interledger.core.fluent.FluentCompareTo.is;
import static org.interledger.stream.StreamUtils.generatedFulfillableFulfillment;
import static org.interledger.stream.StreamUtils.max;
import static org.interledger.stream.StreamUtils.min;

import org.interledger.codecs.stream.StreamCodecContextFactory;
import org.interledger.core.DateUtils;
import org.interledger.core.InterledgerCondition;
import org.interledger.core.InterledgerErrorCode;
import org.interledger.core.InterledgerFulfillPacket;
import org.interledger.core.InterledgerPacketType;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerRejectPacket;
import org.interledger.core.InterledgerResponsePacket;
import org.interledger.link.Link;
import org.interledger.fx.Denomination;
import org.interledger.stream.StreamConnection;
import org.interledger.stream.StreamConnectionId;
import org.interledger.stream.StreamException;
import org.interledger.stream.StreamPacket;
import org.interledger.stream.crypto.StreamEncryptionUtils;
import org.interledger.stream.errors.StreamConnectionClosedException;
import org.interledger.stream.frames.ConnectionAssetDetailsFrame;
import org.interledger.stream.frames.ConnectionNewAddressFrame;
import org.interledger.stream.frames.ErrorCodes;
import org.interledger.stream.frames.StreamCloseFrame;
import org.interledger.stream.frames.StreamFrame;
import org.interledger.stream.frames.StreamFrameType;
import org.interledger.stream.frames.StreamMoneyFrame;
import org.interledger.stream.fx.ExchangeRateService;
import org.interledger.stream.good.SendMoneyRequest;
import org.interledger.stream.good.SendMoneyResult;
import org.interledger.stream.sender.AimdCongestionController;
import org.interledger.stream.sender.CongestionController;
import org.interledger.stream.sender.StreamConnectionManager;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.google.common.primitives.UnsignedLong;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.annotation.concurrent.ThreadSafe;

/**
 * <p>A default implementation of {@link StreamSender} that opens a STREAM connection, sends money, and then closes the
 * connection, yielding a response.</p>
 *
 * <p>Note that this implementation does not currently support sending data, which is defined in the STREAM
 * protocol.</p>
 *
 * <p>Finally, per https://github.com/hyperledger/quilt/issues/242, as of the publication of this client,
 * connectors will reject ILP packets that exceed 32kb. This implementation does not overtly restrict the size of the
 * data field in any particular {@link InterledgerPreparePacket}, for two reasons. First, this implementation never
 * packs a sufficient number of STREAM frames into a single Prepare packet for this 32kb limit to ever be reached;
 * Second, if the ILPv4 RFC ever changes to increase this size limitation, we don't want sender/receiver software to
 * have to be updated across the Interledger.</p>
 */
@ThreadSafe
public class DefaultStreamSender implements StreamSender {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  // TODO: StreamSender Event listener! (can be used by Neil's packet-aggregator code).

  private final Link link;
  private final StreamConnectionManager streamConnectionManager;
  private final StreamEncryptionUtils streamEncryptionUtils;
  private final ExchangeRateService exchangeRateService;

  // The overall executor for this payment. Times-out the payment at the appropriate time.
  private final ExecutorService executorService;

  /**
   * Required-args Constructor.
   *
   * @param link                A {@link Link} that is used to send ILPv4 packets to an immediate peer.
   * @param exchangeRateService
   */
  public DefaultStreamSender(final Link link, final ExchangeRateService exchangeRateService) {
    this(
        new StreamConnectionManager(),
        StreamEncryptionUtils.sharedStreamEncryptionUtils(),
        exchangeRateService,
        link,
        newDefaultExecutor());
  }

  /**
   * Required-args Constructor.
   *
   * @param streamConnectionManager
   * @param exchangeRateService
   * @param link                    A {@link Link} that is used to send ILPv4 packets to an immediate peer.
   * @param executorService         A {@link ExecutorService} to run the payments.
   */
  // TODO: Javadock
  public DefaultStreamSender(
      final StreamConnectionManager streamConnectionManager,
      final StreamEncryptionUtils streamEncryptionUtils,
      final ExchangeRateService exchangeRateService,
      final Link link,
      final ExecutorService executorService
  ) {
    this.link = Objects.requireNonNull(link);
    this.streamConnectionManager = streamConnectionManager;
    this.streamEncryptionUtils = streamEncryptionUtils;
    this.exchangeRateService = exchangeRateService;

    // Note that pools with similar properties but different details (for example, timeout parameters) may be
    // created using {@link ThreadPoolExecutor} constructors.
    this.executorService = Objects.requireNonNull(executorService);
  }

  // TODO: Javadoc
  private static ExecutorService newDefaultExecutor() {
    ThreadFactory factory = new ThreadFactoryBuilder()
        .setDaemon(true)
        .setNameFormat("simple-stream-sender-%d")
        .build();
    return Executors.newFixedThreadPool(5, factory);
  }

  @Override
  public CompletableFuture<SendMoneyResult> sendMoney(final SendMoneyRequest sendMoneyRequest) {
    Objects.requireNonNull(sendMoneyRequest);

    final StreamConnection streamConnection = this.streamConnectionManager.openConnection(
        StreamConnectionId.from(sendMoneyRequest.destinationAddress(), sendMoneyRequest.sharedSecret())
    );

    // If sendMoneyRequest has no receiverDenomination, then try to determine via preflight.
    final Optional<Denomination> receiverDenomination = determineReceiverDenomination(streamConnection,
        sendMoneyRequest);
    final SendMoneyRequest adjustedSendMoneyRequest = SendMoneyRequest.builder().from(sendMoneyRequest)
        .receiverDenomination(receiverDenomination)
        .build();

    return new StreamSenderAggregator(
        this.executorService,
        streamConnection,
        new AimdCongestionController(
            UnsignedLong.valueOf(1000L),
            adjustedSendMoneyRequest.senderAmount().dividedBy(UnsignedLong.valueOf(10L)),
            BigDecimal.valueOf(2.0),
            StreamCodecContextFactory.oer()
        ),
        streamEncryptionUtils,
        new DefaultPaymentTracker(sendMoneyRequest.senderAmount()),
        this.exchangeRateService,
        this.link,
        adjustedSendMoneyRequest
    ).send();
  }

  // TODO: JavaDoc
  @VisibleForTesting
  Optional<Denomination> determineReceiverDenomination(
      final StreamConnection streamConnection, final SendMoneyRequest sendMoneyRequest
  ) {
    Objects.requireNonNull(streamConnection);
    Objects.requireNonNull(sendMoneyRequest);

    return sendMoneyRequest.receiverDenomination()
        .map(Optional::of)
        .orElseGet(() -> {
          try {
            return preflightCheck(streamConnection, sendMoneyRequest);
          } catch (Exception e) {
            final String errorMessage = String
                .format("Preflight check failed. sendMoneyRequest=%s error=%s", sendMoneyRequest, e.getMessage());
            throw new StreamException(errorMessage, e);
          }
        });
  }

  /**
   * <p>Attempts to "pre-flight" the Connection before actual value is transferred.</p>
   *
   * <p>This operation is used to initialize a new Stream connection in order to fulfill any prerequisites necessary
   * before sending real value. For example, it is necessary to obtain the receiver's "Connection Asset Details" before
   * a sender can send value, in order to manage slippage for the sender.</p>
   *
   * @return An optionally-present {@link Denomination} that contains the asset information for the receiver. If the
   *     receiver does not advertise their denomination, then none will be returned.
   *
   * @throws StreamConnectionClosedException if the Stream connection was be closed and cannot be used.
   */
  @VisibleForTesting
  Optional<Denomination> preflightCheck(
      final StreamConnection streamConnection, final SendMoneyRequest sendMoneyRequest
  ) throws StreamConnectionClosedException {
    Objects.requireNonNull(streamConnection);
    Objects.requireNonNull(sendMoneyRequest);

    // Load up the STREAM packet
    final UnsignedLong sequence;
    try {
      sequence = streamConnection.nextSequence();
    } catch (StreamConnectionClosedException e) {
      // The Connection is closed, so we can't send anything more on it.
      logger.error(
          "Unable to send more packets on a closed StreamConnection. streamConnection={} error={}",
          streamConnection, e
      );
      throw e;
    }

    final List<StreamFrame> frames = Lists.newArrayList(
        StreamMoneyFrame.builder()
            // This aggregator supports only a single stream-id, which is one.
            .streamId(UnsignedLong.ONE)
            .shares(UnsignedLong.ONE)
            .build(),
        // Always send a ConnectionNewAddress frame (sometimes with an empty address) in order to trigger the receiver
        // on the other side of this STREAM to send us our ConnectionAssetDetails. This is odd behavior, but the
        // RFC authors can't agree on a standardization in IL-RFC-29. So until then, we have this dirty little secret
        // in every implementation that just does this. Read more in the following issues:
        // https://github.com/interledger/rfcs/issues/554
        // https://github.com/interledger/rfcs/issues/571
        // https://github.com/interledger/rfcs/pull/573
        ConnectionNewAddressFrame.builder()
            .sourceAddress(sendMoneyRequest.senderAddress())
            .build(),
        ConnectionAssetDetailsFrame.builder()
            .sourceDenomination(sendMoneyRequest.senderDenomination())
            .build()
    );

    final StreamPacket streamPacket = StreamPacket.builder()
        .interledgerPacketType(InterledgerPacketType.PREPARE)
        .prepareAmount(UnsignedLong.ZERO)
        .sequence(sequence)
        .frames(frames)
        .build();

    // Create the ILP Prepare packet
    final byte[] streamPacketData = streamEncryptionUtils.toEncrypted(sendMoneyRequest.sharedSecret(), streamPacket);
    final InterledgerCondition executionCondition =
        generatedFulfillableFulfillment(sendMoneyRequest.sharedSecret(), streamPacketData).getCondition();

    final InterledgerPreparePacket preparePacket = InterledgerPreparePacket.builder()
        .destination(sendMoneyRequest.destinationAddress())
        .amount(UnsignedLong.ZERO)
        .executionCondition(executionCondition)
        .expiresAt(DateUtils.now().plus(sendMoneyRequest.perPacketTimeout()))
        .data(streamPacketData)
        .typedData(streamPacket)
        .build();

    // The function that parses out the STREAM packets...
    final Function<InterledgerResponsePacket, Optional<Denomination>> readDetailsFromStream = (responsePacket) -> {
      final StreamPacket packet = streamEncryptionUtils
          .fromEncrypted(sendMoneyRequest.sharedSecret(), responsePacket.getData());
      if (packet != null) {
        return packet.frames().stream()
            .filter(f -> f.streamFrameType() == StreamFrameType.ConnectionAssetDetails)
            .findFirst()
            .map(f -> (ConnectionAssetDetailsFrame) f)
            .map(f -> Denomination.builder().from(f.sourceDenomination()).build());
      } else {
        return Optional.empty();
      }
    };

    return link.sendPacket(preparePacket)
        // We typically expect this Prepare operation to reject, but regardless of whether the response is a fulfill
        // or a reject, try to read the Receiver's Connection Asset Details and return them.
        .map(
            readDetailsFromStream::apply,
            readDetailsFromStream::apply
        );
  }

  /**
   * <p>Sends a single payment on a pre-existing STREAM Connection using the next available stream identifier. This
   * implementation uses a "sender-amount" mode where the amount being sent is denominated in the sender's units, and
   * only that amount can be sent. This guards against ILP network or other attacks where the sender's funds might be
   * drained if the FX rates become very bad. In other words, a sender _never_ wants to send an unlimited amount of
   * funds in order to complete a payment (at least, that's the assumption for this implementation - if that assumption
   * is incorrect, an alternative implementation should be used).</p>
   *
   * <p>Despite this implementation always using the sender's units to denominate the payment, a payment _may_ be
   * cancelled if the FX rate exceeds a certain pre-defined slippage amount. In other words, if slippage is defined,
   * then the payment will not continue if a node in the payment path takes too much money.</p>
   */
  static class StreamSenderAggregator {

    private static final BigDecimal ONE_POINT_TWO = new BigDecimal("1.2");

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final StreamConnection streamConnection;
    private final StreamEncryptionUtils streamEncryptionUtils;
    private final CongestionController congestionController;
    private final Link link;
    private final ExchangeRateService exchangeRateService;

    private final AtomicBoolean shouldSendSourceAddress;

    private final PaymentTracker paymentTracker;
    private final SendMoneyRequest sendMoneyRequest;

    // TODO: Payment Event Handler. See Rust code.
    // The Executor that sends packets.
    private final ExecutorService perPacketExecutorService;

    // TODO: Fix Javadoc

    /**
     * Required-args Constructor.
     *
     * @param perPacketExecutorService An {@link ExecutorService} for sending each packet of a Stream.
     * @param streamConnection         A {@link StreamConnection} that can be used to send packets with.
     * @param congestionController     A {@link CongestionController} that supports back-pressure for money streams.
     * @param streamEncryptionUtils    A {@link StreamEncryptionUtils} for performing Stream encryption operations.
     * @param paymentTracker
     * @param exchangeRateService
     * @param link                     The {@link Link} used to send ILPv4 packets containing Stream packets.
     * @param sendMoneyRequest         A {@link SendMoneyRequest} that contains all relevant details about the money to
     */
    public StreamSenderAggregator(
        final ExecutorService perPacketExecutorService,
        final StreamConnection streamConnection,
        final CongestionController congestionController,
        final StreamEncryptionUtils streamEncryptionUtils,
        final PaymentTracker paymentTracker,
        final ExchangeRateService exchangeRateService,
        final Link link,
        final SendMoneyRequest sendMoneyRequest
    ) {
      this.perPacketExecutorService = perPacketExecutorService;
      this.streamConnection = Objects.requireNonNull(streamConnection);
      this.congestionController = Objects.requireNonNull(congestionController);
      this.streamEncryptionUtils = Objects.requireNonNull(streamEncryptionUtils);
      this.paymentTracker = Objects.requireNonNull(paymentTracker);
      this.exchangeRateService = exchangeRateService;
      this.link = Objects.requireNonNull(link);

      // Populate the sender address properly from the link if it was not specified by the caller.
      Objects.requireNonNull(sendMoneyRequest);
      this.sendMoneyRequest = SendMoneyRequest.builder().from(sendMoneyRequest)
          .senderAddress(sendMoneyRequest.senderAddress().orElseGet((link.getOperatorAddressSupplier())))
          .build();

      this.shouldSendSourceAddress = new AtomicBoolean(true);
    }

    /**
     * Send money in an individual stream.
     *
     * @return A {@link CompletableFuture} containing a {@link SendMoneyResult}.
     */
    // TODO: Package-private once moved.
    public CompletableFuture<SendMoneyResult> send() {

      // TODO: Ensure this is only callable once at a time (e.g., Semaphor or AtomicBoolean). Calling this twice by the same instantiator will have indeterminate results and should not be done.

      // TODO: Think about this executor
      // A separate executor is needed for the overall call to sendPacket otherwise a livelock can occur.
      // Using a shared executor could cause sendPacket to internally get blocked from submitting tasks
      // because the shared executor is already blocked waiting on the results of the call here to sendPacket
      ExecutorService sendMoneyExecutor = Executors.newSingleThreadExecutor();
      final Instant paymentStart = DateUtils.now();
      // All futures will run here using the Cached Executor service.
      return CompletableFuture
          .supplyAsync(() -> {

            // This state machine handles timeout, throttling, etc. and emits events relating to the ongoing payment.
            final RunLoopStateMachine stateMachine = new RunLoopStateMachine(
                perPacketExecutorService,
                paymentTracker,
                congestionController, sendMoneyRequest,
                this::constructPacketSupplier,
                // This function will be called on every run-loop in order to send another packet if required.
                this::sendPacket
            );

            stateMachine.start(); // Blocks until finished.

            // TODO: Shutdown the statemachine inside of itself!
            //stateMachine.shutdown();

            return SendMoneyResult.builder()
                .sendMoneyRequest(sendMoneyRequest)
                .amountDeliveredToReceiver(paymentTracker.getDeliveredAmountInReceiverUnits())
                .amountSentBySender(paymentTracker.getDeliveredAmountInSenderUnits())
                .amountLeftToSend(paymentTracker.getOriginalAmountLeft())
                .numFulfilledPackets(paymentTracker.getNumFulfilledPackets())
                .numRejectPackets(paymentTracker.getNumRejectedPackets())
                .sendMoneyDuration(Duration.between(paymentStart, DateUtils.now()))
                .successfulPayment(paymentTracker.successful())
                .build();
          }, sendMoneyExecutor)
          .whenComplete(($, error) -> {
            sendMoneyExecutor.shutdown();
            try {
              closeStream();
            } catch (StreamConnectionClosedException e) {
              logger.error("closeStream failed", e);
            }
            if (error != null) {
              logger.error("SendMoney Stream failed: " + error.getMessage(), error);
            }
            if (!$.successfulPayment()) {
              logger.error("Failed to send full amount. sendMoneyRequestId={}", sendMoneyRequest.requestId());
            }
          });
    }

    /**
     * Helper method to send money in packetized operations.
     *
     * @return
     */
    @VisibleForTesting
    // TODO: Create a sendPacket(PrepareAmounts prepareAmounts) and that should be able to be used for the 0-value send.
    PreparePacketComponents constructPacketSupplier() {
      final PrepareAmounts prepareAmounts = this.computePrepareAmounts();

      // Load up the STREAM packet
      final UnsignedLong sequence;
      try {
        sequence = this.streamConnection.nextSequence();
      } catch (StreamConnectionClosedException e) {
        // The Connection is closed, so we can't send anything more on it.
        logger.warn(
            "Unable to send more packets on a closed StreamConnection. streamConnection={} error={}",
            streamConnection, e
        );
        throw new RuntimeException(e);
      }

      // TODO: should_send_source_account? See Rust

      final List<StreamFrame> frames = Lists.newArrayList(
          StreamMoneyFrame.builder()
              // This aggregator supports only a simple stream-id, which is one.
              .streamId(UnsignedLong.ONE)
              .shares(UnsignedLong.ONE)
              .build()
      );

      final StreamPacket streamPacket = StreamPacket.builder()
          .interledgerPacketType(InterledgerPacketType.PREPARE)
          // If the STREAM packet is sent on an ILP Prepare, this represents the minimum the receiver should accept.
          .prepareAmount(prepareAmounts.minimumAmountToAccept())
          .sequence(sequence)
          .frames(frames)
          .build();

      // Create the ILP Prepare packet
      final byte[] streamPacketData = this.streamEncryptionUtils
          .toEncrypted(sendMoneyRequest.sharedSecret(), streamPacket);
      final InterledgerCondition executionCondition
          = generatedFulfillableFulfillment(sendMoneyRequest.sharedSecret(), streamPacketData).getCondition();

      final Supplier<Optional<InterledgerPreparePacket>> preparePacketSupplier = () -> {
        if (paymentTracker.auth(prepareAmounts.amountToSend())) {
          congestionController.prepare(prepareAmounts.amountToSend());
          return Optional.of(
              InterledgerPreparePacket.builder()
                  .destination(sendMoneyRequest.destinationAddress())
                  .amount(prepareAmounts.amountToSend())
                  .executionCondition(executionCondition)
                  .expiresAt(DateUtils.now().plusSeconds(30L))
                  .data(streamPacketData)
                  // Added here for JVM convenience, but only the bytes above are encoded to ASN.1 OER
                  .typedData(streamPacket)
                  .build()
          );
        } else {
          logger.warn("Unable to reserve Stream packet amount via auth. prepareAmounts={}", prepareAmounts);
          return Optional.empty();
        }
      };

      return PreparePacketComponents.builder()
          .preparePacketSupplier(preparePacketSupplier)
          .prepareAmounts(prepareAmounts)
          .build();
    }

    // TODO: Create a sendPacket(PrepareAmounts prepareAmounts) and that should be able to be used for the 0-value send.
    void sendPacket(final PreparePacketComponents preparePacketComponents) {
      Objects.requireNonNull(preparePacketComponents);

      // Auth is assumed to have occurred before this call to send.
      preparePacketComponents.preparePacketSupplier().get()
          .ifPresent(preparePacket -> {
            try {
              final StreamPacket streamPacket = preparePacket.typedData()
                  .map(typedData -> {
                    if (typedData instanceof StreamPacket) {
                      return (StreamPacket) typedData;
                    } else {
                      throw new RuntimeException(
                          String.format(
                              "TypedData in Prepare Packet was not a StreamPacket. class=%s value=%s",
                              typedData.getClass().getName(), typedData.toString())
                      );
                    }
                  })
                  .orElseThrow(() -> new RuntimeException("Typed StreamPacket was missing in Prepare Packet"));

              // Auth is assumed to have ocurred before this call to send.
              link.sendPacket(preparePacket).handle(
                  fulfillPacket -> handleFulfill(preparePacket, streamPacket, fulfillPacket),
                  rejectPacket -> handleReject(preparePacket, streamPacket, rejectPacket, congestionController)
              );
            } catch (Exception e) {
              logger.error("Link send failed. preparePacket={}", preparePacketComponents.prepareAmounts(), e);
              congestionController.onReject(preparePacket.getAmount(), InterledgerRejectPacket.builder()
                  .code(InterledgerErrorCode.F00_BAD_REQUEST)
                  .message(
                      String.format("Link send failed. preparePacket=%s error=%s", preparePacket, e.getMessage())
                  )
                  .build());
              paymentTracker.rollback(preparePacket.getAmount());
            }
          });
    }

    /**
     * <p>Calculates a new {@link PrepareAmounts} by determining a scaled exchange-rate between a source and
     * destination while accounting for slippage.</p>
     *
     * <p>For example, because the sender knows the minimum exchange-rate and maximum slippage that they'll tolerate
     * for a payment, the sender can calculate a minimum packet amount such that the minimum destination amount arrives
     * at the recipient without failing due to rounding (note that the packet can still be rejected by the receiver if
     * the exchange rate is too low). Conveniently, if any intermediary takes more slippage such that the final packet
     * value rounds down to 0, then this packet wouldn't meet the minimum destination amount and the recipient would
     * reject.</p>
     *
     * @return A {@link PrepareAmounts}.
     *
     * @see "https://github.com/interledger-rs/interledger-rs/issues/513"
     */
    @VisibleForTesting
    PrepareAmounts computePrepareAmounts() {

      // TODO: Refactor the FX rate contract to allow an empty rate. E.g., the FX provider for the sender may not have a rate for the advertised
      // Denomination. Likewise, the receiver may not advertise a rate. Both of these need to be accounted for.
      // In rust, the lack of a rate or rate/calculation puts the scaled FX rate to 0, which makes sense because this
      // rate is only used to compute the min amount, and in the absence of an FX rate, the min amount should be 0.
      final BigDecimal scaledExchangeRate = sendMoneyRequest.receiverDenomination()
          .map($ ->
              this.exchangeRateService.getScaledExchangeRate(
                  sendMoneyRequest.senderDenomination(),
                  $,
                  sendMoneyRequest.maxSlippagePercent().orElse(BigDecimal.ZERO))
          )
          .orElse(BigDecimal.ZERO);

      Objects.requireNonNull(scaledExchangeRate);

      // Minimum percentage difference between the sender's scaled rate and the connector's scaled rate (remember:
      // both already include slippage!). For instance, if the sender and connector use exactly the same rate and
      // slippage, the payment will likely always fail: the connector will round the decimal amount down, and the
      // sender will round the decimal amount up when calculating the minimum destination amount. So, there must be
      // some difference between these two scaled rates in order for the payment to succeed. The larger this difference
      // is, the more "wiggle room" we have, and the more "wiggle room" we have, the smaller we can make the minimum
      // source packet amount without it getting rounded down and rejected since it doesn't meet the minimum destination
      // amount. But, if we set this margin of error or minimum percentage difference too low, we'd have to send massive
      // packets, which would be bad.

      // This should probably be much smaller than the slippage we're willing to accept.
      // (Default slippage is 1.5% vs default margin of error is 0.1%)
      // (ceil(dest_amt) - dest_amt) / dest_amt
      final BigDecimal marginOfError = new BigDecimal("0.001");

      // Compute the minimum source amount. If liquidity congestion reduces the packet amount too much,
      // exchange rate rounding errors will prevent any money from getting through.
      // This implementation provides a floor that enables money to get delivered, based on the minimum destination
      // amount.
      // More info: https://github.com/interledger-rs/interledger-rs/issues/513
      // Use min_source_amount = 1 / (scaled_rate * margin_of_error)
      final BigDecimal rateWithMarginOfError;
      if (is(scaledExchangeRate).greaterThan(BigDecimal.ZERO)) {
        rateWithMarginOfError = BigDecimal.ONE.divide(scaledExchangeRate.multiply(marginOfError), RoundingMode.CEILING);
      } else {
        rateWithMarginOfError = BigDecimal.ZERO;
      }

      // TODO: Check precision
      UnsignedLong minSourceAmount = UnsignedLong
          .valueOf(rateWithMarginOfError.setScale(0, RoundingMode.CEILING).longValue());

      ////////////////////////////
      // Compute the source amount, in reverse order of precedence.
      ////////////////////////////

      // (5) Amount left in window for congestion
      UnsignedLong sourceAmount = this.congestionController.getAmountLeftInWindow();

      // (4) Min source amount so rounding errors don't prevent delivery
      sourceAmount = max(sourceAmount, minSourceAmount);

      // (3) Distribute "dust" amount across remaining packets
      // By chance at end of payment, the amount remaining is too small to deliver.
      // Approximate the remaining number of packets.
      // If the final packet amount might be dust (less than or close in value to min packet amount),
      // distribute the final dust amount across the other remaining packets.
      // Note: If the max packet amount < min source amount, the payment may fail due to rates anyways.

      final UnsignedLong remainingAmount = paymentTracker.getOriginalAmountLeft();
      // The estimated total number of packets remaining.
      final UnsignedLong estimatedNumPackets = remainingAmount.dividedBy(sourceAmount);
      final UnsignedLong estimatedFinalAmount = remainingAmount.mod(sourceAmount);

      // Calculate 120% of the minSourceAmount (to make this value a bit bigger than it absolutely should be, just in
      // case)
      BigDecimal tmpVal = ONE_POINT_TWO.multiply(new BigDecimal(minSourceAmount.bigIntegerValue()));
      UnsignedLong generousMinPacketAmount = UnsignedLong.valueOf(tmpVal.setScale(0, RoundingMode.CEILING).longValue());

      final boolean possibleDust = is(estimatedNumPackets).greaterThan(UnsignedLong.ZERO)
          && is(estimatedFinalAmount).greaterThan(UnsignedLong.ZERO)
          && is(estimatedFinalAmount).lessThan(generousMinPacketAmount);
      if (possibleDust) {
        // i.e. ceil(remaining_amount / estimated_num_packets)
        sourceAmount = remainingAmount.plus(estimatedNumPackets.minus(UnsignedLong.ONE).dividedBy(estimatedNumPackets));
      }

      // (2) Max packet amount allowed by nodes in path.
      final UnsignedLong effectiveFinalSourceAmount = sourceAmount;
      sourceAmount = this.congestionController.getMaxPacketAmount()
          .map(maxPacketAmount -> min(effectiveFinalSourceAmount, maxPacketAmount))
          .orElse(sourceAmount);

      // (1) Amount available to send, subtracting fulfilled and in-flight amounts
      sourceAmount = min(sourceAmount, this.paymentTracker.getOriginalAmountLeft());

      // Compute the minimum destination amount using the same rate
      UnsignedLong minDestinationAmount = this.exchangeRateService.convert(sourceAmount, scaledExchangeRate);

      return PrepareAmounts.builder()
          .amountToSend(sourceAmount)
          .minimumAmountToAccept(minDestinationAmount)
          .build();
    }

    // TODO: JavaDoc
    // TODO: Unit TEsts
    @VisibleForTesting
    void handleFulfill(
        final InterledgerPreparePacket originalPreparePacket,
        final StreamPacket originalStreamPacket,
        final InterledgerFulfillPacket fulfillPacket
    ) {
      Objects.requireNonNull(originalPreparePacket);
      Objects.requireNonNull(originalStreamPacket);
      Objects.requireNonNull(fulfillPacket);

      // Do this first before trying to decrypt below because decryption can throw an Exception.
      this.congestionController.onFulfill(originalPreparePacket.getAmount());

      final StreamPacket streamPacket = this.streamEncryptionUtils
          .fromEncrypted(sendMoneyRequest.sharedSecret(), fulfillPacket.getData());

      // Since we decrypted the response, the recipient read the request packet and knows our account. Therefore, no
      // need to send the sourceAddress frame anymore.
      this.shouldSendSourceAddress.set(false);

      if (streamPacket.interledgerPacketType() == InterledgerPacketType.FULFILL) {
        UnsignedLong deliveredAmount = streamPacket.prepareAmount();
        // The delivered amount is in receiver's units, as reported by the receiver. No need to convert it or do
        // anything special with it because it's just a reporting thing.
        paymentTracker.commit(originalPreparePacket.getAmount(), deliveredAmount);
      } else {
        if (logger.isWarnEnabled()) {
          logger.warn("Unable to parse STREAM packet from fulfill data. "
                  + "originalPreparePacket={} originalStreamPacket={} fulfillPacket={}",
              originalPreparePacket, originalStreamPacket, fulfillPacket);
        }
      }

      if (logger.isDebugEnabled()) {
        logger.debug("Prepare packet fulfilled ({} left to send). "
                + "originalPreparePacket={} originalStreamPacket={} fulfillPacket={}",
            paymentTracker.getOriginalAmountLeft(), originalPreparePacket, originalStreamPacket, fulfillPacket
        );
      }
    }

    /**
     * Handle a rejection packet.
     *
     * @param originalPreparePacket The {@link InterledgerPreparePacket} that triggered this rejection.
     * @param originalStreamPacket  The {@link StreamPacket} that was inside of {@code originalPreparePacket}.
     * @param rejectPacket          The {@link InterledgerRejectPacket} received from a peer directly connected via a
     *                              {@link Link}.
     * @param congestionController  The {@link CongestionController} used for this sendMoney operation.
     */
    @VisibleForTesting
    // TODO: JavaDoc
    // TODO: Unit TEsts
    void handleReject(
        final InterledgerPreparePacket originalPreparePacket,
        final StreamPacket originalStreamPacket,
        final InterledgerRejectPacket rejectPacket,
        final CongestionController congestionController
    ) {
      Objects.requireNonNull(originalPreparePacket);
      Objects.requireNonNull(originalStreamPacket);
      Objects.requireNonNull(rejectPacket);
      Objects.requireNonNull(congestionController);

      //final UnsignedLong amountToSend = originalPreparePacket.getAmount();

      paymentTracker.rollback(originalPreparePacket.getAmount());
      congestionController.onReject(originalPreparePacket.getAmount(), rejectPacket);

      if (logger.isWarnEnabled()) {
        logger.warn(
            "Prepare with amount {} was rejected with code: {} ({} left to send). originalPreparePacket={} "
                + "originalStreamPacket={} rejectPacket={}",
            originalPreparePacket.getAmount(),
            rejectPacket.getCode().getCode(),
            paymentTracker.getOriginalAmountLeft(),
            originalPreparePacket,
            originalStreamPacket,
            rejectPacket
        );
      }

      // Log the rejection if it's catastrophic
      if (!paymentTracker.shouldToFailImmediately(rejectPacket.getCode())) {
        logger.error(
            "Encountered an unexpected ILPv4 error that will prevent further prepare packets for this sendMoney operation."
                + "originalPreparePacket={} originalStreamPacket={} rejectPacket={}",
            originalPreparePacket, originalStreamPacket, rejectPacket
        );
      }

    }

    /**
     * Close the current STREAM connection by sending a {@link StreamCloseFrame} to the receiver.
     *
     * @return An {@link UnsignedLong} representing the amount delivered by this individual stream.
     */
    @VisibleForTesting
    void closeStream() throws StreamConnectionClosedException {
      this.sendStreamFramesInZeroValuePacket(Lists.newArrayList(
          StreamCloseFrame.builder()
              .streamId(UnsignedLong.ONE)
              .errorCode(ErrorCodes.NoError)
              .build()
      ));
    }

    // TODO: FIXME per https://github.com/hyperledger/quilt/issues/308. Until this is completed, parallel STREAM send()
    //  operations are not thread-safe. Consider a new instance of everything on each sendMoney? In other words, each
    //  SendMoney requires its own connection information,
    // so as long as two senders don't use the same Connection details, everything should be thread-safe.

    // /**
    //  * Close the current STREAM connection by sending a {@link ConnectionCloseFrame} to the receiver.
    //  *
    //  * @return An {@link UnsignedLong} representing the amount delivered by this individual stream.
    //  */
    // // TODO: Add unit test coverage here per See https://github.com/hyperledger/quilt/issues/308
    // @VisibleForTesting
    //     void closeConnection() throws StreamConnectionClosedException {
    //       this.sendStreamFramesInZeroValuePacket(Lists.newArrayList(
    //           ConnectionCloseFrame.builder()
    //               .errorCode(ErrorCodes.NoError)
    //               .build()
    //       ));
    //     }

    private void sendStreamFramesInZeroValuePacket(final Collection<StreamFrame> streamFrames)
        throws StreamConnectionClosedException {
      Objects.requireNonNull(streamFrames);

      if (streamFrames.size() <= 0) {
        logger.warn("sendStreamFrames called with 0 frames");
        return;
      }

      final StreamPacket streamPacket = StreamPacket.builder()
          .interledgerPacketType(InterledgerPacketType.PREPARE)
          .prepareAmount(UnsignedLong.ZERO)
          .sequence(streamConnection.nextSequence())
          .addAllFrames(streamFrames)
          .build();

      // Create the ILP Prepare packet using an encrypted StreamPacket as the encryptedStreamPacket payload...
      final byte[] encryptedStreamPacket = this.streamEncryptionUtils
          .toEncrypted(sendMoneyRequest.sharedSecret(), streamPacket);
      final InterledgerCondition executionCondition;
      executionCondition = generatedFulfillableFulfillment(sendMoneyRequest.sharedSecret(), encryptedStreamPacket)
          .getCondition();

      final InterledgerPreparePacket preparePacket = InterledgerPreparePacket.builder()
          .destination(sendMoneyRequest.destinationAddress())
          .amount(UnsignedLong.ZERO)
          .executionCondition(executionCondition)
          .expiresAt(DateUtils.now().plusSeconds(30L))
          .data(encryptedStreamPacket)
          .typedData(streamPacket)
          .build();

      link.sendPacket(preparePacket);

      //TODO: FIXME per https://github.com/hyperledger/quilt/issues/308
      // Mark the streamConnection object as closed if the caller supplied a ConnectionCloseFrame
      //       streamFrames.stream()
      //           .filter(streamFrame -> streamFrame.streamFrameType() == StreamFrameType.ConnectionClose)
      //           .findAny()
      //           .ifPresent($ -> {
      //             streamConnection.closeConnection();
      //             logger.info("STREAM Connection closed.");
      //           });

      // Emit a log statement if the caller supplied a StreamCloseFrame
      streamFrames.stream()
          .filter(streamFrame -> streamFrame.streamFrameType() == StreamFrameType.StreamClose)
          .findAny()
          .map($ -> (StreamCloseFrame) $)
          .ifPresent($ -> {
            if (logger.isInfoEnabled()) {
              logger.info("StreamId {} Closed. Delivered: {} ({} packets fulfilled, {} packets rejected)",
                  $.streamId(), paymentTracker.getDeliveredAmountInReceiverUnits(),
                  paymentTracker.getNumFulfilledPackets(), paymentTracker.getNumRejectedPackets()
              );
            }
          });
    }
  }
}
