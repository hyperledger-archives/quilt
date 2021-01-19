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
  private final StatisticsTracker statisticsTracker;

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
    this.statisticsTracker = new StatisticsTracker();
  }

  public StreamConnection getStreamConnection() {
    return this.streamConnection;
  }

  public ExchangeRateTracker getExchangeRateTracker() {
    return this.exchangeRateTracker;
  }

  public AssetDetailsTracker getAssetDetailsTracker() {
    return this.assetDetailsTracker;
  }

  public MaxPacketAmountTracker getMaxPacketAmountTracker() {
    return this.maxPacketAmountTracker;
  }

  public AmountTracker getAmountTracker() {
    return this.amountTracker;
  }

  public PacingTracker getPacingTracker() {
    return this.pacingTracker;
  }

  public StatisticsTracker getStatisticsTracker() {
    return this.statisticsTracker;
  }
}
