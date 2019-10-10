package org.interledger.link;

import org.interledger.link.events.LinkConnectionEventListener;

/**
 * <p>Defines a {@link Link} that holds connection state and possibly other stateful data. Because this type of {@link
 * Link} holds a connection, it cannot be load-balance in the typical fashion. For example, a Link with an underlying
 * Websocket connection cannot easily be transitioned and serviced by another ILP node instance in a different
 * runtime.</p>
 */
public interface StatefulLink<LS extends LinkSettings> extends Link<LS>, Connectable {

  /**
   * Add an event listener to this link.
   *
   * <p>Care should be taken when adding multiple listeners to ensure that they perform distinct operations, otherwise
   * duplicate functionality might be unintentionally introduced.</p>
   *
   * @param eventListener A {@link LinkConnectionEventListener} that can listen for and response to various types of
   *                      events emitted by this link.
   */
  void addLinkEventListener(LinkConnectionEventListener eventListener);

  /**
   * Removes an event listener from the collection of listeners registered with this link.
   *
   * @param eventListener A {@link LinkConnectionEventListener} representing the listener to remove.
   */
  void removeLinkEventListener(LinkConnectionEventListener eventListener);

}
