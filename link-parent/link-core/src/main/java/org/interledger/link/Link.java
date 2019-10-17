package org.interledger.link;

import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerResponsePacket;
import org.interledger.link.exceptions.LinkHandlerAlreadyRegisteredException;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * <p>An abstraction for communicating with a remote Interledger peer in a connection-state-less manner, meaning links
 * do not hold connection state during their lifecycle.</p>
 *
 * <p>If you want to make a {@link Link} that can holds connection state (such as for a link protocol using
 * Websockets), consider {@link StatefulLink} instead.</p>
 */
public interface Link<L extends LinkSettings> extends LinkSender {

  InterledgerAddress SELF = InterledgerAddress.of("self.node");

  /**
   * <p>A unique identifier for this {@link Link} so that the same link can be referenced across requests.</p>
   *
   * <p>Note that this value is non-optional because even though an account operating on a Link may lazily load its
   * Interledger address, a Link will always have an Id because it always corresponds to a single account, which always
   * has an accountId.</p>
   *
   * @return A {@link LinkId}.
   */
  LinkId getLinkId();

  /**
   * Allows the linkId to be set after construction. This is important because often times a LinkId is not known until
   * after it is constructed (i.e., it cannot be discerned from {@link LinkSettings}).
   *
   * @param linkId The {@link LinkId} to set on this Link.
   */
  void setLinkId(LinkId linkId);

  /**
   * A supplier for the ILP address of this node operating this Link. This value may be uninitialized, for example, in
   * cases where the Link obtains its address from a parent node using IL-DCP. If an ILP address has not been assigned,
   * or it has not been obtained via IL-DCP, then this value will by default be {@link Link#SELF}.
   *
   * @return A {@link Supplier} of the {@link InterledgerAddress} that represents the address of the node operating this
   *     Link.
   */
  default Supplier<InterledgerAddress> getOperatorAddressSupplier() {
    return () -> SELF;
  }

  /**
   * The settings for this Link.
   *
   * @return An instance of {@code L}.
   */
  L getLinkSettings();

  /**
   * <p>Set the callback which is used to handle incoming prepared data packets. The handler should expect one
   * parameter (an ILP Prepare Packet) and return a {@link InterledgerResponsePacket} as a response. If an error occurs,
   * the callback MAY throw an exception. In general, however, the callback should behave as {@link
   * Link#sendPacket(InterledgerPreparePacket)} does.</p>
   *
   * <p>If a data handler is already set, this method throws a {@link LinkHandlerAlreadyRegisteredException}. In order
   * to change the data handler, the old handler must first be removed via {@link #unregisterLinkHandler()}. This is to
   * ensure that handlers are not overwritten by accident.</p>
   *
   * <p>If an incoming packet is received by the link, but no handler is registered, the link SHOULD respond with
   * an error.</p>
   *
   * @param dataHandler An instance of {@link LinkHandler}.
   */
  void registerLinkHandler(LinkHandler dataHandler) throws LinkHandlerAlreadyRegisteredException;

  /**
   * Accessor for the currently registered (though optionally-present) {@link LinkHandler}.
   *
   * @return An optionally-present {@link LinkHandler}.
   */
  Optional<LinkHandler> getLinkHandler();

  /**
   * Accessor for the currently registered (though optionally-present) {@link LinkHandler}.
   *
   * @return The currently registered {@link LinkHandler}.
   *
   * @throws RuntimeException if no handler is registered (A Link is not in a valid state until it has handlers
   *                          registered)
   */
  default LinkHandler safeGetLinkHandler() {
    return this.getLinkHandler()
        .orElseThrow(() -> new IllegalStateException("You MUST register a LinkHandler before using this link!"));
  }

  /**
   * Removes the currently used {@link LinkHandler}. This has the same effect as if {@link
   * #registerLinkHandler(LinkHandler)} had never been called. If no link handler is currently set, this method does
   * nothing.
   */
  void unregisterLinkHandler();

  /**
   * <p>Check the connection's connectivity. to see  for ping by making an HTTP Head request with a ping packet, and
   * asserting the values returned are one of the supported content-types required for BLAST.</p>
   *
   * <p>If the endpoint does not support producing BLAST responses, we expect a 406 NOT_ACCEPTABLE response. If the
   * endpoint does not support BLAST requests, then we expect a 415 UNSUPPORTED_MEDIA_TYPE.</p>
   */
  default void testConnection() {
  }
}
