package org.interledger.link.http;

import static org.interledger.link.http.IlpOverHttpLinkSettings.AUTH_TYPE;
import static org.interledger.link.http.IlpOverHttpLinkSettings.DOT;
import static org.interledger.link.http.IlpOverHttpLinkSettings.ILP_OVER_HTTP;
import static org.interledger.link.http.IlpOverHttpLinkSettings.OUTGOING;
import static org.interledger.link.http.IlpOverHttpLinkSettings.SHARED_SECRET;
import static org.interledger.link.http.IlpOverHttpLinkSettings.TOKEN_AUDIENCE;
import static org.interledger.link.http.IlpOverHttpLinkSettings.TOKEN_EXPIRY;
import static org.interledger.link.http.IlpOverHttpLinkSettings.TOKEN_ISSUER;
import static org.interledger.link.http.IlpOverHttpLinkSettings.TOKEN_SUBJECT;
import static org.interledger.link.http.IlpOverHttpLinkSettings.URL;

import okhttp3.HttpUrl;
import org.immutables.value.Value;
import org.immutables.value.Value.Modifiable;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public interface OutgoingLinkSettings extends SharedSecretTokenSettings {

  String HTTP_OUTGOING_AUTH_TYPE = ILP_OVER_HTTP + DOT + OUTGOING + DOT + AUTH_TYPE;

  String HTTP_OUTGOING_TOKEN_ISSUER = ILP_OVER_HTTP + DOT + OUTGOING + DOT + TOKEN_ISSUER;
  String HTTP_OUTGOING_TOKEN_AUDIENCE = ILP_OVER_HTTP + DOT + OUTGOING + DOT + TOKEN_AUDIENCE;
  String HTTP_OUTGOING_TOKEN_SUBJECT = ILP_OVER_HTTP + DOT + OUTGOING + DOT + TOKEN_SUBJECT;
  String HTTP_OUTGOING_TOKEN_EXPIRY = ILP_OVER_HTTP + DOT + OUTGOING + DOT + TOKEN_EXPIRY;

  String HTTP_OUTGOING_SHARED_SECRET = ILP_OVER_HTTP + DOT + OUTGOING + DOT + SHARED_SECRET;
  String HTTP_OUTGOING_URL = ILP_OVER_HTTP + DOT + OUTGOING + DOT + URL;

  static ImmutableOutgoingLinkSettings.Builder builder() {
    return ImmutableOutgoingLinkSettings.builder();
  }

  /**
   * Constructs a new builder with the correct custom settings, as found in {@code customSettings}.
   *
   * @param customSettings A {@link Map} of custom settings to apply.
   *
   * @return A {@link ImmutableOutgoingLinkSettings.Builder} with applied custom settings.
   */
  static ImmutableOutgoingLinkSettings.Builder fromCustomSettings(Map<String, Object> customSettings) {
    Objects.requireNonNull(customSettings);

    return applyCustomSettings(ImmutableOutgoingLinkSettings.builder(), customSettings);
  }

  /**
   * Populate a supplied builder with the correct custom settings, as found in {@code customSettings}.
   *
   * @param builder        A {@link ImmutableIncomingLinkSettings.Builder} to apply custom settings to.
   * @param customSettings A {@link Map} of custom settings to apply.
   *
   * @return A {@link ImmutableOutgoingLinkSettings.Builder} with applied custom settings.
   */
  static ImmutableOutgoingLinkSettings.Builder applyCustomSettings(
      final ImmutableOutgoingLinkSettings.Builder builder, Map<String, Object> customSettings
  ) {
    Objects.requireNonNull(builder);
    Objects.requireNonNull(customSettings);

    // When loaded from a properties file, the properties are hierarchical in a Map. However, in Java, they are not, so
    // consider both options. Generally only one will be present, but if for some reason both are present, the String
    // values will win.
    Optional.ofNullable(customSettings.get(ILP_OVER_HTTP))
        .map(val -> (Map<String, Object>) val)
        .ifPresent(blastSettings -> {

          Optional.ofNullable(blastSettings.get(OUTGOING))
              .map(val -> (Map<String, Object>) val)
              .ifPresent(outgoingSettings -> {

                Optional.ofNullable(outgoingSettings.get(TOKEN_SUBJECT))
                    .map(Object::toString)
                    .ifPresent(builder::tokenSubject);

                Optional.ofNullable(outgoingSettings.get(TOKEN_ISSUER))
                    .map(Object::toString)
                    .map(HttpUrl::parse)
                    .ifPresent(builder::tokenIssuer);

                Optional.ofNullable(outgoingSettings.get(TOKEN_AUDIENCE))
                    .map(Object::toString)
                    .map(HttpUrl::parse)
                    .ifPresent(builder::tokenAudience);

                Optional.ofNullable(outgoingSettings.get(AUTH_TYPE))
                    .map(Object::toString)
                    .map(String::toUpperCase)
                    .map(IlpOverHttpLinkSettings.AuthType::valueOf)
                    .ifPresent(builder::authType);

                Optional.ofNullable(outgoingSettings.get(SHARED_SECRET))
                    .map(Object::toString)
                    .ifPresent(builder::encryptedTokenSharedSecret);

                Optional.ofNullable(outgoingSettings.get(TOKEN_EXPIRY))
                    .map(Object::toString)
                    .map(Duration::parse)
                    .ifPresent(builder::tokenExpiry);

                Optional.ofNullable(outgoingSettings.get(URL))
                    .map(Object::toString)
                    .map(HttpUrl::parse)
                    .ifPresent(builder::url);

              });
        });

    Optional.ofNullable(customSettings.get(HTTP_OUTGOING_TOKEN_ISSUER))
        .map(Object::toString)
        .map(HttpUrl::parse)
        .ifPresent(builder::tokenIssuer);

    Optional.ofNullable(customSettings.get(HTTP_OUTGOING_TOKEN_AUDIENCE))
        .map(Object::toString)
        .map(HttpUrl::parse)
        .ifPresent(builder::tokenAudience);

    Optional.ofNullable(customSettings.get(HTTP_OUTGOING_AUTH_TYPE))
        .map(Object::toString)
        .map(String::toUpperCase)
        .map(IlpOverHttpLinkSettings.AuthType::valueOf)
        .ifPresent(builder::authType);

    Optional.ofNullable(customSettings.get(HTTP_OUTGOING_TOKEN_SUBJECT))
        .map(Object::toString)
        .ifPresent(builder::tokenSubject);

    Optional.ofNullable(customSettings.get(HTTP_OUTGOING_SHARED_SECRET))
        .map(Object::toString)
        .ifPresent(builder::encryptedTokenSharedSecret);

    Optional.ofNullable(customSettings.get(HTTP_OUTGOING_TOKEN_EXPIRY))
        .map(Object::toString)
        .map(Duration::parse)
        .ifPresent(builder::tokenExpiry);

    Optional.ofNullable(customSettings.get(HTTP_OUTGOING_URL))
        .map(Object::toString)
        .map(HttpUrl::parse)
        .ifPresent(builder::url);

    return builder;
  }

  /**
   * The expected `sub` value of an ILP-over-HTTP authentication token. This values identifies the principal that the
   * token is authenticating.
   *
   * @return A {@link String} with the token subject.
   */
  String tokenSubject();

  /**
   * If present, determines how often to sign a new token for auth. Optional to support the shared-secret use-case.
   *
   * @return An optionally present {@link Duration} representing the token expiry.
   */
  Optional<Duration> tokenExpiry();

  /**
   * endpoint to POST packets to. If url contains a percent and the link is in `multi` mode, then the segment after this
   * link's own address will be filled where the `%` is  when routing packets.
   *
   * @return An {@link HttpUrl} for this Link.
   */
  HttpUrl url();

  @Value.Immutable
  @Modifiable
  abstract class AbstractOutgoingLinkSettings implements OutgoingLinkSettings {

    @Override
    @Value.Redacted
    public abstract String encryptedTokenSharedSecret();

  }
}
