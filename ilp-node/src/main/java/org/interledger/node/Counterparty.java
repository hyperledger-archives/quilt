package org.interledger.node;

import org.interledger.annotations.Immutable;

/**
 * A counterparty is the entity that holds one or more accounts with this node
 */
public interface Counterparty {

  static CounterpartyBuilder builder() {
    return new CounterpartyBuilder();
  }

  String getName();

  @Immutable
  abstract class AbstractCounterparty implements Counterparty {

  }

}
