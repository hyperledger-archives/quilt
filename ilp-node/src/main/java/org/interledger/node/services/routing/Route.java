package org.interledger.node.services.routing;

import org.interledger.annotations.Immutable;
import org.interledger.annotations.Wrapped;
import org.interledger.annotations.Wrapper;
import org.interledger.core.InterledgerAddress;
import org.interledger.node.Account;

import javax.money.convert.ExchangeRate;
import java.time.Duration;
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
   * The target ILP address prefix of addresses that are routed to with this route.
   */
  InterledgerAddress getTargetPrefix();

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
   * Get the {@link ExchangeRate} for this route.
   *
   * @return an {@link ExchangeRate} to convert incoming amounts to outgoing amounts
   */
  ExchangeRate getExchangeRate();


  /**
   * Get the amount of time to subtract from the expiry when forwarding packets on this route
   *
   * @return a {@link Duration} to subtract from the incoming expiry
   */
  Duration getExpiryMargin();

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
