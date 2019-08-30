package org.interledger.stream;

import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerResponsePacket;
import org.interledger.stream.congestion.CongestionController;

import com.google.common.primitives.UnsignedLong;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class StreamClient implements StreamEndpoint {

  private final ConnectionManager connectionManager;

  public StreamClient() {
    this(new ConnectionManager());
  }

  public StreamClient(final ConnectionManager connectionManager) {
    this.connectionManager = Objects.requireNonNull(connectionManager);
  }

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

    while (true) {
      // Determine the amount to send
      amountToSend = StreamUtils.min(sourceAmount, congestionController.getMaxAmount());


    }


  }

  @Override
  public void sendData() {
    throw new RuntimeException("Not yet implemented!");
  }

  private static class SendMoneyCallable extends CompletableFuture<InterledgerResponsePacket> {

  }

  protected static class MoneySender {

    private final CongestionController congestionController;

    public MoneySender(final CongestionController congestionController) {
      this.congestionController = Objects.requireNonNull(congestionController);
    }

    /**
     * Send money in an individual stream.
     *
     * @param sharedSecret
     * @param destinationAddress
     * @param amount
     *
     * @return
     */
    public InterledgerResponsePacket sendMoney(
        final byte[] sharedSecret,
        final InterledgerAddress destinationAddress,
        final UnsignedLong amount
    ) {
      Objects.requireNonNull(destinationAddress);
      Objects.requireNonNull(amount);

      // Fire off requests until the congestion controller tells us to stop or we've sent the total amount
      boolean sentPackets = false;

      while (true) {
        // Determine the amount to send
        amountToSend = StreamUtils.min(sourceAmount, congestionController.get());


      }


    }

  }


}
