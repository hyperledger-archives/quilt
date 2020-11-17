package org.interledger.core.cf;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.immutables.value.Value.Immutable;

/**
 * A tuple. Ideal for use in modeling responses from a {@link CompletableFuture}.
 *
 * @param <T>
 */
@Immutable
public interface Tuple<T> {

  static ImmutableTuple.Builder builder() {
    return ImmutableTuple.builder();
  }

  Optional<T> result();

  Throwable throwable();
}
