package org.interledger.link.http.auth;

import org.interledger.link.http.OutgoingLinkSettings;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Date;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * <p>Defines a {@link Supplier} that provides a JWT that can be used as an ILP-over-HTTP bearer token.</p>
 *
 * <p>This implementation attempts to reduce the amount of time the shared-secret is in-memory by preemptively
 * generating an authentication token that conforms to `JWT_HS_256`, and then wiping the shared-secret from it allocated
 * memory. Additionally, generated auth tokens have short durations (i.e, they expire), so typing them as Strings is
 * tolerable for this implementation.</p>
 */
public class JwtHs256BearerTokenSupplier implements BearerTokenSupplier {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private final LoadingCache<String, String> ilpOverHttpAuthTokens;
  private final OutgoingLinkSettings outgoingLinkSettings;

  /**
   * Required-args Constructor.
   *
   * @param sharedSecretBytesSupplier A {@link SharedSecretBytesSupplier} that returns a copy of the shared secret used
   *                                  to sign JWTs using the HS_256 algorithm.
   * @param outgoingLinkSettings      A {@link OutgoingLinkSettings} that contains all settings required to construct a
   *                                  bearer token that can auth an outgoing request.
   */
  public JwtHs256BearerTokenSupplier(
      final SharedSecretBytesSupplier sharedSecretBytesSupplier,
      final OutgoingLinkSettings outgoingLinkSettings
  ) {
    Objects.requireNonNull(sharedSecretBytesSupplier);
    Objects.requireNonNull(outgoingLinkSettings);

    this.outgoingLinkSettings = Objects.requireNonNull(outgoingLinkSettings);

    ilpOverHttpAuthTokens = CacheBuilder.newBuilder()
        // There should only ever be 1 or 2 tokens in-memory for a given client instance.
        .maximumSize(3)
        // Expire after this duration, which will correspond to the last incoming request from the peer.
        .expireAfterAccess(
            this.outgoingLinkSettings.tokenExpiry().orElse(Duration.of(30, ChronoUnit.MINUTES)).toMinutes(),
            TimeUnit.MINUTES
        )
        .removalListener((RemovalListener<String, String>) notification ->
            logger.debug("Removing IlpOverHttp AuthToken from Cache for Principal: {}", notification.getKey())
        )
        .build(new CacheLoader<String, String>() {
          public String load(final String authSubject) {
            Objects.requireNonNull(authSubject);

            final byte[] sharedSecretBytes = sharedSecretBytesSupplier.get();
            try {
              return JWT.create()
                  //.withIssuedAt(new Date())
                  //      .withIssuer(getOutgoingLinkSettings()
                  //        .tokenIssuer()
                  //        .map(HttpUrl::toString)
                  //        .orElseThrow(() -> new RuntimeException("JWT Blast Senders require an Outgoing Issuer!"))
                  //      )
                  .withSubject(outgoingLinkSettings.tokenSubject()) // account identifier at the remote server.
                  // Expire at the appointed time, or else after 15 minutes.
                  .withExpiresAt(
                      JwtHs256BearerTokenSupplier.this.outgoingLinkSettings.tokenExpiry()
                      .map(expiry -> Date.from(Instant.now().plus(expiry)))
                      .orElseGet(() -> Date.from(Instant.now().plus(15, ChronoUnit.MINUTES)))
                  )
                  .sign(Algorithm.HMAC256(sharedSecretBytes));
            } finally {
              // Zero-out all bytes in the `sharedSecretBytes` array.
              Arrays.fill(sharedSecretBytes, (byte) 0);
            }
          }
        });
  }

  /**
   * Gets an ILP-over-HTTP bearer token which can be used to make requests to a remote HTTP endpoint supporting
   * ILP-over-HTTP.
   *
   * @return A JWT bearer token, as a {@link String}.
   */
  @Override
  public String get() {
    try {
      return this.ilpOverHttpAuthTokens.get(outgoingLinkSettings.tokenSubject());
    } catch (ExecutionException e) {
      throw new RuntimeException(e);
    }
  }

}
