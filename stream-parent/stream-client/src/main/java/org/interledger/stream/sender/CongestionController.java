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
  void fulfill(final UnsignedLong prepareAmount);

  /**
   * Allows an external process to inform this controller that a packet has been rejected.
   *
   * @param prepareAmount A {@link UnsignedLong} indicating the amount that was prepared, yet ultiimately rejected.
   * @param rejectPacket  An {@link InterledgerRejectPacket} containing information about the rejection.
   */
  void reject(final UnsignedLong prepareAmount, final InterledgerRejectPacket rejectPacket);

  UnsignedLong getMaxAmount();

  CongestionState getCongestionState();

  Optional<UnsignedLong> getMaxPacketAmount();

  boolean hasInFlight();
}
