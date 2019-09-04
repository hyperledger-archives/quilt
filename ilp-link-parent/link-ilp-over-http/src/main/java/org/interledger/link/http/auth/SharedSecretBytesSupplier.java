package org.interledger.link.http.auth;

import java.util.function.Supplier;

/**
 * <p>Defines a {@link Supplier} that provides a byte-array containing the shared-secret that can be used to perform
 * JWT signing operations. Note that this interface is generally targeted at the "HMAC with SHA-2" family of algorithms
 * defined in RFC-7518.</p>
 *
 * <p>This implementation attempts to reduce the amount of time the shared-secret is in-memory by preemptively
 * generating an authentication token that conforms to `JWT_HS_256`, copying it, and then wiping the shared-secret from
 * it allocated memory. Consumers of the secret bytes should take care to do the same.</p>
 *
 * @see "https://tools.ietf.org/html/rfc7518#section-3.2"
 */
public interface SharedSecretBytesSupplier extends Supplier<byte[]> {

  /**
   * Return a shared secret that can be used to sign JWT tokens using the HS_256 alogrithm.
   *
   * @return A JWT bearer token, as a {@link String}.
   */
  @Override
  byte[] get();

}
