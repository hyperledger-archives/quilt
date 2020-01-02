package org.interledger.link.http;

import okhttp3.HttpUrl;
import org.immutables.value.Value;

import java.time.Duration;
import java.util.Optional;

/**
 * Settings for authenticating use JWT with HS256 or RS256
 */
@Value.Immutable
public interface JwtAuthSettings {

  static ImmutableJwtAuthSettings.Builder builder() {
    return ImmutableJwtAuthSettings.builder();
  }

  /**
   * The expected `sub` value of a JWT token. This values identifies the principal that the
   * token is authenticating.
   *
   * @return A {@link String} with the token subject.
   */
  String tokenSubject();

  /**
   * An encrypted shared `secret`, encoded per `EncryptedSecret`, that can be used to authenticate an incoming
   * ILP-over-HTTP (BLAST) connection.
   *
   * @return A {@link String} containing the shared secret in base64 encoding. Required for HS256.
   */
  @Value.Redacted
  Optional<String> encryptedTokenSharedSecret();

  /**
   * The expected `iss` value of the issuer of a JWT_RS_256 token. This value should always be a URL so that it can be rooted
   * in Internet PKI and compared against the TLS certificate issued by the other side of a blast connection (i.e., the
   * remote peer). It is optional in order to support node-wide issuance, or account-level issuance.
   *
   * @return An optionally present {@link HttpUrl} for the token issuer. Required for RS256.
   */
  Optional<HttpUrl> tokenIssuer();

  /**
   * The expected `aud` claim value of an incoming JWT_RS_256 token. In general, this value should be the URL of the
   * Connector operating this BLAST link, since the remote will want to narrow the scope of its token to only be valid
   * on this endpoint.
   *
   * @return An optionally present String for the token audience. Required for RS256.
   */
  Optional<String> tokenAudience();

  /**
   * If present, determines how often to sign a new token for auth. Optional to support the shared-secret use-case.
   *
   * @return An optionally present {@link Duration} representing the token expiry.
   */
  Optional<Duration> tokenExpiry();

}
