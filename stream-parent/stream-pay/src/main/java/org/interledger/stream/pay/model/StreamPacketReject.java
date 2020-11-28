package org.interledger.stream.pay.model;

@Deprecated // TODO: Remove
//@Immutable
public interface StreamPacketReject {//extends StreamPacketReply {

//  static Builder builder() {
//    return ImmutableStreamPacketReject.builder();
//  }
//
//  @Override
//  @Derived
//  // TODO: Used?
//  default boolean isReject() {
//    return true;
//  }
//
//  // TODO: Used?
//  @Override
//  @Derived
//  default boolean isFulfill() {
//    return false;
//  }
//
//  /**
//   * The actual response from the Stream packet send operation.
//   *
//   * @return A {@link InterledgerResponsePacket}.
//   */
//  // TODO: Does this need to be declared here? Should work from parent class.
//  Optional<InterledgerRejectPacket> interledgerResponsePacket();
//
//  @Value.Check
//  default void check() {
//    Preconditions.checkState(isReject() == !isFulfill(), "isReject must not be equal to isFulfill");
//
//    // TODO: if InterledgerRejectPacket works, then remove this. Otherwise, enforce that only a reject packet can be assigned to this class.
////    Preconditions.checkState(
////        this.interledgerResponsePacket()
////        .filter(interledgerRejectPacket -> )
////        .map(Optional::get)
////    );
//  }
//
////  @Derived
////  default Optional<InterledgerErrorCode> getCode() {
////    return this.interledgerResponsePacket()
////        .map(InterledgerResponsePacket::typedData)
////        .filter(Optional::isPresent)
////        .map(Optional::get)
////        .map($ -> (InterledgerRejectPacket) $)
////        .map(InterledgerRejectPacket::getCode);
////  }
//
//  @Default
//  default SendState sendState() {
//    return SendState.Ready;
//  }
}
