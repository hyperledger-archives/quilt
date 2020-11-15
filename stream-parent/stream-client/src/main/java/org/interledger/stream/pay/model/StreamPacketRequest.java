package org.interledger.stream.pay.model;

import com.google.common.primitives.UnsignedInteger;
import org.interledger.stream.frames.StreamFrame;

import com.google.common.primitives.UnsignedLong;
import org.immutables.value.Value.Default;
import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Modifiable;

import java.util.Collection;

@Immutable
@Modifiable
public interface StreamPacketRequest {

  /**
   * Sequence number of the STREAM packet.
   */
  UnsignedInteger sequence();

  /**
   * Amount to send in the ILP Prepare
   */
  UnsignedLong sourceAmount();

  /**
   * Minimum destination amount to tell the recipient ("prepare amount")
   */
  UnsignedLong minDestinationAmount();

  /**
   * Frames to load within the STREAM packet
   */
  Collection<StreamFrame> requestFrames();

  /**
   * Indicates whether the recipient should be allowed to fulfill this request, or if it should use a random condition.
   */
  boolean isFulfillable();

  /**
   * The {@link StreamConnection} that this filter can use to actually transmit a packet.
   *
   * @return The {@link StreamConnection} for this request.
   */
  StreamConnection streamConnection();

  @Default
  default SendState sendState() {
    return SendState.Ready;
  }
}
