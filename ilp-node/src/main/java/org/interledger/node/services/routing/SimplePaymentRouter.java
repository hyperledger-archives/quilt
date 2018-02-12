package org.interledger.node.services.routing;

import java.util.Objects;
import java.util.Optional;

import org.interledger.core.InterledgerAddress;
import org.interledger.node.Account;

/**
 * A simple implementation of {@link PaymentRouter} that simply chooses the first route if multiple are returned from
 * the routing table.
 */
public class SimplePaymentRouter implements PaymentRouter<Route> {

  private final RoutingTable<Route> routingTable;

  public SimplePaymentRouter(final RoutingTable<Route> routingTable) {
    this.routingTable = Objects.requireNonNull(routingTable);
  }

  /**
   * Given an incoming transfer on a particular source ledger, this method finds the best "next-hop" route that should
   * be utilized to complete an Interledger payment.
   *
   * At a general level, this method works as follows:
   *
   * Given an ILP Payment from A→C, find the next hop B on the payment path from A to C.
   *
   * @param finalDestinationAddress An {@link InterledgerAddress} representing the final payment destination for a
   *                                payment or message (this address may or may not be locally accessible in the routing
   *                                table).
   */
  public Optional<Route> findBestNexHop(final InterledgerAddress finalDestinationAddress) {
    InterledgerAddress.requireNotAddressPrefix(finalDestinationAddress);
    return this.routingTable.findNextHopRoutes(finalDestinationAddress).stream().findFirst();
  }

  /**
   * Given an incoming transfer on a particular source ledger, this method finds the best "next-hop" route that should
   * be utilized to complete an Interledger payment.
   *
   * At a general level, this method works as follows:
   *
   * Given an ILP Payment from A→C, find the next hop B on the payment path from A to C.
   *
   * @param finalDestinationAddress An {@link InterledgerAddress} representing the final payment destination for a
   *                                payment or message (this address may or may not be locally accessible in the routing
   *                                table).
   * @param sourceLedgerPrefix      An {@link InterledgerAddress} prefix that indicates the ILP node that received the
   *                                payment being routed. This value is used to optionally restrict the set of
   *                                available
   */
  @Override
  public Optional<Route> findBestNexHop(InterledgerAddress finalDestinationAddress, Account sourceAccount) {
    InterledgerAddress.requireNotAddressPrefix(finalDestinationAddress);
    Objects.requireNonNull(sourceAccount);
    return this.routingTable.findNextHopRoutes(finalDestinationAddress, sourceAccount).stream().findFirst();
  }
}
