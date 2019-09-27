package org.interledger.stream.sender;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.google.common.primitives.UnsignedLong;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.interledger.codecs.stream.StreamCodecContextFactory;
import org.interledger.core.Immutable;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerCondition;
import org.interledger.core.InterledgerFulfillPacket;
import org.interledger.core.InterledgerPacketType;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerRejectPacket;
import org.interledger.core.InterledgerResponsePacket;
import org.interledger.encoding.asn.framework.CodecContext;
import org.interledger.link.Link;
import org.interledger.stream.SendMoneyResult;
import org.interledger.stream.StreamPacket;
import org.interledger.stream.StreamUtils;
import org.interledger.stream.crypto.StreamEncryptionService;
import org.interledger.stream.frames.ConnectionCloseFrame;
import org.interledger.stream.frames.ConnectionNewAddressFrame;
import org.interledger.stream.frames.ErrorCode;
import org.interledger.stream.frames.StreamFrame;
import org.interledger.stream.frames.StreamMoneyFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.concurrent.NotThreadSafe;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.interledger.core.InterledgerErrorCode.F08_AMOUNT_TOO_LARGE_CODE;
import static org.interledger.core.InterledgerErrorCode.F99_APPLICATION_ERROR_CODE;
import static org.interledger.stream.StreamUtils.generatedFulfillableFulfillment;

/**
 * <p>A simple implementation of {@link StreamSender} that opens a STREAM connection, sends money, and then closes the
 * connection, yielding a response.</p>
 *
 * <p>Note that this implementation does not currently support sending data, which is defined in the STREAM
 * protocol.</p>
 *
 * <p>Note that, per https://github.com/hyperledger/quilt/issues/242, as of the publication of this client,
 * connectors will reject ILP packets that exceed 32kb. This implementation does not overtly check to restrict
 * the size of thedatafield in any particular {@link InterledgerPreparePacket}, for two reasons. First, this
 * implementation never packs a sufficient number of STREAM frames into a single Prepare packet for this 32kb
 * limit to be an issue; Second, if the ILPv4 RFC ever changes to increase this size limitation, we don't want
 * sender/receiver software to have to be updated across the Interledger.</p>
 *
 */
@NotThreadSafe
public class SimpleStreamSender implements StreamSender {

  private final Link link;
  private final StreamEncryptionService streamEncryptionService;
  private final ExecutorService executorService;

  /**
   * Required-args Constructor.
   *
   * @param streamEncryptionService An instance of {@link StreamEncryptionService} used to encrypt and decrypted
   *                                end-to-end STREAM packet data (i.e., packets that should only be visible between
   *                                sender and receiver).
   * @param link                    A {@link Link} that is used to send ILPv4 packets to an immediate peer.
   */
  public SimpleStreamSender(
      final StreamEncryptionService streamEncryptionService, final Link link
  ) {
    // from the Executors.newCachedThreadPool
    this(streamEncryptionService, link, new ThreadPoolExecutor(0, 200,
        60L, TimeUnit.SECONDS,
        new SynchronousQueue<>(),  new ThreadFactoryBuilder()
        .setDaemon(true)
        .setNameFormat("simple-stream-sender-%d")
        .build()));
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
    this.streamEncryptionService = Objects.requireNonNull(streamEncryptionService);
    this.link = Objects.requireNonNull(link);

    // Note that pools with similar properties but different details (for example, timeout parameters) may be
    // created using {@link ThreadPoolExecutor} constructors.
    this.executorService = Objects.requireNonNull(executorService);
  }

  @Override
  public CompletableFuture<SendMoneyResult> sendMoney(
      final byte[] sharedSecret,
      final InterledgerAddress sourceAddress,
      final InterledgerAddress destinationAddress,
      final UnsignedLong amount
  ) {
    return sendMoney(sharedSecret, sourceAddress, destinationAddress, amount, Duration.ofSeconds(60));
  }

  @Override
  public CompletableFuture<SendMoneyResult> sendMoney(
      final byte[] sharedSecret,
      final InterledgerAddress sourceAddress,
      final InterledgerAddress destinationAddress,
      final UnsignedLong amount,
      final Duration timeout
  ) {
    Objects.requireNonNull(destinationAddress);
    Objects.requireNonNull(amount);

    final SendMoneyAggregator sendMoneyAggregator = new SendMoneyAggregator(
        this.executorService,
        StreamCodecContextFactory.oer(),
        this.link,
        new AimdCongestionController(),
        this.streamEncryptionService,
        sharedSecret,
        sourceAddress,
        destinationAddress,
        amount,
        timeout
    );
    return sendMoneyAggregator.send();
  }


  @Immutable
  public interface CloseConnectionResult {

    static CloseConnectionResultBuilder builder() {
      return new CloseConnectionResultBuilder();
    }

    // Implementations MUST close the connection once either endpoint has sent 2^31 packets.
    int numFulfilledPackets();

    int numRejectPackets();

    default int totalPackets() {
      return numFulfilledPackets() + numRejectPackets();
    }

    UnsignedLong amountDelivered();
  }

  // TODO: Pull Connection-management operations out of the Aggregator? Maybe make the client itself able to do these
  //  things via an accessor to the connection manager. It's generally not used, but can be if desired.

  /**
   * Encapsulates everything needed to send a particular amount of money by breaking up a payment into a bunch of
   * smaller packets, and then handling all responses. This aggregator operates on a single Connection by opening and
   * closing a single stream.
   */
  protected static class SendMoneyAggregator {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final ExecutorService executorService;
    private final CodecContext streamCodecContext;
    private final StreamEncryptionService streamEncryptionService;
    private final CongestionController congestionController;
    private final Link link;
    private final byte[] sharedSecret;
    private final Duration timeout;

    private final InterledgerAddress sourceAddress;
    private final InterledgerAddress destinationAddress;

    // The amount
    private AtomicReference<UnsignedLong> originalAmountToSend;
    private AtomicReference<UnsignedLong> amountLeftToSend;
    private AtomicReference<UnsignedLong> confirmedAmountRemaining;
    private AtomicReference<UnsignedLong> deliveredAmount;

    private AtomicBoolean shouldSendSourceAddress;

    private AtomicReference<UnsignedLong> sequence;
    private AtomicInteger numFulfilledPackets;
    private AtomicInteger numRejectedPackets;

    private final AtomicReference<Long> lastResponseTimestamp = new AtomicReference<>();

    /**
     * Required-args Constructor.
     *
     * @param executorService         An {@link ExecutorService} for sending multiple STREAM frames in parallel.
     * @param streamCodecContext      A {@link CodecContext} that can encode and decode ASN.1 OER Stream packets and
     *                                frames.
     * @param link                    The {@link Link} used to send ILPv4 packets containing Stream packets.
     * @param congestionController    A {@link CongestionController} that supports back-pressure for money streams.
     * @param streamEncryptionService A {@link StreamEncryptionService} that allows for Stream packet encryption and
     *                                decryption.
     * @param sharedSecret            The shared secret negotiated between the sender and receiver during payment setup
     *                                (e.g., using SPSP).
     * @param sourceAddress           The {@link InterledgerAddress} of the payment sender.
     * @param destinationAddress      The {@link InterledgerAddress} of the payment receiver.
     * @param originalAmountToSend    The amount of units (in the senders units) to send to the receiver.
     */
    public SendMoneyAggregator(
        final ExecutorService executorService,
        final CodecContext streamCodecContext,
        final Link link,
        final CongestionController congestionController,
        final StreamEncryptionService streamEncryptionService,
        final byte[] sharedSecret,
        final InterledgerAddress sourceAddress,
        final InterledgerAddress destinationAddress,
        final UnsignedLong originalAmountToSend,
        final Duration timeout
    ) {
      this.executorService = Objects.requireNonNull(executorService);
      this.streamCodecContext = Objects.requireNonNull(streamCodecContext);
      this.link = Objects.requireNonNull(link);
      this.streamEncryptionService = Objects.requireNonNull(streamEncryptionService);
      this.congestionController = Objects.requireNonNull(congestionController);
      this.shouldSendSourceAddress = new AtomicBoolean(true);

      this.sharedSecret = Objects.requireNonNull(sharedSecret);
      this.sourceAddress = sourceAddress;
      this.destinationAddress = Objects.requireNonNull(destinationAddress);

      this.originalAmountToSend = new AtomicReference<>(originalAmountToSend);
      this.amountLeftToSend = new AtomicReference<>(originalAmountToSend);
      this.confirmedAmountRemaining = new AtomicReference<>(originalAmountToSend);
      this.deliveredAmount = new AtomicReference<>(UnsignedLong.ZERO);
      this.sequence = new AtomicReference<>(UnsignedLong.ZERO);

      this.numFulfilledPackets = new AtomicInteger(0);
      this.numRejectedPackets = new AtomicInteger(0);

      this.timeout = timeout;
    }

    /**
     * Send money in an individual stream.
     *
     * @return A {@link CompletableFuture} containing a {@link SendMoneyResult}.
     */
    public CompletableFuture<SendMoneyResult> send() {
      Objects.requireNonNull(sharedSecret);
      Objects.requireNonNull(destinationAddress);
      Objects.requireNonNull(originalAmountToSend);

      // Fire off requests until the congestion controller tells us to stop or we've sent the total amount
      final List<CompletableFuture<InterledgerResponsePacket>> allFutures = Lists.newArrayList();

      final Instant start = Instant.now();

      final CompletableFuture<Void> paymentFuture = CompletableFuture.supplyAsync(() -> {
        sendMoneyPacketized();
        return null;
      });

      // To track the duration...
      final AtomicReference<Duration> sendMoneyDuration = new AtomicReference<>();

      // All futures will run here using the Cached Executor service.
      return paymentFuture.whenComplete(($, error) -> {
        if (error != null) {
          logger.error("SendMoney Stream failed: " + error.getMessage(), error);
        }
        if (!originalAmountToSend.get().equals(this.deliveredAmount.get())) {
          logger.error("Failed to send full amount");
        }
        sendMoneyDuration.set(Duration.between(start, Instant.now()));
      }).thenApply($ -> closeConnection(sequence.get()))
          .thenApply(closeConnectionResult -> SendMoneyResult.builder()
              .amountDelivered(closeConnectionResult.amountDelivered())
              .originalAmount(originalAmountToSend.get())
              .numFulfilledPackets(closeConnectionResult.numFulfilledPackets())
              .numRejectPackets(closeConnectionResult.numRejectPackets())
              .sendMoneyDuration(sendMoneyDuration.get())
              .build()
          );
    }

    private void sendMoneyPacketized() {
      final AtomicBoolean timeoutReached = new AtomicBoolean(false);

      if (!timeout.isZero()) {
        ScheduledExecutorService timeoutMonitor = Executors.newSingleThreadScheduledExecutor();
        timeoutMonitor.schedule(() -> timeoutReached.set(true), timeout.toMillis(), TimeUnit.MILLISECONDS);
      }

      while (soldierOn(timeoutReached.get())) {

        // Determine the amount to send
        final UnsignedLong amountToSend = StreamUtils.min(amountLeftToSend.get(), congestionController.getMaxAmount());
        if (amountToSend.equals(UnsignedLong.ZERO) || timeoutReached.get()) {
          try {
            Thread.sleep(100);
          } catch (InterruptedException e) {
            throw new StreamSenderException(e.getMessage(), e);
          }
          continue;
        }

        this.amountLeftToSend.getAndUpdate(sourceAmount -> sourceAmount.minus(amountToSend));

        // Load up the STREAM packet
        this.sequence.getAndUpdate($ -> $.plus(UnsignedLong.ONE));

        final List<StreamFrame> frames = Lists.newArrayList(
            StreamMoneyFrame.builder()
                // This aggregator supports only a simple stream-id, which is one.
                .streamId(UnsignedLong.ONE)
                .shares(UnsignedLong.ONE)
                .build()
        );

        // This isn't perfectly synchronized (it will be true until the first fulfill comes back), but it's OK if
        // potentially many ILPv4 packets are sent out with this frame because it will always be the same "new" source
        // address, so the receiver will merely update to the same address a few times, which is fine.
        if (this.shouldSendSourceAddress.get()) {
          frames.add(ConnectionNewAddressFrame.builder()
              .sourceAddress(sourceAddress)
              .build()
          );
        }

        final StreamPacket streamPacket = StreamPacket.builder()
            .interledgerPacketType(InterledgerPacketType.PREPARE)
            // If the STREAM packet is sent on an ILP Prepare, this represents the minimum the receiver should accept.
            // TODO: enforce min exchange rate?
            .prepareAmount(UnsignedLong.ZERO)
            .sequence(sequence.get())
            .frames(frames)
            .build();

        // Create the ILP Prepare packet
        final byte[] streamPacketData = this.toEncrypted(sharedSecret, streamPacket);
        final InterledgerCondition executionCondition;
        executionCondition = generatedFulfillableFulfillment(sharedSecret, streamPacketData).getCondition();

        final InterledgerPreparePacket preparePacket = InterledgerPreparePacket.builder()
            .destination(destinationAddress)
            .amount(amountToSend.bigIntegerValue())
            .executionCondition(executionCondition)
            .expiresAt(Instant.now().plusSeconds(30L))
            .data(streamPacketData)
            .build();


        try {
          // don't submit new tasks if the timeout was reached within this iteration of the while loop
          if (canBeScheduled(timeoutReached.get(), streamPacket.sequence())) {
            // call this before spinning off a task. failing to do so can create a condition in the
            // soliderOn check where we will incorrectly evaluate hasInFlight on the congestion
            // controller to not reflect what we've actually scheduled to run, resulting in the loop
            // breaking prematurely
            congestionController.prepare(amountToSend);
            executorService.submit(() -> {
              if (!timeoutReached.get()) {
                try  {
                  InterledgerResponsePacket responsePacket = link.sendPacket(preparePacket);
                  responsePacket.handle(
                      (fulfillPacket -> {
                        handleFulfill(streamPacket.sequence(), amountToSend, fulfillPacket);
                      }),
                      (rejectPacket -> {
                        handleReject(streamPacket.sequence(), amountToSend, rejectPacket);
                      }));
                } catch (Exception e) {
                  logger.error("Send packet failed", e);
                }
              }
            });
          }

        }
        catch (Exception e) {
          logger.error("Submit failed", e);
        }

      }
    }

    private boolean maxPacketsReached() {
      return sequence.get().compareTo(StreamPacket.MAX_FRAMES_PER_CONNECTION) >= 0;
    }

    private boolean maxPacketsExceeded(UnsignedLong currentSequence) {
      return currentSequence.compareTo(StreamPacket.MAX_FRAMES_PER_CONNECTION) > 0;
    }

    /**
     * Tasks are only scheduled if we haven't timed out and haven't exceeded the max number of packets that can be sent.
     * Per IL-RFC-29, "Implementations MUST close the connection once either endpoint has sent 2^31 packets. According
     * to NIST, it is unsafe to use AES-GCM for more than 2^32 packets using the same encryption key. (STREAM uses the
     * limit of 2^31 because both endpoints encrypt packets with the same key.)
     *
     * @param timeoutReached whether or not we've exceeded the timeout for non in-flight requests
     * @param currentSequence sequence number of the most recent packet to be sent
     *
     * @return true if we can schedule a task.
     */
    @VisibleForTesting
    boolean canBeScheduled(boolean timeoutReached, UnsignedLong currentSequence) {
      // Integer.MAX_VALUE is equal to: 2<sup>31</sup>-1, so break only after we exceed Integer.MAX_VALUE
      return !timeoutReached && !maxPacketsExceeded(currentSequence);
    }

    @VisibleForTesting
    boolean soldierOn(boolean timeoutReached) {
      // if money in flight, always soldier on
      // else
      //   you haven't reached max packets
      //   and you haven't delivered the full amount
      //   and you haven't timed out
      return this.congestionController.hasInFlight() ||
          (!maxPacketsReached() &&
           this.deliveredAmount.get().compareTo(this.originalAmountToSend.get()) < 0 &&
           !timeoutReached
          );
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
    protected byte[] toEncrypted(final byte[] sharedSecret, final StreamPacket streamPacket) {
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
    protected StreamPacket fromEncrypted(final byte[] sharedSecret, final byte[] encryptedStreamPacketBytes) {
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
    protected void handleFulfill(
        final UnsignedLong sequence,
        final UnsignedLong amount,
        final InterledgerFulfillPacket fulfillPacket
    ) {
      Objects.requireNonNull(sequence);
      Objects.requireNonNull(amount);
      Objects.requireNonNull(fulfillPacket);

      this.numFulfilledPackets.getAndIncrement();

      // TODO should we check the fulfillment and expiry or can we assume the plugin does that?
      this.congestionController.fulfill(amount);
      this.shouldSendSourceAddress.set(false);

      final StreamPacket streamPacket = this.fromEncrypted(sharedSecret, fulfillPacket.getData());

      //if let Ok (packet) = StreamPacket::from_encrypted ( & self.shared_secret, fulfill.into_data()){
      if (streamPacket.interledgerPacketType() == InterledgerPacketType.FULFILL) {
        // TODO check that the sequence matches our outgoing packet
        this.deliveredAmount.getAndUpdate(currentAmount -> currentAmount.plus(streamPacket.prepareAmount()));
      } else {
        logger.warn("Unable to parse STREAM packet from fulfill data for sequence {}", sequence);
      }

      logger.debug(
          "Prepare {} with amount {} was fulfilled ({} left to send)",
          sequence, amount, this.amountLeftToSend
      );
    }

    /**
     * Handle a rejection packet.
     *
     * @param sequence     An {@link UnsignedLong} representing the current sequence number as viewed from this client.
     * @param amountToSend The amount that was originally sent in the prepare packet.
     * @param rejectPacket The {@link InterledgerRejectPacket} received from a peer directly connected via a {@link
     *                     Link}.
     */
    @VisibleForTesting
    protected void handleReject(
        final UnsignedLong sequence,
        final UnsignedLong amountToSend,
        final InterledgerRejectPacket rejectPacket
    ) {
      Objects.requireNonNull(sequence);
      Objects.requireNonNull(amountToSend);
      Objects.requireNonNull(rejectPacket);

      this.numRejectedPackets.getAndIncrement();
      this.amountLeftToSend.getAndUpdate(currentAmount -> currentAmount.plus(amountToSend));
      this.congestionController.reject(amountToSend, rejectPacket);

      logger.debug(
          "Prepare {} with amount {} was rejected with code: {} ({} left to send)",
          sequence,
          amountToSend,
          rejectPacket.getCode(),
          this.amountLeftToSend.get()
      );

      switch (rejectPacket.getCode().getCode()) {
        case F08_AMOUNT_TOO_LARGE_CODE: {
          // Handled by the congestion controller
          break;
        }
        case F99_APPLICATION_ERROR_CODE: {
          // TODO handle STREAM errors
          break;
        }
        default: {
          throw new StreamSenderException(String.format("Packet was rejected. rejectPacket=%s", rejectPacket));
        }
      }
    }

    /**
     * Close the current STREAM connection by sending a {@link ConnectionCloseFrame} to the receiver.
     *
     * @param sequence An {@link UnsignedLong} representing the current sequence number as viewed from this client.
     *
     * @return An {@link UnsignedLong} representing the amount delivered by this individual stream.
     */
    @VisibleForTesting
    protected CloseConnectionResult closeConnection(final UnsignedLong sequence) {
      Objects.requireNonNull(sequence);

      final StreamPacket streamPacket = StreamPacket.builder()
          .interledgerPacketType(InterledgerPacketType.PREPARE)
          // TODO: enforce min exchange rate.
          .prepareAmount(UnsignedLong.ZERO)
          .sequence(sequence)
          .addFrames(ConnectionCloseFrame.builder()
              .errorCode(ErrorCode.NoError)
              .build())
          .build();

      // Create the ILP Prepare packet using an encrypted StreamPacket as the encryptedStreamPacket payload...
      final byte[] encryptedStreamPacket = this.toEncrypted(sharedSecret, streamPacket);
      final InterledgerCondition executionCondition;
      executionCondition = generatedFulfillableFulfillment(sharedSecret, encryptedStreamPacket).getCondition();

      final InterledgerPreparePacket preparePacket = InterledgerPreparePacket.builder()
          .destination(destinationAddress)
          .amount(BigInteger.ZERO)
          .executionCondition(executionCondition)
          .expiresAt(Instant.now().plusSeconds(30L))
          .data(encryptedStreamPacket)
          .build();

      logger.debug("Closing STREAM Connection...");

      link.sendPacket(preparePacket).handle(
          (fulfillPacket -> {
            handleFulfill(sequence, UnsignedLong.valueOf(preparePacket.getAmount()), fulfillPacket);
          }),
          (rejectPacket -> {
            handleReject(sequence, UnsignedLong.valueOf(preparePacket.getAmount()), rejectPacket);
          })
      );

      logger.debug(
          "Send money future finished. Delivered: {} ({} packets fulfilled, {} packets rejected)",
          this.deliveredAmount, this.numFulfilledPackets.get(), this.numRejectedPackets.get()
      );

      return CloseConnectionResult.builder()
          .amountDelivered(this.deliveredAmount.get())
          .numFulfilledPackets(this.numFulfilledPackets.get())
          .numRejectPackets(this.numRejectedPackets.get())
          .build();
    }

    /**
     * Helper method to obtain the `sequence` of this {@link SendMoneyAggregator}. While under normal circumstances it
     * is ill-advised to be testing a private variable, here we break this rule as a testing optimization. The behavior
     * that this is exposing this method allows us to validate is the closing of a STREAM Connection after 2^31 frames.
     * While the _correct_ way to test this would be to send 2^31 frames into the send method and then assert that the
     * connection closes,we instead expose this mutator in order to avoid 2^31 redundant calls on every test execution.
     *
     * @param newSequence An {@link UnsignedLong} containing a value to set {@link #sequence} to.
     */
    @VisibleForTesting
    void setSequenceForTesting(final UnsignedLong newSequence) {
      Objects.requireNonNull(newSequence);
      this.sequence.set(newSequence);
    }

    /**
     * Helper method to mess with delivered amount for the purpose of testing loop breaking conditions.
     *
     * @param deliveredAmount
     */
    @VisibleForTesting
    void setDeliveredAmountForTesting(final UnsignedLong deliveredAmount) {
      Objects.requireNonNull(deliveredAmount);
      this.deliveredAmount.set(deliveredAmount);
    }
  }
}
