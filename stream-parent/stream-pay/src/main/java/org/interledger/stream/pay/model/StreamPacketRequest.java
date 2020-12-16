package org.interledger.stream.pay.model;

import org.interledger.core.InterledgerPreparePacket;
import org.interledger.stream.frames.ErrorCode;
import org.interledger.stream.frames.ErrorCodes;
import org.interledger.stream.frames.StreamFrame;
import org.interledger.stream.pay.model.ImmutableStreamPacketRequest.Builder;

import com.google.common.primitives.UnsignedInteger;
import com.google.common.primitives.UnsignedLong;
import org.immutables.value.Value.Default;
import org.immutables.value.Value.Derived;
import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Modifiable;

import java.util.List;
import java.util.Optional;

/**
 * A request containing everything needed to send the next STREAM packet on an ILPv4 link. This request is mutable
 * because it is adjusted via the filter-chain before sending.
 */
@Immutable
@Modifiable
public interface StreamPacketRequest {

  static Builder builder() {
    return ImmutableStreamPacketRequest.builder();
  }

  /**
   * The prepare packet that prompted this stream packet reply.
   *
   * @return
   */
  Optional<InterledgerPreparePacket> interledgerPreparePacket();

  /**
   * Sequence number of the STREAM packet.
   */
  @Default
  default UnsignedInteger sequence() {
    return UnsignedInteger.ZERO;
  }

  /**
   * Amount to send in the ILP Prepare
   */
  @Default
  default UnsignedLong sourceAmount() {
    return UnsignedLong.ZERO;
  }

  /**
   * Minimum destination amount to tell the recipient ("prepare amount")
   */
  @Default
  default UnsignedLong minDestinationAmount() {
    return UnsignedLong.ZERO;
  }

  /**
   * Frames to load within the STREAM packet.
   */
  List<StreamFrame> requestFrames();

  /**
   * Indicates whether the recipient should be allowed to fulfill this request, or if it should use a random condition.
   */
  @Default
  default boolean isFulfillable() {
    return true;
  }

  @Default
  default SendState sendState() {
    return SendState.Ready;
  }

  /**
   * Indicates whether or not the Stream Connection should be closed.
   *
   * @return
   */
  @Derived
  default boolean shouldConnectionCloseBeSent() {
    return streamErrorCodeForConnectionClose() != ErrorCodes.NoError;
  }

  /**
   * The {@link ErrorCode} to send when closing the Stream.
   *
   * @return A {@link ErrorCode}.
   */
  @Default
  default ErrorCode streamErrorCodeForConnectionClose() {
    return ErrorCodes.NoError;
  }
}
