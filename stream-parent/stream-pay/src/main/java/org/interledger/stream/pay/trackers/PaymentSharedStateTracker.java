package org.interledger.stream.pay.trackers;

import org.interledger.stream.connection.StreamConnection;

import java.util.Objects;

/**
 * Holds shared state for a Stream Payment across many packetized requests.
 */
public class PaymentSharedStateTracker {

  private final StreamConnection streamConnection;
  private final ExchangeRateTracker exchangeRateTracker;
  private final AssetDetailsTracker assetDetailsTracker;
  private final MaxPacketAmountTracker maxPacketAmountTracker;
  private final AmountTracker amountTracker;
  private final PacingTracker pacingTracker;

  /**
   * Required-args constructor.
   *
   * @param streamConnection A {@link StreamConnection} to track data for.s
   */
  public PaymentSharedStateTracker(final StreamConnection streamConnection) {
    this.streamConnection = Objects.requireNonNull(streamConnection);
    this.maxPacketAmountTracker = new MaxPacketAmountTracker();
    this.exchangeRateTracker = new ExchangeRateTracker();
    this.assetDetailsTracker = new AssetDetailsTracker(
      streamConnection.getSourceAccountDetails(), streamConnection.getDestinationAddress()
    );
    this.amountTracker = new AmountTracker(exchangeRateTracker);
    this.pacingTracker = new PacingTracker();
  }

  public StreamConnection getStreamConnection() {
    return streamConnection;
  }

  public ExchangeRateTracker getExchangeRateTracker() {
    return exchangeRateTracker;
  }

  public AssetDetailsTracker getAssetDetailsTracker() {
    return assetDetailsTracker;
  }

  public MaxPacketAmountTracker getMaxPacketAmountTracker() {
    return maxPacketAmountTracker;
  }

  public AmountTracker getAmountTracker() {
    return amountTracker;
  }

  public PacingTracker getPacingTracker() {
    return pacingTracker;
  }
}
