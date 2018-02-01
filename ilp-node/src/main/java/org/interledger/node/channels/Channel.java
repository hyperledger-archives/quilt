package org.interledger.node.channels;

import org.interledger.core.InterledgerFulfillPacket;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerProtocolException;
import org.interledger.node.events.ChannelClosedEvent;
import org.interledger.node.events.ChannelErrorEvent;
import org.interledger.node.events.ChannelOpenedEvent;
import org.interledger.node.events.IncomingRequestEvent;
import org.interledger.node.events.IncomingTransferEvent;
import org.interledger.node.exceptions.ChannelNotOpenException;
import org.interledger.node.handlers.ChannelClosedEventHandler;
import org.interledger.node.handlers.ChannelErrorEventHandler;
import org.interledger.node.handlers.ChannelOpenedEventHandler;
import org.interledger.node.handlers.IncomingRequestHandler;
import org.interledger.node.handlers.IncomingTransferEventHandler;

import java.io.Closeable;
import java.util.concurrent.Future;

/**
 * Defines an abstraction that is meant to be plugged-in to an ILP node system in order
 * for it to communicate with a another node.
 *
 * <p>Based on IL-RFC-24, the general contract for a channels is that, for a single account, it can
 * send ILP packets and transfers from to the destination node all of which impact the an account
 * linked to this channel.
 *
 * <p>Additionally, channel plugins can also emit events received from the peer, such as incoming
 * ILP packets or transfers.
 *
 * <p>An ILP node will have a unique instance of a channels for each account.
 *
 * <p>The following high-level component diagram illustrates how ledger plugins are used by a
 * connector:
 *
 * @see "https://interledger.org/rfcs/0024-ledger-plugin-interface-2/"
 */
public interface Channel extends Closeable {

  /**
   * Called to open the channel.
   *
   * <p>This should initiate event subscriptions and establish a connection to the peer.
   *
   * <p>Once this method is called, the channels MUST attempt to subscribe to and report events,
   * including the "open" event immediately after a successful connection is established. If the
   * connection is lost, the ledger channels SHOULD emit the "closed" event.
   *
   */
  void open();

  /**
   * Called to disconnect this channel from the peer.
   *
   * @throws ChannelNotOpenException if the channels is not connected.
   *
   */
  void close();

  /**
   * Query whether the channel is currently open.
   *
   * @return {@code true} if the channel is open, {@code false} otherwise.
   */
  boolean isOpen();

  /**
   * Initiate a transfer of money to the peer.
   *
   * <p>The ILP node will reduce the balance on the account linked to this channels if this call
   * completes successfully.
   *
   * <p>Example: If this channels is used to make transfers over a payment channel then this should
   * result in a new claim being sent over the channel.
   *
   * @param amount The amount to credit the account in favour of the sender.
   *
   * @throws ChannelNotOpenException if the channel is not open.
   *
   * @return a Future that resolves when the transfer has been sent.
   */
  Future<Void> sendTransfer(long amount);

  /**
   * Sends an ILP request packet to the peer and returns the response packet.
   *
   * @param request The request to send to the peer.
   *
   * @throws ChannelNotOpenException if the channels is not connected.
   * @throws InterledgerProtocolException if the request is rejected by the peer.
   *
   * @return A Future that resolves to the ILP response from the peer.
   */
  Future<InterledgerFulfillPacket> sendRequest(InterledgerPreparePacket request);

  /**
   * Set the handler for {@link ChannelOpenedEvent} events.
   *
   * <p>Plugins MUST emit this event AFTER they have successfully established a connection with
   * their peer.
   *
   * <p>If a handler is already set then it will be replaced. Implementations MAY throw a
   * {@link RuntimeException} if the handler is already set.
   *
   *
   * @param eventHandler A {@link ChannelOpenedEventHandler} that handles
   * {@link ChannelOpenedEvent}s.
   *
   * @return this for chaining commands
   */
  Channel setOnOpened(ChannelOpenedEventHandler eventHandler);

  /**
   * Set the handler for {@link ChannelClosedEvent} events.
   *
   * <p>Plugins MUST emit this event AFTER they have successfully established a connection with
   * their peer.
   *
   * <p>If a handler is already set then it will be replaced. Implementations MAY throw a
   * {@link RuntimeException} if the handler is already set.
   *
   *
   * @param eventHandler A {@link ChannelClosedEvent} that handles
   * {@link ChannelClosedEvent}s.
   *
   * @return this for chaining commands
   */
  Channel setOnClosed(ChannelClosedEventHandler eventHandler);

  /**
   * Set the handler for {@link ChannelErrorEvent} events.
   *
   * <p>Plugins MUST emit this event when encountering any critical events. Nodes MUST consider
   * the channels unusable after this event and can attempt to gracefully close the channel.
   *
   * <p>If a handler is already set then it will be replaced. Implementations MAY throw a
   * {@link RuntimeException} if the handler is already set.
   *
   *
   * @param eventHandler A {@link ChannelErrorEventHandler} that handles
   * {@link ChannelErrorEvent}s.
   *
   * @return this for chaining commands
   */
  Channel setOnError(ChannelErrorEventHandler eventHandler);

  /**
   * Set the handler for {@link IncomingTransferEvent} events.
   *
   * <p>Plugins MUST emit this event when a peer has made a transfer into the channel's account.
   * This will notify the node that the tracked balance on the account can be updated.
   *
   * <p>If a handler is already set then it will be replaced. Implementations MAY throw a
   * {@link RuntimeException} if the handler is already set.
   *
   *
   * @param eventHandler A {@link IncomingTransferEventHandler} that handles
   * {@link IncomingTransferEvent}s.
   *
   * @return this for chaining commands
   */
  Channel setOnIncomingTransfer(IncomingTransferEventHandler eventHandler);

  /**
   * Set the handler for {@link IncomingRequestEvent} events.
   *
   * <p>Plugins MUST invoke this handler when a new ILP request comes in over the channel and then
   * forward the subsequent response back down the same channel.
   *
   * <p>If a handler is already set then it will be replaced. Implementations MAY throw a
   * {@link RuntimeException} if the handler is already set.
   *
   *
   * @param handler An {@link IncomingRequestHandler} that handles
   * {@link InterledgerPreparePacket}s and returns {@link InterledgerFulfillPacket}s.
   *
   * @return this for chaining commands
   */
  Channel setIncomingRequestHandler(IncomingRequestHandler handler);

}
