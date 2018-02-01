package org.interledger.node;

import org.interledger.core.InterledgerAddress;

import java.util.Optional;

public interface NodeConfiguration {

  Optional<InterledgerAddress> getAddress();
}
