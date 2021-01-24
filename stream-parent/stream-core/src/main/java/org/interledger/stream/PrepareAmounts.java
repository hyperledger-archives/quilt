package org.interledger.stream;

import org.interledger.core.Immutable;
import org.interledger.core.InterledgerPreparePacket;

import com.google.common.primitives.UnsignedLong;

/**
 * A container object that captures amounts used in auth, rollback, and commit operations inside of the Stream Sender.
 *
 * @deprecated Will be removed once StreamSender is removed.
 */
@Deprecated
@Immutable
public interface PrepareAmounts {

  static PrepareAmountsBuilder builder() {
    return new PrepareAmountsBuilder();
  }

  /**
   * Helper method to construct an instance of {@link PrepareAmounts} from a Prepare packet and an embedded Stream
   * Packet.
   *
   * @param preparePacket An {@link InterledgerPreparePacket} used in a Stream send operation.
   * @param streamPacket  A {@link StreamPacket} that was enclosed in {@code preparePacket}.
   *
   * @return A {@link PrepareAmounts} constructed from the supplied inputs.
   */
  static PrepareAmounts from(final InterledgerPreparePacket preparePacket, final StreamPacket streamPacket) {
    return PrepareAmounts.builder()
      .amountToSend(preparePacket.getAmount())
      .minimumAmountToAccept(streamPacket.prepareAmount())
      .build();
  }

  /**
   * The amount that should be sent in a prepare packet for purposes of STREAM.
   *
   * @return An {@link UnsignedLong} that represents the `amount` value in an ILPv4 Prepare packet when used by a STREAM
   *   sender.
   */
  UnsignedLong getAmountToSend();

  /**
   * The amount that should be put into a Stream Packet's amount field to indicate the minimum amount a receiver should
   * accept for a given Stream Packet payment.
   *
   * @return An {@link UnsignedLong} that represents the minimum `amount` that a Stream Receiver should accept in an
   *   individual Stream Packet.
   */
  UnsignedLong getMinimumAmountToAccept();

}
