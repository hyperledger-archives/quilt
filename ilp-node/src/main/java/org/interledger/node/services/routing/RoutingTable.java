package org.interledger.node.services.routing;

import org.interledger.core.InterledgerAddress;
import org.interledger.node.Account;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * Defines a lookup-table of Interledger Address routes that can be used by any application to determine which target
 * account should be sent to as the "next-hop" in a multi-hop payment.
 * <p>
 * The following is an example routing table:
 * <p>
 * <pre>
 *  | targetPrefix | nextHopLedgerAccount | sourceAccountFilter|
 *  |==============|======================|====================|
 *  | g.eur.bank.  | Bitstamp             | Bitso              |
 *  | g.eur.       | Coinone              | [absent]           |
 *  | g.  	       | Alice                | [absent]           |
 * </pre>
 * <p>
 * TODO Review this
 * <p>
 * In the above example, the route returned for a final-destination address of "g.eur.bank.bob" would be
 * "Bitstamp"; the route returned for a final-destination address of "g.eur.bob" would be
 * "g.usd.bank2.connector"; Finally, the above table has a global catch-all route for the "g." prefix, which will return
 * a "next-hop" ledger of "g.ledger." for any routing requests that don't match any other prefix in the table.
 * <p>
 * Using this data, a Connector would, for example, be able to assemble an outgoing-payment to "g.usd.bank.connector"
 * for a payment with a final destination of "g.eur.bank.bob". This allows the ILP node using this table to forward a
 * payment for "bob" to the next hop in an overall path, without holding the entire graph of all ILP nodes in-memory.
 * <p>
 * This interface is extensible in that it can hold simple routes of type {@link R}, or it can hold more complicated
 * implementations that extend {@link Route}.
 */
public interface RoutingTable<R extends Route> {

  /**
   * Add a route to this routing table. If the route already exists (keyed by {@link R#getTargetPrefix()} and {@link
   * R#getSourceAccount()}), then this operation is a no-op.
   *
   * @param route
   */
  boolean addRoute(R route);

  /**
   * Remove a particular route from the routing table, based upon {@link R#getTargetPrefix()} and any other data inside
   * of the supplied {@code route}.
   *
   * @param route
   */
  boolean removeRoute(R route);

  /**
   * Accessor for all Routes in this routing table that are keyed by a target-prefix. Unlike {@link
   * #findNextHopRoutes(InterledgerAddress)} or {@link #findNextHopRoutes(InterledgerAddress, Account)}, this
   * method does not do any "longest-prefix" matching, and is instead meant to provide get-by-key semantics for the
   * routing table.
   *
   * @param addressPrefix An {@link InterledgerAddress} prefix used as a key in the routing table.
   * @return A {@link Collection} of all routes for the supplied {@code addressPrefix} key.
   */
  Collection<R> getRoutesByTargetPrefix(InterledgerAddress addressPrefix);

  /**
   * Remove all routes from the routing table that are keyed by {@code targetPrefix}.
   *
   * @param targetPrefix An {@link InterledgerAddress} prefix used as a key in the routing table.
   */
  Collection<R> removeAllRoutesForTargetPrefix(InterledgerAddress targetPrefix);

  /**
   * Perform the following action on each item in the routing table.
   *
   * @param action
   */
  void forEach(final BiConsumer<? super String, ? super Collection<R>> action);

  /**
   * Determine the ledger-prefix for the "next hop" ledger that a payment should be delivered/forwarded to. If this
   * routing table has no such route, then return {@link Optional#empty()}.
   *
   * @param finalDestinationAddress An {@link InterledgerAddress} representing the final destination of the payment
   *                                (i.e., the address of the receiver of an ILP payment).
   * @return An optionally-present ILP-prefix identifying the ledger-plugin that should be used to make the next local
   * transfer in an Interledger payment.
   */
  Collection<R> findNextHopRoutes(InterledgerAddress finalDestinationAddress);

  /**
   * Given a final destination ILP address, determine the "best" route that an ILP payment message or should traverse.
   *
   * @param finalDestinationAddress An {@link InterledgerAddress} representing the final payment destination for a
   *                                payment or message (this address may or may not be locally accessible in the routing
   *                                table).
   * @param sourceAccount           The {@link Account} from which the request originates. Used to filter next-hop
   *                                routes by a source address based upon the attributes of each route.
   * @return An optionally-present {@link R} for the supplied addresses.
   */
  default Collection<R> findNextHopRoutes(
      final InterledgerAddress finalDestinationAddress, final Account sourceAccount
  ) {
    InterledgerAddress.requireNotAddressPrefix(finalDestinationAddress);
    Objects.requireNonNull(sourceAccount);

    return this.findNextHopRoutes(finalDestinationAddress).stream()
        // Only return routes that are allowed per the source account filter...
        .filter(route -> sourceAccount.equals(route.getSourceAccount()))
        .collect(Collectors.toList());
  }

}
