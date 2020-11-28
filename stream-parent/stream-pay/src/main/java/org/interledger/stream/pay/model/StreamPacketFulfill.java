package org.interledger.stream.pay.model;

@Deprecated // TODO: Remove
//@Immutable
public interface StreamPacketFulfill {//extends StreamPacketReply {

//  static Builder builder() {
//    return ImmutableStreamPacketFulfill.builder();
//  }
//
//  @Override
//  @Derived
//  // TODO: Used?
//  default boolean isReject() {
//    return false;
//  }
//
//  // TODO: Used?
//  @Override
//  @Derived
//  default boolean isFulfill() {
//    return true;
//  }
//
//  /**
//   * The actual response from the Stream packet send operation.
//   *
//   * @return A {@link InterledgerResponsePacket}.
//   */
//  // TODO: Does this need to be declared here? Should work from parent class.
//  Optional<InterledgerFulfillPacket> interledgerResponsePacket();
//
//  @Default
//  default SendState sendState() {
//    return SendState.Ready;
//  }
//
//  @Value.Check
//  default void check() {
//    Preconditions.checkState(isReject() == !isFulfill(), "isReject must not be equal to isFulfill");
//  }
}
