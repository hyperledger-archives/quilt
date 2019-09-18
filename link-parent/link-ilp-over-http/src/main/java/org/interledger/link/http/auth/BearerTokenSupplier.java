package org.interledger.link.http.auth;

import java.util.function.Supplier;

/**
 * <p>Defines a {@link Supplier} that provides a JWT that can be used as an ILP-over-HTTP bearer token.</p>
 *
 * <p>This implementation attempts to reduce the amount of time the shared-secret is in-memory by preemptively
 * generating an authentication token that conforms to `JWT_HS_256`, and then wiping the shared-secret from it allocated
 * memory. Additionally, generated auth tokens have short durations (i.e, they expire), so typing them as Strings is
 * tolerable for this implementation.</p>
 */
public interface BearerTokenSupplier extends Supplier<String> {

  /**
   * Gets an ILP-over-HTTP bearer token which can be used to make requests to a remote HTTP endpoint supporting
   * ILP-over-HTTP.
   *
   * @return A JWT bearer token, as a {@link String}.
   */
  @Override
  String get();

}
