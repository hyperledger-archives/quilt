package org.interledger.link;

import org.interledger.core.InterledgerFulfillPacket;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerRejectPacket;
import org.interledger.core.InterledgerResponsePacket;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Defines how to send data to the other side of a bilateral link (i.e., the other party operating a single account in
 * tandem with the operator of this sender).
 */
@FunctionalInterface
public interface LinkSender {

  /**
   * <p>Sends an ILPv4 request packet to a peer and returns the response packet (if one is returned).</p>
   *
   * <p>This method supports one of three responses, which can be handled by using either
   * {@link InterledgerResponsePacket#handle(Consumer, Consumer)} or {@link InterledgerResponsePacket#map(Function,
   * Function)}.
   * </p>
   *
   * <ul>
   *   <li>An instance of {@link InterledgerFulfillPacket}, which means the packet was fulfilled by the receiver.</li>
   *   <li>An instance of {@link InterledgerRejectPacket}, which means the packet was rejected by one of the nodes in
   *   the payment path.
   *   </li>
   * </ul>
   *
   * @param preparePacket An {@link InterledgerPreparePacket} to send to the remote peer.
   *
   * @return An {@link InterledgerResponsePacket}, which will be of concrete type {@link InterledgerFulfillPacket} or }
   *     } or {@link InterledgerRejectPacket}.
   */
  InterledgerResponsePacket sendPacket(InterledgerPreparePacket preparePacket);

}
