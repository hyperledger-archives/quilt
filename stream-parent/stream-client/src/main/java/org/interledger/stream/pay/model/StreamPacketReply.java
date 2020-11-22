package org.interledger.stream.pay.model;

import com.google.common.primitives.UnsignedLong;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import org.immutables.value.Value.Derived;
import org.immutables.value.Value.Immutable;
import org.interledger.core.InterledgerFulfillPacket;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerRejectPacket;
import org.interledger.core.InterledgerResponsePacket;
import org.interledger.stream.StreamPacket;
import org.interledger.stream.frames.StreamFrame;
import org.interledger.stream.pay.model.ImmutableStreamPacketReply.Builder;

/**
 * Holds all information concerning a reply for a stream request.
 */
@Immutable
public interface StreamPacketReply {

  static Builder builder() {
    return ImmutableStreamPacketReply.builder();
  }

  /**
   * Amount the recipient claimed to receive. Omitted if no authentic STREAM reply.
   *
   * @deprecated Move to StreamPacketUtils? Upside is it's all in one place. Downside is it's potentially computed more
   * than once per-packet. Consider maybe just moving this to the StreamPacket class? Once a StreamPacket is created,
   * then calling any of these functions would only be done once.
   */
  @Deprecated
  @Derived
  default Optional<UnsignedLong> destinationAmountClaimed() {
    return interledgerResponsePacket()
      .map(InterledgerResponsePacket::typedData)
      .filter(Optional::isPresent)
      .map(Optional::get)
      .filter(typedData -> StreamPacket.class.isAssignableFrom(typedData.getClass()))
      .map($ -> (StreamPacket) $)
      .map(StreamPacket::prepareAmount);
  }

  /**
   * Parsed frames from the STREAM response packet. Empty if no authentic STREAM reply.
   *
   * @deprecated Move to StreamPacketUtils? Upside is it's all in one place. Downside is it's potentially computed more
   * than once per-packet. Consider maybe just moving this to the StreamPacket class? Once a StreamPacket is created,
   * then calling any of these functions would only be done once.
   */
  @Deprecated
  default Collection<StreamFrame> frames() {
    return interledgerResponsePacket()
      .map(InterledgerResponsePacket::typedData)
      .filter(Optional::isPresent)
      .map(Optional::get)
      .filter(typedData -> StreamPacket.class.isAssignableFrom(typedData.getClass()))
      .map($ -> (StreamPacket) $)
      .map(StreamPacket::frames)
      .orElseGet(Collections::emptyList);
  }

  /**
   * The prepare packet the prompted this stream packet reply.
   *
   * @return
   */
  Optional<InterledgerPreparePacket> interledgerPreparePacket();

  /**
   * The actual response from the Stream packet send operation; empty if there was no response or if this reply should
   * be ignored.
   *
   * @return A {@link InterledgerResponsePacket}.
   */
  Optional<InterledgerResponsePacket> interledgerResponsePacket();

//  // TODO: Extract PaymentErrors out of StreamPacketReply's SendState on ground that we should throw an exception
//  // instead.
//  @Default
//  default SendState sendState() {
//    return SendState.Ready;
//  }

  /**
   * Contains STREAM frames that should be sent to the receiver if {@link #sendState()} is {@link SendState#End}.
   *
   * @return A {@link Set} of {@link StreamFrame} for sending on a connection-close operation.
   */
  Set<StreamFrame> streamFramesForConnectionClose();

  /**
   * Did the recipient authenticate that they received the STREAM request packet? If they responded with a Fulfill or a
   * valid STREAM reply, they necessarily decoded the request.
   */
  // TODO: Compare this with StreamPacketUtils.isAuthentic(interledgerRejectPacket) and maybe just implement here?
  //  Check usages.
  @Derived
  default boolean isAuthentic() {
    // Or was there a STREAM reply.
    boolean hasStreamPacket = interledgerResponsePacket()
      .map(InterledgerResponsePacket::typedData)
      .filter(Optional::isPresent)
      .map(Optional::get)
      .filter(typedData -> StreamPacket.class.isAssignableFrom(typedData.getClass()))
      // TODO: Should there be frames here to be authentic?
      .map($ -> true)
      .orElse(false);

    return isFulfill() || hasStreamPacket;
  }

  @Derived
  default boolean isReject() {
    return this.interledgerResponsePacket()
      .filter(packet -> InterledgerRejectPacket.class.isAssignableFrom(packet.getClass()))
      .isPresent();
  }

  @Derived
  default boolean isFulfill() {
    return this.interledgerResponsePacket()
      .filter(packet -> InterledgerFulfillPacket.class.isAssignableFrom(packet.getClass()))
      .isPresent();
  }

  /**
   * Handle this response packet using one of the two supplied functions, depending on this packet's actual type. If
   * this packet is a fulfill packet, then {@code fulfillHandler} will be called. If this packet is a reject packet,
   * then {@code rejectHandler} will be called instead.
   *
   * @param fulfillHandler A {@link Consumer} to call if this packet is an instance of {@link
   *                       InterledgerFulfillPacket}.
   * @param rejectHandler  A {@link Consumer} to call if this packet is an instance of {@link InterledgerRejectPacket}.
   */
  default void handle(
    final Consumer<StreamPacketReply> fulfillHandler, final Consumer<StreamPacketReply> rejectHandler
  ) {
    Objects.requireNonNull(fulfillHandler);
    Objects.requireNonNull(rejectHandler);

    this.interledgerResponsePacket()
      .ifPresent(interledgerResponsePacket -> interledgerResponsePacket.handle(
        (fulfillPacket) -> {
          fulfillHandler.accept(this);
        },
        (rejectPacket) -> {
          rejectHandler.accept(this);
        }
      ));
  }

  /**
   * <p>Handle this response packet using one of the two supplied functions, depending on this packet's actual type. If
   * this packet is a fulfill packet, then {@code fulfillHandler} will be called. If this packet is a reject packet,
   * then {@code rejectHandler} will be called instead.</p>
   *
   * <p>This variant allows for a more fluent style due to returning this object, but is otherwise equivalent to {@link
   * #handle(Consumer, Consumer)}.</p>
   *
   * @param fulfillHandler A {@link Consumer} to call if this packet is an instance of {@link
   *                       InterledgerFulfillPacket}.
   * @param rejectHandler  A {@link Consumer} to call if this packet is an instance of {@link InterledgerRejectPacket}.
   * @return This instance of {@link InterledgerResponsePacket}.
   */
  default StreamPacketReply handleAndReturn(
    final Consumer<StreamPacketReply> fulfillHandler, final Consumer<StreamPacketReply> rejectHandler
  ) {
    this.handle(fulfillHandler, rejectHandler);
    return this;
  }

  /**
   * Map this packet to another class using one of the two supplied functions, depending on the actual type of this
   * response packet. If this packet is a fulfill packet, then {@code fulfillMapper} will be called. If this packet is a
   * reject packet, then  {@code rejectMapper} will be called instead.
   *
   * @param fulfillMapper A {@link Function} to call if this packet is an instance of {@link StreamPacketFulfill}.
   * @param rejectMapper  A {@link Function} to call if this packet is an instance of {@link StreamPacketReject}.
   * @param <R>           The return type of this mapping function.
   * @return An instance of {@code R}.
   */
  default <R> Optional<R> map(
    final Function<StreamPacketReply, R> fulfillMapper, final Function<StreamPacketReply, R> rejectMapper
  ) {
    Objects.requireNonNull(fulfillMapper);
    Objects.requireNonNull(rejectMapper);

    return this.interledgerResponsePacket()
      .map(interledgerResponsePacket -> {
        if (InterledgerFulfillPacket.class.isAssignableFrom(interledgerResponsePacket.getClass())) {
          return fulfillMapper.apply(this);
        } else if (InterledgerRejectPacket.class.isAssignableFrom(interledgerResponsePacket.getClass())) {
          return rejectMapper.apply(this);
        } else {
          throw new RuntimeException(String.format("Unsupported StreamReply Type: %s", this.getClass()));
        }
      });
  }

}
