package org.interledger.node.services.routing;

import java.util.Collection;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import org.interledger.core.InterledgerAddress;
import org.interledger.node.Account;

/**
 * An implementation of {@link RoutingTable} that stores all routes in-memory using an {@link InterledgerPrefixMap} for
 * efficient search and prefix-matching operations.
 *
 * This implementation is meant for use-cases where routes do not change very often, like statically-configured routing
 * environments where this table can be populated when the server starts-up.
 */
public class InMemoryRoutingTable implements RoutingTable<Route> {

  private final InterledgerPrefixMap interledgerPrefixMap;

  public InMemoryRoutingTable() {
    this(new InterledgerPrefixMap());
  }

  /**
   * Exists for testing purposes, but is otherwise not necessary.
   *
   * @param interledgerPrefixMap
   */
  public InMemoryRoutingTable(final InterledgerPrefixMap interledgerPrefixMap) {
    this.interledgerPrefixMap = Objects.requireNonNull(interledgerPrefixMap);
  }

  @Override
  public boolean addRoute(final Route route) {
    Objects.requireNonNull(route);
    return this.interledgerPrefixMap.add(route);
  }

  @Override
  public boolean removeRoute(final Route route) {
    Objects.requireNonNull(route);
    return this.interledgerPrefixMap.removeRoute(route);
  }

  @Override
  public Collection<Route> getRoutesByTargetPrefix(final InterledgerAddress addressPrefix) {
    Objects.requireNonNull(addressPrefix);
    InterledgerAddress.requireAddressPrefix(addressPrefix);
    return this.interledgerPrefixMap.getRoutes(addressPrefix);
  }

  @Override
  public Collection<Route> removeAllRoutesForTargetPrefix(InterledgerAddress addressPrefix) {
    Objects.requireNonNull(addressPrefix);
    InterledgerAddress.requireAddressPrefix(addressPrefix);
    return this.interledgerPrefixMap.removeAllRoutes(addressPrefix);
  }

  @Override
  public void forEach(final BiConsumer<? super String, ? super Collection<Route>> action) {
    Objects.requireNonNull(action);
    this.interledgerPrefixMap.forEach(action);
  }

  @Override
  public Collection<Route> findNextHopRoutes(InterledgerAddress finalDestinationAddress) {
    InterledgerAddress.requireNotAddressPrefix(finalDestinationAddress);
    return this.interledgerPrefixMap.findNextHopRoutes(finalDestinationAddress);
  }

  @Override
  public Collection<Route> findNextHopRoutes(
    final InterledgerAddress finalDestinationAddress,
    final Account sourceAccount
  ) {
    InterledgerAddress.requireNotAddressPrefix(finalDestinationAddress);
    Objects.requireNonNull(sourceAccount);
    return this.interledgerPrefixMap.findNextHopRoutes(finalDestinationAddress).stream()
      // Only return routes that are allowed per the source account filter...
      .filter(route -> route.getSourceAccount().equals(sourceAccount))
      .collect(Collectors.toList());
  }
}
