package org.interledger.node.services.routing;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import org.apache.commons.collections4.trie.PatriciaTrie;
import org.interledger.core.InterledgerAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A key/value data structure that holds {@link InterledgerAddress} keys in a hierarchical order to allow for easy
 * prefix-matching.
 */
class InterledgerPrefixMap {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private final PatriciaTrie<Collection<Route>> prefixMap;

  public InterledgerPrefixMap() {
    this.prefixMap = new PatriciaTrie<>();
  }

  /**
   * The current number of address-prefix keys in the map. This is distinct from the total number of routes in the Map.
   */
  public int getNumKeys() {
    return prefixMap.size();
  }

  /**
   * {@inheritDoc}
   *
   * @throws NullPointerException {@inheritDoc}
   */
  public boolean add(final Route route) {
    Objects.requireNonNull(route);

    final Collection<Route> prefixedRouteSet;
    // Only allow a single thread to add a new route into this map at a time because the PatriciaTrie is not
    // thread-safe during puts.
    synchronized (prefixMap) {
      prefixedRouteSet = Optional.ofNullable(this.prefixMap.get(route.getTargetPrefix().getValue()))
        .orElseGet(() -> {
          final Set<Route> newPrefixedRoutes = Sets.newConcurrentHashSet();
          // Synchronized so that another thread doesn't add an identical route prefix from underneath us.
          this.prefixMap.put(route.getTargetPrefix().getValue(), newPrefixedRoutes);
          return newPrefixedRoutes;
        });
    }

    // This is ok to perform outside of the critical section because the prefixedRouteSet Set is thread-safe
    // (though this might not hurt to do inside of the synchronized block)
    return prefixedRouteSet.add(route);
  }

  /**
   * Remove a single route from the Prefix Map.
   *
   * @param route A {@link Route} to remove from the {@code prefixMap}.
   *
   * @return <tt>true</tt> if an element was removed as a result of this call.
   */
  public boolean removeRoute(final Route route) {
    Objects.requireNonNull(route);

    synchronized (prefixMap) {
      // There will be only a single route in the routing table that can be removed. Find that, then remove it.
      final Collection<Route> routeCollection = this.getRoutes(route.getTargetPrefix());
      final boolean result = routeCollection.remove(route);
      if (result && routeCollection.isEmpty()) {
        // If there are no more routes in the table, then remove the entire collection to the getKeys works
        // properly.
        this.prefixMap.remove(route.getTargetPrefix().getValue());
      }
      return result;
    }
  }

  /**
   * Remove all routes for the supplied {@code addressPrefix} key.
   *
   * @param addressPrefix
   */
  public Collection<Route> removeAllRoutes(final InterledgerAddress addressPrefix) {
    InterledgerAddress.requireAddressPrefix(addressPrefix);
    synchronized (prefixMap) {
      return this.prefixMap.remove(addressPrefix.getValue());
    }
  }

  public Collection<Route> getRoutes(final InterledgerAddress addressPrefix) {
    InterledgerAddress.requireAddressPrefix(addressPrefix);

    return Optional.ofNullable(this.prefixMap.get(addressPrefix.getValue())).orElse(Sets.newConcurrentHashSet());
  }

  /**
   * Returns a {@link Set} of keys that are contained in this map in {@link InterledgerAddress} form. Due to the
   * implementation of the PatriciaTrie (it stores Strings instead of ILP Address keys), the returned set is NOT backed
   * by the map, so changes to the map are not reflected in the set.
   *
   * @return a set view of the keys contained in this map
   */
  public Set<InterledgerAddress> getPrefixMapKeys() {
    return this.prefixMap.keySet().stream().map(InterledgerAddress::of).collect(Collectors.toSet());
  }

  /**
   * Take an action for each {@link Route} in the PrefixMap.
   *
   * @param action
   */
  public void forEach(final BiConsumer<? super String, ? super Collection<Route>> action) {
    Objects.requireNonNull(action);
    this.prefixMap.forEach(action);
  }

  /**
   * Given an ILP final destination address, determine the longest-matching target address in the routing table, and
   * then return all routes that exist for that target address.
   *
   * @param finalDestinationAddress An ILP prefix address of type {@link InterledgerAddress}.
   */
  public Collection<Route> findNextHopRoutes(final InterledgerAddress finalDestinationAddress) {
    InterledgerAddress.requireNotAddressPrefix(finalDestinationAddress);

    return finalDestinationAddress.getParentPrefix()
      .map(this::findLongestPrefix)
      .filter(Optional::isPresent)
      .map(Optional::get)
      .map(longestPrefix -> this.prefixMap.get(longestPrefix))
      .orElse(ImmutableList.of());
  }

  /**
   * Helper method to find the longest-prefix match given a destination ILP address (as a <tt>String</tt>).
   *
   * Because the PatriciaTrie allows for all entries with a given prefix to be cheaply returned from the map, we first
   * search the Trie for entries that contain a substring of the destination address up to the last period (.)
   * character. If a match is found, then all routes for the match are returned. However, if no match is found, then a
   * substring of the destination address is used, starting at index 0, up to the last period (.) character, moving from
   * the end of destinationAddress backwards towards index 0.
   *
   * After repeating this process back through the {@code destinationAddress}, if no matches in the routing table are
   * found, then this method returns {@link Optional#empty()}.
   *
   * @param destinationAddressPrefix A {@link String} representing a destination ILP address.
   *
   * @return The longest-prefix match in the PatriciaTrie for the supplied {@code destinationAddress}.
   */
  @VisibleForTesting
  protected Optional<String> findLongestPrefix(final InterledgerAddress destinationAddressPrefix) {
    InterledgerAddress.requireAddressPrefix(destinationAddressPrefix);

    // Unlike a typical prefix match, ILP addresses can be matched by subsections delimited by a period separator.

    // 1.) Narrow the overall search-space by using the PatriciaTrie to filter out any non-matching prefixes
    // (e.g. for destAddress of "g.foo.bar.baz", the entries "g." and "g.foo." will not be returned by the
    // prefixMap. This allows us to find a longest-match in that space. If there aren't any matches, we
    // recurse this method (#findLongestPrefix) using the parent-prefix. For example, if the routing-table has
    // only "g.", then "g.1.2." will not return a sub-map, so we should recursively search for a sub-map with
    // with "g.1.", and then "g.".

    final SortedMap<String, Collection<Route>> prefixSubMap = prefixMap.prefixMap(destinationAddressPrefix.getValue());
    if (prefixSubMap.isEmpty() && destinationAddressPrefix.getParentPrefix().isPresent()) {
      final InterledgerAddress parentPrefix = destinationAddressPrefix.getParentPrefix().get();
      final Optional<String> longestMatch = this.findLongestPrefix(parentPrefix);
      if (longestMatch.isPresent()) {
        return longestMatch;
      } else {
        // Fallback to a global-prefix match, if any is defined in the routing table.
        return this.findLongestPrefix(getRootPrefix(destinationAddressPrefix));
      }
    } else {
      // There are prefixes in the Trie to search. So, we loop through each one in this reduced search space.
      // This is effectively an O(n) operation with n being the number of entries in prefixSubMap.
      return prefixSubMap.keySet().stream()
        .filter(val -> val.length() <= destinationAddressPrefix.getValue().length())
        .distinct()
        // Don't allow more than one in this list.
        .collect(Collectors.reducing((a, b) -> {
          logger.error(
            "Routing table has more than one longest-match! This should never happen!"
          );
          return null;
        }));
    }
  }

  /**
   * Compute the root-prefix of the supplied {@code address}.
   *
   * @return An {@link InterledgerAddress} representing the root prefix for the supplied Interledger address.
   */
  @VisibleForTesting
  protected InterledgerAddress getRootPrefix(final InterledgerAddress address) {
    Objects.requireNonNull(address);

    while (address.getParentPrefix().isPresent()) {
      return getRootPrefix(address.getParentPrefix().get());
    }

    return address;
  }

}
