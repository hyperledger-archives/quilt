package org.interledger.stream.client;

import static org.interledger.core.InterledgerErrorCode.F08_AMOUNT_TOO_LARGE_CODE;
import static org.interledger.core.InterledgerErrorCode.F99_APPLICATION_ERROR_CODE;
import static org.interledger.stream.StreamUtils.generatedFulfillableFulfillment;

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
import org.interledger.stream.ConnectionManager;
import org.interledger.stream.SendMoneyResult;
import org.interledger.stream.StreamEndpoint;
import org.interledger.stream.StreamPacket;
import org.interledger.stream.StreamUtils;
import org.interledger.stream.crypto.StreamEncryptionService;
import org.interledger.stream.frames.ConnectionCloseFrame;
import org.interledger.stream.frames.ConnectionNewAddressFrame;
import org.interledger.stream.frames.ErrorCode;
import org.interledger.stream.frames.StreamFrame;
import org.interledger.stream.frames.StreamMoneyFrame;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.google.common.primitives.UnsignedLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.SignatureException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class StreamClient implements StreamEndpoint {

  // TODO: Executor executor = Executors.newFixedThreadPool(10);
  // Maybe here: https://www.callicoder.com/java-8-completablefuture-tutorial/

  private final StreamEncryptionService streamEncryptionService;

  // TODO: Deal with this?
  private final Link link;
  private final ExecutorService executorService;

  public StreamClient(
      final StreamEncryptionService streamEncryptionService,
      Link link, final ConnectionManager connectionManager
  ) {
    this.streamEncryptionService = Objects.requireNonNull(streamEncryptionService);
    this.link = link;

    // Note that pools with similar properties but different details (for example, timeout parameters) may be
    // created using {@link ThreadPoolExecutor} constructors.
    this.executorService = Executors.newCachedThreadPool();
  }

  // TODO: Handle Ping.
//  /**
//   * Send a very-small value payment to the destination and expect an ILP fulfillment, which demonstrates this sender
//   * has send-data ping to the indicated destination address.
//   *
//   * @param destinationAddress
//   */
//  InterledgerResponsePacket ping(final InterledgerAddress destinationAddress, final BigInteger pingAmount) {
//    Objects.requireNonNull(destinationAddress);
//
//    final InterledgerPreparePacket pingPacket = InterledgerPreparePacket.builder()
//        .executionCondition(PING_PROTOCOL_CONDITION)
//        // TODO: Make this timeout configurable!
//        .expiresAt(Instant.now().plusSeconds(30))
//        .amount(pingAmount)
//        .destination(destinationAddress)
//        .build();
//
//    return this.sendPacket(pingPacket);
//  }

  @Override
  public CompletableFuture<SendMoneyResult> sendMoney(
      final byte[] sharedSecret,
      final InterledgerAddress sourceAddress,
      final InterledgerAddress destinationAddress,
      final UnsignedLong amount
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
        amount
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

  // TODO: Also see ConnectionDetails

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

    private final InterledgerAddress sourceAddress;
    private final InterledgerAddress destinationAddress;

    // The amount
    private AtomicReference<UnsignedLong> originalAmountToSend;
    private AtomicReference<UnsignedLong> amountLeftToSend;
    private AtomicReference<UnsignedLong> deliveredAmount;

    private AtomicBoolean shouldSendSourceAddress;

    private AtomicInteger sequence;
    private AtomicInteger numFulfilledPackets;
    private AtomicInteger numRejectedPackets;

    /**
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
        final UnsignedLong originalAmountToSend
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
      this.deliveredAmount = new AtomicReference<>(UnsignedLong.ZERO);
      this.sequence = new AtomicInteger(1);

      this.numFulfilledPackets = new AtomicInteger(0);
      this.numRejectedPackets = new AtomicInteger(0);
    }

    /**
     * Send money in an individual stream.
     *
     * @return
     */
    public CompletableFuture<SendMoneyResult> send() {
      Objects.requireNonNull(sharedSecret);
      Objects.requireNonNull(destinationAddress);
      Objects.requireNonNull(originalAmountToSend);

      // Fire off requests until the congestion controller tells us to stop or we've sent the total amount
      final List<CompletableFuture<InterledgerResponsePacket>> allFutures = Lists.newArrayList();

      final AtomicInteger totalStreamPackets = new AtomicInteger(0);
      final Instant start = Instant.now();
      while (totalStreamPackets.getAndIncrement() >= 0) {
        if (UnsignedLong.ZERO.equals(amountLeftToSend.get())) {
          break;
        }

        // Determine the amount to send
        final UnsignedLong amountToSend = StreamUtils.min(amountLeftToSend.get(), congestionController.getMaxAmount());
        if (amountToSend.equals(UnsignedLong.ZERO)) {
          try {
            Thread.sleep(100);
          } catch (InterruptedException e) {
            throw new StreamClientException(e.getMessage(), e);
          }
          continue;
        }

        this.amountLeftToSend.getAndUpdate(sourceAmount -> sourceAmount.minus(amountToSend));

        // Load up the STREAM packet
        final long sequence = this.sequence.getAndIncrement();

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
            .sequence(sequence)
            .frames(frames)
            .build();

        // Create the ILP Prepare packet
        final byte[] streamPacketData = this.toEncrypted(sharedSecret, streamPacket);
        final InterledgerCondition executionCondition;
        try {
          executionCondition = generatedFulfillableFulfillment(sharedSecret, streamPacketData).getCondition();
        } catch (SignatureException e) {
          throw new StreamClientException(e.getMessage(), e);
        }

        final InterledgerPreparePacket preparePacket = InterledgerPreparePacket.builder()
            .destination(destinationAddress)
            .amount(amountToSend.bigIntegerValue())
            .executionCondition(executionCondition)
            .expiresAt(Instant.now().plusSeconds(30L))
            .data(streamPacketData)
            .build();

        // Send it!
        final CompletableFuture<InterledgerResponsePacket> sendMoneyFuture = CompletableFuture.supplyAsync(() -> {
          congestionController.prepare(amountToSend);
          return link.sendPacket(preparePacket);
        }, executorService).thenApply(responsePacket -> {
          // Executed in the same thread as above
          responsePacket.handle(
              (fulfillPacket -> {
                handleFulfill(sequence, amountToSend, fulfillPacket);
              }),
              (rejectPacket -> {
                handleReject(sequence, amountToSend, rejectPacket);
              })
          );
          return responsePacket;
        });

        allFutures.add(sendMoneyFuture);
      }

      final CompletableFuture[] allFuturesArray = allFutures.toArray(new CompletableFuture[0]);
      final CompletableFuture<Void> combinedFuture = CompletableFuture.allOf(allFuturesArray);

      // To track the duration...
      final AtomicReference<Duration> sendMoneyDuration = new AtomicReference<>();
      // All futures will run here using the Cached Executor service.
      return combinedFuture.whenComplete(($, error) -> {
        if (error != null) {
          logger.error("SendMoney Stream failed: " + error.getMessage(), error);
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

    /**
     * Convert a {@link StreamPacket} to bytes using the CodecContext and then encrypt it using the supplied {@code
     * sharedSecret}.
     *
     * @param streamPacket
     *
     * @return
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
        throw new StreamClientException(e.getMessage(), e);
      }
    }

    /**
     * Convert the encrypted bytes of a stream packet into a {@link StreamPacket} using the CodecContext and {@code
     * sharedSecret}.
     *
     * @param sharedSecret
     * @param encryptedStreamPacketBytes
     *
     * @return
     */
    @VisibleForTesting
    protected StreamPacket fromEncrypted(final byte[] sharedSecret, final byte[] encryptedStreamPacketBytes) {
      Objects.requireNonNull(sharedSecret);
      Objects.requireNonNull(encryptedStreamPacketBytes);

      final byte[] streamPacketBytes = this.streamEncryptionService.decrypt(sharedSecret, encryptedStreamPacketBytes);
      try {
        return streamCodecContext.read(StreamPacket.class, new ByteArrayInputStream(streamPacketBytes));
      } catch (IOException e) {
        throw new StreamClientException(e.getMessage(), e);
      }
    }

    @VisibleForTesting
    protected void handleFulfill(
        final long sequence,
        final UnsignedLong amount,
        final InterledgerFulfillPacket fulfillPacket
    ) {
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

    @VisibleForTesting
    protected void handleReject(
        final long sequence,
        final UnsignedLong amountToSend,
        final InterledgerRejectPacket rejectPacket
    ) {
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
          throw new StreamClientException(String.format("Packet was rejected. rejectPacket=%s", rejectPacket));
        }
      }
    }

    /**
     * @param sequence
     *
     * @return An {@link UnsignedLong} representing the amount delivered by this individual stream.
     */
    @VisibleForTesting
    protected CloseConnectionResult closeConnection(final long sequence) {

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
      try {
        executionCondition = generatedFulfillableFulfillment(sharedSecret, encryptedStreamPacket)
            .getCondition();
      } catch (SignatureException e) {
        throw new StreamClientException(e.getMessage(), e);
      }

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
          this.deliveredAmount, this.sequence.decrementAndGet(), -1 //TODO this.numRejectedPackets
      );

      return CloseConnectionResult.builder()
          .amountDelivered(this.deliveredAmount.get())
          .numFulfilledPackets(this.numFulfilledPackets.get())
          .numRejectPackets(this.numRejectedPackets.get())
          .build();
    }
  }
}
