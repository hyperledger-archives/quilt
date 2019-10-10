package org.interledger.stream.sender;

import static org.interledger.core.InterledgerErrorCode.F08_AMOUNT_TOO_LARGE_CODE;
import static org.interledger.core.InterledgerErrorCode.T04_INSUFFICIENT_LIQUIDITY_CODE;
import static org.interledger.stream.StreamUtils.generatedFulfillableFulfillment;

import org.interledger.codecs.stream.StreamCodecContextFactory;
import org.interledger.core.Immutable;
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
import org.interledger.stream.StreamConnectionClosedException;
import org.interledger.stream.StreamConnectionId;
import org.interledger.stream.StreamPacket;
import org.interledger.stream.crypto.JavaxStreamEncryptionService;
import org.interledger.stream.crypto.StreamEncryptionService;
import org.interledger.stream.frames.ConnectionAssetDetailsFrame;
import org.interledger.stream.frames.ConnectionCloseFrame;
import org.interledger.stream.frames.ConnectionNewAddressFrame;
import org.interledger.stream.frames.StreamCloseFrame;
import org.interledger.stream.frames.StreamFrame;
import org.interledger.stream.frames.StreamFrameType;
import org.interledger.stream.frames.StreamMoneyFrame;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.google.common.primitives.UnsignedLong;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.immutables.value.Value.Derived;
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
 */
@ThreadSafe
public class SimpleStreamSender implements StreamSender {

  private final Link link;
  private final StreamEncryptionService streamEncryptionService;
  private final ExecutorService executorService;
  private final StreamConnectionManager streamConnectionManager;

  /**
   * Required-args Constructor.
   *
   * @param link A {@link Link} that is used to send ILPv4 packets to an immediate peer.
   */
  public SimpleStreamSender(final Link link) {
    this(new JavaxStreamEncryptionService(), link);
  }

  /**
   * Required-args Constructor.
   *
   * @param streamEncryptionService An instance of {@link StreamEncryptionService} used to encrypt and decrypted
   *                                end-to-end STREAM packet data (i.e., packets that should only be visible between
   *                                sender and receiver).
   * @param link                    A {@link Link} that is used to send ILPv4 packets to an immediate peer.
   */
  public SimpleStreamSender(final StreamEncryptionService streamEncryptionService, final Link link) {
    this(
        streamEncryptionService, link, newDefaultExecutor());
  }

  /**
   * Required-args Constructor.
   *
   * @param streamEncryptionService An instance of {@link StreamEncryptionService} used to encrypt and decrypted
   *                                end-to-end STREAM packet data (i.e., packets that should only be visible between
   *                                sender and receiver).
   * @param link                    A {@link Link} that is used to send ILPv4 packets to an immediate peer.
   * @param executorService         executorService to run the payments
   */
  public SimpleStreamSender(
      final StreamEncryptionService streamEncryptionService, final Link link, ExecutorService executorService
  ) {
    this(streamEncryptionService, link, executorService, new StreamConnectionManager());
  }

  /**
   * Required-args Constructor.
   *
   * @param streamEncryptionService A {@link StreamEncryptionService} used to encrypt and decrypted end-to-end STREAM
   *                                packet data (i.e., packets that should only be visible between sender and
   *                                receiver).
   * @param link                    A {@link Link} that is used to send ILPv4 packets to an immediate peer.
   * @param executorService         A {@link ExecutorService} to run the payments.
   * @param streamConnectionManager A {@link StreamConnectionManager} that manages connections for all senders and
   *                                receivers in this JVM.
   */
  public SimpleStreamSender(
      final StreamEncryptionService streamEncryptionService,
      final Link link,
      final ExecutorService executorService,
      final StreamConnectionManager streamConnectionManager
  ) {
    this.streamEncryptionService = Objects.requireNonNull(streamEncryptionService);
    this.link = Objects.requireNonNull(link);

    // Note that pools with similar properties but different details (for example, timeout parameters) may be
    // created using {@link ThreadPoolExecutor} constructors.
    this.executorService = Objects.requireNonNull(executorService);
    this.streamConnectionManager = Objects.requireNonNull(streamConnectionManager);
  }

  private static ExecutorService newDefaultExecutor() {
    ThreadFactory factory = new ThreadFactoryBuilder()
        .setDaemon(true)
        .setNameFormat("simple-stream-sender-%d")
        .build();
    return Executors.newFixedThreadPool(30, factory);
  }

  @Override
  public CompletableFuture<SendMoneyResult> sendMoney(SendMoneyRequest request) {
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
        request
    ).send();
  }

  /**
   * Contains summary information about a STREAM Connection.
   */
  @Immutable
  public interface ConnectionStatistics {

    static ConnectionStatisticsBuilder builder() {
      return new ConnectionStatisticsBuilder();
    }

    /**
     * The number of fulfilled packets that were received over the lifetime of this connection.
     *
     * @return An int representing the number of fulfilled packets.
     */
    int numFulfilledPackets();

    /**
     * The number of rejected packets that were received over the lifetime of this connection.
     *
     * @return An int representing the number of rejected packets.
     */
    int numRejectPackets();

    /**
     * Compute the total number of packets that were fulfilled or rejected on this Connection.
     *
     * @return An int representing the total number of response packets processed.
     */
    @Derived
    default int totalPackets() {
      return numFulfilledPackets() + numRejectPackets();
    }

    /**
     * The total amount delivered to the receiver.
     *
     * @return An {@link UnsignedLong} representing the total amount delivered.
     */
    UnsignedLong amountDelivered();
  }

  /**
   * Encapsulates everything needed to send a particular amount of money by breaking up a payment into a bunch of
   * smaller packets, and then handling all responses. This aggregator operates on a single Connection by opening and
   * closing a single stream.
   */
  static class SendMoneyAggregator {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final ExecutorService executorService;
    private final StreamConnection streamConnection;
    private final CodecContext streamCodecContext;
    private final StreamEncryptionService streamEncryptionService;
    private final CongestionController congestionController;
    private final Link link;
    private final SharedSecret sharedSecret;
    private final Optional<Duration> timeout;
    private final Denomination senderDenomination;

    private final InterledgerAddress sourceAddress;
    private final InterledgerAddress destinationAddress;

    private final AtomicBoolean shouldSendSourceAddress;
    private final AtomicInteger numFulfilledPackets;
    private final AtomicInteger numRejectedPackets;
    private final PaymentTracker paymentTracker;
    private Optional<Denomination> receiverDenomination = Optional.empty();

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
     * @param request                 all relevant details about the money to send
     */
    SendMoneyAggregator(
        final ExecutorService executorService,
        final StreamConnection streamConnection,
        final CodecContext streamCodecContext,
        final Link link,
        final CongestionController congestionController,
        final StreamEncryptionService streamEncryptionService,
        final SendMoneyRequest request
    ) {
      this.executorService = Objects.requireNonNull(executorService);
      this.streamConnection = Objects.requireNonNull(streamConnection);

      this.streamCodecContext = Objects.requireNonNull(streamCodecContext);
      this.link = Objects.requireNonNull(link);
      this.streamEncryptionService = Objects.requireNonNull(streamEncryptionService);
      this.congestionController = Objects.requireNonNull(congestionController);
      this.shouldSendSourceAddress = new AtomicBoolean(true);

      this.sharedSecret = request.sharedSecret();
      this.sourceAddress = request.sourceAddress();
      this.destinationAddress = request.destinationAddress();

      this.numFulfilledPackets = new AtomicInteger(0);
      this.numRejectedPackets = new AtomicInteger(0);

      this.timeout = request.timeout();

      this.senderDenomination = request.denomination();

      this.paymentTracker = request.paymentTracker();
    }

    /**
     * Send money in an individual stream.
     *
     * @return A {@link CompletableFuture} containing a {@link SendMoneyResult}.
     */
    CompletableFuture<SendMoneyResult> send() {
      Objects.requireNonNull(sharedSecret);
      Objects.requireNonNull(destinationAddress);

      Instant startPreflight = Instant.now();
      try {
        receiverDenomination = preflightCheck();
      } catch (StreamConnectionClosedException e) {
        return CompletableFuture.completedFuture(SendMoneyResult.builder()
            .sendMoneyDuration(Duration.between(startPreflight, Instant.now()))
            .numRejectPackets(1)
            .numFulfilledPackets(0)
            .amountDelivered(UnsignedLong.ZERO)
            .amountSent(UnsignedLong.ZERO)
            .originalAmount(paymentTracker.getOriginalAmount())
            .amountLeftToSend(paymentTracker.getOriginalAmountLeft())
            .successfulPayment(paymentTracker.successful())
            .build());
      } catch (Exception e) {
        logger.warn("Preflight check failed", e);
      }
      // A separate executor is needed for overall call to sendMoneyPacketized otherwise a livelock can occur.
      // Using a shared executor could cause sendMoneyPacketized to internally get blocked from submitting tasks
      // because the shared executor is already blocked waiting on the results of the call here to sendMoneyPacketized
      ExecutorService sendMoneyExecutor = Executors.newSingleThreadExecutor();
      final Instant start = Instant.now();
      // All futures will run here using the Cached Executor service.
      return CompletableFuture
          .supplyAsync(() -> {

            // Do all the work of sending packetized money for this Stream/sendMoney request.
            this.sendMoneyPacketized();
            return SendMoneyResult.builder()
                .amountDelivered(paymentTracker.getDeliveredAmount())
                .amountSent(paymentTracker.getAmountSent())
                .amountLeftToSend(paymentTracker.getOriginalAmountLeft())
                .originalAmount(paymentTracker.getOriginalAmount())
                .numFulfilledPackets(numFulfilledPackets.get())
                .numRejectPackets(numRejectedPackets.get())
                .sendMoneyDuration(Duration.between(start, Instant.now()))
                .successfulPayment(paymentTracker.successful())
                .build();
          }, sendMoneyExecutor)
          .whenComplete(($, error) -> {
            sendMoneyExecutor.shutdown();
            if (error != null) {
              logger.error("SendMoney Stream failed: " + error.getMessage(), error);
            }
            if (!$.successfulPayment()) {
              logger.error("Failed to send full amount");
            }
          });
    }

    /**
     * TODO: See https://github.com/hyperledger/quilt/issues/308 to determine when the Stream and/or Connection should
     * be closed.
     */
    @VisibleForTesting
    Optional<Denomination> preflightCheck() throws StreamConnectionClosedException {
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
        throw e;
      }

      final List<StreamFrame> frames = Lists.newArrayList(
          StreamMoneyFrame.builder()
              // This aggregator supports only a simple stream-id, which is one.
              .streamId(UnsignedLong.ONE)
              .shares(UnsignedLong.ONE)
              .build(),
          ConnectionNewAddressFrame.builder()
              .sourceAddress(sourceAddress)
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
          .expiresAt(Instant.now().plusSeconds(30L))
          .data(streamPacketData)
          .build();

      InterledgerResponsePacket responsePacket = link.sendPacket(preparePacket);

      final Function<InterledgerResponsePacket, Optional<Denomination>> readDetails = (p) -> {
        final StreamPacket packet = this.fromEncrypted(sharedSecret, p.getData());
        return packet.frames().stream()
            .filter(f -> f.streamFrameType() == StreamFrameType.ConnectionAssetDetails)
            .findFirst()
            .map(f -> (ConnectionAssetDetailsFrame) f)
            .map(f -> Denomination.builder().from(f.sourceDenomination()).build());
      };

      return responsePacket.map(
          fulfillPacket -> readDetails.apply(fulfillPacket),
          rejectPacket -> readDetails.apply(rejectPacket)
      );
    }

    private void sendMoneyPacketized() {

      final AtomicBoolean timeoutReached = new AtomicBoolean(false);

      final ScheduledExecutorService timeoutMonitor = Executors.newSingleThreadScheduledExecutor();

      timeout.ifPresent($ -> timeoutMonitor.schedule(
          () -> {
            timeoutReached.set(true);
            timeoutMonitor.shutdown();
          },
          $.toMillis(), TimeUnit.MILLISECONDS
      ));

      while (soldierOn(timeoutReached.get())) {
        // Determine the amount to send
        PrepareAmounts amounts = paymentTracker.getSendPacketAmounts(congestionController.getMaxAmount(),
            senderDenomination,
            receiverDenomination);
        UnsignedLong amountToSend = amounts.getAmountToSend();
        UnsignedLong receiverMinimum = amounts.getMinimumAmountToAccept();

        if (amountToSend.equals(UnsignedLong.ZERO) || timeoutReached.get()) {
          try {
            // Don't send any more, but wait a bit for outstanding requests to complete so we don't cycle needlessly in
            // a while loop that doesn't do anything useful.
            Thread.sleep(100);
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
        final InterledgerCondition executionCondition;
        executionCondition = generatedFulfillableFulfillment(sharedSecret, streamPacketData).getCondition();

        final InterledgerPreparePacket preparePacket = InterledgerPreparePacket.builder()
            .destination(destinationAddress)
            .amount(amountToSend)
            .executionCondition(executionCondition)
            .expiresAt(Instant.now().plusSeconds(30L))
            .data(streamPacketData)
            .build();

        // auth
        // capture
        // rollback

        final PrepareAmounts prepareAmounts = PrepareAmounts.from(preparePacket, streamPacket);
        paymentTracker.auth(prepareAmounts);

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
            continue;
          }
        } catch (Exception e) {
          // Retry this amount on the next run...
          paymentTracker.rollback(prepareAmounts, false);
          logger.error("Submit failed", e);
        }
      }
      timeoutMonitor.shutdownNow();
    }

    @VisibleForTesting
    void schedule(AtomicBoolean timeoutReached, InterledgerPreparePacket preparePacket, StreamPacket streamPacket,
                  PrepareAmounts prepareAmounts) {
      try {
        executorService.submit(() -> {
          if (!timeoutReached.get()) {
            try {
              InterledgerResponsePacket responsePacket = link.sendPacket(preparePacket);
              responsePacket.handle(
                  fulfillPacket -> handleFulfill(preparePacket, streamPacket, fulfillPacket, prepareAmounts),
                  rejectPacket -> handleReject(preparePacket, streamPacket, rejectPacket, prepareAmounts,
                      numRejectedPackets, congestionController)
              );
            } catch (Exception e) {
              logger.error("Link send failed. preparePacket={}", preparePacket, e);
              congestionController.reject(preparePacket.getAmount(), InterledgerRejectPacket.builder()
                  .code(InterledgerErrorCode.F00_BAD_REQUEST)
                  .message(
                      String.format("Link send failed. preparePacket=%s error=%s", preparePacket, e.getMessage())
                  )
                  .build());
              paymentTracker.rollback(prepareAmounts, false);
            }
          }
        });
      } catch (RejectedExecutionException e) {
        // If we get here, it means the task was unable to be scheduled, so we need to unwind the congestion
        // controller to prevent deadlock.
        congestionController.reject(preparePacket.getAmount(), InterledgerRejectPacket.builder()
            .code(InterledgerErrorCode.F00_BAD_REQUEST)
            .message(
                String.format("Unable to schedule sendMoney task. preparePacket=%s error=%s", preparePacket,
                    e.getMessage())
            )
            .build());
        throw e;
      }
    }

    @VisibleForTesting
    boolean soldierOn(boolean timeoutReached) {
      // if money in flight, always soldier on
      // otherwise, soldier on if
      //   the connection is not closed
      //   and you haven't delivered the full amount
      //   and you haven't timed out
      return this.congestionController.hasInFlight()
          || (!streamConnection.isClosed() && paymentTracker.moreToSend() && !timeoutReached);
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

      // TODO should we check the fulfillment and expiry or can we assume the plugin does that?
      this.congestionController.fulfill(originalPreparePacket.getAmount());
      this.shouldSendSourceAddress.set(false);

      final StreamPacket streamPacket = this.fromEncrypted(sharedSecret, fulfillPacket.getData());

      //if let Ok (packet) = StreamPacket::from_encrypted ( & self.shared_secret, fulfill.into_data()){
      if (streamPacket.interledgerPacketType() == InterledgerPacketType.FULFILL) {
        // TODO check that the sequence matches our outgoing packet
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
      congestionController.reject(amountToSend, rejectPacket);

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

      switch (rejectPacket.getCode().getCode()) {

        case T04_INSUFFICIENT_LIQUIDITY_CODE:
        case F08_AMOUNT_TOO_LARGE_CODE: {
          // Handled by the congestion controller
          break;
        }
        default: {
          if (rejectPacket.getCode().getErrorFamily() == ErrorFamily.TEMPORARY) {
            logger.warn(
                "Temporary ILPv4 transport outage. Retrying... originalPreparePacket={} originalStreamPacket={} "
                    + "rejectPacket={}",
                originalPreparePacket, originalStreamPacket, rejectPacket);

          } else {
            logger.error(
                "Encountered Final ILPv4 error. Retrying, but this sendMoney will likely hang until timeout."
                    + " originalPreparePacket={} originalStreamPacket={} rejectPacket={}",
                originalPreparePacket, originalStreamPacket, rejectPacket);
          }
          break;
        }
      }
    }

    ///**
    // * Close the current STREAM connection by sending a {@link ConnectionCloseFrame} to the receiver.
    // *
    // * @return An {@link UnsignedLong} representing the amount delivered by this individual stream.
    // */
    // TODO: Add unit test coverage here per See https://github.com/hyperledger/quilt/issues/308
    // @VisibleForTesting
    // void closeStream() throws StreamConnectionClosedException {
    //   this.sendStreamFramesInZeroValuePacket(Lists.newArrayList(
    //       StreamCloseFrame.builder()
    //           .streamId(UnsignedLong.ONE)
    //           .errorCode(ErrorCode.NoError)
    //           .build()
    //   ));
    // }

    // /**
    //  * Close the current STREAM connection by sending a {@link ConnectionCloseFrame} to the receiver.
    //  *
    //  * @return An {@link UnsignedLong} representing the amount delivered by this individual stream.
    //  */
    // // TODO: Add unit test coverage here per See https://github.com/hyperledger/quilt/issues/308
    // @VisibleForTesting
    // void closeConnection() throws StreamConnectionClosedException {
    //   this.sendStreamFramesInZeroValuePacket(Lists.newArrayList(
    //       ConnectionCloseFrame.builder()
    //           .errorCode(ErrorCode.NoError)
    //           .build()
    //   ));
    // }

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
          .expiresAt(Instant.now().plusSeconds(30L))
          .data(encryptedStreamPacket)
          .build();

      final PrepareAmounts prepareAmounts = PrepareAmounts.from(preparePacket, streamPacket);

      link.sendPacket(preparePacket).handle(
          fulfillPacket -> handleFulfill(preparePacket, streamPacket, fulfillPacket, prepareAmounts),
          rejectPacket -> handleReject(preparePacket, streamPacket, rejectPacket, prepareAmounts, numRejectedPackets,
              congestionController)
      );

      // Mark the streamConnection object as closed if the caller supplied a ConnectionCloseFrame
      streamFrames.stream()
          .filter(streamFrame -> streamFrame.streamFrameType() == StreamFrameType.ConnectionClose)
          .findAny()
          .ifPresent($ -> {
            streamConnection.closeConnection();
            logger.info("STREAM Connection closed.");
          });

      // Emit a log statement if the called supplied a StreamCloseFrame
      streamFrames.stream()
          .filter(streamFrame -> streamFrame.streamFrameType() == StreamFrameType.StreamClose)
          .findAny()
          .map($ -> (StreamCloseFrame) $)
          .ifPresent($ -> {
            logger.info(
                "StreamId {} Closed. Delivered: {} ({} packets fulfilled, {} packets rejected)",
                $.streamId(), paymentTracker.getDeliveredAmount(), this.numFulfilledPackets.get(),
                this.numRejectedPackets.get()
            );
          });
    }

    /**
     * Close the current STREAM connection by sending a {@link ConnectionCloseFrame} to the receiver.
     *
     * @return An {@link UnsignedLong} representing the amount delivered by this individual stream.
     */
    @VisibleForTesting
    SendMoneyResult collectSendMoneyStatistics() {
      return SendMoneyResult.builder()
          .amountDelivered(paymentTracker.getDeliveredAmount())
          .amountSent(paymentTracker.getAmountSent())
          .amountLeftToSend(paymentTracker.getOriginalAmountLeft())
          .numFulfilledPackets(this.numFulfilledPackets.get())
          .numRejectPackets(this.numRejectedPackets.get())
          .successfulPayment(paymentTracker.successful())
          .build();
    }
  }
}
