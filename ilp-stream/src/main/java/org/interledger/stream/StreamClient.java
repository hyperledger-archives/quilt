package org.interledger.stream;


import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerCondition;
import org.interledger.core.InterledgerPacketType;
import org.interledger.core.InterledgerResponsePacket;
import org.interledger.stream.congestion.AimdCongestionController;
import org.interledger.stream.congestion.CongestionController;
import org.interledger.stream.frames.ConnectionNewAddressFrame;
import org.interledger.stream.frames.StreamFrame;
import org.interledger.stream.frames.StreamMoneyFrame;

import com.google.common.collect.Lists;
import com.google.common.primitives.UnsignedLong;

import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class StreamClient implements StreamEndpoint {

  // TODO: Consider creating a module called ilp-ping and putting this in there in somehting like PingContstants.
  public static final InterledgerCondition PING_PROTOCOL_CONDITION =
      InterledgerCondition.of(Base64.getDecoder().decode("jAC8DGFPZPfh4AtZpXuvXFe2oRmpDVSvSJg2oT+bx34="));

  private final ConnectionManager connectionManager;

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

  private static class SendMoneyCallable extends CompletableFuture<InterledgerResponsePacket> {

  }

  /**
   * Encapsulates everything needed to send a particular amount of money using STREAM.
   */
  protected static class MoneySender {

    private final CongestionController congestionController;
    private final byte[] sharedSecret;

    private final InterledgerAddress sourceAddress;
    private final InterledgerAddress destinationAddress;
    private final boolean shouldSendSourceAccount;
    private AtomicLong sequence;
    private AtomicReference<UnsignedLong> sourceAmount;

    public MoneySender(
        final byte[] sharedSecret,
        final InterledgerAddress sourceAddress, final InterledgerAddress destinationAddress,
        final UnsignedLong sourceAmount,
        final boolean shouldSendSourceAccount
    ) {
      this(
          new AimdCongestionController(), sharedSecret, sourceAddress, destinationAddress, sourceAmount,
          shouldSendSourceAccount
      );
    }

    public MoneySender(
        final CongestionController congestionController,
        final byte[] sharedSecret,
        final InterledgerAddress sourceAddress, final InterledgerAddress destinationAddress,
        final UnsignedLong sourceAmount,
        boolean shouldSendSourceAccount) {
      this.sharedSecret = Objects.requireNonNull(sharedSecret);
      this.congestionController = Objects.requireNonNull(congestionController);
      this.sourceAddress = sourceAddress;
      this.destinationAddress = Objects.requireNonNull(destinationAddress);
      this.shouldSendSourceAccount = shouldSendSourceAccount;

      Objects.requireNonNull(sourceAmount);
      this.sourceAmount = new AtomicReference<>(sourceAmount);

      this.sequence = new AtomicLong(1L);
    }

    /**
     * Send money in an individual stream.
     *
     * @return
     */
    public InterledgerResponsePacket send() {
      Objects.requireNonNull(sharedSecret);
      Objects.requireNonNull(destinationAddress);
      Objects.requireNonNull(sourceAmount);

      // Fire off requests until the congestion controller tells us to stop or we've sent the total amount
      boolean sentPackets = false;

      while (true) {
        // Determine the amount to send
        final UnsignedLong amountToSend = StreamUtils.min(sourceAmount.get(), congestionController.getMaxAmount());
        if (amountToSend.equals(UnsignedLong.ZERO)) {
          break;
        }

        this.sourceAmount.getAndUpdate(sourceAmount -> sourceAmount.minus(amountToSend));

        // Load up the STREAM packet
        long sequence = this.sequence.incrementAndGet();

        final List<StreamFrame> frames = Lists.newArrayList();
        final StreamMoneyFrame streamMoneyFrame = StreamMoneyFrame.builder()
            .streamId(1L)
            .shares(1)
            .build();
        frames.add(streamMoneyFrame);

        if (this.shouldSendSourceAccount) {
          frames.add(ConnectionNewAddressFrame.builder()
              .sourceAddress(sourceAddress)
              .build()
          );
        }

        final StreamPacket streamPacket = StreamPacket.builder()
            .type(InterledgerPacketType.PREPARE)
            // TODO: enforce min exchange rate.
            .prepareAmount(0L)
            .sequence(sequence)
            .frames(frames)
            .build();

        // Create the ILP Prepare packet
        // TODO: CODEC
        //final byte[] data = stream_packet.into_encrypted( & self.shared_secret);

        // Send it!
        this.congestionController.prepare(amountToSend);

        // TODO: Send the Prepare packet on a Link.

        // response = link.sendPacket(prepare);
      }

      return null;
    }

  }


}
