package org.interledger.link.http;

import static org.interledger.link.http.IlpOverHttpLinkSettings.AUTH_TOKEN;
import static org.interledger.link.http.IlpOverHttpLinkSettings.AUTH_TYPE;
import static org.interledger.link.http.IlpOverHttpLinkSettings.DOT;
import static org.interledger.link.http.IlpOverHttpLinkSettings.ILP_OVER_HTTP;
import static org.interledger.link.http.IlpOverHttpLinkSettings.JWT;
import static org.interledger.link.http.IlpOverHttpLinkSettings.OUTGOING;
import static org.interledger.link.http.IlpOverHttpLinkSettings.SHARED_SECRET;
import static org.interledger.link.http.IlpOverHttpLinkSettings.SIMPLE;
import static org.interledger.link.http.IlpOverHttpLinkSettings.TOKEN_AUDIENCE;
import static org.interledger.link.http.IlpOverHttpLinkSettings.TOKEN_EXPIRY;
import static org.interledger.link.http.IlpOverHttpLinkSettings.TOKEN_ISSUER;
import static org.interledger.link.http.IlpOverHttpLinkSettings.TOKEN_SUBJECT;
import static org.interledger.link.http.IlpOverHttpLinkSettings.URL;

import com.google.common.collect.ImmutableMap;
import okhttp3.HttpUrl;
import org.immutables.value.Value;
import org.immutables.value.Value.Modifiable;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Value.Immutable
@Modifiable
public interface OutgoingLinkSettings extends AuthenticatedLinkSettings {

  String HTTP_OUTGOING_AUTH_TYPE = ILP_OVER_HTTP + DOT + OUTGOING + DOT + AUTH_TYPE;
  String HTTP_OUTGOING_TOKEN_ISSUER = ILP_OVER_HTTP + DOT + OUTGOING + DOT + JWT + DOT + TOKEN_ISSUER;
  String HTTP_OUTGOING_TOKEN_AUDIENCE = ILP_OVER_HTTP + DOT + OUTGOING + DOT + JWT + DOT + TOKEN_AUDIENCE;
  String HTTP_OUTGOING_TOKEN_SUBJECT = ILP_OVER_HTTP + DOT + OUTGOING + DOT + JWT + DOT + TOKEN_SUBJECT;
  String HTTP_OUTGOING_TOKEN_EXPIRY = ILP_OVER_HTTP + DOT + OUTGOING + DOT + JWT + DOT + TOKEN_EXPIRY;
  String HTTP_OUTGOING_SHARED_SECRET = ILP_OVER_HTTP + DOT + OUTGOING + DOT + JWT + DOT + SHARED_SECRET;

  String HTTP_OUTGOING_SIMPLE_AUTH_TOKEN = ILP_OVER_HTTP + DOT + OUTGOING + DOT + SIMPLE + DOT + AUTH_TOKEN;
  String HTTP_OUTGOING_URL = ILP_OVER_HTTP + DOT + OUTGOING + DOT + URL;

  static ImmutableOutgoingLinkSettings.Builder builder() {
    return ImmutableOutgoingLinkSettings.builder();
  }

  /**
   * Constructs a new builder with the correct custom settings, as found in {@code customSettings}. Custom settings
   * are expected to follow 1 of the following sets of properties.
   * <p>
   * Simple
   * </p>
   * <pre>
   *   "ilpOverHttp.outgoing.auth_type": "SIMPLE"
   *   "ilpOverHttp.outgoing.simple.auth_token": "password",
   *   "ilpOverHttp.outgoing.url": "http://java1.connector.com/accounts/foo/ilp"
   * </pre>
   *
   * <p>
   * JWT (using HS256)
   * </p>
   * <pre>
   *   "ilpOverHttp.outgoing.auth_type": "JWT_HS_256"
   *   "ilpOverHttp.incoming.jwt.shared_secret": "some-key-used-for-symmetric-encryption",
   *   "ilpOverHttp.outgoing.jwt.subject": "foo@them",
   *   "ilpOverHttp.outgoing.jwt.expiryDuration": "pt500s",
   *   "ilpOverHttp.outgoing.url": "http://java1.connector.com/accounts/foo/ilp"
   * </pre>
   *
   * <p>
   * JWT (using RS256)
   * </p>
   * <pre>
   *   "ilpOverHttp.outgoing.auth_type": "JWT_RS_256"
   *   "ilpOverHttp.outgoing.jwt.issuer": "http://me.example.com",
   *   "ilpOverHttp.outgoing.jwt.audience": "https://me.example.com",
   *   "ilpOverHttp.outgoing.jwt.subject": "foo@me",
   *   "ilpOverHttp.outgoing.url": "http://java1.connector.com/accounts/foo/ilp"
   * </pre>
   *
   * @param customSettings A {@link Map} of custom settings to apply.
   * @return A {@link ImmutableOutgoingLinkSettings.Builder} with applied custom settings.
   */
  static ImmutableOutgoingLinkSettings.Builder fromCustomSettings(Map<String, Object> customSettings) {
    Objects.requireNonNull(customSettings);

    return applyCustomSettings(ImmutableOutgoingLinkSettings.builder(), customSettings);
  }

  /**
   * Populate a supplied builder with the correct custom settings, as found in {@code customSettings}.
   *
   * @param builder        A {@link ImmutableOutgoingLinkSettings.Builder} to apply custom settings to.
   * @param customSettings A {@link Map} of custom settings to apply.
   * @return A {@link ImmutableOutgoingLinkSettings.Builder} with applied custom settings.
   */
  static ImmutableOutgoingLinkSettings.Builder applyCustomSettings(
      final ImmutableOutgoingLinkSettings.Builder builder, Map<String, Object> customSettings
  ) {
    Objects.requireNonNull(builder);
    Objects.requireNonNull(customSettings);

    Optional.ofNullable(LinkSettingsUtils.flattenSettings(customSettings))
        .ifPresent(settings ->
            LinkSettingsUtils.getOutgoingAuthType(settings)
                .ifPresent((authType) -> {

                  builder.authType(authType);

                  Optional.ofNullable(settings.get(HTTP_OUTGOING_URL))
                      .map(Object::toString)
                      .map(HttpUrl::parse)
                      .map(builder::url)
                      .orElseThrow(() -> new IllegalArgumentException(HTTP_OUTGOING_URL + " is required"));

                  switch (authType) {
                    case SIMPLE: {
                      builder.simpleAuthSettings(buildSettingsForSimple(settings));
                      break;
                    }
                    case JWT_HS_256:
                    case JWT_RS_256: {
                      builder.jwtAuthSettings(buildSettingsForJwt(settings));
                      break;
                    }
                    default: throw new IllegalArgumentException("unsupported authType " + authType);
                  }
                })
        );

    return builder;
  }

  static SimpleAuthSettings buildSettingsForSimple(Map<String, Object> settings) {
    return Optional.ofNullable(settings.get(HTTP_OUTGOING_SIMPLE_AUTH_TOKEN))
        .map(Object::toString)
        .map(SimpleAuthSettings::forAuthToken)
        .orElseThrow(() -> new IllegalArgumentException(HTTP_OUTGOING_SIMPLE_AUTH_TOKEN + " required"));
  }

  static JwtAuthSettings buildSettingsForJwt(Map<String, Object> settings) {
    ImmutableJwtAuthSettings.Builder authSettingsBuilder = JwtAuthSettings.builder();

    Optional.ofNullable(settings.get(HTTP_OUTGOING_TOKEN_ISSUER))
        .map(Object::toString)
        .map(HttpUrl::parse)
        .map(authSettingsBuilder::tokenIssuer);

    Optional.ofNullable(settings.get(HTTP_OUTGOING_TOKEN_AUDIENCE))
        .map(Object::toString)
        .map(authSettingsBuilder::tokenAudience);

    Optional.ofNullable(settings.get(HTTP_OUTGOING_TOKEN_SUBJECT))
        .map(Object::toString)
        .map(authSettingsBuilder::tokenSubject);

    Optional.ofNullable(settings.get(HTTP_OUTGOING_TOKEN_EXPIRY))
        .map(Object::toString)
        .map(Duration::parse)
        .map(authSettingsBuilder::tokenExpiry);

    Optional.ofNullable(settings.get(HTTP_OUTGOING_SHARED_SECRET))
        .map(Object::toString)
        .map(authSettingsBuilder::encryptedTokenSharedSecret);

    return authSettingsBuilder.build();
  }

  /**
   * endpoint to POST packets to. If url contains a percent and the link is in `multi` mode, then the segment after this
   * link's own address will be filled where the `%` is  when routing packets.
   *
   * @return An {@link HttpUrl} for this Link.
   */
  HttpUrl url();

  /**
   * Generates a custom settings map representation of this outgoing link settings such that
   * {@code OutgoingLinkSettings.fromCustomSettings(someSettings.toCustomSettingsMap()).equals(someSettings); }
   *
   * @return map with custom settings for this outgoing link settings instance
   */
  @Value.Auxiliary
  default Map<String, Object> toCustomSettingsMap() {
    ImmutableMap.Builder<String, Object> mapBuilder = ImmutableMap.builder();
    mapBuilder.put(HTTP_OUTGOING_URL, url().toString());
    mapBuilder.put(HTTP_OUTGOING_AUTH_TYPE, authType().toString());
    simpleAuthSettings().ifPresent(settings -> mapBuilder.put(HTTP_OUTGOING_SIMPLE_AUTH_TOKEN, settings.authToken()));
    jwtAuthSettings().ifPresent(settings -> {
      mapBuilder.put(HTTP_OUTGOING_TOKEN_SUBJECT, settings.tokenSubject());
      settings.encryptedTokenSharedSecret().ifPresent(value -> mapBuilder.put(HTTP_OUTGOING_SHARED_SECRET, value));
      settings.tokenAudience().ifPresent(value -> mapBuilder.put(HTTP_OUTGOING_TOKEN_AUDIENCE, value.toString()));
      settings.tokenIssuer().ifPresent(value -> mapBuilder.put(HTTP_OUTGOING_TOKEN_ISSUER, value.toString()));
      settings.tokenExpiry().ifPresent(value -> mapBuilder.put(HTTP_OUTGOING_TOKEN_EXPIRY, value.toString()));
    });
    return mapBuilder.build();
  }

  @Value.Check
  default OutgoingLinkSettings validate() {
    LinkSettingsUtils.validate(this);
    return this;
  }

}
