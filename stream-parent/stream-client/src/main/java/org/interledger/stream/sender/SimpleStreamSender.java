package org.interledger.stream.sender;

import static org.interledger.core.InterledgerErrorCode.F08_AMOUNT_TOO_LARGE;
import static org.interledger.core.InterledgerErrorCode.F99_APPLICATION_ERROR;
import static org.interledger.stream.StreamUtils.generatedFulfillableFulfillment;

import org.interledger.codecs.stream.StreamCodecContextFactory;
import org.interledger.core.DateUtils;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerCondition;
import org.interledger.core.InterledgerErrorCode;
import org.interledger.core.InterledgerErrorCode.ErrorFamily;
import org.interledger.core.InterledgerFulfillPacket;
import org.interledger.core.InterledgerPacketType;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerRejectPacket;
import org.interledger.core.InterledgerResponsePacket;
import org.interledger.core.SharedSecret;
import org.interledger.encoding.asn.framework.CodecContext;
import org.interledger.link.Link;
import org.interledger.stream.Denomination;
import org.interledger.stream.PaymentTracker;
import org.interledger.stream.PrepareAmounts;
import org.interledger.stream.SendMoneyRequest;
import org.interledger.stream.SendMoneyResult;
import org.interledger.stream.StreamConnection;
import org.interledger.stream.StreamConnectionId;
import org.interledger.stream.StreamPacket;
import org.interledger.stream.crypto.JavaxStreamEncryptionService;
import org.interledger.stream.crypto.StreamEncryptionService;
import org.interledger.stream.StreamConnectionClosedException;
import org.interledger.stream.frames.ConnectionAssetDetailsFrame;
import org.interledger.stream.frames.ConnectionNewAddressFrame;
import org.interledger.stream.frames.ErrorCodes;
import org.interledger.stream.frames.StreamCloseFrame;
import org.interledger.stream.frames.StreamFrame;
import org.interledger.stream.frames.StreamFrameType;
import org.interledger.stream.frames.StreamMoneyFrame;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.primitives.UnsignedLong;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.annotation.concurrent.ThreadSafe;

/**
 * <p>A simple implementation of {@link StreamSender} that opens a STREAM connection, sends money, and then closes the
 * connection, yielding a response.</p>
 *
 * <p>Note that this implementation does not currently support sending data, which is defined in the STREAM
 * protocol.</p>
 *
 * <p>Note that, per https://github.com/hyperledger/quilt/issues/242, as of the publication of this client,
 * connectors will reject ILP packets that exceed 32kb. This implementation does not overtly restrict the size of the
 * data field in any particular {@link InterledgerPreparePacket}, for two reasons. First, this implementation never
 * packs a sufficient number of STREAM frames into a single Prepare packet for this 32kb limit to ever be reached;
 * Second, if the ILPv4 RFC ever changes to increase this size limitation, we don't want sender/receiver software to
 * have to be updated across the Interledger.</p>
 *
 * @deprecated Will be removed in a future version. Prefer ILP-Pay functionality instead.
 */
@Deprecated
@ThreadSafe
public class SimpleStreamSender implements StreamSender {

  private final Link link;
  private final Duration sendPacketSleepDuration;
  private final StreamEncryptionService streamEncryptionService;
  private final StreamConnectionManager streamConnectionManager;
  private final ExecutorService executorService;

  /**
   * Required-args Constructor.
   *
   * @param link A {@link Link} that is used to send ILPv4 packets to an immediate peer.
   */
  public SimpleStreamSender(final Link link) {
    this(link, Duration.ofMillis(10L));
  }

  /**
   * Required-args Constructor.
   *
   * @param link                    A {@link Link} that is used to send ILPv4 packets to an immediate peer.
   * @param sendPacketSleepDuration A {@link Duration} representing the amount of time for the soldierOn thread to sleep
   *                                before attempting more processing.
   */
  public SimpleStreamSender(final Link link, final Duration sendPacketSleepDuration) {
    this(link, sendPacketSleepDuration, new JavaxStreamEncryptionService());
  }

  /**
   * Required-args Constructor.
   *
   * @param link                    A {@link Link} that is used to send ILPv4 packets to an immediate peer.
   * @param sendPacketSleepDuration A {@link Duration} representing the amount of time for the soldierOn thread to sleep
   *                                before attempting more processing.
   * @param streamEncryptionService An instance of {@link StreamEncryptionService} used to encrypt and decrypted
   *                                end-to-end STREAM packet data (i.e., packets that should only be visible between
   *                                sender and receiver).
   */
  public SimpleStreamSender(
    final Link link, final Duration sendPacketSleepDuration, final StreamEncryptionService streamEncryptionService
  ) {
    this(link, sendPacketSleepDuration, streamEncryptionService, new StreamConnectionManager());
  }

  /**
   * Required-args Constructor.
   *
   * @param streamEncryptionService An instance of {@link StreamEncryptionService} used to encrypt and decrypted
   *                                end-to-end STREAM packet data (i.e., packets that should only be visible between
   *                                sender and receiver).
   * @param link                    A {@link Link} that is used to send ILPv4 packets to an immediate peer.
   * @param sendPacketSleepDuration A {@link Duration} representing the amount of time for the soldierOn thread to sleep
   *                                before attempting more processing.
   */
  public SimpleStreamSender(
    final Link link, final Duration sendPacketSleepDuration, final StreamEncryptionService streamEncryptionService,
    final StreamConnectionManager streamConnectionManager
  ) {
    this(link, sendPacketSleepDuration, streamEncryptionService, streamConnectionManager, newDefaultExecutor());
  }

  /**
   * Required-args Constructor.
   *
   * @param link                    A {@link Link} that is used to send ILPv4 packets to an immediate peer.
   * @param sendPacketSleepDuration A {@link Duration} representing the amount of time for the soldierOn thread to sleep
   *                                before attempting more processing.
   * @param streamEncryptionService A {@link StreamEncryptionService} used to encrypt and decrypted end-to-end STREAM
   *                                packet data (i.e., packets that should only be visible between sender and
   *                                receiver).
   * @param streamConnectionManager A {@link StreamConnectionManager} that manages connections for all senders and
   * @param executorService         A {@link ExecutorService} to run the payments.
   */
  public SimpleStreamSender(
    final Link link,
    final Duration sendPacketSleepDuration,
    final StreamEncryptionService streamEncryptionService,
    final StreamConnectionManager streamConnectionManager,
    final ExecutorService executorService
  ) {
    this.link = Objects.requireNonNull(link);
    this.sendPacketSleepDuration = Objects.requireNonNull(sendPacketSleepDuration);
    this.streamEncryptionService = Objects.requireNonNull(streamEncryptionService);
    this.streamConnectionManager = Objects.requireNonNull(streamConnectionManager);

    // Note that pools with similar properties but different details (for example, timeout parameters) may be
    // created using {@link ThreadPoolExecutor} constructors.
    this.executorService = Objects.requireNonNull(executorService);
  }

  private static ExecutorService newDefaultExecutor() {
    ThreadFactory factory = new ThreadFactoryBuilder()
      .setDaemon(true)
      .setNameFormat("simple-stream-sender-%d")
      .build();
    return Executors.newFixedThreadPool(30, factory);
  }

  @Override
  public CompletableFuture<SendMoneyResult> sendMoney(final SendMoneyRequest request) {
    Objects.requireNonNull(request);

    final StreamConnection streamConnection = this.streamConnectionManager.openConnection(
      StreamConnectionId.from(request.destinationAddress(), request.sharedSecret())
    );

    return new SendMoneyAggregator(
      this.executorService,
      streamConnection,
      StreamCodecContextFactory.oer(),
      this.link,
      new AimdCongestionController(),
      this.streamEncryptionService,
      this.sendPacketSleepDuration,
      request
    ).send();
  }

  /**
   * Encapsulates everything needed to send a particular amount of money by breaking up a payment into a bunch of
   * smaller packets, and then handling all responses. This aggregator operates on a single Connection by opening and
   * closing a single stream.
   */
  static class SendMoneyAggregator {

    // These error codes, despite beging FINAL, should be treated as "recoverable", meaning the Stream sendMoney
    // operation should not immediately fail if one of these is encountered.
    private static final Set<InterledgerErrorCode> NON_TERMINAL_ERROR_CODES = ImmutableSet.of(
      F08_AMOUNT_TOO_LARGE, F99_APPLICATION_ERROR
    );

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final ExecutorService executorService;
    private final StreamConnection streamConnection;
    private final CodecContext streamCodecContext;
    private final StreamEncryptionService streamEncryptionService;
    private final CongestionController congestionController;
    private final Link link;
    private final SharedSecret sharedSecret;
    private final Optional<Duration> timeout;
    private final Optional<InterledgerAddress> senderAddress;
    private final Denomination senderDenomination;
    private final InterledgerAddress destinationAddress;
    private final AtomicBoolean shouldSendSourceAddress;
    private final AtomicInteger numFulfilledPackets;
    private final AtomicInteger numRejectedPackets;
    private final PaymentTracker paymentTracker;
    private final AtomicBoolean unrecoverableErrorEncountered;
    private final SendMoneyRequest sendMoneyRequest;
    private Optional<Denomination> receiverDenomination;
    private Duration sendPacketSleepDuration;

    /**
     * Required-args Constructor.
     *
     * @param executorService         An {@link ExecutorService} for sending multiple STREAM frames in parallel.
     * @param streamConnection        A {@link StreamConnection} that can be used to send packets with.
     * @param streamCodecContext      A {@link CodecContext} that can encode and decode ASN.1 OER Stream packets and
     *                                frames.
     * @param link                    The {@link Link} used to send ILPv4 packets containing Stream packets.
     * @param congestionController    A {@link CongestionController} that supports back-pressure for money streams.
     * @param streamEncryptionService A {@link StreamEncryptionService} that allows for Stream packet encryption and
     *                                decryption.
     * @param sendMoneyRequest        A {@link SendMoneyRequest} that contains all relevant details about the money to
     */
    SendMoneyAggregator(
      final ExecutorService executorService,
      final StreamConnection streamConnection,
      final CodecContext streamCodecContext,
      final Link link,
      final CongestionController congestionController,
      final StreamEncryptionService streamEncryptionService,
      final Duration sendPacketSleepDuration,
      final SendMoneyRequest sendMoneyRequest
    ) {
      this.executorService = Objects.requireNonNull(executorService);
      this.streamConnection = Objects.requireNonNull(streamConnection);

      this.streamCodecContext = Objects.requireNonNull(streamCodecContext);
      this.link = Objects.requireNonNull(link);
      this.streamEncryptionService = Objects.requireNonNull(streamEncryptionService);
      this.congestionController = Objects.requireNonNull(congestionController);
      this.shouldSendSourceAddress = new AtomicBoolean(true);
      this.unrecoverableErrorEncountered = new AtomicBoolean(false);

      this.sharedSecret = sendMoneyRequest.sharedSecret();
      this.senderAddress = sendMoneyRequest.sourceAddress();
      this.destinationAddress = sendMoneyRequest.destinationAddress();
      this.sendPacketSleepDuration = Objects.requireNonNull(sendPacketSleepDuration);

      this.numFulfilledPackets = new AtomicInteger(0);
      this.numRejectedPackets = new AtomicInteger(0);

      this.sendMoneyRequest = Objects.requireNonNull(sendMoneyRequest);
      this.timeout = sendMoneyRequest.timeout();
      this.senderDenomination = sendMoneyRequest.denomination();
      this.paymentTracker = sendMoneyRequest.paymentTracker();
      this.receiverDenomination = Optional.empty();
    }

    /**
     * Send money in an individual stream.
     *
     * @return A {@link CompletableFuture} containing a {@link SendMoneyResult}.
     */
    CompletableFuture<SendMoneyResult> send() {
      Objects.requireNonNull(sharedSecret);
      Objects.requireNonNull(destinationAddress);

      Instant startPreflight = DateUtils.now();
      try {
        receiverDenomination = preflightCheck();
        if (paymentTracker.requiresReceiverDenomination() && !receiverDenomination.isPresent()) {
          // The PaymentTrack requires a receiver denomination, but the receiver didn't send one. Thus, we must abort.
          return CompletableFuture.completedFuture(this.constructSendMoneyResultForInvalidPreflight(startPreflight));
        }
      } catch (Exception e) {
        if (paymentTracker.requiresReceiverDenomination()) {
          logger.error("Preflight check failed. sendMoneyRequest={}", sendMoneyRequest, e);
          return CompletableFuture.completedFuture(this.constructSendMoneyResultForInvalidPreflight(startPreflight));
        } else {
          logger.warn(
            "Preflight check failed, but was not crucial for this sendMoney operation. sendMoneyRequest={}",
            sendMoneyRequest, e
          );
        }
      }

      // A separate executor is needed for overall call to sendMoneyPacketized otherwise a livelock can occur.
      // Using a shared executor could cause sendMoneyPacketized to internally get blocked from submitting tasks
      // because the shared executor is already blocked waiting on the results of the call here to sendMoneyPacketized
      ExecutorService sendMoneyExecutor = Executors.newSingleThreadExecutor();
      final Instant start = DateUtils.now();
      // All futures will run here using the Cached Executor service.
      return CompletableFuture
        .supplyAsync(() -> {

          // Do all the work of sending packetized money for this Stream/sendMoney request.
          this.sendMoneyPacketized();
          return SendMoneyResult.builder()
            .senderAddress(this.computeSenderAddressForReportingPurposes())
            .senderDenomination(senderDenomination)
            .destinationAddress(destinationAddress)
            .destinationDenomination(receiverDenomination)
            .amountDelivered(paymentTracker.getDeliveredAmountInReceiverUnits())
            .amountSent(paymentTracker.getDeliveredAmountInSenderUnits())
            .amountLeftToSend(paymentTracker.getOriginalAmountLeft())
            .originalAmount(paymentTracker.getOriginalAmount())
            .numFulfilledPackets(numFulfilledPackets.get())
            .numRejectPackets(numRejectedPackets.get())
            .sendMoneyDuration(Duration.between(start, DateUtils.now()))
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
     * Helper method to construct a {@link SendMoneyResult} that can be used when preflight checks are not successful.
     *
     * @param startPreflight An {@link Instant} representing the moment in time that preflight was started.
     *
     * @return A {@link SendMoneyResult}.
     */
    private SendMoneyResult constructSendMoneyResultForInvalidPreflight(final Instant startPreflight) {
      Objects.requireNonNull(startPreflight);
      return SendMoneyResult.builder()
        .senderAddress(this.computeSenderAddressForReportingPurposes())
        .senderDenomination(senderDenomination)
        .destinationAddress(destinationAddress)
        .destinationDenomination(receiverDenomination)
        .sendMoneyDuration(Duration.between(startPreflight, DateUtils.now()))
        .numRejectPackets(0)
        .numFulfilledPackets(0)
        .amountDelivered(UnsignedLong.ZERO)
        .amountSent(UnsignedLong.ZERO)
        .originalAmount(paymentTracker.getOriginalAmount())
        .amountLeftToSend(paymentTracker.getOriginalAmountLeft())
        .successfulPayment(paymentTracker.successful())
        .build();
    }

    /**
     * Helper method to centralize the computation of this sener's {@link InterledgerAddress}. Basically, if {@link
     * #senderAddress} is empty, then the implementation will report back the link address. However, during STREAM
     * operation with a receiver, no address will be reported (e.g., such as inside of a `ConnectionNewAddress` frame).
     *
     * @return The {@link InterledgerAddress} of this sender, for usage by the creator of this stream sender.
     */
    private InterledgerAddress computeSenderAddressForReportingPurposes() {
      return this.senderAddress.orElseGet((link.getOperatorAddressSupplier()));
    }

    /**
     * <p>Send a zero-value Prepare packet to "pre-flight" the Connection before actual value is transferred.</p>
     *
     * <p>This operation is used to initialize a new Stream connection in order to fulfill any prerequisites necessary
     * before sending real value. For example, it is necessary to obtain the receiver's "Connection Asset Details"
     * before a sender can send value, in order to manage slippage for the sender.</p>
     *
     * <p>Likewise, it may be desirable to perform other preflight checks in the future, such as checking an exchange
     * rate or some other type of check.</p>
     * <p>
     * TODO: See https://github.com/hyperledger/quilt/issues/308 to determine when the Stream and/or Connection should
     * be closed.
     *
     * @return A {@link Denomination} that contains the asset information for the receiver.
     *
     * @throws StreamConnectionClosedException if the denomination could not be loaded and the Stream should be closed.
     */
    @VisibleForTesting
    Optional<Denomination> preflightCheck() throws StreamConnectionClosedException {
      // Load up the STREAM packet
      final UnsignedLong sequence;
      try {
        sequence = this.streamConnection.nextSequence();
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
          .sourceAddress(senderAddress)
          .build(),
        ConnectionAssetDetailsFrame.builder()
          .sourceDenomination(senderDenomination)
          .build()
      );

      final StreamPacket streamPacket = StreamPacket.builder()
        .interledgerPacketType(InterledgerPacketType.PREPARE)
        .prepareAmount(UnsignedLong.ZERO)
        .sequence(sequence)
        .frames(frames)
        .build();

      // Create the ILP Prepare packet
      final byte[] streamPacketData = this.toEncrypted(sharedSecret, streamPacket);
      final InterledgerCondition executionCondition;
      executionCondition = generatedFulfillableFulfillment(sharedSecret, streamPacketData).getCondition();

      final InterledgerPreparePacket preparePacket = InterledgerPreparePacket.builder()
        .destination(destinationAddress)
        .amount(UnsignedLong.ZERO)
        .executionCondition(executionCondition)
        .expiresAt(DateUtils.now().plusSeconds(30L))
        .data(streamPacketData)
        .typedData(streamPacket)
        .build();

      // The function that parses out the STREAM packets...
      final Function<InterledgerResponsePacket, Optional<Denomination>> readDetailsFromStream = (responsePacket) -> {
        final StreamPacket packet = this.fromEncrypted(sharedSecret, responsePacket.getData());
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
        .handleAndReturn(
          fulfillPacket -> {
          }, // Do nothing on fulfill
          this::checkForAndTriggerUnrecoverableError // check for unrecoverable error.
        )
        // We typically expect this Prepare operation to reject, but regardless of whether the response is a fulfill
        // or a reject, try to read the Receiver's Connection Asset Details and return them.
        .map(readDetailsFromStream::apply, readDetailsFromStream::apply);
    }

    /**
     * Helper method to send money in a packetized operation.
     */
    private void sendMoneyPacketized() {

      final AtomicBoolean timeoutReached = new AtomicBoolean(false);

      final ScheduledExecutorService timeoutMonitor = Executors.newSingleThreadScheduledExecutor();

      boolean tryingToSendTooMuch = false;

      timeout.ifPresent($ -> timeoutMonitor.schedule(
        () -> {
          timeoutReached.set(true);
          timeoutMonitor.shutdown();
        },
        $.toMillis(), TimeUnit.MILLISECONDS
      ));

      while (soldierOn(timeoutReached.get(), tryingToSendTooMuch)) {
        // Determine the amount to send
        final PrepareAmounts amounts = receiverDenomination
          .map(receiverDenomination -> paymentTracker.getSendPacketAmounts(
            congestionController.getMaxAmount(), senderDenomination, receiverDenomination
          ))
          .orElseGet(() -> paymentTracker.getSendPacketAmounts(
            congestionController.getMaxAmount(), senderDenomination
          ));

        UnsignedLong amountToSend = amounts.getAmountToSend();
        UnsignedLong receiverMinimum = amounts.getMinimumAmountToAccept();

        if (amountToSend.equals(UnsignedLong.ZERO) || timeoutReached.get() || unrecoverableErrorEncountered.get()) {
          try {
            // Don't send any more, but wait a bit for outstanding requests to complete so we don't cycle needlessly in
            // a while loop that doesn't do anything useful.
            Thread.sleep(sendPacketSleepDuration.toMillis());
          } catch (InterruptedException e) {
            throw new StreamSenderException(e.getMessage(), e);
          }
          continue;
        }

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
          continue;
        }

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
          .prepareAmount(receiverMinimum)
          .sequence(sequence)
          .frames(frames)
          .build();

        // Create the ILP Prepare packet
        final byte[] streamPacketData = this.toEncrypted(sharedSecret, streamPacket);
        final InterledgerCondition executionCondition
          = generatedFulfillableFulfillment(sharedSecret, streamPacketData).getCondition();

        final Supplier<InterledgerPreparePacket> preparePacket = () -> InterledgerPreparePacket.builder()
          .destination(destinationAddress)
          .amount(amountToSend)
          .executionCondition(executionCondition)
          .expiresAt(DateUtils.now().plusSeconds(30L))
          .data(streamPacketData)
          // Added here for JVM convenience, but only the bytes above are encoded to ASN.1 OER
          .typedData(streamPacket)
          .build();

        // auth
        // capture
        // rollback

        final PrepareAmounts prepareAmounts = PrepareAmounts.from(preparePacket.get(), streamPacket);
        if (!paymentTracker.auth(prepareAmounts)) {
          // if we can't auth, just skip this iteration of the loop until everything else completes
          tryingToSendTooMuch = true;
          continue;
        }

        try {
          // don't submit new tasks if the timeout was reached within this iteration of the while loop
          if (!timeoutReached.get()) {
            // call this before spinning off a task. failing to do so can create a condition in the
            // soliderOn check where we will incorrectly evaluate hasInFlight on the congestion
            // controller to not reflect what we've actually scheduled to run, resulting in the loop
            // breaking prematurely
            congestionController.prepare(amountToSend);
            schedule(timeoutReached, preparePacket, streamPacket, prepareAmounts);
          } else {
            logger.error("SoldierOn runLoop had more tasks to schedule but was timed-out");
          }
        } catch (Exception e) {
          // Retry this amount on the next run...
          paymentTracker.rollback(prepareAmounts, false);
          logger.error("Submit failed", e);
        }
      }
      timeoutMonitor.shutdownNow();
    }

    /**
     * Schedules a {@link Callable} with {@link this#executorService} to actually send an {@link
     * InterledgerPreparePacket} on an existing Stream connection.
     *
     * @param timeoutReached        An {@link AtomicBoolean} that can be set to indicate whether a timeout has been
     *                              reached.
     * @param preparePacketSupplier A {@link Supplier} of the {@link InterledgerPreparePacket} that will be used by this
     *                              method. This is a supplier in order to facilitate late-bound construction of the
     *                              packet in order to ensure the when a packet is constructed, its expiry is
     *                              initialized very close to when the packet will actually be sent on a link. Without
     *                              this supplier, packets were getting created and then handing around in the executor.
     *                              Under extreme-load conditions, these packets would expire before ever getting sent
     *                              out over the wire.
     * @param streamPacket          A decoded {@link StreamPacket} containing all STREAM information that is inside of
     *                              the Prepare packet passed into this method. This is provided for debugging and minor
     *                              optimization improvements inside of nested methods called by this method.
     * @param prepareAmounts        A {@link PrepareAmounts} for augmenting the sending of the prepare packet.
     */
    @VisibleForTesting
    void schedule(
      final AtomicBoolean timeoutReached,
      final Supplier<InterledgerPreparePacket> preparePacketSupplier,
      final StreamPacket streamPacket,
      final PrepareAmounts prepareAmounts
    ) {
      Objects.requireNonNull(timeoutReached);
      Objects.requireNonNull(preparePacketSupplier);
      Objects.requireNonNull(streamPacket);
      Objects.requireNonNull(prepareAmounts);

      try {
        executorService.submit(() -> {
          InterledgerPreparePacket preparePacket = preparePacketSupplier.get();
          if (!timeoutReached.get()) {
            try {
              link.sendPacket(preparePacket).handle(
                fulfillPacket -> handleFulfill(preparePacket, streamPacket, fulfillPacket, prepareAmounts),
                rejectPacket -> handleReject(
                  preparePacket, streamPacket, rejectPacket,
                  prepareAmounts, numRejectedPackets, congestionController
                )
              );
            } catch (Exception e) {
              logger.error("Link send failed. preparePacket={}", preparePacket, e);
              congestionController.onReject(preparePacket.getAmount(), InterledgerRejectPacket.builder()
                .code(InterledgerErrorCode.F00_BAD_REQUEST)
                .message(
                  String.format("Link send failed. preparePacket=%s error=%s", preparePacket, e.getMessage())
                )
                .build());
              paymentTracker.rollback(prepareAmounts, false);
            }
          } else {
            logger.info("timeout reached, not sending packet");
            congestionController.onReject(preparePacket.getAmount(), InterledgerRejectPacket.builder()
              .code(InterledgerErrorCode.R00_TRANSFER_TIMED_OUT)
              .message(String.format("Timeout reached before packet could be sent. preparePacket=%s", preparePacket))
              .build());
          }
        });
      } catch (RejectedExecutionException e) {
        // If we get here, it means the task was unable to be scheduled, so we need to unwind the congestion
        // controller to prevent deadlock.
        congestionController.onReject(preparePacketSupplier.get().getAmount(), InterledgerRejectPacket.builder()
          .code(InterledgerErrorCode.F00_BAD_REQUEST)
          .message(
            String
              .format("Unable to schedule sendMoney task. preparePacket=%s error=%s", preparePacketSupplier.get(),
                e.getMessage())
          )
          .build());
        throw e;
      }
    }

    @VisibleForTesting
    boolean soldierOn(final boolean timeoutReached, final boolean tryingToSendTooMuch) {
      // if money in flight, always soldier on
      // otherwise, soldier on if
      //   the connection is not closed
      //   and you haven't delivered the full amount
      //   and you haven't timed out
      //   and you're not trying to send too much
      //   and we haven't hit an unrecoverable error
      return this.congestionController.hasInFlight()
        || (!streamConnection.isClosed()
        && paymentTracker.moreToSend()
        && !timeoutReached
        && !tryingToSendTooMuch
        && !unrecoverableErrorEncountered.get());
    }

    /**
     * Convert a {@link StreamPacket} to bytes using the CodecContext and then encrypt it using the supplied {@code
     * sharedSecret}.
     *
     * @param sharedSecret The shared secret known only to this client and the remote STREAM receiver, used to encrypt
     *                     and decrypt STREAM frames and packets sent and received inside of ILPv4 packets sent over the
     *                     Interledger between these two entities (i.e., sender and receiver).
     * @param streamPacket A {@link StreamPacket} to encode into ASN.1 OER and then encrypt into a byte array.
     *
     * @return A byte-array containing the encrypted version of an ASN.1 OER encoded {@link StreamPacket}.
     */
    @VisibleForTesting
    byte[] toEncrypted(final SharedSecret sharedSecret, final StreamPacket streamPacket) {
      Objects.requireNonNull(sharedSecret);
      Objects.requireNonNull(streamPacket);

      try {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        streamCodecContext.write(streamPacket, baos);
        final byte[] streamPacketBytes = baos.toByteArray();
        return streamEncryptionService.encrypt(sharedSecret, streamPacketBytes);
      } catch (IOException e) {
        throw new StreamSenderException(e.getMessage(), e);
      }
    }

    /**
     * Convert the encrypted bytes of a stream packet into a {@link StreamPacket} using the CodecContext and {@code
     * sharedSecret}.
     *
     * @param sharedSecret               The shared secret known only to this client and the remote STREAM receiver,
     *                                   used to encrypt and decrypt STREAM frames and packets sent and received inside
     *                                   of ILPv4 packets sent over the Interledger between these two entities (i.e.,
     *                                   sender and receiver).
     * @param encryptedStreamPacketBytes A byte-array containing an encrypted ASN.1 OER encoded {@link StreamPacket}.
     *
     * @return The decrypted {@link StreamPacket}.
     */
    @VisibleForTesting
    StreamPacket fromEncrypted(final SharedSecret sharedSecret, final byte[] encryptedStreamPacketBytes) {
      Objects.requireNonNull(sharedSecret);
      Objects.requireNonNull(encryptedStreamPacketBytes);

      final byte[] streamPacketBytes = this.streamEncryptionService.decrypt(sharedSecret, encryptedStreamPacketBytes);
      try {
        return streamCodecContext.read(StreamPacket.class, new ByteArrayInputStream(streamPacketBytes));
      } catch (IOException e) {
        throw new StreamSenderException(e.getMessage(), e);
      }
    }

    @VisibleForTesting
    void handleFulfill(
      final InterledgerPreparePacket originalPreparePacket,
      final StreamPacket originalStreamPacket,
      final InterledgerFulfillPacket fulfillPacket,
      final PrepareAmounts prepareAmounts
    ) {
      Objects.requireNonNull(originalPreparePacket);
      Objects.requireNonNull(originalStreamPacket);
      Objects.requireNonNull(fulfillPacket);
      Objects.requireNonNull(prepareAmounts);

      this.numFulfilledPackets.getAndIncrement();
      this.congestionController.onFulfill(originalPreparePacket.getAmount());
      this.shouldSendSourceAddress.set(false);

      final StreamPacket streamPacket = this.fromEncrypted(sharedSecret, fulfillPacket.getData());

      //if let Ok (packet) = StreamPacket::from_encrypted ( & self.shared_secret, fulfill.into_data()){
      if (streamPacket.interledgerPacketType() == InterledgerPacketType.FULFILL) {
        UnsignedLong deliveredAmount = streamPacket.prepareAmount();
        paymentTracker.commit(prepareAmounts, deliveredAmount);
      } else {
        logger.warn("Unable to parse STREAM packet from fulfill data. "
            + "originalPreparePacket={} originalStreamPacket={} fulfillPacket={}",
          originalPreparePacket, originalStreamPacket, fulfillPacket);
      }

      logger.debug("Prepare packet fulfilled ({} left to send). "
          + "originalPreparePacket={} originalStreamPacket={} fulfillPacket={}",
        paymentTracker.getOriginalAmountLeft(), originalPreparePacket, originalStreamPacket, fulfillPacket
      );
    }

    /**
     * Handle a rejection packet.
     *
     * @param originalPreparePacket The {@link InterledgerPreparePacket} that triggered this rejection.
     * @param originalStreamPacket  The {@link StreamPacket} that was inside of {@code originalPreparePacket}.
     * @param rejectPacket          The {@link InterledgerRejectPacket} received from a peer directly connected via a
     *                              {@link Link}.
     * @param numRejectedPackets    An {@link AtomicInteger} that holds the total number of packets rejected thus far on
     *                              this sendMoney operation.
     * @param congestionController  The {@link CongestionController} used for this sendMoney operation.
     */
    @VisibleForTesting
    void handleReject(
      final InterledgerPreparePacket originalPreparePacket,
      final StreamPacket originalStreamPacket,
      final InterledgerRejectPacket rejectPacket,
      final PrepareAmounts prepareAmounts,
      final AtomicInteger numRejectedPackets,
      final CongestionController congestionController
    ) {
      Objects.requireNonNull(originalPreparePacket);
      Objects.requireNonNull(originalStreamPacket);
      Objects.requireNonNull(rejectPacket);
      Objects.requireNonNull(numRejectedPackets);
      Objects.requireNonNull(congestionController);
      Objects.requireNonNull(prepareAmounts);

      final UnsignedLong amountToSend = originalPreparePacket.getAmount();

      numRejectedPackets.getAndIncrement();

      paymentTracker.rollback(prepareAmounts, true);
      congestionController.onReject(amountToSend, rejectPacket);

      if (logger.isDebugEnabled()) {
        logger.debug(
          "Prepare with amount {} was rejected with code: {} ({} left to send). originalPreparePacket={} "
            + "originalStreamPacket={} rejectPacket={}",
          amountToSend,
          rejectPacket.getCode().getCode(),
          paymentTracker.getOriginalAmountLeft(),
          originalPreparePacket,
          originalStreamPacket,
          rejectPacket
        );
      }

      this.checkForAndTriggerUnrecoverableError(rejectPacket);

      ////////////
      // Log the rejection
      ////////////

      if (ErrorFamily.FINAL.equals(rejectPacket.getCode().getErrorFamily())) {
        // Most Final errors trigger immediate stoppage of send operations, except for F08 and F99.
        if (NON_TERMINAL_ERROR_CODES.contains(rejectPacket.getCode())) {
          // error was a tolerable F rejection (currently F08 or F99)
          logger.debug(
            "Encountered an expected ILPv4 FINAL error. Retrying... "
              + "originalPreparePacket={} originalStreamPacket={} rejectPacket={}",
            originalPreparePacket, originalStreamPacket, rejectPacket);
        } else {
          logger.error(
            "Encountered an unexpected ILPv4 FINAL error. Aborting this sendMoney. "
              + "originalPreparePacket={} originalStreamPacket={} rejectPacket={}",
            originalPreparePacket, originalStreamPacket, rejectPacket
          );
        }
      } else if (ErrorFamily.RELATIVE.equals(rejectPacket.getCode().getErrorFamily())) {
        // All Relative errors trigger immediate stoppage of send operations.
        logger.warn(
          "Relative ILPv4 transport outage. originalPreparePacket={} originalStreamPacket={} rejectPacket={}",
          originalPreparePacket, originalStreamPacket, rejectPacket
        );
      } else { // if (ErrorFamily.TEMPORARY.equals(rejectPacket.getCode().getErrorFamily())) {
        logger.warn(
          "Temporary ILPv4 transport outage. originalPreparePacket={} originalStreamPacket={} rejectPacket={}",
          originalPreparePacket, originalStreamPacket, rejectPacket
        );
      }
    }

    /**
     * <p>A helper method to centralize all logic around when to trigger an "unrecoverable error" that will stop this
     * sender from schedule further packets.</p>
     *
     * <p>This method functions according to the following rules:</p>
     *
     * <p>Interledger Reject packets with the following attributes will immediately trigger an UNRECOVERABLE_ERROR
     * condition, stopping the sendMoney operation:</p>
     * <ol>
     *   <li>Any F-family rejections (except F08 and F99, which are used for congestion control and rate-probing).</li>
     *   <li>Any R-family rejections.</li>
     * </ol>
     *
     * <p>Interledger Reject packets with the following attributes MAY trigger an UNRECOVERABLE_ERROR condition after
     * some threshold (this logic is not yet implemented, and may never be):</p>
     * <ol>
     *  <li>T00 Errors (generally should be accepted up to some threshold)</li>
     *  <li>T01 Errors (generally should be accepted up to some threshold)</li>
     *  <li>F99 Errors (generally should be accepted up to some threshold; used for FX problems where prepare amount is
     *  less than min amount the receiver should receive).</li>
     * </ol>
     *
     * <p>Interledger Reject packets with the following attributes will NEVER trigger an UNRECOVERABLE_ERROR condition:</p>
     * <ol>
     *  <li>T02-T99 Errors (these may resolve with time)</li>
     *  <li>F08 Errors (used for congestion control)</li>
     * </ol>
     *
     * @param rejectPacket An {@link InterledgerRejectPacket} that was received from an outgoing {@link Link} in
     *                     response to a prepare packet.
     */
    @VisibleForTesting
    void checkForAndTriggerUnrecoverableError(final InterledgerRejectPacket rejectPacket) {
      Objects.requireNonNull(rejectPacket);

      if (ErrorFamily.FINAL.equals(rejectPacket.getCode().getErrorFamily())) {
        // Most Final errors trigger immediate stoppage of send operations, except for F08 and F99.
        if (NON_TERMINAL_ERROR_CODES.contains(rejectPacket.getCode())) {
          // error was a tolerable F rejection (currently F08 or F99)
        } else {
          unrecoverableErrorEncountered.set(true);
        }
      } else if (ErrorFamily.RELATIVE.equals(rejectPacket.getCode().getErrorFamily())) {
        // All Relative errors trigger immediate stoppage of send operations.
        unrecoverableErrorEncountered.set(true);
      } else { // if (ErrorFamily.TEMPORARY.equals(rejectPacket.getCode().getErrorFamily())) {
        // do nothing.
      }
    }

    @VisibleForTesting
    protected boolean isUnrecoverableErrorEncountered() {
      return this.unrecoverableErrorEncountered.get();
    }

    @VisibleForTesting
    void setUnrecoverableErrorEncountered(boolean unrecoverableErrorEncountered) {
      this.unrecoverableErrorEncountered.set(unrecoverableErrorEncountered);
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
      final byte[] encryptedStreamPacket = this.toEncrypted(sharedSecret, streamPacket);
      final InterledgerCondition executionCondition;
      executionCondition = generatedFulfillableFulfillment(sharedSecret, encryptedStreamPacket).getCondition();

      final InterledgerPreparePacket preparePacket = InterledgerPreparePacket.builder()
        .destination(destinationAddress)
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

      // Emit a log statement if the called supplied a StreamCloseFrame
      streamFrames.stream()
        .filter(streamFrame -> streamFrame.streamFrameType() == StreamFrameType.StreamClose)
        .findAny()
        .map($ -> (StreamCloseFrame) $)
        .ifPresent($ -> {
          logger.info(
            "StreamId {} Closed. Delivered: {} ({} packets fulfilled, {} packets rejected)",
            $.streamId(), paymentTracker.getDeliveredAmountInReceiverUnits(), this.numFulfilledPackets.get(),
            this.numRejectedPackets.get()
          );
        });
    }
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", SimpleStreamSender.class.getSimpleName() + "[", "]")
      .add("link=" + link)
      .add("sendPacketSleepDuration=" + sendPacketSleepDuration)
      .add("streamEncryptionService=" + streamEncryptionService)
      .add("streamConnectionManager=" + streamConnectionManager)
      .toString();
  }
}
