package org.interledger.stream;

import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerCondition;
import org.interledger.core.InterledgerFulfillPacket;
import org.interledger.core.InterledgerPacketType;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerResponsePacket;
import org.interledger.encoding.asn.framework.CodecContext;
import org.interledger.link.Link;
import org.interledger.stream.congestion.CongestionController;
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
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class StreamClient implements StreamEndpoint {

  // TODO: Executor executor = Executors.newFixedThreadPool(10);
  // Maybe here: https://www.callicoder.com/java-8-completablefuture-tutorial/

  private final ConnectionManager connectionManager;

  // TODO: add logging back in from Rust!
  private Logger logger = LoggerFactory.getLogger(this.getClass());

  public StreamClient() {
    this(new ConnectionManager());
  }

  public StreamClient(final ConnectionManager connectionManager) {
    this.connectionManager = Objects.requireNonNull(connectionManager);
  }

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
  public InterledgerResponsePacket sendMoney(
      final byte[] sharedSecret,
      final InterledgerAddress destinationAddress,
      final UnsignedLong amount
  ) {
    Objects.requireNonNull(destinationAddress);
    Objects.requireNonNull(amount);

    // Fire off requests until the congestion controller tells us to stop or we've sent the total amount
    boolean sentPackets = false;

//    while (true) {
//      // Determine the amount to send
//      amountToSend = StreamUtils.min(sourceAmount, congestionController.getMaxAmount());
//
//
//    }

    return null;
  }

  @Override
  public void sendData() {
    throw new RuntimeException("Not yet implemented!");
  }

  /**
   * Encapsulates everything needed to send a particular amount of money by breaking up a payment into a bunch of
   * smaller packets, and then handling all responses. This aggregator operates on a single Connection by opening and
   * closing a single stream.
   */
  protected static class SendMoneyAggregator {

    // TODO: Consider extracing the Open/Close Connection logic so that multiple payments can be made on the same Connection.
    // This is not strictly useful in this design, but might be in the future.

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final ExecutorService executorService;

    private final CodecContext streamCodecContext;
    private final StreamEncryptionService streamEncryptionService;
    private final CongestionController congestionController;
    private final byte[] sharedSecret;

    private final InterledgerAddress sourceAddress;
    private final InterledgerAddress destinationAddress;
    private final UnsignedLong originalSourceAmount;

    private final Link link;
    private AtomicBoolean shouldSendSourceAddress;

    private AtomicReference<UnsignedLong> sourceAmount;
    private AtomicReference<UnsignedLong> deliveredAmount;
    private AtomicLong sequence;

//    public SendMoneyAggregator(
//        final Link link, final byte[] sharedSecret,
//        final InterledgerAddress sourceAddress, final InterledgerAddress destinationAddress,
//        final UnsignedLong sourceAmount,
//        final boolean shouldSendSourceAccount,
//        final StreamEncryptionService streamEncryptionService
//    ) {
//      this(
//          link, streamEncryptionService,
//          sharedSecret, sourceAddress, destinationAddress, sourceAmount, shouldSendSourceAccount,
//          new AimdCongestionController()
//      );
//    }

    public SendMoneyAggregator(
        final CodecContext streamCodecContext,
        final Link link,
        final StreamEncryptionService streamEncryptionService,
        final byte[] sharedSecret,
        final InterledgerAddress sourceAddress,
        final InterledgerAddress destinationAddress,
        final UnsignedLong sourceAmount,
        final CongestionController congestionController,
        final AtomicBoolean shouldSendSourceAddress
    ) {
      this.streamCodecContext = Objects.requireNonNull(streamCodecContext);
      this.link = Objects.requireNonNull(link);
      this.streamEncryptionService = Objects.requireNonNull(streamEncryptionService);
      this.sharedSecret = Objects.requireNonNull(sharedSecret);
      this.sourceAddress = sourceAddress;
      this.destinationAddress = Objects.requireNonNull(destinationAddress);
      this.originalSourceAmount = Objects.requireNonNull(sourceAmount);
      this.congestionController = Objects.requireNonNull(congestionController);
      this.shouldSendSourceAddress = shouldSendSourceAddress;

      this.sourceAmount = new AtomicReference<>(originalSourceAmount);
      this.deliveredAmount = new AtomicReference<>(UnsignedLong.ZERO);
      this.sequence = new AtomicLong(1L);

      // Note that pools with similar properties but different details (for example, timeout parameters) may be
      // created using {@link ThreadPoolExecutor} constructors.
      this.executorService = Executors.newCachedThreadPool();
    }

    /**
     * Send money in an individual stream.
     *
     * @return
     */
    public CompletableFuture<InterledgerResponsePacket> send() {
      Objects.requireNonNull(sharedSecret);
      Objects.requireNonNull(destinationAddress);
      Objects.requireNonNull(sourceAmount);

      // Fire off requests until the congestion controller tells us to stop or we've sent the total amount
      boolean sentPackets = false;

      final List<CompletableFuture<InterledgerResponsePacket>> allFutures = Lists.newArrayList();

      while (true) {
        // Determine the amount to send
        final UnsignedLong amountToSend = StreamUtils.min(sourceAmount.get(), congestionController.getMaxAmount());
        if (amountToSend.equals(UnsignedLong.ZERO)) {
          break;
        }

        this.sourceAmount.getAndUpdate(sourceAmount -> sourceAmount.minus(amountToSend));

        // Load up the STREAM packet
        final long sequence = this.sequence.incrementAndGet();

        final List<StreamFrame> frames = Lists.newArrayList();
        final StreamMoneyFrame streamMoneyFrame = StreamMoneyFrame.builder()
            .streamId(UnsignedLong.ONE)
            .shares(UnsignedLong.ONE)
            .build();
        frames.add(streamMoneyFrame);

        if (this.shouldSendSourceAddress.get()) {
          frames.add(ConnectionNewAddressFrame.builder()
              .sourceAddress(sourceAddress)
              .build()
          );
        }

        final StreamPacket streamPacket = StreamPacket.builder()
            .interledgerPacketType(InterledgerPacketType.PREPARE)
            // TODO: enforce min exchange rate.
            .prepareAmount(UnsignedLong.ZERO)
            .sequence(sequence)
            .frames(frames)
            .build();

        // Create the ILP Prepare packet
        final byte[] streamPacketData = this.toEncrypted(sharedSecret, streamPacket);
        final InterledgerCondition executionCondition;
        try {
          executionCondition = StreamUtils.generatedFulfillableFulfillment(sharedSecret, streamPacketData)
              .getCondition();
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
        CompletableFuture<Void> future = CompletableFuture.supplyAsync(() -> {
          congestionController.prepare(amountToSend);
          return link.sendPacket(preparePacket);
        }).thenApply(responsePacket -> {
          // Executed in the same thread as above
          responsePacket.handle(
              (fulfillPacket -> {
                handleFulfill(sequence, amountToSend, fulfillPacket);
              }),
              (rejectPacket -> {
                handleReject();
              })
          );
          return null;
        });


      }

      CompletableFuture[] allFuturesArray = allFutures.toArray(new CompletableFuture[0]);
      CompletableFuture<Void> combinedFuture = CompletableFuture.allOf(allFuturesArray);

      try {
        combinedFuture.join();
      } catch (Exception e) {
        throw new StreamClientException(e.getMessage(), e);
      }

//      String combined = Stream.of(allFuturesArray)
//          .map(sendPacketFuture -> {
//
//          })
      //.collect(Collectors.joining(" "));

      return null;
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
          sequence, amount, this.sourceAmount
      );
    }

    @VisibleForTesting
    protected void handleReject() {
      // TODO: Implement this after F08 and STREAM Codecs.
      throw new RuntimeException("FIXME!");
    }

    /**
     * @param sequence
     *
     * @return An {@link UnsignedLong} representing the amount delivered by this individual stream.
     */
    @VisibleForTesting
    protected UnsignedLong closeConnection(
        final long sequence
    ) {

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
        executionCondition = StreamUtils.generatedFulfillableFulfillment(sharedSecret, encryptedStreamPacket)
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
            handleFulfill(sequence, UnsignedLong.ZERO, fulfillPacket);
          }),
          (rejectPacket -> {
            handleReject();
          })
      );

      // TODO: numRejectedPackets should be tracked by handleReject.
      logger.debug(
          "Send money future finished. Delivered: {} ({} packets fulfilled, {} packets rejected)",
          this.deliveredAmount, this.sequence.decrementAndGet(), -1 //this.numRejectedPackets
      );
      return this.deliveredAmount.get();
    }
  }

}
