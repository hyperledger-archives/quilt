package org.interledger.ilqp;

import org.interledger.InterledgerAddress;


/**
 * A quote service delivers quote requests to connectors and returns their response.
 *
 * @param <T> the type of the input to the service.
 * @param <R> the type of the result of the service.
 */
public interface QuoteService<T extends QuoteRequest, R extends QuoteResponse> {

  /**
   * Requests a quote and applies the given selection strategy to return the best quote received.
   *
   * @param quoteRequest      The quote requested.
   * @param selectionStrategy A strategy for selecting the best of a given set of quote responses.
   *
   * @return An instance {@link R} which is the best quote, or null if no responses where received.
   */
  R requestQuote(T quoteRequest, QuoteSelectionStrategy selectionStrategy)
      throws InterledgerQuotingException;

  /**
   * Requests a quote of the connector at the given address.
   *
   * @param quoteRequest The quote requested.
   * @param connector    The ILP address of the connector to get the quote of.
   *
   * @return The quote response of the connector, or null if no response is received.
   */
  R requestQuote(T quoteRequest, InterledgerAddress connector) throws InterledgerQuotingException;
}
