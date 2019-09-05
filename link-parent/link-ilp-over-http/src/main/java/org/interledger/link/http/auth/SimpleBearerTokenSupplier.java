package org.interledger.link.http.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * <p>Defines a {@link Supplier} that provides a plain-text bearer auth token.</p>
 *
 * <p>WARNING: This implementation is intended for testing and debugging purposes, and should not be used in a
 * production environment because the tokens produced never expire, and are held in memory. Instead, consider using
 * {@link JwtHs256BearerTokenSupplier} instead.</p>
 */
public class SimpleBearerTokenSupplier implements BearerTokenSupplier {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private final String bearerAuthToken;

  /**
   * Required-args Constructor.
   *
   * @param bearerAuthToken An array of chars that contains a plaintext bearer auth token.
   */
  public SimpleBearerTokenSupplier(final String bearerAuthToken) {
    this.bearerAuthToken = Objects.requireNonNull(bearerAuthToken);
  }

  /**
   * Gets an ILP-over-HTTP bearer token which can be used to make requests to a remote HTTP endpoint supporting
   * ILP-over-HTTP.
   *
   * @return A simple plain-text bearer token, as a {@link String}.
   */
  @Override
  public String get() {
    return bearerAuthToken;
  }

}
