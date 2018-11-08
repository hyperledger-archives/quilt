package org.interledger.btp;

/**
 * A helper class for mapping an instance {@link BtpPacket} according to the actual polymorphic type of the passed-in
 * instantiated object.
 *
 * @param <T> The response type to emit after mapping a particular packet.
 */
public abstract class BtpPacketMapper<T> {

  /**
   * Handle the supplied {@code BtpPacket} in a type-safe manner.
   *
   * @param btpPacket The generic {@link BtpPacket} to be mapped in a type-safe manner.
   *
   * @return An instance of {@link T}.
   */
  public final T map(final BtpPacket btpPacket) {

    switch (btpPacket.getType()) {
      case MESSAGE: {
        return this.mapBtpMessage((BtpMessage) btpPacket);
      }
      case ERROR: {
        return this.mapBtpError((BtpError) btpPacket);
      }
      case RESPONSE: {
        return this.mapBtpResponse((BtpResponse) btpPacket);
      }
      case TRANSFER: {
        return this.mapBtpTransfer((BtpTransfer) btpPacket);
      }
      default: {
        throw new RuntimeException(String.format("Unsupported BtpPacket Type: %s", btpPacket.getType()));
      }
    }
  }

  /**
   * Handle the packet as a {@link BtpMessage}.
   *
   * @param btpMessage The generic {@link BtpMessage} to be mapped in a type-safe manner.
   *
   * @return An instance of {@link T}.
   */
  protected abstract T mapBtpMessage(final BtpMessage btpMessage);

  /**
   * Handle the packet as a {@link BtpMessage}.
   *
   * @param btpTransfer The generic {@link BtpTransfer} to be mapped in a type-safe manner.
   *
   * @return An instance of {@link T}.
   */
  protected abstract T mapBtpTransfer(final BtpTransfer btpTransfer);

  /**
   * Handle the packet as a {@link BtpError}.
   *
   * @param btpError The generic {@link BtpError} to be mapped in a type-safe manner.
   *
   * @return An instance of {@link T}.
   */
  protected abstract T mapBtpError(final BtpError btpError);

  /**
   * Handle the packet as a {@link BtpResponse}.
   *
   * @param btpResponse The generic {@link BtpResponse} to be mapped in a type-safe manner.
   *
   * @return An instance of {@link T}.
   */
  protected abstract T mapBtpResponse(final BtpResponse btpResponse);

}
