package org.interledger.link.http;

import static org.interledger.link.http.IlpOverHttpLinkSettings.AUTH_TOKEN;
import static org.interledger.link.http.IlpOverHttpLinkSettings.AUTH_TYPE;
import static org.interledger.link.http.IlpOverHttpLinkSettings.DOT;
import static org.interledger.link.http.IlpOverHttpLinkSettings.ILP_OVER_HTTP;
import static org.interledger.link.http.IlpOverHttpLinkSettings.INCOMING;
import static org.interledger.link.http.IlpOverHttpLinkSettings.JWT;
import static org.interledger.link.http.IlpOverHttpLinkSettings.SHARED_SECRET;
import static org.interledger.link.http.IlpOverHttpLinkSettings.SIMPLE;
import static org.interledger.link.http.IlpOverHttpLinkSettings.TOKEN_AUDIENCE;
import static org.interledger.link.http.IlpOverHttpLinkSettings.TOKEN_ISSUER;
import static org.interledger.link.http.IlpOverHttpLinkSettings.TOKEN_SUBJECT;

import com.google.common.collect.ImmutableMap;
import okhttp3.HttpUrl;
import org.immutables.value.Value;
import org.immutables.value.Value.Modifiable;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Settings for incoming ILP-over-HTTP Links. Note that this interface
 * purposefully does not define a `sub` claim because the `accountId` for any given account should always be used
 * instead for verification purposes.
 */
@Value.Immutable
@Modifiable
public interface IncomingLinkSettings extends AuthenticatedLinkSettings {

  String HTTP_INCOMING_AUTH_TYPE = ILP_OVER_HTTP + DOT + INCOMING + DOT + AUTH_TYPE;

  String INCOMING_JWT_SETINGS_PREFIX = ILP_OVER_HTTP + DOT + INCOMING + DOT + JWT;
  String INCOMING_SIMPLE_SETINGS_PREFIX = ILP_OVER_HTTP + DOT + INCOMING + DOT + SIMPLE;
  String HTTP_INCOMING_TOKEN_ISSUER = INCOMING_JWT_SETINGS_PREFIX + DOT + TOKEN_ISSUER;
  String HTTP_INCOMING_TOKEN_AUDIENCE = INCOMING_JWT_SETINGS_PREFIX + DOT + TOKEN_AUDIENCE;
  String HTTP_INCOMING_SHARED_SECRET = INCOMING_JWT_SETINGS_PREFIX + DOT + SHARED_SECRET;
  String HTTP_INCOMING_TOKEN_SUBJECT = INCOMING_JWT_SETINGS_PREFIX + DOT + TOKEN_SUBJECT;
  String HTTP_INCOMING_SIMPLE_AUTH_TOKEN = INCOMING_SIMPLE_SETINGS_PREFIX + DOT + AUTH_TOKEN;

  static ImmutableIncomingLinkSettings.Builder builder() {
    return ImmutableIncomingLinkSettings.builder();
  }

  /**
   * Constructs a new builder with the correct custom settings, as found in {@code customSettings}. Custom settings
   * are expected to follow 1 of the following sets of properties.
   * <p>
   *   Simple
   * </p>
   * <pre>
   *   "ilpOverHttp.incoming.auth_type": "SIMPLE"
   *   "ilpOverHttp.incoming.simple.auth_token": "password"
   * </pre>
   *
   * <p>
   *   JWT (using HS256)
   * </p>
   * <pre>
   *   "ilpOverHttp.incoming.auth_type": "JWT_HS_256"
   *   "ilpOverHttp.incoming.jwt.shared_secret": "some-key-used-for-symmetric-encryption",
   *   "ilpOverHttp.incoming.jwt.subject": "foo@me"
   * </pre>
   *
   * <p>
   *   JWT (using RS256)
   * </p>
   * <pre>
   *   "ilpOverHttp.incoming.auth_type": "JWT_RS_256"
   *   "ilpOverHttp.incoming.jwt.issuer": "http://them.example.com",
   *   "ilpOverHttp.incoming.jwt.audience": "https://me.example.com",
   *   "ilpOverHttp.incoming.jwt.subject": "foo@me"
   * </pre>
   *
   * * @param customSettings A {@link Map} of custom settings to apply.
   *
   * @return A {@link ImmutableIncomingLinkSettings.Builder} with applied custom settings.
   */
  static ImmutableIncomingLinkSettings.Builder fromCustomSettings(Map<String, Object> customSettings) {
    Objects.requireNonNull(customSettings);

    return applyCustomSettings(ImmutableIncomingLinkSettings.builder(), customSettings);
  }

  /**
   * Populate a supplied builder with the correct custom settings, as found in {@code customSettings}.
   *
   * @param builder        A {@link ImmutableIncomingLinkSettings.Builder} to apply custom settings to.
   * @param customSettings A {@link Map} of custom settings to apply.
   *
   * @return A {@link ImmutableIncomingLinkSettings.Builder} with applied custom settings.
   */
  static ImmutableIncomingLinkSettings.Builder applyCustomSettings(
      final ImmutableIncomingLinkSettings.Builder builder, Map<String, Object> customSettings
  ) {
    Objects.requireNonNull(builder);
    Objects.requireNonNull(customSettings);

    Optional.ofNullable(LinkSettingsUtils.flattenSettings(customSettings))
        .ifPresent(settings ->
              LinkSettingsUtils.getIncomingAuthType(settings)
                  .ifPresent((authType) -> {
                    builder.authType(authType);
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
    return Optional.ofNullable(settings.get(HTTP_INCOMING_SIMPLE_AUTH_TOKEN))
        .map(Object::toString)
        .map(SimpleAuthSettings::forAuthToken)
        .orElseThrow(() -> new IllegalArgumentException(HTTP_INCOMING_SIMPLE_AUTH_TOKEN + " required"));
  }

  static JwtAuthSettings buildSettingsForJwt(Map<String, Object> settings) {
    ImmutableJwtAuthSettings.Builder authSettingsBuilder = JwtAuthSettings.builder();
    Optional.ofNullable(settings.get(HTTP_INCOMING_TOKEN_ISSUER))
        .map(Object::toString)
        .map(HttpUrl::parse)
        .map(authSettingsBuilder::tokenIssuer);

    Optional.ofNullable(settings.get(HTTP_INCOMING_TOKEN_AUDIENCE))
        .map(Object::toString)
        .map(authSettingsBuilder::tokenAudience);

    Optional.ofNullable(settings.get(HTTP_INCOMING_TOKEN_SUBJECT))
        .map(Object::toString)
        .map(authSettingsBuilder::tokenSubject);

    Optional.ofNullable(settings.get(HTTP_INCOMING_SHARED_SECRET))
        .map(Object::toString)
        .map(authSettingsBuilder::encryptedTokenSharedSecret);

    return authSettingsBuilder.build();
  }

  /**
   * <p>The minimum amount of time (in milliseconds) to budget for receiving a response message from an account.</p>
   *
   * <p>Especially useful for ILP packets, if a packet expires in 30 seconds, then a link should only wait 29 seconds
   * before timing out so that it can generally be sure to reject the request (as opposed to merely allowing a timeout
   * to occur, because timeouts are ambiguous).</p>
   *
   * @return A {@link Duration}.
   */
  @Value.Default
  default Duration getMinMessageWindow() {
    return Duration.of(2500, ChronoUnit.MILLIS);
  }

  /**
   * The type of Auth to support for outgoing HTTP connections.
   *
   * @return A {@link IlpOverHttpLinkSettings.AuthType} for incoming link
   */
  IlpOverHttpLinkSettings.AuthType authType();

  /**
   * Auth settings if using SIMPLE scheme.
   *
   * @return settings
   */
  Optional<SimpleAuthSettings> simpleAuthSettings();

  /**
   * Auth settings if using JWT scheme.
   *
   * @return settings
   */
  Optional<JwtAuthSettings> jwtAuthSettings();

  /**
   * Generates a custom settings map representation of this incoming link settings such that
   * {@code IncomingLinkSettings.fromCustomSettings(someSettings.toCustomSettingsMap()).equals(someSettings); }
   *
   * @return map with custom settings for this incoming link settings instance
   */
  @Value.Auxiliary
  default Map<String, Object> toCustomSettingsMap() {
    ImmutableMap.Builder<String, Object> mapBuilder = ImmutableMap.builder();
    mapBuilder.put(HTTP_INCOMING_AUTH_TYPE, authType().toString());
    simpleAuthSettings().ifPresent(settings -> mapBuilder.put(HTTP_INCOMING_SIMPLE_AUTH_TOKEN, settings.authToken()));
    jwtAuthSettings().ifPresent(settings -> {
      mapBuilder.put(HTTP_INCOMING_TOKEN_SUBJECT, settings.tokenSubject());
      settings.encryptedTokenSharedSecret().ifPresent(value -> mapBuilder.put(HTTP_INCOMING_SHARED_SECRET, value));
      settings.tokenAudience().ifPresent(value -> mapBuilder.put(HTTP_INCOMING_TOKEN_AUDIENCE, value.toString()));
      settings.tokenIssuer().ifPresent(value -> mapBuilder.put(HTTP_INCOMING_TOKEN_ISSUER, value.toString()));
    });
    return mapBuilder.build();
  }

  @Value.Check
  default IncomingLinkSettings validate() {
    LinkSettingsUtils.validate(this);
    return this;
  }

}
