package org.interledger.ilqp;

import java.util.Set;
import java.util.function.Function;

@FunctionalInterface
public interface QuoteSelectionStrategy
    extends Function<Set<? extends QuoteResponse>, QuoteResponse> {

}
