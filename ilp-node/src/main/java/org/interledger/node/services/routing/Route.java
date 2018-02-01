package org.interledger.node.services.routing;

import org.interledger.annotations.Immutable;
import org.interledger.annotations.Wrapped;
import org.interledger.annotations.Wrapper;
import org.interledger.node.Account;
import org.interledger.node.services.fx.RateConverter;

import java.util.UUID;

/**
 * A route is a connection between a source account and destination account.
 *
 */
public interface Route extends Comparable<Route>{

  static RouteBuilder builder() {
    return new RouteBuilder();
  }

  /**
   * Get the local identifier for this route.
   *
   * @return
   */
  RouteId getRouteId();

  /**
   * Get the source account for the route.
   *
   * @return
   */
  Account getSourceAccount();

  /**
   * Get the destination account for the route.
   *
   * @return an {@link Account} that is the destination of this route.
   */
  Account getDestinationAccount();


  /**
   * Get the {@link RateConverter} for this route.
   *
   * @return an {@link }
   */
  RateConverter getRateConverter();

  @Immutable
  abstract class AbstractRoute implements Route {

    @Override
    public int compareTo(Route otherRoute) {

      int compare = this.getSourceAccount().compareTo(otherRoute.getDestinationAccount());

      if(compare == 0) {
        return this.getDestinationAccount().compareTo(otherRoute.getDestinationAccount());
      }

      return compare;
    }

    /**
     * A route is considered equal if the source and destination accounts are equal.
     *
     * @param otherRoute the route to comapre to.
     *
     * @return true if the source and destination account on both routes are equal
     */
    @Override
    public boolean equals(Object otherRoute) {

      if(otherRoute != null && otherRoute instanceof Route) {
        return this.getSourceAccount().equals(((Route) otherRoute).getSourceAccount())
            && this.getDestinationAccount().equals(((Route) otherRoute).getDestinationAccount());
      }

      return false;

    }
  }

  /**
   * Identifier for {@link Route}.
   */
  @Wrapped
  abstract class WrappedRouteId extends Wrapper<UUID> {

  }

}
