package org.interledger.stream.sender;

import org.interledger.core.InterledgerRejectPacket;
import org.interledger.stream.sender.AimdCongestionController.CongestionState;

import com.google.common.primitives.UnsignedLong;

import java.util.Optional;

/**
 * Allows STREAM clients and servers to adjust payment amounts and frequency depending on various ILPv4 responses (i.e.,
 * adjusting the rate at which ILP packets are sent based on network throughput limits). Congestion is an ILPv4-layer
 * concern, and will inform stream and connection-level flow control (i.e., adjusting the rate at which money and data
 * are sent).
 */
public interface CongestionController {

  /**
   * Inform this controller that a packet has been prepared.
   *
   * @param amount A {@link UnsignedLong} representing the total packet amount that was prepared.
   */
  void prepare(UnsignedLong amount);

  /**
   * Allows an external process to inform this controller that a packet has been fulfilled.
   *
   * @param prepareAmount A {@link UnsignedLong} indicating the amount that was fulfilled.
   */
  void onFulfill(final UnsignedLong prepareAmount);

  /**
   * Allows an external process to inform this controller that a packet has been rejected.
   *
   * @param prepareAmount A {@link UnsignedLong} indicating the amount that was prepared, yet ultiimately rejected.
   * @param rejectPacket  An {@link InterledgerRejectPacket} containing information about the rejection. This packet is
   *                      generally received from a remote peer.
   */
  void onReject(final UnsignedLong prepareAmount, final InterledgerRejectPacket rejectPacket);

  /**
   * <p>Compute the maximum packet amount that should be used if a new ILPv4 packet is going to be sent as part of this
   * Stream. This value is used by the stream client to form the next prepare packet size, and fluctuates based upon
   * prior stream activity.</p>
   *
   * <p>This computation depends on a sub-computation called `amountLeftInWindow`, which is the difference between the
   * {@link #maxInFlight} and the current {@link #amountInFlight}. The {@code maxAmount} returned by this function is
   * then computed by taking the min of `amountLeftInWindow` and {@link #maxPacketAmount}.</p>
   *
   * @return An {@link UnsignedLong} representing the current max packet amount for packets in this stream.
   */
  // Consider using AmountLeftInWindow instead.
  @Deprecated
  UnsignedLong getMaxAmount();

  /**
   * <p>Returns the difference between the current 'maxInFlight' (the maximum amount of value that can be outstanding
   * at any given time) and the current 'amountInFlight' (the total amount of value that is currently outstanding),
   * which is known at the current "window."</p>
   *
   * @return An {@link UnsignedLong} representing the amoount of units available to be sent in the current window.
   */
  UnsignedLong getAmountLeftInWindow();

  CongestionState getCongestionState();

  Optional<UnsignedLong> getMaxPacketAmount();

  boolean hasInFlight();

  UnsignedLong getMaxInFlight();
}
